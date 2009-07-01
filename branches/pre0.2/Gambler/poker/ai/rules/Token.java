/*
 * Token.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.rules;


/**
 * Token holds position and text.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Token
{

	/** line position */
	private int line_;
	
	/** character position (tab = 1 char) */
	private int char_;
	
	/** column position (tab aligned) */
	private int col_;

	/** filename of token */
	public String fname;
	
	/** text in token */
	public String text;
	
	/** number */
	public long number;
	
	
	/**
	 * Constructor.
	 * @param fname filename
	 * @param text token text
	 * @param line_ line position
	 * @param char_ character position
	 * @param col_ column position
	 */
	public Token(String fname, String text, int line_, int char_, int col_) {
		this.fname = fname;
		this.text = text;
		this.line_ = line_;
		this.char_ = char_;
		this.col_ = col_;
	}
}
