package poker.util.ui;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import poker.common.Rect;
import poker.server.session.model.visual.Region;
import poker.server.session.model.visual.Screen;
import poker.util.vnc.Framebuffer;
import poker.util.vnc.VncClient;
import poker.util.web.VncCanvas;


public class VncViewerFrame extends JFrame implements KeyListener
{

	private static final long	serialVersionUID	= 3256445811087847989L;
	
	VncCanvas canvas;

	private int	saveCount;

	private String	saveBase;
	
	public VncViewerFrame(VncClient vnc)
	{
		super("VNC Viewer");
		
		this.saveCount = 1;
		this.saveBase = "/home/lowentropy/src/screendump/vnc/save-";

		canvas = new VncCanvas(vnc.getFramebuffer());
		getContentPane().add(canvas);
		
		this.pack();
		this.setResizable(true);
		this.addKeyListener(this);
	}
	
	/**
	 * @see javax.swing.JFrame#update(java.awt.Graphics)
	 */
	public void update(Graphics g)
	{
		canvas.update(g);
	}

	public void keyTyped(KeyEvent arg0)
	{
	}

	public void keyPressed(KeyEvent arg0)
	{
		char c = arg0.getKeyChar();
		switch (c)
		{
		case 's':
			saveImage();
			break;
		}
	}

	public void keyReleased(KeyEvent arg0)
	{
	}
	
	private void saveImage()
	{
		String fname = saveBase + (saveCount++) + ".img";
		Rect r = canvas.getRect();
		try
		{
			r.save(new File(fname));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void drawSchema(Screen screen)
	{
		Rect rect = canvas.getRect();
		for (Region r : screen.getAllRegions())
		{
			int x = r.getX(), y = r.getY();
			int w = r.getWidth(), h = r.getHeight();
			rect.drawBox(x, y, x+w-1,y+h-1,(byte)0x7,true);
			Framebuffer fb = canvas.getFramebuffer();
			fb.newPixels(x,y,w,h);
			canvas.updated(x, y, w, h);
		}
	}

}
