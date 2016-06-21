package net.shadowmage.po;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import net.shadowmage.Util;

public class PDFPrinter
{
  public static void printPDFAcrobat(Properties config, File outputFile)
  {
    String progLoc = Util.appendQuotes(config.getProperty("acrobatPath"));
    String fileLoc = Util.appendQuotes(outputFile.getAbsolutePath());
    String printerName = Util.appendQuotes(config.getProperty("printerName"));
    String printerDriver = Util.appendQuotes(config.getProperty("printerDriver"));
    String printerPort = Util.appendQuotes(config.getProperty("printerPort"));
    String printCommand = String.valueOf(progLoc) + " /p /s /h /t " + fileLoc+ " " + printerName + " " + printerDriver + " " + printerPort;
    try
    {
      Runtime.getRuntime().exec(printCommand);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  public static void printPDFSumatra(Properties config, File outputFile)
  {
    String progLoc = Util.appendQuotes(config.getProperty("sumatraPath"));
    String fileLoc = Util.appendQuotes(outputFile.getAbsolutePath());
    String printerName = Util.appendQuotes(config.getProperty("printerName"));
    String printCommand = String.valueOf(progLoc) + " -print-to " + printerName +" "+ fileLoc;
    System.out.println("Executing command: "+printCommand);
    try
    {
      Runtime.getRuntime().exec(printCommand);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    System.out.println("Pausing for: 10 seconds to allow printing to finish.");
    try
    {
      Thread.sleep(10 * 1000);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }       
  }
}
