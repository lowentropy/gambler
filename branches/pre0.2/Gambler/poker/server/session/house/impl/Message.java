/* 
 * Message.java
 * 
 * created: 24-Apr-06
 * author: Nathan Matthews
 * email: lowentropy@gmail.com
 * 
 * Copyright (C) 2006
 */
package poker.server.session.house.impl;


/**
 * TODO: Message
 * 
 * @author lowentropy
 * 
 */
public class Message
{

	public String type;
	
	public Object arg;
	
	public Message(String type, Object arg)
	{
		this.type = type;
		this.arg = arg;
	}
	
	public String toString()
	{
		return "[" + type + "] : " + arg.toString();
	}
}
