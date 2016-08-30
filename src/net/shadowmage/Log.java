package net.shadowmage;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Log
{
  
  private static PrintStream logStream;
  
  public static void init()
  {
    if(logStream==null)
    {
      try
      {
        logStream = new PrintStream("log.txt");
      }
      catch (FileNotFoundException e)
      {
        e.printStackTrace();
      }
    }    
  }
  
  public static void log(String data)
  {
    logStream.print(data);
    logStream.println();
    logStream.flush();
    System.out.println(data);
  }
  
  public static void exception(Exception e)
  {
    e.printStackTrace(logStream);
    logStream.flush();
    e.printStackTrace();
  }
  
}
