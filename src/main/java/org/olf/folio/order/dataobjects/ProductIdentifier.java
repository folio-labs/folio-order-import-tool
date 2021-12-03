package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.marc.DataField;
import org.olf.folio.order.Config;
import org.olf.folio.order.Constants;
import org.olf.folio.order.MarcRecordMapping;
import org.olf.folio.order.utils.Utils;

import java.util.List;

import static org.olf.folio.order.MarcRecordMapping.getIdentifierValue;

public class ProductIdentifier extends JsonDataObject {
  public static final String P_PRODUCT_ID_TYPE = "productIdType";
  public static final String P_PRODUCT_ID = "productId";

  public static ProductIdentifier fromJson (JSONObject piJson) {
    ProductIdentifier pi = new ProductIdentifier();
    pi.json = piJson;
    return pi;
  }

  public static JSONArray createProductIdentifiersFromMarc (MarcRecordMapping mappedMarc) {
    return createProductIdentifiersJson(mappedMarc,false,
            Constants.ISBN,
            Constants.ISSN,
            Constants.OTHER_STANDARD_IDENTIFIER,
            Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER );

  }
  /**
   * Creates a JSON array of identifiers (types and values) on the schema used in Order Storage, product IDs
   * @param includeQualifiers Indication whether to include additional subfields to certain identifier values
   * @param identifierTypeIds One or more identifier types to look up values for in the MARC record
   * @return A JSON array of identifiers
   */
  public static JSONArray createProductIdentifiersJson(MarcRecordMapping mappedMarc, boolean includeQualifiers, String ...identifierTypeIds) {
      JSONArray identifiersJson = new JSONArray();
      for (String identifierTypeId : identifierTypeIds) {
          List<DataField> identifierFields = mappedMarc.getDataFieldsForIdentifierType(identifierTypeId);
          for (DataField identifierField : identifierFields) {
              String value = getIdentifierValue( identifierTypeId, identifierField, includeQualifiers );
              if (value != null && ! value.isEmpty()) {
                  if (isNotInvalidIsbnThatShouldBeRemoved(identifierTypeId, value)) {
                    identifiersJson.put(new ProductIdentifier()
                            .putProductId(value)
                            .putProductIdType(identifierTypeId).asJson());
                  }
              }
          }
      }
      return identifiersJson;
  }

  public static boolean isNotInvalidIsbnThatShouldBeRemoved (String identifierTypeId, String value) {
    if (identifierTypeId.equals(Constants.ISBN)) {
      if (Utils.isInvalidIsbn(value)) {
        return !Config.onIsbnInvalidRemoveIsbn;
      }
    }
    return true;
  }

  public ProductIdentifier putProductIdType (String productIdType) {
    putString(P_PRODUCT_ID_TYPE, productIdType);
    return this;
  }

  public String getProductIdType () {
    return getString(P_PRODUCT_ID_TYPE);
  }

  public String getProductId () {
    return getString(P_PRODUCT_ID);
  }

  public ProductIdentifier putProductId (String productId) {
    putString(P_PRODUCT_ID, productId);
    return this;
  }


}
