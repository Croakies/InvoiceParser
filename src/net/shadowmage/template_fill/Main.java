package net.shadowmage.template_fill;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.shadowmage.template_fill.PDFConverter;
import org.jdom.JDOMException;
import org.jopendocument.dom.template.JavaScriptFileTemplate;
import org.jopendocument.dom.template.TemplateException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

   public static void main(String[] aArgs) throws IOException, TemplateException, JDOMException {
      File templateFile = new File(aArgs[0]);
      File output = new File(aArgs[1]);
      File inputFile = new File(aArgs[2]);
      String inputText = new String(Files.readAllBytes(inputFile.toPath()));
      JavaScriptFileTemplate template = new JavaScriptFileTemplate(templateFile);
      JSONObject jo = new JSONObject(inputText);
      JSONObject dataFields = jo.getJSONObject("dataFields");
      Iterator e = dataFields.keySet().iterator();

      while(e.hasNext()) {
         String tablesObject = (String)e.next();
         String end = dataFields.getString(tablesObject);
         template.setField(tablesObject, end);
      }

      JSONObject tablesObject1 = jo.getJSONObject("tableData");
      String e1;
      if(tablesObject1 != null) {
         Iterator end1 = tablesObject1.keySet().iterator();

         while(end1.hasNext()) {
            e1 = (String)end1.next();
            template.setField(e1, createTable(tablesObject1.getJSONObject(e1)));
         }
      }

      template.createDocument().saveToPackageAs(output);

      try {
         e1 = output.getAbsolutePath();
         int end2 = e1.lastIndexOf(46);
         e1 = e1.substring(0, end2);
         PDFConverter.saveAsPdf(output.getAbsolutePath(), e1 + ".pdf");
      } catch (Exception var11) {
         var11.printStackTrace();
      }

      Files.delete(inputFile.toPath());
   }

   private static List createTable(JSONObject tableData) {
      ArrayList outputList = new ArrayList();
      JSONArray columnNames = tableData.getJSONArray("columnNames");
      JSONArray entriesObject = tableData.getJSONArray("entries");
      int len = entriesObject.length();
      int colLen = columnNames.length();

      for(int i = 0; i < len; ++i) {
         HashMap entryMap = new HashMap();
         JSONObject entryObject = entriesObject.getJSONObject(i);

         for(int k = 0; k < colLen; ++k) {
            String colName = columnNames.getString(k);
            entryMap.put(colName, entryObject.getString(colName));
         }

         outputList.add(entryMap);
      }

      return outputList;
   }
}
