ALWAYS := true
NEVER := true
{	ALWAYS -> false
}NEVERNEVER := true
{	NEVER -> false
}ALWAYS:
{	print "foo"
	NEVER -> print "bar"
	NEVERNEVER -> print "baz"
}exit
