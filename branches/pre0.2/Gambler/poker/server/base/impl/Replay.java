
package poker.server.base.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bayes.Distribution;

/**
 * For a given player at a poker table, remembers the values of observed nodes
 * (including prior-dist nodes) in the network on each action the player took.
 * If that player shows down his hand, the network states can be used to query
 * the player bias/strategy.
 * 
 * @author lowentropy
 */
public class Replay
{

	private List<Map<String,String>> omaps;
	
	private List<Map<String,Distribution>> pmaps;
	
	private List<Boolean> preflops;
	
	private List<double[][]> pfDists;

	public Replay()
	{
		omaps = new ArrayList<Map<String,String>>();
		pmaps = new ArrayList<Map<String,Distribution>>();
		preflops = new ArrayList<Boolean>();
		pfDists = new ArrayList<double[][]>();
	}
	
	public void add(Map<String,String> omap, Map<String,Distribution> pmap, double[][] wh, boolean preflop)
	{
		omaps.add(copy(omap));
		pmaps.add(copy(pmap));
		preflops.add(preflop);
		pfDists.add(wh);
	}
	
	public int numStates()
	{
		return omaps.size();
	}
	
	public Map<String,String> getObsMap(int idx)
	{
		return omaps.get(idx);
	}
	
	public Map<String,Distribution> getPriorMap(int idx)
	{
		return pmaps.get(idx);
	}
	
	public double[][] getPostflopDists(int idx)
	{
		return pfDists.get(idx);
	}
	
	private static <A,B> Map<A,B> copy(Map<A,B> m)
	{
		Map c = null;
		try
		{
			c = m.getClass().newInstance();
			c.putAll(m);
		}
		catch (InstantiationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		c.putAll(m);
		return c;
	}

	public boolean getPreflop(int i)
	{
		return preflops.get(i);
	}
}
