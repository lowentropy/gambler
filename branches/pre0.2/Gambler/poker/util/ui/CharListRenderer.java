/*
 * CharListRenderer.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import poker.ai.ocr.Char;
import poker.common.Rect;


public class CharListRenderer extends JLabel implements ListCellRenderer
{

	private static final long	serialVersionUID	= -5900684483375085411L;

	private static final int	LEFT_SIDE_WIDTH		= 20;

	private Char				c;


	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
	{
		c = (Char) value;

		Rect rect = c.draw(LEFT_SIDE_WIDTH, 0);
		ImageIcon ico = new ImageIcon(list.createImage(new RectImageProducer(
				rect)));

		if (isSelected)
		{
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else
		{
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}

		setIcon(ico);
		setText(c.getText() + (isSelected ? " *" : ""));
		
		return this;
	}


	public Char getChar()
	{
		return c;
	}
}
