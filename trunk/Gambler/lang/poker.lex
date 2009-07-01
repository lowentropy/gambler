/*
 * poker.cup: JLex syntax for AutoPoker rules language
 * 
 * 20050912 <lowentropy@gmail.com>
 * Copyright (c) 2005 Nathan Matthews
 */

package poker.ai.rules;

%%

%cup
%line
%char
%notunix
%public
%state STRING


digit = [0-9]
cardc = [a-zA-Z_]
atomc = [a-zA-Z\-_]
sym = [\/\&\|\-\>\:\=\,\(\)\[\]\<\!\{\}\+]
nl = [\r\n]
ws = [\ \t]

%%

<YYINITIAL> {atomc}
{
	return Lexer.atom(yytext());
}

<YYINITIAL> {atomc}{atomc}{atomc}*{cardc}
{
	return Lexer.atom(yytext());
}

<YYINITIAL> ({digit})({cardc})
{
	return Lexer.card(yytext());
}

<YYINITIAL> "in"
{
	return Lexer.atom(yytext());
}

<YYINITIAL> "call-"
{
	return Lexer.atom(yytext());
}

<YYINITIAL> ({cardc})({cardc})
{
	return Lexer.card(yytext());
}

<YYINITIAL> "$"({digit})+("."({digit})({digit}))?
{
	return Lexer.currency(yytext());
}

<YYINITIAL> ({digit})+
{
	return Lexer.decimal(yytext());
}

<YYINITIAL> {sym}
{
	return Lexer.symbol(yytext());
}

<YYINITIAL> "->"
{
	return Lexer.symbol(yytext());
}

<YYINITIAL> "<="
{
	return Lexer.symbol(yytext());
}

<YYINITIAL> ">="
{
	return Lexer.symbol(yytext());
}

<YYINITIAL> "!="
{
	return Lexer.symbol(yytext());
}

<YYINITIAL> ":="
{
	return Lexer.symbol(yytext());
}

<YYINITIAL> ({ws})|({nl})
{
	Lexer.scan(yytext());
}

<YYINITIAL> \"
{
	Lexer.beginString();
	Lexer.scan(yytext());
	yybegin(STRING);
}

<STRING> \\[^\r\n]
{
	Lexer.buf(yytext());
}

<STRING> \"
{
	Lexer.scan(yytext());
	yybegin(YYINITIAL);
	return Lexer.string();
}

<STRING> ([^\r\n\"])*
{
	Lexer.buf(yytext());
}