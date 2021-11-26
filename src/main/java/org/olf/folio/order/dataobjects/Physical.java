package org.olf.folio.order.dataobjects;

import org.olf.folio.order.Constants;
import org.olf.folio.order.MarcRecordMapping;

public class Physical extends JsonDataObject {
  public static final String P_CREATE_INVENTORY = "createInventory";
  public static final String P_MATERIAL_TYPE = "materialType";
  public static final String V_INSTANCE_HOLDING_ITEM = "Instance, Holding, Item";

  public static Physical fromMarcRecord(MarcRecordMapping mappedMarc) {
    return new Physical().putCreateInventory(V_INSTANCE_HOLDING_ITEM);
  }

  public Physical putCreateInventory(String createInventory) {
    return (Physical) putString(P_CREATE_INVENTORY, createInventory);
  }
  public Physical putMaterialType(String materialType) {
    return (Physical) putString(P_MATERIAL_TYPE, materialType);
  }

  public String getCreateInventory() {
    return (String) json.get(P_CREATE_INVENTORY);
  }
  public String getMaterialType () {
    return (String) json.get(P_MATERIAL_TYPE);
  }
  private static String getMaterialTypeId (String materialType) {
    return isUUID(materialType) ? materialType : Constants.MATERIAL_TYPES_MAP.get(materialType);
  }


}
