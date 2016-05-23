package net.shadowmage.template_fill;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import java.io.File;
import java.nio.file.Files;
import ooo.connector.BootstrapSocketConnector;

public class PDFConverter {

   public static void main(String[] args) {
      String inputFileName = args[0];
      String outputFileName = args[1];
      saveAsPdf(inputFileName, outputFileName);
   }

   public static void saveAsPdf(String inputFileName, String outputFileName) {
      String inputFileURL = pathToURL(inputFileName);
      String outputFileURL = pathToURL(outputFileName);
      System.out.println("Loading existing file: " + inputFileURL);
      System.out.println("Saving to pdf: " + outputFileURL);

      try {
         XDesktop e = getDesktop();
         XComponentLoader xCompLoader = (XComponentLoader)UnoRuntime.queryInterface(XComponentLoader.class, e);
         PropertyValue[] pv = new PropertyValue[]{new PropertyValue()};
         pv[0].Name = "Hidden";
         pv[0].Value = Boolean.valueOf(true);
         XComponent xComponent = xCompLoader.loadComponentFromURL(inputFileURL, "_blank", 0, pv);
         XStorable xStorable = (XStorable)UnoRuntime.queryInterface(XStorable.class, xComponent);
         PropertyValue[] aMediaDescriptor = new PropertyValue[]{new PropertyValue()};
         aMediaDescriptor[0].Name = "FilterName";
         aMediaDescriptor[0].Value = "writer_pdf_Export";
         xStorable.storeToURL(outputFileURL, aMediaDescriptor);
      } catch (IOException var11) {
         System.out.println("ERROR: Caught exception while attempting to convert to PDF.  Likely cause is that the output file is locked from editing or input files are invalid. FileName: " + outputFileName + " : " + inputFileName);
         var11.printStackTrace();
      } catch (IllegalArgumentException var12) {
         System.out.println("ERROR: Caught exception while attempting to convert to PDF.  Likely cause is that the output file is locked from editing or input files are invalid. FileName: " + outputFileName + " : " + inputFileName);
         var12.printStackTrace();
      }

      try {
         Files.delete((new File(inputFileName)).toPath());
      } catch (java.io.IOException var10) {
         System.out.println("ERROR: Caught exception while attempting delete converted template file.  Likely cause is that the file is locked from editing or files are invalid. FileName: " + outputFileName + " : " + inputFileName);
         var10.printStackTrace();
      }

   }

   public static String pathToURL(String input) {
      input = input.replace('\\', '/');
      input = "file:///" + input;
      return input;
   }

   public static XDesktop getDesktop() {
      String ooLoc = "C:/Program Files (x86)/OpenOffice 4/program/";
      XDesktop xDesktop = null;

      try {
         XComponentContext e = BootstrapSocketConnector.bootstrap(ooLoc);
         XMultiComponentFactory xMCF = e.getServiceManager();
         if(xMCF != null) {
            Object oDesktop = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", e);
            xDesktop = (XDesktop)UnoRuntime.queryInterface(XDesktop.class, oDesktop);
         } else {
            System.out.println("Can\'t create a desktop. No connection, no remote office servicemanager available!");
         }
      } catch (Exception var5) {
         var5.printStackTrace(System.err);
         System.exit(1);
      }

      return xDesktop;
   }
}
