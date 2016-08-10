package net.shadowmage.invoice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.shadowmage.Util;
import net.shadowmage.po.EmailSender;
import net.shadowmage.po.PDFConverter;
import net.shadowmage.po.PDFPrinter;
import net.shadowmage.po.TemplateFiller;

import org.json.JSONArray;
import org.json.JSONObject;

public class InvoiceParser
{
  
  public static void parseInvoice(Properties config, File inputFile, File tempDirectory, File outputDirectory)
  {
    try
    {
      List<String> fileLines = Files.readAllLines(inputFile.toPath());      
      //first pass parsing; clean up new-line feed chars
      //grab email override if present      
      String line;
      int len = fileLines.size();    
      for(int i = 0; i< len; i++)
      {
        line = fileLines.get(i);
        if (line.startsWith("\f"))
        {
          line = line.substring(1);
          fileLines.set(i, line);
        }
        if(line.startsWith("Send Email:"))
        {
          config.setProperty("emailOverride", Util.getLineValue(line));
        }
      }
      
      InvoiceParser parser = new InvoiceParser();
      List<InvoiceData> invoices = parser.parseLines(fileLines);
      
      //finally, send to the next step of processing; for filling template, converting to pdf, and email/print as needed      
      for(InvoiceData e : invoices)
      {
        processInvoice(config, e.toJSON(), tempDirectory, outputDirectory);
      }
      config.remove("emailOverride");
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private static void processInvoice(Properties config, JSONObject obj, File tempDirectory, File outputDirectory)
  {
    File templateFile = new File(config.getProperty("templateFile"));    
    String fileBasicName = obj.getJSONObject("dataFields").getString("invoiceNum");
    String filledTemplateName = fileBasicName + ".ott";
    String convertedPDFName = fileBasicName + ".pdf";    
    File filledTemplateFile = new File(tempDirectory, filledTemplateName);
    try
    {
      TemplateFiller.fillTemplate(obj, filledTemplateFile, templateFile);
      File convertedPDFFile = new File(outputDirectory, convertedPDFName);
      PDFConverter.saveAsPdf(filledTemplateFile, convertedPDFFile);
      boolean deleteTemplate = Boolean.parseBoolean(config.getProperty("deleteFilledTemplate"));
      boolean deletePDF = Boolean.parseBoolean(config.getProperty("deletePrintedPDF"));
      if(deleteTemplate)
      {
        System.out.println("Deleting filled template file: "+filledTemplateFile.getAbsolutePath());
        Files.delete(filledTemplateFile.toPath());
      }
      if(config.getProperty("emailOutput").equals("true"))
      {
        emailPDF(config, convertedPDFFile);
      }
      boolean override = false;
      String overrideAddress = config.getProperty("emailOverride");
      if(overrideAddress != null && overrideAddress.contains("@"))
      {
        override = true;
      }
      boolean print = Boolean.parseBoolean(config.getProperty("printOutput").toLowerCase());
      if(!override && print)
      {
        System.out.println("Printing PDF: "+convertedPDFFile.getAbsolutePath());
        PDFPrinter.printPDFSumatra(config, convertedPDFFile);
      }
      if(deletePDF)
      {
        System.out.println("Deleting printed PDF file: "+convertedPDFFile.getAbsolutePath());
        Files.delete(convertedPDFFile.toPath());
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  private static void emailPDF(Properties config, File convertedPDFFile)
  {
    String emailOverrideAddress = config.getProperty("emailOverride", "");
    String[] emailAddresses = null;
    if(emailOverrideAddress.length()>0 && emailOverrideAddress.contains("@"))
    {
      emailAddresses = new String[]{emailOverrideAddress};
    }
    else
    {
      emailAddresses = Util.getEmailAddresses(config.getProperty("emailNamesFile"));
    }
    if(emailAddresses.length>0)
    {
      String sender = config.getProperty("emailSender");
      String host = config.getProperty("emailHost");
      String user = config.getProperty("emailUser");
      String subject = "Purchase Order: "+convertedPDFFile.getName();
      String bodyText = Util.getEmailBodyText(config.getProperty("emailTextFile"));
      EmailSender.sendEmail(sender, host, user, emailAddresses, subject, bodyText, convertedPDFFile);
    }
  }
  
  public static void log(String data)
  {
    System.out.println(data);    
  }
  
  private final List<InvoiceData> parseLines(List<String> fileData)
  {
    List<List<String>> invoicePages = new ArrayList<List<String>>();
    List<String> currentPageLines = new ArrayList<String>();
    String line;
    int len = fileData.size();
    //loop through all lines in file, breaking into distinct invoices based on the 'invoiceNumber' starting line
    for(int i = 0; i < len; i++)
    {
      line = fileData.get(i);
      if(line.startsWith("invoiceNumber"))
      {
        if(!currentPageLines.isEmpty())
        {
          invoicePages.add(currentPageLines);
          currentPageLines = new ArrayList<String>();
        }
      }
      currentPageLines.add(line);
    }
    //there is no final 'invoiceNumber' to trigger the next, so check what was previously parsed, it is likely an invoice
    if(!currentPageLines.isEmpty())
    {
      invoicePages.add(currentPageLines);      
    }
    log("Parsed: "+invoicePages.size()+" distinct invoices.");
    
    //loop through all sets of invoice page data processing into InvoiceData instances
    InvoiceData data;
    List<InvoiceData> invoices = new ArrayList<InvoiceData>();
    len = invoicePages.size();
    for(int i = 0; i < len; i++)
    {
      data = new InvoiceData(invoicePages.get(i));
      invoices.add(data);
    }
    return invoices;
  }
    
  private class InvoiceData
  {
    private List<InvoiceTableEntry> tableData = new ArrayList<InvoiceTableEntry>();
    private String invoiceNumber = "";
    private String shipName = "";
    private String shipAddress1 = "";
    private String shipAddress2 = "";
    private String shipAddress3 = "";
    private String shipAddress4 = "";
    private String billName = "";
    private String billAddress1 = "";
    private String billAddress2 = "";
    private String billAddress3 = "";
    private String customerNumber = "";
    private String invoiceDate = "";
    private String dueDate = "";
    private String poNumber = "";
    private String shipVia = "";
    private String orderNumber = "";
    private String slsNumber = "";
    private String terms = "";
    private String departmentNumber = "";
    private String requiredShipDate = "";
    private String cancelDate = "";
    private String orderCode = "";
    private String priceLevel = "";
    private String shipNumber = "";
    private String grossTotal = "";
    private String shippingCharges = "";
    private String discountPercent = "";
    private String discountValue = "";
    private String additionalCharges = "";
    private String orderTotal = "";
    private String tagMemo = "";
    private String isInvoice = "INVOICE";    
    
    private InvoiceData(List<String> invoiceLines)
    {
      grossTotal = "0.00";
      shippingCharges = "0.00";
      discountPercent = "0";
      discountValue = "0.00";
      additionalCharges = "0.00";
      isInvoice = "INVOICE";
      tableData = new ArrayList<InvoiceTableEntry>();
      parse(invoiceLines);
    }

    private JSONObject toJSON()
    {
      JSONObject root = new JSONObject();
      //TODO add email output data to the JSON root object so that it can be read by the next step of processing?
      
      JSONObject dataFields = new JSONObject();
      root.put("dataFields", dataFields);
      dataFields.put("invoiceNum", invoiceNumber);
      dataFields.put("invDate", invoiceDate);
      dataFields.put("custNum", customerNumber);
      dataFields.put("shipName", shipName);
      dataFields.put("shipAdd1", shipAddress1);
      dataFields.put("shipAdd2", shipAddress2);
      dataFields.put("shipAdd3", shipAddress3);
      dataFields.put("shipAdd4", shipAddress4);
      dataFields.put("billName", billName);
      dataFields.put("billAdd1", billAddress1);
      dataFields.put("billAdd2", billAddress2);
      dataFields.put("billAdd3", billAddress3);
      dataFields.put("dueDate", dueDate);
      dataFields.put("poNum", poNumber);
      dataFields.put("shipVia", shipVia);
      dataFields.put("orderNum", orderNumber);
      dataFields.put("slsNum", slsNumber);
      dataFields.put("terms", terms);
      dataFields.put("deptNum", departmentNumber);
      dataFields.put("reqShip", requiredShipDate);
      dataFields.put("cancel", cancelDate);
      dataFields.put("orderCode", orderCode);
      dataFields.put("pl", priceLevel);
      dataFields.put("shipNum", shipNumber);
      dataFields.put("isInvoice", isInvoice);
      dataFields.put("orderTotal", orderTotal);
      dataFields.put("grossTotal", grossTotal);
      dataFields.put("discountPercent", discountPercent);
      dataFields.put("discountValue", discountValue);
      dataFields.put("additionalCharges", additionalCharges);
      dataFields.put("shippingCharges", shippingCharges);
      dataFields.put("tagMemo", Util.getCombinedTagMemo(tagMemo));
      JSONObject tableData = new JSONObject();
      root.put("tableData", tableData);
      JSONObject productTable = new JSONObject();
      tableData.put("productTable", productTable);
      JSONArray columnNames = new JSONArray();
      columnNames.put("ITEM NUMBER");
      columnNames.put("DESCRIPTION");
      columnNames.put("UOM");
      columnNames.put("QUANTITY");
      columnNames.put("PRICE");
      columnNames.put("AMOUNT");
      productTable.put("columnNames", columnNames);
      JSONArray tableEntries = new JSONArray();
      productTable.put("entries", tableEntries);
      for(InvoiceTableEntry entry : this.tableData)
      {
        tableEntries.put(entry.toJSON());
      }
      return root;
    }
    
    private void parse(List<String> invoiceLines)
    {
      int len = invoiceLines.size();
      String line;
      for(int i = 0; i < len; i++)
      {
        line = invoiceLines.get(i);
        if(line.startsWith("invoiceNumber")){invoiceNumber = Util.getLineValue(line);}
        else if(line.startsWith("shipName")){shipName = Util.getLineValue(line);}
        else if(line.startsWith("shipAddress1")){shipAddress1 = Util.getLineValue(line);}
        else if(line.startsWith("shipAddress2")){shipAddress2 = Util.getLineValue(line);}
        else if(line.startsWith("shipAddress3")){shipAddress3 = Util.getLineValue(line);}
        else if(line.startsWith("shipAddress4")){shipAddress4 = Util.getLineValue(line);}
        else if(line.startsWith("billName")){billName = Util.getLineValue(line);}
        else if(line.startsWith("billAddress1")){billAddress1 = Util.getLineValue(line);}
        else if(line.startsWith("billAddress2")){billAddress2 = Util.getLineValue(line);}
        else if(line.startsWith("billAddress3")){billAddress3 = Util.getLineValue(line);}
        else if(line.startsWith("customerNumber")){customerNumber = Util.getLineValue(line);}
        else if(line.startsWith("invoiceDate")){invoiceDate = Util.getLineValue(line);}
        else if(line.startsWith("dueDate")){dueDate = Util.getLineValue(line);}
        else if(line.startsWith("poNumber")){poNumber = Util.getLineValue(line);}
        else if(line.startsWith("shipVia")){shipVia = Util.getLineValue(line);}
        else if(line.startsWith("orderNumber")){orderNumber = Util.getLineValue(line);}
        else if(line.startsWith("slsNumber")){slsNumber = Util.getLineValue(line);}
        else if(line.startsWith("terms")){terms = Util.getLineValue(line);}
        else if(line.startsWith("departmentNumber")){departmentNumber = Util.getLineValue(line);}
        else if(line.startsWith("requiredShipDate")){requiredShipDate = Util.getLineValue(line);}
        else if(line.startsWith("cancelDate")){cancelDate = Util.getLineValue(line);}
        else if(line.startsWith("orderCode")){orderCode = Util.getLineValue(line);}
        else if(line.startsWith("priceLevel")){priceLevel = Util.getLineValue(line);}
        else if(line.startsWith("shipNumber")){shipNumber = Util.getLineValue(line);}
        else if(line.startsWith("grossTotal")){grossTotal = Util.getLineValue(line);}
        else if(line.startsWith("shippingCharges")){shippingCharges = Util.getLineValue(line);}
        else if(line.startsWith("discountPercent")){discountPercent = Util.getLineValue(line);}
        else if(line.startsWith("discountValue")){discountValue = Util.getLineValue(line);}
        else if(line.startsWith("additionalCharges")){additionalCharges = Util.getLineValue(line);}
        else if(line.startsWith("orderTotal")){orderTotal = Util.getLineValue(line);}
        else if(line.startsWith("isInvoice")){isInvoice = Util.getLineValue(line).equals("True")? "INVOICE" : "CREDIT";}
        else if(line.startsWith("tagMemo")){tagMemo = Util.sanatizeForXML(Util.getLineValue(line));}
        else if(line.startsWith("itemCode")){i = parseItemBlock(invoiceLines, i);}
      }
      grossTotal = Util.getFormattedDecimalValue(grossTotal);
      shippingCharges = Util.getFormattedDecimalValue(shippingCharges);
      discountValue = Util.getFormattedDecimalValue(discountValue);
      additionalCharges = Util.getFormattedDecimalValue(additionalCharges);
      orderTotal = Util.getFormattedDecimalValue(orderTotal);
    }
    
    private int parseItemBlock(List<String> lines, int startIndex)
    {
      int len = lines.size();
      List<String> itemLines = new ArrayList<String>();
      String line;
      int endIndex = startIndex;
      for(int i = startIndex; i < len; i++)
      {
        line = lines.get(i);
        if(i > startIndex && line.startsWith("itemCode"))
        {
          break;
        }
        itemLines.add(line);
        endIndex = i;        
      }
      InvoiceTableEntry ite = new InvoiceTableEntry(itemLines);
      tableData.add(ite);
      return endIndex;
    }
    
  }//end class InvoiceData
  
  private class InvoiceTableEntry
  {    
    private String itemCode;
    private String itemDescription;
    private String unitOfMeasure;
    private String quantity;
    private String price;
    private String amount;
    private String skuData;
    private List<String> commentData = new ArrayList<String>();
    
    private InvoiceTableEntry(List<String> lines)
    {
      String line;
      int len = lines.size();
      for(int i = 0; i < len; i++)
      {
        line = lines.get(i);
        if(line.startsWith("itemCode")){itemCode=Util.getLineValue(line);}
        else if(line.startsWith("itemDescription")){itemDescription=Util.getLineValue(line);}
        else if(line.startsWith("unitOfMeasure")){unitOfMeasure = Util.getLineValue(line);}
        else if(line.startsWith("quantity")){quantity = Util.getLineValue(line);}
        else if(line.startsWith("price")){price = Util.getLineValue(line);}
        else if(line.startsWith("amount")){amount = Util.getLineValue(line);}
        else if(line.startsWith("skuData")){skuData = Util.getLineValue(line);}        
        else if(line.startsWith("commentData"))
        {
          String[] tagLines = line.split(":",-1)[1].trim().split("/N",-1);          
          int len2 = tagLines.length;
          for(int k = 0; k < len2; k++)
          {
            if(tagLines[k].length()>0)
            {
              commentData.add(tagLines[k]);  
            }
          }
        }        
      }
      price = Util.getFormattedDecimalValue(price);
      amount = Util.getFormattedDecimalValue(amount);
    }
    
    private JSONObject toJSON()
    {
      JSONObject out = new JSONObject();
      String description = itemDescription;
      out.put("ITEM NUMBER", itemCode);
      out.put("UOM", unitOfMeasure);
      out.put("QUANTITY", quantity);
      out.put("PRICE", price);
      out.put("AMOUNT", amount);
      if(skuData != null && skuData.length() > 0)
      {
        skuData = Util.sanatizeForXML(skuData);
        description = (new StringBuilder(String.valueOf(description))).append("<text:span><text:line-break /></text:span>SKU: ").append(skuData).toString();
      }
      if(commentData.size() > 0)
      {
        for(int i = 0; i < commentData.size(); i++)
        {
          description = (new StringBuilder(String.valueOf(description))).append("<text:span><text:line-break /></text:span>  ").append((String)commentData.get(i)).toString();
        }         
      }
      out.put("DESCRIPTION", description);
      return out;
    }
    
  }//end class InvoiceTableEntry
  
}
