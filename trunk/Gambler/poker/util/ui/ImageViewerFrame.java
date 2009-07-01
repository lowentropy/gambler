
package poker.util.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import poker.ai.AIError;
import poker.server.session.model.ModelError;
import poker.server.session.model.visual.Region;
import poker.server.session.model.visual.Screen;
import poker.util.xml.XmlObject;
import poker.util.xml.XmlReader;
import poker.util.xml.XmlReaderException;
import poker.util.xml.XmlSchema;
import poker.util.xml.XmlSchemaException;

public class ImageViewerFrame extends JFrame implements ActionListener,
		MouseListener, KeyListener
{

	private static final String	houseFile			= "houses/pokerroom.com.xml";

	/** serial version UID */
	private static final long	serialVersionUID	= -4083583964437472688L;

	/** current image file */
	private File				file;

	/** image canvas displaying current image */
	private UtilCanvas			canvas;

	/** current X and Y location of cursor */
	private int					curX, curY;

	/** cursor position label */
	private JLabel				cursorLabel;

	/** color label */
	private JLabel				colorLabel;

	/** cursor position/bounds label */
	private JLabel				boundsLabel;

	/** tolerance label */
	private JLabel				tolLabel;

	/** button to go to previous image */
	private JButton				prevButton;

	/** button to go to next image */
	private JButton				nextButton;

	/** bounds array */
	private int[]				bounds				= null;

	/** tolerance for character extraction */
	private int					tol;

	/** current anchor points, the origin for display coordinates */
	private int					anchorX, anchorY;

	private Screen				curSchema;

	private int					schemaIdx;

	private Screen[]			schemas;


	/**
	 * Constructor.
	 * 
	 * @param file
	 *            initial image file to load
	 * @throws IOException
	 */
	public ImageViewerFrame(File file) throws IOException
	{
		super(file.getName() + " - Poker Image Viewer");
		this.file = file;

		curX = 0;
		curY = 0;
		anchorX = 0;
		anchorY = 0;
		tol = 0;

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		addInfoPanel();
		addCanvas();

		pack();
		setResizable(true);

		updateCursor(0, 0);
	}


	/**
	 * Add the panel containing information labels and prev/next buttons above
	 * the image.
	 */
	private void addInfoPanel()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

		colorLabel = new JLabel("rgb: ?         ");
		tolLabel = new JLabel("tol: 0");
		cursorLabel = new JLabel("cursor: (0, 0)      ");
		boundsLabel = new JLabel("bounds:                   ");

		p.add(colorLabel);
		p.add(tolLabel);
		p.add(cursorLabel);
		p.add(boundsLabel);

		prevButton = new JButton("Prev");
		prevButton.setActionCommand("prev");
		prevButton.addActionListener(this);
		p.add(prevButton);

		nextButton = new JButton("next");
		nextButton.setActionCommand("next");
		nextButton.addActionListener(this);
		p.add(nextButton);

		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		getContentPane().add(p);
	}


	/**
	 * Create and add the image canvas to the frame.
	 */
	private void addCanvas() throws IOException
	{
		canvas = new UtilCanvas();
		canvas.loadImage(file, true);
		canvas.addMouseListener(this);
		canvas.addKeyListener(this);
		getContentPane().add(canvas);
	}


	/**
	 * Update the cursory location.
	 * 
	 * @param newX
	 *            new x coordinate
	 * @param newY
	 *            new y coordinate
	 */
	private void updateCursor(int newX, int newY)
	{
		curX = (newX < 0) ? 0 : (newX >= width() ? width() - 1 : newX);
		curY = (newY < 0) ? 0 : (newY >= height() ? height() - 1 : newY);

		cursorLabel.setText("pos: (" + getRelX() + ", " + getRelY() + ")");
		colorLabel.setText("rgb: " + canvas.getRGB(curX, curY));

		canvas.redrawCursor(curX, curY);
	}


	/**
	 * Update label displaying current bounds (for placing rectangles in a poker
	 * house description, or getting the extends of a bitmap dump).
	 * 
	 * @param bounds
	 * 
	 * <pre>
	 *  
	 *               string description of bounds, in format @(x,y) *(w,h)
	 *   
	 * </pre>
	 */
	private void updateBounds(int[] bounds)
	{
		this.bounds = bounds;

		if (bounds == null)
			boundsLabel.setText("bounds: <none>");
		else
			boundsLabel.setText("bounds: @(" + (bounds[0] - anchorX) + ","
					+ (bounds[1] - anchorY) + ") *(" + bounds[2] + ","
					+ bounds[3] + ")");
	}


	private void modifyTolerance(int dt)
	{
		tol += dt;
		tol = (tol < 0) ? 0 : (tol > 255 ? 255 : tol);
		tolLabel.setText("tol: " + tol);
	}


	/**
	 * Save a bitmap dump in an XML-compatible format (ASCII-hex).
	 * 
	 * @param dump
	 *            string dump of bitmap
	 */
	private void saveDump(String dump)
	{
		try
		{
			new PrintStream(new FileOutputStream(
					"/home/lowentropy/src/Gambler/doc/hexdump")).println(dump);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * @return cursor X coordinate relative to anchor
	 */
	private int getRelX()
	{
		return curX - anchorX;
	}


	/**
	 * @return cursor Y coordinate relative to anchor
	 */
	private int getRelY()
	{
		return curY - anchorY;
	}


	/**
	 * @return width of image
	 */
	private int width()
	{
		return canvas.width();
	}


	/**
	 * @return height of image
	 */
	private int height()
	{
		return canvas.height();
	}


	/**
	 * @throws IOException
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();

		try
		{
			if (cmd.equals("prev"))
				prevFile();
			else if (cmd.equals("next"))
				nextFile();
		}
		catch (IOException err)
		{
			err.printStackTrace();
		}
	}


	/**
	 * @see java.awt.Component#update(java.awt.Graphics)
	 */
	public void update(Graphics g)
	{
		canvas.update(g);
	}


	/**
	 * Get the next image file.
	 * 
	 * @throws IOException
	 */
	private void nextFile() throws IOException
	{
		String fname = file.getAbsolutePath();

		int idx1 = fname.indexOf('-');
		int idx2 = fname.indexOf('.');
		if (idx1 == -1 || idx2 == -1)
			return;

		String a = fname.substring(0, idx1 + 1);
		String b = fname.substring(idx1 + 1, idx2);
		String c = fname.substring(idx2);
		fname = a + (Integer.parseInt(b) + 1) + c;

		File f2 = new File(fname);
		if (!f2.canRead())
			return;
		file = f2;
		canvas.loadImage(file, true);

		this.setTitle(file.getName() + " - Poker Image Viewer");

		updateCursor(curX, curY);
	}


	/**
	 * Get the previous image file.
	 * 
	 * @throws IOException
	 */
	private void prevFile() throws IOException
	{
		String fname = file.getAbsolutePath();

		int idx1 = fname.indexOf('-');
		int idx2 = fname.indexOf('.');
		if (idx1 == -1 || idx2 == -1)
			return;

		String a = fname.substring(0, idx1 + 1);
		String b = fname.substring(idx1 + 1, idx2);
		String c = fname.substring(idx2);
		fname = a + (Integer.parseInt(b) - 1) + c;

		File f2 = new File(fname);
		if (!f2.canRead())
			return;
		file = f2;
		canvas.loadImage(file, true);

		this.setTitle(file.getName() + " - Poker Image Viewer");

		updateCursor(curX, curY);
	}


	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e)
	{
	}


	/**
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e)
	{
		updateCursor(e.getX(), e.getY());
	}


	/**
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e)
	{
	}


	/**
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e)
	{
	}


	/**
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e)
	{
	}


	/**
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e)
	{
	}


	/**
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			updateCursor(curX - (e.isShiftDown() ? 10 : 1), curY);
			break;

		case KeyEvent.VK_RIGHT:
			updateCursor(curX + (e.isShiftDown() ? 10 : 1), curY);
			break;

		case KeyEvent.VK_UP:
			updateCursor(curX, curY - (e.isShiftDown() ? 10 : 1));
			break;

		case KeyEvent.VK_DOWN:
			updateCursor(curX, curY + (e.isShiftDown() ? 10 : 1));
			break;

		case KeyEvent.VK_SPACE:
			updateBounds(canvas.mark(curX, curY));
			break;

		case KeyEvent.VK_A:
			anchorX = curX;
			anchorY = curY;
			updateCursor(curX, curY);
			updateBounds(bounds);
			break;

		case KeyEvent.VK_F:
			canvas.setForegroundFrom(curX, curY);
			break;

		case KeyEvent.VK_T:
			modifyTolerance(e.isShiftDown() ? 1 : -1);
			break;

		case KeyEvent.VK_R:
			canvas.resetMarks();
			updateBounds(null);
			break;

		case KeyEvent.VK_M:
			canvas.mask(tol);
			break;

		case KeyEvent.VK_E:
			try
			{
				canvas.extract(tol);
			}
			catch (AIError e1)
			{
				e1.printStackTrace();
			}
			break;

		case KeyEvent.VK_C:
			canvas.classify(tol);
			break;

		case KeyEvent.VK_D:
			String dump = canvas.dump();
			if (dump != null)
				saveDump(dump);
			break;

		case KeyEvent.VK_S:
			if (e.isShiftDown())
				removeSchema();
			else
				cycleSchema();
			break;

		case KeyEvent.VK_P:
			try
			{
				canvas.crop().save(nextCropFile());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			break;
		}
	}


	private int	cropIdx	= 1;


	private File nextCropFile()
	{
		return new File("screendump/crop-" + (cropIdx++) + ".img");
	}


	/**
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e)
	{
	}


	private void removeSchema()
	{
		canvas.resetMarks();
		curSchema = null;
		schemas = null;
	}


	private void cycleSchema()
	{
		if (schemas == null)
			getAvailableSchema();
		if (curSchema == null)
			schemaIdx = -1;
		schemaIdx = (schemaIdx + 1) % schemas.length;
		useSchema(schemas[schemaIdx]);
	}


	private void getAvailableSchema()
	{
		// FIXME: use real scan of houses/
		try
		{
			XmlSchema schema = new XmlSchema(new File("conf/xml-house-schema"));
			XmlObject root = XmlReader.read(new File(houseFile), schema);
			List<XmlObject> sxmls = root.getChild("screens").getChildren();
			schemas = new Screen[sxmls.size()];
			for (int i = 0; i < schemas.length; i++)
				schemas[i] = new Screen(sxmls.get(i));
		}
		catch (IOException e)
		{
			// TODO: Auto-generated catch block
			e.printStackTrace();
		}
		catch (XmlSchemaException e)
		{
			// TODO: Auto-generated catch block
			e.printStackTrace();
		}
		catch (XmlReaderException e)
		{
			// TODO: Auto-generated catch block
			e.printStackTrace();
		}
		catch (ModelError e)
		{
			// TODO: Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void useSchema(Screen screen)
	{
		curSchema = screen;
		canvas.resetMarks();
		drawSchema(curSchema);
	}


	private void drawSchema(Screen s)
	{
		for (Region r : s.getAllRegions())
			canvas.drawBox(r.getX(), r.getY(), r.getX() + r.getWidth(), r
					.getY()
					+ r.getHeight(), (byte) 0x7);
	}

}
