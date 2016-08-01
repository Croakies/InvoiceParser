package net.shadowmage.invoice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class InvoiceMonitor
{
  
  private Properties config;
  private File monitoredDirectory;
  private File localTempDirectory;
  private File localOutputDirectory;
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
    localTempDirectory = new File(config.getProperty("localArchivePath"));
    localOutputDirectory = new File(config.getProperty("localOutputPath"));
    monitoredDirectory.mkdirs();
    localTempDirectory.mkdirs();
    localOutputDirectory.mkdirs();
    System.out.println("Setting up Directory Monitor for Invoice processing:");
    System.out.println("Monitored Directory : "+monitoredDirectory.getAbsolutePath());
    System.out.println("Local Temp Directory: "+localTempDirectory.getAbsolutePath());
    System.out.println("Local Out Directory : "+localOutputDirectory.getAbsolutePath());
  }
  
  private void runMonitor()
  {
    while(true)
    {
      monitorLoop();
      if(frequency > 0)
      {
        System.out.println("Pausing for: "+frequency+" seconds between directory scans.");
        try
        {
          Thread.sleep(frequency * 1000);
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
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
    System.out.println("Scanning directory: "+monitoredDirectory.getAbsolutePath()+" for files.");
    File[] files = monitoredDirectory.listFiles();
    if(files==null || files.length==0){return;}//check for null, else network problems cause null-refs
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
        File localInputFile = new File(localTempDirectory, fileToParse.getName());
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
        InvoiceParser.parseInvoice(config, fileToParse, localTempDirectory, localOutputDirectory);
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }
  
}
