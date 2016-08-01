package net.shadowmage.invoice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
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
  
  /**
   * WORKING
   * @param config configuration file 
   * @param inputFile raw file, may contain multiple data sets
   * @param tempDirectory directory to save any interim/temporary files (filled templates, JSON raw outputs, etc)
   * @param outputDirectory directory to save in which to save the finished and converted .pdf document
   */
  public static void parseInvoice(Properties config, File inputFile, File tempDirectory, File outputDirectory)
  {
    try
    {
      List<String> fileLines = Files.readAllLines(inputFile.toPath()); 
      
      //first pass parsing; clean up new-line feed chars
      //grab email override if present
      
      //      String line;
      //      int len = fileLines.size();    
      //      for(int i = 0; i< len; i++)
      //      {
      //        line = fileLines.get(i);
      //        if (line.startsWith("\f"))
      //        {
      //          line = line.substring(1);
      //          fileLines.set(i, line);
      //        }
      //        if(line.startsWith("Send Email:"))
      //        {
      //          config.setProperty("emailOverride", Util.getLineValue(line));
      //        }
      //      }
      
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
  
  /**
   * WORKING
   * @param config
   * @param obj
   * @param tempDirectory
   * @param outputDirectory
   */
  private static void processInvoice(Properties config, JSONObject obj, File tempDirectory, File outputDirectory)
  {
    //log("Processing invoice JSON data:\n"+obj.toString(2));
    //if(true){return;}
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
  
  /**
   * WORKING
   * @param config
   * @param convertedPDFFile
   */
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
      int len = emailAddresses.length;
      for(int i = 0; i < len; i++)
      {
        System.out.println("Dest email: "+emailAddresses[i]);
      }
      String sender = config.getProperty("emailSender");
      String host = config.getProperty("emailHost");
      String user = config.getProperty("emailUser");
      String subject = "Purchase Order: "+convertedPDFFile.getName();
      String bodyText = Util.getEmailBodyText(config.getProperty("emailTextFile"));
      System.out.println("Subject: "+subject);
      System.out.println("BodyText: "+bodyText);
      EmailSender.sendEmail(sender, host, user, emailAddresses, subject, bodyText, convertedPDFFile);
    }
  }
  
  /**
   * WORKING
   * @param data
   */
  public static void log(String data)
  {
    System.out.println(data);    
  }
  
  /**
   * WORKING
   * Iterate through the input lines from the entire file.  May contain multiple invoices.
   * Parse through lines, splitting at page-feed characters ('\f'), add pages to a list of pages
   * Examine each page for its invoice number
   * @param fileData
   */
  private final List<InvoiceData> parseLines(List<String> fileData)
  {
    List<List<String>> invoicePages = new ArrayList<List<String>>();
    List<String> currentPageLines = new ArrayList<String>();
    
    String line;
    int len = fileData.size();
    for(int i = 0; i < len; i++)
    {
      line = fileData.get(i);
      if(line.startsWith("\f") && !currentPageLines.isEmpty())//page break, if lines were not empty
      {
        invoicePages.add(currentPageLines);
        currentPageLines = new ArrayList<String>();
      }
      currentPageLines.add(line);            
    }
    log("  Parsed: " + invoicePages.size()+" pages of raw invoice data.  Examining pages for invoice numbers and combining pages for processing into documents.");
    
    //loop through the split out pages and add them to list of lines by invoice number
    HashMap<String, List<String>> invoicesByNumber = new HashMap<String, List<String>>();
    List<String> page;
    String invoiceNumber;
    len = invoicePages.size();
    for(int i = 0; i < len; i++)
    {
      page = invoicePages.get(i);
      invoiceNumber = getInvoiceNumber(page);
      if(!invoicesByNumber.containsKey(invoiceNumber))
      {
        invoicesByNumber.put(invoiceNumber, new ArrayList<String>());
      }
      invoicesByNumber.get(invoiceNumber).addAll(page);
    }
    
    List<String> invoiceLineData;
    InvoiceData data;
    List<InvoiceData> invoices = new ArrayList<InvoiceData>();
    for(String key : invoicesByNumber.keySet())
    {
      invoiceLineData = invoicesByNumber.get(key);
      data = new InvoiceData(invoiceLineData);
      invoices.add(data);
    }
    return invoices;
  }
  
  /**
   * WORKING
   * @param invoicePageLines
   * @return
   */
  private String getInvoiceNumber(List<String> invoicePageLines)
  {
    String invoiceNumberAndPage = ((String)invoicePageLines.get(3)).trim();
    String invoiceNumber = invoiceNumberAndPage.substring(14, 22).trim();
    return invoiceNumber;
  }
  
  /**
   * Data container and parser class for a single Invoice document.
   * Includes methods to parse an invoice from raw lines and convert into a JSON format suitable for use in the template-filling system.
   */
  private class InvoiceData
  {
    private List<InvoiceTableEntry> tableData = new ArrayList<InvoiceTableEntry>();
    private List<String> tagMemoLines = new ArrayList<String>();
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
    
    /**
     * WORKING
     * @param invoiceLines
     */
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
    
    /**
     * WORKING
     * @return
     */
    private JSONObject toJSON()
    {
      JSONObject root = new JSONObject();
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
      dataFields.put("tagMemo", tagMemo);
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
      List<List<String>> pageLines = new ArrayList<List<String>>();
      List<String> currentPage = new ArrayList<String>();
      int len = invoiceLines.size();
      String line;
      int headerLength = 0;
      for(int i = 0; i < len; i++)
      {
        line = invoiceLines.get(i);
        if(line.startsWith("\f"))//end of current page / start of next page
        {          
          if(!currentPage.isEmpty())
          {
            pageLines.add(currentPage);
            currentPage = new ArrayList<String>();
          }
        }
        currentPage.add(line);
        if(i > 21 && headerLength==0 && !line.isEmpty() && !line.startsWith(" "))
        {
          headerLength = i;
          log("Set header length to: "+headerLength+" from line: "+line);
        }
      }
      pageLines.add(currentPage);
      //log("Split raw data into: "+pageLines.size()+" pages of data.");
      
      //log("Parsing header data from page 1.");
      parseHeaderLines(pageLines.get(0));
      //log("Invoice number is: "+invoiceNumber);
      //log("Parsing tag memo from page 1.");
      parseTagMemo(pageLines.get(0));      
      //log("Parsing footer data from page "+pageLines.size());
      parseFooterData(pageLines.get(pageLines.size()-1));
      
      List<String> tableLines = new ArrayList<String>();
      len = pageLines.size();
      int linesLen;
      for(int i = 0; i < len; i++)
      {        
        currentPage = pageLines.get(i);
        linesLen = currentPage.size();
        if(i>0){headerLength = 24;}
        for(int k = headerLength; k < linesLen; k++)
        {
          //log("Parsing table data line of: "+currentPage.get(k));
          tableLines.add(currentPage.get(k));
        }
      }      
      parseTableLines(tableLines);
      
      len = tagMemoLines.size();
      for(int i = 0; i < len; i++)
      {
        if(i > 0)
        {
          tagMemo = tagMemo + "<text:span><text:line-break /></text:span>"; 
        }
        tagMemo = tagMemo + tagMemoLines.get(i);
      }
    }
    
    /**
     * WORKING
     * @param headerLines
     */
    private void parseHeaderLines(List<String> headerLines)
    {
      if(((String)headerLines.get(1)).startsWith("                                               *** CREDIT ***"))
        isInvoice = "CREDIT";
      String invoiceNumberAndPage = ((String)headerLines.get(3)).trim();
      invoiceNumber = invoiceNumberAndPage.substring(14, 22).trim();
      billName = ((String)headerLines.get(4)).trim();
      billAddress1 = ((String)headerLines.get(5)).trim();
      billAddress2 = ((String)headerLines.get(6)).trim();
      billAddress3 = ((String)headerLines.get(7)).substring(0, 50).trim();
      String lineEight = (String)headerLines.get(7);
      customerNumber = lineEight.substring(50, 71).trim();
      invoiceDate = lineEight.substring(71, lineEight.length()).trim();
      shipName = ((String)headerLines.get(10)).trim();
      shipAddress1 = ((String)headerLines.get(11)).trim();
      shipAddress2 = ((String)headerLines.get(12)).trim();
      shipAddress3 = ((String)headerLines.get(13)).trim();
      shipAddress3 = ((String)headerLines.get(14)).trim();
      dueDate = ((String)headerLines.get(15)).trim();
      String poLine = (String)headerLines.get(18);
      poNumber = poLine.substring(0, 12).trim();
      shipVia = poLine.substring(12, 44).trim();
      orderNumber = poLine.substring(44, 52).trim();
      slsNumber = poLine.substring(52, 60).trim();
      terms = poLine.substring(60, poLine.length()).trim();
      String deptLine = (String)headerLines.get(21);
      departmentNumber = deptLine.substring(0, 12).trim();
      requiredShipDate = deptLine.substring(12, 24).trim();
      cancelDate = deptLine.substring(24, 40).trim();
      orderCode = deptLine.substring(40, 52).trim();
      priceLevel = deptLine.substring(52, 60).trim();
      shipNumber = deptLine.substring(60, deptLine.length()).trim();
    }
        
    private void parseTagMemo(List<String> pageLines)
    {
      int start = 22;
      int end = pageLines.size();
      String line;
      for(int i = start; i < end; i++)
      {          
        line = pageLines.get(i);
        if(line.isEmpty()){continue;}
        if(!line.startsWith(" "))//not a tag memo line, done reading tag, exit reading loop
        {
          break;
        }        
        //log("TAG MEMO PARSING: "+line);
        if(line.startsWith(" "))
        {
          //skip continued lines
          if(line.startsWith("                                                           Continued"))
          {
            continue;
          }
          line = line.trim();
          if(!line.isEmpty())
          {
            tagMemoLines.add(Util.sanatizeForXML(line));
          }
        }
      }
    }
    
    private void parseFooterData(List<String> pageLines)
    {
      //TODO parse from the bottom up, stop when the first non-empty line is found that doesn't start with a space
      int end = pageLines.size();
      int start = end - 20;
      if(start<0){start = 0;}
      String line;
      //log("Parsing footer from lines: "+end+" - "+start);
      for(int i = end-1; i >=start; i--)
      {          
        line = pageLines.get(i);
        if(!line.isEmpty() && !line.startsWith(" "))
        {
          break;
        }
        if(line.isEmpty()){continue;}
        //log("FOOTER PARSING: "+line);
        if(line.startsWith("                                                 Pay This Amount"))
        {
          line = line.trim();
          //log("processing totals line: " + line);
          orderTotal = line.substring(15).trim();
        }
        else if(line.startsWith("                                               Total Gross"))
        {
          grossTotal = line.substring(59).trim();
        }   
        else if(line.startsWith("                                       Order disc :"))
        {
          line = line.substring(52).trim();
          discountPercent = line.substring(0, 10).trim();
          discountValue = line.substring(10).trim();
          //log((new StringBuilder("parsing discount line: ")).append(line).append(" : perc: ").append(discountPercent).append(" :: val: ").append(discountValue).toString());
        }
        else if(line.startsWith("                                         ADDITIONAL CHARGES"))
        {
          additionalCharges = line.substring(59).trim();
          //log((new StringBuilder("parsed additional charges line: ")).append(additionalCharges).toString());
          if(additionalCharges.isEmpty())
          {
            additionalCharges = "0.00";
          }   
        }
        else if(line.startsWith("                                        SHIPPING & HANDLING"))
        {
          log("parsing shipping and handling line: "+line);
          shippingCharges = line.substring(59).trim();
          if(shippingCharges.isEmpty())
          {
            shippingCharges = "0.00";
          }   
        }
        else if(line.startsWith("                                      SHIPPING AND HANDLING"))
        {
          log("parsing shipping and handling line: "+line);
          shippingCharges = line.substring(59).trim();
          if(shippingCharges.isEmpty())
          {
            shippingCharges = "0.00";
          }   
        }
        else if(line.startsWith("                                      INTERNATIONAL FREIGHT"))
        {
          String val = line.substring(59).trim();
          if(shippingCharges.isEmpty())
          {
            shippingCharges = val;
          }
          else
          {
            float fv = Float.parseFloat(shippingCharges);
            fv += Float.parseFloat(val);
            shippingCharges = String.format("%.2f", fv);
          }
        }
      }
    }
    
    private void parseTableLines(List<String> tableDataLines)
    {
      //log((new StringBuilder("Parsing table data of size: ")).append(tableDataLines.size()).toString());
      InvoiceTableEntry entry = null;
      for(int i = 0; i < tableDataLines.size(); i++)
      {
        String line = (String)tableDataLines.get(i);
        //log((new StringBuilder("Processing table line: ")).append(i).append(" :: ").append(line).toString());
        if(line.startsWith("                   ") && line.charAt(20) != ' ')
        {
          line = line.trim();
          //log((new StringBuilder("Parsed table entry line comment of: ")).append(line).toString());
          if(entry == null)
            log("ENTRY WAS NULL!");
          else
            entry.addCommentData(line);
        }
        else if(line.startsWith("             ") && line.trim().length() > 0)
        {
//          if(i >= 36)
//          {
//            log("Skipping second page tag memo line");
//          }
//          else
//          {
//            line = line.trim();
//            log((new StringBuilder("found tag memo line of: ")).append(line).toString());
//            tagMemoLines.add(line);
//          }
        }
        else if(line.startsWith("  "))
        {
          line = line.trim();
          if(line.isEmpty())
          {
            //log((new StringBuilder("Skipping empty line at index: ")).append(i).toString());
          } else
          {
            //log((new StringBuilder("Parsed table entry SKU data of: ")).append(line).toString());
            entry.setSKUData(line);
          }
        }
        else if(!line.startsWith(" ") && !line.isEmpty())
        {
          line = line.trim();
          if(line.isEmpty())
          {
            //log((new StringBuilder("Skipping empty line at index: ")).append(i).toString());
          }
          else
          {
            //log((new StringBuilder("Parsing line number: ")).append(i).append(" data: ").append(line).append(" as table entry!").toString());
            entry = parseTableEntry(line);
          }
        }
        else
        {
          //log((new StringBuilder("unprocessed line: ")).append(line).toString());
        }
      }
    }
    
    /**
     * WORKING
     * @param line
     * @return
     */
    private InvoiceTableEntry parseTableEntry(String line)
    {
      InvoiceTableEntry entry = new InvoiceTableEntry(line);
      tableData.add(entry);
      return entry;
    }
    
  }//end class InvoiceData
  
  /**
   * WORKING
   */
  private class InvoiceTableEntry
  {    
    private String itemCode;
    private String itemDescription;
    private String unitOfMeasure;
    private String quantity;
    private String price;
    private String amount;
    private String skuData;
    private List<String> commentData;
    
    private InvoiceTableEntry(String line)
    {
      commentData = new ArrayList<String>();
      itemCode = line.substring(0, 13).trim();
      itemDescription = line.substring(13, 45).trim();
      unitOfMeasure = line.substring(45, 50).trim();
      quantity = line.substring(50, 55).trim();
      price = line.substring(55, 66).trim();
      amount = line.substring(66, 78).trim();
    }
    
    public void setSKUData(String skuData)
    {
      this.skuData = skuData;
    }
    
    public void addCommentData(String line)
    {
      commentData.add(line);
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
