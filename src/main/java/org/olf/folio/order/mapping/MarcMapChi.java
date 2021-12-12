package org.olf.folio.order.mapping;

import org.marc4j.marc.Record;
import org.olf.folio.order.entities.Item;
import org.olf.folio.order.imports.RecordResult;
import org.olf.folio.order.storage.ValidationLookups;

public class MarcMapChi extends BaseMapping {
  protected static final String BARCODE  = "o";

  public MarcMapChi(Record marcRecord) {
    super(marcRecord);
  }

  /**
   * @return 980$o
   */
  public String barcode() {
    return d980.getSubfieldsAsString(BARCODE);
  }

  public boolean hasBarcode() {
    return has(barcode());
  }

  public boolean updateItem () {
    return true;
  }

  public void populateItemFromMarc (Item item) {
    if (super.updateItem()) {
      super.populateItemFromMarc(item);
    }
    if (hasBarcode()) {
      item.putBarcode(barcode());
    }
  }

  public boolean validate (RecordResult outcome) throws Exception {
    super.validate(outcome);
    if (hasBarcode()) {
      outcome.addValidationMessageIfAny(ValidationLookups.validateBarcodeUniqueness(barcode()));
    }
    return !outcome.failedValidation();
  }
}
