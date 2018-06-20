package io.insight.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
  public static String camel2snake(String input) {
    Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(input);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(sb, "_" + m.group().toLowerCase());
    }
    m.appendTail(sb);
    return sb.toString();
  }

  public static String toCamelCase(String name, boolean upperCaseFirst) {
    String[] names = name.split("_");
    StringBuilder sb=new StringBuilder();
    boolean upper = upperCaseFirst;
    for (String n : names) {
      if(n.length() >0) {
        String firstChar = n.charAt(0) + "";
        sb.append(upper ? firstChar.toUpperCase() : firstChar.toLowerCase());
        sb.append(n.substring(1));
      }
      upper = true;
    }
    return sb.toString();
  }

  public static String is2str(InputStream inputStream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    return result.toString(StandardCharsets.UTF_8.name());
  }
}
