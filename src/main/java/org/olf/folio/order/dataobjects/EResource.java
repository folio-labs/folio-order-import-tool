package org.olf.folio.order.dataobjects;

import org.olf.folio.order.MarcRecordMapping;

public class EResource extends JsonDataObject {

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

  public EResource putActivated (boolean activated) {
    return (EResource) putBoolean(P_ACTIVATED, activated);
  }
  public EResource putCreateInventory (String createInventory) {
    return (EResource) putString(P_CREATE_INVENTORY, createInventory);
  }
  public EResource putUserLimit(String userLimit) {
    return (EResource) putString(P_USER_LIMIT, userLimit);
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



  public static EResource createEResource (MarcRecordMapping mappedMarc)
  throws Exception {
    EResource eResource =
            new EResource()
                    .putActivated(false)
                    .putCreateInventory(V_INSTANCE_HOLDING);
    if (mappedMarc.hasUserLimit()) {
      eResource.putUserLimit(mappedMarc.userLimit());
    }
    eResource.putTrial(false)
            .putAccessProvider(mappedMarc.accessProviderUUID());
    return eResource;
  }

}
