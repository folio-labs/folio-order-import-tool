package org.olf.folio.order.entities.orders;

import org.json.JSONArray;
import org.olf.folio.order.entities.FolioEntity;
import org.olf.folio.order.mapping.MarcMapLambda;

public class Tags extends FolioEntity {
  public static final String P_TAG_LIST = "tagList";

  public static Tags fromMarcRecord(MarcMapLambda mappedMarc) {
    JSONArray tagList = new JSONArray();
    if (mappedMarc.hasObjectCode()) {
      tagList.put(mappedMarc.objectCode());
    }
    if (mappedMarc.hasProjectCode()) {
      tagList.put(mappedMarc.projectCode());
    }
    return (Tags) (new Tags().putArray(P_TAG_LIST, tagList));
  }

  public JSONArray getTagList () {
    return json.getJSONArray(P_TAG_LIST);
  }
}
