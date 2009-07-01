
package poker.server.session.house;

import java.io.IOException;

import poker.util.vnc.VncClient;


public class VncAction
{

	public static final int		CLICK_MODE_DOWN		= 1;
	public static final int		CLICK_MODE_UP		= 2;
	public static final int		CLICK_MODE_CLICK	= 3;

	private static final int	TYPING				= 1;

	private static final int	CLICK				= 2;
	
	private static final int MOVE = 3;

	private int					mode;

	private int					type;

	private int					b, x, y;

	private String				text;
	private boolean	deep;


	public VncAction(String text)
	{
		type = TYPING;
		this.text = text;
	}


	public VncAction(int mode, int btn, int x, int y)
	{
		type = CLICK;
		this.mode = mode;
		this.b = btn;
		this.x = x;
		this.y = y;
	}
	
	
	public VncAction(int x, int y)
	{
		this.x = x;
		this.y = y;
		this.type = MOVE;
	}


	public void post(VncClient vnc) throws IOException
	{
		if (type == TYPING)
		{
			for (int i = 0; i < text.length(); i++)
			{
				char c = text.charAt(i);
				vnc.keyType(c);
				if (i < (text.length() - 1))
				{
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
				}
				}
			}
		}
		else if (type == CLICK)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
			}
			if (mode == CLICK_MODE_CLICK)
				if (deep)
					vnc.mouseClickDeep(b, x, y);
				else
					vnc.mouseClick(b, x, y);
			else if (mode == CLICK_MODE_DOWN)
				vnc.mouseDown(b, x, y);
			else
				vnc.mouseUp(b, x, y);
		}
		else if (type == MOVE)
		{
			vnc.clearButtons();
			vnc.mouseMoved(x, y);
		}
	}


	public void setDeep(boolean c)
	{
		deep = c;
	}
}
