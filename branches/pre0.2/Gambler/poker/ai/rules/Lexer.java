/*
 * Lexer.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.rules;

import java.util.Hashtable;

import java_cup.runtime.Symbol;


/**
 * Helper class for the spec lexer.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Lexer
{

	/** filename */
	private static String						fname;

	/** width of a tab character */
	private static int							tab_width	= 4;

	/** character position (tab is one character) */
	private static int							char_;

	/** column (tabbed by tab_width) */
	private static int							col_;

	/** line number */
	private static int							line_;

	/** string line number */
	private static int							str_linepos;

	/** string character position */
	private static int							str_charpos;

	/** string column position */
	private static int							str_colpos;

	/** keyword map: name -> sym. type */
	private static Hashtable<String, Integer>	keywords;

	/** symbol map: name -> sym. type */
	private static Hashtable<String, Integer>	symbols;

	/** string buffer for strings */
	private static StringBuffer					buffer;

	static
	{
		initKeywords();
		initSymbols();
	}


	/**
	 * Initialize.
	 */
	public static void init(String fname)
	{
		Lexer.fname = fname;
		char_ = 1;
		col_ = 1;
		line_ = 1;
		buffer = new StringBuffer();
	}


	/**
	 * Process a token that looks like an atom, but could also be a keyword.
	 * 
	 * @param text
	 *            text of token
	 * @return lexer symbol
	 */
	public static Symbol atom(String text)
	{
		int linepos = line_;
		int charpos = char_;
		int colpos = col_;
		scan(text);
		Token token = new Token(fname, text, linepos, charpos, colpos);
		if (keywords.get(text) != null)
		{
			int type = keywords.get(text);
			return new Symbol(type, linepos, colpos, token);
		}
		else
		{
			return new Symbol(sym.ATOM, linepos, colpos, token);
		}
	}


	/**
	 * Process a symbol. Get the right type.
	 * 
	 * @param text
	 *            text to convert
	 * @return lexer symbol
	 */
	public static Symbol symbol(String text)
	{
		int linepos = line_;
		int charpos = char_;
		int colpos = col_;
		scan(text);
		Token token = new Token(fname, text, linepos, charpos, colpos);
		if (symbols.get(text) != null)
		{
			int type = ((Integer) symbols.get(text)).intValue();
			return new Symbol(type, linepos, colpos, token);
		}
		else
		{
			return new Symbol(sym.ERROR, linepos, colpos, token);
		}
	}


	/**
	 * Process a binary number.
	 * 
	 * @param text
	 *            text to process
	 * @return lexer symbol
	 */
	public static Symbol binary(String text)
	{
		return number(text, 2);
	}


	/**
	 * Process an octal number.
	 * 
	 * @param text
	 *            text to process
	 * @return lexer symbol
	 */
	public static Symbol octal(String text)
	{
		return number(text, 8);
	}


	/**
	 * Process a decimal number.
	 * 
	 * @param text
	 *            text to process
	 * @return lexer symbol
	 */
	public static Symbol decimal(String text)
	{
		return number(text, 10);
	}


	/**
	 * Process a hexadecimal number.
	 * 
	 * @param text
	 *            text to process
	 * @return lexer symbol
	 */
	public static Symbol hex(String text)
	{
		return number(text, 16);
	}


	/**
	 * Process a poker card.
	 * 
	 * @param text
	 *            text of card
	 * @return lexer symbol
	 */
	public static Symbol card(String text)
	{
		int linepos = line_;
		int charpos = char_;
		int colpos = col_;
		scan(text);
		Token token = new Token(fname, text, linepos, charpos, colpos);
		return new Symbol(sym.CARD, linepos, colpos, token);
	}


	/**
	 * Process a currency value of the form $XXX[.YY]
	 * 
	 * @param text
	 *            text of currency, including $
	 * @return lexer symbol
	 */
	public static Symbol currency(String text)
	{
		int linepos = line_;
		int charpos = char_;
		int colpos = col_;
		scan(text);
		int idx = text.indexOf('.');
		text = text.substring(1);
		text = (idx == -1) ? text + ".00" : text;
		Token token = new Token(fname, text, linepos, charpos, colpos);
		return new Symbol(sym.CURRENCY, linepos, colpos, token);
	}


	/**
	 * Begin a string buffer.
	 */
	public static void beginString()
	{
		str_linepos = line_;
		str_charpos = char_;
		str_colpos = col_;
		buffer.setLength(0);
	}


	/**
	 * Append text to the string buffer.
	 * 
	 * @param text
	 *            text to append
	 */
	public static void buf(String text)
	{
		buffer.append(text);
	}


	/**
	 * Process a number with given base.
	 * 
	 * @param text
	 *            text to process
	 * @param base
	 *            number base (2,8,10,16)
	 * @return lexer symbol
	 */
	private static Symbol number(String text, int base)
	{
		int linepos = line_;
		int charpos = char_;
		int colpos = col_;
		scan(text);
		Token token = new Token(fname, text, linepos, charpos, colpos);
		token.number = convertNumber(text, base);
		return new Symbol(sym.NUMBER, linepos, colpos, token);
	}


	/**
	 * Scan text and update position.
	 * 
	 * @param text
	 *            text to scan
	 */
	public static void scan(String text)
	{
		char[] buf = text.toCharArray();
		for (int i = 0; i < buf.length; i++)
			scan(buf[i]);
	}


	/**
	 * Scan a single character.
	 * 
	 * @param c
	 *            character to scan
	 */
	private static void scan(char c)
	{
		if (c == '\r') /* do nothing */
			;
		else if (c == '\n')
		{
			line_++;
			char_ = col_ = 1;
		}
		else if (c == '\t')
		{
			char_++;
			col_ = (((col_ - 1) / tab_width) + 1) * tab_width + 1;
		}
		else
		{
			char_++;
			col_++;
		}
	}


	/**
	 * Initialize keyword map.
	 */
	private static void initSymbols()
	{
		symbols = new Hashtable<String, Integer>();
		symbols.put("{", new Integer(sym.TAB));
		symbols.put("}", new Integer(sym.UNTAB));
		symbols.put("/", new Integer(sym.SLASH));
		symbols.put(":", new Integer(sym.COLON));
		symbols.put("!=", new Integer(sym.NEQ));
		symbols.put("(", new Integer(sym.LP));
		symbols.put(")", new Integer(sym.RP));
		symbols.put("=", new Integer(sym.EQ));
		symbols.put("[", new Integer(sym.LSB));
		symbols.put("]", new Integer(sym.RSB));
		symbols.put(",", new Integer(sym.COM));
		symbols.put("&", new Integer(sym.AND));
		symbols.put("|", new Integer(sym.OR));
		symbols.put("<", new Integer(sym.LT));
		symbols.put(">", new Integer(sym.GT));
		symbols.put("->", new Integer(sym.ARROW));
		symbols.put(":=", new Integer(sym.ASSIGN));
		symbols.put("<=", new Integer(sym.LTEQ));
		symbols.put(">=", new Integer(sym.GTEQ));
		symbols.put("!", new Integer(sym.NOT));
		symbols.put("+", new Integer(sym.PLUS));
	}


	/**
	 * Initialize symbol map.
	 */
	private static void initKeywords()
	{
		keywords = new Hashtable<String, Integer>();
		keywords.put("bet", new Integer(sym.BET));
		keywords.put("fold", new Integer(sym.FOLD));
		keywords.put("raise", new Integer(sym.RAISE));
		keywords.put("call", new Integer(sym.CALL));
		keywords.put("call-", new Integer(sym.CALLM));
		keywords.put("check", new Integer(sym.CHECK));
		keywords.put("true", new Integer(sym.TRUE));
		keywords.put("false", new Integer(sym.FALSE));
		keywords.put("in", new Integer(sym.IN));
		keywords.put("print", new Integer(sym.PRINT));
		keywords.put("exit", new Integer(sym.STOP));
		keywords.put("follows", new Integer(sym.FOLLOWS));
	}


	/**
	 * Convert a number from text to long.
	 * 
	 * @param text
	 *            text to convert
	 * @param base
	 *            base to use (2,8,10,16)
	 * @return long number
	 */
	private static long convertNumber(String text, int base)
	{
		long result = 0;
		text = text.toLowerCase();
		for (int i = 2; i < text.length(); i++)
		{
			int d;
			char c = text.charAt(i);
			if (c > '9')
				d = (int) (c - 'a') + 10;
			else
				d = (int) (c - '0');
			result = (result * base) + d;
		}
		return result;
	}


	public static String getLocation()
	{
		return "line " + line_ + ", char " + char_;
	}


	/**
	 * Return a string symbol based on the buffer.
	 * 
	 * @return string symbol to lexer
	 */
	public static Symbol string()
	{
		String str_text = buffer.toString();
		Token token = new Token(fname, str_text, str_linepos, str_charpos,
				str_colpos);
		return new Symbol(sym.STRING, str_linepos, str_colpos, token);
	}

}
