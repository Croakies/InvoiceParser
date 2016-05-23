package net.shadowmage.invoice_parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import net.shadowmage.invoice_parser.InvoiceParser;
import net.shadowmage.template_fill.Main;
import org.jdom.JDOMException;
import org.jopendocument.dom.template.TemplateException;

public class InvoiceDirectoryMonitor {

   private boolean singleRun = false;
   private int freq;
   private File serverFolder;
   private File outputFolder;
   private Logger log;
   private List filesToParse = new ArrayList();
   private List filesToPrint = new ArrayList();


   public static void main(String ... aArgs) {
      boolean freq = true;
      String path = "";
      int freq1;
      if(aArgs != null && aArgs.length != 0) {
         if(aArgs.length == 1) {
            System.out.println("Single argument detected for print program.");
            System.out.println("Running in standard mode (set input directory to: " + aArgs[0] + ")");
            freq1 = 20;
            path = aArgs[0];
         } else {
            if(aArgs.length != 2) {
               System.out.println("The program must be launched with 0 arguments (static setup), or two arguments (frequency, path_to_monitor)");
               return;
            }

            if(aArgs[1].startsWith("\"")) {
               aArgs[1] = aArgs[1].substring(1, aArgs[1].length());
            }

            if(aArgs[1].endsWith("\"")) {
               aArgs[1] = aArgs[1].substring(0, aArgs[1].length() - 1);
            }

            try {
               freq1 = Integer.valueOf(aArgs[0]).intValue();
            } catch (NumberFormatException var6) {
               freq1 = 2;
            }

            path = aArgs[1];
            System.out.println("Two arguments detected for print program.");
            System.out.println("Running in standard mode (set input directory to: " + path + ")");
            System.out.println("With frequency of: " + freq1);
         }
      } else {
         System.out.println("No arguments detected for print program.");
         System.out.println("Running in static mode (static set start params)");
         freq1 = 20;
         path = "";
      }

      InvoiceDirectoryMonitor parser = new InvoiceDirectoryMonitor(freq1, path);

      try {
         parser.startMonitoring();
      } catch (Exception var5) {
         ;
      }

   }

   public InvoiceDirectoryMonitor(int frequency, String pathToMonitor) {
      this.freq = frequency;
      this.serverFolder = new File(pathToMonitor);
      this.outputFolder = new File("output");
      this.outputFolder.mkdirs();
      if(this.freq == 0) {
         this.singleRun = true;
      }

   }

   public void startMonitoring() {
      this.log = Logger.getLogger("com.croakies.invoiceParse");
      System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
      FileHandler h = null;

      try {
         File e = new File("logs");
         e.mkdirs();
         String date = String.valueOf(System.currentTimeMillis());
         h = new FileHandler("logs/log--" + date + ".txt");
      } catch (SecurityException var5) {
         var5.printStackTrace();
      } catch (IOException var6) {
         var6.printStackTrace();
      }

      if(h != null) {
         this.log.addHandler(h);
         h.setFormatter(new SimpleFormatter());
      }

      this.log("Setting up Invoice parsing to monitor directory: " + this.serverFolder.getAbsolutePath());
      this.log("Beginning directory monitoring.");
      this.log("Redirecting output to logger, please check log.txt for output.");

      try {
         if(!this.serverFolder.exists()) {
            throw new RuntimeException("Folder to read from does not exist or cannot be reached: " + this.serverFolder.getAbsolutePath());
         }

         do {
            try {
               Thread.sleep((long)(this.freq * 1000));
            } catch (InterruptedException var4) {
               ;
            }

            this.singleMonitorLoop();
         } while(!this.singleRun);

         this.log("finished single run, exiting program!");
      } catch (Exception var7) {
         this.log.log(Level.SEVERE, "Caught exception from parsing thread: " + var7.getMessage() + " :: ", var7);
         var7.printStackTrace();
      }

      System.exit(1);
   }

   private void singleMonitorLoop() {
      if(this.scanForFiles()) {
         this.parseFiles();
         this.printFiles();
         this.log("Processing loop finished, resuming directory monitoring.");
         this.filesToPrint.clear();
         this.filesToParse.clear();
      }

   }

   private void log(String message) {
      this.log.log(Level.SEVERE, message);
   }

   private boolean scanForFiles() {
      File[] files = this.serverFolder.listFiles();
      if(files != null && files.length > 0) {
         File[] var5 = files;
         int var4 = files.length;

         for(int var3 = 0; var3 < var4; ++var3) {
            File f = var5[var3];
            if(!f.isDirectory() && f.isFile()) {
               this.filesToParse.add(f);
               this.log("found file: " + f);
            }
         }

         return !this.filesToParse.isEmpty();
      } else {
         return false;
      }
   }

   private void parseFiles() {
      Iterator it = this.filesToParse.iterator();

      File f;
      while(it.hasNext() && (f = (File)it.next()) != null) {
         this.log("Parsing file: " + f.getAbsolutePath());
         this.filesToPrint.addAll(this.parseInputFile(f));
         it.remove();
      }

      this.log("Found a total of: " + this.filesToPrint.size() + " invoices");
   }

   private List parseInputFile(File toParse) {
      InvoiceParser parser = new InvoiceParser(toParse, this.outputFolder);
      parser.log = this.log;
      parser.parseFile();
      return parser.getParsedFiles();
   }

   private void printFiles() {
      Iterator var2 = this.filesToPrint.iterator();

      while(var2.hasNext()) {
         File file = (File)var2.next();
         this.log("Found parsed output filename of: " + file.getAbsolutePath());

         try {
            Main.main(new String[]{"resources/InvoiceTemplate.ott", this.outputFolder.getAbsolutePath() + "/" + file.getName() + ".ott", file.getAbsolutePath()});
         } catch (IOException var4) {
            var4.printStackTrace();
         } catch (TemplateException var5) {
            var5.printStackTrace();
         } catch (JDOMException var6) {
            var6.printStackTrace();
         }
      }

   }
}
