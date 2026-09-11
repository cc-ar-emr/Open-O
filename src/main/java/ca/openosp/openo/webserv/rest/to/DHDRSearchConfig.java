package ca.openosp.openo.webserv.rest.to;

/**
 * Search configuration payload for a DHDR (Digital Health Drug Repository) dispense query.
 *
 * <p>Carries the optional bounding dates and pagination cursor supplied by the DHDR viewer when
 * requesting a patient's dispensed-medication history. All fields are optional; a {@code null}
 * date bound means "unbounded" on that side and a {@code null} {@code searchId}/{@code pageId}
 * requests the first page of a fresh search.</p>
 *
 * <p>The bounds are {@code yyyy-MM-dd} strings rather than {@link java.util.Date}. They are
 * calendar dates a clinician chose, not instants: they carry no time and belong to no time zone.
 * Binding them to {@code Date} made Jackson read a date-only string as UTC midnight, which the
 * outbound formatter then rendered in the server's own zone - moving both bounds a day earlier in
 * any negative-offset zone and dropping the dispenses on the end date the clinician asked for.
 * Keeping them as strings means the value the viewer sends is the value DHDR is queried with.</p>
 *
 * @since 2026-07-02
 */
public class DHDRSearchConfig {
  private String startDate = null;
  private String endDate = null;
  private String searchId = null;
  private String pageId = null;

  /**
   * @return String the inclusive lower bound as {@code yyyy-MM-dd}, or null for unbounded
   */
  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  /**
   * @return String the inclusive upper bound as {@code yyyy-MM-dd}, or null to default to today
   */
  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  /**
   * @return String the search id carried back from a previous response, which pages through that
   *     same result set, or null to start a fresh search
   */
  public String getSearchId() {
    return searchId;
  }

  public void setSearchId(String searchId) {
    this.searchId = searchId;
  }

  /**
   * @return String the page to fetch within the search named by {@link #getSearchId()}, or null for
   *     the first page. Held as a String because that is how the viewer sends it; a value that is
   *     not a whole number is read as null rather than rejected
   */
  public String getPageId() {
    return pageId;
  }

  public void setPageId(String pageId) {
    this.pageId = pageId;
  }
}
