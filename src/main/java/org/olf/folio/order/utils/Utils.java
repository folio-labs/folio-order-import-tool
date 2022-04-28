package org.olf.folio.order.utils;

import org.folio.isbn.IsbnUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Utils {
  private static final List<String> articles = Arrays.asList(
          "a ", "a'", "al ", "al-", "am ", "an ", "an t-", "ane ", "ang ", "ang mga ", "as ", "az ",
          "bat ", "bir ", "d'", "da ", "das ",
          "de ", "dei ", "dem ", "den ", "der ", "des ", "det ", "di ", "die ", "dos ",
          "e ", "'e ", "een ", "eene ", "egy ", "ei ", "ein ", "eine ", "einem ", "einen ",
          "einer ", "eines ", "eit ", "el ", "el-", "els ", "en ", "enne ", "et ", "ett ", "eyn ", "eyne ",
          "gl'", "gli ",
          "ha-", "hai ", "he ", "hē ", "he-", "heis ", "hen ", "hena ", "henas ", "het ", "hin ",
          "hina ", "hinar ", "hinir ", "hinn ", "hinna ", "hinnar ", "hinni ", "hins ", "hinu ",
          "hinum ", "hið ", "ho ", "hoi ",
          "i ", "ih'", "il ", "il-", "in ", "it ",
          "ka ", "ke ",
          "l'", "l-", "la ", "las ", "le ", "les ", "lh ", "lhi ", "li ", "lis ", "lo ", "los ",
          "lou ", "lu ",
          "mga ", "mia ",
          "'n ", "na ", "na h-", "njē ", "ny ",
          "'o ", "o ", "os ",
          "'r ",
          "'s ",
          "'t ", "ta ", "tais ", "tas ", "tē ", "tēn ", "tēs ", "the ", "to ", "tō ", "tois ",
          "ton ", "tōn ", "tou ",
          "um ", "uma ", "un ", "un'", "una ", "une ", "unei ", "unha ", "uno ", "uns ", "unui ",
          "us ",
          "y ", "ye ", "yr ");

  static {
    // When removing article, consider "an -t" before "an , "ang mga " before "ang " etc
    Collections.reverse(articles);
  }

  /**
   * Removes initial articles from title, case-insensitively.
   * @param title to remove article from
   * @return title without the initial article if any
   */
  public static String makeIndexTitle (String title) {
    for (String article : articles) {
      if (title.toLowerCase().startsWith(article)) {
        return title.replaceFirst("(?i)"+article,"");
      }
    }
    return title;
  }

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
