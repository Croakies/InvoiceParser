package net.shadowmage.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import net.shadowmage.monitor.MonitorExecutor;
import net.shadowmage.template_fill.Main;
import org.jdom.JDOMException;
import org.jopendocument.dom.template.TemplateException;

public class DirectoryMonitorBase {

   private boolean singleRun = false;
   private int freq;
   private File monitorFolder;
   private File archiveFolder;
   private File outputFolder;
   private Logger log;
   private List filesToParse = new ArrayList();
   private List filesToPrint = new ArrayList();
   private String parserClassName;
   private String templateFileName;
   private MonitorExecutor parserTarget;
   private Properties config;
   private boolean deleteOriginal = true;
   private boolean archiveOriginal = false;
   private boolean printFile = true;
   private boolean emailFile = true;
   private boolean deleteOutput = true;


   public static void main(String ... aArgs) {
      DirectoryMonitorBase monitor = new DirectoryMonitorBase();

      try {
         monitor.startMonitoring();
      } catch (Exception var3) {
         ;
      }

   }

   public DirectoryMonitorBase() {
      this.setupLog();
      this.config = new Properties();
      Properties prop = new Properties();
      FileInputStream input = null;

      try {
         input = new FileInputStream("resources/config/po_parser.cfg");
         prop.load(input);
      } catch (Exception var5) {
         this.log(var5.toString());
         System.exit(1);
      }

      this.config = prop;
      this.freq = Integer.parseInt(this.config.getProperty("monitorFrequency"));
      if(this.freq == 0) {
         this.singleRun = true;
      }

      this.monitorFolder = new File(this.config.getProperty("monitorPath"));
      this.monitorFolder.mkdirs();
      this.archiveFolder = new File(this.config.getProperty("localArchivePath"));
      this.archiveFolder.mkdirs();
      this.outputFolder = new File(this.config.getProperty("localOutputPath"));
      this.outputFolder.mkdirs();
      this.templateFileName = this.config.getProperty("templateFile");
      this.parserClassName = "net.shadowmage.po_parser.PurchaseOrderParser2";
      this.deleteOriginal = Boolean.parseBoolean(this.config.getProperty("deleteOriginal"));
      this.archiveOriginal = Boolean.parseBoolean(this.config.getProperty("archiveOriginal"));
      this.printFile = Boolean.parseBoolean(this.config.getProperty("printOutput"));
      this.emailFile = Boolean.parseBoolean(this.config.getProperty("emailOutput"));
      this.deleteOutput = Boolean.parseBoolean(this.config.getProperty("deleteOutput"));

      try {
         this.parserTarget = (MonitorExecutor)Class.forName(this.parserClassName).newInstance();
         this.log("Created parser instance from class name: " + this.parserClassName + " :: " + this.parserTarget);
      } catch (Exception var4) {
         var4.printStackTrace();
         System.exit(1);
      }

      this.log("Setting up Invoice parsing to monitor directory: " + this.monitorFolder.getAbsolutePath());
      this.log("Beginning directory monitoring.");
      this.log("Redirecting output to logger, please check log.txt for output.");
   }

   private void setupLog() {
      this.log = Logger.getLogger("com.croakies.invoiceParse");
      System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
      FileHandler h = null;

      try {
         File e1 = new File("logs");
         e1.mkdirs();
         String date = String.valueOf(System.currentTimeMillis());
         h = new FileHandler("logs/log--" + date + ".txt");
      } catch (SecurityException var4) {
         var4.printStackTrace();
      } catch (IOException var5) {
         var5.printStackTrace();
      }

      if(h != null) {
         this.log.addHandler(h);
         h.setFormatter(new SimpleFormatter());
      }

   }

   public void startMonitoring() {
      try {
         if(!this.monitorFolder.exists()) {
            throw new RuntimeException("Folder to read from does not exist or cannot be reached: " + this.monitorFolder.getAbsolutePath());
         }

         do {
            this.log("Monitor is sleeping for: " + this.freq + " seconds.");

            try {
               Thread.sleep((long)(this.freq * 1000));
            } catch (InterruptedException var2) {
               ;
            }

            this.singleMonitorLoop();
         } while(!this.singleRun);

         this.log("Finished single run, exiting program!");
      } catch (Exception var3) {
         this.log.log(Level.SEVERE, "Caught exception from parsing thread: " + var3.getMessage() + " :: ", var3);
         var3.printStackTrace();
      }

      System.exit(1);
   }

   private void singleMonitorLoop() {
      if(this.scanForFiles()) {
         this.parseFiles();
         this.processFiles();
         this.log("Processing loop finished, resuming directory monitoring.");
         this.filesToPrint.clear();
         this.filesToParse.clear();
      }

   }

   private void log(String message) {
      this.log.log(Level.SEVERE, message);
   }

   private boolean scanForFiles() {
      File[] files = this.monitorFolder.listFiles();
      if(files != null && files.length > 0) {
         File[] var5 = files;
         int var4 = files.length;

         for(int var3 = 0; var3 < var4; ++var3) {
            File f = var5[var3];
            if(!f.isDirectory() && f.isFile()) {
               this.filesToParse.add(f);
               this.log("Found raw data file: " + f);
            }
         }

         return !this.filesToParse.isEmpty();
      } else {
         return false;
      }
   }

   private void parseFiles() {
      File f;
      for(Iterator it = this.filesToParse.iterator(); it.hasNext() && (f = (File)it.next()) != null; it.remove()) {
         this.log("Parsing file: " + f.getAbsolutePath());
         this.filesToPrint.addAll(this.parseInputFile(f));
         if(this.archiveOriginal) {
            this.archiveFile(f);
         }

         if(this.deleteOriginal) {
            this.deleteFile(f);
         }
      }

      this.log("Found a total of: " + this.filesToPrint.size() + " parsed files to process");
   }

   private void deleteFile(File file) {
      try {
         this.log("Deleting file: " + file.getName());
         Files.delete(file.toPath());
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   private void archiveFile(File file) {
      this.log("Archiving parsed file: " + file.getName());
      File out = new File(this.archiveFolder, file.getName());

      try {
         Files.copy(file.toPath(), out.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
      } catch (IOException var4) {
         var4.printStackTrace();
      }

   }

   private List parseInputFile(File toParse) {
      this.parserTarget.OnFileRead(toParse, this.archiveFolder, this.outputFolder, this.log);
      return this.parserTarget.getParsedFiles();
   }

   private void processFiles() {
      Iterator var2 = this.filesToPrint.iterator();

      while(var2.hasNext()) {
         File file = (File)var2.next();
         String outputFileName = file.getName();
         int index = outputFileName.lastIndexOf(46);
         outputFileName = outputFileName.substring(0, index) + ".ott";
         this.log("Found parsed output filename of: " + file.getAbsolutePath());

         try {
            Main.main(new String[]{this.templateFileName, this.outputFolder.getAbsolutePath() + "/" + outputFileName, file.getAbsolutePath()});
            outputFileName = outputFileName.replace(".ott", ".pdf");
            File e = new File(this.outputFolder, outputFileName);
            if(this.printFile) {
               this.printFile(e);
            }

            if(this.emailFile) {
               this.sendEmail(this.log, this.config, e.getAbsolutePath());
            }

            if(this.printFile || this.emailFile) {
               try {
                  Thread.sleep(10000L);
               } catch (InterruptedException var14) {
                  ;
               }
            }

            String defaultPrinterName = "WO Printer";
            String defaultPrinterSetCommand = "wmic printer where name=\'" + defaultPrinterName + "\' call setdefaultprinter";

            try {
               this.log("POParser resetting default printer to: " + defaultPrinterName);
               Runtime.getRuntime().exec(defaultPrinterSetCommand);
            } catch (IOException var13) {
               var13.printStackTrace();
            }

            boolean killProcess = Boolean.parseBoolean(this.config.getProperty("killAcrobat"));
            if(this.printFile && killProcess) {
               this.log("Attempting to kill Acrobat process.");
               Process p = Runtime.getRuntime().exec("tasklist");
               BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
               String srv = "AcroRd32.exe";

               String line;
               while((line = reader.readLine()) != null) {
                  if(line.contains(srv)) {
                     Runtime.getRuntime().exec("taskkill /F /IM " + srv);
                     this.log("Killed acrobat service!");
                     break;
                  }
               }
            }

            if(this.deleteOutput) {
               this.log("Deleting output file....");
               this.deleteFile(e);
            }
         } catch (IOException var15) {
            var15.printStackTrace();
         } catch (TemplateException var16) {
            var16.printStackTrace();
         } catch (JDOMException var17) {
            var17.printStackTrace();
         }
      }

   }

   private void printFile(File outputFile) {
      this.log("Printing file: " + outputFile.getAbsolutePath());
      String progLoc = this.appendQuotes(this.config.getProperty("acrobatPath"));
      String fileLoc = this.appendQuotes(outputFile.getAbsolutePath());
      String printerName = this.appendQuotes(this.config.getProperty("printerName"));
      String printerDriver = this.appendQuotes(this.config.getProperty("printerDriver"));
      String printerPort = this.appendQuotes(this.config.getProperty("printerPort"));
      String printCommand = progLoc + " /p /s /h /t " + fileLoc + " " + printerName + " " + printerDriver + " " + printerPort;
      this.log("Print Command: " + printCommand);

      try {
         Runtime.getRuntime().exec(printCommand);
      } catch (IOException var9) {
         var9.printStackTrace();
         System.exit(1);
      }

      this.log("File printed!");
   }

   private String appendQuotes(String input) {
      return "\"" + input + "\"";
   }

   private void sendEmail(Logger log, Properties config, String attachFilePath) {
      File file = new File(attachFilePath);
      String bodyText = "";

      List names;
      try {
         names = Files.readAllLines((new File(config.getProperty("emailNamesFile"))).toPath());
         bodyText = getBodyText(config.getProperty("emailTextFile"));
      } catch (IOException var20) {
         var20.printStackTrace();
         return;
      }

      if(names != null && !names.isEmpty()) {
         String sender = config.getProperty("emailSender");
         String host = config.getProperty("emailHost");
         String user = config.getProperty("emailUser");
         Properties props = System.getProperties();
         props.setProperty("mail.smtp.host", host);
         props.setProperty("mail.user", user);
         props.setProperty("mail.password", "omitted");
         Session session = Session.getDefaultInstance(props);

         try {
            MimeMessage e = new MimeMessage(session);
            e.setFrom(new InternetAddress(sender));
            Iterator date = names.iterator();

            while(date.hasNext()) {
               String sdf = (String)date.next();
               e.addRecipient(RecipientType.BCC, new InternetAddress(sdf));
            }

            SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");
            String date1 = sdf1.format(new Date());
            e.setSubject("Purchase Order " + file.getName() + " : " + date1);
            MimeBodyPart messageBodyText = new MimeBodyPart();
            messageBodyText.setText(bodyText);
            FileDataSource source = new FileDataSource(file);
            DataHandler handler = new DataHandler(source);
            MimeBodyPart messageAttachmentPart = new MimeBodyPart();
            messageAttachmentPart.setDataHandler(handler);
            messageAttachmentPart.setFileName(file.getName());
            MimeMultipart mp = new MimeMultipart();
            mp.addBodyPart(messageBodyText);
            mp.addBodyPart(messageAttachmentPart);
            e.setContent(mp);
            Transport.send(e);
            this.log("Sent email message successfully....");
         } catch (Exception var21) {
            var21.printStackTrace();
         }

      } else {
         this.log("Email names was null or empty, cannot send emails!");
      }
   }

   private static String getBodyText(String fileName) throws IOException {
      List data = Files.readAllLines((new File(fileName)).toPath());
      String out = "";

      String s;
      for(Iterator var4 = data.iterator(); var4.hasNext(); out = s + data + "\r\n") {
         s = (String)var4.next();
      }

      return out;
   }
}
