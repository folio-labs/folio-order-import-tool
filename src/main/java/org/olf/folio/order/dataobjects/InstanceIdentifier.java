package org.olf.folio.order.dataobjects;


import org.json.JSONArray;
import org.marc4j.marc.DataField;
import org.olf.folio.order.Constants;
import org.olf.folio.order.MarcRecordMapping;

import java.util.List;

import static org.olf.folio.order.MarcRecordMapping.getIdentifierValue;
import static org.olf.folio.order.dataobjects.ProductIdentifier.isNotInvalidIsbnThatShouldBeRemoved;

public class InstanceIdentifier extends JsonDataObject {
  public static final String P_IDENTIFIER_TYPE_ID = "identifierTypeId";
  public static final String P_VALUE = "value";

  public static JSONArray createInstanceIdentifiersFromMarc (MarcRecordMapping mappedMarc) {
    return createInstanceIdentifiersJson(mappedMarc, true,
            Constants.ISBN,
            Constants.INVALID_ISBN,
            Constants.ISSN,
            Constants.INVALID_ISSN,
            Constants.LINKING_ISSN,
            Constants.OTHER_STANDARD_IDENTIFIER,
            Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER,
            Constants.SYSTEM_CONTROL_NUMBER);
  }
  /**
   * Creates a JSON array of identifiers (types and values) on the schema used in Instance records
   * @param includeQualifiers Indication whether to include additional subfields to certain identifier values
   * @param identifierTypeIds One or more identifier types to look up values for in the MARC record
   * @return A JSON array of identifiers
   */
  public static JSONArray createInstanceIdentifiersJson(
          MarcRecordMapping mappedMarc, boolean includeQualifiers, String ...identifierTypeIds) {

      JSONArray identifiersJson = new JSONArray();
      for (String identifierTypeId : identifierTypeIds) {
          List<DataField> identifierFields = mappedMarc.getDataFieldsForIdentifierType(identifierTypeId);
          for (DataField identifierField : identifierFields) {
              String value = getIdentifierValue( identifierTypeId, identifierField, includeQualifiers );
              if (value != null && ! value.isEmpty())
              {
                if (isNotInvalidIsbnThatShouldBeRemoved(identifierTypeId, value)) {
                  identifiersJson.put(new InstanceIdentifier().putValue(value).putIdentifierTypeId(
                          identifierTypeId).asJson());
                }
              }
          }
      }
      return identifiersJson;
  }

  public InstanceIdentifier putIdentifierTypeId (String identifierTypeId) {
    putString(P_IDENTIFIER_TYPE_ID, identifierTypeId);
    return this;
  }

  public InstanceIdentifier putValue (String value) {
    putString(P_VALUE, value);
    return this;
  }


}
