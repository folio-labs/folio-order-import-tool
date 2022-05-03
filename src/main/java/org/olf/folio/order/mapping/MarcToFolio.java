package org.olf.folio.order.mapping;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.olf.folio.order.Config;
import org.olf.folio.order.entities.inventory.BookplateNote;
import org.olf.folio.order.entities.inventory.ElectronicAccessUrl;
import org.olf.folio.order.entities.inventory.HoldingsRecord;
import org.olf.folio.order.entities.inventory.Instance;
import org.olf.folio.order.entities.inventory.InstanceIdentifier;
import org.olf.folio.order.entities.inventory.Item;
import org.olf.folio.order.entities.orders.PoLineLocation;
import org.olf.folio.order.entities.orders.ProductIdentifier;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.folioapis.FolioData;
import org.olf.folio.order.folioapis.ValidationLookups;
import org.olf.folio.order.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.olf.folio.order.mapping.Constants.CONTRIBUTOR_NAME_TYPES_MAP;

public abstract class MarcToFolio {
  Record marcRecord;
  DataField d041;
  DataField d245;
  DataField d250;
  DataField d260;
  DataField d264;
  DataField d336;
  DataField d337;
  DataField d338;
  DataField d490;
  List<VariableField> d880s;
  DataField d980;
  DataField first856;
  boolean has856;

  // Mappings 041
  protected static final String LANGUAGE = "a";

  // Mappings 245
  protected static final String TITLE_ONE = "a";
  protected static final String TITLE_TWO = "b";
  protected static final String TITLE_THREE = "c";
  protected static final String TITLE_NAME_OF_PART = "p";

  // Mappings 250
  protected static final String EDITION = "a";

  // Mappings 260, 264
  protected static final String PUBLISHER        = "b";
  protected static final String PUBLICATION_DATE = "c";

  // Mappings 336
  protected static final String RESOURCE_TYPE = "a";

  // Mappings 337
  protected static final String MEDIA_TYPE = "a";

  // Mappings 338
  protected static final String CARRIER_TYPE = "a";

  // Mappings 880
  protected static final String LINKAGE = "6";

  // Mappings 980
  protected static final String FUND_CODE             = "b";
  protected static final String VENDOR_ITEM_ID        = "c";
  protected static final String DESCRIPTION           = "e";
  protected static final String SELECTOR              = "f";
  protected static final String VENDOR_ACCOUNT        = "g";
  protected static final String VENDOR_INVOICE_NO     = "h";
  protected static final String INVOICE_DATE          = "i";
  protected static final String SUB_TOTAL             = "j";
  protected static final String CURRENCY              = "k";
  protected static final String ACCESS_PROVIDER_CODE  = "l";
  protected static final String PRICE                 = "m";
  protected static final String NOTES                 = "n";
  protected static final String DONOR                 = "p";
  protected static final String BILL_TO               = "s";
  protected static final String ACQUISITION_METHOD    = "t";
  protected static final String REF_NUMBER_TYPE       = "u";
  protected static final String VENDOR_CODE           = "v";
  protected static final String RUSH                  = "w";
  protected static final String RECEIVING_NOTE        = "x";
  protected static final String EXPENSE_CLASS_CODE    = "y";
  protected static final String ELECTRONIC_INDICATOR  = "z";

  // Mappings 856
  protected static final String USER_LIMIT           = "x";

  // Fall-back resource type
  public static final String V_RESOURCE_TYPE = "text";

  protected static final String V_ELECTRONIC = "ELECTRONIC";

  // Two of FOLIO Inventory's default holdings type labels. A library could have changed them.
  public static final String V_HOLDINGS_TYPE_ELECTRONIC = "Electronic";
  public static final String V_HOLDINGS_TYPE_PHYSICAL = "Physical";

  // For reporting missing mandatory mappings in 980
  public static final Map<String, String> FOLIO_TO_MARC_FIELD_MAP = new HashMap<>();
  public static final String FUND_CODE_LABEL = "Fund code";
  public static final String PRICE_LABEL = "Price";
  public static final String VENDOR_CODE_LABEL = "Vendor code";
  public static final String MARC_980_B = "980$b";
  public static final String MARC_980_V = "980$v";
  public static final String MARC_980_M = "980$m";

  public static final Logger logger = Logger.getLogger(MarcToFolio.class);
  public MarcToFolio(Record marcRecord) {
    this.marcRecord = marcRecord;
    if (FOLIO_TO_MARC_FIELD_MAP.isEmpty()) {
      FOLIO_TO_MARC_FIELD_MAP.put(PRICE_LABEL, MARC_980_M);
      FOLIO_TO_MARC_FIELD_MAP.put(FUND_CODE_LABEL, MARC_980_B);
      FOLIO_TO_MARC_FIELD_MAP.put(VENDOR_CODE_LABEL, MARC_980_V);
    }
    d041 = (DataField) marcRecord.getVariableField("041");
    d245 = (DataField) marcRecord.getVariableField("245");
    d250 = (DataField) marcRecord.getVariableField("250");
    d260 = (DataField) marcRecord.getVariableField("260");
    d264 = (DataField) marcRecord.getVariableField("264");
    d336 = (DataField) marcRecord.getVariableField("336");
    d337 = (DataField) marcRecord.getVariableField("337");
    d338 = (DataField) marcRecord.getVariableField("338");
    d490 = (DataField) marcRecord.getVariableField("490");
    d880s = marcRecord.getVariableFields("880");
    d980 = (DataField) marcRecord.getVariableField("980");
    first856 = getFirst856(marcRecord);
    has856 =  (first856 != null);
  }

  public Record getRecord() {
    return marcRecord;
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

  public boolean has041() {
    return d041 != null;
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

  public boolean has336() {
    return d336 != null;
  }

  public boolean has337() {
    return d338 != null;
  }

  public boolean has338() {
    return d338 != null;
  }
  public boolean hasFormatFields () {
    return has337() && has338();
  }

  public boolean has880s() {
    return d880s != null;
  }

  public String getAlternativeTitle () {
    if (has880s()) {
      for (VariableField d880 : d880s) {
        if (((DataField) d880).getSubfieldsAsString(LINKAGE).startsWith("245")) {
          //noinspection SpellCheckingInspection
          return ((DataField) d880).getSubfieldsAsString("abcp");
        }
      }
    }
    return null;
  }

  public boolean has490() {
    return d490 != null;
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

  /**
   * @return 245$p
   */
  public String nameOfPart() {
    return d245.getSubfieldsAsString(TITLE_NAME_OF_PART);
  }

  public String title() {
    return titleOne()
            + (titleTwo() == null ? "" : " " + titleTwo())
            + (titleThree() == null ? "" : " " + titleThree()
            + (nameOfPart() == null ? "" : " " + nameOfPart()));
  }

  /**
   *
   * @return UUID for type from 336$a or default to UUID for 'text'
   */
  public String instanceTypeId() throws Exception {
    if (has336() && d336.getSubfieldsAsString(RESOURCE_TYPE) != null) {
      String instanceTypeId = FolioData.getInstanceTypeId(d336.getSubfieldsAsString(RESOURCE_TYPE));
      if (instanceTypeId != null) {
        return instanceTypeId;
      }
    }
    // Default to 'text'
    return FolioData.getInstanceTypeId(V_RESOURCE_TYPE);
  }

  public JSONArray instanceFormatIds () throws Exception {
    JSONArray instanceFormats = new JSONArray();
    if (hasFormatFields()) {
      String instanceFormatName =
              d337.getSubfieldsAsString(MEDIA_TYPE)
              + " -- "
              + d338.getSubfieldsAsString(CARRIER_TYPE);
      String instanceFormatId = FolioData.getInstanceFormatId(instanceFormatName);
      instanceFormats.put(instanceFormatId);
    }
    return instanceFormats;
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

  public JSONArray getLanguages () {
    JSONArray languageArray = new JSONArray();
    if (has041()) {
      List<Subfield> languages = d041.getSubfields(LANGUAGE);
      if (languages != null) {
        for (Subfield lang : languages) {
          languageArray.put(lang.getData());
        }
      }
    }
    return languageArray;
  }

  public JSONArray series() {
    JSONArray seriesArray = new JSONArray();
    if (has490()) {
      @SuppressWarnings( "SpellCheckingInspection" )
      String seriesStatement = d490.getSubfieldsAsString("alvx368");
      if (seriesStatement != null && ! seriesStatement.isEmpty()) {
        seriesArray.put(seriesStatement);
      }
    }
    return seriesArray;
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

  public int quantity() {
    return 1;
  }

  /**
   * @return 980$b
   */
  public String fundCode() {
    return d980.getSubfieldsAsString(FUND_CODE);
  }

  /**
   * @return true if 980$b is set
   */
  public boolean hasFundCode() {
    return has(fundCode());
  }

  public String fundId() throws Exception {
    return FolioData.getFundId(fundCode());
  }

  public String budgetId() throws Exception {
    if (has(fundId()) && hasExpenseClassCode()) {
      return FolioData.getBudgetId(fundId(),FolioData.getFiscalYearId(Config.fiscalYearCode));
    } else {
      return null;
    }
  }

  public boolean hasBudgetId() throws Exception {
    return has(budgetId());
  }

  /**
   * @return 980$v
   */
  public String vendorCode() {
    return d980.getSubfieldsAsString(VENDOR_CODE);
  }

  public boolean hasVendorCode() {
    return has(vendorCode());
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

  public boolean hasPrice() {
    return price() != null && !price().isEmpty();
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
    return V_ELECTRONIC.equalsIgnoreCase(electronicIndicator());
  }

  public boolean physical() {
    return !electronic();
  }

  public String currency() {
    return d980.getSubfieldsAsString(CURRENCY) == null ? "USD"
            : d980.getSubfieldsAsString(CURRENCY);
  }

  public String acquisitionMethodValue() {
    return d980.getSubfieldsAsString(ACQUISITION_METHOD) == null ? "Purchase"
            : d980.getSubfieldsAsString(ACQUISITION_METHOD);
  }

  public boolean hasAcquisitionMethod() {
    return has(acquisitionMethodValue());
  }

  /**
   * Current FOLIO platform releases contain versions of the Orders APIs that require a
   * semantic name for the acquisitionMethod, but a coming version of Orders -- now found on
   * FOLIO snapshot -- will require a UUID that is present in an acquisition_method table.
   * This method attempts the UUID look-up first, then falls back to the semantic name from
   * the MARC record.
   * @return A UUID or a semantic label depending on the FOLIO API version.
   */
  public String acquisitionMethodId() throws Exception {
    if (Config.acquisitionMethodsApiPresent) {
      return FolioData.getAcquisitionMethodId(acquisitionMethodValue());
    } else {
      return acquisitionMethodValue();
    }
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

  public String expenseClassId() throws Exception {
    if (hasExpenseClassCode()) {
      return FolioData.getExpenseClassId(expenseClassCode());
    } else {
      return null;
    }
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

  public JSONArray poLineLocations () throws Exception {
    JSONArray locationsJson = new JSONArray();
    PoLineLocation poLoc = new PoLineLocation();
    poLoc.putLocationId(locationId());
    if (electronic()) {
      poLoc.putQuantityElectronic(quantity());
    } else {
      poLoc.putQuantityPhysical(quantity());
    }
    locationsJson.put(poLoc.asJson());
    return locationsJson;
  }

  /**
   * @return 980$j
   */
  public String subTotal() {
    return d980.getSubfieldsAsString(SUB_TOTAL);
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

  public JSONArray getElectronicAccess (String defaultLinkText) {
    return ElectronicAccessUrl.getElectronicAccessFromMarcRecord( this, defaultLinkText);
  }

  public String locationName() {
    return ( electronic() ?
            (Config.importInvoice && hasInvoice()) ?
                    Config.permELocationWithInvoiceImport : Config.permELocationName
            : ( Config.importInvoice && hasInvoice() ) ?
                    Config.permLocationWithInvoiceImport : Config.permLocationName);
  }

  public String locationId() throws Exception {
      return FolioData.getLocationIdByName(locationName());
  }

  public String materialTypeId() throws Exception {
    return Constants.MATERIAL_TYPES_MAP.get(Config.materialType);
  }

  public boolean hasISBN() {
    return !getDataFieldsForIdentifierType(Constants.ISBN).isEmpty();
  }

  public String getISBN() {
    List<DataField> isbnFields =
            getDataFieldsForIdentifierType(Constants.ISBN);
    if (!isbnFields.isEmpty()) {
      return getIdentifierValue(Constants.ISBN, isbnFields.get(0), false);
    } else {
      return null;
    }
  }

  public List<String> applicableProductIdentifierTypeIds() {
    return Arrays.asList(
            Constants.ISBN,
            Constants.ISSN,
            Constants.OTHER_STANDARD_IDENTIFIER,
            Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER);
  }

  public JSONArray productIdentifiers () {
    JSONArray identifiersJson = new JSONArray();
    for (String identifierTypeId : applicableProductIdentifierTypeIds()) {
      for (DataField identifierField : getDataFieldsForIdentifierType(identifierTypeId)) {
        String value = getIdentifierValue( identifierTypeId, identifierField);
        if (has(value) && doIncludeThisIdentifier(identifierTypeId, value)) {
          identifiersJson.put(new ProductIdentifier()
                  .putProductId(value)
                  .putProductIdType(identifierTypeId).asJson());
        }
      }
    }
    return identifiersJson;
  }

  public boolean hasNoApplicableProductIdentifiers() {
    for (String identifierTypeId : applicableProductIdentifierTypeIds()) {
      if (hasIdentifierOfType(identifierTypeId)) {
        return false;
      }
    }
    return true;
  }

  public boolean hasIdentifierOfType (String identifierTypeId) {
    return !getDataFieldsForIdentifierType(identifierTypeId).isEmpty();
  }

  public JSONArray instanceIdentifiers (List<String> identifierTypeIds) {
    JSONArray identifiersJson = new JSONArray();
    for (String identifierTypeId : identifierTypeIds) {
      for (DataField identifierField : getDataFieldsForIdentifierType(identifierTypeId)) {
        String value = getIdentifierValueWithQualifiers( identifierTypeId, identifierField);
        if (has(value) && doIncludeThisIdentifier(identifierTypeId, value)) {
          identifiersJson.put(new InstanceIdentifier().putValue(value).putIdentifierTypeId(
                  identifierTypeId).asJson());
        }
      }
    }
    return identifiersJson;
  }

  public JSONArray instanceIdentifiers () {
    return instanceIdentifiers(Arrays.asList(
            Constants.ISBN,
            Constants.INVALID_ISBN,
            Constants.ISSN,
            Constants.INVALID_ISSN,
            Constants.LINKING_ISSN,
            Constants.OTHER_STANDARD_IDENTIFIER,
            Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER,
            Constants.SYSTEM_CONTROL_NUMBER));
  }

  /**
   * Finds identifier fields in the provided MARC records by tag, subfield tag(s) and possibly indicator2 -- all
   * dependent on the given identifier type
   * @param requestedIdentifierType The Identifier type to find data fields for
   * @return List of identifier fields matching the applicable criteria for the given identifier type
   */
  private List<DataField> getDataFieldsForIdentifierType( String requestedIdentifierType) {
    List<DataField> identifierFields = new ArrayList<>();
    switch(requestedIdentifierType)
    {
      case Constants.ISBN:
        return findIdentifierFieldsByTagAndSubFields( "020", 'a' );
      case Constants.INVALID_ISBN:
        return findIdentifierFieldsByTagAndSubFields(  "020", 'z' );
      case Constants.ISSN:
        return findIdentifierFieldsByTagAndSubFields(  "022", 'a' );
      case Constants.LINKING_ISSN:
        return findIdentifierFieldsByTagAndSubFields(  "022", 'l' );
      case Constants.INVALID_ISSN:
        return findIdentifierFieldsByTagAndSubFields(  "022", "zym".toCharArray() );
      case Constants.OTHER_STANDARD_IDENTIFIER:
        identifierFields.addAll( findIdentifierFieldsByTagAndSubFields(  "024", 'a' ) );
        identifierFields.addAll( findIdentifierFieldsByTagAndSubFields(  "025", 'a' ) );
        return identifierFields;
      case Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER:
        return findIdentifierFieldsByTagAndSubFields(  "028", 'a' );
      case Constants.SYSTEM_CONTROL_NUMBER:
        List<DataField> fields035_1 = findIdentifierFieldsByTagAndSubFields(  "035", 'a' );
        for ( DataField dataField : fields035_1 )
        {
          if ( dataField.getIndicator2() == '9' )
          {
            identifierFields.add( dataField );
          }
        }
        return identifierFields;
      case Constants.OCLC:
        List<DataField> fields035_2 = findIdentifierFieldsByTagAndSubFields(  "035", 'a' );
        for ( DataField dataField : fields035_2 )
        {
          if ( dataField.getIndicator2() != '9' )
          {
            identifierFields.add( dataField );
          }
        }
        return identifierFields;
      default:
        //logger.error("Requested identifier type not recognized when attempting to look up identifier field: " + requestedIdentifierType);
        return identifierFields;
    }
  }

  /**
   * Finds data fields in the MARC record by their tag and filtered by the presence of given subfields
   * @param tagToFind  The tag (field) to look for
   * @param withAnyOfTheseSubFields One or more subfield codes, of which at least one must be present for the field to be included
   * @return A list of Identifier fields matching the given tag and subfield code criteria
   */
  private List<DataField> findIdentifierFieldsByTagAndSubFields( String tagToFind, char ...withAnyOfTheseSubFields) {
    List<DataField> fieldsFound = new ArrayList<>();
    List<VariableField> fieldsFoundForTag = marcRecord.getVariableFields(tagToFind);
    for (VariableField field : fieldsFoundForTag) {
      DataField dataField = (DataField) field;
      for (char subTag : withAnyOfTheseSubFields) {
        if (dataField.getSubfield( subTag ) != null) {
          fieldsFound.add(dataField);
          break;
        }
      }
    }
    return fieldsFound;
  }

  private static String getIdentifierValue(String identifierType, DataField identifierField) {
    return getIdentifierValue(identifierType, identifierField, false);
  }

  private static String getIdentifierValueWithQualifiers(String identifierType, DataField identifierField) {
    return getIdentifierValue(identifierType, identifierField, true);
  }

  /**
   * Looks up the value of the identifier fields, optionally adding additional subfields to the value for given Identifier types
   * Will strip colons and spaces from ISBN value when not including qualifiers.
   * @param identifierType The type of identifier to find the identifier value for
   * @param identifierField  The identifier field to look for the value in
   * @param includeQualifiers Indication whether to add additional subfield(s) to the identifier value
   * @return The resulting identifier value
   */
  private static String getIdentifierValue ( String identifierType, DataField identifierField, boolean includeQualifiers) {
    String identifierValue;
    switch ( identifierType ) {
      case Constants.ISBN:                           // 020 using $a, extend with c,q
      case Constants.ISSN:                           // 022 using $a, extend with c,q
        identifierValue = identifierField.getSubfieldsAsString( "a" );
        if ( includeQualifiers ) {
          if ( identifierField.getSubfield( 'c' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "c" );
          if ( identifierField.getSubfield( 'q' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "q" );
        } else {
          if (Constants.ISBN.equals(identifierType)) {
            identifierValue = getInitialIsbnCharacters(identifierValue);
          }
        }
        break;
      case Constants.INVALID_ISBN:                   // 020 using $z, extend with c,q
        identifierValue = identifierField.getSubfieldsAsString( "z" );
        if ( includeQualifiers ) {
          if ( identifierField.getSubfield( 'c' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "c" );
          if ( identifierField.getSubfield( 'q' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "q" );
        }
        break;
      case Constants.LINKING_ISSN:                   // 022 using $l, extend with c,q
        identifierValue = identifierField.getSubfieldsAsString( "l" );
        if ( includeQualifiers ) {
          if ( identifierField.getSubfield( 'c' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "c" );
          if ( identifierField.getSubfield( 'q' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "q" );
        }
        break;
      case Constants.INVALID_ISSN:                   // 022 using $z,y,m
        identifierValue = "";
        if ( identifierField.getSubfield('z') != null) identifierValue += identifierField.getSubfieldsAsString("z");
        if ( identifierField.getSubfield('y') != null) identifierValue += " " +  identifierField.getSubfieldsAsString("y");
        if ( identifierField.getSubfield('m') != null) identifierValue += " " + identifierField.getSubfieldsAsString("m");
        break;
      case Constants.OTHER_STANDARD_IDENTIFIER:       // 024, 025 using $a
      case Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER: // 028 using $a
      case Constants.SYSTEM_CONTROL_NUMBER:           // 035 using $a (indicator2=9)
      case Constants.OCLC:                            // 035 using $a (other/no indicators)
        identifierValue = identifierField.getSubfieldsAsString("a");
        break;
      default:
        //logger.error("Requested identifier type not recognized when attempting to retrieve identifier value: " + identifierType);
        identifierValue = null;
        break;
    }
    return identifierValue;
  }

  /**
   * Returns a substring from the start of the input containing only valid ISBN characters: digits and 'X'.
   * @param s The potential ISBN
   * @return The resulting potential ISBN cleaned of bad characters
   */
  private static String getInitialIsbnCharacters(String s) {
    String trimmed = s.trim();
    StringBuilder f = new StringBuilder();
    for (int i = 0; i < trimmed.length(); i++)
      if (Character.isDigit(trimmed.charAt(i)) || ('X' == trimmed.charAt(i))) {
        f.append(trimmed.charAt(i));
      } else {
        break;
      }
    return f.length()>0 ? f.toString() : s;
  }

  private static boolean doIncludeThisIdentifier(String identifierTypeId, String value) {
    if (identifierTypeId.equals(Constants.ISBN)) {
      if (Utils.isInvalidIsbn(getInitialIsbnCharacters(value))) {
        return !Config.onIsbnInvalidRemoveIsbn;
      }
    }
    return true;
  }

  public void populateInstance(Instance instance) throws Exception {
    instance.putTitle(title())
            .putIndexTitle(Utils.makeIndexTitle(title()))
            .putSource(Instance.V_FOLIO)
            .putInstanceTypeId(instanceTypeId())
            .putInstanceFormatIds(instanceFormatIds())
            .putLanguages(getLanguages())
            .putEdition(edition())
            .putSeries(series())
            .putIdentifiers(instanceIdentifiers())
            .putContributors(getContributorsForInstance())
            .putDiscoverySuppress(Instance.DISCOVERY_SUPPRESS)
            .putElectronicAccess(getElectronicAccess(Config.textForElectronicResources))
            .putNatureOfContentTermIds(new JSONArray())
            .putPrecedingTitles(new JSONArray())
            .putSucceedingTitles(new JSONArray());
  }

  public void populateHoldingsRecord(HoldingsRecord holdingsRecord) throws Exception {
    holdingsRecord.putElectronicAccess(getElectronicAccess(Config.textForElectronicResources));
    if (electronic()) {
      holdingsRecord.putHoldingsTypeId(FolioData.getHoldingsTypeIdByName(V_HOLDINGS_TYPE_ELECTRONIC));
      if (hasDonor()) {
        holdingsRecord.addBookplateNote(
                BookplateNote.createElectronicBookplateNote(donor()));
      }
    } else {
      holdingsRecord.putHoldingsTypeId(FolioData.getHoldingsTypeIdByName(V_HOLDINGS_TYPE_PHYSICAL));
    }
  }

  public boolean updateItem () {
    return (hasDonor() && !electronic());
  }

  public void populateItem(Item item) throws Exception {
    item.addBookplateNote(BookplateNote.createPhysicalBookplateNote(donor()));
  }

  public void validate(RecordResult outcome) throws Exception {

    outcome.setInputMarcData(this);

    // Sanity check
    if (!has980()) {
      outcome.addValidationMessageIfAny("Record is missing the 980 field");
    } else {
      // Check for mandatory fields. Check that mappings to FOLIO IDs resolve.
      if (!hasFundCode()) {
        outcome.addValidationMessageIfAny("Record is missing required fund code (980$b)");
      } else {
        outcome.addValidationMessageIfAny(ValidationLookups.validateFund(fundCode()));
      }
      if (!hasVendorCode()) {
        outcome.addValidationMessageIfAny("Record is missing required vendor code");
      } else {
        outcome.addValidationMessageIfAny(ValidationLookups.validateOrganization(vendorCode()));
      }
      if (!hasPrice()) {
        outcome.addValidationMessageIfAny("Record is missing required price info (980$m)");
      }
      if (hasBillTo()) {
        outcome.addValidationMessageIfAny(ValidationLookups.validateAddress(billTo()));
      }
      if (hasExpenseClassCode()) {
        if (FolioData.getExpenseClassId(expenseClassCode()) == null) {
          outcome.addValidationMessageIfAny("No expense class with the code (" + expenseClassCode() + ") found in FOLIO.");
        } else {
          if (hasBudgetId()) {
            if (ValidationLookups.validateBudgetExpenseClass(budgetId(), expenseClassId()) != null) {
              outcome.addValidationMessageIfAny(String.format(
                      "No budget expense class found for fund code (%s) and expense class (%s).",
                      fundCode(), expenseClassCode()));
            }
          }
        }
      }
      if (hasAcquisitionMethod()) {
        if (Config.acquisitionMethodsApiPresent) {
          if (FolioData.getAcquisitionMethodId(acquisitionMethodValue()) == null) {
            outcome.addValidationMessageIfAny("No acquisition method with the value (" + acquisitionMethodValue() + ") found in FOLIO.");
          }
        }
      }
      if (Config.importInvoice) {
        outcome.addValidationMessageIfAny(ValidationLookups.validateRequiredValuesForInvoice(title(), getRecord()));
      }

      // Flag issues with ISBN or other identifiers
      if (hasISBN() && Utils.isInvalidIsbn(getISBN())) {
        if (Config.V_ON_ISBN_INVALID_REMOVE_ISBN.equalsIgnoreCase(Config.onIsbnInvalid)) {
          outcome.setFlagIfNotNull(
                  String.format("ISBN %s is not valid. Will remove the ISBN to continue.", getISBN()));
        } else if (Config.V_ON_ISBN_INVALID_REPORT_ERROR.equalsIgnoreCase(Config.onIsbnInvalid)) {
          outcome.addValidationMessageIfAny("ISBN is invalid").markSkipped(Config.onValidationErrorsSKipFailed);
        }
      } else if (hasNoApplicableProductIdentifiers()) {
        String existingInstancesMessage;
        JSONObject instances = FolioData.getInstancesByQuery("title=\"" + cqlEncode(title()) + "\"");
        int totalRecords = instances.getInt("totalRecords");
        if (totalRecords > 0) {
          Instance firstExistingInstance = Instance.fromJson((JSONObject) instances.getJSONArray(FolioData.INSTANCES_ARRAY).get(0));
          existingInstancesMessage = String.format("%s in Inventory with the same title%sHRID %s",
                  ( totalRecords > 1 ? " There are already " + totalRecords + " instances" : " There is already an Instance" ),
                  ( totalRecords > 1 ? ", for example one with " : " and " ), firstExistingInstance.getHrid());
        } else {
          existingInstancesMessage = "Found no existing Instances with that exact title.";
        }
        outcome.setFlagIfNotNull(
                "No applicable product identifiers found in the record. This order import might trigger the creation of a new Instance in FOLIO." + existingInstancesMessage);
      }
    }
  }

  private static String cqlEncode(String title) {
    // Titles with quoted sections AND slashes can throw FOLIO CQL parser off. The query will work without the quotes.
    title = title.replaceAll("\"","");

    // ?, * and ^ are special characters and should be escaped
    title = title.replaceAll("([\\?\\^\\*])", "\\\\$1");

    return title;
  }

  public boolean has (String string) {
    return string != null && !string.isEmpty();
  }

}
