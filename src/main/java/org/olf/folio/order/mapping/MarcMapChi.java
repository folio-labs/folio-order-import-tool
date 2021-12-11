package org.olf.folio.order.mapping;

import org.marc4j.marc.Record;

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

}
