package org.olf.folio.order;

import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.olf.folio.order.dataobjects.ElectronicAccessUrl;
import org.olf.folio.order.dataobjects.Identifier;
import org.olf.folio.order.storage.FolioData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.olf.folio.order.Constants.CONTRIBUTOR_NAME_TYPES_MAP;

public class MarcRecordMapping {

  Record marcRecord;
  DataField d245;
  DataField d250;
  DataField d260;
  DataField d264;
  DataField d980;
  DataField first856;
  boolean has856;

  // Mappings 245
  private static final String TITLE_ONE = "a";
  private static final String TITLE_TWO = "b";
  private static final String TITLE_THREE = "c";

  // Mappings 250
  private static final String EDITION = "a";

  // Mappings 260, 264
  private static final String PUBLISHER        = "b";
  private static final String PUBLICATION_DATE = "c";

  // Mappings 980
  private static final String FUND_CODE             = "b";
  private static final String VENDOR_ITEM_ID        = "c";
  private static final String DESCRIPTION           = "e";
  private static final String SELECTOR              = "f";
  private static final String VENDOR_ACCOUNT        = "g";
  private static final String VENDOR_INVOICE_NO     = "h";
  private static final String INVOICE_DATE          = "i";
  private static final String SUB_TOTAL             = "j";
  private static final String CURRENCY              = "k";
  private static final String ACCESS_PROVIDER_CODE  = "l";
  private static final String PRICE                 = "m";
  private static final String NOTES                 = "n";
  private static final String OBJECT_CODE           = "o";
  private static final String DONOR                 = "p";
  private static final String PROJECT_CODE          = "r";
  private static final String BILL_TO               = "s";
  private static final String ACQUISITION_METHOD    = "t";
  private static final String REF_NUMBER_TYPE       = "u";
  private static final String VENDOR_CODE           = "v";
  private static final String RUSH                  = "w";
  private static final String RECEIVING_NOTE        = "x";
  private static final String EXPENSE_CLASS_CODE    = "y";
  private static final String ELECTRONIC_INDICATOR  = "z";

  // Mappings 856
  private static final String USER_LIMIT           = "x";

  public MarcRecordMapping(Record marcRecord) {
    this.marcRecord = marcRecord;
    d245 = (DataField) marcRecord.getVariableField("245");
    d250 = (DataField) marcRecord.getVariableField("250");
    d260 = (DataField) marcRecord.getVariableField("260");
    d264 = (DataField) marcRecord.getVariableField("264");
    d980 = (DataField) marcRecord.getVariableField("980");
    first856 = getFirst856(marcRecord);
    has856 =  (first856 != null);
  }

  public String controlNumber() {
    return marcRecord.getControlNumber();
  }

  private DataField getFirst856 (Record record) {
    List<VariableField> fields =  record.getVariableFields("856");
    if (fields != null && !fields.isEmpty()) {
      return (DataField) fields.get(0);
    } else {
      return null;
    }
  }

  public List<DataField> getAll856s () {
    List<DataField> d856List = new ArrayList<>();
    List<VariableField> fields = marcRecord.getVariableFields("856");
    if (fields != null) {
      for (VariableField field : fields) {
        d856List.add((DataField) field);
      }
    }
    return d856List;
  }

  public boolean has250() {
    return d250 != null;
  }

  public boolean has260() {
    return d260 != null;
  }

  public boolean has264() {
    return d264 != null;
  }

  public boolean has980() {
    return d980 != null;
  }

  /**
   * @return 245$a
   */
  public String titleOne() {
    return d245.getSubfieldsAsString(TITLE_ONE);
  }

  /**
   * @return 245$b
   */
  public String titleTwo() {
    return d245.getSubfieldsAsString(TITLE_TWO);
  }

  /**
   * @return 245$c
   */
  public String titleThree() {
    return d245.getSubfieldsAsString(TITLE_THREE);
  }

  public String title() {
    return titleOne()
            + (titleTwo() == null ? "" : " " + titleTwo())
            + (titleThree() == null ? "" : " " + titleThree());
  }

  /**
   * @return 250$a or null
   */
  public String edition() {
    return (has250() ? d250.getSubfieldsAsString(EDITION) : null);
  }

  public boolean hasEdition() {
    return edition() != null && ! edition().isEmpty();
  }

  public String publisher(String field) {
    if (field != null && Arrays.asList("260","264").contains(field)) {
      if (field.equals("260")) {
        return has260() ? d260.getSubfieldsAsString(PUBLISHER) : null;
      } else {
        return has264() ? d264.getSubfieldsAsString(PUBLISHER) : null;
      }
    } else {
      if (has260()) {
        return d260.getSubfieldsAsString(PUBLISHER);
      } else if (has264()) {
        return d264.getSubfieldsAsString(PUBLISHER);
      }
    }
    return null;
  }

  public String publicationDate(String field) {
    if (field != null && Arrays.asList("260","264").contains(field)) {
      if (field.equals("260")) {
        return has260() ? d260.getSubfieldsAsString(PUBLICATION_DATE) : null;
      } else {
        return has264() ? d264.getSubfieldsAsString(PUBLICATION_DATE) : null;
      }
    } else {
      if (has260()) {
        return d260.getSubfieldsAsString(PUBLICATION_DATE);
      } else if (has264()) {
        return d264.getSubfieldsAsString(PUBLICATION_DATE);
      }
    }
    return null;
  }

  /**
   * @return 980$o
   */
  public String objectCode() {
    return d980.getSubfieldsAsString(OBJECT_CODE);
  }

  public boolean hasObjectCode() {
    return objectCode() != null && !objectCode().isEmpty();
  }

  /**
   * @return 980$r
   */
  public String projectCode() {
    return d980.getSubfieldsAsString(PROJECT_CODE);
  }

  public boolean hasProjectCode() {
    return projectCode() != null && !projectCode().isEmpty();
  }

  /**
   * @return 980$b
   */
  public String fundCode() {
    return d980.getSubfieldsAsString(FUND_CODE);
  }

  public String fundUUID() throws Exception {
    return FolioData.getFundId(fundCode());
  }

  /**
   * @return 980$v
   */
  public String vendorCode() {
    return d980.getSubfieldsAsString(VENDOR_CODE);
  }

  public String vendorUuid() throws Exception {
    return FolioData.getOrganizationId(vendorCode());
  }

  /**
   * @return 980$n
   */
  public String notes() {
    return d980.getSubfieldsAsString(NOTES);
  }

  public boolean hasNotes() {
    return notes() != null && ! notes().isEmpty();
  }

  /**
   * @return 980$m
   */
  public String price() {
    return d980.getSubfieldsAsString(PRICE);
  }

  /**
   * @return 980$z
   */
  public String electronicIndicator() {
    return d980.getSubfieldsAsString(ELECTRONIC_INDICATOR);
  }

  /**
   * @return 980$c
   */
  public String vendorItemId() {
    return d980.getSubfieldsAsString(VENDOR_ITEM_ID);
  }

  public boolean hasVendorItemId() {
    return vendorItemId() != null && !vendorItemId().isEmpty();
  }

  public boolean electronic() {
    return "ELECTRONIC".equalsIgnoreCase(electronicIndicator());
  }

  public boolean physical() {
    return !electronic();
  }

  public String currency() {
    return d980.getSubfieldsAsString(CURRENCY) == null ? "USD"
            : d980.getSubfieldsAsString(CURRENCY);
  }

  public String acquisitionMethod() {
    return d980.getSubfieldsAsString(ACQUISITION_METHOD) == null ? "Purchase"
            : d980.getSubfieldsAsString(ACQUISITION_METHOD);
  }

  /**
   * @return 980$f
   */
  public String selector() {
    return d980.getSubfieldsAsString(SELECTOR);
  }

  public boolean hasSelector() {
    return selector() != null && !selector().isEmpty();
  }

  /**
   * @return 980$g
   */
  public String vendorAccount() {
    return d980.getSubfieldsAsString(VENDOR_ACCOUNT);
  }

  public boolean hasVendorAccount() {
    return vendorAccount() != null && !vendorAccount().isEmpty();
  }

  /**
   * @return 980$p
   */
  public String donor() {
    return d980.getSubfieldsAsString(DONOR);
  }

  public boolean hasDonor() {
    return donor() != null && ! donor().isEmpty();
  }

  /**
   * @return 980$u
   */
  public String refNumberType() {
    return d980.getSubfieldsAsString(REF_NUMBER_TYPE);
  }

  public boolean hasRefNumberType() {
    return refNumberType() != null && !refNumberType().isEmpty();
  }

  /**
   * @return 980$w
   */
  public boolean rush() {
    return "RUSH".equalsIgnoreCase(d980.getSubfieldsAsString(RUSH));
  }

  /**
   * @return 980$y
   */
  public String expenseClassCode() {
    return d980.getSubfieldsAsString(EXPENSE_CLASS_CODE);
  }

  public boolean hasExpenseClassCode() {
    return expenseClassCode() != null && !expenseClassCode().isEmpty();
  }

  public String getExpenseClassUUID() throws Exception {
    return FolioData.getExpenseClassId(expenseClassCode());
  }

  /**
   * @return 856$x or null
   */
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

  public String accessProviderUUID() throws Exception {
    return FolioData.getOrganizationId(accessProviderCode());
  }

  /**
   * @return 980$s
   */
  public String billTo() {
    return d980.getSubfieldsAsString(BILL_TO);
  }

  public boolean hasBillTo() {
    return billTo() != null && ! billTo().isEmpty();
  }

  public String billToUuid() throws Exception {
    return FolioData.getAddressIdByName(billTo());
  }

  /**
   * @return 980$x
   */
  public String receivingNote() {
    return d980.getSubfieldsAsString(RECEIVING_NOTE);
  }

  public boolean hasReceivingNote() {
    return receivingNote() != null && ! receivingNote().isEmpty();
  }

  /**
   * @return 980$e
   */
  public String description() {
    return d980.getSubfieldsAsString(DESCRIPTION);
  }

  public boolean hasDescription() {
    return description() != null && ! description().isEmpty();
  }

  public boolean hasInvoice() {
    return (d980.getSubfieldsAsString("h") != null);
  }

  /**
   * @return 980$h
   */
  public String vendorInvoiceNo() {
    return d980.getSubfieldsAsString(VENDOR_INVOICE_NO);
  }

  /**
   * @return 980$i
   */
  public String invoiceDate() {
    return d980.getSubfieldsAsString(INVOICE_DATE);
  }

  /**
   * @return 980$j
   */
  public String subTotal() {
    return d980.getSubfieldsAsString(SUB_TOTAL);
  }

  public JSONArray getInstanceIdentifiers(boolean includeQualifiers, String ...identifierTypeIds) {
    return Identifier.createInstanceIdentifiersJson(this.marcRecord, includeQualifiers, identifierTypeIds);
  }

  public JSONArray getInstanceIdentifiers() {
    return getInstanceIdentifiers(true, Constants.ISBN,
            Constants.INVALID_ISBN,
            Constants.ISSN,
            Constants.INVALID_ISSN,
            Constants.LINKING_ISSN,
            Constants.OTHER_STANDARD_IDENTIFIER,
            Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER,
            Constants.SYSTEM_CONTROL_NUMBER);
  }

  public JSONArray getProductIdentifiers(boolean includeQualifiers, String ...identifierTypeIds) {
    return Identifier.createProductIdentifiersJson(this.marcRecord, includeQualifiers, identifierTypeIds);
  }

  public JSONArray getProductIdentifiers() {
    return getProductIdentifiers(false,
            Constants.ISBN,
            Constants.ISSN,
            Constants.OTHER_STANDARD_IDENTIFIER,
            Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER );
  }

  public JSONArray getContributorsForOrderLine() throws Exception {
    return getContributors(true);
  }

  public JSONArray getContributorsForInstance () throws Exception {
    return getContributors(false);
  }

  private JSONArray getContributors(boolean buildForOrderLine) throws Exception {
    JSONArray contributors = new JSONArray();
    List<DataField> fields = marcRecord.getDataFields();
    for (DataField field : fields) {
      if (field.getTag().equalsIgnoreCase("100") || field.getTag().equalsIgnoreCase("700")) {
        if (buildForOrderLine) {
          contributors.put(makeContributorForOrderLine(field, "Personal name"));
        } else {
          contributors.put(makeContributor(field, "Personal name",
                  new String[] {"a", "b", "c", "d", "f", "g", "j", "k", "l", "n", "p", "t", "u"}));
        }
      } else if (( field.getTag().equals("110") || field.getTag().equals(
              "710") ) && buildForOrderLine) {
        contributors.put(makeContributorForOrderLine(field, "Corporate name"));
      } else if (( field.getTag().equals("111") || field.getTag().equals(
              "711") ) && buildForOrderLine) {
        contributors.put(makeContributorForOrderLine(field, "Meeting name"));
      }
    }
    return contributors;
  }

  private JSONObject makeContributorForOrderLine(DataField field, String contributorNameType) {
    Subfield subfield = field.getSubfield( 'a' );
    JSONObject contributor = new JSONObject();
    contributor.put("contributor", subfield.getData());
    contributor.put("contributorNameTypeId", CONTRIBUTOR_NAME_TYPES_MAP.get(contributorNameType));
    return contributor;
  }

  private JSONObject makeContributor( DataField field, String name_type, String[] subfieldArray) throws Exception {
    List<String> list = Arrays.asList(subfieldArray);
    JSONObject contributor = new JSONObject();
    contributor.put("name", "");
    contributor.put("contributorNameTypeId", CONTRIBUTOR_NAME_TYPES_MAP.get(name_type));
    List<Subfield> subfields =  field.getSubfields();
    Iterator<Subfield> subfieldIterator = subfields.iterator();
    String contributorName = "";
    while (subfieldIterator.hasNext()) {
      Subfield subfield = subfieldIterator.next();
      String subfieldAsString = String.valueOf(subfield.getCode());
      if (subfield.getCode() == '4') {
        if (FolioData.getContributorTypeIdByName(subfield.getData()) != null) {
          contributor.put("contributorTypeId", FolioData.getContributorTypeIdByName(subfield.getData()));
        } else {
          contributor.put("contributorTypeId", FolioData.getContributorTypeIdByCode("bkp"));
        }
      }
      else if (subfield.getCode() == 'e') {
        contributor.put("contributorTypeText", subfield.getData());
      }
      else if (list.contains(subfieldAsString)) {
        if (!contributorName.isEmpty()) {
          contributorName += ", " + subfield.getData();
        }
        else {
          contributorName +=  subfield.getData();
        }
      }
    }
    contributor.put("name", contributorName);
    return contributor;
  }

  public JSONArray getElectronicAccess () {
    return ElectronicAccessUrl.getElectronicAccessFromMarcRecord(this);
  }

  public boolean hasISBN() {
    return !Identifier.getDataFieldsForIdentifierType(Constants.ISBN,this.marcRecord).isEmpty();
  }

  public String getISBN() {
    List<DataField> isbnFields =
            Identifier.getDataFieldsForIdentifierType(Constants.ISBN, this.marcRecord);
    if (!isbnFields.isEmpty()) {
      return Identifier.getIdentifierValue(Constants.ISBN, isbnFields.get(0), false);
    } else {
      return null;
    }
  }
}
