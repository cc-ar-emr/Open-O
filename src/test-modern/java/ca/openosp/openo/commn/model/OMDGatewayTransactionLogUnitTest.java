package ca.openosp.openo.commn.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the bounding {@link OMDGatewayTransactionLog#setMedicationDispenseIds} applies to
 * the dispense ids an interaction returned (DHDR15.01 j).
 *
 * <p>The bounding is what keeps the column honest. Submitting more than the column holds is not an
 * error on a server running with an empty {@code sql_mode} - MySQL stores what fits, warns, and
 * leaves the last id cut in half, so the row reads as a complete record of ids that it is not.
 * Rather than let that happen the setter keeps whole ids and appends a count of the ones it left
 * out. These tests pin that contract, which had no coverage.
 *
 * <p>The bound is deliberately conservative: it counts characters, while the column's limit is in
 * bytes, so it holds for any charset the column could be declared with rather than only for the
 * ASCII that FHIR ids actually are. {@link #shouldFitTheColumn_whenIdsAreMultiByte()} is what makes
 * that guarantee explicit, and {@link #shouldCutAPageBelowTheSizeRequested()} records what it costs.
 */
@Tag("unit")
@Tag("dhdr")
class OMDGatewayTransactionLogUnitTest {

  /** The column's own limit, in bytes: it is declared TEXT. */
  private static final int COLUMN_BYTES = 65535;

  /** Width of a real DHDR dispense id - 32 characters of uppercase hex in every OMD capture. */
  private static final int REAL_ID_WIDTH = 32;

  /** Width the FHIR id primitive allows at most, which is what the bound was sized against. */
  private static final int MAX_ID_WIDTH = 64;

  /**
   * Builds a comma separated list of distinct ids, each padded to the given width.
   *
   * @param count int how many ids the list should hold
   * @param width int how many characters wide each id should be
   * @return String the ids, comma separated
   */
  private static String idList(int count, int width) {
    StringBuilder ids = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        ids.append(',');
      }
      ids.append(String.format("%0" + width + "X", i));
    }
    return ids.toString();
  }

  private static String stored(String ids) {
    OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
    log.setMedicationDispenseIds(ids);
    return log.getMedicationDispenseIds();
  }

  /** Counts the ids in a stored value, ignoring the overflow marker when one is present. */
  private static int idsIn(String storedValue) {
    String[] parts = storedValue.split(",");
    return storedValue.endsWith(" more not recorded)") ? parts.length - 1 : parts.length;
  }

  @Nested
  @DisplayName("lists the bound leaves alone")
  class WithinBound {

    @Test
    @DisplayName("should store nothing when the interaction returned no dispenses")
    void shouldStoreNothing_whenNoDispensesReturned() {
      assertThat(stored(null)).isNull();
    }

    @Test
    @DisplayName("should store the list unchanged when it fits")
    void shouldStoreUnchanged_whenListFits() {
      String ids = idList(16, REAL_ID_WIDTH);

      assertThat(stored(ids)).isEqualTo(ids);
    }

    /**
     * The headroom the bound was chosen for: 246 ids at the widest an id may be. Every real DHDR id
     * is half that width, so this is the pessimistic case rather than the expected one.
     */
    @Test
    @DisplayName("should store 246 ids of the maximum width unchanged")
    void shouldStoreUnchanged_when246MaximumWidthIdsSupplied() {
      String ids = idList(246, MAX_ID_WIDTH);

      String result = stored(ids);

      assertThat(result).isEqualTo(ids);
      assertThat(result).doesNotContain("not recorded");
    }
  }

  @Nested
  @DisplayName("lists the bound cuts back")
  class BeyondBound {

    @Test
    @DisplayName("should keep whole ids and say how many were left out when the list overflows")
    void shouldKeepWholeIdsAndMarkTheRest_whenListOverflows() {
      String ids = idList(2000, REAL_ID_WIDTH);

      String result = stored(ids);

      assertThat(result).endsWith(" more not recorded)");
      int kept = idsIn(result);
      // no id is stored as a fragment, which is the whole point of cutting back here
      for (String id : result.split(",", kept + 1)) {
        if (!id.startsWith("(")) {
          assertThat(id).hasSize(REAL_ID_WIDTH);
        }
      }
      assertThat(result).contains("(" + (2000 - kept) + " more not recorded)");
    }

    /**
     * Records what the bound costs rather than asserting it is right. The search asks the service
     * for {@code _count=1000} ({@code DHDRManager}), so a page can carry more ids than one row will
     * hold - the surplus is declared by the marker rather than lost silently. Whether a page that
     * large ever arrives is the service's decision: the DHDR CapabilityStatement defaults paging to
     * 50 records with a configurable maximum, and the largest seen in any OMD capture is 16.
     *
     * <p>If the bound is ever raised, this test fails and should be updated deliberately.
     */
    @Test
    @DisplayName("should cut a full requested page back to what the column holds")
    void shouldCutAPageBelowTheSizeRequested() {
      String result = stored(idList(1000, REAL_ID_WIDTH));

      assertThat(idsIn(result)).isEqualTo(483);
      assertThat(result).endsWith("(517 more not recorded)");
    }

    /**
     * Why the bound counts characters against a column measured in bytes: at the widest encoding the
     * column's charset allows, a list at the bound is still comfortably inside the column. The cost
     * of that guarantee is the previous test.
     */
    @Test
    @DisplayName("should fit the column even when every character is multi-byte")
    void shouldFitTheColumn_whenIdsAreMultiByte() {
      StringBuilder ids = new StringBuilder();
      for (int i = 0; i < 484; i++) {
        if (i > 0) {
          ids.append(',');
        }
        for (int c = 0; c < REAL_ID_WIDTH; c++) {
          // a three-byte character, the widest the column's utf8 charset stores
          ids.append('中');
        }
      }

      String result = stored(ids.toString());

      assertThat(result.getBytes(StandardCharsets.UTF_8).length).isLessThan(COLUMN_BYTES);
    }
  }
}
