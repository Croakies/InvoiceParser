package net.shadowmage.template_fill;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdom.JDOMException;
import org.jopendocument.dom.template.JavaScriptFileTemplate;
import org.jopendocument.dom.template.TemplateException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main
{
  public static void main(String[] aArgs) throws IOException, TemplateException, JDOMException
  {
    File templateFile = new File(aArgs[0]);
    File output = new File(aArgs[1]);
    File inputFile = new File(aArgs[2]);
    String inputText = new String(Files.readAllBytes(inputFile.toPath()));
    JavaScriptFileTemplate template = new JavaScriptFileTemplate(templateFile);
    JSONObject jo = new JSONObject(inputText);
    JSONObject dataFields = jo.getJSONObject("dataFields");
    for (String dataKey : dataFields.keySet())
    {
      String value = dataFields.getString(dataKey);
      template.setField(dataKey, (Object) value);
    }
    JSONObject tablesObject = jo.getJSONObject("tableData");
    if (tablesObject != null)
    {
      for (String tableName : tablesObject.keySet())
      {
        template.setField(tableName, Main.createTable(tablesObject.getJSONObject(tableName)));
      }
    }
    template.createDocument().saveToPackageAs(output);
    try
    {
      String outputFileName = output.getAbsolutePath();
      int end = outputFileName.lastIndexOf(46);
      outputFileName = outputFileName.substring(0, end);
      PDFConverter.saveAsPdf(output.getAbsolutePath(),
          String.valueOf(outputFileName) + ".pdf");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    Files.delete(inputFile.toPath());
  }
  
  private static List<HashMap<String, String>> createTable(JSONObject tableData)
  {
    ArrayList<HashMap<String, String>> outputList = new ArrayList<HashMap<String, String>>();
    JSONArray columnNames = tableData.getJSONArray("columnNames");
    JSONArray entriesObject = tableData.getJSONArray("entries");
    int len = entriesObject.length();
    int colLen = columnNames.length();
    int i = 0;
    while (i < len)
    {
      HashMap<String, String> entryMap = new HashMap<String, String>();
      JSONObject entryObject = entriesObject.getJSONObject(i);
      int k = 0;
      while (k < colLen)
      {
        String colName = columnNames.getString(k);
        entryMap.put(colName, entryObject.getString(colName));
        ++k;
      }
      outputList.add(entryMap);
      ++i;
    }
    return outputList;
  }
}
