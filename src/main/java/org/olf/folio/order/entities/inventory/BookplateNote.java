package org.olf.folio.order.entities.inventory;

import org.olf.folio.order.mapping.Constants;
import org.olf.folio.order.entities.FolioEntity;

public class BookplateNote extends FolioEntity {
  public static final String P_HOLDINGS_NOTE_TYPE_ID = "holdingsNoteTypeId";
  public static final String P_ITEM_NOTE_TYPE_ID = "itemNoteTypeId";
  public static final String P_NOTE = "note";
  public static final String P_STAFF_ONLY = "staffOnly";

  public static BookplateNote createElectronicBookplateNote (String note) {
    return new BookplateNote()
          .putHoldingsNoteTypeId(Constants.HOLDINGS_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE)
          .putNote(note)
          .putStaffOnly(false);
  }

  public static BookplateNote createElectronicBookplateNoteForItem(String note) {
    return new BookplateNote()
            .putItemNoteTypeId(Constants.ITEM_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE)
            .putNote(note)
            .putStaffOnly(false);
  }

  public BookplateNote putHoldingsNoteTypeId (String holdingsNoteTypeId) {
    return (BookplateNote) putString(P_HOLDINGS_NOTE_TYPE_ID, holdingsNoteTypeId);
  }
  public BookplateNote putItemNoteTypeId (String itemNoteTypeId) {
    return (BookplateNote) putString(P_ITEM_NOTE_TYPE_ID, itemNoteTypeId);
  }
  public BookplateNote putNote(String note) {
    return (BookplateNote) putString(P_NOTE, note);
  }
  public BookplateNote putStaffOnly(boolean staffOnly) {
    return (BookplateNote) putBoolean(P_STAFF_ONLY, staffOnly);
  }
}
