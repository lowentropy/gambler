/*
 * CharListModel.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import poker.ai.ocr.Charset;


public class CharListModel implements ListModel
{

	/** displayed character set */
	private Charset					cs;

	/** list event listeners */
	private List<ListDataListener>	listeners;

	private JList					list;

	int								lastSize;


	/**
	 * Constructor.
	 * 
	 * @param cs
	 *            character set
	 */
	public CharListModel(JList list, Charset cs)
	{
		this.cs = cs;
		this.list = list;
		this.lastSize = (cs == null ? 0 : cs.chars.size());
		listeners = new LinkedList<ListDataListener>();
	}


	/**
	 * @see javax.swing.ListModel#getSize()
	 */
	public int getSize()
	{
		return lastSize;
	}


	/**
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	public Object getElementAt(int index)
	{
		if (cs == null)
			throw new IndexOutOfBoundsException();
		return cs.chars.get(index);
	}


	/**
	 * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
	 */
	public void addListDataListener(ListDataListener l)
	{
		listeners.add(l);
	}


	/**
	 * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
	 */
	public void removeListDataListener(ListDataListener l)
	{
		listeners.remove(l);
	}


	public void changeCharset(Charset cs)
	{
		clear();
		setCharset(cs);
	}


	private void setCharset(Charset cs)
	{
		this.cs = cs;
		lastSize = cs.chars.size();
		if (lastSize == 0)
			return;
		for (ListDataListener l : listeners)
			l.intervalAdded(new ListDataEvent(list,
					ListDataEvent.INTERVAL_ADDED, 0, lastSize - 1));
	}


	public void clear()
	{
		if (lastSize == 0)
			return;

		for (ListDataListener l : listeners)
			l.intervalRemoved(new ListDataEvent(list,
					ListDataEvent.INTERVAL_REMOVED, 0, lastSize - 1));

		lastSize = 0;
	}


	public void reload()
	{
		if (cs == null)
			return;

		if (lastSize > 0)
			for (ListDataListener l : listeners)
				l.intervalRemoved(new ListDataEvent(list,
						ListDataEvent.INTERVAL_REMOVED, 0, lastSize - 1));

		lastSize = cs.chars.size();

		if (lastSize > 0)
			for (ListDataListener l : listeners)
				l.intervalAdded(new ListDataEvent(list,
						ListDataEvent.INTERVAL_ADDED, 0, lastSize - 1));
	}


	public void delete(int idx)
	{
		for (ListDataListener l : listeners)
			l.intervalRemoved(new ListDataEvent(list,
					ListDataEvent.INTERVAL_REMOVED, idx, idx));
		lastSize--;
		cs.deleteChar(cs.chars.get(idx));
	}
}
