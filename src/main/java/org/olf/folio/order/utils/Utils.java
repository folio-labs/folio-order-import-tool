package org.olf.folio.order.utils;

import org.folio.isbn.IsbnUtil;

public class Utils {
  public static boolean isInvalidIsbn (String isbn) {
    return !isValidIsbn(isbn);
  }

  public static boolean isValidIsbn (String isbn) {
    if (isbn.length() == 10) {
      return IsbnUtil.isValid10DigitNumber(isbn);
    } else if (isbn.length() == 13) {
      return IsbnUtil.isValid13DigitNumber(isbn);
    } else {
      return false;
    }
  }
}
