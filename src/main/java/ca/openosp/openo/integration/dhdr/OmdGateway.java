package ca.openosp.openo.integration.dhdr;
/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */

import ca.openosp.OscarProperties;
import ca.openosp.openo.commn.dao.OMDGatewayTransactionLogDao;
import ca.openosp.openo.commn.dao.SystemPreferencesDao;
import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;
import ca.openosp.openo.commn.model.SystemPreferences;

import ca.openosp.openo.integration.fhircast.Event;
import ca.openosp.openo.integration.ohcms.CMSException;
import ca.openosp.openo.integration.ohcms.CMSManager;
import ca.openosp.openo.integration.oneId.OneIDTokenUtils;
import ca.openosp.openo.integration.oneId.OneIdGatewayData;
import ca.openosp.openo.integration.oneId.OneIdSession;
import ca.openosp.openo.integration.oneId.OneIdSessionDao;
import ca.openosp.openo.integration.oneId.TokenExpiredException;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.PKCEUtils;
import ca.openosp.openo.utility.PathUtils;
import ca.openosp.openo.utility.SpringUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.struts2.ServletActionContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OmdGateway {

	private final String KEYSTORE_FILE = "keystore_file";
	private final String KEYSTORE_PATH = "/opt/labs/olis/oneid.jks";
	private static final Logger logger = MiscUtils.getLogger();
	
	protected OMDGatewayTransactionLogDao transactionLogDao = SpringUtils.getBean(OMDGatewayTransactionLogDao.class);
	private SystemPreferencesDao systemPreferencesDao = SpringUtils.getBean(SystemPreferencesDao.class);
    private OneIdSessionDao oneIdSessionDao = SpringUtils.getBean(OneIdSessionDao.class);

	public enum ToolbarKeys {
		FHIR_ISS("FHIR_iss"),
		HUB_URL("hub.url"),
		CMS_URL("cms_url");

		public final String key;

		ToolbarKeys(String key) {
			this.key = key;
		}
	}

	/**
	 * The {@code transactionType} values written for the outcome of an EHR service (viewlet) launch.
	 *
	 * <p>Named here because two sides have to agree on them and neither owns both ends: the viewlet
	 * result endpoint writes them, and the DHDR temporary consent unblock report selects and labels
	 * its rows by them. They were literals in both places, so a branch added on the writing side
	 * reached the audit table under a value the report does not select, and the row was simply
	 * absent from a report that is meant to account for it.
	 *
	 * <p>These are stored values. Live rows carry them in
	 * {@code OMDGatewayTransactionLog.transactionType}, so renaming one orphans every row written
	 * under the old name. Add rather than rename, and add to
	 * {@link ConsentOverrideChoice#reportTransactionTypes()} in the same change.
	 *
	 * <p>{@link #VIEWLET_RESULT} is written for a confirmed outcome and for a failed one alike; the
	 * success column is what separates them. The other three carry their meaning on their own.
	 *
	 * @since 2026-09-02
	 */
	public static final String VIEWLET_RESULT = "viewletResult";

	/**
	 * The clinician closed the EHR service without going through with it.
	 *
	 * @see #VIEWLET_RESULT
	 */
	public static final String VIEWLET_RESULT_CANCELLED = "viewletResultCancelled";

	/**
	 * The window closed without answering, so no outcome was observed.
	 *
	 * @see #VIEWLET_RESULT
	 */
	public static final String VIEWLET_RESULT_NO_RESPONSE = "viewletResultNoResponse";

	/**
	 * The reply confirmed the viewlet call but not the service it was launched for.
	 *
	 * @see #VIEWLET_RESULT
	 */
	public static final String VIEWLET_RESULT_PARTIAL = "viewletResultPartial";

	public static OMDGatewayTransactionLog getOMDGatewayTransactionLog(LoggedInInfo loggedInInfo, Integer demographicNo, String externalSystem, String transactionType) {
		OMDGatewayTransactionLog omdGatewayTransactionLog = new OMDGatewayTransactionLog();
		OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
		if(oneIdGatewayData != null) {
			logger.debug("Access token expires in " + oneIdGatewayData.howLongUntilAccessTokenIsExpired() + "s");
			omdGatewayTransactionLog.setSecondsLeft(oneIdGatewayData.howLongUntilAccessTokenIsExpired());
			omdGatewayTransactionLog.setUao(oneIdGatewayData.getUao());
			omdGatewayTransactionLog.setContextSessionId(oneIdGatewayData.getCtxSessionId());
			omdGatewayTransactionLog.setUniqueSessionId(oneIdGatewayData.getUniqueSessionId());
		}

		omdGatewayTransactionLog.setDemographicNo(demographicNo);
		omdGatewayTransactionLog.setExternalSystem(externalSystem);
		omdGatewayTransactionLog.setInitiatingProviderNo(loggedInInfo.getLoggedInProviderNo());
		omdGatewayTransactionLog.setOscarSessionId(loggedInInfo.getSession().getId());
		omdGatewayTransactionLog.setStarted(new Date());
		omdGatewayTransactionLog.setTransactionType(transactionType);

		return omdGatewayTransactionLog;
	}

	protected static void completeLog(OMDGatewayTransactionLog log, Response response2) {
		completeLog(log, response2, true);
	}

	/**
	 * Records the outcome of a gateway call on its transaction log row.
	 *
	 * <p>The response headers and body are only captured when {@code storeResponseDetail} is set.
	 * Callers that exchange credentials must leave it unset: the OAuth token endpoints return access
	 * and refresh tokens in the body, an authorize response carries the authorization code in its
	 * Location header, and either may set a session cookie. None of that belongs in the audit table.
	 * The outcome fields above are always recorded, for every caller, and the correlation headers are
	 * lifted into their own columns, so a call excluded here is still traceable.
	 *
	 * <p>The entity is buffered before it is read, because auditing a response must not consume it.
	 * A CXF response stream is readable once, and reading it closes it, so without buffering the
	 * caller's own read of the same response fails with "Entity is not available". Every caller that
	 * reads a body does so after this method has returned - {@code DHDRManager.search2} and the DHIR
	 * retrievals for the payload they were called for, the OAuth callers for a token response whose
	 * failure was recorded here.</p>
	 *
	 * @param log OMDGatewayTransactionLog the row opened before the call was made
	 * @param response2 Response the response returned by the gateway
	 * @param storeResponseDetail boolean whether the response headers and body belong in the audit row
	 */
	protected static void completeLog(OMDGatewayTransactionLog log, Response response2, boolean storeResponseDetail) {
		log.setResultCode(response2.getStatus());
		log.setEnded(new Date());

		try {
			response2.bufferEntity();
		} catch (Exception e) {
			// An entity already consumed or closed cannot be buffered, and the reads below then fail on it
			// too - which is why they go through readBody rather than calling readEntity directly. The
			// class name only: a transport exception's message carries the request URI, and the DHDR
			// request URI carries the patient's health number.
			logger.warn("Gateway response entity could not be buffered (" + e.getClass().getSimpleName() + ")");
		}

		// The correlation identifiers are recorded whenever the service supplies them, but their
		// absence must not suppress the outcome: a failure that omits X-Request-Id is still a failure.
		String xRequestId = response2.getHeaderString("X-Request-Id");
		if (xRequestId != null) {
			log.setxRequestId(xRequestId);
		}
		String xLobTxId = response2.getHeaderString("X-LobTxId");
		if (xLobTxId != null) {
			log.setxLobTxId(xLobTxId);
		}
		String xCorrelationId = response2.getHeaderString("X-Correlation-Id");
		if (xCorrelationId != null) {
			log.setxCorrelationId(xCorrelationId);
		}

		// Read once and used three times below. readBody re-reads the buffered entity on every call,
		// and a DHDR searchset is the largest body the gateway returns, so this is not a place to
		// take it three times. It goes through readBody rather than readEntity so that a response
		// which could not be buffered yields null here instead of throwing over the outcome fields.
		String body = readBody(response2);

		// Parsed once and read three times: every call reaches here, including the OAuth ones whose
		// bodies are not FHIR at all, so this is not a place to parse the same string repeatedly.
		JsonNode responseJson = readJson(body);

		// Stored for any status: a refusal carries the code that explains it, and a 200 can still
		// carry one, for example the notice that a temporary consent unblock is in effect.
		log.setEhrResultCode(extractEhrResultCode(responseJson));

		// DHDR15.01(j) and 15.02(d): the resource ids the interaction carried. Both are null on any
		// call whose body is not a searchset, which is every call but the drug history search.
		log.setMessageHeaderId(extractBundleId(responseJson));
		log.setMedicationDispenseIds(extractMedicationDispenseIds(responseJson));

		boolean failed = response2.getStatus() >= 300;
		log.setSuccess(!failed);
		if (failed) {
			// A CMS refusal body quotes back the patient context just sent, so it is not stored.
			// The outcome code is lifted into its own column above, which is what an auditor
			// filters on. A refusal from any other system carries an OperationOutcome instead.
			if (!CMS_EXTERNAL_SYSTEM.equals(log.getExternalSystem())) {
				log.setError(body);
			}
		}

		// Headers are withheld on the same terms as the body, because an OAuth response carries
		// secrets in both: an authorize response returns the authorization code in Location. The
		// correlation ids are lifted into their own columns above, so a call excluded here is
		// still traceable.
		if (storeResponseDetail) {
			StringBuilder headers = new StringBuilder();
			for (String headerName : response2.getHeaders().keySet()) {
				headers.append(headerName).append(":").append(response2.getHeaderString(headerName)).append("\n");
			}
			log.setHeaders(headers.toString());
			if (!failed) {
				// The body is PHI (DHDR) or clinical payload; the access-controlled audit table is its
				// only sanctioned destination. It must not be echoed to the application log.
				log.setDataRecieved(body);
			}
		}
	}

	/**
	 * Records a failed outcome on a row whose call threw before any response arrived.
	 *
	 * <p>Best effort on purpose: a token refresh has to end in a sign-in prompt, so failing to
	 * write the audit row must not replace that prompt with a database error.</p>
	 *
	 * @param log   OMDGatewayTransactionLog the open row, or null when the failure came before one was written
	 * @param cause Throwable the failure to record
	 */
	private void completeFailedRow(OMDGatewayTransactionLog log, Throwable cause) {
		// A row that already has an end time was closed by completeLog, so the response it records
		// is the real one. Only a row still open is finished here.
		if (log == null || log.getEnded() != null) {
			return;
		}
		try {
			log.setSuccess(Boolean.FALSE);
			log.setError(stackTraceWithoutMessages(cause));
			log.setEnded(new Date());
			transactionLogDao.merge(log);
		} catch (Exception e) {
			logger.warn("Could not record the failed gateway outcome (" + e.getClass().getSimpleName() + ")");
		}
	}

	/** Generates a unique X-Request-Id for a single gateway transaction. */
	protected static String newRequestId() {
		return UUID.randomUUID().toString();
	}

	/** The externalSystem recorded on a context call to the Ontario Health CMS. */
	protected static final String CMS_EXTERNAL_SYSTEM = "CMS";

	/** The FHIRcast event that ends the provider's CMS session. */
	private static final String CMS_USER_LOGOUT_EVENT = "userLogout";

	/**
	 * The client-assertion type named by RFC 7523. Held unencoded: these parameters go in a form
	 * body, and the form writer percent-encodes what it is given, so a pre-encoded value would
	 * reach the gateway double-encoded.
	 */
	private static final String CLIENT_ASSERTION_TYPE =
			"urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

	/** How many nested causes to render before stopping. */
	private static final int MAX_CAUSE_DEPTH = 10;

	/**
	 * Renders a throwable as its class names and stack frames, with every message dropped.
	 *
	 * <p>A failed gateway call raises an exception whose message embeds the request URI, and a
	 * search URI carries the patient's health card number and date of birth. A stack frame holds
	 * only a class, method, file and line, so it cannot carry request data; only the message can.
	 * Keeping the frames and dropping the messages leaves the diagnostic value in the application
	 * log and takes the patient data out of it.
	 *
	 * @param throwable Throwable the exception to render, which may be null
	 * @return String the class names and stack frames, or "(none)" when there is no exception
	 * @since 2026-08-05
	 */
	public static String stackTraceWithoutMessages(Throwable throwable) {
		if (throwable == null) {
			return "(none)";
		}
		StringBuilder rendered = new StringBuilder();
		Throwable current = throwable;
		for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
			if (depth > 0) {
				rendered.append("Caused by: ");
			}
			rendered.append(current.getClass().getName()).append('\n');
			for (StackTraceElement frame : current.getStackTrace()) {
				rendered.append("\tat ").append(frame).append('\n');
			}
			// A throwable may report itself as its own cause; stop rather than loop.
			current = current.getCause() == current ? null : current.getCause();
		}
		return rendered.toString();
	}

	private static final ObjectMapper auditObjectMapper = new ObjectMapper();
	private static final int MAX_EHR_RESULT_CODE_LENGTH = 255;
	private static final int MAX_MESSAGE_ID_LENGTH = 64;

	/**
	 * Parses a response body for the audit row, or reports that there is nothing to read.
	 *
	 * <p>Every gateway call reaches {@link #completeLog}, including the OAuth ones whose bodies are
	 * not FHIR and not necessarily JSON. Parsing once here keeps that cost to one pass and lets each
	 * extractor below stay a pure read of the tree.</p>
	 *
	 * @param body String the raw response body, which may be empty or may not be JSON at all
	 * @return JsonNode the parsed body, or null where there is nothing to parse
	 */
	private static JsonNode readJson(String body) {
		if (body == null || body.trim().isEmpty()) {
			return null;
		}
		try {
			return auditObjectMapper.readTree(body);
		} catch (Exception e) {
			// A body that is not JSON carries none of what the extractors below look for.
			return null;
		}
	}

	/**
	 * Reads the EHR service's own outcome codes out of a response body, for example IN_0045 or
	 * CONSENT_EXISTS. They arrive inside an OperationOutcome, either on its own or as an entry in a
	 * returned Bundle, and they are the only thing that says why a call was refused: the HTTP status
	 * records that it was.
	 *
	 * @param body JsonNode the parsed response body, which may be null or may not be FHIR
	 * @return String the codes found, comma separated, or null when the body carries none
	 * @since 2026-08-04
	 */
	static String extractEhrResultCode(JsonNode body) {
		if (body == null) {
			return null;
		}
		try {
			List<String> codes = new ArrayList<String>();
			collectOutcomeCodes(body, codes);
			if (codes.isEmpty()) {
				return null;
			}
			String joined = String.join(",", codes);
			return joined.length() > MAX_EHR_RESULT_CODE_LENGTH
					? joined.substring(0, MAX_EHR_RESULT_CODE_LENGTH) : joined;
		} catch (Exception e) {
			// A body that is not JSON, or not shaped like FHIR, simply has no code to record.
			return null;
		}
	}

	/**
	 * Walks a parsed body for OperationOutcome resources and collects the codes on their issues.
	 *
	 * <p>The search stops at each OperationOutcome instead of collecting every coding in the body.
	 * A returned Bundle is full of clinical codings, drug identifiers among them, and sweeping
	 * those into an audit column would put patient data in a field meant to hold a status.
	 */
	private static void collectOutcomeCodes(JsonNode node, List<String> codes) {
		if (node == null) {
			return;
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				collectOutcomeCodes(child, codes);
			}
			return;
		}
		if (!node.isObject()) {
			return;
		}
		JsonNode resourceType = node.get("resourceType");
		if (resourceType != null && "OperationOutcome".equals(resourceType.asText())) {
			JsonNode issues = node.get("issue");
			if (issues != null && issues.isArray()) {
				for (JsonNode issue : issues) {
					JsonNode details = issue.get("details");
					JsonNode coding = details == null ? null : details.get("coding");
					if (coding == null || !coding.isArray()) {
						continue;
					}
					for (JsonNode entry : coding) {
						JsonNode code = entry.get("code");
						if (code != null && code.isTextual() && !code.asText().trim().isEmpty()
								&& !codes.contains(code.asText().trim())) {
							codes.add(code.asText().trim());
						}
					}
				}
			}
			return;
		}
		for (JsonNode child : node) {
			collectOutcomeCodes(child, codes);
		}
	}

	/**
	 * Reads the id of a returned searchset {@code Bundle}.
	 *
	 * <p>This is what fills {@code messageHeaderId}. DHDR15.01(j) names {@code MessageHeader.id},
	 * but a DHDR retrieval returns {@code type: searchset} and carries no {@code MessageHeader} -
	 * it is absent from every OMD capture and from the DHDR IG. DHDR15.02(d) resolves the same
	 * value by its own cross-reference: FHIR R4 "Resource - Base Resource Definitions" defines
	 * {@code Resource.id} as the logical id used in the URL for the resource, and for a retrieval
	 * that resource is the Bundle.</p>
	 *
	 * @param body JsonNode the parsed response body, which may be null or may not be FHIR
	 * @return String the Bundle id, or null where the body carries none
	 */
	static String extractBundleId(JsonNode body) {
		if (body == null) {
			return null;
		}
		try {
			JsonNode resourceType = body.get("resourceType");
			if (resourceType == null || !"Bundle".equals(resourceType.asText())) {
				return null;
			}
			JsonNode id = body.get("id");
			if (id == null || !id.isTextual() || id.asText().trim().isEmpty()) {
				return null;
			}
			String value = id.asText().trim();
			// No conformant id can exceed the FHIR id primitive's 64 characters, so this only bites
			// on a malformed one - where recording a bounded value beats failing the whole row.
			return value.length() > MAX_MESSAGE_ID_LENGTH
					? value.substring(0, MAX_MESSAGE_ID_LENGTH) : value;
		} catch (Exception e) {
			// A body that is not JSON, or not shaped like FHIR, simply has no id to record.
			return null;
		}
	}

	/**
	 * Collects the ids of every {@code MedicationDispense} a returned searchset {@code Bundle}
	 * carries, in the order they arrive.
	 *
	 * <p>One transaction row covers a search that returns many dispenses, so DHDR15.01(j)'s
	 * dispense ids are a list rather than a value. Nothing is truncated here: the column's own
	 * setter bounds an over-long list to whole ids and marks how many it left out.</p>
	 *
	 * @param body JsonNode the parsed response body, which may be null or may not be FHIR
	 * @return String the ids comma separated, or null where the body carries none
	 */
	static String extractMedicationDispenseIds(JsonNode body) {
		if (body == null) {
			return null;
		}
		try {
			JsonNode entries = body.get("entry");
			if (entries == null || !entries.isArray()) {
				return null;
			}
			List<String> ids = new ArrayList<String>();
			for (JsonNode entry : entries) {
				JsonNode resource = entry.get("resource");
				if (resource == null) {
					continue;
				}
				JsonNode resourceType = resource.get("resourceType");
				if (resourceType == null || !"MedicationDispense".equals(resourceType.asText())) {
					continue;
				}
				JsonNode id = resource.get("id");
				if (id != null && id.isTextual() && !id.asText().trim().isEmpty()) {
					ids.add(id.asText().trim());
				}
			}
			return ids.isEmpty() ? null : String.join(",", ids);
		} catch (Exception e) {
			// A body that is not JSON, or not shaped like FHIR, simply has no ids to record.
			return null;
		}
	}

	/**
	 * Resolves the base URL of this OpenO instance, used to compose the OAuth2 redirect URI and the
	 * post-logout redirect URI. The configured {@code clinic.url} property wins whenever it holds a
	 * value; when it is absent the address is derived from the request being served, so a stock
	 * installation works without the property being set.
	 *
	 * @return String the base URL ending in a slash, for example {@code https://emr.example.ca/oscar/}
	 * @throws IllegalStateException when {@code clinic.url} is unset and there is no request to derive from
	 * @since 2026-08-04
	 */
	static String resolveBaseUrl() {
		String configured = OscarProperties.getInstance().getProperty("clinic.url");
		if (configured != null && !configured.trim().isEmpty()) {
			return PathUtils.addTrailingSlash(configured.trim());
		}
		HttpServletRequest request = currentRequest();
		if (request == null) {
			throw new IllegalStateException("The OpenO address could not be determined: clinic.url is not "
					+ "configured and this call is not serving a request. Set clinic.url in the properties file.");
		}
		return PathUtils.addTrailingSlash(baseUrlFromRequest(request));
	}

	/** Returns the request being served, or null outside a request. */
	private static HttpServletRequest currentRequest() {
		try {
			return ServletActionContext.getRequest();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Builds the base URL from the address the servlet container reports for the request. The
	 * X-Forwarded headers are deliberately not read here: they are set by the client and would let a
	 * crafted request choose the redirect URI. A proxied installation supplies its public address one
	 * of two ways, both of which land in the values read below: configure the container's
	 * {@code RemoteIpValve} (or {@code RemoteIpFilter}), which resolves those headers only for
	 * requests arriving from a trusted proxy, or set {@code clinic.url}, which takes precedence over
	 * this method entirely.
	 *
	 * @param request HttpServletRequest the request being served
	 * @return String the scheme, authority and context path, with no trailing slash
	 */
	private static String baseUrlFromRequest(HttpServletRequest request) {
		String scheme = request.getScheme();
		int port = request.getServerPort();
		StringBuilder url = new StringBuilder(scheme).append("://").append(request.getServerName());
		if (port > 0 && port != defaultPort(scheme)) {
			url.append(':').append(port);
		}
		return url.append(request.getContextPath()).toString();
	}

	private static int defaultPort(String scheme) {
		return "https".equalsIgnoreCase(scheme) ? 443 : 80;
	}

	/**
	 * Reads a gateway response body, or {@code null} when it cannot be read.
	 *
	 * <p>An audit row is owed for every call, and its outcome fields - status, return code,
	 * correlation identifiers - do not depend on the body. A body that cannot be read therefore costs
	 * the row its {@code error} or {@code dataRecieved} text, not the row itself. The case that
	 * reaches here is an entity consumed before this class saw it: buffering fails on it, and so does
	 * this read.</p>
	 *
	 * @param response2 Response the response to read the body from
	 * @return String the body, or {@code null} if it could not be read
	 */
	private static String readBody(Response response2) {
		try {
			return response2.readEntity(String.class);
		} catch (Exception e) {
			// Class name only, for the reason given where the buffering failure is logged.
			logger.warn("Gateway response body could not be read (" + e.getClass().getSimpleName() + ")");
			return null;
		}
	}

	/**
	 * Records an interaction that does not itself call an EHR service - a user viewing or printing
	 * data already retrieved from one (DHDR15.01).
	 *
	 * @param loggedInInfo LoggedInInfo the current session, supplying the initiating EMR user
	 * @param externalSystem String the EHR service the data originated from, e.g. {@link AuditInfo#DHDR}
	 * @param transactionType String the interaction, e.g. {@link AuditInfo#VIEW} or {@link AuditInfo#PRINT}
	 * @param demographicNo Integer the patient the interaction concerns, or {@code null} when the
	 *     interaction is not scoped to a single patient
	 */
	public void logInteraction(LoggedInInfo loggedInInfo, String externalSystem, String transactionType, Integer demographicNo) {
		logInteraction(loggedInInfo, externalSystem, transactionType, demographicNo, Boolean.TRUE, null, null);
	}

	/**
	 * Records an interaction that does not itself call an EHR service, with the outcome the EMR actually
	 * observed (DHDR15.01 / DHDR15.02).
	 *
	 * <p>The four-argument form above is for interactions that cannot fail once they are reached - a view
	 * or a print of data already in hand - and so records success unconditionally. This form exists for
	 * the ones that can: a decision posted back from a viewlet may report that it did not complete, or
	 * report a code the EMR does not recognise, and an audit row must not claim an outcome nobody
	 * observed. The decision itself stays in {@code transactionType}, which is what the DHDR13.02 report
	 * reads, so recording an unsuccessful transaction does not hide which choice was made.</p>
	 *
	 * @param loggedInInfo LoggedInInfo the current session, supplying the initiating EMR user
	 * @param externalSystem String the EHR service the interaction concerns, e.g. {@link AuditInfo#DHDR}
	 * @param transactionType String the interaction, e.g. {@link AuditInfo#VIEW} or a consent-override choice
	 * @param demographicNo Integer the patient the interaction concerns, or {@code null} when it is not
	 *     scoped to a single patient
	 * @param success Boolean the outcome the EMR observed
	 * @param detail String the payload describing the interaction, or {@code null} to leave it unset
	 * @param correlationId String the correlation identifier, or {@code null} when the caller had none
	 */
	public void logInteraction(LoggedInInfo loggedInInfo, String externalSystem, String transactionType,
			Integer demographicNo, Boolean success, String detail, String correlationId) {
		OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, demographicNo, externalSystem, transactionType);
		omdGatewayTransactionLog.setSuccess(success);
		if (detail != null) {
			omdGatewayTransactionLog.setDataRecieved(detail);
		}
		if (correlationId != null) {
			omdGatewayTransactionLog.setxCorrelationId(correlationId);
		}
		persistCompleted(omdGatewayTransactionLog);
	}

	/**
	 * Stores a transaction-log row that records an event rather than a call made over the wire.
	 *
	 * <p>A row with no {@code ended} timestamp reads as a transaction still in flight, and telling a
	 * finished transaction from an unfinished one is what that column exists for (DHDR15.01 h). These
	 * rows have no HTTP call to wait on - the event is complete at the moment it is written - so the
	 * start and end coincide rather than the end being absent. Calls that do go over the wire are
	 * stamped by {@link #completeLog} instead.
	 *
	 * @param log OMDGatewayTransactionLog the row to finish and store
	 */
	private void persistCompleted(OMDGatewayTransactionLog log) {
		log.setEnded(new Date());
		transactionLogDao.persist(log);
	}

	protected List<OperationOutcome> hasOperationOutcome(Bundle bundle)  {
		List<OperationOutcome> result = new ArrayList<OperationOutcome>();

		for(BundleEntryComponent comp : bundle.getEntry()) {
			Resource resource = comp.getResource();
			if(resource.getResourceType() == ResourceType.OperationOutcome) {
				OperationOutcome oo = (OperationOutcome)resource;
				result.add(oo);
			}
		}
		return result;
	}
	
	public boolean hasGatewayPropertiesSet(LoggedInInfo loggedInInfo) throws Exception{
		String clientId = getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_id);
		String keystoreLocation = getPreferenceValue(SystemPreferences.ONEID_KEYS.keystore_path);
		Path keystorePath = keystoreLocation == null || keystoreLocation.trim().isEmpty()
				? null
				: Paths.get(keystoreLocation);
		String keystorePassword = getPreferenceValue(SystemPreferences.ONEID_KEYS.keystore_password);
		OneIdSession oneIdSession = oneIdSessionDao.find(loggedInInfo.getLoggedInProviderNo());
		String endPoint = oneIdSession == null ? "" : oneIdSession.getUrlFromToolbar(ToolbarKeys.FHIR_ISS.key);

		StringBuilder sb = new StringBuilder();

		logger.debug("clientId " + clientId + " publicKeyStore " + keystorePath + " endPoint " + endPoint);

		if(clientId == null || clientId.trim().isEmpty()) {
			sb.append("Client Id has not been configured. Use OSCAR property 'oneid.consumerKey' to configure.\n");
		}



		if(keystorePath == null) {
			sb.append("Public Keystore has not been configured. Use OSCAR property 'oneid.gateway.keystore' to configure.\n");
		} else {
			try {
				if(Files.notExists(keystorePath)) {
					sb.append("Public Keystore can not be found at: ").append(keystorePath).append("\n");
				}
			}catch(Exception e) {
				sb.append("Public Keystore can not be found at: ").append(keystorePath).append("\n");
			}
		}

		if(keystorePassword == null || keystorePassword.trim().isEmpty()) {
			sb.append("Keystore password has not been configured. Use OSCAR property 'oneid.gateway.keystore.password' to configure.\n");
		}

		if(endPoint == null || endPoint.trim().isEmpty()) {
			sb.append("Gateway endPoint has not been configured. Use OSCAR property 'oneid.gateway.url' to configure.\n");
		}


		if(sb.length() > 0) {
			OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, null, "GATEWAY" , "Configuration Error");
			omdGatewayTransactionLog.setStarted(new Date());
			// This row records an attempt that failed, so it says so (DHDR15.01 h). It sits on the
			// search path - getWebClient calls this before every call - and a row left with a null
			// success reads as a transaction still in flight rather than one that never left.
			omdGatewayTransactionLog.setSuccess(Boolean.FALSE);
			omdGatewayTransactionLog.setError(sb.toString());
			persistCompleted(omdGatewayTransactionLog);
			throw(new Exception("Gateway Configuration Error"));
		}
    logger.info("has props out " + sb);
		return true;
	}
	
	public void logError(LoggedInInfo loggedInInfo,String externalSystem, String transactionType,String error) {
		logError(loggedInInfo, externalSystem, transactionType, error, null, null);
	}

	public void logError(LoggedInInfo loggedInInfo,String externalSystem, String transactionType,String error,Integer demographicNo,String uniqueToken) {
		logError(loggedInInfo, externalSystem, transactionType, error, null, demographicNo, uniqueToken);
	}

	/**
	 * Records a failed interaction, keeping the operator's explanation and the external system's own
	 * response in the columns each belongs in.
	 *
	 * <p>The gateway log screen renders {@code error} and not {@code dataRecieved}, so an explanation
	 * written to the latter never reaches the person reading the log. A raw response body is the
	 * other way round: it can carry patient detail, and belongs where whole payloads already go - on
	 * the row, out of the rendered column. Writing both on one row lets it say what went wrong
	 * without putting the response itself on screen.
	 *
	 * @param loggedInInfo LoggedInInfo the acting provider session
	 * @param externalSystem String the system the call was made to
	 * @param transactionType String the transaction being recorded
	 * @param error String the operator-facing explanation, which must carry no patient detail
	 * @param dataReceived String the external system's own response, or null when there was none
	 * @param demographicNo Integer the patient the call concerns, or null
	 * @param uniqueToken String the correlation id tying this row to its request, or null
	 * @since 2026-09-02
	 */
	public void logError(LoggedInInfo loggedInInfo,String externalSystem, String transactionType,String error,String dataReceived,Integer demographicNo,String uniqueToken) {
		OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, null, externalSystem, transactionType);
		omdGatewayTransactionLog.setStarted(new Date());
		omdGatewayTransactionLog.setSuccess(Boolean.FALSE);
		omdGatewayTransactionLog.setError(error);
		if(dataReceived != null) {
			omdGatewayTransactionLog.setDataRecieved(dataReceived);
		}
		if(demographicNo != null) {
			omdGatewayTransactionLog.setDemographicNo(demographicNo);
		}
		if(uniqueToken != null) {
			omdGatewayTransactionLog.setxCorrelationId(uniqueToken);
		}
		persistCompleted(omdGatewayTransactionLog);
	}
	
	public void logDataReceived(LoggedInInfo loggedInInfo,String externalSystem, String transactionType,String dataReceived,Integer demographicNo) {
		logDataReceived( loggedInInfo, externalSystem,  transactionType, dataReceived, demographicNo,null) ;
	}
	
	public void logDataReceived(LoggedInInfo loggedInInfo,String externalSystem, String transactionType,String dataReceived,Integer demographicNo,String uniqueToken) {
		logDataReceived(loggedInInfo, externalSystem, transactionType, dataReceived, demographicNo, uniqueToken, Boolean.TRUE);
	}

	/**
	 * Records data received from an external system, with the outcome stated by the caller.
	 *
	 * <p>The success column is what an auditor filters on to answer whether an access happened, so
	 * it has to mean one thing. The overloads above record a success because that is all they are
	 * used for; a caller that records a mix of outcomes through this method must pass the real one,
	 * otherwise a row describing a failed operation still reads as successful.
	 *
	 * @param loggedInInfo   LoggedInInfo the acting provider session
	 * @param externalSystem String the system the data came from
	 * @param transactionType String the transaction being recorded
	 * @param dataReceived   String the payload to store on the row
	 * @param demographicNo  Integer the patient the data concerns, or null
	 * @param uniqueToken    String the correlation id tying this row to its request, or null
	 * @param success        Boolean the real outcome; null when it is genuinely not known
	 * @since 2026-08-04
	 */
	public void logDataReceived(LoggedInInfo loggedInInfo,String externalSystem, String transactionType,String dataReceived,Integer demographicNo,String uniqueToken,Boolean success) {
		OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, null, externalSystem, transactionType);
		omdGatewayTransactionLog.setStarted(new Date());
		omdGatewayTransactionLog.setSuccess(success);
		if(demographicNo != null) {
			omdGatewayTransactionLog.setDemographicNo(demographicNo);
		}
		omdGatewayTransactionLog.setDataRecieved(dataReceived);
		if(uniqueToken != null) {
			omdGatewayTransactionLog.setxCorrelationId(uniqueToken);
		}
		persistCompleted(omdGatewayTransactionLog);
	}

	public WebClient getWebClientWholeURL(LoggedInInfo loggedInInfo,String url) throws Exception {
		hasGatewayPropertiesSet(loggedInInfo);
		WebClient wc = WebClient.create(url);
		WebClient.getConfig(wc).getHttpConduit().setTlsClientParameters(getTLSClientParameters(loggedInInfo));
		long timeoutMillis = getTimeoutMillis();
		WebClient.getConfig(wc).getHttpConduit().getClient().setConnectionTimeout(timeoutMillis);
		WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(timeoutMillis);
		return wc;
	}
	
	public WebClient getWebClient(LoggedInInfo loggedInInfo,String resource) throws Exception {
			hasGatewayPropertiesSet(loggedInInfo);
			String gatewayUrl = getEndpointURL(loggedInInfo.getLoggedInProviderNo());
			String fullURL = gatewayUrl+resource;

			WebClient wc = WebClient.create(fullURL);
			WebClient.getConfig(wc).getHttpConduit().setTlsClientParameters(getTLSClientParameters(loggedInInfo));
			long timeoutMillis = getTimeoutMillis();
			WebClient.getConfig(wc).getHttpConduit().getClient().setConnectionTimeout(timeoutMillis);
			WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(timeoutMillis);

			return wc;
		}

	/** Gateway connection/receive timeout in milliseconds, from the configurable timeout preference (in seconds). */
	protected long getTimeoutMillis() {
		long seconds = 65;
		String configured = getPreferenceValue(SystemPreferences.ONEID_KEYS.timeout);
		if (configured != null && !configured.trim().isEmpty()) {
			try {
				seconds = Long.parseLong(configured.trim());
			} catch (NumberFormatException e) {
				logger.warn("Invalid ONE ID gateway timeout '" + configured + "'; using " + seconds + "s");
			}
		}
		return seconds * 1000;
	}

	/**
	 * A socket factory and the keystore it was built from. The file's own timestamp and size are
	 * part of what identifies that keystore, because replacing an uploaded keystore keeps the same
	 * path and password, and the settings alone cannot tell the new file from the old one.
	 */
	private static final class TlsKeystore {
		private final String path;
		private final String password;
		private final long modified;
		private final long size;
		private final SSLSocketFactory socketFactory;

		private TlsKeystore(String path, String password, File file, SSLSocketFactory socketFactory) {
			this.path = path;
			this.password = password;
			this.modified = file.lastModified();
			this.size = file.length();
			this.socketFactory = socketFactory;
		}

		private boolean isFor(String otherPath, String otherPassword, File file) {
			return Objects.equals(path, otherPath)
					&& Objects.equals(password, otherPassword)
					&& modified == file.lastModified()
					&& size == file.length();
		}
	}

	/** The last keystore built, reused until the settings or the file itself change. */
	private static volatile TlsKeystore tlsKeystore;

	protected TLSClientParameters getTLSClientParameters(LoggedInInfo loggedInInfo) throws Exception {
		hasGatewayPropertiesSet(loggedInInfo);
		TLSClientParameters tlsParams = new TLSClientParameters();
		tlsParams.setSSLSocketFactory(clientSocketFactory());
		// Hostname (CN) verification stays on; disable it only for local development.
		if ("true".equalsIgnoreCase(OscarProperties.getInstance().getProperty("oneid.disable_tls_cn_check"))) {
			tlsParams.setDisableCNCheck(true);
		}
		return tlsParams;
	}

	/**
	 * The socket factory that presents the client certificate, built once per keystore. Every
	 * gateway call went through here, so the keystore was read from disk, its key material parsed
	 * and a fresh SSL context built each time, which also gave the connection no session to resume.
	 *
	 * @return SSLSocketFactory the factory holding the configured client certificate
	 * @throws Exception when the keystore cannot be read or the context cannot be built
	 */
	private SSLSocketFactory clientSocketFactory() throws Exception {
		String keystorePath = getPreferenceValue(SystemPreferences.ONEID_KEYS.keystore_path);
		String keystorePassword = getPreferenceValue(SystemPreferences.ONEID_KEYS.keystore_password);
		File keystoreFile = Paths.get(keystorePath).toFile();

		TlsKeystore cached = tlsKeystore;
		if (cached != null && cached.isFor(keystorePath, keystorePassword, keystoreFile)) {
			return cached.socketFactory;
		}

		KeyStore ks = loadKeystore(keystorePath, keystorePassword);
		// The key material is what presents the client certificate to the gateway.
		SSLContext sslContext = SSLContexts.custom()
				.loadKeyMaterial(ks, keystorePassword.toCharArray())
				.build();
		SSLSocketFactory socketFactory = sslContext.getSocketFactory();
		tlsKeystore = new TlsKeystore(keystorePath, keystorePassword, keystoreFile, socketFactory);
		return socketFactory;
	}

	public Response doGet(LoggedInInfo loggedInInfo, WebClient wc) throws TokenExpiredException {
		return doGet(loggedInInfo,wc,null);
	}

	/**
	 * Reads a ONE ID gateway setting.
	 *
	 * @param key ONEID_KEYS the setting to read
	 * @return String the configured value, or null where the setting has no row or holds no value
	 */
	private String getPreferenceValue(SystemPreferences.ONEID_KEYS key) {
		SystemPreferences preference = systemPreferencesDao.findPreferenceByName(key);
		return preference == null ? null : preference.getValue();
	}

	/**
	 * A setting's value, or a failure naming the setting that has none.
	 *
	 * <p>A URL composed from an unset setting is still a URL: an absent row concatenates as the
	 * text "null", and the value the migration seeds is empty, which leaves the base address on its
	 * own. Either reaches the gateway as a redirect address it does not recognise, and what comes
	 * back names the address rather than the setting behind it.</p>
	 *
	 * @param key   ONEID_KEYS the setting to read
	 * @param label String the setting's name on the gateway settings screen
	 * @return String the value, trimmed
	 * @throws IllegalStateException when the setting holds no value
	 */
	private String requirePreferenceValue(SystemPreferences.ONEID_KEYS key, String label) {
		String value = getPreferenceValue(key);
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalStateException("The " + label + " setting is not configured.");
		}
		return value.trim();
	}

	protected String getConsumerKey() {
		return getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_id);
	}

	protected String getConsumerSecret() {
		return getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_secret);
	}
	
	protected String getEndpointURL(String providerNumber) {
		OneIdSession oneIdSession = oneIdSessionDao.find(providerNumber);
		return oneIdSession != null && oneIdSession.getUrlFromToolbar(ToolbarKeys.FHIR_ISS.key) != null
				? oneIdSession.getUrlFromToolbar(ToolbarKeys.FHIR_ISS.key)
				: "";
	}
	
	public Response doGet(LoggedInInfo loggedInInfo, WebClient wc, AuditInfo auditInfo) throws TokenExpiredException {
		OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
		// Refresh the access token if it has expired (throws when the refresh token is dead too).
		OneIDTokenUtils.verifyAccessTokenIsValid(loggedInInfo, oneIdGatewayData);
		String consumerKey = getConsumerKey();
		String consumerSecret = getConsumerSecret();
		String accessToken = oneIdGatewayData.getAccessToken();

		Integer demographicNo = null;
		String externalSystem = null;
		String transactionType = null;
		if(auditInfo != null) {
			demographicNo = auditInfo.getDemographicNo();
			externalSystem = auditInfo.getExternalSystem();
			transactionType = auditInfo.getTransactionType();
		}

		String requestId = newRequestId();
		OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, demographicNo, externalSystem, transactionType);
		omdGatewayTransactionLog.setDataSent(wc.getCurrentURI().toASCIIString());
		omdGatewayTransactionLog.setxGtwyClientId(consumerKey);
		omdGatewayTransactionLog.setxRequestId(requestId);
		transactionLogDao.persist(omdGatewayTransactionLog);

		Response response2;
		try {
			response2 = wc.header("Authorization", "Bearer " + accessToken).header("X-Gtwy-Client-Id", consumerKey).header("X-Gtwy-Client-Secret", consumerSecret).header("X-Request-Id", requestId).get();
			completeLog(omdGatewayTransactionLog,response2);
			transactionLogDao.merge(omdGatewayTransactionLog);
		}catch(Exception e) {
			logger.error("OMD Gateway GET failed\n" + stackTraceWithoutMessages(e));
			// The call threw before any response arrived, so completeLog never ran and the outcome
			// is set here instead. A row left with a null success reads as one whose call is still
			// in flight, and one with no end time reads as one that never finished. The end is when
			// the failure was handled, because no response ever came.
			omdGatewayTransactionLog.setSuccess(Boolean.FALSE);
			omdGatewayTransactionLog.setError(stackTraceWithoutMessages(e));
			omdGatewayTransactionLog.setEnded(new Date());
			transactionLogDao.merge(omdGatewayTransactionLog);
			throw(e);
		}
		return response2;
	}

	/**
	 * Builds the launch URL for the consent Viewlet: sets the consent target in CMS context (with
	 * the bounded retry), takes the PCOI service address resolved from the ONE ID toolbar, and
	 * appends the launch topic, FHIR issuer and authorization reference. One transaction-log row is
	 * written per launch.
	 *
	 * @param loggedInInfo  LoggedInInfo the acting provider session
	 * @param demographicNo int the patient the Viewlet is launched for
	 * @param target        String the consent target the override applies to
	 * @param uniqueToken   String correlation token recorded on the transaction-log row
	 * @return String the consent Viewlet launch URL
	 * @throws Exception CMSException when the context is not acknowledged or the toolbar carries no
	 *                   PCOI address
	 */
	public String getConsentViewletURL(LoggedInInfo loggedInInfo, int demographicNo, String target,String uniqueToken) throws Exception {
		// Held across both steps: the URL names the hub topic of the context set on the line above,
		// and a second request moving that context in between would launch on another patient.
		synchronized (CMSManager.contextLock(loggedInInfo)) {
			setContextWithRetry(() -> CMSManager.consentTargetChange(loggedInInfo, demographicNo, target));
			OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
			String pcoiUrl = oneIdGatewayData.getPcoiUrl();
			if (pcoiUrl == null || pcoiUrl.trim().isEmpty()) {
				throw new CMSException("No consent service address was found in the ONE ID toolbar. Check the PCOI Key setting names a registered Viewlet.");
			}
			String url = pcoiUrl+"?launch="+oneIdGatewayData.getHubTopic()+"&iss="+oneIdGatewayData.getFhirIss()+"&inheritanceID="+oneIdGatewayData.getAuthorizationId();
			OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, demographicNo, "PCOI", "consentViewletLaunch");
			omdGatewayTransactionLog.setDataSent(url);
			omdGatewayTransactionLog.setxCorrelationId(uniqueToken);
			// No outcome is claimed here. All that has happened is that a URL was composed: the window
			// may be blocked, or opened and never answer. The viewletResult row carries what the EHR
			// service actually reported, tied to this one by the correlation id.
			persistCompleted(omdGatewayTransactionLog);
			return url;
		}
	}

	/**
	 * Builds the launch URL for a configured Viewlet: puts the patient in CMS context (with the
	 * bounded retry), resolves the Viewlet's service address from the ONE ID toolbar by its key,
	 * and appends the launch topic, FHIR issuer and authorization reference. One transaction-log
	 * row is written per launch.
	 *
	 * @param loggedInInfo  LoggedInInfo the acting provider session
	 * @param demographicNo int the patient the Viewlet is launched for
	 * @param viewletKey    String the Viewlet's toolbar key
	 * @param uniqueToken   String correlation token recorded on the transaction-log row
	 * @return String the Viewlet launch URL
	 * @throws Exception CMSException when the context is not acknowledged or the toolbar carries no
	 *                   address for the key
	 */
	public String getViewletLaunchURL(LoggedInInfo loggedInInfo, int demographicNo, String viewletKey, String uniqueToken) throws Exception {
		// Held across both steps: the URL names the hub topic of the context set on the line above,
		// and a second request moving that context in between would launch on another patient.
		synchronized (CMSManager.contextLock(loggedInInfo)) {
			setContextWithRetry(() -> CMSManager.patientChange(loggedInInfo, demographicNo));
			OneIdSession oneIdSession = oneIdSessionDao.find(loggedInInfo.getLoggedInProviderNo());
			String serviceUrl = oneIdSession == null ? null : oneIdSession.getUrlFromToolbar(viewletKey);
			if (serviceUrl == null || serviceUrl.trim().isEmpty()) {
				throw new CMSException("No service address was found in the ONE ID toolbar for key " + viewletKey + ".");
			}
			OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
			String url = serviceUrl+"?launch="+oneIdGatewayData.getHubTopic()+"&iss="+oneIdGatewayData.getFhirIss()+"&inheritanceID="+oneIdGatewayData.getAuthorizationId();
			OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, demographicNo, viewletKey, "viewletLaunch");
			omdGatewayTransactionLog.setDataSent(url);
			omdGatewayTransactionLog.setxCorrelationId(uniqueToken);
			// No outcome is claimed here. All that has happened is that a URL was composed: the window
			// may be blocked, or opened and never answer. The viewletResult row carries what the EHR
			// service actually reported, tied to this one by the correlation id.
			persistCompleted(omdGatewayTransactionLog);
			return url;
		}
	}

	private static final int MAX_SET_CONTEXT_ATTEMPTS = 3;

	private interface ContextCall {
		void run() throws Exception;
	}

	/**
	 * Runs a CMS set-context call, retrying a bounded number of times when the context is not
	 * acknowledged: either the CMS returns a non-2xx ({@link CMSException}) or no response comes
	 * back at all ({@link ProcessingException}, e.g. a connection or read timeout). Once the
	 * attempts are exhausted the last failure is propagated so the caller can surface it and leave a
	 * re-attempt available. A missing UAO or any other error propagates immediately.
	 *
	 * @param contextCall ContextCall the set-context call to run
	 * @throws Exception the last failure when the context is not acknowledged after the bounded attempts
	 */
	private void setContextWithRetry(ContextCall contextCall) throws Exception {
		for (int attempt = 1; attempt <= MAX_SET_CONTEXT_ATTEMPTS; attempt++) {
			try {
				contextCall.run();
				return;
			} catch (CMSException | ProcessingException e) {
				logger.warn("CMS set-context not acknowledged (attempt {} of {})", attempt, MAX_SET_CONTEXT_ATTEMPTS);
				if (attempt == MAX_SET_CONTEXT_ATTEMPTS) {
					throw e;
				}
			}
		}
	}
	
	public Response doPost(LoggedInInfo loggedInInfo, WebClient wc, Event fhirCastEvent) throws Exception {
		OneIdGatewayData gatewayData = loggedInInfo.getOneIdGatewayData();
		// A provider with no gateway data has not signed in to ONE ID. That needs a sign-in, not an
		// authority, so it is raised as the same condition a dead session raises and reaches the
		// same prompt. Refreshes the access token if it has expired, and throws when the refresh
		// token is dead too.
		OneIDTokenUtils.verifyAccessTokenIsValid(loggedInInfo, gatewayData);
		// Context submission must carry the acting authority; block it when no UAO is selected.
		// A sign-out is the exception: it withdraws context rather than submitting any, and a
		// provider who cleared their authority before signing out would otherwise be refused here
		// and leave their session standing at the far end.
		boolean withdrawingContext = fhirCastEvent != null
				&& CMS_USER_LOGOUT_EVENT.equals(fhirCastEvent.getHubEvent());
		if (!withdrawingContext
				&& (gatewayData.getUao() == null || gatewayData.getUao().trim().isEmpty())) {
			throw new IllegalStateException("A ONE ID Under Authority Of (UAO) value must be selected before submitting context to the gateway.");
		}
		String consumerKey = getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_id);
		String consumerSecret =getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_secret);
		String accessToken = gatewayData.getAccessToken();
		Integer demographicNo = null;
		String externalSystem = null;
		String transactionType = null;
		if(fhirCastEvent != null) {
			externalSystem = CMS_EXTERNAL_SYSTEM;
			transactionType = fhirCastEvent.getHubEvent();
		}
		String requestId = newRequestId();
		OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, demographicNo, externalSystem, transactionType);
		omdGatewayTransactionLog.setDataSent(fhirCastEvent.getFhirCastEvent());
		omdGatewayTransactionLog.setxGtwyClientId(consumerKey);
		omdGatewayTransactionLog.setxRequestId(requestId);
		transactionLogDao.persist(omdGatewayTransactionLog);
		Response response2 = null;
		try {
			response2 = wc.header("Authorization", "Bearer " + accessToken).header("X-Gtwy-Client-Id", consumerKey)
				.header("X-Gtwy-Client-Secret", consumerSecret).header("X-Request-Id", requestId)
				.header("X-Correlation-Id", fhirCastEvent.getId()).header("X-LobTxId", fhirCastEvent.getId())
				.header("Content-Type", "application/json").post(fhirCastEvent.getFhirCastEvent());
		completeLog(omdGatewayTransactionLog,response2);
		transactionLogDao.merge(omdGatewayTransactionLog);
		}catch(Exception e) {
			logger.error("OMD Gateway POST failed\n" + stackTraceWithoutMessages(e));
			// The call threw before any response arrived, so completeLog never ran and the outcome
			// is set here instead. A row left with a null success reads as one whose call is still
			// in flight, and one with no end time reads as one that never finished. The end is when
			// the failure was handled, because no response ever came.
			omdGatewayTransactionLog.setSuccess(Boolean.FALSE);
			omdGatewayTransactionLog.setError(stackTraceWithoutMessages(e));
			omdGatewayTransactionLog.setEnded(new Date());
			transactionLogDao.merge(omdGatewayTransactionLog);
			throw(e);
		}
		return response2;
	}

	public Response getTokens(LoggedInInfo loggedInInfo,String code,String clientId, String codeVerifier,String jwt)  {
		String externalSystem = "OIDC";
		String transactionType = "TOKENS";
		String tokenUrl = getPreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_access_token);
		String callbackUrl = resolveBaseUrl()
				+ requirePreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_callback, "Callback Path");

		String requestId = newRequestId();
		OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, null, externalSystem, transactionType);
		omdGatewayTransactionLog.setDataSent(null);
		omdGatewayTransactionLog.setxRequestId(requestId);
		transactionLogDao.persist(omdGatewayTransactionLog);
		Response response2 = null;
		try {
			WebClient wc = WebClient.create(tokenUrl);
			// The authorization code, the PKCE verifier and the signed client assertion are
			// credentials, so they go in the request body. A query string is written to the access
			// log at both ends and kept by any proxy in between.
			Form form = new Form();
			form.param("grant_type", "authorization_code");
			form.param("client_assertion_type", CLIENT_ASSERTION_TYPE);
			form.param("code", code);
			form.param("redirect_uri", callbackUrl);
			form.param("client_id", clientId);
			form.param("code_verifier", codeVerifier);
			form.param("client_assertion", jwt);

			response2 = wc.header("X-Request-Id", requestId).form(form);


		completeLog(omdGatewayTransactionLog,response2,false);
		transactionLogDao.merge(omdGatewayTransactionLog);
		}catch(Exception e) {
			logger.error("ONE ID token request failed\n" + stackTraceWithoutMessages(e));
			// The call threw before any response arrived, so completeLog never ran and the outcome
			// is set here instead. A row left with a null success reads as one whose call is still
			// in flight, and one with no end time reads as one that never finished. The end is when
			// the failure was handled, because no response ever came.
			omdGatewayTransactionLog.setSuccess(Boolean.FALSE);
			omdGatewayTransactionLog.setError(stackTraceWithoutMessages(e));
			omdGatewayTransactionLog.setEnded(new Date());
			transactionLogDao.merge(omdGatewayTransactionLog);
			throw(e);
		}
		return response2;
	}

	/**
	 * Builds the private_key_jwt client assertion used to authenticate to the token endpoint,
	 * signed with the configured keystore key.
	 *
	 * @return String the signed client-assertion JWT
	 * @throws Exception when the keystore cannot be read or holds no usable private key
	 */
	protected String buildClientAssertion() throws Exception {
		String clientId = getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_id);
		String audURL = getPreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_audience);
		String alias = getPreferenceValue(SystemPreferences.ONEID_KEYS.keystore_alias);
		String keystoreLocation = getPreferenceValue(SystemPreferences.ONEID_KEYS.keystore_path);
		String keystorePassword = getPreferenceValue(SystemPreferences.ONEID_KEYS.keystore_password);

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 10);
		Date expiryDate = cal.getTime();

		KeyStore keystore = loadKeystore(keystoreLocation, keystorePassword);
		Key key = keystore.getKey(alias, keystorePassword.toCharArray());
		if (!(key instanceof PrivateKey)) {
			throw new IllegalStateException("Keystore alias does not hold a private key");
		}
		Certificate cert = keystore.getCertificate(alias);
		return JWT.create().withSubject(clientId).withAudience(audURL).withExpiresAt(expiryDate).withIssuer(clientId)
				.sign(Algorithm.RSA256((RSAPublicKey) cert.getPublicKey(), (RSAPrivateKey) key));
	}

	/**
	 * Reads the configured keystore, taking its type from the file's extension.
	 *
	 * <p>Every caller went its own way about this: the TLS socket factory read the file as JKS,
	 * while the client assertion read it as the JDK default, which is PKCS12 from Java 9 on. A .p12
	 * uploaded on the settings screen therefore signed assertions and failed the TLS handshake, and
	 * a .jks did the reverse. One reader means the file is read the same way wherever it is used.
	 *
	 * @param keystorePath String where the keystore is stored
	 * @param keystorePassword String the password it was written with
	 * @return KeyStore the loaded keystore
	 * @throws Exception when the file cannot be read or does not open with that password
	 */
	private static KeyStore loadKeystore(String keystorePath, String keystorePassword) throws Exception {
		String lowerCased = keystorePath == null ? "" : keystorePath.toLowerCase(Locale.ROOT);
		String type = (lowerCased.endsWith(".p12") || lowerCased.endsWith(".pfx")) ? "PKCS12" : "JKS";
		KeyStore keystore = KeyStore.getInstance(type);
		char[] password = keystorePassword == null ? null : keystorePassword.toCharArray();
		try (FileInputStream keystoreStream = new FileInputStream(keystorePath)) {
			keystore.load(keystoreStream, password);
		}
		return keystore;
	}

	/**
	 * Exchanges an authorization code for tokens using PKCE and a private_key_jwt client assertion.
	 *
	 * @param loggedInInfo LoggedInInfo the current session context
	 * @param code String the authorization code returned to the callback
	 * @param codeVerifier String the PKCE code verifier generated at login
	 * @return Response the raw token-endpoint response
	 * @throws Exception when the client assertion cannot be built or the call fails
	 */
	public Response exchangeCodeForTokens(LoggedInInfo loggedInInfo, String code, String codeVerifier) throws Exception {
		String clientId = getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_id);
		return getTokens(loggedInInfo, code, clientId, codeVerifier, buildClientAssertion());
	}

	/**
	 * Builds the OAuth2 authorize URL to redirect the browser to, including the requested scope,
	 * the PKCE code challenge, the state, and the nonce.
	 *
	 * @param oneIdGatewayData OneIdGatewayData carries the requested scope and profile
	 * @param state String the anti-forgery state stored in the session
	 * @param nonce String the nonce stored in the session for id-token validation
	 * @param verifier String the PKCE code verifier stored in the session
	 * @return String the fully-built authorize URL
	 * @throws Exception when the PKCE challenge cannot be generated
	 */
	public String buildAuthorizeUrl(OneIdGatewayData oneIdGatewayData, String state, String nonce, String verifier) throws Exception {
		String authorizeUrl = getPreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_authorize);
		String callbackUrl = resolveBaseUrl()
				+ requirePreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_callback, "Callback Path");
		String clientId = getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_id);
		String aud = getPreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_audience);
		String challenge = PKCEUtils.generateChallengeS256(verifier);

		UriBuilder uriBuilder = UriBuilder.fromUri(authorizeUrl)
				.queryParam("response_type", "code")
				.queryParam("scope", oneIdGatewayData.getScope())
				.queryParam("code_challenge_method", "S256")
				.queryParam("code_challenge", challenge)
				.queryParam("redirect_uri", callbackUrl)
				.queryParam("client_id", clientId)
				.queryParam("state", state)
				.queryParam("nonce", nonce);
		if (oneIdGatewayData.get_profile() != null && !oneIdGatewayData.get_profile().isEmpty()) {
			uriBuilder.queryParam("_profile", oneIdGatewayData.get_profile());
		}
		if (aud != null && !aud.isEmpty()) {
			uriBuilder.queryParam("aud", aud);
		}
		if (oneIdGatewayData.getUao() != null) {
			uriBuilder.queryParam("uao", oneIdGatewayData.getUao());
		}
		return uriBuilder.build().toString();
	}

	public String generateVerifier() {
	    byte[] randomBytes = new byte[32];
	    new SecureRandom().nextBytes(randomBytes);
	    return PKCEUtils.encodeBase64NoPadding(randomBytes);
	}
	
	/**
	 * One lock per provider, so two of their requests cannot refresh against the same refresh token
	 * at the same time. Kept for the life of the application: there is one entry per provider who
	 * has refreshed, which the staff list bounds.
	 */
	private static final ConcurrentMap<String, Object> refreshLocks = new ConcurrentHashMap<String, Object>();

	private static Object refreshLock(String providerNo) {
		return refreshLocks.computeIfAbsent(providerNo, key -> new Object());
	}

	/**
	 * Exchanges the provider's refresh token for a new access token, one request at a time.
	 *
	 * <p>The gateway rotates the refresh token, so two requests that both found the access token
	 * expired would send the same one twice. The second send is a reuse of a token the gateway has
	 * already replaced: it refuses that call, and may withdraw the whole grant over it, which signs
	 * the provider out of every tab they have open. The second request waits here instead and then
	 * finds the token the first one fetched.
	 *
	 * <p>The wait is per provider, so one provider's slow refresh does not hold up anyone else.
	 *
	 * @param loggedInInfo LoggedInInfo the acting provider session
	 * @param oneIdGatewayData OneIdGatewayData the gateway data whose tokens are replaced
	 * @throws TokenExpiredException when the gateway refuses the refresh
	 */
	public void refreshToken(LoggedInInfo loggedInInfo,OneIdGatewayData oneIdGatewayData) throws TokenExpiredException {
		String providerNo = loggedInInfo == null ? null : loggedInInfo.getLoggedInProviderNo();
		if (providerNo == null) {
			// Nothing to serialize on, and no stored session to read a newer token back from.
			exchangeRefreshToken(loggedInInfo, oneIdGatewayData);
			return;
		}
		synchronized (refreshLock(providerNo)) {
			if (adoptStoredTokens(providerNo, oneIdGatewayData)) {
				return;
			}
			exchangeRefreshToken(loggedInInfo, oneIdGatewayData);
		}
	}

	/**
	 * Takes the tokens off the provider's stored ONE ID session when they are newer than the ones in
	 * hand, and says whether that leaves a usable access token.
	 *
	 * <p>Runs on entering the refresh lock, to find out whether the request that just left it has
	 * already done the work. Two requests usually share one OneIdGatewayData, so the first one's new
	 * access token is already on the object the second is holding. They hold separate copies when the
	 * session filter rebuilt one of them, and then the stored row is the only place the new tokens
	 * are.
	 *
	 * <p>The read sees the other request's write because the DAO carries its own transaction and
	 * nothing here holds an outer one: the lookup opens a persistence context, reads, and commits,
	 * so it goes to the database rather than answering from one held open across the request.
	 *
	 * @param providerNo String the acting provider
	 * @param oneIdGatewayData OneIdGatewayData the gateway data to bring up to date
	 * @return boolean true when the tokens now in hand are good and no exchange is needed
	 */
	private boolean adoptStoredTokens(String providerNo, OneIdGatewayData oneIdGatewayData) {
		try {
			if (!oneIdGatewayData.isAccessTokenExpired()) {
				return true;
			}
			OneIdSession stored = oneIdSessionDao.find(providerNo);
			if (stored == null) {
				return false;
			}
			String storedAccessToken = stored.getAccessToken();
			if (storedAccessToken == null || storedAccessToken.isEmpty()
					|| storedAccessToken.equals(oneIdGatewayData.getAccessTokenStr())) {
				return false;
			}
			oneIdGatewayData.processAccessToken(storedAccessToken);
			String storedRefreshToken = stored.getRefreshToken();
			if (storedRefreshToken != null && !storedRefreshToken.isEmpty()) {
				oneIdGatewayData.setRefreshTokenStr(storedRefreshToken);
				oneIdGatewayData.processRefreshToken(storedRefreshToken);
			}
			return !oneIdGatewayData.isAccessTokenExpired();
		} catch (Exception e) {
			// A stored session that cannot be read or decoded leaves the exchange to go ahead, which
			// is what would have happened without this check. Class name only: a decode failure
			// quotes the token it choked on.
			logger.warn("Could not read the stored ONE ID tokens before refreshing ("
					+ e.getClass().getSimpleName() + ")");
			return false;
		}
	}

	private void exchangeRefreshToken(LoggedInInfo loggedInInfo,OneIdGatewayData oneIdGatewayData) throws TokenExpiredException {
		String tokenUrl = getPreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_access_token);
		String clientId = getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_id);

		Map<String, String> params = new HashMap<String, String>();
		params.put("grant_type", "refresh_token");
		params.put("client_id", clientId);
		params.put("client_assertion_type", CLIENT_ASSERTION_TYPE);
		params.put("refresh_token", oneIdGatewayData.getRefreshTokenString());

		// Declared out here so a call that throws before any response can still close its row.
		OMDGatewayTransactionLog omdGatewayTransactionLog = null;
		try {
			// The same assertion the code exchange sends. Built here from its own keystore read
			// before, which is how the two came to disagree about the keystore's type, and which
			// let a keystore holding no private key send a refresh with no assertion at all.
			params.put("client_assertion", buildClientAssertion());

			WebClient wc = WebClient.create(tokenUrl);
			// The refresh token and the client assertion are credentials; they go in the body
			// rather than the query string, for the reason getTokens gives.
			Form form = new Form();
			for (Entry<String, String> entry : params.entrySet()) {
				form.param(entry.getKey(), entry.getValue());
			}
			String requestId = newRequestId();
			omdGatewayTransactionLog = OmdGateway.getOMDGatewayTransactionLog(loggedInInfo, null, "Auth", "REFRESH");
			omdGatewayTransactionLog.setxRequestId(requestId);
			transactionLogDao.persist(omdGatewayTransactionLog);
			Response response2 = wc.header("X-Request-Id", requestId).form(form);
			completeLog(omdGatewayTransactionLog,response2,false);
			transactionLogDao.merge(omdGatewayTransactionLog);

			if(response2.getStatus() == 200) {
				String body = response2.readEntity(String.class);
				JSONObject respObj = new JSONObject(body);
				String accessToken = respObj.getString("access_token");
				oneIdGatewayData.processAccessToken(accessToken);
				String rotatedRefreshToken = respObj.optString("refresh_token", null);
				if (rotatedRefreshToken != null && !rotatedRefreshToken.isEmpty()) {
					oneIdGatewayData.setRefreshTokenStr(rotatedRefreshToken);
					try {
						oneIdGatewayData.processRefreshToken(rotatedRefreshToken);
					} catch (Exception e) {
						logger.warn("Could not decode the rotated refresh token; keeping the raw value");
					}
				}
				persistRefreshedTokens(loggedInInfo, accessToken, rotatedRefreshToken);

			} else {
				logger.error("ONE ID token refresh failed (HTTP " + response2.getStatus() + ")");
				throw new TokenExpiredException();
			}

		}catch(TokenExpiredException e) {
			throw e;
		}catch(Exception e) {
			logger.error("ONE ID token refresh failed\n" + stackTraceWithoutMessages(e));
			completeFailedRow(omdGatewayTransactionLog, e);
			throw new TokenExpiredException();
		}
	}

	/**
	 * Writes freshly refreshed tokens onto the provider's persisted ONE ID session. Without this
	 * the session filter restores the replaced tokens from the row on the next request, forcing a
	 * refresh on every call and losing a rotated refresh token entirely. Best-effort: the tokens
	 * already refreshed in memory must keep serving the current request even when the row cannot
	 * be written.
	 *
	 * @param loggedInInfo LoggedInInfo the current session context
	 * @param accessToken String the new access token
	 * @param refreshToken String the rotated refresh token, or null when the broker kept the old one
	 */
	private void persistRefreshedTokens(LoggedInInfo loggedInInfo, String accessToken, String refreshToken) {
		try {
			if (loggedInInfo == null || loggedInInfo.getLoggedInProviderNo() == null) {
				return;
			}
			OneIdSession oneIdSession = oneIdSessionDao.find(loggedInInfo.getLoggedInProviderNo());
			if (oneIdSession == null) {
				return;
			}
			oneIdSession.setAccessToken(accessToken);
			if (refreshToken != null && !refreshToken.isEmpty()) {
				oneIdSession.setRefreshToken(refreshToken);
			}
			oneIdSessionDao.merge(oneIdSession);
		} catch (Exception e) {
			logger.warn("Could not persist the refreshed ONE ID tokens (" + e.getClass().getSimpleName() + ")");
		}
	}

	/**
	 * Revokes the ONE ID tokens for the acting provider at the revocation endpoint. Revoking one
	 * token revokes the whole grant.
	 *
	 * @param loggedInInfo LoggedInInfo the current session context
	 * @param oneIdGatewayData OneIdGatewayData the gateway data holding the access token
	 * @throws Exception when the client assertion cannot be built or the call fails
	 */
	public void revokeToken(LoggedInInfo loggedInInfo, OneIdGatewayData oneIdGatewayData) throws Exception {
		String revokeUrl = getPreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_revocation);
		String clientId = getPreferenceValue(SystemPreferences.ONEID_KEYS.oag_client_id);
		String jwt = buildClientAssertion();
		String requestId = newRequestId();
		OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo, null, "Auth", "REVOKE");
		omdGatewayTransactionLog.setxRequestId(requestId);
		transactionLogDao.persist(omdGatewayTransactionLog);
		try {
			WebClient wc = WebClient.create(revokeUrl);
			// The token being revoked and the client assertion are credentials; they go in the
			// body rather than the query string, for the reason getTokens gives.
			Form form = new Form();
			form.param("token", oneIdGatewayData.getAccessTokenStr());
			form.param("client_id", clientId);
			form.param("client_assertion_type", CLIENT_ASSERTION_TYPE);
			form.param("client_assertion", jwt);
			Response response2 = wc.header("X-Request-Id", requestId).form(form);
			completeLog(omdGatewayTransactionLog, response2, false);
			transactionLogDao.merge(omdGatewayTransactionLog);
		} catch (Exception e) {
			// The call threw before any response arrived, so completeLog never ran and the outcome
			// is set here instead. A row left with a null success reads as one whose call is still
			// in flight, and one with no end time reads as one that never finished. The end is when
			// the failure was handled, because no response ever came.
			omdGatewayTransactionLog.setSuccess(Boolean.FALSE);
			omdGatewayTransactionLog.setError(stackTraceWithoutMessages(e));
			omdGatewayTransactionLog.setEnded(new Date());
			transactionLogDao.merge(omdGatewayTransactionLog);
			throw e;
		}
	}

	/**
	 * Builds the OpenID Connect End Session URL to redirect the browser to at logout. Users with EHR
	 * service access land on the residual-PHI notice page; others land on the login page.
	 *
	 * @param idTokenHint       String the id token that hints the session being ended, or null
	 * @param showPrivacyNotice boolean whether to land on the residual-PHI notice page
	 * @return String the End Session URL
	 */
	public String buildEndSessionUrl(String idTokenHint, boolean showPrivacyNotice) {
		String endSessionUrl = getPreferenceValue(SystemPreferences.ONEID_KEYS.endpoint_end_session);
		String landingPage = showPrivacyNotice ? "oneIdLoggedOut.jsp" : "index.jsp";
		String postLogout = resolveBaseUrl() + landingPage;
		UriBuilder uriBuilder = UriBuilder.fromUri(endSessionUrl).queryParam("post_logout_redirect_uri", postLogout);
		if (idTokenHint != null && !idTokenHint.isEmpty()) {
			uriBuilder.queryParam("id_token_hint", idTokenHint);
		}
		return uriBuilder.build().toString();
	}
}
