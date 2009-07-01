
package poker.server.session.model.data.chat;

import java.util.List;


public class ChatItem
{

	private static boolean debug = false;
	
	public List<ChatNode>	nodes;

	public String			value;

	public List<String>		args;


	public ChatItem(List<ChatNode> nodes, String value, List<String> args)
	{
		this.nodes = nodes;
		this.value = value;
		this.args = args;

		dbg("constructed item: %s\n", toString());
	}


	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (ChatNode node : nodes)
			sb.append(node.toString());
		sb.append(" ~ ");
		sb.append(value);
		if (args.size() > 0)
		{
			sb.append("(");
			for (String arg : args)
				sb.append(arg + ",");
			sb.setLength(sb.length() - 1);
			sb.append(")");
		}
		return sb.toString();
	}

	private static void dbg(String fmt, Object... objs)
	{
		if (debug)
			System.out.printf(fmt, objs);
	}
}
