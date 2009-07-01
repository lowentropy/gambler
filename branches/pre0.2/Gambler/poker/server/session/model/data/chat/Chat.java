
package poker.server.session.model.data.chat;

import java.io.IOException;

import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;

/**
 * The Chat class is a type of data container for screen schemas. It accepts
 * text from a multiline label in the visual model. It converts successive line
 * feeds into a stream of text separated by newlines. It then uses a chat
 * definition (given in a separate file) to fire events on the current XmlGame
 * object.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Chat
{

	/** name of chat data container */
	private String		name;

	/** name of file containing chat format definition */
	private String		chatFname;

	/** chat format interpreter */
	private ChatFormat	format;

	/** message buffer */
	private String[]	buf;

	private boolean		modified;


	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public Chat(XmlObject xml) throws ModelError
	{
		buf = null;
		name = xml.getValue("name");
		chatFname = xml.getValue("chatfile");
		try
		{
			format = new ChatFormat(chatFname);
		}
		catch (IOException e)
		{
			throw new ModelError("error loading chat format", e);
		}
	}


	/**
	 * Match some text to the chat window. Text is multiline and data should
	 * have a chat object.
	 * 
	 * @param text
	 *            multi-line text to match
	 * @param data
	 *            data container for results
	 * @return whether match was successful
	 */
	public synchronized boolean match(String[] text, ScreenData data)
	{
		String[] msgText;

		// replace nulls with empty strings
		boolean a = false;
		int n = 0, p = 0;
		for (int i = 0; i < text.length; i++)
			if (text[i] == null)
				text[i] = "";

		// find number of non-empty lines at start and end
		for (String s : text)
		{
			if (s.length() > 0)
			{
				a = true;
				n = 0;
			}
			else
				n++;
			if (!a)
				p++;
		}

		// fail if no non-empty lines
		if (!a)
			return false;

		// get text surrounded by emptiness
		String[] ntext = new String[text.length - n - p];
		for (int i = 0; i < ntext.length; i++)
			ntext[i] = text[i + p];
		text = ntext;

		// find out how much of the text is new
		if (buf != null)
		{
			int sep = -1;
			for (int i = 0; i < text.length; i++)
			{
				for (int j = 0; j < buf.length; j++)
				{
					if (buf[j].equals(text[i]))
					{
						sep = j - i;
						break;
					}
				}
				if (sep != -1)
					break;
			}

			if (sep < 0)
			{
				msgText = text;
				printErr(buf, text);
			}
			else
			{
				sep += (text.length - buf.length);

				// obtain new text
				msgText = new String[sep];
				for (int i = 0, j = text.length - sep; i < msgText.length; i++, j++)
					msgText[i] = text[j];
			}
		}
		else
			msgText = text;

		// reset buffer for next update
		buf = text;

		boolean mod = msgText.length > 0;

		int next = 0;
		ChatEventHandler h = data.getChatEventHandler();
		boolean af = false;

		// try 1-line matches primarily, 2-line secondarily
		while (next < msgText.length)
		{
			boolean m = format.match(msgText[next], h);
			boolean d = false;
			if (!m && (next < msgText.length - 1))
				d = format.match(msgText[next] + msgText[next + 1], h);
			if (!m && !d)
			{
				af = true;
				h.handleInvalid(msgText[next]);
			}
			next += d ? 2 : 1;
		}

		// if any matches failed, return failure
		if (!af)
			modified = mod;
		return !af;
	}


	private void printErr(String[] old, String[] new_)
	{
		System.out.printf("CHAT ERROR\nC1:");
		for (String s : old)
			System.out.printf("\t%s\n", s);
		System.out.printf("C2:");
		for (String s : new_)
			System.out.printf("\t%s\n", s);
	}


	public String getName()
	{
		return name;
	}


	public boolean wasModified()
	{
		return modified;
	}


	public void clear()
	{
		buf = null;
	}
}
