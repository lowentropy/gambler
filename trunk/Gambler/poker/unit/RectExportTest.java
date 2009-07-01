
package poker.unit;

import java.io.File;
import java.io.IOException;

import poker.common.Rect;

import junit.framework.TestCase;

public class RectExportTest extends TestCase
{

	/**
	 * Test method for 'poker.common.Rect.export(String, String)'
	 */
	public void testExport()
	{
		String[] formats = new String[] {"JPEG", "PNG"};

		for (String format : formats)
		{
			try
			{
				String fname = "data/test." + format.toLowerCase();
				File file = new File(fname);
				if (file.exists())
					file.delete();
				assertFalse(file.exists());

				File img = new File("data/nance.img");
				Rect rect = Rect.load(img);

				rect.export(format, fname);
				assertTrue(file.exists());
			}
			catch (IOException e)
			{
				e.printStackTrace();
				fail();
			}
		}

	}

}
