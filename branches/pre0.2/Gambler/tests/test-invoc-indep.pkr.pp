ALWAYS := true
OUTER := false
{	INNER -> true
}NOUTER := true
{	OUTER -> false
}INNER := true
OUTER -> print "A"
NOUTER -> print "B"
INNER := false
OUTER -> print "A"
NOUTER -> print "B"
exit
