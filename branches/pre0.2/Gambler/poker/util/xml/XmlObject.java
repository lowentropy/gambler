
package poker.util.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Tag of a simple XML file. Contains attributes, child objects, and possible
 * text.
 * 
 * @author lowentropy
 */
public class XmlObject
{

	/** tag of xml object */
	private String				tag;

	/** map from xml-style attributes (name=value) */
	private Map<String, String>	attributes;

	/** child object tags */
	private XmlObject[]			children;

	/** text with no guarantees on whitespace */
	private String				text;


	/**
	 * Constructor.
	 * 
	 * @param tag
	 *            Xml tag
	 * @param attributes
	 *            Xml attributes
	 * @param children
	 *            Xml subnodes
	 * @param text
	 *            Xml text within tag
	 */
	public XmlObject(String tag, Map<String, String> attributes,
			XmlObject[] children, String text)
	{
		this.tag = tag;
		this.attributes = attributes;
		this.children = children;
		this.text = text.trim();
	}


	/**
	 * Get attribute value.
	 * 
	 * @param attr
	 *            attribute name
	 * @return value
	 */
	public String getValue(String attr)
	{
		return attributes.get(attr);
	}


	/**
	 * @return text inside node
	 */
	public String getText()
	{
		return text;
	}


	/**
	 * Return all children whose tag equal the given type.
	 * 
	 * @param type
	 *            type of tags to return
	 * @return subset of children
	 */
	public List<XmlObject> getChildren(String type)
	{
		List<XmlObject> sub = new ArrayList<XmlObject>();
		for (XmlObject o : children)
			if (o.tag.equals(type))
				sub.add(o);
		return sub;
	}


	/**
	 * Children of xml object.
	 * 
	 * @return children
	 */
	public List<XmlObject> getChildren()
	{
		List<XmlObject> sub = new ArrayList<XmlObject>();
		for (XmlObject o : children)
			sub.add(o);
		return sub;
	}


	/**
	 * Return first instance of child with given type, or null if none exist.
	 * 
	 * @param type
	 *            type of tags to return
	 * @return first child of type
	 */
	public XmlObject getChild(String type)
	{
		for (XmlObject o : children)
			if (o.tag.equals(type))
				return o;
		return null;
	}


	/**
	 * @return tag type of object
	 */
	public String getTag()
	{
		return tag;
	}


	/**
	 * Print out the object.
	 */
	public void print()
	{
		print("");
	}


	/**
	 * Print the object.
	 * 
	 * @param pre
	 */
	private void print(String pre)
	{
		System.out.print(pre + tag + " : ");
		for (String key : attributes.keySet())
			System.out.printf("%s = %s, ", key, attributes.get(key));
		System.out.printf("text = '%s'\n", text);

		for (XmlObject child : children)
			child.print(pre + "    ");
	}

}
