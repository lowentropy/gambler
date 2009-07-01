/*
 * PreProc.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.rules;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * PreProc is the poker pre-processor. It does three things: it removes trailing
 * backslashes from lines, it inserts the characters { and } to delimit the
 * beginning and end of sub-tabbed rules, and it strips comments. This is just a
 * pre-step to help the parser.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class PreProc
{

	public static void process(String fname, String ppFname) throws IOException
	{
		String input = readFully(fname);
		StringTokenizer st = new StringTokenizer(input, "\r\n");
		StringBuffer output = new StringBuffer();
		String eolBuf = "";
		int tablevel = 0;

		while (st.hasMoreTokens())
		{
			String line = st.nextToken();
			int idx = line.indexOf("//");
			if (idx != -1)
				line = line.substring(0, idx);
			if (line.trim().equals(""))
				continue;
			if (line.charAt(line.length() - 1) == '\\')
			{
				eolBuf += line.substring(0, line.length() - 1);
				continue;
			}
			line = eolBuf + line;
			eolBuf = "";
			int nt = countTabs(line);
			if (nt > tablevel)
				for (int i = tablevel; i < nt; i++)
					output.append('{');
			else
				for (int i = nt; i < tablevel; i++)
					output.append('}');
			tablevel = nt;
			output.append(line + "\n");
		}

		for (int i = 0; i < tablevel; i++)
			output.append('}');

		FileWriter w = new FileWriter(ppFname);
		w.write(output.toString());
		w.close();
	}


	private static int countTabs(String line)
	{
		int idx = 0;
		while (line.charAt(idx) == '\t')
			idx++;
		return idx;
	}


	private static String readFully(String fname) throws IOException
	{
		int read;
		int off = 0;
		int num = 1000;
		char[] cbuf = new char[1000];
		List<char[]> blocks = new ArrayList<char[]>();
		FileReader reader = new FileReader(fname);

		while ((read = reader.read(cbuf, off, num)) != -1)
		{
			if (read == num)
			{
				blocks.add(cbuf);
				cbuf = new char[1000];
				off = 0;
				num = 1000;
			}
			else
			{
				off += read;
				num -= read;
			}
		}
		int tot = blocks.size() * 1000 + off;
		char[] sbuf = new char[tot];
		for (int i = 0; i < blocks.size(); i++)
			System.arraycopy(blocks.get(i), 0, sbuf, i * 1000, 1000);
		System.arraycopy(cbuf, 0, sbuf, blocks.size() * 1000, off);
		return new String(sbuf);
	}
}
