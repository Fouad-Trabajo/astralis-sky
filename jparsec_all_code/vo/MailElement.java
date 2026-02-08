/*
 * This file is part of JPARSEC library.
 *
 * (C) Copyright 2006-2020 by T. Alonso Albi - OAN (Spain).
 *
 * Project Info:  http://conga.oan.es/~alonso/jparsec/jparsec.html
 *
 * JPARSEC library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JPARSEC library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package jparsec.vo;

import javax.mail.*;
import javax.mail.internet.*;

import java.io.Serializable;
import java.util.Properties;
import javax.activation.*;

import jparsec.graph.DataSet;
import jparsec.io.*;
import jparsec.util.*;

/**
 * A class to send mails using a smtp server.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MailElement implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Natural name of the sender.
	 */
	public String name;
	/**
	 * Full login of the sender, for example somebody@somewhere.com.
	 */
	public String login;
	/**
	 * The password of the mail account.
	 */
	public String password;

	/**
	 * The name of the smtp server.
	 */
	public String smtpServer;
	/**
	 * The list of recipients for the mail.
	 */
	public String recipients[];
	/**
	 * The subject.
	 */
	public String subject;
	/**
	 * The content body of the mail.
	 */
	public String content;
	/**
	 * Sets the content type of the mail.
	 */
	public String contentType;

	/**
	 * Constant for a plain text message.
	 */
	public static final String CONTENT_TYPE_PLAIN_TEXT = "text/plain";
	/**
	 * Constant for a html message.
	 */
	public static final String CONTENT_TYPE_HTML_TEXT = "text/html";

	private String attachments[] = new String[0];

	/**
	 * Empty constructor.
	 */
	public MailElement() {}
	
	/**
	 * Main constructor.
	 * @param n Sender natural name.
	 * @param l Login.
	 * @param p Password.
	 * @param smtp SMTP server.
	 * @param r Recipients.
	 * @param s Subject.
	 * @param c Content.
	 * @param ct Content type.
	 */
	public MailElement(String n, String l, String p, String smtp, String r[], String s, String c, String ct) {
		name = n;
		login = l;
		password = p;
		smtpServer = smtp;
		recipients = r;
		subject = s;
		content = c;
		contentType = ct;
	}
	
	/**
	 * Sends the email.
	 * @throws JPARSECException If an error occurs.
	 */
	public void send() throws JPARSECException {
		this.send(this.login);
	}

	/**
	 * Sends the email.
	 * @param from Mail address of the sender. Can be
	 * different from {@link #login} only if the server allows
	 * to send messages as written from other accounts.
	 * @throws JPARSECException If an error occurs.
	 */
	public void send(String from)
	throws JPARSECException {
		String server = "smtp";
		if (this.smtpServer.indexOf("pop") >= 0) server = "pop";
		Properties prop = new Properties();
		prop.put("mail."+server+".host", this.smtpServer);
		prop.put("mail."+server+".auth", "true");
		prop.put("mail."+server+".starttls.enable","true");

		try{
			Session session = Session.getInstance(prop , null );
			Message msg = getMessage(session);
			if (!from.equals(this.login)) msg.setFrom(new InternetAddress(from, this.name));

			Transport t = session.getTransport(server);
			try {
			    t.connect(this.login.substring(0, this.login.indexOf("@")), this.password);
			    t.sendMessage(msg, msg.getAllRecipients());
			} finally {
			    t.close();
			}
		}
		catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	private MimeMessage getMessage(Session session)
	throws JPARSECException {
		try{
			MimeMessage msg = new MimeMessage(session);
			msg.setSubject(subject);
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(this.recipients[0]));
			msg.setFrom(new InternetAddress(this.login, this.name));
			msg.setContent(this.content, this.contentType);
			this.setContent(msg);
			for (int i=1; i<this.recipients.length; i++)
			{
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(this.recipients[i]));
			}
			return msg;
		}
		catch (java.io.UnsupportedEncodingException ex)
		{
			throw new JPARSECException(ex);
		}
		catch (MessagingException ex)
		{
			throw new JPARSECException(ex);
		}
	}

	/**
	 * Adds an attachment to this message. Note that attachments
	 * can be added at any location in the current text message.
	 * A string like '[ATTACHMENT]' will be added to the current
	 * contents to identify the attachment position, so you should
	 * not use any text like this in the body by yourself.
	 * @param fileName Path to the file to attach.
	 */
	public void addAttachment(String fileName)
	{
		this.content += "[ATTACHMENT]";
		attachments = DataSet.addStringArray(attachments, fileName);
	}

    private void setContent(Message msg)
             throws MessagingException
    {
    	int n = FileIO.getNumberOfFields(this.content, "[ATTACHMENT]", true);
    	if (n == 1) return;

        Multipart mp = new MimeMultipart();

    	for (int i=0; i<n; i++)
    	{
    		String part = FileIO.getField(i+1, this.content, "[ATTACHMENT]", true);

            // Create and fill first part
            MimeBodyPart p1 = new MimeBodyPart();
            p1.setContent(part, this.contentType);
            mp.addBodyPart(p1);

            if (i < n-1)
            {
	            // Create second part
	            MimeBodyPart p2 = new MimeBodyPart();

	            // Put a file in the second part
	            FileDataSource fds = new FileDataSource(this.attachments[i]);
	            p2.setDataHandler(new DataHandler(fds));
	            p2.setFileName(fds.getName());

	            mp.addBodyPart(p2);
            }
    	}

        // Set Multipart as the message's content
        msg.setContent(mp);
    }
}
