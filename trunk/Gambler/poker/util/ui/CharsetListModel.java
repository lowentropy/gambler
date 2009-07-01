/*
 * CharsetListModel.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import poker.ai.ocr.Charset;


public class CharsetListModel implements ComboBoxModel
{

	private JComboBox				cb;

	private List<Charset>			charsets;

	private Charset					selected	= null;

	private List<ListDataListener>	listeners;

	private CharsetFrame			frame;


	/**
	 * Constructor.
	 * 
	 * @param cb
	 */
	public CharsetListModel(JComboBox cb, CharsetFrame frame)
	{
		this.cb = cb;
		listeners = new LinkedList<ListDataListener>();
		this.frame = frame;
		loadCharsets();
	}


	/**
	 * Load charsets.
	 */
	private void loadCharsets()
	{
		File dir = new File("charsets");
		File[] files = dir.listFiles();
		charsets = new ArrayList<Charset>(files.length);
		for (File f : files)
		{
			if (f.getName().startsWith("."))
				continue;

			try
			{
				charsets.add(Charset.load(f));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}


	/**
	 * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
	 */
	public void setSelectedItem(Object anItem)
	{
		selected = (Charset) anItem;
		frame.cmodel.changeCharset(selected);
		frame.pack();

		frame.setToleranceField(selected.getTolerance());
	}


	public Object getSelectedItem()
	{
		return selected;
	}


	public int getSize()
	{
		return charsets.size() == 0 ? 1 : charsets.size();
	}


	public Object getElementAt(int index)
	{
		if (charsets.size() == 0 && index == 0)
			return null;
		return charsets.get(index);
	}


	public void addListDataListener(ListDataListener l)
	{
		listeners.add(l);

		if (charsets.size() == 0)
			return;

		l.intervalAdded(new ListDataEvent(cb, ListDataEvent.INTERVAL_ADDED, 0,
				charsets.size() - 1));
	}


	public void removeListDataListener(ListDataListener l)
	{
		listeners.remove(l);
	}


	public void remove(Charset cs)
	{
		int idx = charsets.indexOf(cs);
		charsets.remove(idx);
		selected = null;

		frame.cmodel.clear();

		for (ListDataListener l : listeners)
		{
			l.intervalRemoved(new ListDataEvent(cb,
					ListDataEvent.INTERVAL_REMOVED, idx, idx));
			l.contentsChanged(new ListDataEvent(cb,
					ListDataEvent.CONTENTS_CHANGED, 0, 0));
		}

	}


	public Charset add(String name)
	{
		for (Charset ocs : charsets)
			if (ocs.getName().equals(name))
			{
				JOptionPane.showMessageDialog(frame,
						"Charset name already exists.");
				return null;
			}

		Charset cs = new Charset(name, frame.fg, frame.tolerance);
		try
		{
			cs.save();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		int n = charsets.size();
		charsets.add(cs);
		selected = cs;
		frame.cmodel.changeCharset(cs);

		for (ListDataListener l : listeners)
			l.intervalAdded(new ListDataEvent(cb, ListDataEvent.INTERVAL_ADDED,
					n, n));

		return cs;
	}

}
