package org.olf.folio.order;

import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.dataobjects.Instance;
import org.olf.folio.order.storage.FolioData;

public class DataObjectBuilder {

  public static Instance createUpdatedInstance(MarcRecordMapping mappedMarc, JSONObject instanceAsJson, Config config) throws Exception {
    return Instance.fromJson(instanceAsJson)
            .putTitle(mappedMarc.title())
            .putSource(config.importSRS ? Instance.V_MARC : Instance.V_FOLIO)
            .putInstanceTypeId(FolioData.getInstanceTypeId("text"))
            .putIdentifiers(mappedMarc.getInstanceIdentifiers())
            .putContributors(mappedMarc.getContributorsForInstance())
            .putDiscoverySuppress(false)
            .putElectronicAccess(mappedMarc.getElectronicAccess())
            .putNatureOfContentTermIds(new JSONArray())
            .putPrecedingTitles(new JSONArray())
            .putSucceedingTitles(new JSONArray());
  }

}
