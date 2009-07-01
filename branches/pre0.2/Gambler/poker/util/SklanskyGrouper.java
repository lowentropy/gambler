
package poker.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import poker.ai.bnet.PokerNet;


public class SklanskyGrouper
{

	private String[]	sGroupNames;

	private String[][]	sGroups;

	private String[][]	sTable;


	public static void main(String[] args)
	{
		new SklanskyGrouper().compute(true);
	}


	public void compute()
	{
		compute(false);
	}


	public void compute(boolean verbose)
	{
		String t_e_nr_p = "AA,KK,QQ,JJ,TT,99,88,77,AKs,AQs,AJs,ATs,KQs,KJs,KTs,QJs,QTs,JTs,AKu,AQu,AJu,KQu";
		String t_e_nr_r = "AA,KK,QQ,JJ,TT,AKs,AQs,AJs,AKu,AQu";
		String t_e_1r_p = t_e_nr_r;
		String t_e_1r_r = "AA,KK,QQ,JJ,TT,AKs,AKu";
		String t_e_2r_p = "AA,KK,QQ,AKs";
		String t_e_2r_r = t_e_2r_p;
		String t_m_nr_p = "AA,KK,QQ,JJ,TT,99,88,77,66,55,44,33,22,AKs,AQs,AJs,ATs,A9s,A8s,A7s,A6s,A5s,A4s,A3s,A2s,KQs,KJs,KTs,K9s,QJs,QTs,Q9s,JTs,J9s,T9s,98s,AKu,AQu,AJu,ATu,KQu,KJu";
		String t_m_nr_r = "AA,KK,QQ,JJ,TT,99,AKs,AQs,AJs,ATs,KQs,KJs,AKu,AQu,AJu,KQu";
		String t_m_1r_p = t_e_1r_p;
		String t_m_1r_r = t_e_1r_p;
		String t_m_2r_p = t_e_2r_p;
		String t_m_2r_r = t_e_2r_p;
		String t_l_nr_p = t_m_nr_p + ",87s,76s,65s,54s,KTu,QJu,QTu,JTu";
		String t_l_nr_r = "AA,KK,QQ,JJ,TT,99,AKs,AQs,AJs,ATs,A9s,A8s,KQs,KJs,KTs,QJs,AKu,AQu,AJu,ATu,KQu";
		String t_l_1r_p_sh = t_e_1r_p;
		String t_l_1r_p_mh = t_l_1r_p_sh + ",99,88,77,66,55,44,33,22,QJs,T9s";
		String t_l_1r_r = t_e_1r_r;
		String t_l_2r_p = t_e_2r_p;
		String t_l_2r_r = t_e_2r_p;
		String t_s_nr_p = t_l_nr_p + ",43s,32s"; // TODO: is XYs connectors,
		// just
		// suited? CHECK SKLANSKY
		String t_s_nr_r = "AA,KK,QQ,JJ,TT,99,AKs,AQs,AJs,ATs,KQs,KJs,AKu,AQu";
		String t_s_1r_p_sh = t_e_1r_p;
		String t_s_1r_p_mh = t_e_1r_p + ",99,88,77,66,55,44,33,22";
		String t_s_1r_r = t_e_1r_r;
		String t_s_2r_p = t_e_2r_p;
		String t_s_2r_r = t_e_2r_p;
		String t_b_nr_p = t_s_nr_p;
		String t_b_nr_r = t_s_nr_r;
		String t_b_1r_p_sh = t_l_1r_p_sh;
		String t_b_1r_p_mh = t_l_1r_p_mh;
		String t_b_1r_r = "AA,KK,QQ,JJ,TT,AKs,AKu";
		String t_b_2r_p = t_e_2r_p;
		String t_b_2r_r = t_e_2r_p;

		String l_e_nr_p = t_m_nr_p;
		String l_e_nr_r = t_m_nr_r;
		String l_e_1r_p = "AA,KK,QQ,JJ,TT,99,88,77,66,55,44,33,22,AKs,AQs,AJs,ATs,KQs,KJs,KTs,QJs,JTs,AKu,AQu";
		String l_e_1r_r = "AA,KK,QQ,JJ,TT,99,AKs,AQs,AKu,AQu";
		String l_e_2r_p = "AA,KK,QQ,JJ,TT,AKs,AQs,AJs,KQs,AKu";
		String l_e_2r_r = "AA,KK,QQ,AKs";
		String l_m_nr_p = l_e_nr_p;
		String l_m_nr_r = l_e_nr_r;
		String l_m_1r_p = l_e_1r_p;
		String l_m_1r_r = l_e_1r_r;
		String l_m_2r_p = l_e_2r_p;
		String l_m_2r_r = l_e_2r_r;
		String l_l_nr_p = l_m_nr_p
				+ ",K8s,K7s,K6s,K5s,K4s,K3s,K2s,Q8s,J8s,J7s,87s,76s,65s,54s,43s,T8s,97s,86s,75s,64s,53s,KTu,QJu,QTu,JTu";
		String l_l_nr_r = "AA,KK,QQ,JJ,TT,99,88,AKs,AQs,AJs,ATs,KQs,KJs,KTs,QJs,QTs,JTs,A9s,A8s,K9s,AKu,AQu,AJu,KQu";
		String l_l_1r_p_sh = t_l_1r_p_mh;
		String l_l_1r_r_sh = t_l_1r_r;
		String l_l_2r_p_sh = t_l_2r_p;
		String l_l_2r_r_sh = t_l_2r_r;
		String l_l_1r_p_mh = "AA,KK,QQ,JJ,TT,99,88,77,66,55,44,33,22,AKs,AQs,AJs,ATs,KQs,KJs,KTs,QJs,QTs,JTs,A9s,A8s,A7s,A6s,A5s,A4s,A3s,A2s,T9s,98s,87s,76s,AKu,AQu";
		String l_l_1r_r_mh = "AA,KK,QQ,JJ,TT,AKs,AQs,AJs,KQs,AKu";
		String l_l_2r_p_mh = "AA,KK,QQ,JJ,TT,AKs,AQs,AJs,KQs,AKu";
		String l_l_2r_r_mh = "AA,KK,QQ,AKs";
		String l_s_nr_p = l_l_nr_p
				+ ",Q7s,Q6s,Q5s,Q4s,Q3s,Q2s,J6s,J5s,J4s,J3s,J2s,T7s,T6s,T5s,T4s,T3s,T2s,96s,95s,94s,93s,92s,85s,84s,83s,82s,74s,73s,72s,63s,62s,52s,42s,32s";
		String l_s_nr_r = "AA,KK,QQ,JJ,TT,99,AKs,AQs,AJs,ATs,KQs,KJs,AKu,AQu";
		;
		String l_s_1r_p = l_e_1r_p;
		String l_s_1r_r = "AA,KK,QQ,JJ,TT,AKs,AQs,AJs,KQs,AKu";
		String l_s_2r_p = t_e_2r_p;
		String l_s_2r_r = t_e_2r_p;
		String l_b_nr_p = l_s_nr_p;
		String l_b_nr_r = l_s_nr_r;
		String l_b_1r_p = l_l_1r_p_sh;
		String l_b_1r_r = t_e_2r_p;
		String l_b_2r_p = t_e_2r_p;
		String l_b_2r_r = t_e_2r_p;

		String[] inStrs = new String[] {t_e_nr_p, t_e_nr_r, t_e_1r_p, t_e_1r_r,
				t_e_2r_p, t_e_2r_r, t_m_nr_p, t_m_nr_r, t_m_1r_p, t_m_1r_r,
				t_m_2r_p, t_m_2r_r, t_l_nr_p, t_l_nr_r, t_l_1r_p_sh,
				t_l_1r_p_mh, t_l_1r_r, t_l_2r_p, t_l_2r_r, t_s_nr_p, t_s_nr_r,
				t_s_1r_p_sh, t_s_1r_p_mh, t_s_1r_r, t_s_2r_p, t_s_2r_r,
				t_b_nr_p, t_b_nr_r, t_b_1r_p_sh, t_b_1r_p_mh, t_b_1r_r,
				t_b_2r_p, t_b_2r_r,

				l_e_nr_p, l_e_nr_r, l_e_1r_p, l_e_1r_r, l_e_2r_p, l_e_2r_r,
				l_m_nr_p, l_m_nr_r, l_m_1r_p, l_m_1r_r, l_m_2r_p, l_m_2r_r,
				l_l_nr_p, l_l_nr_r, l_l_1r_p_sh, l_l_1r_p_mh, l_l_1r_r_sh,
				l_l_1r_r_mh, l_l_2r_p_sh, l_l_2r_p_mh, l_l_2r_r_sh,
				l_l_2r_r_mh, l_s_nr_p, l_s_nr_r, l_s_1r_p, l_s_1r_r, l_s_2r_p,
				l_s_2r_r, l_b_nr_p, l_b_nr_r, l_b_1r_p, l_b_1r_r, l_b_2r_p,
				l_b_2r_r};

		String[] posNames = new String[] {"t_e_nr_p", "t_e_nr_r", "t_e_1r_p",
				"t_e_1r_r", "t_e_2r_p", "t_e_2r_r", "t_m_nr_p", "t_m_nr_r",
				"t_m_1r_p", "t_m_1r_r", "t_m_2r_p", "t_m_2r_r", "t_l_nr_p",
				"t_l_nr_r", // 14
				
				"t_l_1r_p_sh", "t_l_1r_p_mh", 
				
				"t_l_1r_r",
				"t_l_2r_p", "t_l_2r_r", "t_s_nr_p", "t_s_nr_r", // 5 
				
				"t_s_1r_p_sh",
				"t_s_1r_p_mh", 
				
				"t_s_1r_r", "t_s_2r_p", "t_s_2r_r", "t_b_nr_p",
				"t_b_nr_r", // 4 
				
				"t_b_1r_p_sh", "t_b_1r_p_mh", 
				
				"t_b_1r_r",
				"t_b_2r_p", "t_b_2r_r", "l_e_nr_p", "l_e_nr_r", "l_e_1r_p",
				"l_e_1r_r", "l_e_2r_p", "l_e_2r_r", "l_m_nr_p", "l_m_nr_r",
				"l_m_1r_p", "l_m_1r_r", "l_m_2r_p", "l_m_2r_r", "l_l_nr_p",
				"l_l_nr_r", // 17 
				
				"l_l_1r_p_sh", "l_l_1r_p_mh", "l_l_1r_r_sh",
				"l_l_1r_r_mh", "l_l_2r_p_sh", "l_l_2r_p_mh", "l_l_2r_r_sh",
				"l_l_2r_r_mh", 
				
				"l_s_nr_p", "l_s_nr_r", "l_s_1r_p", "l_s_1r_r",
				"l_s_2r_p", "l_s_2r_r", "l_b_nr_p", "l_b_nr_r", "l_b_1r_p",
				"l_b_1r_r", "l_b_2r_p", "l_b_2r_r" // 12
				
		};
		
		List<Set<String>> sets = new ArrayList<Set<String>>(inStrs.length);
		for (String inStr : inStrs)
		{
			String[] strs = inStr.split(",");
			Set<String> strSet = new HashSet<String>();
			for (String str : strs)
				strSet.add(str);
			sets.add(strSet);
		}

		List<List<String>> refs = new ArrayList<List<String>>(inStrs.length);
		for (int i = 0; i < inStrs.length; i++)
			refs.add(new ArrayList<String>());

		Map<String, Set<String>> grps = new HashMap<String, Set<String>>();

		if (verbose)
		{
			System.out.println("Initial Groups:");
			for (int i = 0; i < sets.size(); i++)
			{
				System.out.printf("\t%s: [", posNames[i]);
				for (String c : sets.get(i))
					System.out.printf("%s,", c);
				System.out.printf("]\n");
			}
		}

		int i = 1;
		while (nonempty(sets))
		{
			if (i > 200)
				break;

			Set<String> sub = findSub(sets);
			ref(sets, refs, grps, sub, i++);
		}

		// add ungrouped hands to JUNK group
		Set<String> junk = new HashSet<String>();
		for (String h : PokerNet.holePairs)
		{
			boolean ok = true;
			for (Set<String> list : grps.values())
				if (list.contains(h))
				{
					ok = false;
					break;
				}
			if (ok)
				junk.add(h);
		}
		grps.put("JUNK", junk);

		int num = 0;
		for (Set<String> list : grps.values())
			num += list.size();
		if (verbose)
			System.out.printf("SANITY CHECK: %d\n", num);

		if (verbose)
		{
			System.out.printf("\nNew Groups:\n");
			for (String n : grps.keySet())
			{
				System.out.printf("\tgroup %s: [", n);
				for (String c : grps.get(n))
					System.out.printf("%s,", c);
				System.out.printf("]\n");
			}

			System.out.printf("\nReferences:\n");
			for (i = 0; i < posNames.length; i++)
			{
				System.out.printf("\t%s: [", posNames[i]);
				for (String r : refs.get(i))
					System.out.printf("%s, ", r);
				System.out.printf("]\n");
			}
		}

		sGroupNames = grps.keySet().toArray(new String[0]);
		sGroups = new String[sGroupNames.length][];
		for (i = 0; i < sGroups.length; i++)
			sGroups[i] = grps.get(sGroupNames[i]).toArray(new String[0]);

		sTable = new String[2 * 5 * 3 * 3 * 2][];
		int j = 0;
		for (i = 0; i < posNames.length; i++)
		{
			if (posNames[i].endsWith("h"))
			{
				if (posNames[i].charAt(0) == 't')
				{
					if (posNames[i - 1].endsWith("sh"))
						sTable[j++] = sTable[j++] = refs.get(i).toArray(
								new String[0]);
					else
						sTable[j++] = refs.get(i).toArray(new String[0]);
				}
				else
				{
					if (posNames[i - 1].endsWith("sh"))
						sTable[j++] = refs.get(i).toArray(new String[0]);
					else
						sTable[j++] = sTable[j++] = refs.get(i).toArray(
								new String[0]);
				}
			}
			else
			{
				sTable[j++] = sTable[j++] = sTable[j++] = refs.get(i).toArray(
						new String[0]);
			}
	
		}
		
		
		String[][] tmp = new String[6][];
		for (i = 0; i < sTable.length; i += 6)
		{
			for (j = 0; j < 6; j++)
				tmp[j] = sTable[i+j];
			sTable[i+0] = tmp[0];
			sTable[i+1] = tmp[3];
			sTable[i+2] = tmp[1];
			sTable[i+3] = tmp[4];
			sTable[i+4] = tmp[2];
			sTable[i+5] = tmp[5];
		}
	}


	private static boolean nonempty(List<Set<String>> sets)
	{
		for (Set<String> set : sets)
			if (!set.isEmpty())
				return true;
		return false;
	}


	private static Set<String> findSub(List<Set<String>> sets)
	{
		Set<String> sub = new HashSet<String>();
		Set<String> orig = new HashSet<String>();
		List<Set<String>> con = new ArrayList<Set<String>>();
		List<Set<String>> acon = new ArrayList<Set<String>>();

		for (Set<String> set : sets)
			if (!set.isEmpty())
			{
				orig = set;
				break;
			}

		boolean first = true;
		for (String elem : orig)
		{
			if (first)
			{
				sub.add(elem);
				for (Set<String> set : sets)
				{
					if (set == orig)
						continue;
					if (set.contains(elem))
						con.add(set);
					else
						acon.add(set);
				}
				first = false;
				continue;
			}
			boolean ok = true;
			for (Set<String> set : con)
				if (!set.contains(elem))
				{
					ok = false;
					break;
				}
			for (Set<String> set : acon)
				if (set.contains(elem))
				{
					ok = false;
					break;
				}
			if (ok)
				sub.add(elem);
		}

		return sub;
	}


	private static void printArray(Set<String> set)
	{
		System.out.printf("[");
		for (String s : set)
			System.out.printf("%s,", s);
		System.out.printf("]\n");
	}


	private static void ref(List<Set<String>> sets, List<List<String>> refs,
			Map<String, Set<String>> grps, Set<String> sub, int g)
	{
		String gs = "G" + Integer.toString(g);
		grps.put(gs, sub);

		for (int i = 0; i < sets.size(); i++)
		{
			Set<String> set = sets.get(i);
			if (set.containsAll(sub))
			{
				set.removeAll(sub);
				refs.get(i).add(gs);
			}
		}
	}


	/**
	 * @return the names of the groups
	 */
	public String[] getGroupNames()
	{
		return sGroupNames;
	}


	/**
	 * @return the actual groups of hole pairs
	 */
	public String[][] getGroups()
	{
		return sGroups;
	}


	/**
	 * @return the action table of group names
	 */
	public String[][] getTable()
	{
		return sTable;
	}

}
