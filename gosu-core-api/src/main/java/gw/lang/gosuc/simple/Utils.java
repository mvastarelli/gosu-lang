package gw.lang.gosuc.simple;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {
  public static boolean isNullOrWhiteSpace(String value) {
    return value == null || value.trim().isEmpty();
  }

  public static String combine(String str1, String str2) {
    return combine(str1, str2, "\n");
  }

  public static String combine(String str1, String str2, String combiner) {
    if(isNullOrWhiteSpace(str1)) {
      return nullCoalesce(str2);
    }

    return nullCoalesce(str1) + combiner + nullCoalesce(str2);
  }

  public static String nullTrim(String value) {
    return value == null ? "" : value.trim();
  }

  public static String nullCoalesce(String value) {
    return nullCoalesce(value, "");
  }

  public static String nullCoalesce(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  public static String getStackTrace(Throwable e) {
    var sw = new StringWriter();
    var pw = new PrintWriter(sw);

    e.printStackTrace(pw);

    return sw.toString();
  }
}
