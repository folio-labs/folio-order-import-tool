package org.olf.folio.order.mapping;

import org.json.JSONArray;
import org.marc4j.marc.Record;
import org.olf.folio.order.entities.inventory.Item;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.folioapis.ValidationLookups;

import java.util.List;
import java.util.Arrays;

public class MarcMapChi extends MarcToFolio {
  public static final String V_COPY_NUMBER = "c.1";
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
    return (!electronic());
  }

  public JSONArray instanceIdentifiers () {
    return instanceIdentifiers(Arrays.asList(
            Constants.ISBN,
            Constants.INVALID_ISBN,
            Constants.ISSN,
            Constants.INVALID_ISSN,
            Constants.LINKING_ISSN,
            Constants.OTHER_STANDARD_IDENTIFIER,
            Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER,
            Constants.SYSTEM_CONTROL_NUMBER,
            Constants.OCLC));
  }

  public void populateItem(Item item) throws Exception{
    if (super.updateItem()) {
      super.populateItem(item);
    }
    if (hasBarcode()) {
      item.putBarcode(barcode());
    }
    item.putCopyNumber(V_COPY_NUMBER);
  }

  public List<String> applicableProductIdentifierTypeIds() {
    return List.of(Constants.ISBN);
  }

  public void validate (RecordResult outcome) throws Exception {
    super.validate(outcome);
    if (hasBarcode()) {
      outcome.addValidationMessageIfAny(ValidationLookups.validateBarcodeUniqueness(barcode()));
    }
  }
}
