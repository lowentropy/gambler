
package poker.unit;

import java.io.IOException;

import poker.server.session.model.data.chat.ChatEventHandler;
import poker.server.session.model.data.chat.ChatFormat;
import poker.server.session.model.data.chat.ChatValue;
import junit.framework.TestCase;


public class ChatTest extends TestCase
{

	private class ChatTester implements ChatEventHandler
	{

		private ChatValue	exp;


		public void handleEvent(ChatValue value)
		{
			assertEquals(exp, value);
		}


		public void handleInvalid(String text)
		{
			if (exp != null)
				fail("invalid: " + text);
		}


		public void expect(String tree)
		{
			exp = (tree == null) ? null : ChatValue.parse(tree);
		}

	}


	public void testChatFormat()
	{
		try
		{
			ChatFormat format = new ChatFormat("houses/pokerroom.com/table.chat");
			ChatTester tester = new ChatTester();

			doTest(
					format,
					tester,
					"low_entropy has disconnected but is not allowed to go all-in.",
					"ignore");
			doTest(
					format,
					tester,
					"low_entropy hasn't acted but is still connected and will be check-folded.",
					"ignore");
			doTest(
					format,
					tester,
					"bob has disconnected and is given 30 seconds to reconnect.",
					"ignore");
			doTest(format, tester, "megan leaves the table.",
					"notattable(megan)");
			doTest(format, tester, "low_entropy calls $2.16.",
					"action(low_entropy,CALL,$2.16)");
			doTest(format, tester, "low_entropy folds.",
					"action(low_entropy,FOLD)");
			doTest(format, tester, "Dealing Turn: [3c]", "deal(TURN,3c)");
			doTest(format, tester, "Dealing River: [Ts]", "deal(RIVER,Ts)");
			doTest(format, tester, "hank raises to $216.",
					"action(hank,RAISE,$216)");
			doTest(format, tester, "The betting is now capped.", "maxbets");
			doTest(format, tester, "low_entropy checks.",
					"action(low_entropy,CHECK)");
			doTest(format, tester, "bob dole joins the table.",
					"attable(bobdole)");
			doTest(format, tester, "Dealing Flop: [5d, 3h, Qc]",
					"deal(FLOP,5d,3h,Qc)");
			doTest(format, tester, "bob shows a two high.",
					"showhand(bob,high(2))");
			doTest(format, tester, "bob shows an eight high.",
					"showhand(bob,high(8))");
			doTest(format, tester, "bob shows a pair of Tens.",
					"showhand(bob,pair(T))");
			doTest(format, tester, "bob shows two pairs, Tens and Queens.",
					"showhand(bob,twopair(T,Q))");
			doTest(format, tester, "bob shows three Kings.",
					"showhand(bob,set(K))");
			doTest(format, tester, "bob shows a straight, nine high.",
					"showhand(bob,straight(9))");
			doTest(format, tester, "bob shows a flush, two high.",
					"showhand(bob,flush(2))");
			doTest(format, tester, "bob shows four Aces.",
					"showhand(bob,four(A))");
			doTest(format, tester, "bob shows a straight flush, Jack high.",
					"showhand(bob,sflush(J))");
			doTest(format, tester, "bob shows a royal flush.",
					"showhand(bob,sflush(A))");
			doTest(format, tester,
					"bob wins side-pot $2.16 with a straight, seven high.",
					"wins(bob,straight(7),$2.16)");
			doTest(format, tester,
					"------ Starting hand #1,234,567,890 ------",
					"newhand(1\\,234\\,567\\,890)");
			doTest(format, tester, "BOLD: <low_entropy> <i'm> gonna <git> you, suckaz",
					"chat(low_entropy,<i'm>gonna<git>you\\,suckaz)");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}

	}


	private void doTest(ChatFormat format, ChatTester tester, String msg,
			String exp)
	{
		tester.expect(exp);
		format.match(msg, tester);
	}
}
