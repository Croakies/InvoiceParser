package net.shadowmage.invoice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.shadowmage.Log;
import net.shadowmage.Util;

public class InvoiceMonitor
{
  private Properties config;
  private File monitoredDirectory;
  private File localArchiveDirectory;
  private File localOutputDirectory;
  private File localTempDirectory;
  private int frequency = 10;

  /**
   * Entry point to the entire application
   * No args are used; config file loaded from static location
   * @param args
   */
  public static void main(String[] args)
  {
    Properties prop = new Properties();
    FileInputStream input = null;
    try
    {
      input = new FileInputStream("invoiceConfig/config.cfg");
      prop.load(input);
      Log.init(prop);
      InvoiceMonitor monitor = new InvoiceMonitor(prop);
      monitor.runMonitor();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }
  
  /**
   * Public constructor for purchase order monitor setup
   */
  private InvoiceMonitor(Properties configFile)
  {
    this.config = configFile;
    frequency = Integer.parseInt(config.getProperty("monitorFrequency", "10"));
    monitoredDirectory = new File(config.getProperty("monitorPath"));
    localArchiveDirectory = new File(config.getProperty("localArchivePath"));
    localOutputDirectory = new File(config.getProperty("localOutputPath"));
    localTempDirectory = new File(config.getProperty("localTempPath"));
    monitoredDirectory.mkdirs();
    localArchiveDirectory.mkdirs();
    localOutputDirectory.mkdirs();
    localTempDirectory.mkdirs();
    Log.log("Setting up Directory Monitor for Invoice processing:");
    Log.log("Monitored Directory : "+monitoredDirectory.getAbsolutePath());
    Log.log("Local Arch Directory: "+localArchiveDirectory.getAbsolutePath());
    Log.log("Local Out Directory : "+localOutputDirectory.getAbsolutePath());
    Log.log("Local Temp Directory: "+localTempDirectory.getAbsolutePath());
  }
  
  private void runMonitor()
  {
    Log.log("Starting directory scanning process for path: "+monitoredDirectory.getAbsolutePath());
    while(true)
    {
      try
      {
        monitorLoop();
      }
      catch(Exception e)
      {
        Log.exception(e);
      }
      if(frequency > 0)
      {
        //System.out.println("Pausing for: "+frequency+" seconds between directory scans.");
        try
        {
          Thread.sleep(frequency * 1000);
        }
        catch (InterruptedException e)
        {
          Log.exception(e);
        }        
      }
      else//single run, specified by frequency <= 0
      {
        break;
      }
    }
  }
  
  private void monitorLoop()
  {
    File[] files = monitoredDirectory.listFiles();
    if(files != null)//check for null, else network problems cause null-refs
    {
      for(File fileToParse : files)
      {
        //skip directories, only process actual files
        if(!fileToParse.isFile())
        {
          continue;
        }
        try
        {
          System.out.println("Found remote file: "+fileToParse.getAbsolutePath());
          // the local destination temp file; just so we are working on a local copy
          File localInputFile = new File(localArchiveDirectory, fileToParse.getName());
          // copy it to the local file
          Files.copy(fileToParse.toPath(), localInputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
          System.out.println("Copied to local file: "+localInputFile.getAbsolutePath());
          // delete the remote original copy
          Files.delete(fileToParse.toPath());
          System.out.println("Deleted remote original: "+fileToParse.getAbsolutePath());
          // re-seat reference for further use; the 'inputFile' is now explicitly the local copy
          fileToParse = localInputFile;

          //pass the parser the reference to the local copy of the file
          //parser will be responsible for any further processing of files and post-parsing cleanup        
          parseMultipleInvoice(config, fileToParse, localTempDirectory, localArchiveDirectory, localOutputDirectory);
        }
        catch (IOException e)
        {
          Log.exception(e);
        }
      }
    }
    processLocal(config, localTempDirectory, localArchiveDirectory, localOutputDirectory);
  }
  
  public static void parseMultipleInvoice(Properties config, File inputFile, File tempDirectory, File archiveDirectory, File outputDirectory)
  {
    breakUpFile(inputFile, tempDirectory);
    processLocal(config, tempDirectory, archiveDirectory, outputDirectory); 
  }
  
  private static void breakUpFile(File inputFile, File tempDirectory)
  {    
    try
    {
      List<String> fileLines = Files.readAllLines(inputFile.toPath());
      String line;
      int len = fileLines.size();    
      String emailOverride = "";
      for(int i = len-1; i >= 0; i--)
      {
        line = fileLines.get(i);
        if (line.startsWith("\f"))
        {
          line = line.substring(1);
          fileLines.set(i, line);
        }
        if(line.startsWith("Send Email:"))
        {
          String val = Util.getLineValue(line);
          if(!val.isEmpty())
          {
            emailOverride=val;
          }
          fileLines.remove(i);
        }
      }
      
      List<String> currentFileLines = new ArrayList<String>();
      len = fileLines.size();
      for(int i = 0; i < len; i++)
      {
        line = fileLines.get(i);
        if(line.startsWith("invoiceNumber") && !currentFileLines.isEmpty())
        {
          writeInvoiceRawFile(currentFileLines, tempDirectory, emailOverride);
          currentFileLines.clear();
        }
        currentFileLines.add(line);
      }
      if(!currentFileLines.isEmpty())
      {
        writeInvoiceRawFile(currentFileLines, tempDirectory, emailOverride);
      }
    }
    catch (IOException e)
    {
      Log.exception(e);
    }
  }
  
  private static File writeInvoiceRawFile(List<String> lines, File tempDirectory, String emailOverride) throws IOException
  {
    File file = null;
    String invoiceNumber = getInvoiceNumber(lines);
    file = new File(tempDirectory, invoiceNumber+".raw");
    FileWriter wr = new FileWriter(file);
    if(!emailOverride.isEmpty())
    {
      wr.write("Send Email: "+emailOverride);
      wr.write("\n");
    }
    for(String fileLine : lines)
    {
      wr.write(fileLine);
      wr.write("\n");
    }
    wr.close();
    return file;
  }
  
  private static String getInvoiceNumber(List<String> lines)
  {
    String number = "000000";
    String line;
    int len = lines.size();
    for(int i = 0; i < len; i++)
    {
      line = lines.get(i);
      if(line.startsWith("invoiceNumber:"))
      {
        number = Util.getLineValue(line);
        break;
      }
    }
    return number;
  }
    
  public static void processLocal(Properties config, File tempDirectory, File archiveDirectory, File outputDirectory)
  {
    File[] files = tempDirectory.listFiles();
    if(files==null || files.length==0){return;}//check for null, else network problems cause null-refs
    Log.log("Processing: "+files.length+" invoices from local folder.");
    for(File fileToParse : files)
    {
      //skip directories, only process actual files
      if(!fileToParse.isFile())
      {
        continue;
      }
      InvoiceParser.parseSingleInvoice(config, fileToParse, archiveDirectory, outputDirectory);      
      File archivedFile = new File(archiveDirectory, fileToParse.getName());
      try
      {
        Files.copy(fileToParse.toPath(), archivedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.delete(fileToParse.toPath());
      }
      catch (IOException e)
      {
        Log.exception(e);
      }
    }    
  }
  
}
