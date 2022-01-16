package org.olf.folio.order.mapping;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.olf.folio.order.entities.inventory.Item;
import org.olf.folio.order.entities.orders.PoLineLocation;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.folioapis.FolioData;
import org.olf.folio.order.folioapis.ValidationLookups;

import java.util.ArrayList;
import java.util.List;

public class MarcMapSigma extends MarcToFolio {

  List<DataField> d980s = new ArrayList<>();

  protected static final String LOCATION = "a";
  protected static final String MATERIAL_TYPE = "d";
  protected static final String QUANTITY = "q";
  protected static final String LOAN_TYPE = "r";

  public static final Logger logger = Logger.getLogger(MarcToFolio.class);
  public MarcMapSigma(Record marcRecord) {
    super(marcRecord);
    setAll980s();
  }

  private void setAll980s() {
    logger.debug("Caching all 980s from MARC file");
    List<VariableField> variableFields = marcRecord.getVariableFields("980");
    for (VariableField field : variableFields) {
      d980s.add((DataField) field);
    }
  }

  /**
   * @return 980$a
   */
  public String locationName(int i) {
    return d980s.get(i).getSubfieldsAsString(LOCATION);
  }

  public String locationId(int i) throws Exception {
    return FolioData.getLocationIdByName(locationName(i));
  }

  public boolean hasLocation() {
    return has(locationName());
  }

  public String materialType () {
    return d980.getSubfieldsAsString(MATERIAL_TYPE);
  }

  public String materialTypeId () throws Exception {
    // TODO: fallback to config?
    if (has(materialType())) {
      return FolioData.getMaterialTypeId(materialType());
    } else {
      return null;
    }
  }

  public int quantity (int i) {
    logger.debug("Getting quantity from multi-980 record");
    String quantity = d980s.get(i).getSubfieldsAsString(QUANTITY);
    logger.debug("... quantity is [" + quantity + "]");
    if (has(quantity)) {
      try {
        return Integer.parseInt(quantity);
      } catch (NumberFormatException nfe) {
        return -1;
      }
    } else {
      return 1;
    }
  }

  public String loanType () {
    return d980s.get(0).getSubfieldsAsString(LOAN_TYPE);
  }

  // TODO: default to config loan type? mod-orders already does defaulting
  public String loanTypeId () throws Exception {
    if (has(loanType())) {
      return FolioData.getLoanTypeId(loanType());
    } else {
      return null;
    }
  }

  public JSONArray poLineLocations () throws Exception {
    JSONArray locationsJson = new JSONArray();
    for (int f=0; f<d980s.size(); f++) {
      PoLineLocation poLoc = new PoLineLocation();
      logger.debug("980 #" + f + ", setting location " + locationId(f));
      poLoc.putLocationId(locationId(f));
      if (electronic(f)) {
        poLoc.putQuantityElectronic(quantity(f));
      } else {
        poLoc.putQuantityPhysical(quantity(f));
      }
      locationsJson.put(poLoc.asJson());
    }
    return locationsJson;
  }

  public int quantity() {
    int quantity = 0;
    for (int i=0; i<d980s.size(); i++) {
      quantity+=quantity(i);
    }
    return quantity;
  }

  public boolean electronic (int i) {
    return V_ELECTRONIC.equalsIgnoreCase(electronicIndicator(i));
  }

  public String electronicIndicator (int i) {
    return d980s.get(i).getSubfieldsAsString(ELECTRONIC_INDICATOR);
  }

  public void populateItem(Item item) throws Exception {
    super.populateItem(item);
    item.putPermanentLoanTypeId(loanTypeId());
  }

  public boolean updateItem () {
    return true;
  }

  public void validate(RecordResult outcome) throws Exception {
    super.validate(outcome);
    if (has980()) {
      if (! hasLocation()) {
        outcome.addValidationMessageIfAny("Record is missing location (looked in 980$a)");
      } else {
        outcome.addValidationMessageIfAny(ValidationLookups.validateLocationName(locationName()));
      }
      if (has(materialType())) {
        outcome.addValidationMessageIfAny(ValidationLookups.validateMaterialTypeName(materialType()));
      } else {
        outcome.addValidationMessageIfAny("Record is missing material type (looked in 980$d)");
      }
    }
  }
}
