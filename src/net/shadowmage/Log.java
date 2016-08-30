package net.shadowmage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Properties;

import net.shadowmage.po.EmailSender;

public class Log
{
  
  private static PrintStream logStream;
  private static String errorEmail = "johnc@croakies.com";
  private static String emailHost;
  private static String emailUser;
  private static String emailSender;
  
  public static void init(Properties config)
  {
    errorEmail = config.getProperty("errorEmail");
    emailHost = config.getProperty("emailHost");
    emailUser = config.getProperty("emailUser");
    emailSender = config.getProperty("emailSender");
    try
    {
      logStream = new PrintStream("log.txt");
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
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
    e.printStackTrace();
    e.printStackTrace(logStream);
    logStream.flush();
    if(!errorEmail.isEmpty())
    {
      log("Sending error email to: "+errorEmail);
      EmailSender.sendEmail(emailSender, emailHost, emailUser, new String[]{errorEmail}, "Automated Program Error Email", "See attached log for details on the error.", new File("log.txt"));
    }
  }
  
}
