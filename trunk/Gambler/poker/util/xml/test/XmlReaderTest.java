package poker.util.xml.test;

import java.io.File;
import java.io.IOException;

import poker.util.xml.XmlReader;
import poker.util.xml.XmlSchema;
import poker.util.xml.XmlSchemaException;
import junit.framework.TestCase;


public class XmlReaderTest extends TestCase
{

	public void testReader()
	{
		try
		{
			XmlSchema schema = new XmlSchema(new File("doc/xml-schema"));
			XmlReader.read(new File("doc/pokerroom.xml"), schema).print();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}
}
