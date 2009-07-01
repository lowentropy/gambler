package poker.server.session.model.data.chat;


public class ChatNode
{

	public String specName;
	
	public String target;
	
	public String pattern;
	
	public String text;

	public ChatNode(String specName, String target, String pattern, String text)
	{
		this.specName = specName;
		this.target = target;
		this.pattern = pattern;
		this.text = text;
	}
	
	public String toString()
	{
		if (text != null)
			return text;
		String str = "<" + ((specName == null) ? pattern : specName);
		if (target != null)
			str += ":" + target;
		return str + ">";
	}
}
