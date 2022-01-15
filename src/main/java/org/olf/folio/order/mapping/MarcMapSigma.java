package org.olf.folio.order.mapping;

import org.marc4j.marc.Record;
import org.olf.folio.order.entities.inventory.Item;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.folioapis.FolioData;
import org.olf.folio.order.folioapis.ValidationLookups;

public class MarcMapSigma extends MarcToFolio {

  protected static final String LOCATION = "a";
  protected static final String MATERIAL_TYPE = "d";
  protected static final String QUANTITY = "q";
  protected static final String LOAN_TYPE = "r";


  public MarcMapSigma(Record marcRecord) {
    super(marcRecord);
  }

  /**
   * @return 980$a
   */
  public String locationName() {
    return d980.getSubfieldsAsString(LOCATION);
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

  public int quantity () {
    String quantity = d980.getSubfieldsAsString(QUANTITY);
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
    return d980.getSubfieldsAsString(LOAN_TYPE);
  }

  // TODO: default to config loan type? mod-orders already does defaulting
  public String loanTypeId () throws Exception {
    if (has(loanType())) {
      return FolioData.getLoanTypeId(loanType());
    } else {
      return null;
    }
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
