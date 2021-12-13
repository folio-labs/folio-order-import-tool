package org.olf.folio.order.entities.orders;

import org.olf.folio.order.entities.FolioEntity;
import org.olf.folio.order.mapping.MarcToFolio;

public class EResource extends FolioEntity {
  // Constants
  public static final boolean ACTIVATED = false;
  public static final boolean TRIAL = false;

  public static final String P_ACTIVATED = "activated";
  public static final String P_CREATE_INVENTORY = "createInventory";
  public static final String V_INSTANCE_HOLDING = "Instance, Holding";
  public static final String P_USER_LIMIT = "userLimit";
  public static final String P_TRIAL = "trial";
  public static final String P_ACCESS_PROVIDER = "accessProvider";
  public static final String P_URI = "uri";
  public static final String P_LINK_TEXT = "linkText";
  public static final String P_PUBLIC_NOTE = "publicNote";
  public static final String P_RELATIONSHIP_ID = "relationshipId";

  public static EResource fromMarcRecord(MarcToFolio mappedMarc)
          throws Exception {

    return new EResource()
                    .putActivated(ACTIVATED)
                    .putCreateInventory(V_INSTANCE_HOLDING)
                    .putUserLimitIfPresent(mappedMarc.userLimit())
                    .putTrial(TRIAL)
                    .putAccessProvider(mappedMarc.accessProviderUUID());
  }

  public EResource putActivated (boolean activated) {
    return (EResource) putBoolean(P_ACTIVATED, activated);
  }
  public EResource putCreateInventory (String createInventory) {
    return (EResource) putString(P_CREATE_INVENTORY, createInventory);
  }
  public EResource putUserLimit(String userLimit) {
    return (EResource) putString(P_USER_LIMIT, userLimit);
  }
  public EResource putUserLimitIfPresent (String userLimit) {
    return present(userLimit) ? putUserLimit(userLimit) : this;
  }
  public EResource putTrial(boolean trial) {
    return (EResource) putBoolean(P_TRIAL, trial);
  }
  public EResource putAccessProvider (String accessProvider) {
    return (EResource) putString(P_ACCESS_PROVIDER, accessProvider);
  }
  public EResource putUri (String uri) {
    return (EResource) putString(P_URI, uri);
  }
  public EResource putLinkText (String linkText) {
    return (EResource) putString(P_LINK_TEXT, linkText);
  }
  public EResource putPublicNote (String publicNote) {
    return (EResource) putString(P_PUBLIC_NOTE, publicNote);
  }
  public EResource putRelationshipId (String relationshipId) {
    return (EResource) putString(P_RELATIONSHIP_ID, relationshipId);
  }

}
