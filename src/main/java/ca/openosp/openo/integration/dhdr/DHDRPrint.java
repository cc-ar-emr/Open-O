package ca.openosp.openo.integration.dhdr;
/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License. This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * <p>This software was written for the Department of Family Medicine McMaster University Hamilton
 * Ontario, Canada
 */

import ca.openosp.OscarProperties;
import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.commn.printing.FontSettings;
import ca.openosp.openo.commn.printing.PdfWriterFactory;
import ca.openosp.openo.managers.DemographicManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.logging.log4j.Logger;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.awt.*;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class DHDRPrint {

  /**
   * The DHDR disclaimer, printed on each page per DHDR13.01.g, which refers to DHDR03.03 for the
   * message. Quoted verbatim from DHDR03.03 with its [URL] placeholder resolved, so it must stay
   * identical to the copy rendered in dhdr/index.jsp.
   */
  private static final String DHDR_DISCLAIMER =
      "Warning: Limited to Drug and Pharmacy Service Information available in the Digital Health Drug"
          + " Repository (DHDR) EHR Service. To ensure a Best Possible Medication History,"
          + " please review this information with the patient/family and use other available sources"
          + " of medication information in addition to the DHDR EHR Service. For more details on the"
          + " information available in the DHDR EHR Service, please click"
          + " https://forms.mgcs.gov.on.ca/en/dataset/014-5056-87.";

  private static final Logger logger = MiscUtils.getLogger();

  /**
   * DHDR consumer-profile min>=1 dispense fields that come from the MedicationDispense itself (not the
   * contained Medication, so their absence is a real upstream-conformance gap rather than the external-
   * reference case in the viewer). Absence is logged for audit; the cell still renders as-is.
   */
  private static final String[] MANDATORY_DISPENSE_FIELDS = {
    "whenPrepared", "dose", "frequency", "dispensedQuantity", "estimatedDaysSupply"
  };

  DemographicManager demographicManager = SpringUtils.getBean(DemographicManager.class);
  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd  'at' HH:mm:ss z");

  public void printDetail(
      LoggedInInfo loggedInInfo,
      Integer demographicNo,
      OutputStream outputStream,
      JSONObject jsonOb)
      throws Exception {
    Demographic demo = demographicManager.getDemographic(loggedInInfo, demographicNo);

    if (demo == null) throw new DocumentException();

    // Portrait, unlike the two grid views - this one is a label/value list, not a wide table.
    Document document =
        openDhdrDocument(loggedInInfo, demo, outputStream, "DHDR Detailed", jsonOb, false);

    document.add(Chunk.NEWLINE);

    /////// table
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100.0f);

    JSONObject med = jsonOb.optJSONObject("med");

    if (med != null) {
      auditMandatoryDispenseFields(med, 0);

      table.addCell(getHeaderCell("Dispensed Date"));
      table.addCell(getHeaderCell(displayDate(med.optString("whenPrepared")))); // Dispensed Date

      table.setHeaderRows(1);

      // DHDR06.01(a) inherits every element of the Summary View, which includes DHDR04.01(b) pickup
      // date. The detail screen shows it directly under Dispensed Date; the print omitted it, so the
      // one view that is meant to be the most complete was the only one missing it. Reads pickUpDate,
      // the property the viewer assigns for drug events (the service tables read whenHandedOver; both
      // come from the same MedicationDispense.whenHandedOver).
      table.addCell(getHeaderCell("Pickup Date"));
      table.addCell(getItemCell(displayDate(med.optString("pickUpDate"))));

      // DHDR13.01(b) / BP14: the Detailed view is scoped to one event, so the DHDR-side identity that
      // belongs on this page is that event's own contained Patient - which is what the viewer now sends
      // as dhdrPatient - together with the per-field match flags the screen shows. One HCN search can
      // legitimately return several recorded identities; without this block the paper carried only the
      // page header and lost the mismatch marking that is the whole point of BP14 clause 2.
      addEventPatientRows(table, jsonOb.optJSONObject("dhdrPatient"));

      table.addCell(getHeaderCell("Generic"));
      table.addCell(getItemCell(med.optString("genericName"))); // Generic

      JSONObject brandObj = med.optJSONObject("brandName");
      table.addCell(getHeaderCell("Brand"));
      table.addCell(getItemCell(brandObj != null ? brandObj.optString("display") : "")); // Brand

      table.addCell(getHeaderCell("DIN/PIN"));
      table.addCell(getItemCell(brandObj != null ? brandObj.optString("code") : "")); // DIN/PIN

      // The parse stores these as plain display strings, not objects - read them as strings, as
      // populateSummaryDrugData does.
      String ahfsClass = med.optString("ahfsClass");
      if (!ahfsClass.isEmpty()) {
        table.addCell(getHeaderCell("Therapeutic Class"));
        table.addCell(getItemCell(ahfsClass));
      }

      String ahfsSubClass = med.optString("ahfsSubClass");
      if (!ahfsSubClass.isEmpty()) {
        table.addCell(getHeaderCell("Therapeutic Sub-Class"));
        table.addCell(getItemCell(ahfsSubClass));
      }

      table.addCell(getHeaderCell("Rx Number"));
      table.addCell(getItemCell(med.optString("rxNumber"))); // Brand

      table.addCell(getHeaderCell("Medical Condition/Reason for Use"));

      StringBuilder reasonCodesStr = new StringBuilder();
      JSONArray reasonCodes = med.optJSONArray("reasonCode");
      if (reasonCodes != null) {
        for (int i = 0; i < reasonCodes.length(); i++) {
          JSONObject jsonObject = reasonCodes.optJSONObject(i);
          if (jsonObject == null) {
            continue;
          }
          // DHDR06.01(c): reasonCode is a CodeableConcept, so the code and display sit on its
          // codings rather than on the concept. Falls back to the concept's plain text where it
          // carries no coding, so a reason that is only written out still prints.
          JSONArray codings = jsonObject.optJSONArray("coding");
          for (int c = 0; codings != null && c < codings.length(); c++) {
            JSONObject coding = codings.optJSONObject(c);
            if (coding == null) {
              continue;
            }
            if (reasonCodesStr.length() > 0) {
              reasonCodesStr.append("; ");
            }
            reasonCodesStr.append(coding.opt("code") + " -- " + coding.opt("display"));
          }
          String text = jsonObject.optString("text", "");
          if ((codings == null || codings.length() == 0) && !text.isEmpty()) {
            if (reasonCodesStr.length() > 0) {
              reasonCodesStr.append("; ");
            }
            reasonCodesStr.append(text);
          }
        }
      }
      table.addCell(getItemCell(reasonCodesStr.toString()));

      table.addCell(getHeaderCell("Strength"));
      table.addCell(getItemCell(med.optString("dispensedDrugStrength")));
      table.addCell(getHeaderCell("Dosage Form"));
      table.addCell(getItemCell(med.optString("drugDosageForm")));
      // The viewer splits dose into a value and a separate unit, and holds frequency as a bare
      // count alongside its period - so printing either primitive on its own drops the unit of
      // measure. Composed as the summary print and the screen already do.
      table.addCell(getHeaderCell("Dosage"));
      table.addCell(getItemCell(med.optString("dose") + " " + med.optString("doseUnit")));
      table.addCell(getHeaderCell("Frequency"));
      table.addCell(getItemCell(frequencyText(med)));
      table.addCell(getHeaderCell("Quantity"));
      table.addCell(
          getItemCell(
              med.optString("dispensedQuantity") + " " + med.optString("dispensedQuantityUnit")));
      table.addCell(getHeaderCell("Est Days Supply"));
      table.addCell(getItemCell(med.optString("estimatedDaysSupply")));

      table.addCell(getHeaderCell("Refills Remaining"));
      table.addCell(getItemCell(med.optString("refillsRemaining")));
      table.addCell(getHeaderCell("Quantity Remaining"));
      table.addCell(getItemCell(med.optString("quantityRemaining")));

      JSONObject prescriberLicenceNumberObj = med.optJSONObject("prescriberLicenceNumber");
      table.addCell(getHeaderCell("Prescriber"));
      table.addCell(
          getItemCell(
              med.optString("prescriberLastname")
                  + ", "
                  + med.optString("prescriberFirstname")));

      if (prescriberLicenceNumberObj != null && prescriberLicenceNumberObj.length() > 0) {
        // Licensing body by name, not the raw identifier system URI. The body is empty for an
        // unrecognised system, so join conditionally rather than leaving a leading space.
        String licenceBody = licenceBody(prescriberLicenceNumberObj.optString("system"));
        String licenceValue = prescriberLicenceNumberObj.optString("value");
        table.addCell(getHeaderCell("Prescriber ID"));
        table.addCell(
            getItemCell(licenceBody.isEmpty() ? licenceValue : (licenceBody + " " + licenceValue)));
      }

      table.addCell(getHeaderCell("Prescriber Phone"));
      table.addCell(getItemCell(med.optString("prescriberPhoneNumber")));
      table.addCell(getHeaderCell("Pharmacy"));
      table.addCell(getItemCell(med.optString("dispensingPharmacy")));
      table.addCell(getHeaderCell("Pharmacy Fax"));
      table.addCell(getItemCell(med.optString("dispensingPharmacyFaxNumber")));

      table.addCell(getHeaderCell("Pharmacy Phone"));
      table.addCell(getItemCell(med.optString("dispensingPharmacyPhoneNumber")));

      table.addCell(getHeaderCell("Pharmacist"));
      table.addCell(getItemCell(pharmacistName(med)));

      document.add(table);
    } else {
      Paragraph noResults =
          new Paragraph(
              "No Med Found.",
              FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK));
      noResults.add(Chunk.NEWLINE);
      document.add(noResults);
    }

    document.close();
  }

  public void printSummary(
      LoggedInInfo loggedInInfo,
      Integer demographicNo,
      OutputStream outputStream,
      JSONObject jsonOb)
      throws Exception {

    Demographic demo = demographicManager.getDemographic(loggedInInfo, demographicNo);

    if (demo == null) throw new DocumentException();

    Document document =
        openDhdrDocument(loggedInInfo, demo, outputStream, "DHDR Summary", jsonOb, true);
    addSearchPeriod(document, jsonOb);

    JSONArray arr = sortByWhenPreparedDesc(jsonOb.getJSONArray("meds"), "drug");
    addSectionHeading(document, "Drug Product", arr.length());
    addDrugEventTables(document, arr);

    if (jsonOb.has("services")) {
      JSONArray serviceArr = sortByWhenPreparedDesc(jsonOb.getJSONArray("services"), "service");
      document.add(Chunk.NEWLINE);
      addSectionHeading(document, "Pharma Services", serviceArr.length());
      addGrid(document, pharmacyServiceColumns(), serviceArr, "service");
    }

    document.close();
  }

  /**
   * Renders one table per drug event - the per-event patient/prescriber/pharmacy block, then that
   * event's own header and data row - or the no-events line when there are none.
   *
   * <p>Not folded into {@link #addGrid}: this is not one grid but a table per event, each carrying a
   * colspanned metadata block above its own header row. The three {@code populateSummaryDrug*} helpers
   * were already shared between the Summary and Comparative prints and no defect was ever found in
   * them, which is the argument for leaving their shape alone.
   *
   * @param document Document the document being written
   * @param arr JSONArray the drug events, already sorted most-recent-first
   */
  private void addDrugEventTables(Document document, JSONArray arr) throws Exception {
    if (arr.length() == 0) {
      document.add(noEventsParagraph());
      return;
    }
    for (int i = 0; i < arr.length(); i++) {
      try {
        JSONObject med = arr.getJSONObject(i);
        auditMandatoryDispenseFields(med, i);
        PdfPTable table = new PdfPTable(new float[] {3, 3, 3, 1.5f, 1.5f, 1, 1});
        table.setSpacingAfter(10f);
        table.setWidthPercentage(100.0f);
        populateSummaryDrugMetaData(table, med);
        populateSummaryDrugHeader(table);
        populateSummaryDrugData(med, table);
        document.add(table);
      } catch (Exception e) {
        // One malformed dispense must not blank the whole PDF (print-side analogue of the DHDR
        // viewer's contained-guard fix). Log the structural fault - the entry index and the
        // exception types - and render a placeholder so the reader knows a record was present but
        // not shown.
        //
        // The message is dropped rather than trimmed. Everything raised here is raised while
        // reading the dispense record, so the message tends to quote what was being read: a
        // NumberFormatException carries the offending value verbatim, and the JSON layer names the
        // field it could not parse. The index already identifies which record to look at for anyone
        // holding the payload, and the type chain says what kind of malformation it was, which is
        // what this log is for.
        logger.error("DHDR print: skipped drug entry " + i + " - " + exceptionTypes(e));
        document.add(incompleteEntryTable());
      }
    }
  }

  /** Bounds the cause chain walked when logging a throwable, in case a cause cycle exists. */
  private static final int MAX_CAUSE_DEPTH = 10;

  /**
   * Renders a throwable as its chain of exception types, with every message omitted.
   *
   * <p>Types only, deliberately: this runs once per skipped entry inside a loop over the whole
   * dispense history, so stack frames would bury the print log on a payload with several malformed
   * records. The entry index in the message beside it is what locates the record; the type is what
   * says how it was malformed.
   *
   * @param throwable Throwable the exception to render, may be null
   * @return String the exception types from outermost to innermost, joined by " caused by "
   */
  private static String exceptionTypes(Throwable throwable) {
    if (throwable == null) {
      return "(none)";
    }
    StringBuilder rendered = new StringBuilder();
    Throwable current = throwable;
    for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
      if (depth > 0) {
        rendered.append(" caused by ");
      }
      rendered.append(current.getClass().getName());
      current = current.getCause() == current ? null : current.getCause();
    }
    return rendered.toString();
  }

  private PdfPCell populateSummaryDrugMetaData(PdfPTable table, JSONObject med) throws JSONException {
    PdfPTable headerTable = new PdfPTable(new float[] {1f, 3f, 1.5f, 3f, 1.5f, 2f});
    PdfPCell header = new PdfPCell(headerTable);
    header.setColspan(12);
    table.addCell(header);

    String dispenseDate = displayDate(med.optString("whenPrepared"));
    String pickupDate = displayDate(med.optString("pickUpDate"));
    JSONObject patient = med.optJSONObject("patient");
    String patientDob = patient != null ? patient.optString("birthDate") : "N/A";
    String patientGender = patient != null ? patient.optString("gender") : "N/A";
    String patientHcn = "N/A";
    if (patient != null
        && patient.has("identifier")
        && patient.getJSONArray("identifier").length() > 0
        && patient.getJSONArray("identifier").getJSONObject(0).has("value")) {
      patientHcn = patient.getJSONArray("identifier").getJSONObject(0).getString("value");
    }
    String patientFirstName = "N/A";
    String patientLastName = "N/A";
    if (patient != null && patient.has("name") && patient.getJSONArray("name").length() > 0) {
      JSONObject name = patient.getJSONArray("name").getJSONObject(0);
      if (name.has("given") && name.getJSONArray("given").length() > 0) {
        patientFirstName = name.getJSONArray("given").getString(0);
      }
      patientLastName = name.optString("family");
    }

    JSONObject prescriberLicenceNumberObj = med.optJSONObject("prescriberLicenceNumber");
    String prescriberLicenceValue =
        prescriberLicenceNumberObj != null ? prescriberLicenceNumberObj.optString("value") : "";
    // Show the licence when there is one; the test was inverted, so it printed " ()" for a
    // missing licence and hid a real one.
    String prescriberLicenseNumber =
        prescriberLicenceValue.isEmpty() ? "" : " (" + prescriberLicenceValue + ")";

    String prescriberName =
        med.optString("prescriberLastname")
            + (med.optString("prescriberLastname").length() > 0 ? ", " : "")
            + med.optString("prescriberFirstname")
            + prescriberLicenseNumber;
    // Only when no prescriber name resolved at all - this guard must not be inverted.
    if (prescriberName.trim().isEmpty()) {
      prescriberName = "N/A";
    }

    // Row 1
    getSummaryItemHeaderCell(headerTable, "First Name", patientFirstName);
    getSummaryItemHeaderCell(headerTable, "Prescriber", prescriberName);
    getSummaryItemHeaderCell(headerTable, "Dispensed Date", dispenseDate);

    // Row 2
    getSummaryItemHeaderCell(headerTable, "Last Name", patientLastName);
    getSummaryItemHeaderCell(
        headerTable, "Prescriber Phone", med.optString("prescriberPhoneNumber", "N/A"));
    getSummaryItemHeaderCell(headerTable, "Pickup Date", pickupDate);

    // Row 3
    getSummaryItemHeaderCell(headerTable, "Gender", patientGender);
    getSummaryItemHeaderCell(headerTable, "Pharmacy", med.optString("dispensingPharmacy", "N/A"));
    headerTable.addCell(getSpacerCell(2));

    // Row 4
    // DHDR03.06: the same format as every other date on the page. computeAge below keeps the raw
    // value - it parses yyyy-MM-dd, so formatting first would break the age.
    getSummaryItemHeaderCell(headerTable, "DOB", displayDate(patientDob));
    getSummaryItemHeaderCell(
        headerTable, "Pharmacy Fax", med.optString("dispensingPharmacyFaxNumber", "N/A"));
    headerTable.addCell(getSpacerCell(2));

    // Row 5
    getSummaryItemHeaderCell(headerTable, "HIN", patientHcn);
    // DHDR13.01.b lists age among the DHDR-side demographics. The page header carries it, but this
    // per-event block repeats the same identity with one field missing, so a reader checking the
    // block rather than the header sees an incomplete set. Same source as the header's age.
    // Blank rather than "N/A" when it cannot be derived, so it matches the DOB cell it comes from:
    // a patient with no birthDate renders DOB as "" (optString's default), and two different blank
    // markers for the same missing fact reads as a defect.
    getSummaryItemHeaderCell(headerTable, "Age", computeAge(patientDob));
    headerTable.addCell(getSpacerCell(2));

    return header;
  }

  private void populateSummaryDrugHeader(PdfPTable table) {
    table.addCell(getHeaderCell("Generic"));
    table.addCell(getHeaderCell("Brand"));
    table.addCell(getHeaderCell("Therapeutic Class/Sub-class"));
    table.addCell(getHeaderCell("Dosage"));
    table.addCell(getHeaderCell("Frequency"));
    table.addCell(getHeaderCell("Quantity"));
    table.addCell(
        getHeaderCell("Supply / Refills / Qty Remaining"));
  }

  private void populateSummaryDrugData(JSONObject med, PdfPTable table) throws JSONException {
    table.addCell(getItemCell(med.optString("genericName")));
    JSONObject brandObj = med.optJSONObject("brandName");
    String brandDisplay = brandObj != null ? brandObj.optString("display") : "";
    table.addCell(
        getItemCell(
            brandDisplay
                + " "
                + med.optString("drugDosageForm")
                + " "
                + med.optString("dispensedDrugStrength")));
    table.addCell(getItemCell(therapeuticClassText(med)));
    table.addCell(getItemCell(med.optString("dose") + " " + med.optString("doseUnit")));
    table.addCell(getItemCell(frequencyText(med)));
    table.addCell(
        getItemCell(
            med.optString("dispensedQuantity")
                + " "
                + med.optString("dispensedQuantityUnit")));
    table.addCell(
        getItemCell(
            "Est Days Supply:"
                + med.optString("estimatedDaysSupply")
                + " Refills Remaining: "
                + med.optString("refillsRemaining")
                + " Quantity Remaining: "
                + med.optString("quantityRemaining")));
  }

  private PdfPCell getSpacerCell(int i) {
    PdfPCell cell = new PdfPCell();
    cell.setBorderWidth(0f);
    cell.setColspan(i);
    return cell;
  }

  private void getSummaryItemHeaderCell(PdfPTable table, String header, String value) {
    PdfPCell headerCell = getHeaderCell(header);
    headerCell.setBorderWidth(0f);
    headerCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
    table.addCell(headerCell);
    PdfPCell dispenseDateValue = getItemCell(value);
    dispenseDateValue.setBorderWidth(0f);
    table.addCell(dispenseDateValue);
  }

  private PdfPCell getHeaderCell(String name) {
    Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, Color.BLACK);

    PdfPCell cell = new PdfPCell(new Phrase(name, font));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

    return cell;
  }

  private PdfPCell getItemCell(String name) {
    Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.BLACK);

    PdfPCell cell = new PdfPCell(new Phrase(name, font));

    return cell;
  }

  // The value-rendering helpers below, and the two column lists, are package-private rather than
  // private so DHDRPrintUnitTest can exercise them directly - the same reason DHDRManager exposes
  // fhirBirthDate and describesOperationOutcome. They are pure functions of their arguments; nothing
  // outside this package uses them.

  /**
   * One column of a printed grid: the heading, and how to read the value out of one event.
   *
   * <p>Declaring a column list once and rendering it twice is the point. The pharmacy-service grid was
   * built by hand in both the Summary and the Comparative print, and the two drifted: the Comparative
   * copy showed the pharmacy phone under a "Pharmacy #" heading where the obligation is the fax
   * (DHDR07.01(h)), while the Summary copy had always been correct. A column list cannot drift from
   * itself.
   */
  record Column(String label, Function<JSONObject, String> value) {}

  /**
   * The pharmacy-service columns, per DHDR07.01's element list. Rendered by both the Summary and the
   * Comparative print - this list is the single declaration of what that view contains.
   *
   * @return List&lt;Column&gt; the nine columns in display order
   */
  List<Column> pharmacyServiceColumns() {
    return List.of(
        new Column("Dispensed Date", med -> displayDate(med.optString("whenPrepared"))),
        new Column("Pickup Date", med -> displayDate(med.optString("whenHandedOver"))),
        new Column("Pharmacy Service Type", med -> serviceTypeWithPin(med.optJSONObject("brandName"))),
        new Column("Pharmacy Service Description", med -> med.optString("genericName")),
        new Column("Rx Number", med -> med.optString("rxNumber")),
        new Column("Therapeutic Class/Sub-class", this::therapeuticClassText),
        new Column("Pharmacy Name", med -> med.optString("dispensingPharmacy")),
        new Column("Pharmacist", this::pharmacistName),
        new Column("Pharmacy Fax", med -> med.optString("dispensingPharmacyFaxNumber")));
  }

  /**
   * The EMR half of the Comparative view: DHDR05.02's ten elements in the nine columns the screen
   * renders, in the same order, so the two halves stay comparable (DHDR13.01(d)).
   *
   * @return List&lt;Column&gt; the nine columns in display order
   */
  List<Column> emrPrescriptionColumns() {
    return List.of(
        new Column("Start Date", med -> displayDate(med.optString("rxDate"))),
        new Column("Medication", this::emrMedicationName),
        new Column("Strength",
            med -> joinValueAndUnit(optText(med, "strength"), optText(med, "strengthUnit"))),
        new Column("Dosage", this::emrDose),
        new Column("Frequency", med -> optText(med, "frequency")),
        new Column("Prescriber", med -> optText(med, "providerName")),
        new Column("DIN", med -> optText(med, "regionalIdentifier")),
        new Column("Qty / Duration", this::emrQuantityAndDuration),
        new Column("Refills", this::emrRefills));
  }

  /**
   * Renders one full-width grid: a header row, then a row per event, or the no-events line when there
   * are none. Absorbs the header/row/empty-branch handling that each table used to repeat.
   *
   * @param document Document the document being written
   * @param columns List&lt;Column&gt; the columns, in display order
   * @param events JSONArray the events to render, possibly empty
   * @param entryLabel String how to name an entry in the log when one is not a JSON object
   */
  private void addGrid(Document document, List<Column> columns, JSONArray events, String entryLabel)
      throws DocumentException {
    if (events.length() == 0) {
      document.add(noEventsParagraph());
      return;
    }
    PdfPTable table = new PdfPTable(columns.size());
    table.setWidthPercentage(100.0f);
    for (Column column : columns) {
      table.addCell(getHeaderCell(column.label()));
    }
    table.setHeaderRows(1);
    for (int i = 0; i < events.length(); i++) {
      JSONObject event = events.optJSONObject(i);
      if (event == null) {
        logger.error(
            "DHDR print: skipped " + entryLabel + " entry " + i + " - entry is not a JSON object");
        continue;
      }
      for (Column column : columns) {
        table.addCell(getItemCell(column.value().apply(event)));
      }
    }
    document.add(table);
  }

  /**
   * The line shown in place of a grid when a section has no events (DHDR02.04 - a valid search that
   * returns nothing must say so rather than render an empty table).
   *
   * @return Paragraph the no-events line
   */
  private Paragraph noEventsParagraph() {
    Paragraph noResults =
        new Paragraph(
            "No events found for the search time period.",
            FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK));
    noResults.add(Chunk.NEWLINE);
    return noResults;
  }

  /**
   * A section heading with its event count, as in "Pharma Services(Found 3 Events)".
   *
   * @param document Document the document being written
   * @param title String the section name
   * @param count int the number of events in the section
   */
  private void addSectionHeading(Document document, String title, int count)
      throws DocumentException {
    Paragraph heading =
        new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, Color.BLACK));
    heading.add(
        new Phrase(
            "(Found " + count + " Events)",
            FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK)));
    heading.add(Chunk.NEWLINE);
    heading.setSpacingAfter(5f);
    document.add(heading);
  }

  /**
   * Opens a DHDR printout: page size, margins, the per-page footer event (DHDR13.01.c/e/g/h), the
   * running header carrying both patient identities (DHDR13.01.a/b), and the underlined title.
   *
   * <p>The caller adds whatever follows the title - the Summary and Comparative prints add a search
   * period, the Detail print a blank line.
   *
   * @param loggedInInfo LoggedInInfo the current session, for the printed-by line
   * @param demo Demographic the EMR-side patient
   * @param outputStream OutputStream where the PDF is written
   * @param title String the document title, used for both the header and the heading
   * @param jsonOb JSONObject the print payload, read for the DHDR-side patient
   * @param landscape boolean true for the wide grid views, false for the portrait Detail view
   * @return Document the opened document
   */
  private Document openDhdrDocument(LoggedInInfo loggedInInfo, Demographic demo,
      OutputStream outputStream, String title, JSONObject jsonOb, boolean landscape)
      throws Exception {
    Document document = new Document();
    document.setPageSize(landscape ? PageSize.LETTER.rotate() : PageSize.LETTER);
    document.setMargins(36, 36, 90, 140);

    PdfWriter writer =
        PdfWriterFactory.newInstance(document, outputStream, FontSettings.HELVETICA_10PT);
    writer.setPageEvent(new DhdrFooterEvent(DHDR_DISCLAIMER, resolveConfidentialityStatement(),
        buildPrintedInfo(loggedInInfo)));

    document.setHeader(
        getHeaderFooter(demo, title, buildDhdrDemoLine(jsonOb.optJSONObject("dhdrPatient"))));
    document.open();

    Paragraph titleParagraph =
        new Paragraph(
            title,
            FontFactory.getFont(
                FontFactory.HELVETICA, 12, Font.BOLD | Font.UNDERLINE, Color.BLACK));
    titleParagraph.add(Chunk.NEWLINE);
    document.add(titleParagraph);
    return document;
  }

  /**
   * Adds the "Date Range" line. DHDR02.05 requires the search period used be displayed; DHDR03.06
   * requires one date format across the views, so this renders through the same formatter as the grid
   * cells rather than the ISO the payload carries.
   *
   * <p>Where the service reported having applied a range other than the one requested, a second line
   * says so. Without it the printout would state the requested range unqualified, and the paper
   * outlives the screen that carries the warning.
   *
   * @param document Document the document being written
   * @param jsonOb JSONObject the print payload, read for the search bounds
   */
  private void addSearchPeriod(Document document, JSONObject jsonOb) throws DocumentException {
    Paragraph dateRange =
        new Paragraph(
            "Date Range: ", FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, Color.BLACK));
    dateRange.add(
        new Phrase(
            searchPeriodText(jsonOb.optString("startDate"), jsonOb.optString("endDate")),
            FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK)));
    dateRange.add(Chunk.NEWLINE);

    String reported = serviceReportedPeriod(jsonOb.optJSONObject("serviceReportedPeriod"));
    if (!reported.isEmpty()) {
      dateRange.add(
          new Phrase(
              "The DHDR EHR Service reported searching: ",
              FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, Color.BLACK)));
      dateRange.add(
          new Phrase(
              reported,
              FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK)));
      dateRange.add(Chunk.NEWLINE);
    }

    document.add(dateRange);
    document.add(Chunk.NEWLINE);
  }

  /**
   * Describes a search period the way the viewer does, covering the cases where one bound is absent.
   *
   * <p>Previously the printout concatenated {@code start + " to " + end} unconditionally, so a cleared
   * start date produced a dangling {@code " to Jul 30, 2026"} - a range the search never used. BP6
   * requires the date range be displayed "if available else anything that represents the search period",
   * and the screen already renders all four shapes; the printout rendered one.
   *
   * @param startDate String the lower bound as {@code yyyy-MM-dd}, or empty
   * @param endDate String the upper bound as {@code yyyy-MM-dd}, or empty
   * @return String the period in the document's date format (DHDR03.06)
   */
  String searchPeriodText(String startDate, String endDate) {
    String start = displayDate(startDate);
    String end = displayDate(endDate);
    if (start.isEmpty() && end.isEmpty()) {
      return "all available events";
    }
    if (start.isEmpty()) {
      return "all events up to " + end;
    }
    if (end.isEmpty()) {
      return start + " onwards";
    }
    return start + " to " + end;
  }

  /**
   * Describes the search period the DHDR EHR Service reported having applied, or an empty string when
   * it reported the one requested. BP4 requires the echoed search parameters be checked; the viewer
   * does that and passes the result here.
   *
   * <p>Built from the raw bounds rather than the string the screen already formatted, so the line
   * obeys DHDR03.06 like every other date in the document. A service answering with a prefix the EMR
   * never sends leaves no bounds to format - the IG's own {@code DHDR-MedDis.json} echoes {@code eq} -
   * so in that case the viewer's own wording is quoted rather than printing nothing.
   *
   * @param echo JSONObject the mismatch the viewer recorded, or null when there was none
   * @return String the reported period, or "" when there is nothing to report
   */
  String serviceReportedPeriod(JSONObject echo) {
    if (echo == null) {
      return "";
    }
    String start = displayDate(echo.optString("startDate"));
    String end = displayDate(echo.optString("endDate"));
    if (!start.isEmpty() && !end.isEmpty()) {
      return start + " to " + end;
    }
    if (!start.isEmpty()) {
      return start + " onwards";
    }
    if (!end.isEmpty()) {
      return "all events up to " + end;
    }
    return optText(echo, "used");
  }

  /** DHDR03.06: matches the screen's Angular {@code | date} default. See displayDate. */
  private final SimpleDateFormat mediumDate = new SimpleDateFormat("MMM d, yyyy");

  /**
   * Renders a date for display in the same format the screen uses.
   *
   * <p>DHDR03.06 requires one date format across the DHDR views. The screen uses Angular's default
   * {@code | date} ("Jun 23, 2016"); the print kept ISO, so one dispense read two ways.
   *
   * <p>Accepts what the two sources supply: epoch milliseconds from the EMR transfer objects, an ISO
   * date or date-time from the DHDR service. Anything else is returned unchanged rather than blanked.
   *
   * <p>Display only - {@code sortByDateDesc} compares the raw ISO strings, and an ISO string compares
   * year, month and day before any time part, so lexical order is the printed date order. Formatting
   * before sorting would break that.
   *
   * @param raw String the value as it arrives, possibly empty
   * @return String the date as "MMM d, yyyy", or the input unchanged when it cannot be parsed
   */
  String displayDate(String raw) {
    String value = raw == null ? "" : raw.trim();
    if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
      return "";
    }
    // Epoch millis only when the digits are actually timestamp-length. Parsing any all-digit value
    // as epoch turned a FHIR date of "1984" into new Date(1984L) and printed "Jan 1, 1970" - beside
    // an Age cell that computeAge derives correctly from the same string, since it accepts yyyy and
    // yyyy-MM. The two are adjacent in the header block, so the header contradicted itself.
    if (value.matches("\\d{12,}")) {
      try {
        return mediumDate.format(new Date(Long.parseLong(value)));
      } catch (NumberFormatException tooLargeForLong) {
        return value;
      }
    }
    // A date-time carries a 'T'; take the date part.
    String datePart = value.length() > 10 && value.charAt(10) == 'T' ? value.substring(0, 10) : value;
    try {
      return mediumDate.format(java.sql.Date.valueOf(LocalDate.parse(datePart)));
    } catch (Exception notIso) {
      // A partial FHIR date (yyyy, yyyy-MM) reaches here. Print it verbatim rather than filling in a
      // month and day the source did not give: on a medication history an invented precision reads
      // as recorded fact.
      return value;
    }
  }

  /**
   * Reads a string from the payload, treating an explicit JSON null as absent.
   *
   * <p>The EMR payload carries nullable boxed types ({@code refillQuantity}, {@code refillDuration}),
   * and a serialised null read back through {@code optString} surfaces as the text "null" - which
   * printed verbatim reads as data rather than as an absence.
   *
   * @param med JSONObject one EMR medication from the comparative payload
   * @param key String the field to read
   * @return String the trimmed value, or an empty string when absent, blank or a stringified null
   */
  String optText(JSONObject med, String key) {
    String value = med.optString(key);
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    return "null".equalsIgnoreCase(trimmed) ? "" : trimmed;
  }

  /**
   * Reports whether an optional trailing detail is worth printing.
   *
   * <p>The screen guards these with {@code ng-if}, which is falsy for {@code 0}, so a record with no
   * refill or duration recorded shows the count alone. Treating "0" as present printed
   * "0 (0 / 0 days)" against the screen's "0".
   *
   * @param value String the candidate detail
   * @return boolean true when the value is present and not a zero
   */
  boolean isMeaningful(String value) {
    if (value == null || value.trim().isEmpty()) {
      return false;
    }
    try {
      return Double.parseDouble(value.trim()) != 0d;
    } catch (NumberFormatException e) {
      // Not numeric - a unit or free text, so it carries information.
      return true;
    }
  }

  /**
   * Joins a value with its unit of measure, omitting the separator when either part is absent.
   *
   * <p>DHDR05.02 asks for several elements "(value and unit of measure)". Concatenating
   * unconditionally prints a stray space or a bare unit for a record that carries only one part.
   *
   * @param value String the numeric part, possibly empty
   * @param unit String the unit of measure, possibly empty
   * @return String the two joined by a space, either one alone, or an empty string
   */
  String joinValueAndUnit(String value, String unit) {
    String v = value == null ? "" : value.trim();
    String u = unit == null ? "" : unit.trim();
    if (v.isEmpty()) {
      return u;
    }
    return u.isEmpty() ? v : (v + " " + u);
  }

  /**
   * Renders the medication's name for DHDR05.02(c), in the same fallback order as the screen.
   *
   * @param med JSONObject one EMR medication from the comparative payload
   * @return String the generic name, else the brand name, else the custom name, else empty
   */
  String emrMedicationName(JSONObject med) {
    String generic = optText(med, "genericName");
    if (!generic.trim().isEmpty()) {
      return generic;
    }
    String brand = optText(med, "brandName");
    if (!brand.trim().isEmpty()) {
      return brand;
    }
    return optText(med, "customName");
  }

  /**
   * Dosage forms that name a countable unit, so the form itself can stand in as the dose unit.
   *
   * <p>Transcribed from {@code DemographicExportHelper:184-192}, which holds the same list as an
   * if/else chain and is the origin of both this copy and the screen's. Order matters only in that
   * the first match wins, as it does there.
   *
   * <p>"globule" was inherited as "grobule"; the screen lists both so a form recorded with the
   * original typo still matches, and this list follows it rather than the export helper, which
   * carries only the typo. A form spelled correctly would otherwise lose its unit here while
   * keeping it on screen - the divergence this method exists to close.
   */
  private static final List<String> COUNTABLE_DOSE_FORMS = List.of("capsule", "drop", "dosing",
      "globule", "grobule", "granule", "patch", "pellet", "pill", "tablet");

  /**
   * Resolves the dose unit for an EMR medication, mirroring the screen's {@code emrDoseUnit()}.
   *
   * <p>Two steps, and the second is the one this used to omit. The recorded unit is preferred, but
   * only when it differs from the strength unit already in its own column - OpenO stores the dosage
   * unit as free text and a record often repeats the strength there, which would print "50 mg" under
   * both headings. Where it does repeat, or is absent, the drug form stands in if it names something
   * countable. Anything else has no unit to show, and DHDR05.02 asks for these "if available".
   *
   * <p>Lower-cased on the way out, as the screen's is: it compares in lower case and returns what it
   * compared, so a unit recorded as "MG" reads "mg" on screen. Returning it raw here printed the two
   * differently for the same drug.
   *
   * @param med JSONObject one EMR medication from the comparative payload
   * @return String the unit to print, or an empty string when none adds information
   */
  String emrDoseUnit(JSONObject med) {
    String unit = optText(med, "unit").toLowerCase(Locale.ROOT);
    String strengthUnit = optText(med, "strengthUnit").toLowerCase(Locale.ROOT);
    if (!unit.isEmpty() && !unit.equals(strengthUnit)) {
      return unit;
    }
    String form = optText(med, "form").toLowerCase(Locale.ROOT);
    for (String countable : COUNTABLE_DOSE_FORMS) {
      if (form.contains(countable)) {
        return countable;
      }
    }
    return "";
  }

  /**
   * Renders the prescribed dose for DHDR05.02(e), as a single value or a min-max range.
   *
   * @param med JSONObject one EMR medication from the comparative payload
   * @return String the dose, possibly a range, with a unit where one adds information
   */
  String emrDose(JSONObject med) {
    String min = optText(med, "takeMin");
    String max = optText(med, "takeMax");
    // Falsy-for-zero, as the screen's ng-if is: a recorded dose of 0 is placeholder data, not a dose.
    if (!isMeaningful(min)) {
      return "";
    }
    String dose = (!max.isEmpty() && !max.equals(min)) ? (min + " - " + max) : min;
    String unit = emrDoseUnit(med);
    return unit.isEmpty() ? dose : (dose + " " + unit);
  }

  /**
   * Renders the first fill's quantity and duration for DHDR05.02(g).
   *
   * @param med JSONObject one EMR medication from the comparative payload
   * @return String the quantity, followed by "/ duration unit" when a duration is recorded
   */
  String emrQuantityAndDuration(JSONObject med) {
    String quantity = optText(med, "quantity");
    String duration = optText(med, "duration");
    if (!isMeaningful(duration)) {
      return quantity;
    }
    String durationText = joinValueAndUnit(duration, optText(med, "durationUnit"));
    return quantity.isEmpty() ? durationText : (quantity + " / " + durationText);
  }

  /**
   * Renders the refill count with its duration and quantity, DHDR05.02(h) and (i).
   *
   * <p>Both share one column on the screen, so they share one here; a record with no refill detail
   * prints the count alone rather than an empty bracket.
   *
   * @param med JSONObject one EMR medication from the comparative payload
   * @return String the repeat count, optionally followed by the refill quantity and duration
   */
  String emrRefills(JSONObject med) {
    String repeats = optText(med, "repeats");
    String refillQuantity = optText(med, "refillQuantity");
    if (!isMeaningful(refillQuantity)) {
      return repeats;
    }
    String detail = refillQuantity;
    String refillDuration = optText(med, "refillDuration");
    if (isMeaningful(refillDuration)) {
      detail = detail + " / " + refillDuration + " days";
    }
    return repeats.isEmpty() ? detail : (repeats + " (" + detail + ")");
  }

  /**
   * Renders a dispense's frequency from the four parts the viewer model splits it into.
   *
   * <p>None of the parts is guaranteed: {@code dosageInstruction} is optional under the DHDR
   * consumer profile, and the IG's own pharmacy-service example carries none. Concatenating them
   * unconditionally printed {@code " every  - "} for such a record, which reads as a frequency
   * rather than as the absence of one. A record that does carry the parts prints exactly as before.
   *
   * @param med JSONObject one dispense from the print payload
   * @return String the composed frequency, or an empty string when no part was supplied
   */
  String frequencyText(JSONObject med) {
    String frequency = med.optString("frequency");
    String period = med.optString("period");
    String periodMax = med.optString("periodMax");
    String periodUnit = med.optString("periodUnit");
    if (frequency.isEmpty() && period.isEmpty() && periodMax.isEmpty() && periodUnit.isEmpty()) {
      return "";
    }
    return frequency + " every " + period + " - " + periodMax + " " + periodUnit;
  }

  /**
   * A standalone one-cell table marking a dispense that was present in the response but could not be
   * rendered. Used by the per-entry isolation so one malformed record never blanks the whole PDF.
   *
   * @return PdfPTable a full-width placeholder table
   */
  private PdfPTable incompleteEntryTable() {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100.0f);
    table.setSpacingAfter(10f);
    table.addCell(getItemCell("A record could not be displayed (data incomplete)."));
    return table;
  }

  /**
   * Log any DHDR-mandatory dispense field that is absent from this entry. The cell is still rendered
   * as-is (no substitution) - this only surfaces an upstream-conformance gap for audit. Logs the field
   * name and entry index only; never any field value, so no PHI is written.
   *
   * @param med JSONObject the flattened dispense payload
   * @param index int the entry's position in its result list (for correlation in the log)
   */
  private void auditMandatoryDispenseFields(JSONObject med, int index) {
    for (String field : MANDATORY_DISPENSE_FIELDS) {
      if (med.optString(field, "").isEmpty()) {
        logger.warn(
            "DHDR print: entry " + index + " missing mandatory field '" + field + "'");
      }
    }
  }

  /**
   * Maps a prescriber/pharmacist licence identifier {@code system} URI to the full licensing-body
   * name, mirroring the viewer's {@code getLicence} mapping.
   *
   * @param system String the FHIR identifier system URI, or null/empty
   * @return String the licensing-body name, or an empty string when the system is unknown/absent
   */
  String licenceBody(String system) {
    if (system == null || system.isEmpty()) {
      return "";
    }
    if (system.endsWith("ca-on-license-physician"))
      return "College of Physicians and Surgeons of Ontario";
    if (system.endsWith("ca-on-license-dental-surgeon"))
      return "Royal College of Dental Surgeons of Ontario";
    if (system.endsWith("ca-out-of-province-prescriber")) return "Out-of-Province Prescriber";
    if (system.endsWith("ca-on-registration-chiropodist")) return "College of Chiropodists of Ontario";
    if (system.endsWith("ca-on-license-midwife")) return "College of Midwives of Ontario";
    if (system.endsWith("ca-on-license-pharmacist")) return "Ontario College of Pharmacists";
    if (system.endsWith("ca-on-license-optometrist")) return "College of Optometrists of Ontario";
    if (system.endsWith("ca-on-license-nurse")) return "College of Nurses of Ontario";
    if (system.endsWith("ca-on-license-naturopath")) return "College of Naturopaths of Ontario";
    if (system.endsWith("ca-on-unknown-prescriber")) return "Unknown Prescriber";
    return "";
  }

  /**
   * Renders a dispense's pharmacist as "Lastname, Firstname".
   *
   * <p>Name only, and deliberately: the pharmacist's licence number is not an element of any DHDR
   * view. DHDR04.01, DHDR06.01(g) and DHDR07.01(g) all specify exactly {@code
   * [Practitioner.name.given], [Practitioner.name.family]}, and "pharmacist" appears nowhere else in
   * the specification - while the same DHDR06.01 gives the *prescriber* both an ID (e) and a
   * professional ID (f), so the omission reads as deliberate rather than as an oversight. The print
   * used to append the licence, which the screen's own pharmacy-service tables never did.
   *
   * <p>Both separators are conditional: an absent surname must not print a leading comma.
   *
   * @param med JSONObject one dispense from the print payload
   * @return String the pharmacist's name, or an empty string when neither part was supplied
   */
  String pharmacistName(JSONObject med) {
    String last = optText(med, "pharmacistLastname");
    String first = optText(med, "pharmacistFirstname");
    return last.isEmpty() ? first : (first.isEmpty() ? last : (last + ", " + first));
  }

  /**
   * Renders a pharmacy-service type cell, appending the product identifier (PIN) when present.
   * Null-safe on an absent {@code brandName}.
   *
   * @param brandObj JSONObject the parsed brandName object, or null
   * @return String the service-type display with the PIN appended when available
   */
  String serviceTypeWithPin(JSONObject brandObj) {
    if (brandObj == null) {
      return "";
    }
    String display = brandObj.optString("display");
    String pin = brandObj.optString("code");
    if (pin.isEmpty()) {
      return display;
    }
    return display.isEmpty() ? ("PIN: " + pin) : (display + " (PIN: " + pin + ")");
  }

  /**
   * Renders the EMR demographic's date of birth in the same format as every other date on the page.
   *
   * <p>Not {@link Demographic#getBirthDayAsString()} directly: it joins the three birth columns
   * without padding, so it can yield {@code 1984-8-8}, which is not a date any formatter here parses.
   * {@link DHDRManager#fhirBirthDate} already does the padding and validation for the search, so this
   * borrows it rather than repeating it. Falls back to the raw string when it cannot be parsed - an
   * unrecognised value is still information.
   *
   * @param demo Demographic the patient whose date of birth is being printed
   * @return String the date as "MMM d, yyyy", or the raw joined value when it cannot be parsed
   */
  String emrBirthDate(Demographic demo) {
    String iso = DHDRManager.fhirBirthDate(demo);
    return iso != null ? displayDate(iso) : demo.getBirthDayAsString();
  }

  private String getDemoInfo(Demographic demo) {
    StringBuilder demoInfo =
        new StringBuilder(demo.getSexDesc())
            .append(" Age: ")
            .append(demo.getAge())
            .append(" (")
            .append(emrBirthDate(demo))
            .append(")")
            .append(" HIN: (")
            .append(demo.getHcType())
            .append(") ")
            .append(demo.getHin())
            .append(" ")
            .append(demo.getVer());
    return demoInfo.toString();
  }

  private Phrase getTitlePhrase(Demographic demo, String title, String dhdrDemoLine) {
    Phrase titlePhrase =
        new Phrase(
            16, title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.BOLD, Color.BLACK));
    titlePhrase.add(Chunk.NEWLINE);
    titlePhrase.add(
        new Chunk(
            demo.getFormattedName(),
            FontFactory.getFont(FontFactory.HELVETICA, 14, Font.NORMAL, Color.BLACK)));
    titlePhrase.add(Chunk.NEWLINE);
    // The EMR-side demographic (DHDR13.01.a).
    titlePhrase.add(
        new Chunk(
            getDemoInfo(demo),
            FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, Color.BLACK)));

    // The DHDR-side demographic (DHDR13.01.b); omitted when no DHDR patient was resolved.
    if (dhdrDemoLine != null && !dhdrDemoLine.isEmpty()) {
      titlePhrase.add(Chunk.NEWLINE);
      titlePhrase.add(
          new Chunk(
              dhdrDemoLine, FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK)));
    }

    return titlePhrase;
  }

  private HeaderFooter getHeaderFooter(Demographic demo, String title, String dhdrDemoLine) {
    HeaderFooter header = new HeaderFooter(getTitlePhrase(demo, title, dhdrDemoLine), false);
    header.setAlignment(HeaderFooter.ALIGN_RIGHT);
    header.setBorder(Rectangle.BOTTOM);

    return header;
  }

  public void printComparative(
      LoggedInInfo loggedInInfo,
      Integer demographicNo,
      OutputStream outputStream,
      JSONObject jsonOb)
      throws Exception {

    Demographic demo = demographicManager.getDemographic(loggedInInfo, demographicNo);

    if (demo == null) throw new DocumentException();

    Document document =
        openDhdrDocument(loggedInInfo, demo, outputStream, "DHDR Comparative", jsonOb, true);
    addSearchPeriod(document, jsonOb);

    JSONArray arr = sortByWhenPreparedDesc(jsonOb.getJSONArray("meds"), "drug");
    addSectionHeading(document, "DHDR Drugs", arr.length());
    addDrugEventTables(document, arr);

    if (jsonOb.has("services")) {
      JSONArray serviceArr = sortByWhenPreparedDesc(jsonOb.getJSONArray("services"), "service");
      document.add(Chunk.NEWLINE);
      addSectionHeading(document, "DHDR PharmaServices", serviceArr.length());
      // DHDR08.01(b) takes this view's element list from DHDR07.01, whose (h) is the pharmacy FAX.
      // This grid once printed dispensingPharmacyPhoneNumber under a "Pharmacy #" heading while the
      // Summary print's copy of the same table was correct - the divergence that a shared column list
      // makes impossible.
      addGrid(document, pharmacyServiceColumns(), serviceArr, "service");
    }

    if (jsonOb.has("localData")) {
      // DHDR05.02 requires the EMR-recorded medications in descending chronological order by default,
      // and DHDR13.01(d) carries the View's element set onto the paper. The browser posts this array in
      // DrugDao.findByDemographicId's order - createDate DESC - while the column printed as "Start Date"
      // is rxDate, a different field, so a back-dated or bulk-entered prescription landed out of order.
      // The screen hides that because its orderBy sorts at render time and never touches the array.
      JSONArray localArr = sortByDateDesc(jsonOb.getJSONArray("localData"), "rxDate", "EMR prescription");
      document.add(Chunk.NEWLINE);
      addSectionHeading(document, "EMR Prescriptions", localArr.length());
      // DHDR13.01(d): the printout must carry "all data elements required for the View", and for the
      // EMR half that list is DHDR05.02's ten. See emrPrescriptionColumns.
      addGrid(document, emrPrescriptionColumns(), localArr, "local-history");
    }

    document.close();
  }

  /**
   * Builds the "Printed on &lt;date&gt; by &lt;name&gt;" line rendered in the per-page footer
   * (DHDR13.01.c).
   *
   * @param loggedInInfo LoggedInInfo the current session, used for the printing provider's name
   * @return String the printed-on/by footer line
   */
  private String buildPrintedInfo(LoggedInInfo loggedInInfo) {
    String printedByName =
        loggedInInfo.getLoggedInProvider().getLastName()
            + ", "
            + loggedInInfo.getLoggedInProvider().getFirstName();
    return "Printed on " + formatter.format(new Date()) + " by " + printedByName;
  }

  /**
   * Resolves the EMR confidentiality statement printed on each page (DHDR13.01.h). Uses the
   * clinic-configured statement (oscar_mcmaster.properties {@code confidentiality_statement.vN})
   * when set, otherwise a generic personal-health-information confidentiality notice.
   *
   * @return String the confidentiality statement to print
   */
  private String resolveConfidentialityStatement() {
    String configured = OscarProperties.getConfidentialityStatement();
    if (configured != null && !configured.trim().isEmpty()) {
      return configured.trim();
    }
    return "CONFIDENTIALITY NOTICE: This document contains personal health information intended only"
        + " for the authorized recipient. Any unauthorized review, use, disclosure, or distribution"
        + " is prohibited.";
  }

  /**
   * Renders the DHDR-maintained patient identity for the event being printed into the Detailed view's
   * label/value table, marking any field that disagrees with the EMR record.
   *
   * <p>The marker text matches the screen's, deliberately: a reader comparing paper against the modal
   * should see the same word. A field with no value is left blank rather than marked - "not recorded in
   * the DHDR" and "recorded differently" are different facts, and the viewer already distinguishes them.
   *
   * @param table PdfPTable the two-column detail table being built
   * @param eventPatient JSONObject the event's own patient identity and match flags, or null
   */
  private void addEventPatientRows(PdfPTable table, JSONObject eventPatient) {
    if (eventPatient == null) {
      return;
    }
    // nameUnmatched is one flag over both name parts, so the name is printed as one row, exactly as
    // the modal prints it. Split across a First Name and a Last Name row the single flag would mark
    // both when only one differs, asserting a mismatch on a field that in fact agrees with the EMR.
    addEventPatientRow(table, "Patient Name", eventPatientName(eventPatient),
        eventPatient.optBoolean("nameUnmatched", false));
    addEventPatientRow(table, "Gender", eventPatient.optString("gender", ""),
        eventPatient.optBoolean("genderUnmatched", false));
    String dob = eventPatient.optString("dob", "");
    addEventPatientRow(table, "DOB", dob.isEmpty() ? "" : displayDate(dob),
        eventPatient.optBoolean("dobUnmatched", false));
    // Age is derived, so it carries no match flag of its own; DHDR13.01(b) lists it alongside the rest.
    addEventPatientRow(table, "Age", computeAge(dob), false);
    addEventPatientRow(table, "HIN", eventPatient.optString("hin", ""),
        eventPatient.optBoolean("hinUnmatched", false));
  }

  /**
   * Joins the event patient's name parts the way the Detailed view's modal shows them - "Last, First" -
   * so paper and screen read alike. Either part may be absent, and a name with only one part is
   * rendered without a dangling separator.
   *
   * @param eventPatient JSONObject the event's own patient identity
   * @return String the name, or an empty string when neither part was recorded
   */
  String eventPatientName(JSONObject eventPatient) {
    String last = eventPatient.optString("lastName", "").trim();
    String first = eventPatient.optString("firstName", "").trim();
    if (last.isEmpty()) {
      return first;
    }
    return first.isEmpty() ? last : last + ", " + first;
  }

  /**
   * Adds one label/value row of the event patient block, appending the unmatched marker when the field
   * both has a value and disagrees with the EMR.
   *
   * @param table PdfPTable the two-column detail table being built
   * @param label String the row label
   * @param value String the value, already formatted for display; may be empty
   * @param unmatched boolean whether this field disagrees with the EMR record
   */
  private void addEventPatientRow(PdfPTable table, String label, String value, boolean unmatched) {
    table.addCell(getHeaderCell(label));
    table.addCell(getItemCell(eventPatientCellValue(value, unmatched)));
  }

  /**
   * Renders one event-patient value, appending the unmatched marker only when the field has a value to
   * mark. An absent field is left blank: "not recorded in the DHDR" and "recorded differently from the
   * EMR" are different facts, and marking the first as the second would assert a comparison that was
   * never made.
   *
   * @param value String the value, already formatted for display; may be empty
   * @param unmatched boolean whether this field disagrees with the EMR record
   * @return String the cell text
   */
  String eventPatientCellValue(String value, boolean unmatched) {
    return !value.isEmpty() && unmatched ? value + " (UNMATCHED)" : value;
  }

  /**
   * Builds the DHDR-side patient demographic line (DHDR13.01.b) from the front-end {@code
   * dhdrPatient} object (first/last name, gender, DOB, HIN as maintained by the DHDR EHR Service).
   *
   * @param dhdrPatient JSONObject the DHDR-side patient, or null when none was resolved
   * @return String the demographic line, or an empty string when no DHDR patient is available
   */
  String buildDhdrDemoLine(JSONObject dhdrPatient) {
    if (dhdrPatient == null) {
      return "";
    }
    String name = eventPatientName(dhdrPatient);
    String gender = dhdrPatient.optString("gender", "").trim();
    String dob = dhdrPatient.optString("dob", "").trim();
    String hin = dhdrPatient.optString("hin", "").trim();
    if (name.isEmpty() && dob.isEmpty() && hin.isEmpty()) {
      return "";
    }
    // Same join as the Detailed view's patient block, so the header and the table below it cannot
    // render one patient's name two ways.
    StringBuilder sb = new StringBuilder("DHDR EHR Service - ");
    sb.append(name);
    if (!gender.isEmpty()) {
      sb.append("   Gender: ").append(gender);
    }
    if (!dob.isEmpty()) {
      // Formatted for display (DHDR03.06); computeAge still reads the raw yyyy-MM-dd.
      sb.append("   DOB: ").append(displayDate(dob));
      String age = computeAge(dob);
      if (!age.isEmpty()) {
        sb.append("   Age: ").append(age);
      }
    }
    if (!hin.isEmpty()) {
      sb.append("   HIN: ").append(hin);
    }
    return sb.toString();
  }

  /**
   * Computes the age in whole years from a FHIR birthDate string (yyyy, yyyy-MM, or yyyy-MM-dd).
   *
   * @param dob String the FHIR birthDate value
   * @return String the age in years, or an empty string when the value cannot be parsed or is not
   *     yet reached
   */
  String computeAge(String dob) {
    try {
      String[] parts = dob.split("-");
      int year = Integer.parseInt(parts[0]);
      int month = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
      int day = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
      LocalDate birth = LocalDate.of(year, month, day);
      // A date of birth in the future is a data-quality case this method otherwise tolerates, and
      // it has no age to report - so it is omitted the same way an unparseable date is. Tested on
      // the date rather than on the computed years: a date less than a year ahead yields 0 years
      // rather than a negative count, which would have printed as a plausible "Age: 0".
      LocalDate today = LocalDate.now();
      if (birth.isAfter(today)) {
        return "";
      }
      return String.valueOf(Period.between(birth, today).getYears());
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Renders the therapeutic class / sub-class cell, emitting the separator only when there is a value on
   * both sides of it.
   *
   * <p>BP17 states the DHDR maintains these for pharmacy services and not for drug dispenses, and the
   * drug grid duly printed a bare "/" on every row. Dropping the column would have been wrong: the IG's
   * own examples carry AHFS codings on drug dispenses, so a service that supplies them would have had
   * them hidden. Rendering conditionally is correct whichever way the live service behaves, and it
   * covers the pharmacy grid too, where a service missing one of the pair had the same exposure.
   *
   * @param med JSONObject the dispense or pharmacy-service event
   * @return String "class / sub-class", either one alone, or empty
   */
  String therapeuticClassText(JSONObject med) {
    String cls = optText(med, "ahfsClass");
    String sub = optText(med, "ahfsSubClass");
    if (cls.isEmpty()) {
      return sub;
    }
    return sub.isEmpty() ? cls : cls + " / " + sub;
  }

  /**
   * Returns a copy of the given events ordered by dispense date ({@code whenPrepared}) descending -
   * most recent first - as the printed history requires (DHDR13.01). Events with no whenPrepared
   * value sort last. The source array is left unmodified.
   *
   * <p>A malformed entry is dropped here rather than allowed to abort the print. This runs before
   * the guarded rendering loops, so {@code getJSONObject} threw past every one of them and lost the
   * whole PDF over a single bad element - defeating the per-entry isolation those loops exist to
   * provide. Skipping matches how the rendering loops already treat the same case, so one malformed
   * entry costs one row wherever it is encountered.
   *
   * @param arr JSONArray the DHDR dispense / pharmacy-service events
   * @param entryLabel String what the entries are, for the skip log ("drug", "service")
   * @return JSONArray the events ordered by whenPrepared descending
   */
  private JSONArray sortByWhenPreparedDesc(JSONArray arr, String entryLabel) throws JSONException {
    return sortByDateDesc(arr, "whenPrepared", entryLabel);
  }

  /**
   * Returns a copy of the given events ordered by the named ISO date field descending - most recent
   * first. Events with no value for that field sort last, since the empty string compares below every
   * date. The source array is left unmodified.
   *
   * <p>The comparison is lexical on the raw ISO strings. An ISO-8601 value compares year, then month,
   * then day before it reaches any time part, so the result is descending order of the date each row
   * prints - which is the order DHDR13.01 asks for at the granularity this page renders.
   *
   * <p>A FHIR {@code dateTime} may also carry a time and a UTC offset, so two values can order one way
   * by instant and the other by string. That only happens between values sharing a printed date, or
   * where a value in a local offset and one in {@code Z} straddle midnight - and in that second case
   * ordering by instant would print the earlier date above the later one, which on a page showing only
   * dates reads as a fault. Ordering by the string keeps the printed dates in the order a reader can
   * check. Every dispense OMD returns is date-only in any event.
   *
   * @param arr JSONArray the events to order
   * @param dateField String the property holding the ISO date to order by
   * @param entryLabel String what the entries are, for the skip log ("drug", "service", "EMR prescription")
   * @return JSONArray the events ordered by that field, descending
   */
  JSONArray sortByDateDesc(JSONArray arr, String dateField, String entryLabel)
      throws JSONException {
    List<JSONObject> list = new ArrayList<>();
    for (int i = 0; i < arr.length(); i++) {
      JSONObject event = arr.optJSONObject(i);
      if (event == null) {
        logger.error(
            "DHDR print: skipped " + entryLabel + " entry " + i + " - entry is not a JSON object");
        continue;
      }
      list.add(event);
    }
    list.sort((a, b) -> b.optString(dateField, "").compareTo(a.optString(dateField, "")));
    JSONArray sorted = new JSONArray();
    for (JSONObject event : list) {
      sorted.put(event);
    }
    return sorted;
  }

  /**
   * Page event that stamps the DHDR disclaimer (DHDR13.01.g), the EMR confidentiality statement
   * (DHDR13.01.h), the printed-on/by line (DHDR13.01.c) and the "Page X of Y" counter (DHDR13.01.e)
   * into the bottom margin of every page.
   */
  private static class DhdrFooterEvent extends PdfPageEventHelper {
    private final String disclaimer;
    private final String confidentiality;
    private final String printedInfo;
    private PdfTemplate totalPages;
    private final Font footerFont =
        FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private final Font legalFont =
        FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, new Color(80, 80, 80));

    DhdrFooterEvent(String disclaimer, String confidentiality, String printedInfo) {
      this.disclaimer = disclaimer;
      this.confidentiality = confidentiality;
      this.printedInfo = printedInfo;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
      totalPages = writer.getDirectContent().createTemplate(40, 12);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfContentByte cb = writer.getDirectContent();
      float left = document.left();
      float right = document.right();
      float bottom = document.bottom();

      // DHDR disclaimer (13.01.g) then the EMR confidentiality statement (13.01.h), wrapped in one
      // legal band that fills downward from just below the content area to above the page line.
      ColumnText ct = new ColumnText(cb);
      ct.setLeading(8f);
      ct.setSimpleColumn(left, bottom - 100, right, bottom - 6);
      ct.addText(new Phrase(disclaimer, legalFont));
      ct.addText(new Phrase(Chunk.NEWLINE));
      ct.addText(new Phrase(confidentiality, legalFont));
      try {
        ct.go();
      } catch (DocumentException e) {
        // The footer is best-effort chrome; never let it abort the print.
      }

      // Printed-on/by (left) and Page X of Y (right) share the lowest baseline.
      float lineY = bottom - 118;
      ColumnText.showTextAligned(
          cb, Element.ALIGN_LEFT, new Phrase(printedInfo, footerFont), left, lineY, 0);
      ColumnText.showTextAligned(
          cb,
          Element.ALIGN_RIGHT,
          new Phrase("Page " + writer.getPageNumber() + " of ", footerFont),
          right - 40,
          lineY,
          0);
      cb.addTemplate(totalPages, right - 38, lineY);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
      ColumnText.showTextAligned(
          totalPages,
          Element.ALIGN_LEFT,
          new Phrase(String.valueOf(writer.getPageNumber() - 1), footerFont),
          0,
          2,
          0);
    }
  }
}
