/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package net.shadowmage.po_parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.shadowmage.monitor.MonitorExecutor;

import org.json.JSONArray;
import org.json.JSONObject;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class PurchaseOrderParser2
implements MonitorExecutor {
    public Logger log;
    protected File inputFile;
    protected File outputPath;
    protected File archivePath;
    protected List<File> outputFiles = new ArrayList<File>();
    private List<POEntry> entries = new ArrayList<POEntry>();

    @Override
    public final List<File> getParsedFiles() {
        ArrayList<File> output = new ArrayList<File>();
        output.addAll(this.outputFiles);
        this.outputFiles.clear();
        return output;
    }

    private void log(String message) {
        this.log.log(Level.SEVERE, message);
    }

    @Override
    public void OnFileRead(File inputFile, File archive, File outputPath, Logger log) {
        if (!inputFile.exists()) {
            System.out.print("Cannot locate input file for name: " + inputFile.getAbsolutePath());
            return;
        }
        this.archivePath = archive;
        this.archivePath.mkdirs();
        outputPath.mkdirs();
        this.inputFile = inputFile;
        this.outputPath = outputPath;
        this.log = log;
        this.parseRawFiles();
        this.exportJSON();
    }

    private void parseRawFiles() {
        try {
            String line;
            List<String> fileLines = Files.readAllLines(this.inputFile.toPath());
            int len = fileLines.size();
            int i = 0;
            while (i < len) {
                line = fileLines.get(i);
                if (line.startsWith("\f")) {
                    line = line.substring(1);
                    fileLines.set(i, line);
                }
                ++i;
            }
            i = 0;
            while (i < len) {
                line = fileLines.get(i);
                if (line.startsWith("poNumber:")) {
                    i = this.parseRawPO(fileLines, i);
                }
                ++i;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private int parseRawPO(List<String> inputLines, int startLine) {
        POEntry entry = new POEntry();
        int len = inputLines.size();
        int lineNumber = startLine;
        while (lineNumber < len) {
            String line = inputLines.get(lineNumber);
            if (lineNumber > startLine && line.startsWith("poNumber:")) {
                --lineNumber;
                break;
            }
            entry.rawLines.add(line);
            ++lineNumber;
        }
        this.entries.add(entry);
        entry.parseRawLines();
        return lineNumber;
    }

    private void exportJSON() {
        int len = this.entries.size();
        int i = 0;
        while (i < len) {
            POEntry entry = this.entries.get(i);
            String vendName = this.cleanVendorName(entry.vendorName);
            String outputFileName = String.valueOf(vendName) + "-" + entry.poNumber + ".json";
            File outputFile = new File(this.archivePath, outputFileName);
            JSONObject jsonOutputData = entry.toJson();
            this.outputFiles.add(outputFile);
            this.log("Adding file to output: " + outputFile.getName());
            try {
                FileWriter fw = new FileWriter(outputFile);
                fw.write(jsonOutputData.toString(2));
                fw.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            ++i;
        }
        this.entries.clear();
    }

    private String cleanVendorName(String input) {
        input = input.replace(".", "");
        input = input.replace(",", "");
        input = input.replace("\"", "");
        input = input.replace("'", "");
        input = input.replace(":", "");
        input = input.replace(";", "");
        input = input.replace("(", "");
        input = input.replace(")", "");
        input = input.replace("[", "");
        input = input.replace("]", "");
        input = input.replace("{", "");
        input = input.replace("}", "");
        return input;
    }

    private String sanatizeForXML(String input) {
        input = input.replace("<", "&lt;");
        input = input.replace(">", "&gt;");
        input = input.replace("&", "&amp;");
        return input;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public class POEntry {
        String poNumber;
        String vendorName;
        String vendorAdd1;
        String vendorAdd2;
        String vendorAdd3;
        String vendorAdd4;
        String shipToName;
        String shipToAdd1;
        String shipToAdd2;
        String shipToAdd3;
        String shipToAdd4;
        String billToName;
        String billToAdd1;
        String billToAdd2;
        String billToAdd3;
        String billToAdd4;
        String vendorPhone;
        String vendorFax;
        String contact;
        String vendorEmail;
        String sendEmail;
        String poDate;
        String vendorNumber;
        String shipVia;
        String fobDescritpion;
        String terms;
        String deliveryDate;
        String placedWith;
        String freight;
        String buyer;
        String grandTotal;
        String tagMemo;
        List<String> rawLines;
        List<POLineItem> lineItems;

        public POEntry() {
            this.poNumber = "";
            this.vendorName = "";
            this.vendorAdd1 = "";
            this.vendorAdd2 = "";
            this.vendorAdd3 = "";
            this.vendorAdd4 = "";
            this.shipToName = "";
            this.shipToAdd1 = "";
            this.shipToAdd2 = "";
            this.shipToAdd3 = "";
            this.shipToAdd4 = "";
            this.billToName = "";
            this.billToAdd1 = "";
            this.billToAdd2 = "";
            this.billToAdd3 = "";
            this.billToAdd4 = "";
            this.vendorPhone = "";
            this.vendorFax = "";
            this.contact = "";
            this.vendorEmail = "";
            this.sendEmail = "";
            this.poDate = "";
            this.vendorNumber = "";
            this.shipVia = "";
            this.fobDescritpion = "";
            this.terms = "";
            this.deliveryDate = "";
            this.placedWith = "";
            this.freight = "";
            this.buyer = "";
            this.grandTotal = "";
            this.tagMemo = "";
            this.rawLines = new ArrayList<String>();
            this.lineItems = new ArrayList<POLineItem>();
        }

        public void parseRawLines() {
            int len = this.rawLines.size();
            int i = 0;
            while (i < len) {
                String line = this.rawLines.get(i);
                if (line.startsWith("poNumber:")) {
                    this.poNumber = this.getLineValue(line);
                } else if (line.startsWith("vendorName:")) {
                    this.vendorName = this.getLineValue(line);
                } else if (line.startsWith("vendorAdd1:")) {
                    this.vendorAdd1 = this.getLineValue(line);
                } else if (line.startsWith("vendorAdd2:")) {
                    this.vendorAdd2 = this.getLineValue(line);
                } else if (line.startsWith("vendorAdd3:")) {
                    this.vendorAdd3 = this.getLineValue(line);
                } else if (line.startsWith("vendorAdd4:")) {
                    this.vendorAdd4 = this.getLineValue(line);
                } else if (line.startsWith("shipToName:")) {
                    this.shipToName = this.getLineValue(line);
                } else if (line.startsWith("shipToAdd1:")) {
                    this.shipToAdd1 = this.getLineValue(line);
                } else if (line.startsWith("shipToAdd2:")) {
                    this.shipToAdd2 = this.getLineValue(line);
                } else if (line.startsWith("shipToAdd3:")) {
                    this.shipToAdd3 = this.getLineValue(line);
                } else if (line.startsWith("shipToAdd4:")) {
                    this.shipToAdd4 = this.getLineValue(line);
                } else if (line.startsWith("billToName:")) {
                    this.billToName = this.getLineValue(line);
                } else if (line.startsWith("billToAdd1:")) {
                    this.billToAdd1 = this.getLineValue(line);
                } else if (line.startsWith("billToAdd2:")) {
                    this.billToAdd2 = this.getLineValue(line);
                } else if (line.startsWith("billToAdd3:")) {
                    this.billToAdd3 = this.getLineValue(line);
                } else if (line.startsWith("billToAdd4:")) {
                    this.billToAdd4 = this.getLineValue(line);
                } else if (line.startsWith("vendorPhone:")) {
                    this.vendorPhone = this.getLineValue(line);
                } else if (line.startsWith("vendorFax:")) {
                    this.vendorFax = this.getLineValue(line);
                } else if (line.startsWith("vendor email:")) {
                    this.vendorEmail = this.getLineValue(line);
                } else if (line.startsWith("email po y/N:")) {
                    this.sendEmail = this.getLineValue(line);
                } else if (line.startsWith("contact:")) {
                    this.contact = this.getLineValue(line);
                } else if (line.startsWith("poDate:")) {
                    this.poDate = this.getLineValue(line);
                } else if (line.startsWith("vendorNumber:")) {
                    this.vendorNumber = this.getLineValue(line);
                } else if (line.startsWith("shipVia:")) {
                    this.shipVia = this.getLineValue(line);
                } else if (line.startsWith("fobDescritpion:")) {
                    this.fobDescritpion = this.getLineValue(line);
                } else if (line.startsWith("terms:")) {
                    this.terms = this.getLineValue(line);
                } else if (line.startsWith("deliveryDate:")) {
                    this.deliveryDate = this.getLineValue(line);
                } else if (line.startsWith("placedWith:")) {
                    this.placedWith = this.getLineValue(line);
                } else if (line.startsWith("freight:")) {
                    this.freight = this.getLineValue(line);
                } else if (line.startsWith("buyer:")) {
                    this.buyer = this.getLineValue(line);
                } else if (line.startsWith("grandTotal:")) {
                    this.grandTotal = this.getLineValue(line);
                    this.grandTotal = this.getFormattedDecimalValue(this.grandTotal);
                } else if (line.startsWith("Global Tag Memo:")) {
                    this.tagMemo = PurchaseOrderParser2.this.sanatizeForXML(this.getLineValue(line));
                } else if (line.startsWith("itemNumber:")) {
                    i = this.parseItem(this.rawLines, i);
                } else {
                    PurchaseOrderParser2.this.log("Found unsupported line: " + line);
                }
                ++i;
            }
            this.rawLines.clear();
        }

        private int parseItem(List<String> lines, int start) {
            int len;
            POLineItem item = new POLineItem();
            int lineNumber = len = lines.size();
            lineNumber = start;
            while (lineNumber < len) {
                String line = lines.get(lineNumber);
                if (lineNumber > start && line.startsWith("itemNumber:")) {
                    --lineNumber;
                    break;
                }
                if (line.startsWith("itemNumber:")) {
                    item.itemNumber = this.getLineValue(line);
                } else if (line.startsWith("itemDescription:")) {
                    item.itemDescription = this.getLineValue(line);
                } else if (line.startsWith("qty:")) {
                    item.qty = this.getLineValue(line);
                } else if (line.startsWith("uom:")) {
                    item.un = this.getLineValue(line);
                } else if (line.startsWith("unitPrice:")) {
                    item.unitPrice = this.getLineValue(line);
                    item.unitPrice = this.getFormattedDecimalValue(item.unitPrice);
                } else if (line.startsWith("amount:")) {
                    item.amount = this.getLineValue(line);
                    item.amount = this.getFormattedDecimalValue(item.amount);
                } else if (line.startsWith("itemTagMemo:")) {
                    item.tagMemo = PurchaseOrderParser2.this.sanatizeForXML(this.getLineValue(line));
                }
                ++lineNumber;
            }
            this.lineItems.add(item);
            return lineNumber;
        }

        private String getFormattedDecimalValue(String input) {
            int gtLen = input.length();
            int periodIndex = input.indexOf(46);
            if (periodIndex == -1) {
                input = String.valueOf(input) + ".00";
                PurchaseOrderParser2.this.log("Added .00 to grand total");
            } else if (periodIndex > gtLen - 3) {
                PurchaseOrderParser2.this.log("Added X.X0 to grand total");
                input = String.valueOf(input) + "0";
            }
            return input;
        }

        private String getLineValue(String input) {
            String[] split;
            int splitIndex = input.indexOf(":");
            if (splitIndex > 0 && (split = input.split(":", 2)).length > 1) {
                return split[1].trim();
            }
            return "";
        }

        private String getCombinedTagMemo(String input) {
            String output = "";
            String[] memoLines = this.tagMemo.split("/N");
            int i = 0;
            while (i < memoLines.length) {
                output = String.valueOf(output) + "<text:span><text:line-break /></text:span>  " + memoLines[i];
                ++i;
            }
            return output;
        }

        public JSONObject toJson() {
            if (this.vendorAdd2.isEmpty()) {
                this.vendorAdd2 = this.vendorAdd3;
                this.vendorAdd3 = "";
            }
            if (this.vendorAdd3.isEmpty()) {
                this.vendorAdd3 = this.vendorAdd4;
                this.vendorAdd4 = "";
            }
            if (this.shipToAdd2.isEmpty()) {
                this.shipToAdd2 = this.shipToAdd3;
                this.shipToAdd3 = "";
            }
            if (this.shipToAdd3.isEmpty()) {
                this.shipToAdd3 = this.shipToAdd4;
                this.shipToAdd4 = "";
            }
            if (this.billToAdd3.isEmpty()) {
                this.billToAdd3 = this.billToAdd4;
                this.billToAdd4 = "";
            }
            JSONObject root = new JSONObject();
            JSONObject dataFields = new JSONObject();
            root.put("dataFields", (Object)dataFields);
            dataFields.put("poNumber", (Object)this.poNumber);
            dataFields.put("poDate", (Object)this.poDate);
            dataFields.put("vendorName", (Object)this.vendorName);
            dataFields.put("vendorAdd1", (Object)this.vendorAdd1);
            dataFields.put("vendorAdd2", (Object)this.vendorAdd2);
            dataFields.put("vendorAdd3", (Object)this.vendorAdd3);
            dataFields.put("vendorAdd4", (Object)this.vendorAdd4);
            dataFields.put("shipName", (Object)this.shipToName);
            dataFields.put("shipAdd1", (Object)this.shipToAdd1);
            dataFields.put("shipAdd2", (Object)this.shipToAdd2);
            dataFields.put("shipAdd3", (Object)this.shipToAdd3);
            dataFields.put("shipAdd4", (Object)this.shipToAdd4);
            dataFields.put("billName", (Object)this.billToName);
            dataFields.put("billAdd1", (Object)this.billToAdd1);
            dataFields.put("billAdd2", (Object)this.billToAdd2);
            dataFields.put("billAdd3", (Object)this.billToAdd3);
            dataFields.put("billAdd4", (Object)this.billToAdd4);
            dataFields.put("poDate", (Object)this.poDate);
            dataFields.put("vendorPhone", (Object)this.vendorPhone);
            dataFields.put("vendorFax", (Object)this.vendorFax);
            dataFields.put("contact", (Object)this.contact);
            dataFields.put("vendorNumber", (Object)this.vendorNumber);
            dataFields.put("shipVia", (Object)this.shipVia);
            dataFields.put("fobDescription", (Object)this.fobDescritpion);
            dataFields.put("terms", (Object)this.terms);
            dataFields.put("deliveryDate", (Object)this.deliveryDate);
            dataFields.put("placedWith", (Object)this.placedWith);
            dataFields.put("freight", (Object)this.freight);
            dataFields.put("buyer", (Object)this.buyer);
            dataFields.put("grandTotal", (Object)this.grandTotal);
            dataFields.put("tagMemo", (Object)this.getCombinedTagMemo(this.tagMemo));
            JSONObject tableData = new JSONObject();
            root.put("tableData", (Object)tableData);
            JSONObject productTable = new JSONObject();
            tableData.put("productTable", (Object)productTable);
            JSONArray columnNames = new JSONArray();
            columnNames.put((Object)"ITEM NUMBER");
            columnNames.put((Object)"DESCRIPTION");
            columnNames.put((Object)"QUANTITY");
            columnNames.put((Object)"UOM");
            columnNames.put((Object)"PRICE");
            columnNames.put((Object)"AMOUNT");
            productTable.put("columnNames", (Object)columnNames);
            JSONArray tableEntries = new JSONArray();
            productTable.put("entries", (Object)tableEntries);
            for (POLineItem entry : this.lineItems) {
                tableEntries.put((Object)entry.toJSON());
            }
            return root;
        }
    }

    public class POLineItem {
        String itemNumber;
        String itemDescription;
        String qty;
        String un;
        String unitPrice;
        String amount;
        String tagMemo;

        public POLineItem() {
            this.itemNumber = "";
            this.itemDescription = "";
            this.qty = "";
            this.un = "";
            this.unitPrice = "";
            this.amount = "";
            this.tagMemo = "";
        }

        public JSONObject toJSON() {
            JSONObject out = new JSONObject();
            String description = this.itemDescription;
            out.put("ITEM NUMBER", (Object)this.itemNumber);
            out.put("UOM", (Object)this.un);
            out.put("QUANTITY", (Object)this.qty);
            out.put("PRICE", (Object)this.unitPrice);
            out.put("AMOUNT", (Object)this.amount);
            String[] memoLines = this.tagMemo.split("/N");
            int i = 0;
            while (i < memoLines.length) {
                description = String.valueOf(description) + "<text:span><text:line-break /></text:span>  " + memoLines[i];
                ++i;
            }
            out.put("DESCRIPTION", (Object)description);
            return out;
        }
    }

}

