package org.olf.folio.order;

import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.dataobjects.BookplateNote;
import org.olf.folio.order.dataobjects.HoldingsRecord;
import org.olf.folio.order.dataobjects.Instance;
import org.olf.folio.order.dataobjects.Item;
import org.olf.folio.order.storage.FolioData;

public class DataObjectBuilder {

  public static Instance createAndUpdateInstanceFromJson(JSONObject instanceJson, MarcRecordMapping mappedMarc, Config config) throws Exception {
    return Instance.fromJson(instanceJson)
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

  public static HoldingsRecord createAndUpdateHoldingsRecordFromJson(JSONObject holdingsJson, MarcRecordMapping mappedMarc) throws Exception {
    HoldingsRecord holdingsRecord =
            HoldingsRecord.fromJson(holdingsJson)
                    .putElectronicAccess(mappedMarc.getElectronicAccess());
    if (mappedMarc.electronic()) {
      holdingsRecord.putHoldingsTypeId(FolioData.getHoldingsTypeIdByName("Electronic"));
      if (mappedMarc.hasDonor()) {
        holdingsRecord.addBookplateNote(BookplateNote.createElectronicBookplateNote(mappedMarc.donor()));
      }
    }
    return holdingsRecord;
  }

  public static Item createAndUpdateItemFromJson (JSONObject itemJson, MarcRecordMapping mappedMarc) {
    Item item = Item.fromJson(itemJson);
    if (mappedMarc.hasDonor() && !mappedMarc.electronic()) {
      item.addBookplateNote(BookplateNote.createPhysicalBookplateNote(mappedMarc.donor()));
    }

    return item;
  }

}
