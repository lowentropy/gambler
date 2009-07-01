FLUSH(H) := false
{	[_S, _S, _S, _S, _S] in H -> true
}print "start"
FLUSH(HAND1) -> print "A"
FLUSH(HAND2) -> print "B"
print "end"
exit
