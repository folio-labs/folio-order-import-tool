package org.olf.folio.order.entities.inventory;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.entities.FolioEntity;

@CanIgnoreReturnValue
public class Item extends FolioEntity {
  public static final String P_NOTES = "notes";
  public static final String P_BARCODE = "barcode";
  public static final String P_MATERIAL_TYPE_ID = "materialTypeId";
  public static final String P_PERMANENT_LOAN_TYPE = "permanentLoanType";
  public static final String P_COPY_NUMBER = "copyNumber";

  public static Item fromJson (JSONObject itemJson) {
    Item item = new Item();
    item.json = itemJson;
    return item;
  }

  public Item addNote (JSONObject note) {
    if (getNotes() == null) {
      json.put(P_NOTES, new JSONArray());
    }
    getNotes().put(note);
    return this;
  }

  public Item putBarcode (String barcode) {
    return (Item) putString(P_BARCODE, barcode);
  }

  public Item addBookplateNote (BookplateNote note) {
    return addNote(note.asJson());
  }

  public Item putCopyNumber(String copyNumber) {
    return (Item) putString(P_COPY_NUMBER, copyNumber);
  }

  public JSONArray getNotes () {
    return json.getJSONArray(P_NOTES);
  }

  public Item putPermanentLoanTypeId(String loanTypeId) {
    json.getJSONObject(P_PERMANENT_LOAN_TYPE).put("id", loanTypeId);
    return this;
  }

}
