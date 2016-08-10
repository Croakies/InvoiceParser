package net.shadowmage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Util
{
  
  public static String getCombinedTagMemo(String input)
  {
    String output = "";
    String[] memoLines = input.split("/N");
    int i = 0;
    while (i < memoLines.length)
    {
      output = String.valueOf(output) + "<text:span><text:line-break /></text:span>  " + memoLines[i];
      ++i;
    }
    return output;
  }
  
  public static String getSubstring(String input, int begin, int end)
  {
    if (input == null || input.length() == 0)
    {
      return "";
    }
    if (begin >= input.length())
    {
      return "";
    }
    if (end > input.length())
    {
      end = input.length();
    }
    return input.substring(begin, end);
  }
  
  public static String getLineValue(String input)
  {
    String[] split;
    int splitIndex = input.indexOf(":");
    if (splitIndex > 0 && (split = input.split(":", 2)).length > 1)
    {
      return split[1].trim();
    }
    return "";
  }
  
  public static String cleanVendorName(String input)
  {
    input = input.replace(".", "");
    input = input.replace(",", "");
    input = input.replace("\"", "");
    input = input.replace("'", "");
    input = input.replace(":", "");
    input = input.replace(";", "");
    input = input.replace("(", "");
    input = input.replace(")", "");
    input = input.replace("[", "");
    input = input.replace("]", "");
    input = input.replace("{", "");
    input = input.replace("}", "");
    input = input.replace("/", "");
    input = input.replace("\\", "");
    input = input.replace("!", "");
    input = input.replace("@", "");
    input = input.replace("#", "");
    input = input.replace("$", "");
    input = input.replace("%", "");
    input = input.replace("^", "");
    input = input.replace("&", "");
    input = input.replace("*", "");
    input = input.replace("+", "");
    input = input.replace("  ", " ");
    input = input.trim();
    return input;
  }
  
  public static String sanatizeForXML(String input)
  {
    input = input.replace("<", "&lt;");
    input = input.replace(">", "&gt;");
    input = input.replace("&", "&amp;");
    return input;
  }
  
  public static String getFormattedDecimalValue(String input)
  {
    int gtLen = input.length();
    int periodIndex = input.indexOf(46);
    if (periodIndex == -1)
    {
      input = String.valueOf(input) + ".00";
    }
    else if (periodIndex > gtLen - 3)
    {
      input = String.valueOf(input) + "0";
    }
    return input;
  }
  
  public static String appendQuotes(String input)
  {
    return "\"" + input + "\"";
  }
  
  public static String pathToURL(String input)
  {
    input = input.replace('\\', '/');
    input = "file:///" + input;
    return input;
  }
  
  public static String[] getEmailAddresses(String fileName)
  {
    List<String> names = null;
    try
    {
      names = Files.readAllLines(new File(fileName).toPath());
      int len = names.size();
      String[] namesArray = new String[len];
      for(int i = 0; i < len; i++)
      {
        namesArray[i] = names.get(i);
      }
      return namesArray;
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return new String[]{};
  }
  
  public static String getEmailBodyText(String fileName)
  {
    List<String> data = null;
    try
    {
      data = Files.readAllLines(new File(fileName).toPath());
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return "";
    }
    String out = "";
    for (String s : data)
    {
      out = String.valueOf(s) + data + "\r\n";
    }
    return out;
  }
  
}
