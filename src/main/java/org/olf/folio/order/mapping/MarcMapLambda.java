package org.olf.folio.order.mapping;

import org.marc4j.marc.Record;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.folioapis.ValidationLookups;

public class MarcMapLambda extends MarcToFolio {
  protected static final String OBJECT_CODE           = "o";
  protected static final String PROJECT_CODE          = "r";

  public MarcMapLambda(Record record) {
    super(record);
  }

  /**
   * @return null.  Lambda mapping does not use FOLIO-compatible user limits.
   */
  @Override
  public String userLimit() {
    return null;
  }

  @Override
  public boolean hasUserLimit() {
    return false;
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

  public void validate(RecordResult outcome) throws Exception {
    super.validate(outcome);
    if (has980()) {
      if (hasObjectCode()) {
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
  }
}
