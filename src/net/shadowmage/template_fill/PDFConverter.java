package net.shadowmage.template_fill;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import ooo.connector.BootstrapSocketConnector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class PDFConverter
{
  public static void main(String[] args)
  {
    String inputFileName = args[0];
    String outputFileName = args[1];
    PDFConverter.saveAsPdf(inputFileName, outputFileName);
  }
  
  public static void saveAsPdf(String inputFileName, String outputFileName)
  {
    String inputFileURL = PDFConverter.pathToURL(inputFileName);
    String outputFileURL = PDFConverter.pathToURL(outputFileName);
    System.out.println("Loading existing file: " + inputFileURL);
    System.out.println("Saving to pdf: " + outputFileURL);
    try
    {
      XDesktop xDesktop = PDFConverter.getDesktop();
      XComponentLoader xCompLoader = (XComponentLoader) UnoRuntime
          .queryInterface(XComponentLoader.class, xDesktop);
      PropertyValue[] pv = new PropertyValue[]
      { new PropertyValue() };
      pv[0].Name = "Hidden";
      pv[0].Value = true;
      XComponent xComponent = xCompLoader.loadComponentFromURL(inputFileURL,
          "_blank", 0, pv);
      XStorable xStorable = (XStorable) UnoRuntime.queryInterface(
          XStorable.class, xComponent);
      PropertyValue[] aMediaDescriptor = new PropertyValue[]
      { new PropertyValue() };
      aMediaDescriptor[0].Name = "FilterName";
      aMediaDescriptor[0].Value = "writer_pdf_Export";
      xStorable.storeToURL(outputFileURL, aMediaDescriptor);
    }
    catch (com.sun.star.io.IOException e)
    {
      System.out
          .println("ERROR: Caught exception while attempting to convert to PDF.  Likely cause is that the output file is locked from editing or input files are invalid. FileName: "
              + outputFileName + " : " + inputFileName);
      e.printStackTrace();
    }
    catch (IllegalArgumentException e)
    {
      System.out
          .println("ERROR: Caught exception while attempting to convert to PDF.  Likely cause is that the output file is locked from editing or input files are invalid. FileName: "
              + outputFileName + " : " + inputFileName);
      e.printStackTrace();
    }
    try
    {
      Files.delete(new File(inputFileName).toPath());
    }
    catch (IOException e)
    {
      System.out
          .println("ERROR: Caught exception while attempting delete converted template file.  Likely cause is that the file is locked from editing or files are invalid. FileName: "
              + outputFileName + " : " + inputFileName);
      e.printStackTrace();
    }
  }
  
  public static String pathToURL(String input)
  {
    input = input.replace('\\', '/');
    input = "file:///" + input;
    return input;
  }
  
  public static XDesktop getDesktop()
  {
    String ooLoc = "C:/Program Files (x86)/OpenOffice 4/program/";
    XDesktop xDesktop = null;
    try
    {
      XComponentContext xContext = BootstrapSocketConnector
          .bootstrap((String) ooLoc);
      XMultiComponentFactory xMCF = xContext.getServiceManager();
      if (xMCF != null)
      {
        Object oDesktop = xMCF.createInstanceWithContext(
            "com.sun.star.frame.Desktop", xContext);
        xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class,
            oDesktop);
      }
      else
      {
        System.out
            .println("Can't create a desktop. No connection, no remote office servicemanager available!");
      }
    }
    catch (Exception e)
    {
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return xDesktop;
  }
}
