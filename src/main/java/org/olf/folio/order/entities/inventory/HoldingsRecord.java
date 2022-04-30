package org.olf.folio.order.entities.inventory;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.entities.FolioEntity;

@CanIgnoreReturnValue
public class HoldingsRecord extends FolioEntity {
  // Property names
  public static final String P_ID = "id";
  public static final String P_ELECTRONIC_ACCESS = "electronicAccess";
  public static final String P_HOLDINGS_TYPE_ID = "holdingsTypeId";
  public static final String P_NOTES = "notes";

  public static HoldingsRecord fromJson(JSONObject holdingsJson) {
    HoldingsRecord holdingsRecord = new HoldingsRecord();
    holdingsRecord.json = holdingsJson;
    return holdingsRecord;
  }

  public HoldingsRecord putElectronicAccess ( JSONArray electronicAccess) {
    return (HoldingsRecord) putArray(P_ELECTRONIC_ACCESS, electronicAccess);
  }

  public HoldingsRecord putHoldingsTypeId (String holdingsTypeId) {
    return (HoldingsRecord) putString(P_HOLDINGS_TYPE_ID, holdingsTypeId);
  }

  public HoldingsRecord putNotes (JSONArray notes) {
    return (HoldingsRecord) putArray(P_NOTES, notes);
  }

  public HoldingsRecord addNote (JSONObject note) {
    if (getNotes() == null) {
      json.put(P_NOTES, new JSONArray());
    }
    getNotes().put(note);
    return this;
  }

  public HoldingsRecord addBookplateNote (BookplateNote note) {
    return addNote(note.asJson());
  }

  public JSONArray getNotes () {
    return json.getJSONArray(P_NOTES);
  }

}
