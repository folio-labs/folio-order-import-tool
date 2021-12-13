package org.olf.folio.order.entities.inventory;

import org.olf.folio.order.entities.FolioEntity;

public class InstanceIdentifier extends FolioEntity {
  public static final String P_IDENTIFIER_TYPE_ID = "identifierTypeId";
  public static final String P_VALUE = "value";

  public InstanceIdentifier putIdentifierTypeId (String identifierTypeId) {
    putString(P_IDENTIFIER_TYPE_ID, identifierTypeId);
    return this;
  }

  public InstanceIdentifier putValue (String value) {
    putString(P_VALUE, value);
    return this;
  }


}
