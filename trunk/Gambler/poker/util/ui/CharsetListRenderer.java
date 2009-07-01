/*
 * CharsetListRenderer.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import poker.ai.ocr.Charset;


public class CharsetListRenderer implements ListCellRenderer
{

	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
	{
		Charset cs = (Charset) value;
		JLabel label = new JLabel((cs == null) ? "Select a charset" : cs
				.getName());
		label.setForeground(isSelected ? Color.WHITE : Color.BLACK);
		label.setBackground(isSelected ? Color.BLUE : Color.WHITE);
		return label;
	}

}
