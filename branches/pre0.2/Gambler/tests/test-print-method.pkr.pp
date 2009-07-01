ALWAYS := true
pmethod(str) := false
{	ALWAYS -> print str
}N_ in [3_]:
{	pmethod(N) -> print "notprinted"
}print "XXX"
exit
