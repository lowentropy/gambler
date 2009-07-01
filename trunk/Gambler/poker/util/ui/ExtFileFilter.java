/*
 * ExtFileFilter.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.io.File;

import javax.swing.filechooser.FileFilter;


public class ExtFileFilter extends FileFilter
{

	private String[]	exts;


	public ExtFileFilter(String[] exts)
	{
		this.exts = exts;
	}


	public boolean accept(File f)
	{
		if (f.isDirectory())
			return true;
		int idx = f.getName().lastIndexOf('.');
		if (idx == -1)
			return false;
		String ext = f.getName().substring(idx + 1);
		for (String aext : exts)
			if (ext.equals(aext))
				return true;
		return false;
	}


	public String getDescription()
	{
		return "Files of these extensions: " + descExts();
	}


	private String descExts()
	{
		StringBuilder b = new StringBuilder();
		for (String s : exts)
			b.append("." + s + ", ");
		if (exts.length > 0)
			b.setLength(b.length() - 2);
		return b.toString();
	}

}
