PAIR(N) := false								
{	[N_, N_] in POCKET -> true
}RULES := true									
INIT := false									
{	PRE_FLOP -> true
}BOAT := false									
{	[N_, N_, M_, M_, M_] in HAND -> true
}FLUSH := false									
{	[_S, _S, _S, _S, _S] in HAND -> true
}STRAIGHT_DRAW := false							
{	[C_, D_] in TABLE & CON_HIGH(L,H) -> false
{		C follows H -> false
{			D follows C -> true
}		L follows C -> false
{			C follows D -> true
}}}STRAIGHT := false								
{	STRAIGHT_DRAW -> false
{		[C_, D_, E_] in TABLE & CON_HIGH(A,B) -> false
{			C follows B -> false
{				D follows C -> false
{					E follows D -> true
}}			A follows C -> false
{				C follows D -> false
{					D follows E -> true
}}}}}AXS(X,S) := false								
{	[AS, XS] in POCKET & X >= Q -> true
}AXO(X) := false									
{	[A_, X_] in POCKET & X >= Q -> true
{		AXS(Y,S) -> false
}}CON(L,H) := false								
{	[L_, H_] in POCKET -> false
{		H follows L	-> true
}}CON_HIGH(L,H) := false							
{	[L_, H_] in POCKET -> false
{		H follows L	-> false
{			L >= 10 -> true
}}}SCON_HIGH(L,H,S) := false						
{	[LS, HS] in POCKET -> false
{		H follows L -> false
{			L >= 10 -> true
}}}BASE_SCARE := false								
{	[_X, _X, _X] in TABLE -> true
{		BOAT | FLUSH -> false
}}SCON_HIGH(L,H,S):
{	PRE_FLOP -> bet/call-
	POST_FLOP -> fold
{		[_S, _S] in DEAL -> bet/call		
		STRAIGHT_DRAW -> bet/call			
}	POST_TURN -> check, call
{		[_S, _S, _S] in TABLE -> bet/raise	
		STRAIGHT -> bet/raise				
}	POST_RIVER -> check, fold
{		[_S, _S, _S] in TABLE -> bet/raise+	
		STRAIGHT -> bet/raise+				
}}AXO(N):
{	SCARE := false
{		BASE_SCARE -> true
}	PRE_FLOP -> call
	POST_FLOP -> fold
{		[A_, N_] in TABLE -> bet/call
}	POST_TURN -> bet/call
{		SCARE -> fold
}	POST_RIVER -> bet/call
{		SCARE -> fold
}}AXS(N,S):
{	SCARE := false
{		BASE_SCARE -> true
}	PRE_FLOP -> call
	POST_FLOP -> fold
{		[A_, N_] in TABLE -> bet/call
		[_S, _S] in TABLE -> bet/call
}	POST_TURN -> check, call
{		[_S] in DEAL -> bet/raise+
		SCARE -> fold
}	POST_RIVER -> fold
{		[A_] in TABLE -> bet/call
		[N_] in TABLE -> bet/call
		[_S] in TURN-DEAL -> bet/raise+
		[_S] in DEAL -> bet/raise+
}}PAIR(N):
{	SCARE := false
{		BASE_SCARE -> true
}	HIGH := false
{		N >= J -> true
}	OVER_CARD := false
{		[M_] in DEAL & M > N -> true
}	SET := false
{		[N_, N_, N_] in HAND -> true
}	BOAT -> bet/raise+
	PRE_FLOP -> bet/call
{		(POSITION > 6) & (BETS = 0) & HIGH -> bet/raise
}	POST_FLOP -> bet/raise
{		!SET -> bet/call
{			OVER_CARD -> fold
}}	POST_TURN:
{		SET:
{			PLAYERS <= 3 -> bet/call
{				SCARE -> bet/call
{					BETS >= 2 -> fold
}				BETS >= 2 -> call
}			PLAYERS > 3 -> bet/raise+
{				SCARE -> check, fold
}}		PAIR -> bet/raise
{			SCARE | OVER_CARD -> check, fold
{				PLAYERS <= 3 -> bet/call
{					BETS >= 2 -> fold
}}			BETS >= 2 -> call	
}}	POST_RIVER & !SET & TURN_CHECK 	-> check, call
{		OVER_CARD -> check, fold
{			POT >= $4 -> check, call
}		SCARE -> check, fold
{			N >= 9:
{				SCARE_FLUSH | SCARE_STRAIGHT -> check, call
}}}	POST_RIVER & !SET & (TURN_BET | TURN_RAISE) 	-> check, call
{		OVER_CARD -> check, call
		SCARE -> check, fold
{			POT >= $6 -> check, fold
}		BETS >= 2 -> fold
}	POST_RIVER & SET & RE-RAISED:
{		OVER_CARD -> bet/raise
{			BETS >= 2 -> call
}}	POST_RIVER & SET & TURN_CHECK 	-> check, call
{		SCARE -> check, fold
{			N >= 9:
{				SCARE_FLUSH | SCARE_STRAIGHT -> check, call
}}}	POST_RIVER & SET & (TURN_BET | TURN_RAISE) 	-> raise
{		SCARE -> check, fold
{			POT >= $6 -> call
}		BETS >= 2-> call
}}