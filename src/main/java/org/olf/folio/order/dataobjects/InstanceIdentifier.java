package org.olf.folio.order.dataobjects;

public class InstanceIdentifier extends JsonDataObject {
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
