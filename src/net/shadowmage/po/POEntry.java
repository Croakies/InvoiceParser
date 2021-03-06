package net.shadowmage.po;

import java.util.ArrayList;
import java.util.List;

import net.shadowmage.Util;

import org.json.JSONArray;
import org.json.JSONObject;

public class POEntry
{
  
  String poNumber = "";
  String vendorName = "";
  String vendorAdd1 = "";
  String vendorAdd2 = "";
  String vendorAdd3 = "";
  String vendorAdd4 = "";
  String shipToName = "";
  String shipToAdd1 = "";
  String shipToAdd2 = "";
  String shipToAdd3 = "";
  String shipToAdd4 = "";
  String billToName = "";
  String billToAdd1 = "";
  String billToAdd2 = "";
  String billToAdd3 = "";
  String billToAdd4 = "";
  String vendorPhone = "";
  String vendorFax = "";
  String contact = "";
  String vendorEmail = "";
  String sendEmail = "";
  String poDate = "";
  String vendorNumber = "";
  String shipVia = "";
  String fobDescritpion = "";
  String terms = "";
  String deliveryDate = "";
  String placedWith = "";
  String freight = "";
  String buyer = "";
  String grandTotal = "";
  String tagMemo = "";
  List<POLineItem> lineItems = new ArrayList<POLineItem>();
  
  public POEntry()
  {
    
  }
    
  public void parseRawLines(List<String> rawLines)
  {
    int len = rawLines.size();
    int i = 0;
    while (i < len)
    {
      String line = rawLines.get(i);
      System.out.println("Parsing line: "+line);
      if (line.startsWith("poNumber:"))
      {
        poNumber = Util.getLineValue(line);
      }
      else if (line.startsWith("vendorName:"))
      {
        vendorName = Util.cleanVendorName(Util.getLineValue(line));
      }
      else if (line.startsWith("vendorAdd1:"))
      {
        vendorAdd1 = Util.getLineValue(line);
      }
      else if (line.startsWith("vendorAdd2:"))
      {
        vendorAdd2 = Util.getLineValue(line);
      }
      else if (line.startsWith("vendorAdd3:"))
      {
        vendorAdd3 = Util.getLineValue(line);
      }
      else if (line.startsWith("vendorAdd4:"))
      {
        vendorAdd4 = Util.getLineValue(line);
      }
      else if (line.startsWith("shipToName:"))
      {
        shipToName = Util.getLineValue(line);
      }
      else if (line.startsWith("shipToAdd1:"))
      {
        shipToAdd1 = Util.getLineValue(line);
      }
      else if (line.startsWith("shipToAdd2:"))
      {
        shipToAdd2 = Util.getLineValue(line);
      }
      else if (line.startsWith("shipToAdd3:"))
      {
        shipToAdd3 = Util.getLineValue(line);
      }
      else if (line.startsWith("shipToAdd4:"))
      {
        shipToAdd4 = Util.getLineValue(line);
      }
      else if (line.startsWith("billToName:"))
      {
        billToName = Util.getLineValue(line);
      }
      else if (line.startsWith("billToAdd1:"))
      {
        billToAdd1 = Util.getLineValue(line);
      }
      else if (line.startsWith("billToAdd2:"))
      {
        billToAdd2 = Util.getLineValue(line);
      }
      else if (line.startsWith("billToAdd3:"))
      {
        billToAdd3 = Util.getLineValue(line);
      }
      else if (line.startsWith("billToAdd4:"))
      {
        billToAdd4 = Util.getLineValue(line);
      }
      else if (line.startsWith("vendorPhone:"))
      {
        vendorPhone = Util.getLineValue(line);
      }
      else if (line.startsWith("vendorFax:"))
      {
        vendorFax = Util.getLineValue(line);
      }
      else if (line.startsWith("vendor email:"))
      {
        vendorEmail = Util.getLineValue(line);
      }
      else if (line.startsWith("email po y/N:"))
      {
        sendEmail = Util.getLineValue(line);
      }
      else if (line.startsWith("contact:"))
      {
        contact = Util.getLineValue(line);
      }
      else if (line.startsWith("poDate:"))
      {
        poDate = Util.getLineValue(line);
      }
      else if (line.startsWith("vendorNumber:"))
      {
        vendorNumber = Util.getLineValue(line);
      }
      else if (line.startsWith("shipVia:"))
      {
        shipVia = Util.getLineValue(line);
      }
      else if (line.startsWith("fobDescritpion:"))
      {
        fobDescritpion = Util.getLineValue(line);
      }
      else if (line.startsWith("terms:"))
      {
        terms = Util.getLineValue(line);
      }
      else if (line.startsWith("deliveryDate:"))
      {
        deliveryDate = Util.getLineValue(line);
      }
      else if (line.startsWith("placedWith:"))
      {
        placedWith = Util.getLineValue(line);
      }
      else if (line.startsWith("freight:"))
      {
        freight = Util.getLineValue(line);
      }
      else if (line.startsWith("buyer:"))
      {
        buyer = Util.getLineValue(line);
      }
      else if (line.startsWith("grandTotal:"))
      {
        grandTotal = Util.getFormattedDecimalValue(Util.getLineValue(line));
      }
      else if (line.startsWith("Global Tag Memo:"))
      {
        tagMemo = Util.sanatizeForXML(Util.getLineValue(line));
      }
      else if (line.startsWith("itemNumber:"))
      {
        i = this.parseItem(rawLines, i);
      }
      else if(line.startsWith("Send Email:"))
      {
        System.out.println("Found email override of: "+ Util.getLineValue(line));
      }
      else
      {
        System.out.println("Found unsupported line: " + line);
      }
      ++i;
    }
  }
  
  private int parseItem(List<String> lines, int start)
  {
    int len;
    POLineItem item = new POLineItem();
    int lineNumber = len = lines.size();
    lineNumber = start;
    while (lineNumber < len)
    {
      String line = lines.get(lineNumber);
      if (lineNumber > start && line.startsWith("itemNumber:"))
      {
        --lineNumber;
        break;
      }
      if (line.startsWith("itemNumber:"))
      {
        item.itemNumber = Util.getLineValue(line);
      }
      else if (line.startsWith("itemDescription:"))
      {
        item.itemDescription = Util.getLineValue(line);
      }
      else if (line.startsWith("qty:"))
      {
        item.qty = Util.getLineValue(line);
      }
      else if (line.startsWith("uom:"))
      {
        item.un = Util.getLineValue(line);
      }
      else if (line.startsWith("unitPrice:"))
      {
        item.unitPrice = Util.getLineValue(line);
        item.unitPrice = Util.getFormattedDecimalValue(item.unitPrice);
      }
      else if (line.startsWith("amount:"))
      {
        item.amount = Util.getLineValue(line);
        item.amount = Util.getFormattedDecimalValue(item.amount);
      }
      else if (line.startsWith("itemTagMemo:"))
      {
        item.tagMemo = Util.sanatizeForXML(Util.getLineValue(line));
      }
      ++lineNumber;
    }
    this.lineItems.add(item);
    return lineNumber;
  }
      
  private String getCombinedTagMemo(String input)
  {
    String output = "";
    String[] memoLines = this.tagMemo.split("/N");
    int i = 0;
    while (i < memoLines.length)
    {
      output = String.valueOf(output) + "<text:span><text:line-break /></text:span>  " + memoLines[i];
      ++i;
    }
    return output;
  }
  
  public JSONObject toJson()
  {
    if (this.vendorAdd2.isEmpty())
    {
      this.vendorAdd2 = this.vendorAdd3;
      this.vendorAdd3 = "";
    }
    if (this.vendorAdd3.isEmpty())
    {
      this.vendorAdd3 = this.vendorAdd4;
      this.vendorAdd4 = "";
    }
    if (this.shipToAdd2.isEmpty())
    {
      this.shipToAdd2 = this.shipToAdd3;
      this.shipToAdd3 = "";
    }
    if (this.shipToAdd3.isEmpty())
    {
      this.shipToAdd3 = this.shipToAdd4;
      this.shipToAdd4 = "";
    }
    if (this.billToAdd3.isEmpty())
    {
      this.billToAdd3 = this.billToAdd4;
      this.billToAdd4 = "";
    }
    
    JSONObject root = new JSONObject();
    root.put("fileName", vendorName+"-"+poNumber);
    
    JSONObject dataFields = new JSONObject();
    root.put("dataFields", (Object) dataFields);
    dataFields.put("poNumber", (Object) this.poNumber);
    dataFields.put("poDate", (Object) this.poDate);
    dataFields.put("vendorName", (Object) this.vendorName);
    dataFields.put("vendorAdd1", (Object) this.vendorAdd1);
    dataFields.put("vendorAdd2", (Object) this.vendorAdd2);
    dataFields.put("vendorAdd3", (Object) this.vendorAdd3);
    dataFields.put("vendorAdd4", (Object) this.vendorAdd4);
    dataFields.put("shipName", (Object) this.shipToName);
    dataFields.put("shipAdd1", (Object) this.shipToAdd1);
    dataFields.put("shipAdd2", (Object) this.shipToAdd2);
    dataFields.put("shipAdd3", (Object) this.shipToAdd3);
    dataFields.put("shipAdd4", (Object) this.shipToAdd4);
    dataFields.put("billName", (Object) this.billToName);
    dataFields.put("billAdd1", (Object) this.billToAdd1);
    dataFields.put("billAdd2", (Object) this.billToAdd2);
    dataFields.put("billAdd3", (Object) this.billToAdd3);
    dataFields.put("billAdd4", (Object) this.billToAdd4);
    dataFields.put("poDate", (Object) this.poDate);
    dataFields.put("vendorPhone", (Object) this.vendorPhone);
    dataFields.put("vendorFax", (Object) this.vendorFax);
    dataFields.put("contact", (Object) this.contact);
    dataFields.put("vendorNumber", (Object) this.vendorNumber);
    dataFields.put("shipVia", (Object) this.shipVia);
    dataFields.put("fobDescription", (Object) this.fobDescritpion);
    dataFields.put("terms", (Object) this.terms);
    dataFields.put("deliveryDate", (Object) this.deliveryDate);
    dataFields.put("placedWith", (Object) this.placedWith);
    dataFields.put("freight", (Object) this.freight);
    dataFields.put("buyer", (Object) this.buyer);
    dataFields.put("grandTotal", (Object) this.grandTotal);
    dataFields.put("tagMemo", (Object) this.getCombinedTagMemo(this.tagMemo));
    JSONObject tableData = new JSONObject();
    root.put("tableData", (Object) tableData);
    JSONObject productTable = new JSONObject();
    tableData.put("productTable", (Object) productTable);
    JSONArray columnNames = new JSONArray();
    columnNames.put((Object) "ITEM NUMBER");
    columnNames.put((Object) "DESCRIPTION");
    columnNames.put((Object) "QUANTITY");
    columnNames.put((Object) "UOM");
    columnNames.put((Object) "PRICE");
    columnNames.put((Object) "AMOUNT");
    productTable.put("columnNames", (Object) columnNames);
    JSONArray tableEntries = new JSONArray();
    productTable.put("entries", (Object) tableEntries);
    for (POLineItem entry : this.lineItems)
    {
      tableEntries.put((Object) entry.toJSON());
    }
    return root;
  }
  
  private class POLineItem
  {
    String itemNumber;
    String itemDescription;
    String qty;
    String un;
    String unitPrice;
    String amount;
    String tagMemo;
    
    public POLineItem()
    {
      this.itemNumber = "";
      this.itemDescription = "";
      this.qty = "";
      this.un = "";
      this.unitPrice = "";
      this.amount = "";
      this.tagMemo = "";
    }
    
    public JSONObject toJSON()
    {
      JSONObject out = new JSONObject();
      String description = this.itemDescription;
      out.put("ITEM NUMBER", (Object) this.itemNumber);
      out.put("UOM", (Object) this.un);
      out.put("QUANTITY", (Object) this.qty);
      out.put("PRICE", (Object) this.unitPrice);
      out.put("AMOUNT", (Object) this.amount);
      String[] memoLines = this.tagMemo.split("/N");
      int i = 0;
      while (i < memoLines.length)
      {
        description = String.valueOf(description) + "<text:span><text:line-break /></text:span>  " + memoLines[i];
        ++i;
      }
      out.put("DESCRIPTION", (Object) description);
      return out;
    }
  }
  
}
