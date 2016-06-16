package net.shadowmage.po;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.shadowmage.Util;

import org.json.JSONObject;

public class PurchaseOrderParser
{
    
  /**
   * @param config configuration file 
   * @param inputFile raw file, may contain multiple data sets
   * @param tempDirectory directory to save any interim/temporary files (filled templates, JSON raw outputs, etc)
   * @param outputDirectory directory to save in which to save the finished and converted .pdf document
   */
  public static void parsePOs(Properties config, File inputFile, File tempDirectory, File outputDirectory)
  {
    try
    {
      String line;
      List<String> fileLines = Files.readAllLines(inputFile.toPath());
      List<String> poLines = new ArrayList<String>();
      List<POEntry> entries = new ArrayList<POEntry>();
      int len = fileLines.size();
      
      //first pass parsing; clean up new-line feed chars
      //grab email override if present
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
      
      //second pass parsing, create the PO entries from groups of PO lines
      for(int i = 0; i < len;i++)
      {
        line = fileLines.get(i);
        if(line.startsWith("Send Email:"))
        {
          continue;
        }
        if (line.startsWith("poNumber:"))
        {
          if(!poLines.isEmpty())
          {
            POEntry e = new POEntry();
            e.parseRawLines(poLines);
            entries.add(e);
            poLines.clear();
          }
        }
        poLines.add(line);
      }
      //catch for end of loop for whatever was currently parsing at the end of the file
      if(!poLines.isEmpty())
      {
        POEntry e = new POEntry();
        e.parseRawLines(poLines);
        entries.add(e);
      }
      
      //finally, send to the next step of processing; for filling template, converting to pdf, and email/print as needed      
      for(POEntry e : entries)
      {
        processPO(config, e.toJson(), tempDirectory, outputDirectory);
      }
      config.remove("emailOverride");
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private static void processPO(Properties config, JSONObject obj, File tempDirectory, File outputDirectory)
  {
    File templateFile = new File(config.getProperty("templateFile"));    
    String fileBasicName = obj.getString("fileName");
    String filledTemplateName = fileBasicName + ".ott";
    String convertedPDFName = fileBasicName + ".pdf";    
    File filledTemplateFile = new File(tempDirectory, filledTemplateName);
    try
    {
      TemplateFiller.fillTemplate(obj, filledTemplateFile, templateFile);
      File convertedPDFFile = new File(outputDirectory, convertedPDFName);
      PDFConverter.saveAsPdf(filledTemplateFile, convertedPDFFile);
      if(config.get("deleteFilledTemplate").equals("true"))
      {
        System.out.println("Deleting filled template file: "+filledTemplateFile.getAbsolutePath());
        Files.delete(filledTemplateFile.toPath());
      }
      if(config.getProperty("emailOutput").equals("true"))
      {
        emailPO(config, convertedPDFFile);
      }
      if(!config.containsKey("emailOverride") && config.getProperty("printOutput").equals("true"))
      {
        System.out.println("Printing PDF: "+convertedPDFFile.getAbsolutePath());
        PDFPrinter.printPDFSumatra(config, convertedPDFFile);
      }
      if(config.get("deletePrintedPDF").equals("true"))
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
  
  private static void emailPO(Properties config, File convertedPDFFile)
  {
    String emailOverrideAddress = config.getProperty("emailOverride", "");
    String[] emailAddresses = null;
    if(emailOverrideAddress.length()>0)
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
  
}
