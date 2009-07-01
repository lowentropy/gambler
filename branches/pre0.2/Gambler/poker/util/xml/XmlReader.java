
package poker.util.xml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * Reads XML text into a tree of XmlObject objects, checking with an XmlSchema.
 * 
 * @author lowentropy
 */
public class XmlReader
{

	/**
	 * Read and parse an XML file.
	 * 
	 * @param file
	 *            file to read
	 * @param schema
	 *            schema describing structure of file
	 * @return root XML node
	 * @throws XmlReaderException
	 */
	public static XmlObject read(File file, XmlSchema schema)
			throws XmlReaderException
	{
		Element root;

		try
		{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(file);
			root = doc.getRootElement();
		}
		catch (Exception e)
		{
			throw new XmlReaderException(e);
		}

		if (!root.getName().equals(schema.rootTag()))
			throw new XmlReaderException("invalid root schema: expected "
					+ schema.rootTag() + ", found " + root.getName());

		return convert(root, schema);
	}


	/**
	 * Convert a JDOM Xml Object into a poker xml object.
	 * 
	 * @param element
	 *            JDOM element to convert
	 * @param schema
	 *            schema controlling conversion (like a DTD)
	 * @return converted element
	 * @throws XmlReaderException
	 */
	private static XmlObject convert(Element element, XmlSchema schema)
			throws XmlReaderException
	{
		/* get attributes or fail */
		int numAttrs = 0;

		String[] attrs = schema.getAttributes(element.getName());
		String[] defs = schema.getDefaults(element.getName());
		int[] modes = schema.getAttributeModes(element.getName());
		Map<String, String> attributes = new HashMap<String, String>();

		for (int i = 0; i < attrs.length; i++)
		{
			Attribute attr = element.getAttribute(attrs[i]);

			if (attr == null)
				if (modes[i] == XmlSchema.ONE)
					throw new XmlReaderException("tag " + element.getName()
							+ " must contain attribute " + attrs[i]);
				else if (defs[i] != null)
					attributes.put(attrs[i], defs[i]);
				else
					;
			else
			{
				numAttrs++;
				attributes.put(attr.getName(), attr.getValue());
			}
		}

		if (numAttrs != element.getAttributes().size())
			throw new XmlReaderException("tag " + element.getName()
					+ " contains invalid attributes (" + numAttrs
					+ " good attrs, "
					+ (element.getAttributes().size() - numAttrs) + " bad)");

		/* get children and return object */
		XmlObject[] children = getChildrenFor(element, schema);
		return new XmlObject(element.getName(), attributes, children,
				element.getText());
	}


	/**
	 * Get the children of the given object.
	 * 
	 * @param element
	 *            JDOM element to convert
	 * @param schema
	 *            schema controlling conversion (like a DTD)
	 * @return xml object children of element
	 * @throws XmlReaderException
	 */
	private static XmlObject[] getChildrenFor(Element element, XmlSchema schema)
			throws XmlReaderException
	{
		int numChildren = 0;

		String[] subtags = schema.getSubtags(element.getName());
		int[] submodes = schema.getSubmodes(element.getName());

		for (int i = 0; i < subtags.length; i++)
		{
			List children = element.getChildren(subtags[i]);
			int num = children.size();

			switch (submodes[i]) {
			case XmlSchema.ONE:
				if (num != 1)
					throw new XmlReaderException("tag " + element.getName()
							+ " should contain one " + subtags[i]);
				break;

			case XmlSchema.PLUS:
				if (num < 1)
					throw new XmlReaderException("tag " + element.getName()
							+ " should contain at least one " + subtags[i]);
				break;

			case XmlSchema.MAYBE:
				if (num > 1)
					throw new XmlReaderException("tag " + element.getName()
							+ " should contain 0 or 1 " + subtags[i]);

			case XmlSchema.ANY:
				break;
			}

			numChildren += num;
		}

		List children = element.getChildren();
		if (children.size() > numChildren)
			throw new XmlReaderException("tag " + element.getName()
					+ " contains invalid subtags");

		XmlObject[] sub = new XmlObject[numChildren];
		for (int i = 0; i < numChildren; i++)
			sub[i] = convert((Element) children.get(i), schema);

		return sub;
	}
}
