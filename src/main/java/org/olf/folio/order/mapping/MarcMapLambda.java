package org.olf.folio.order.mapping;

import javax.servlet.http.HttpSession;

import org.marc4j.marc.Record;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.folioapis.FolioData;
import org.olf.folio.order.folioapis.ValidationLookups;

public class MarcMapLambda extends MarcToFolio {
  protected static final String OBJECT_CODE           = "o";
  protected static final String PROJECT_CODE          = "r";

  protected HttpSession session;

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
  }

  public void addSessionContext(HttpSession session) {
    this.session = session;
  }

  public String assignedTo() {
    logger.info("retrieving username from session: " + this.session.getAttribute("username"));
    String username = (String)this.session.getAttribute("username");
    try {
      return FolioData.getUserIdByUsername(username);
    }
    catch (Exception e) {
      logger.error("Error retrieving user UUID for username: " + username, e);
      return null;
    }
  }

}
