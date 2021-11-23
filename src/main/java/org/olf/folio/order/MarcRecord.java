package org.olf.folio.order;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

import java.util.List;

public class MarcRecord {

  DataField d245;
  DataField d980;
  DataField first856;
  boolean has856;
  // Mappings 245
  private static final String TITLE_ONE = "a";
  private static final String TITLE_TWO = "b";
  private static final String TITLE_THREE = "c";

  // Mappings 980
  private static final String OBJECT_CODE           = "o";
  private static final String PROJECT_CODE          = "r";
  private static final String FUND_CODE             = "b";
  private static final String VENDOR_CODE           = "v";
  private static final String NOTES                 = "n";
  private static final String PRICE                 = "m";
  private static final String ELECTRONIC_INDICATOR  = "z";
  private static final String VENDOR_ITEM_ID        = "c";
  private static final String CURRENCY              = "k";
  private static final String ACQUISITION_METHOD    = "t";
  private static final String SELECTOR              = "f";
  private static final String VENDOR_ACCOUNT        = "g";
  private static final String DONOR                 = "p";
  private static final String REF_NUMBER_TYPE       = "u";
  private static final String RUSH                  = "w";
  private static final String EXPENSE_CLASS_CODE    = "y";
  private static final String ACCESS_PROVIDER_CODE  = "l";
  private static final String BILL_TO               = "s";
  private static final String RECEIVING_NOTE        = "x";
  private static final String DESCRIPTION           = "e";
  private static final String VENDOR_INVOICE_NO     = "h";
  private static final String INVOICE_DATE          = "i";
  private static final String SUB_TOTAL             = "j";

  // Mappings 856
  private static final String USER_LIMIT           = "x";

  public MarcRecord(Record marcRecord) {
    d245 = (DataField) marcRecord.getVariableField("245");
    d980 = (DataField) marcRecord.getVariableField("980");
    first856 = getFirst856(marcRecord);
    has856 =  (first856 != null);
  }

  private DataField getFirst856 (Record record) {
    List<VariableField> fields =  record.getVariableFields("856");
    if (fields != null && !fields.isEmpty()) {
      return (DataField) fields.get(0);
    } else {
      return null;
    }
  }

  public String titleOne() {
    return d245.getSubfieldsAsString(TITLE_ONE);
  }

  public String titleTwo() {
    return d245.getSubfieldsAsString(TITLE_TWO);
  }

  public String titleThree() {
    return d245.getSubfieldsAsString(TITLE_THREE);
  }

  public String title() {
    return titleOne()
            + (titleTwo() == null ? "" : " " + titleTwo())
            + (titleThree() == null ? "" : " " + titleThree());
  }

  public String objectCode() {
    return d980.getSubfieldsAsString(OBJECT_CODE);
  }

  public boolean hasObjectCode() {
    return objectCode() != null && !objectCode().isEmpty();
  }

  public String projectCode() {
    return d980.getSubfieldsAsString(PROJECT_CODE);
  }

  public boolean hasProjectCode() {
    return projectCode() != null && !projectCode().isEmpty();
  }

  public String fundCode() {
    return d980.getSubfieldsAsString(FUND_CODE);
  }

  public String vendorCode() {
    return d980.getSubfieldsAsString(VENDOR_CODE);
  }

  public String notes() {
    return d980.getSubfieldsAsString(NOTES);
  }

  public boolean hasNotes() {
    return notes() != null && ! notes().isEmpty();
  }

  public String price() {
    return d980.getSubfieldsAsString(PRICE);
  }

  public String electronicIndicator() {
    return d980.getSubfieldsAsString(ELECTRONIC_INDICATOR);
  }

  public String vendorItemId() {
    return d980.getSubfieldsAsString(VENDOR_ITEM_ID);
  }

  public boolean hasVendorItemId() {
    return vendorItemId() != null && !vendorItemId().isEmpty();
  }

  public boolean electronic() {
    return "ELECTRONIC".equalsIgnoreCase(electronicIndicator());
  }

  public String currency() {
    return d980.getSubfieldsAsString(CURRENCY) == null ? "USD"
            : d980.getSubfieldsAsString(CURRENCY);
  }

  public String acquisitionMethod() {
    return d980.getSubfieldsAsString(ACQUISITION_METHOD) == null ? "Purchase"
            : d980.getSubfieldsAsString(ACQUISITION_METHOD);
  }

  public String selector() {
    return d980.getSubfieldsAsString(SELECTOR);
  }

  public boolean hasSelector() {
    return selector() != null && !selector().isEmpty();
  }
  public String vendorAccount() {
    return d980.getSubfieldsAsString(VENDOR_ACCOUNT);
  }

  public boolean hasVendorAccount() {
    return vendorAccount() != null && !vendorAccount().isEmpty();
  }

  public String donor() {
    return d980.getSubfieldsAsString(DONOR);
  }

  public boolean hasDonor() {
    return donor() != null && ! donor().isEmpty();
  }
  public String refNumberType() {
    return d980.getSubfieldsAsString(REF_NUMBER_TYPE);
  }

  public boolean hasRefNumberType() {
    return refNumberType() != null && !refNumberType().isEmpty();
  }

  public String rush() {
    return d980.getSubfieldsAsString(RUSH);
  }

  public String expenseClassCode() {
    return d980.getSubfieldsAsString(EXPENSE_CLASS_CODE);
  }

  public boolean hasExpenseClassCode() {
    return expenseClassCode() != null && !expenseClassCode().isEmpty();
  }

  public String userLimit() {
    return has856 ? first856.getSubfieldsAsString(USER_LIMIT) : null;
  }

  public boolean hasUserLimit() {
    return userLimit() != null && !userLimit().isEmpty();
  }

  public String accessProviderCode() {
    return d980.getSubfieldsAsString(ACCESS_PROVIDER_CODE) == null ? vendorCode()
            : d980.getSubfieldsAsString(ACCESS_PROVIDER_CODE);
  }

  public String billTo() {
    return d980.getSubfieldsAsString(BILL_TO);
  }

  public boolean hasBillTo() {
    return billTo() != null && ! billTo().isEmpty();
  }

  public String receivingNote() {
    return d980.getSubfieldsAsString(RECEIVING_NOTE);
  }

  public boolean hasReceivingNote() {
    return receivingNote() != null && ! receivingNote().isEmpty();
  }

  public String description() {
    return d980.getSubfieldsAsString(DESCRIPTION);
  }

  public boolean hasDescription() {
    return description() != null && ! description().isEmpty();
  }

  public boolean hasInvoice() {
    return (d980.getSubfieldsAsString("h") != null);
  }

  public String vendorInvoiceNo() {
    return d980.getSubfieldsAsString(VENDOR_INVOICE_NO);
  }

  public String invoiceDate() {
    return d980.getSubfieldsAsString(INVOICE_DATE);
  }

  public String subTotal() {
    return d980.getSubfieldsAsString(SUB_TOTAL);
  }
}
