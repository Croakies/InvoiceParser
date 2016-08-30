package net.shadowmage.po;

import java.io.File;

import net.shadowmage.Log;
import net.shadowmage.Util;
import ooo.connector.BootstrapSocketConnector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class PDFConverter
{
  
  public static void saveAsPdf(File inputFile, File outputFile)
  {
    saveAsPdf(inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
  }
  
  public static void saveAsPdf(String inputFileName, String outputFileName)
  {
    String inputFileURL = Util.pathToURL(inputFileName);
    String outputFileURL = Util.pathToURL(outputFileName);
    Log.log("Converting and saving to new pdf file: " + inputFileURL + " -> " +outputFileURL);
    try
    {
      XDesktop xDesktop = PDFConverter.getDesktop();
      XComponentLoader xCompLoader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, xDesktop);
      PropertyValue[] pv = new PropertyValue[]{ new PropertyValue() };
      pv[0].Name = "Hidden";
      pv[0].Value = true;
      XComponent xComponent = xCompLoader.loadComponentFromURL(inputFileURL,"_blank", 0, pv);
      XStorable xStorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, xComponent);
      PropertyValue[] aMediaDescriptor = new PropertyValue[]{ new PropertyValue() };
      aMediaDescriptor[0].Name = "FilterName";
      aMediaDescriptor[0].Value = "writer_pdf_Export";
      xStorable.storeToURL(outputFileURL, aMediaDescriptor);
    }
    catch (Exception e)
    {
      Log.log("ERROR: Caught exception while attempting to convert to PDF.  Likely cause is that the output file is locked from editing or input files are invalid. FileName: "+ outputFileName + " : " + inputFileName);
      Log.exception(e);
    }
  }
  
  private static XDesktop getDesktop()
  {
    String ooLoc = "C:/Program Files (x86)/OpenOffice 4/program/";
    XDesktop xDesktop = null;
    try
    {
      XComponentContext xContext = BootstrapSocketConnector.bootstrap((String) ooLoc);
      XMultiComponentFactory xMCF = xContext.getServiceManager();
      if (xMCF != null)
      {
        Object oDesktop = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
        xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class,oDesktop);
      }
      else
      {
        Log.log("Can't create a desktop. No connection, no remote office servicemanager available!");
      }
    }
    catch (Exception e)
    {
      Log.exception(e);
      System.exit(1);
    }
    return xDesktop;
  }
  
}
