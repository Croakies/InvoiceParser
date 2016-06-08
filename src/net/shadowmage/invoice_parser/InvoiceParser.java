/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package net.shadowmage.invoice_parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class InvoiceParser {
    public Logger log;
    protected File inputFile;
    protected File outputPath;
    protected List<File> outputFiles = new ArrayList<File>();

    public InvoiceParser(File inputFile, File outputPath) {
        if (!inputFile.exists()) {
            System.out.print("Cannot locate input file for name: " + inputFile.getAbsolutePath());
            return;
        }
        outputPath.mkdirs();
        this.inputFile = inputFile;
        this.outputPath = outputPath;
    }

    private void log(String message) {
        this.log.log(Level.SEVERE, message);
    }

    public final List<File> getParsedFiles() {
        return this.outputFiles;
    }

    public void parseFile() {
        try {
            this.log("parsing file: " + this.inputFile);
            this.parseLines(Files.readAllLines(this.inputFile.toPath(), Charset.defaultCharset()));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final void parseLines(List<String> fileData) {
        ArrayList<List<String>> invoicePages = new ArrayList<List<String>>();
        ArrayList<String> currentLines = new ArrayList<String>();
        HashMap<String, InvoiceData> invoicesByNumber = new HashMap<String, InvoiceData>();
        for (String line : fileData) {
            if (line.startsWith("\f")) {
                line = line.substring(1);
                if (!currentLines.isEmpty()) {
                    invoicePages.add(currentLines);
                    currentLines = new ArrayList<String>();
                }
            }
            currentLines.add(line);
        }
        this.log("parsed: " + invoicePages.size() + " invoice pages");
        for (List<String> page : invoicePages) {
            String invoiceNumber = this.getInvoiceNumber(page);
            if (!invoicesByNumber.containsKey(invoiceNumber)) {
                InvoiceData data = new InvoiceData(invoiceNumber);
                invoicesByNumber.put(invoiceNumber, data);
            }
            ((InvoiceData)invoicesByNumber.get(invoiceNumber)).addInvoicePage(page);
        }
        for (InvoiceData data : invoicesByNumber.values()) {
            data.process();
        }
        for (String num : invoicesByNumber.keySet()) {
            String name = this.outputPath + "/" + num + "-output";
            File outputFile = new File(name);
            JSONObject jsonOutputData = ((InvoiceData)invoicesByNumber.get(num)).toJSON();
            this.outputFiles.add(outputFile);
            this.log("saving JSON output to: " + outputFile.getAbsolutePath());
            try {
                FileWriter fw = new FileWriter(outputFile);
                fw.write(jsonOutputData.toString(2));
                fw.close();
                continue;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getInvoiceNumber(List<String> invoicePageLines) {
        String invoiceNumberAndPage = invoicePageLines.get(3).trim();
        String invoiceNumber = invoiceNumberAndPage.substring(14, 22).trim();
        return invoiceNumber;
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
    private class InvoiceData {
        private List<String> headerLines;
        private List<String> tagMemoLines;
        private List<String> tableLines;
        private String invoiceNumber;
        private String shipName;
        private String shipAddress1;
        private String shipAddress2;
        private String shipAddress3;
        private String billName;
        private String billAddress1;
        private String billAddress2;
        private String billAddress3;
        private String customerNumber;
        private String invoiceDate;
        private String dueDate;
        private String poNumber;
        private String shipVia;
        private String orderNumber;
        private String slsNumber;
        private String terms;
        private String departmentNumber;
        private String requiredShipDate;
        private String cancelDate;
        private String orderCode;
        private String priceLevel;
        private String shipNumber;
        private String grossTotal;
        private String shippingCharges;
        private String discountPercent;
        private String discountValue;
        private String additionalCharges;
        private String orderTotal;
        private String tagMemo;
        private String isInvoice;
        private List<InvoiceTableEntry> tableData;
        private boolean parsedHeader;

        private InvoiceData(String invoiceNumber) {
            this.headerLines = new ArrayList<String>();
            this.tagMemoLines = new ArrayList<String>();
            this.tableLines = new ArrayList<String>();
            this.invoiceNumber = " ";
            this.shipName = " ";
            this.shipAddress1 = " ";
            this.shipAddress2 = " ";
            this.shipAddress3 = " ";
            this.billName = " ";
            this.billAddress1 = " ";
            this.billAddress2 = " ";
            this.billAddress3 = " ";
            this.customerNumber = " ";
            this.invoiceDate = " ";
            this.dueDate = " ";
            this.poNumber = " ";
            this.shipVia = " ";
            this.orderNumber = " ";
            this.slsNumber = " ";
            this.terms = " ";
            this.departmentNumber = " ";
            this.requiredShipDate = " ";
            this.cancelDate = " ";
            this.orderCode = " ";
            this.priceLevel = " ";
            this.shipNumber = " ";
            this.grossTotal = "0.00";
            this.shippingCharges = "0.00";
            this.discountPercent = "0";
            this.discountValue = "0.00";
            this.additionalCharges = "0.00";
            this.orderTotal = " ";
            this.tagMemo = "";
            this.isInvoice = "INVOICE";
            this.tableData = new ArrayList<InvoiceTableEntry>();
            this.parsedHeader = false;
            this.invoiceNumber = invoiceNumber;
            log("Creating new invoice data for invoice number: " + invoiceNumber);
        }

        public void addInvoicePage(List<String> lines) {
            int i;
            if (!this.parsedHeader) {
                this.parsedHeader = true;
                i = 0;
                while (i < 22) {
                    this.addHeaderLine(lines.get(i));
                    ++i;
                }
            }
            i = 22;
            while (i < lines.size()) {
                this.addTableLine(lines.get(i));
                ++i;
            }
        }

        public void addHeaderLine(String line) {
            log("added header line to parse of: " + line);
            this.headerLines.add(line);
        }

        public void addTableLine(String line) {
            log("added table line to parse of: " + line);
            this.tableLines.add(line);
        }

        public void process() {
            this.parseHeaderLines(this.headerLines);
            this.parseTableLines(this.tableLines);
            int i = 0;
            while (i < this.tagMemoLines.size()) {
                String line = sanatizeForXML(this.tagMemoLines.get(i));
                this.tagMemo = String.valueOf(this.tagMemo) + line;
                if (i < this.tagMemoLines.size() - 1) {
                    this.tagMemo = String.valueOf(this.tagMemo) + "<text:span><text:line-break /></text:span>";
                }
                ++i;
            }
            if (this.grossTotal.equals("0.00")) {
                this.grossTotal = this.orderTotal;
            }
        }

        private JSONObject toJSON() {
            JSONObject root = new JSONObject();
            JSONObject dataFields = new JSONObject();
            root.put("dataFields", (Object)dataFields);
            dataFields.put("invoiceNum", (Object)this.invoiceNumber);
            dataFields.put("invDate", (Object)this.invoiceDate);
            dataFields.put("custNum", (Object)this.customerNumber);
            dataFields.put("shipName", (Object)this.shipName);
            dataFields.put("shipAdd1", (Object)this.shipAddress1);
            dataFields.put("shipAdd2", (Object)this.shipAddress2);
            dataFields.put("shipAdd3", (Object)this.shipAddress3);
            dataFields.put("billName", (Object)this.billName);
            dataFields.put("billAdd1", (Object)this.billAddress1);
            dataFields.put("billAdd2", (Object)this.billAddress2);
            dataFields.put("billAdd3", (Object)this.billAddress3);
            dataFields.put("dueDate", (Object)this.dueDate);
            dataFields.put("poNum", (Object)this.poNumber);
            dataFields.put("shipVia", (Object)this.shipVia);
            dataFields.put("orderNum", (Object)this.orderNumber);
            dataFields.put("slsNum", (Object)this.slsNumber);
            dataFields.put("terms", (Object)this.terms);
            dataFields.put("deptNum", (Object)this.departmentNumber);
            dataFields.put("reqShip", (Object)this.requiredShipDate);
            dataFields.put("cancel", (Object)this.cancelDate);
            dataFields.put("orderCode", (Object)this.orderCode);
            dataFields.put("pl", (Object)this.priceLevel);
            dataFields.put("shipNum", (Object)this.shipNumber);
            dataFields.put("isInvoice", (Object)this.isInvoice);
            dataFields.put("orderTotal", (Object)this.orderTotal);
            dataFields.put("grossTotal", (Object)this.grossTotal);
            dataFields.put("discountPercent", (Object)this.discountPercent);
            dataFields.put("discountValue", (Object)this.discountValue);
            dataFields.put("additionalCharges", (Object)this.additionalCharges);
            dataFields.put("shippingCharges", (Object)this.shippingCharges);
            dataFields.put("tagMemo", (Object)this.tagMemo);
            JSONObject tableData = new JSONObject();
            root.put("tableData", (Object)tableData);
            JSONObject productTable = new JSONObject();
            tableData.put("productTable", (Object)productTable);
            JSONArray columnNames = new JSONArray();
            columnNames.put((Object)"ITEM NUMBER");
            columnNames.put((Object)"DESCRIPTION");
            columnNames.put((Object)"UOM");
            columnNames.put((Object)"QUANTITY");
            columnNames.put((Object)"PRICE");
            columnNames.put((Object)"AMOUNT");
            productTable.put("columnNames", (Object)columnNames);
            JSONArray tableEntries = new JSONArray();
            productTable.put("entries", (Object)tableEntries);
            for (InvoiceTableEntry entry : this.tableData) {
                tableEntries.put((Object)entry.toJSON());
            }
            return root;
        }

        private void parseHeaderLines(List<String> headerLines) {
            if (headerLines.get(1).startsWith("                                               *** CREDIT ***")) {
                this.isInvoice = "CREDIT";
            }
            String invoiceNumberAndPage = headerLines.get(3).trim();
            this.invoiceNumber = invoiceNumberAndPage.substring(14, 22).trim();
            this.billName = headerLines.get(4).trim();
            this.billAddress1 = headerLines.get(5).trim();
            this.billAddress2 = headerLines.get(6).trim();
            this.billAddress3 = headerLines.get(7).substring(0, 50).trim();
            String lineEight = headerLines.get(7);
            this.customerNumber = lineEight.substring(50, 71).trim();
            this.invoiceDate = lineEight.substring(71, lineEight.length()).trim();
            this.shipName = headerLines.get(10).trim();
            this.shipAddress1 = headerLines.get(11).trim();
            this.shipAddress2 = headerLines.get(12).trim();
            this.shipAddress3 = headerLines.get(13).trim();
            this.dueDate = headerLines.get(15).trim();
            String poLine = headerLines.get(18);
            this.poNumber = poLine.substring(0, 12).trim();
            this.shipVia = poLine.substring(12, 44).trim();
            this.orderNumber = poLine.substring(44, 52).trim();
            this.slsNumber = poLine.substring(52, 60).trim();
            this.terms = poLine.substring(60, poLine.length()).trim();
            String deptLine = headerLines.get(21);
            this.departmentNumber = deptLine.substring(0, 12).trim();
            this.requiredShipDate = deptLine.substring(12, 24).trim();
            this.cancelDate = deptLine.substring(24, 40).trim();
            this.orderCode = deptLine.substring(40, 52).trim();
            this.priceLevel = deptLine.substring(52, 60).trim();
            this.shipNumber = deptLine.substring(60, deptLine.length()).trim();
        }

        private void parseTableLines(List<String> tableDataLines) {
            log("Parsing table data of size: " + tableDataLines.size());
            InvoiceTableEntry entry = null;
            int i = 0;
            while (i < tableDataLines.size()) {
                String line = tableDataLines.get(i);
                log("Processing table line: " + i + " :: " + line);
                if (line.startsWith("                                                 Pay This Amount")) {
                    line = line.trim();
                    log("processing totals line: " + line);
                    this.orderTotal = line.substring(15).trim();
                } else if (line.startsWith("                                                           Continued:")) {
                    log("Skipping continued on next page line");
                } else if (line.startsWith("                             *** CREDIT ***                Continued:")) {
                    log("Skipping credit continued on next page line");
                } else if (line.startsWith("                                               Total Gross")) {
                    this.grossTotal = line.substring(59).trim();
                } else if (line.startsWith("                                       Order disc :")) {
                    line = line.substring(52).trim();
                    this.discountPercent = line.substring(0, 10).trim();
                    this.discountValue = line.substring(10).trim();
                    log("parsing discount line: " + line + " : perc: " + this.discountPercent + " :: val: " + this.discountValue);
                } else if (line.startsWith("                                         ADDITIONAL CHARGES")) {
                    this.additionalCharges = line.substring(59).trim();
                    log("parsed additional charges line: " + this.additionalCharges);
                    if (this.additionalCharges.isEmpty()) {
                        this.additionalCharges = "0.00";
                    }
                } else if (line.startsWith("                                        SHIPPING & HANDLING")) {
                    this.shippingCharges = line.substring(59).trim();
                    if (this.shippingCharges.isEmpty()) {
                        this.shippingCharges = "0.00";
                    }
                } else if (line.startsWith("                   ") && line.charAt(20) != ' ') {
                    line = line.trim();
                    log("Parsed table entry line comment of: " + line);
                    if (entry == null) {
                        log("ENTRY WAS NULL!");
                    } else {
                        entry.addCommentData(line);
                    }
                } else if (line.startsWith("             ") && line.trim().length() > 0) {
                    if (i >= 36) {
                        log("Skipping second page tag memo line");
                    } else {
                        line = line.trim();
                        log("found tag memo line of: " + line);
                        this.tagMemoLines.add(line);
                    }
                } else if (line.startsWith("  ")) {
                    if ((line = line.trim()).isEmpty()) {
                        log("Skipping empty line at index: " + i);
                    } else {
                        log("Parsed table entry SKU data of: " + line);
                        entry.setSKUData(line);
                    }
                } else if (!line.startsWith(" ") && !line.isEmpty()) {
                    if ((line = line.trim()).isEmpty()) {
                        log("Skipping empty line at index: " + i);
                    } else {
                        log("Parsing line number: " + i + " data: " + line + " as table entry!");
                        entry = this.parseTableEntry(line);
                    }
                } else {
                    log("unprocessed line: " + line);
                }
                ++i;
            }
        }

        private InvoiceTableEntry parseTableEntry(String line) {
            InvoiceTableEntry entry = new InvoiceTableEntry(line);
            this.tableData.add(entry);
            return entry;
        }

//        /* synthetic */ InvoiceData(InvoiceParser invoiceParser, String string, InvoiceData invoiceData) {
//            InvoiceData invoiceData2;
//            invoiceData2(invoiceParser, string);
//        }
    }

    private class InvoiceTableEntry {
        private String itemCode;
        private String itemDescription;
        private String unitOfMeasure;
        private String quantity;
        private String price;
        private String amount;
        private String skuData;
        private List<String> commentData;
        
        private InvoiceTableEntry(String line) {
            this.commentData = new ArrayList<String>();
            this.itemCode = line.substring(0, 13).trim();
            this.itemDescription = line.substring(13, 45).trim();
            this.unitOfMeasure = line.substring(45, 50).trim();
            this.quantity = line.substring(50, 55).trim();
            this.price = line.substring(55, 66).trim();
            this.amount = line.substring(66, 78).trim();
        }

        public void setSKUData(String skuData) {
            this.skuData = skuData;
        }

        public void addCommentData(String line) {
            this.commentData.add(line);
        }

        private JSONObject toJSON() {
            JSONObject out = new JSONObject();
            String description = this.itemDescription;
            out.put("ITEM NUMBER", (Object)this.itemCode);
            out.put("UOM", (Object)this.unitOfMeasure);
            out.put("QUANTITY", (Object)this.quantity);
            out.put("PRICE", (Object)this.price);
            out.put("AMOUNT", (Object)this.amount);
            if (this.skuData != null && this.skuData.length() > 0) {
                this.skuData = sanatizeForXML(this.skuData);
                description = String.valueOf(description) + "<text:span><text:line-break /></text:span>SKU: " + this.skuData;
            }
            if (this.commentData.size() > 0) {
                int i = 0;
                while (i < this.commentData.size()) {
                    description = String.valueOf(description) + "<text:span><text:line-break /></text:span>  " + this.commentData.get(i);
                    ++i;
                }
            }
            out.put("DESCRIPTION", (Object)description);
            return out;
        }

//        /* synthetic */ InvoiceTableEntry(InvoiceParser invoiceParser, String string, InvoiceTableEntry invoiceTableEntry) {
//            InvoiceTableEntry invoiceTableEntry2;
//            invoiceTableEntry2(invoiceParser, string);
//        }
    }

}

