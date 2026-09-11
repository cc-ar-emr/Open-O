/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p>
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */
package ca.openosp.openo.integration.oneId.web;

import ca.openosp.OscarProperties;
import ca.openosp.openo.commn.model.SystemPreferences;
import ca.openosp.openo.commn.model.SystemPreferences.ONEID_KEYS;
import ca.openosp.openo.log.LogAction;
import ca.openosp.openo.log.LogConst;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.PathValidationUtils;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.action.UploadedFilesAware;
import org.apache.struts2.dispatcher.multipart.UploadedFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin screen for the ONE ID gateway settings: client credentials, endpoints, keystore, and the
 * JWKS / issuer / public-key values. Values are stored as {@code SystemPreferences} rows. Secret
 * fields are never rendered back and are only overwritten when a new value is entered, and every
 * add/update of a value is written to the audit log.
 *
 * <p>The keystore is uploaded rather than located by a typed path: the file is stored under the
 * EHR connectivity directory and the setting holds where the EMR put it.</p>
 *
 * @since 2026-07-01
 */
public class EhrConnectivitySettingsAction extends ActionSupport implements UploadedFilesAware {

    private static final Logger logger = MiscUtils.getLogger();

    private final HttpServletRequest request = ServletActionContext.getRequest();

    private final SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);

    private static final String SEC_OBJECT = "_admin.ehrConnectivity";

    /** Keys whose value is a secret: blanked on display, only written when a new value is entered. */
    private static final List<String> SECRET_KEYS = Arrays.asList(
            ONEID_KEYS.oag_client_secret.name(),
            ONEID_KEYS.keystore_password.name());

    /**
     * Keys whose value is a boolean shown as a toggle switch. A switch is a checkbox, and a cleared
     * checkbox submits no parameter at all, so these are read as absent-means-false rather than
     * skipped like the text fields, which is what lets a flag be turned back off.
     */
    private static final List<String> FLAG_KEYS = Arrays.asList(
            ONEID_KEYS.oneid_enabled.name(),
            ONEID_KEYS.dhdr_enabled.name());

    /** The keys this screen manages, in display order. */
    private static final ONEID_KEYS[] EDITABLE_KEYS = {
            ONEID_KEYS.oneid_enabled, ONEID_KEYS.dhdr_enabled,
            ONEID_KEYS.oag_client_id, ONEID_KEYS.oag_client_secret, ONEID_KEYS.oag_public_key,
            ONEID_KEYS.keystore_path, ONEID_KEYS.keystore_alias, ONEID_KEYS.keystore_password,
            ONEID_KEYS.endpoint_authorize, ONEID_KEYS.endpoint_access_token,
            ONEID_KEYS.endpoint_callback, ONEID_KEYS.endpoint_audience,
            ONEID_KEYS.endpoint_jwks, ONEID_KEYS.oneid_issuer,
            ONEID_KEYS.endpoint_end_session, ONEID_KEYS.endpoint_revocation,
            ONEID_KEYS.pcoi_key, ONEID_KEYS.timeout, ONEID_KEYS.viewlet_timeout
    };

    /**
     * The most characters a setting can hold, matching the SystemPreferences value column. The form
     * stops anything longer being submitted, because the settings are written a row at a time: a
     * value the column refuses throws part way through the list and leaves the rest unsaved.
     */
    private static final int VALUE_MAX_LENGTH = 1000;

    /** The property naming the directory uploaded keystores are kept in, created by Startup. */
    private static final String KEYSTORE_DIR_PROPERTY = "EHR_KEYSTORE_DIR";

    /** The keystore file types the gateway can load. */
    private static final List<String> KEYSTORE_EXTENSIONS =
            Arrays.asList(".jks", ".p12", ".pfx", ".keystore");

    /** The keystore submitted on this request, or null when the form carried no file. */
    private UploadedFile keystoreUpload;

    /**
     * Takes the file submitted with the settings form. The form carries one file input, so the
     * keystore is whichever file arrives.
     *
     * @param uploadedFiles List&lt;UploadedFile&gt; the files submitted with the request
     */
    @Override
    public void withUploadedFiles(List<UploadedFile> uploadedFiles) {
        keystoreUpload = (uploadedFiles == null || uploadedFiles.isEmpty()) ? null : uploadedFiles.get(0);
    }

    @Override
    public String execute() {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), SEC_OBJECT, "r", null)) {
            throw new SecurityException("missing required sec object (" + SEC_OBJECT + ")");
        }
        loadSettingsForDisplay();
        return "success";
    }

    public String save() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        if (!securityInfoManager.hasPrivilege(loggedInInfo, SEC_OBJECT, "w", null)) {
            throw new SecurityException("missing required sec object (" + SEC_OBJECT + ")");
        }
        // Settings only change on a POST, so a crafted link or image cannot rewrite the client
        // secret, the keystore password or an endpoint. A plain GET renders the screen unchanged.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            loadSettingsForDisplay();
            return "success";
        }

        String providerNo = loggedInInfo.getLoggedInProviderNo();
        String ip = request.getRemoteAddr();

        // One save can turn up more than one problem, so they are collected rather than each
        // overwriting the last.
        List<String> problems = new ArrayList<>();

        // The keystore arrives as a file rather than a typed path. No file on the form means the
        // stored keystore stays where it is, the same as a blank secret.
        String uploadedKeystorePath = storeUploadedKeystore(problems);

        for (ONEID_KEYS key : EDITABLE_KEYS) {
            String name = key.name();
            boolean flag = FLAG_KEYS.contains(name);
            String submitted = key == ONEID_KEYS.keystore_path
                    ? uploadedKeystorePath
                    : request.getParameter(name);
            // A switch that is off sends nothing, so for a flag an absent parameter is the value
            // "false" rather than "leave it alone" - otherwise a flag could be switched on and
            // never off again.
            if (submitted == null && flag) {
                submitted = "false";
            }
            if (submitted == null) {
                continue;
            }
            submitted = submitted.trim();
            if (flag) {
                submitted = Boolean.toString("true".equalsIgnoreCase(submitted) || "on".equalsIgnoreCase(submitted));
            }
            boolean secret = SECRET_KEYS.contains(name);

            // A blank secret means "leave the stored value unchanged".
            if (secret && submitted.isEmpty()) {
                continue;
            }
            // Checked here rather than left to the column. The settings are written a row at a
            // time, so a value the column refuses throws part way through and leaves every key
            // after it unsaved. The form caps the length too, but only in the browser.
            if (submitted.length() > VALUE_MAX_LENGTH) {
                problems.add(labelFor(key) + " was not saved: it is longer than "
                        + VALUE_MAX_LENGTH + " characters.");
                continue;
            }

            SystemPreferences pref = ehrConnectivityManager.getConfig(key);
            // The column is nullable, so a row written by anything but this screen can hold null.
            String previous = (pref == null || pref.getValue() == null) ? "" : pref.getValue();
            if (previous.equals(submitted)) {
                continue;
            }

            ehrConnectivityManager.saveConfig(loggedInInfo, key, submitted);
            auditChange(providerNo, ip, name, previous, submitted, secret);
        }

        request.setAttribute("saved", Boolean.TRUE);
        if (!problems.isEmpty()) {
            request.setAttribute("problems", problems);
        }
        loadSettingsForDisplay();
        return "success";
    }

    private void loadSettingsForDisplay() {
        List<Map<String, String>> settings = new ArrayList<>();
        for (ONEID_KEYS key : EDITABLE_KEYS) {
            boolean secret = SECRET_KEYS.contains(key.name());
            SystemPreferences pref = ehrConnectivityManager.getConfig(key);
            Map<String, String> setting = new LinkedHashMap<>();
            setting.put("key", key.name());
            setting.put("label", labelFor(key));
            setting.put("type", typeFor(key, secret));
            setting.put("value", secret || pref == null ? "" : pref.getValue());
            settings.add(setting);
        }
        request.setAttribute("settings", settings);
        request.setAttribute("valueMaxLength", VALUE_MAX_LENGTH);
    }

    /**
     * How the field is rendered: a blanked password box, a multi-line box, a toggle switch, or
     * plain text.
     *
     * @param key ONEID_KEYS the setting being rendered
     * @param secret boolean whether the value is withheld from the page
     * @return String the type the JSP switches on
     */
    private String typeFor(ONEID_KEYS key, boolean secret) {
        if (secret) {
            return "secret";
        }
        if (FLAG_KEYS.contains(key.name())) {
            return "switch";
        }
        if (key == ONEID_KEYS.keystore_path) {
            return "file";
        }
        return key == ONEID_KEYS.oag_public_key ? "textarea" : "text";
    }

    private String labelFor(ONEID_KEYS key) {
        switch (key) {
            case oneid_enabled: return "Enable ONE ID sign-in";
            case dhdr_enabled: return "Enable DHDR medication viewer";
            case oag_client_id: return "OAG Client ID";
            case oag_client_secret: return "OAG Client Secret";
            case oag_public_key: return "OAG Public Key";
            case keystore_path: return "Keystore File";
            case keystore_alias: return "Keystore Alias";
            case keystore_password: return "Keystore Password";
            case endpoint_authorize: return "Authorize Endpoint";
            case endpoint_access_token: return "Token Endpoint";
            case endpoint_callback: return "Callback Path";
            case endpoint_audience: return "Audience";
            case endpoint_jwks: return "JWKS Endpoint";
            case oneid_issuer: return "Expected Issuer";
            case endpoint_end_session: return "End-Session Endpoint";
            case endpoint_revocation: return "Revocation Endpoint";
            case pcoi_key: return "Consent Viewlet Key";
            case timeout: return "Gateway Timeout (seconds)";
            case viewlet_timeout: return "Viewlet Response Timeout (seconds)";
            default: return key.name();
        }
    }

    /**
     * Stores the submitted keystore under the EHR connectivity directory and reports where it
     * landed, so the recorded path is one the EMR wrote rather than one an administrator typed.
     *
     * @return String the absolute path of the stored keystore, or null when no file was submitted
     *         or the file could not be stored
     */
    private String storeUploadedKeystore(List<String> problems) {
        if (keystoreUpload == null) {
            return null;
        }
        String originalName = keystoreUpload.getOriginalName();
        if (originalName == null || originalName.trim().isEmpty()) {
            return null;
        }
        if (!hasKeystoreExtension(originalName)) {
            problems.add("The keystore was not stored: it must be one of "
                    + String.join(", ", KEYSTORE_EXTENSIONS) + ".");
            return null;
        }
        try {
            File directory = keystoreDirectory();
            Files.createDirectories(directory.toPath());
            File source = PathValidationUtils.toFile(keystoreUpload);
            File destination = PathValidationUtils.validateUpload(source, originalName, directory);
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destination.getAbsolutePath();
        } catch (IOException e) {
            logger.error("Could not store the uploaded ONE ID keystore", e);
            problems.add("The keystore was not stored. Check the server log.");
            return null;
        } catch (SecurityException | IllegalStateException e) {
            logger.error("Rejected the uploaded ONE ID keystore", e);
            problems.add("The keystore was not stored: the file was rejected. Check the server log.");
            return null;
        }
    }

    private static boolean hasKeystoreExtension(String fileName) {
        String lowerCased = fileName.toLowerCase(Locale.ROOT);
        for (String extension : KEYSTORE_EXTENSIONS) {
            if (lowerCased.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The directory uploaded keystores are kept in, created under the document directory at
     * startup.
     *
     * @return File the keystore directory
     * @throws IllegalStateException when the directory property is not set
     */
    private static File keystoreDirectory() {
        String configured = OscarProperties.getInstance().getProperty(KEYSTORE_DIR_PROPERTY);
        if (configured == null || configured.trim().isEmpty()) {
            throw new IllegalStateException(KEYSTORE_DIR_PROPERTY + " is not configured");
        }
        return new File(configured);
    }

    private void auditChange(String providerNo, String ip, String key, String previous, String current, boolean secret) {
        String data = secret
                ? key + ": secret value changed"
                : key + ": '" + previous + "' -> '" + current + "'";
        LogAction.addLog(providerNo, LogConst.UPDATE, "ehrConnectivitySettings", key, ip, null, data);
    }
}
