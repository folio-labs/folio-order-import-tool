package org.olf.folio.order.mapping;

import org.marc4j.marc.Record;
import org.olf.folio.order.imports.RecordResult;
import org.olf.folio.order.storage.ValidationLookups;

public class MarcMapLambda extends BaseMapping {
  protected static final String OBJECT_CODE           = "o";
  protected static final String PROJECT_CODE          = "r";

  public MarcMapLambda(Record record) {
    super(record);
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

  public boolean validate(RecordResult outcome) throws Exception {
    super.validate(outcome);
    if (has980()) {
      if (!hasObjectCode()) {
        outcome.setFlagIfNotNull(
                "Object code is required with MARC mapping 'Lambda'" + " but no object code found in the record");
      } else {
        String message = ValidationLookups.validateTag(objectCode());
        if (message != null) {
          outcome.addValidationMessageIfAny("Cannot set the object code: " + message);
        }
      }
      if (hasProjectCode()) {
        String message = ValidationLookups.validateTag(projectCode());
        if (message != null) {
          outcome.addValidationMessageIfAny("Cannot set the project code: " + message);
        }
      }
    }
    return !outcome.failedValidation();
  }
}
