package org.olf.folio.order.dataobjects;

public class BookPlateNote extends JsonDataObject {
  public static final String P_HOLDINGS_NOTE_TYPE_ID = "holdingsNoteTypeId";
  public static final String P_ITEM_NOTE_TYPE_ID = "itemNoteTypeId";
  public static final String P_NOTE = "note";
  public static final String P_STAFF_ONLY = "staffOnly";

  public BookPlateNote putHoldingsNoteTypeId (String holdingsNoteTypeId) {
    return (BookPlateNote) putString(P_HOLDINGS_NOTE_TYPE_ID, holdingsNoteTypeId);
  }
  public BookPlateNote putItemNoteTypeId (String itemNoteTypeId) {
    return (BookPlateNote) putString(P_ITEM_NOTE_TYPE_ID, itemNoteTypeId);
  }
  public BookPlateNote putNote(String note) {
    return (BookPlateNote) putString(P_NOTE, note);
  }
  public BookPlateNote putStaffOnly(boolean staffOnly) {
    return (BookPlateNote) putBoolean(P_STAFF_ONLY, staffOnly);
  }
}
