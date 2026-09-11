package ca.openosp.openo.integration.dhdr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;

/**
 * Unit tests for {@link OmdGateway#completeLog}, which stamps the outcome of a gateway call onto its
 * audit row (DHDR15.01: transaction status, return code, the service's message on failure, and the
 * service-generated transaction identifiers).
 *
 * <p>Two behaviours here are load-bearing rather than incidental. A failed response must be recorded
 * as a failure even when the service omits the {@code X-Request-Id} correlation header, and a
 * successful response body must be captured only when the caller opts in - the OAuth token endpoints
 * share this method and return credentials in the body.
 *
 * <p>A third is covered here because it cannot be seen from the audit row at all: the response has to
 * survive being audited. Reading a CXF entity closes it, so a response audited without being buffered
 * first is unreadable by the caller that asked for it.
 */
@Tag("unit")
@Tag("dhdr")
class OmdGatewayCompleteLogUnitTest {

  private static final String BODY = "{\"resourceType\":\"Bundle\"}";

  /**
   * Builds a mocked JAX-RS response.
   *
   * @param status int the HTTP status to report
   * @param requestId String the {@code X-Request-Id} header value, or {@code null} to omit it
   * @return Response the mocked response
   */
  private Response response(int status, String requestId) {
    Response response = mock(Response.class);
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
    when(response.getStatus()).thenReturn(status);
    when(response.getHeaders()).thenReturn(headers);
    when(response.getHeaderString("X-Request-Id")).thenReturn(requestId);
    when(response.readEntity(String.class)).thenReturn(BODY);
    return response;
  }

  @Nested
  @DisplayName("transaction status (DHDR15.01 h, i)")
  class TransactionStatus {

    @Test
    @DisplayName("should record failure when the service fails and omits the correlation header")
    void shouldRecordFailure_whenServiceFailsWithoutRequestIdHeader() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      OmdGateway.completeLog(log, response(500, null), true);

      assertThat(log.getSuccess()).isFalse();
      assertThat(log.getResultCode()).isEqualTo(500);
      assertThat(log.getError()).isEqualTo(BODY);
    }

    @Test
    @DisplayName("should record failure when the service fails and supplies the correlation header")
    void shouldRecordFailure_whenServiceFailsWithRequestIdHeader() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      OmdGateway.completeLog(log, response(404, "req-1"), true);

      assertThat(log.getSuccess()).isFalse();
      assertThat(log.getError()).isEqualTo(BODY);
      assertThat(log.getxRequestId()).isEqualTo("req-1");
    }

    @Test
    @DisplayName("should record success when the service returns a 2xx")
    void shouldRecordSuccess_whenServiceReturnsOk() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      OmdGateway.completeLog(log, response(200, "req-1"), true);

      assertThat(log.getSuccess()).isTrue();
      assertThat(log.getError()).isNull();
      assertThat(log.getEnded()).isNotNull();
    }

    @Test
    @DisplayName("should treat a redirect as a failed transaction")
    void shouldRecordFailure_whenServiceReturnsRedirect() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      OmdGateway.completeLog(log, response(302, null), true);

      assertThat(log.getSuccess()).isFalse();
    }
  }

  @Nested
  @DisplayName("response payload capture")
  class ResponsePayloadCapture {

    @Test
    @DisplayName("should capture the response body when the caller opts in")
    void shouldCaptureBody_whenCallerOptsIn() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      OmdGateway.completeLog(log, response(200, "req-1"), true);

      assertThat(log.getDataRecieved()).isEqualTo(BODY);
    }

    @Test
    @DisplayName("should not persist the response body when the caller does not opt in")
    void shouldNotPersistBody_whenCallerDoesNotOptIn() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(200, "req-1");

      OmdGateway.completeLog(log, response, false);

      // The OAuth token endpoints opt out explicitly; their bodies carry access and refresh tokens,
      // which must never be persisted to the audit table. The body is still read into a local so the
      // EHR result code can be extracted from it, so what matters here is that it is not stored.
      assertThat(log.getDataRecieved()).isNull();
    }

    @Test
    @DisplayName("should capture the response body by default, which the gateway GET and POST rely on")
    void shouldCaptureBody_whenUsingTheDefaultOverload() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      OmdGateway.completeLog(log, response(200, "req-1"));

      // doGet and doPost use the two-argument form and depend on this default to record the DHDR
      // response body (DHDR15.01). The four credential-bearing callers pass false explicitly, so
      // flipping this default would silently stop the audit table holding that evidence.
      assertThat(log.getDataRecieved()).isEqualTo(BODY);
    }

    @Test
    @DisplayName("should not capture response headers when the caller does not opt in")
    void shouldNotCaptureHeaders_whenCallerDoesNotOptIn() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      // A token-endpoint response may set a session cookie; its headers stay out of the audit row.
      OmdGateway.completeLog(log, response(200, "req-1"), false);

      assertThat(log.getHeaders()).isNull();
    }

    @Test
    @DisplayName("should capture response headers when the caller opts in")
    void shouldCaptureHeaders_whenCallerOptsIn() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      OmdGateway.completeLog(log, response(200, "req-1"), true);

      assertThat(log.getHeaders()).isNotNull();
    }

    @Test
    @DisplayName("should still record the service message when a failure is not opted in")
    void shouldRecordError_whenFailureAndCallerDidNotOptIn() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();

      OmdGateway.completeLog(log, response(400, "req-1"), false);

      assertThat(log.getError()).isEqualTo(BODY);
    }
  }

  @Nested
  @DisplayName("entity survives being audited")
  class EntityAvailability {

    @Test
    @DisplayName("should buffer the entity before reading it when the caller opts in")
    void shouldBufferEntity_beforeReadingBody() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(200, "req-1");

      OmdGateway.completeLog(log, response, true);

      // Order is the whole point: buffering after the read is too late, the stream is already closed.
      InOrder inOrder = inOrder(response);
      inOrder.verify(response).bufferEntity();
      inOrder.verify(response).readEntity(String.class);
    }

    @Test
    @DisplayName("should buffer the entity on a failure the caller did not opt in to")
    void shouldBufferEntity_whenFailureAndCallerDidNotOptIn() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(400, "req-1");

      OmdGateway.completeLog(log, response, false);

      // A failed token call is read for its error here and read again by the OAuth caller.
      verify(response).bufferEntity();
    }

    @Test
    @DisplayName("should buffer the entity even when the caller does not opt in")
    void shouldBufferEntity_whenCallerDoesNotOptIn() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(200, "req-1");

      OmdGateway.completeLog(log, response, false);

      // DHDRManager.search2 and the DHIR retrievals read the body after this returns; nothing here
      // knows whether a given caller will, so the entity is left re-readable for all of them.
      // Whether this method reads it is a separate property, asserted in ResponsePayloadCapture.
      verify(response).bufferEntity();
    }

    @Test
    @DisplayName("should still record the outcome when the entity cannot be read at all")
    void shouldRecordOutcome_whenEntityCannotBeBuffered() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(503, "req-1");
      // An entity consumed before this method saw it fails BOTH calls, not just the buffering.
      // Throwing only on bufferEntity would leave the read below to succeed, and the assertion that
      // the row survives would then hold for a reason the real failure does not supply.
      IllegalStateException consumed = new IllegalStateException("Entity is not available");
      doThrow(consumed).when(response).bufferEntity();
      when(response.readEntity(String.class)).thenThrow(consumed);

      OmdGateway.completeLog(log, response, true);

      // An audit row is owed for the call whether or not its body can be recovered (DHDR15.01):
      // the outcome fields do not depend on the body, so only the message text is lost.
      assertThat(log.getSuccess()).isFalse();
      assertThat(log.getResultCode()).isEqualTo(503);
      assertThat(log.getxRequestId()).isEqualTo("req-1");
      assertThat(log.getError()).isNull();
    }

    @Test
    @DisplayName("should record the outcome of a success whose body cannot be read")
    void shouldRecordOutcome_whenSuccessBodyCannotBeRead() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(200, "req-1");
      when(response.readEntity(String.class))
          .thenThrow(new IllegalStateException("Entity is not available"));

      OmdGateway.completeLog(log, response, true);

      // The other call site of the same read: an opted-in capture that cannot recover the payload
      // still leaves a row saying the call succeeded.
      assertThat(log.getSuccess()).isTrue();
      assertThat(log.getResultCode()).isEqualTo(200);
      assertThat(log.getDataRecieved()).isNull();
    }
  }

  @Nested
  @DisplayName("service-generated transaction identifiers (DHDR15.01 c)")
  class TransactionIdentifiers {

    @Test
    @DisplayName("should record the correlation identifiers the service supplies")
    void shouldRecordCorrelationIds_whenServiceSuppliesThem() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(200, "req-1");
      when(response.getHeaderString("X-LobTxId")).thenReturn("lob-1");
      when(response.getHeaderString("X-Correlation-Id")).thenReturn("corr-1");

      OmdGateway.completeLog(log, response, true);

      assertThat(log.getxRequestId()).isEqualTo("req-1");
      assertThat(log.getxLobTxId()).isEqualTo("lob-1");
      assertThat(log.getxCorrelationId()).isEqualTo("corr-1");
    }

    @Test
    @DisplayName("should record the lob identifier even when the request identifier is absent")
    void shouldRecordLobId_whenRequestIdIsAbsent() {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(200, null);
      when(response.getHeaderString("X-LobTxId")).thenReturn("lob-1");

      OmdGateway.completeLog(log, response, true);

      assertThat(log.getxRequestId()).isNull();
      assertThat(log.getxLobTxId()).isEqualTo("lob-1");
    }
  }

  /**
   * Resource ids the interaction carried (DHDR15.01 j, DHDR15.02 d).
   *
   * <p>The shapes here are taken from OntarioMD's own captures: a searchset {@code Bundle} whose id
   * is a lowercase dashed UUID, carrying {@code MedicationDispense} entries whose ids are bare
   * 32-character uppercase hex - no {@code urn:uuid:} prefix and no full reference.
   *
   * <p>{@code messageHeaderId} is filled from {@code Bundle.id}. DHDR15.01(j) names
   * {@code MessageHeader.id}, but a DHDR retrieval carries no {@code MessageHeader}; see
   * {@link OmdGateway#extractBundleId} for why {@code Bundle.id} is the value the requirement's own
   * cross-reference resolves to.
   */
  @Nested
  @DisplayName("resource ids the interaction carried (DHDR15.01 j, DHDR15.02 d)")
  class ResourceIds {

    private static final String DISPENSE_ONE = "5ED4D6C1668D41A0E05400144FF8F552";
    private static final String DISPENSE_TWO = "93F05B0FF74D525FE05400144FF9F54A";
    private static final String BUNDLE_ID = "e72bc3b0-2c91-40d7-bffe-b690fd65fdce";

    /**
     * Builds a searchset body carrying the given MedicationDispense ids.
     *
     * @param ids String[] the dispense ids the bundle should carry, possibly none
     * @return String the response body as the service would return it
     */
    private String searchset(String... ids) {
      StringBuilder body = new StringBuilder("{\"resourceType\":\"Bundle\",\"id\":\"")
          .append(BUNDLE_ID).append("\",\"type\":\"searchset\",\"entry\":[");
      for (int i = 0; i < ids.length; i++) {
        if (i > 0) {
          body.append(',');
        }
        body.append("{\"resource\":{\"resourceType\":\"MedicationDispense\",\"id\":\"")
            .append(ids[i]).append("\"}}");
      }
      return body.append("]}").toString();
    }

    /**
     * Stamps a 200 response carrying the given body onto a fresh audit row.
     *
     * @param body String the response body
     * @return OMDGatewayTransactionLog the completed row
     */
    private OMDGatewayTransactionLog complete(String body) {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      Response response = response(200, "req-1");
      when(response.readEntity(String.class)).thenReturn(body);
      OmdGateway.completeLog(log, response, true);
      return log;
    }

    @Test
    @DisplayName("should record the bundle id and every dispense id a search returns")
    void shouldRecordResourceIds_whenSearchReturnsDispenses() {
      OMDGatewayTransactionLog log = complete(searchset(DISPENSE_ONE, DISPENSE_TWO));

      assertThat(log.getMessageHeaderId()).isEqualTo(BUNDLE_ID);
      assertThat(log.getMedicationDispenseIds()).isEqualTo(DISPENSE_ONE + "," + DISPENSE_TWO);
    }

    @Test
    @DisplayName("should keep the order the service returned, which is the sort the search asked for")
    void shouldPreserveOrder_whenSearchReturnsDispenses() {
      OMDGatewayTransactionLog log = complete(searchset(DISPENSE_TWO, DISPENSE_ONE));

      assertThat(log.getMedicationDispenseIds()).isEqualTo(DISPENSE_TWO + "," + DISPENSE_ONE);
    }

    @Test
    @DisplayName("should record the bundle id when a search returns no dispenses")
    void shouldRecordBundleIdOnly_whenSearchReturnsNothing() {
      OMDGatewayTransactionLog log = complete(searchset());

      assertThat(log.getMessageHeaderId()).isEqualTo(BUNDLE_ID);
      assertThat(log.getMedicationDispenseIds()).isNull();
    }

    @Test
    @DisplayName("should record no resource ids when the service returns an OperationOutcome")
    void shouldRecordNoResourceIds_whenServiceReturnsOperationOutcome() {
      OMDGatewayTransactionLog log = complete(
          "{\"resourceType\":\"OperationOutcome\",\"id\":\"outcome-1\",\"issue\":[]}");

      assertThat(log.getMessageHeaderId()).isNull();
      assertThat(log.getMedicationDispenseIds()).isNull();
    }

    @Test
    @DisplayName("should record no resource ids when the body is not FHIR, as on the OAuth calls")
    void shouldRecordNoResourceIds_whenBodyIsNotFhir() {
      OMDGatewayTransactionLog log = complete("{\"access_token\":\"secret\"}");

      assertThat(log.getMessageHeaderId()).isNull();
      assertThat(log.getMedicationDispenseIds()).isNull();
    }

    @Test
    @DisplayName("should ignore entries that are not dispenses, such as an included OperationOutcome")
    void shouldIgnoreOtherEntries_whenBundleCarriesThem() {
      String body = "{\"resourceType\":\"Bundle\",\"id\":\"" + BUNDLE_ID + "\",\"type\":\"searchset\","
          + "\"entry\":[{\"resource\":{\"resourceType\":\"OperationOutcome\",\"id\":\"skip-me\"}},"
          + "{\"resource\":{\"resourceType\":\"MedicationDispense\",\"id\":\"" + DISPENSE_ONE + "\"}}]}";

      OMDGatewayTransactionLog log = complete(body);

      assertThat(log.getMedicationDispenseIds()).isEqualTo(DISPENSE_ONE);
    }

    @Test
    @DisplayName("should bound a malformed bundle id rather than fail the whole audit row")
    void shouldBoundBundleId_whenServiceReturnsAnOverlongId() {
      String overlong = "x".repeat(80);
      OMDGatewayTransactionLog log = complete(
          "{\"resourceType\":\"Bundle\",\"id\":\"" + overlong + "\",\"type\":\"searchset\"}");

      // 64 is the FHIR id primitive's own limit and the column's width, so no conformant id can
      // reach this - but a row that cannot be inserted loses the whole audit record.
      assertThat(log.getMessageHeaderId()).hasSize(64);
    }
  }
}
