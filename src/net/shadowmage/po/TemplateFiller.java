package net.shadowmage.po;

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

public class TemplateFiller
{
  
  public static void fillTemplate(File inputFile, File outputFile, File templateFile) throws IOException, TemplateException, JDOMException
  {
    String inputText = new String(Files.readAllBytes(inputFile.toPath()));
    JSONObject dataObject = new JSONObject(inputText);
    fillTemplate(dataObject, outputFile, templateFile);
  }
  
  public static void fillTemplate(JSONObject dataObject, File outputFile, File templateFile) throws IOException, TemplateException, JDOMException
  {
    JavaScriptFileTemplate template = new JavaScriptFileTemplate(templateFile);
    JSONObject dataFields = dataObject.getJSONObject("dataFields");
    for (String dataKey : dataFields.keySet())
    {
      String value = dataFields.getString(dataKey);
      template.setField(dataKey, (Object) value);
    }
    JSONObject tablesObject = dataObject.getJSONObject("tableData");
    if (tablesObject != null)
    {
      for (String tableName : tablesObject.keySet())
      {
        template.setField(tableName,TemplateFiller.createTable(tablesObject.getJSONObject(tableName)));
      }
    }
    template.createDocument().saveToPackageAs(outputFile);
    System.out.println("Saving filled template to: "+outputFile.getAbsolutePath());
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
