package org.olf.folio.order.dataobjects;

import org.json.JSONObject;
import org.olf.folio.order.Config;
import org.olf.folio.order.Constants;
import org.olf.folio.order.mapping.BaseMapping;
import org.olf.folio.order.mapping.MarcMapSigma;

public class Physical extends JsonDataObject {
  public static final String P_CREATE_INVENTORY = "createInventory";
  public static final String P_MATERIAL_TYPE = "materialType";
  public static final String V_INSTANCE_HOLDING_ITEM = "Instance, Holding, Item";

  /**
   * Creates a Physical object for the PoLine, populated with a material type from the
   * startup config.
   * @param mappedMarc not yet used
   */
  public static Physical fromMarcRecord(BaseMapping mappedMarc) throws Exception {
    Physical physical =  new Physical()
            .putCreateInventory(V_INSTANCE_HOLDING_ITEM);
    if (mappedMarc instanceof MarcMapSigma) {
      physical.putMaterialType(((MarcMapSigma) mappedMarc).materialTypeId());
    } else {
      physical.putMaterialType(Constants.MATERIAL_TYPES_MAP.get(Config.materialType));
    }
    return physical;
  }

  public static Physical fromJson(JSONObject physicalJson) {
    Physical physical = new Physical();
    physical.json = physicalJson;
    return physical;
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
    return getString(P_MATERIAL_TYPE);
  }

}
