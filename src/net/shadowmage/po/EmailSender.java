package net.shadowmage.po;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.shadowmage.Log;

public class EmailSender
{
  
  public static void sendEmail(String sender, String host, String user, String[] recipients, String subject, String bodyText, File toAttach)
  {
    if(recipients==null || recipients.length<=0){return;}
    Properties props = System.getProperties();
    props.setProperty("mail.smtp.host", host);
    props.setProperty("mail.user", user);
    props.setProperty("mail.password", "omitted");
    Session session = Session.getDefaultInstance(props);
    try
    {
      MimeMessage baseMessage = new MimeMessage(session);
      baseMessage.setFrom((Address) new InternetAddress(sender));
      for (String name : recipients)
      {
        baseMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(name));
      }
      baseMessage.setSubject(subject);
      MimeBodyPart messageBodyText = new MimeBodyPart();
      messageBodyText.setText(bodyText);
      FileDataSource source = new FileDataSource(toAttach);
      DataHandler handler = new DataHandler(source);
      MimeBodyPart messageAttachmentPart = new MimeBodyPart();
      messageAttachmentPart.setDataHandler(handler);
      messageAttachmentPart.setFileName(toAttach.getName());
      MimeMultipart mp = new MimeMultipart();
      mp.addBodyPart((BodyPart) messageBodyText);
      mp.addBodyPart((BodyPart) messageAttachmentPart);
      baseMessage.setContent((Multipart) mp);
      Transport.send((Message) baseMessage);
      Log.log("Emailed "+toAttach.getName()+" to: "+recipients[0]+" with subject: "+subject);
    }
    catch (Exception e)
    {
      Log.exception(e);
    }    
  }
  
}
