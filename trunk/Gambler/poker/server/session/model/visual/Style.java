package poker.server.session.model.visual;

import java.io.IOException;
import java.util.List;

import poker.ai.AIError;
import poker.ai.ocr.Charset;
import poker.ai.ocr.GraphicsAI;
import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;

/**
 * A style is a total font description, including tolerance, fontname, and
 * color.
 * 
 * @author lowentropy
 */
public class Style extends Component
{

	/** name of style */
	private String name;

	/** font name to use */
	private String font;

	/** foreground tolerance */
	private int tol;

	/** separate red, green, and blue values of foreground color */
	@SuppressWarnings("unused")
	private int r, g, b;

	/** foreground color */
	private byte color;

	/** character set of font */
	private Charset charset;

	/** graphics AI */
	private GraphicsAI ai;
	
	/** whether characters are outlined */
	private boolean hasOutline;
	
	/** outline color */
	private byte olColor;

	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object to construct from
	 * @throws ModelError
	 */
	public Style(XmlObject xml) throws ModelError
	{
		name = xml.getValue("name");
		font = xml.getValue("font");
		tol = Integer.parseInt(xml.getValue("tol"));

		// get fg color
		int[] rgbc = parseColor(xml.getValue("fg"));
		r = rgbc[0];
		g = rgbc[1];
		b = rgbc[2];
		color = (byte) rgbc[3];

		// get outline color
		String ol = xml.getValue("ol");
		if (ol.equals("none"))
			hasOutline = false;
		else
		{
			hasOutline = true;
			rgbc = parseColor(ol);
			olColor = (byte) rgbc[3];
		}
		
		// get charset
		try
		{
			charset = Charset.load(font);
		} catch (IOException e)
		{
			throw new ModelError("error loading font", e);
		}

		ai = new GraphicsAI();
	}

	/**
	 * @return style name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData,
	 *      poker.common.Rect, int, int, int)
	 */
	public boolean updateData(ScreenData data, Rect win, int wx, int wy, int ax, int ay)
	{
		return false;
	}

	/**
	 * Use style to extract text from the given pixel rectangle.
	 * 
	 * @param rect
	 *            pixels
	 * @return extracted text
	 */
	public String getText(Rect rect)
	{
		//try {
		//	if (hasOutline)
		//		rect = ai.outlineMask(rect, olColor);
			
			return ai.decodeText(rect, charset, new byte[] { color }, this.tol);
		//} catch (AIError e)
		//{
		//	e.printStackTrace();
		//	return null;
		//}
	}

	/**
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List,
	 *      int, int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
		// do nothing
	}
	
	public int getTolerance()
	{
		return tol;
	}

	public Region getRegion(int ax, int ay)
	{
		return null;
	}

	public String getTarget()
	{
		return null;
	}

	public int getColor()
	{
		return color;
	}
}
