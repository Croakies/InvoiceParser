package net.shadowmage.invoice_parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.shadowmage.template_fill.Main;

import org.jdom.JDOMException;
import org.jopendocument.dom.template.TemplateException;

public class InvoiceDirectoryMonitor {
    private boolean singleRun = false;
    private int freq;
    private File serverFolder;
    private File outputFolder;
    private Logger log;
    private List<File> filesToParse = new ArrayList<File>();
    private List<File> filesToPrint = new ArrayList<File>();

    public static /* varargs */ void main(String ... aArgs) {
        int freq = 1;
        String path = "";
        if (aArgs == null || aArgs.length == 0) {
            System.out.println("No arguments detected for print program.");
            System.out.println("Running in static mode (static set start params)");
            freq = 20;
            path = "";
        } else if (aArgs.length == 1) {
            System.out.println("Single argument detected for print program.");
            System.out.println("Running in standard mode (set input directory to: " + aArgs[0] + ")");
            freq = 20;
            path = aArgs[0];
        } else if (aArgs.length == 2) {
            if (aArgs[1].startsWith("\"")) {
                aArgs[1] = aArgs[1].substring(1, aArgs[1].length());
            }
            if (aArgs[1].endsWith("\"")) {
                aArgs[1] = aArgs[1].substring(0, aArgs[1].length() - 1);
            }
            try {
                freq = Integer.valueOf(aArgs[0]);
            }
            catch (NumberFormatException e) {
                freq = 2;
            }
            path = aArgs[1];
            System.out.println("Two arguments detected for print program.");
            System.out.println("Running in standard mode (set input directory to: " + path + ")");
            System.out.println("With frequency of: " + freq);
        } else {
            System.out.println("The program must be launched with 0 arguments (static setup), or two arguments (frequency, path_to_monitor)");
            return;
        }
        InvoiceDirectoryMonitor parser = new InvoiceDirectoryMonitor(freq, path);
        try {
            parser.startMonitoring();
        }
        catch (Exception var4_5) {
            // empty catch block
        }
    }

    public InvoiceDirectoryMonitor(int frequency, String pathToMonitor) {
        this.freq = frequency;
        this.serverFolder = new File(pathToMonitor);
        this.outputFolder = new File("output");
        this.outputFolder.mkdirs();
        if (this.freq == 0) {
            this.singleRun = true;
        }
    }

    public void startMonitoring() {
        this.log = Logger.getLogger("com.croakies.invoiceParse");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
        Handler h = null;
        try {
            File f = new File("logs");
            f.mkdirs();
            String date = String.valueOf(System.currentTimeMillis());
            h = new FileHandler("logs/log--" + date + ".txt");
        }
        catch (SecurityException e1) {
            e1.printStackTrace();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        if (h != null) {
            this.log.addHandler(h);
            h.setFormatter(new SimpleFormatter());
        }
        this.log("Setting up Invoice parsing to monitor directory: " + this.serverFolder.getAbsolutePath());
        this.log("Beginning directory monitoring.");
        this.log("Redirecting output to logger, please check log.txt for output.");
        try {
            if (!this.serverFolder.exists()) {
                throw new RuntimeException("Folder to read from does not exist or cannot be reached: " + this.serverFolder.getAbsolutePath());
            }
            do {
                try {
                    Thread.sleep(this.freq * 1000);
                }
                catch (InterruptedException e1) {
                    // empty catch block
                }
                this.singleMonitorLoop();
            } while (!this.singleRun);
            this.log("finished single run, exiting program!");
        }
        catch (Exception e) {
            this.log.log(Level.SEVERE, "Caught exception from parsing thread: " + e.getMessage() + " :: ", e);
            e.printStackTrace();
        }
        System.exit(1);
    }

    private void singleMonitorLoop() {
        if (this.scanForFiles()) {
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
        if (files == null || files.length <= 0) {
            return false;
        }
        File[] arrfile = files;
        int n = arrfile.length;
        int n2 = 0;
        while (n2 < n) {
            File f = arrfile[n2];
            if (!f.isDirectory() && f.isFile()) {
                this.filesToParse.add(f);
                this.log("found file: " + f);
            }
            ++n2;
        }
        return !this.filesToParse.isEmpty();
    }

    private void parseFiles() {
        File f;
        Iterator<File> it = this.filesToParse.iterator();
        while (it.hasNext() && (f = it.next()) != null) {
            this.log("Parsing file: " + f.getAbsolutePath());
            this.filesToPrint.addAll(this.parseInputFile(f));
            it.remove();
        }
        this.log("Found a total of: " + this.filesToPrint.size() + " invoices");
    }

    private List<File> parseInputFile(File toParse) {
        InvoiceParser parser = new InvoiceParser(toParse, this.outputFolder);
        parser.log = this.log;
        parser.parseFile();
        return parser.getParsedFiles();
    }

    private void printFiles() {
        for (File file : this.filesToPrint) {
            this.log("Found parsed output filename of: " + file.getAbsolutePath());
            try {
                Main.main(new String[]{"resources/InvoiceTemplate.ott", String.valueOf(this.outputFolder.getAbsolutePath()) + "/" + file.getName() + ".ott", file.getAbsolutePath()});
                continue;
            }
            catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            catch (TemplateException e) {
                e.printStackTrace();
                continue;
            }
            catch (JDOMException e) {
                e.printStackTrace();
            }
        }
    }
}

