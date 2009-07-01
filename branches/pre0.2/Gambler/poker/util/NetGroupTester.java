
package poker.util;

import java.util.HashMap;
import java.util.Map;

import bayes.BayesError;
import bayes.Distribution;
import poker.ai.bnet.loose.SPostflopNet;
import poker.ai.bnet.loose.SPreflopNet;


public class NetGroupTester
{

	public static void main(String[] args)
	{
		try
		{
			SPreflopNet net = new SPreflopNet();
			net.buildNetwork();
			net.buildQueries();

			Map<String, String> omap = new HashMap<String, String>();
			Map<String, Distribution> pmap = new HashMap<String, Distribution>();
			Map<String, Distribution> qmap = new HashMap<String, Distribution>();

			// omap: pos, bias, hole, action, in_pot
			// qmap: group
			omap.put("pos", "M");
			omap.put("bias", "L");
			omap.put("hole", "KTs");
			omap.put("action", "NR");
			omap.put("in_pot", "not_3");
			qmap.put("group", null);
			net.compute("group", omap, pmap, qmap);
			System.out.printf("group: %s\n", qmap.get("group").toString());

			qmap.remove("group");
			qmap.put("strat", null);
			omap.remove("bias");
			pmap.put("bias", new Distribution("bias", new String[] {"T", "L",
					"C"}, new double[] {0.0, 1.0, 0.0}));
			net.compute("fwd", omap, pmap, qmap);
			System.out.printf("strat: %s\n", qmap.get("strat").toString());
		}
		catch (BayesError e)
		{
			e.printStackTrace();
		}

	}
}
