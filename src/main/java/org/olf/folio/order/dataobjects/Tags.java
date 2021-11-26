package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.olf.folio.order.MarcRecordMapping;

public class Tags extends JsonDataObject {
  public static final String tagList = "tagList";

  public static Tags fromMarcRecord(MarcRecordMapping mappedMarc) {
    JSONArray tagList = new JSONArray();
    if (mappedMarc.hasObjectCode()) {
      tagList.put(mappedMarc.objectCode());
    }
    if (mappedMarc.hasProjectCode()) {
      tagList.put(mappedMarc.projectCode());
    }
    return (Tags) (new Tags().putArray("tagList", tagList));
  }
}
