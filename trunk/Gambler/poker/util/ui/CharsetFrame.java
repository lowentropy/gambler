/* * CharsetFrame.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import poker.ai.Block;
import poker.ai.ocr.Charset;
import poker.ai.ocr.GraphicsAI;
import poker.common.Rect;


public class CharsetFrame extends JFrame implements ActionListener
{

	/** uid */
	private static final long	serialVersionUID	= -1597586106116944796L;

	/** tolerance for foreground */
	int							tolerance;

	/** foreground color */
	byte						fg;

	/** rectangles to classify */
	private List<Rect>			rects;

	/** combo box for charsets */
	private JComboBox			cb;

	/** list of characters in charset */
	private JList				charList;

	/** model behind characters */
	CharListModel				cmodel;

	/** model behind character sets */
	private CharsetListModel	csmodel;

	/** name of new charset */
	private JTextField			csName;

	private Rect				source;

	private JTextField			classify;

	private List<Block>			blocks;

	private int					selIdx				= -1;

	private JTextField tolField;


	/**
	 * Constructor.
	 * 
	 * @param tolerance
	 * @param rects
	 */
	public CharsetFrame(int tolerance, byte fg, List<Rect> rects,
			List<Block> blocks, Rect source)
	{
		super("Classify Characters");

		this.tolerance = tolerance;
		this.fg = fg;
		this.rects = rects;
		this.blocks = blocks;
		this.source = source;

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		addCharList();
		addNewCharsetPanel();
		addCharsetListPanel();
		addTolerancePanel();
		addClassifyPanel();
		
		this.setResizable(true);
		this.pack();
	}


	private void addClassifyPanel()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

		p.add(new ImageCanvas(source));
		p.add(new ImageCanvas(Rect.combine(rects, Rect.HORIZONTAL, 1,
				(byte) 0x7, (byte) 0xff)));
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		getContentPane().add(p);

		JButton b = new JButton("Test");
		b.setActionCommand("test");
		b.addActionListener(this);
		p.add(b);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));

		classify = new JTextField(rects.size() * 2);
		p.add(classify);

		b = new JButton("Add chars");
		b.setActionCommand("add");
		b.addActionListener(this);
		p.add(b);

		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		getContentPane().add(p);
	}


	private void addNewCharsetPanel()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

		csName = new JTextField();
		csName.setColumns(10);
		p.add(csName);

		JButton b = new JButton("New Charset");
		b.setActionCommand("new");
		b.addActionListener(this);
		p.add(b);

		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		getContentPane().add(p);
	}


	private void addCharsetListPanel()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

		cb = new JComboBox();
		csmodel = new CharsetListModel(cb, this);
		cb.setModel(csmodel);

		cb.setRenderer(new CharsetListRenderer());
		cb.setEditable(false);
		cb.setMinimumSize(new Dimension(150, 0));

		p.add(cb);

		JButton b = new JButton("Delete");
		b.setActionCommand("delete");
		b.addActionListener(this);
		p.add(b);

		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		getContentPane().add(p);
	}
	
	
	private void addTolerancePanel()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JLabel l = new JLabel("Tolerance: ");
		p.add(l);
		
		tolField = new JTextField("00");
		p.add(tolField);
		
		JButton b = new JButton("Set");
		b.setActionCommand("settol");
		b.addActionListener(this);
		p.add(b);
		
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		getContentPane().add(p);
	}


	private void addCharList()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

		charList = new JList();
		cmodel = new CharListModel(charList, null);
		charList.setModel(cmodel);
		charList.setCellRenderer(new CharListRenderer());
		JScrollPane sp = new JScrollPane(charList);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		p.add(sp);

		JPanel s = new JPanel();
		s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));

		JButton b = new JButton("Delete");
		b.setActionCommand("delchar");
		b.addActionListener(this);
		s.add(b);

		b = new JButton("Save");
		b.setActionCommand("save");
		b.addActionListener(this);
		s.add(b);

		b = new JButton("Force Write");
		b.setActionCommand("force");
		b.addActionListener(this);
		s.add(b);

		p.add(s);

		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		getContentPane().add(p);
	}


	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();

		if (cmd.equals("new"))
		{
			makeNewCharset();
		}
		else if (cmd.equals("delete"))
		{
			deleteCurrentCharset();
		}
		else if (cmd.equals("add"))
		{
			addCharMappings();
		}
		else if (cmd.equals("test"))
		{
			testCharset();
		}
		else if (cmd.equals("settol"))
		{
			setTolerance();
		}
		else if (cmd.equals("up"))
		{
			if (selIdx > 0 && selIdx <= cmodel.getSize())
			{
				selIdx--;
				selectChar();
			}
		}
		else if (cmd.equals("down"))
		{
			if (selIdx < (cmodel.getSize() - 1))
			{
				selIdx++;
				selectChar();
			}
		}
		else if (cmd.equals("delchar"))
		{
			selIdx = charList.getSelectedIndex();
			deleteChar();
		}
		else if (cmd.equals("save"))
		{
			try
			{
				Charset cs = (Charset) csmodel.getSelectedItem();
				if (cs == null)
					return;
				if (cs.wasModified())
				{
					cs.save();
					cmodel.reload();
				}
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
		else if (cmd.equals("force"))
		{
			try
			{
				Charset cs = (Charset) csmodel.getSelectedItem();
				if (cs == null)
					return;
				cs.save();
				cmodel.reload();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}
	
	
	private void setTolerance()
	{
		Charset cs = (Charset) csmodel.getSelectedItem();
		if (cs == null)
		{
			JOptionPane.showMessageDialog(this, "Select a charset first.");
			return;
		}

		cs.setTolerance(Integer.parseInt(tolField.getText()));
	}


	private void testCharset()
	{
		Charset cs = (Charset) csmodel.getSelectedItem();
		if (cs == null)
		{
			JOptionPane.showMessageDialog(this, "Select a charset first.");
			return;
		}

		GraphicsAI ai = new GraphicsAI();
		String text = ai.decodeText(source, cs, new byte[]
			{ fg });
		JOptionPane.showMessageDialog(this, "text = '" + text + "'");
	}


	private void addCharMappings()
	{
		Charset cs = (Charset) csmodel.getSelectedItem();

		if (cs == null)
		{
			JOptionPane.showMessageDialog(this, "Select a charset first.");
			return;
		}

		String msg = cs.addMappings(blocks, classify.getText().trim());
		JOptionPane.showMessageDialog(this, msg);

		cmodel.reload();
		pack();
	}


	private void deleteCurrentCharset()
	{
		Charset cs = (Charset) cb.getSelectedItem();

		if (cs == null)
			return;

		csmodel.remove(cs);
		cs.delete();
	}


	private void makeNewCharset()
	{
		String name = csName.getText().trim();

		if (name.length() == 0)
			return;

		Charset cs = csmodel.add(name);
		if (cs == null)
			return;

		tolField.setText(Integer.toString(cs.getTolerance()));
	}


	private void deleteChar()
	{
		cmodel.delete(selIdx);
		if (selIdx >= cmodel.getSize())
			selIdx--;
		if (selIdx >= 0 && cmodel.getSize() > 0)
			selectChar();
	}


	private void selectChar()
	{
		charList.setSelectedIndex(selIdx);
		pack();
	}


	public void setToleranceField(int tolerance2)
	{
		tolField.setText(Integer.toString(tolerance2));
	}
}
