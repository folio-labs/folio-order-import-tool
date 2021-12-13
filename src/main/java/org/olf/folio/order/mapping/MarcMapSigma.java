package org.olf.folio.order.mapping;

import org.marc4j.marc.Record;
import org.olf.folio.order.imports.RecordResult;
import org.olf.folio.order.storage.FolioData;
import org.olf.folio.order.storage.ValidationLookups;

public class MarcMapSigma extends MarcToFolio {

  protected static final String LOCATION = "a";
  protected static final String MATERIAL_TYPE = "d";

  public MarcMapSigma(Record marcRecord) {
    super(marcRecord);
  }

  /**
   * @return 980$a
   */
  public String location() {
    return d980.getSubfieldsAsString(LOCATION);
  }

  public String locationId() throws Exception {
    // TODO: fallback to config?
    return FolioData.getLocationIdByName(location());
  }

  public boolean hasLocation() {
    return has(location());
  }

  public boolean hasLocationId() throws Exception {
    return has(locationId());
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

  public boolean updateItem () {
    return true;
  }

  public boolean validate(RecordResult outcome) throws Exception {
    super.validate(outcome);
    if (has980()) {
      if (! hasLocation()) {
        outcome.addValidationMessageIfAny("Record is missing location (looked in 980$a");
      } else {
        outcome.addValidationMessageIfAny(ValidationLookups.validateLocationName(location()));
      }
      if (has(materialType())) {
        outcome.addValidationMessageIfAny(ValidationLookups.validateMaterialTypeName(materialType()));
      }
    }
    return !outcome.failedValidation();
  }
}
