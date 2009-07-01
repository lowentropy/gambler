/*
 * BayesTest.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bayes.BayesError;
import bayes.BayesNet;
import bayes.BayesNode;
import bayes.Distribution;
import bayes.Ordering;
import bayes.ProbFunction;
import bayes.Query;
import bayes.RunOnceControl;
import junit.framework.TestCase;


public class BayesTest extends TestCase
{

	private BayesNet					net;

	private BayesNode					a, b, c, d, e;

	private BayesNode					n0, n1, n2, n3, n4;

	private Map<String, Distribution>	qdmap				= new HashMap<String, Distribution>();

	private double[][][]				avgs;

	private String[]					names;

	private boolean						doAllMarkovTests	= true;

	private boolean						doTestMarkovBoth	= false;

	private boolean						doTestMarkovPrior	= false;

	private boolean						markovDebug			= true;

	private boolean						blanketDebug		= true;


	private void setupABC() throws BayesError
	{
		net = new BayesNet("test");
		a = new BayesNode("a", new String[]
			{ "foo" });
		b = new BayesNode("b", new String[]
			{ "bar" });
		c = new BayesNode("c", new String[]
			{ "baz" });
	}


	private void addCBA() throws BayesError
	{
		net.addNode(c);
		net.addNode(b);
		net.addNode(a);
	}


	private void get012() throws BayesError
	{
		n0 = net.getNode(0);
		n1 = net.getNode(1);
		n2 = net.getNode(2);
	}


	private void get01234() throws BayesError
	{
		n0 = net.getNode(0);
		n1 = net.getNode(1);
		n2 = net.getNode(2);
		n3 = net.getNode(3);
		n4 = net.getNode(4);
	}


	private void setupDist() throws BayesError
	{
		setupABC();
		addCBA();
		a.addState("a2");
		b.addState("b2");
		c.addState("c2");
		c.addState("c3");
		b.addParent(a);
		b.setConditional(new double[]
			{ .3, .1, .7, .9 });
	}


	private void setupMedical() throws BayesError
	{
		net = new BayesNet("test");
		a = new BayesNode("a", "T", "F");
		b = new BayesNode("b", "T", "F");
		c = new BayesNode("c", "T", "F");
		d = new BayesNode("d", "T", "F");
		e = new BayesNode("e", "T", "F");
		b.addParent(a);
		c.addParent(a);
		d.addParent(c);
		d.addParent(b);
		e.addParent(c);
		a.setPrior(.2, .8);
		b.setConditional(new double[]
			{ .8, .2, .2, .8 });
		c.setConditional(new double[]
			{ .2, .05, .8, .95 });
		d.setConditional(new double[]
			{ .8, .8, .8, .05, .2, .2, .2, .95 });
		e.setConditional(new double[]
			{ .8, .6, .2, .4 });
		net.addNode(a);
		net.addNode(b);
		net.addNode(c);
		net.addNode(d);
		net.addNode(e);
	}


	private void queryDist(String... vars) throws BayesError
	{
		qdmap.clear();
		for (String var : vars)
			qdmap.put(var, null);
		net.queryDist(qdmap);
	}


	private void checkDist(String var, double... dist)
	{
		compareArrays(dist, qdmap.get(var).values);
	}


	private void compareArrays(double[] a, double[] b)
	{
		assertEquals(a.length, b.length);
		for (int i = 0; i < a.length; i++)
			compareDouble(a[i], b[i]);
	}


	private void compareDouble(double a, double b)
	{
		double diff = Math.abs(a - b);
		if (diff > Distribution.tolerance)
			assertEquals(a, b);
	}


	private void assertParents(BayesNode n, BayesNode... P)
	{
		Collection<BayesNode> parents = n.getParents();
		assertEquals(P.length, parents.size());
		int i = 0;
		for (BayesNode p : parents)
			assertTrue(P[i++] == p);
	}


	private void assertChildren(BayesNode n, BayesNode... C)
	{
		Collection<BayesNode> children = n.getChildren();
		assertEquals(C.length, children.size());
		int i = 0;
		for (BayesNode c : children)
			assertTrue(C[i++] == c);
	}


	/**
	 * Test node ordering which has no cycles.
	 */
	public void testOrderingOk()
	{
		System.out.println("running test: testOrderingOk");

		try
		{
			setupABC();
			b.addParent(a);
			c.addParent(b);
			addCBA();
			get012();
			assertTrue(n0 == a);
			assertTrue(n1 == b);
			assertTrue(n2 == c);

			setupMedical();
			net.forceOrder();
			get01234();
			assertTrue(n0 == a);
			assertTrue(n1 == c);
			assertTrue(n2 == b);
			assertTrue(n3 == e);
			assertTrue(n4 == d);
		}
		catch (BayesError e)
		{
			fail(e.getMessage());
		}
	}


	/**
	 * Test that node ordering with a cycle is detected.
	 */
	public void testOrderingCycle()
	{
		System.out.println("running test: testOrderingCycle");

		try
		{
			setupABC();
			a.addParent(b);
			b.addParent(a);
			addCBA();
			net.forceOrder();
		}
		catch (BayesError e)
		{
			if (e.getMessage().indexOf("cycle") == -1
					|| (e.getMessage().indexOf("a, b") == -1 && e.getMessage()
							.indexOf("b, a") == -1))
				fail(e.getMessage());
		}
	}


	/**
	 * Test adding a parent creates the proper conditional distributions.
	 * 
	 * @throws BayesError
	 */
	public void testAddParent()
	{
		System.out.println("running test: testAddParent");

		try
		{
			/* set the thing up */
			setupDist();
			b.addParent(c);

			/* make sure children/parents are correct */
			assertParents(b, a, c);
			assertChildren(a, b);
			assertChildren(c, b);

			/* make sure distribution is correct */
			compareArrays(new double[]
				{ .3, .3, .3, .1, .1, .1, .7, .7, .7, .9, .9, .9 }, b
					.getConditional());
		}
		catch (BayesError e)
		{
			fail(e.getMessage());
		}
	}


	/**
	 * Test removing a parent creates the proper conditional distributions.
	 */
	public void testRemoveSecondParent()
	{
		System.out.println("running test: testRemoveSecondParent");

		try
		{
			setupDist();
			b.addParent(c);
			b.setConditional(new double[]
				{ .3, .1, .5, .7, .2, .6, .7, .9, .5, .3, .8, .4 });
			b.removeParent(a, "a2");
			compareArrays(new double[]
				{ .7, .2, .6, .3, .8, .4 }, b.getConditional());
		}
		catch (BayesError e)
		{
			fail(e.getMessage());
		}
	}


	/**
	 * Test removing a parent creates the proper conditional distributions.
	 */
	public void testRemoveFirstParent()
	{
		System.out.println("running test: testRemoveFirstParent");

		try
		{
			setupDist();
			b.addParent(c);
			b.setConditional(new double[]
				{ .3, .1, .5, .7, .2, .6, .7, .9, .5, .3, .8, .4 });
			b.removeParent(c, "baz");
			compareArrays(new double[]
				{ .3, .7, .7, .3 }, b.getConditional());
		}
		catch (BayesError e)
		{
			fail(e.getMessage());
		}
	}


	/**
	 * Test adding a state to a variable creates proper prior and conditional
	 * distributions.
	 */
	public void testAddStateToFirst()
	{
		System.out.println("running test: testAddStateToFirst");

		try
		{
			setupDist();
			b.addParent(c);
			b.setConditional(new double[]
				{ .3, .1, .5, .7, .2, .6, .7, .9, .5, .3, .8, .4 });
			c.addState("new");
			compareArrays(new double[]
				{ .3, .1, .5, .5, .7, .2, .6, .6, .7, .9, .5, .5, .3, .8, .4,
						.4 }, b.getConditional());
		}
		catch (BayesError e)
		{
			fail(e.getMessage());
		}
	}


	/**
	 * Test adding a state to a variable creates proper prior and conditional
	 * distributions.
	 */
	public void testAddStateToSecond()
	{
		System.out.println("running test: testAddStateToSecond");

		try
		{
			setupDist();
			b.addParent(c);
			b.setConditional(new double[]
				{ .3, .1, .5, .7, .2, .6, .7, .9, .5, .3, .8, .4 });
			a.addState("new");
			compareArrays(new double[]
				{ .3, .1, .5, .7, .2, .6, .7, .2, .6, .7, .9, .5, .3, .8, .4,
						.3, .8, .4 }, b.getConditional());
		}
		catch (BayesError e)
		{
			fail(e.getMessage());
		}
	}


	/**
	 * Test removing a state from a variable creates proper prior and
	 * conditional distributions.
	 */
	public void testRemoveStateFromFirst()
	{
		System.out.println("running test: testRemoveStateFromFirst");

		try
		{
			setupDist();
			b.addParent(c);
			b.setConditional(new double[]
				{ .3, .1, .5, .7, .2, .6, .7, .9, .5, .3, .8, .4 });

			c.setPrior(new double[]
				{ .1, .5, .4 });
			c.removeState("c2");

			compareArrays(new double[]
				{ .3, .5, .7, .6, .7, .5, .3, .4 }, b.getConditional());
			compareArrays(new double[]
				{ .2, .8 }, c.getPrior());
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	/**
	 * Test removing a state from a variable creates proper prior and
	 * conditional distributions.
	 */
	public void testRemoveStateFromSecond()
	{
		System.out.println("running test: testRemoveStateFromSecond");

		try
		{
			setupDist();
			b.addParent(c);
			b.setConditional(new double[]
				{ .3, .1, .5, .7, .2, .6, .7, .9, .5, .3, .8, .4 });

			a.setPrior(new double[]
				{ .1, .9 });
			a.removeState("foo");

			compareArrays(new double[]
				{ .7, .2, .6, .3, .8, .4 }, b.getConditional());
			compareArrays(new double[]
				{ 1.0 }, a.getPrior());
		}
		catch (BayesError e)
		{
			fail(e.getMessage());
		}
	}


	/**
	 * Test simple inference with evidence nodes and one conditional node.
	 */
	public void testSimpleInference()
	{
		System.out.println("running test: testSimpleInference");

		try
		{
			setupDist();
			b.addParent(c);
			b.setConditional(new double[]
				{ .3, .1, .5, .7, .2, .6, .7, .9, .5, .3, .8, .4 });
			a.observe("foo");
			c.observe("c3");
			net.inference();

			queryDist("b");
			checkDist("b", 0.5, 0.5);

			a.observe("a2");
			c.observe("c2");
			net.inference();

			queryDist("b");
			checkDist("b", .2, .8);
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	public void testComplexInferenceNoEvidence()
	{
		System.out.println("running test: testComplexInferenceNoEvidence");

		try
		{
			setupMedical();
			net.inference();
			String[] vars = new String[]
				{ "a", "b", "c", "d", "e" };
			queryDist(vars);

			checkDist("a", .2, .8);
			checkDist("b", .32, .68);
			checkDist("c", .08, .92);
			checkDist("d", .3308, .6692);
			checkDist("e", .616, .384);
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	/**
	 * Tests both combinations of priors evidence in the medical net.
	 */
	public void testComplexInferenceEvidence()
	{
		System.out.println("running test: testComplexInferenceEvidence");

		try
		{
			setupMedical();
			String[] vars = new String[]
				{ "a", "b", "c", "d", "e" };

			a.observe("T");
			net.inference();
			queryDist(vars);

			checkDist("a", 1.0, 0.0);
			checkDist("b", .8, .2);
			checkDist("c", .2, .8);
			checkDist("d", .68, .32);
			checkDist("e", .64, .36);

			a.observe("F");
			net.inference();
			queryDist(vars);

			checkDist("a", 0.0, 1.0);
			checkDist("b", .2, .8);
			checkDist("c", .05, .95);
			checkDist("d", .23, .77);
			checkDist("e", .61, .39);
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


//	/**
//	 * Test that storing/loading works correctly.
//	 */
//	public void testStore()
//	{
//		System.out.println("running test: testStore");
//
//		try
//		{
//			setupMedical();
//			net.writeToFile("test1.net");
//			BayesNet n2 = BayesNet.readFromFile("test1.net");
//			assertEquals(net, n2);
//
//			setupDist();
//			b.addParent(c);
//			net.writeToFile("test2.net");
//			n2 = BayesNet.readFromFile("test2.net");
//			assertEquals(net, n2);
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}


	/**
	 * Run a simple markov test repeatedly, with an increasing number of
	 * iterations. After each iteration, record the average distributions, but
	 * don't clear the average (they should progress toward the solution).
	 * 
	 * @param max
	 *            maximum iterations
	 * @param step
	 *            stepsize
	 * @throws BayesError
	 */
	private void runMarkovTest(int max, int step, String... vars)
			throws BayesError
	{
		int i = 0, num = max / step;
		avgs = new double[num][][];
		names = vars;

		/* run markov approximation with increasing max iterations */
		RunOnceControl ctl = new RunOnceControl(100, false);
		for (int iter = step; iter <= max; iter += step)
		{
			ctl.setIterations(iter);
			net.markovBlanket(ctl);
			avgs[i++] = net.getAverages(names);
		}
	}


	/**
	 * Check the accuracy of the last markov test sequence. Compare the results
	 * against the solution given by bucket elimination.
	 * 
	 * @param q
	 *            query which yielded correct solution
	 * @param name
	 *            name of test
	 * @param max
	 *            maximum iterations
	 * @param step
	 *            stepsize for iterations
	 * @param maxErr
	 *            maximum allowed error (out of 100.0)
	 * @throws BayesError
	 */
	private void checkMarkovAccuracy(Query q, String name, int max, int step,
			double maxErr, BayesNode... comp) throws BayesError
	{
		/* now recall all the data */
		BayesNode[] test = comp;

		/* get accuracy info */
		double[] tot = new double[avgs.length];
		double[][] cmp = new double[avgs.length][5];
		for (int i = 0; i < avgs.length; i++)
		{
			double sum = 0.0;
			for (int j = 0; j < names.length; j++)
				sum += cmp[i][j] = error(q, names[j], avgs[i][j]);
			tot[i] = sum / (double) names.length;
		}

		/* print the info */
		if (markovDebug)
		{
			System.out.printf("Markov Approx Test: %s\n", name);
			System.out.printf("STEP = %d, MAX = %d\n\n", step, max);
			for (int i = 0; i < avgs.length; i++)
			{
				for (int j = 0; j < names.length; j++)
				{
					if (test[j].isObserved())
						System.out.printf("\t%s\t: evidence = %s\n", names[j],
								test[j].getState());
					else
						System.out.printf(
								"\t%s\t: %8.4f%% (%8.4f%% vs %8.4f%%)\n",
								names[j], cmp[i][j], avgs[i][j][0] * 100.0,
								q.getMarginal(names[j]).values[0] * 100.0);
				}

				System.out.printf("\ttotal\t: %8.4f%%\n\n", tot[i]);
			}
		}

		/* now check to make sure final error is < max% */
		assertTrue(tot[tot.length - 1] < maxErr);
	}


	/**
	 * Check for nans in array.
	 * 
	 * @param data
	 * @throws BayesError
	 */
	private void checkNans(double[] data) throws BayesError
	{
		for (int i = 0; i < data.length; i++)
			if (Double.isNaN(data[i]))
				throw new BayesError("nan detected");
	}


	/**
	 * Error of approximation against real solution.
	 * 
	 * @param var
	 * @param d1
	 * @return
	 * @throws BayesError
	 */
	private double error(Query q, String var, double[] d1) throws BayesError
	{
		if (d1 == null) // an evidence node
			return 0.0;

		double[] d2 = q.getMarginal(var).values;

		checkNans(d1);
		checkNans(d2);

		double diff = 0.0;
		assertEquals(d1.length, d2.length);
		for (int i = 0; i < d1.length; i++)
			diff += Math.abs(d1[i] - d2[i]);
		if (d1.length == 0)
			fail("d1.length == 0");
		diff /= d1.length;
		return diff * 100;
	}


	/**
	 * Test markov approximation accuracy when no evidence given.
	 */
	public void testMarkovApproxNoEvidence()
	{
		if (!doAllMarkovTests)
			return;

		System.out.println("running test: testMarkovApproxNoEvidence");

		try
		{
			/* first, derive inference values and save them */
			String[] names = new String[]
				{ "a", "b", "c", "d", "e" };

			setupMedical();
			Query q = new Query("test", net, names);
			q.setObserved();
			q.setPrior();
			q.setQueried(names);
			q.solve();
			runMarkovTest(100000, 20000, names);
			checkMarkovAccuracy(q, "no evidence", 100000, 20000, 1.0, a, b, c,
					d, e);
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	/**
	 * Test markov approximation accuracy when prior nodes have evidence.
	 */
	public void testMarkovApproxPriorEvidence()
	{
		if (!doAllMarkovTests && !doTestMarkovPrior)
			return;

		System.out.println("running test: testMarkovApproxPriorEvidence");

		try
		{
			/* first, derive inference values and save them */
			setupMedical();

			names = new String[]
				{ "d", "e" };

			a.observe("T");
			Query q = new Query("test", net, names);
			q.setObserved("a");
			q.setPrior();
			q.setQueried(names);
			q.solve();
			runMarkovTest(100000, 20000, names);
			checkMarkovAccuracy(q, "prior evidence (A=T)", 100000, 20000, 1.0,
					d, e);

			a.observe("F");
			// q.invalidate(); // DBG
			q.solve();
			runMarkovTest(100000, 20000, names);
			checkMarkovAccuracy(q, "prior evidence (A=F)", 100000, 20000, 1.0,
					d, e);
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	/**
	 * Run a markov test which cannot be compared to direct inference, because
	 * the observed-ancestor rule is not satisfied. Instead, compare results
	 * against solution with bucket elimination.
	 * 
	 * @param name
	 *            name of test
	 * @param qvars
	 *            variables to query
	 * @param maxErr
	 *            maximum error percentage
	 * @param args
	 *            pairs of variable-name / evidence state
	 * @throws BayesError
	 */
	private void runPosteriorMarkov(String name, String[] qvars, double maxErr,
			String... args) throws BayesError
	{
		/* get names and states from args */
		String[] names = new String[args.length / 2];
		String[] ev = new String[args.length / 2];
		for (int i = 0; i < args.length; i += 2)
		{
			names[i / 2] = args[i];
			ev[i / 2] = args[i + 1];
		}

		/* generate full name of test */
		StringBuilder sb = new StringBuilder();
		sb.append(name + " (");
		for (int i = 0; i < ev.length; i++)
			sb.append(names[i].toUpperCase() + "=" + ev[i] + ", ");
		sb.setLength(sb.length() - 2);
		sb.append(")");
		name = sb.toString();

		/* record clean net then set evidence */
		net.clearEvidence();
		for (int i = 0; i < ev.length; i++)
			net.getNode(names[i]).observe(ev[i]);
		Query q = new Query("test", net, qvars);
		q.setQueried(qvars);
		q.setObserved(names);
		q.setPrior();
		q.solve();

		/* run test, then run inference on mapped priors */
		runMarkovTest(100000, 20000, qvars);

		BayesNode[] qnodes = new BayesNode[qvars.length];
		for (int i = 0; i < qnodes.length; i++)
			qnodes[i] = net.getNode(qvars[i]);

		/*
		 * check priors inference against approximation; evidence is still
		 * cleared, because the accuracy will be checked both ways: real
		 * evidence against priors inference, as well as markov against direct
		 * posterior inference. Evidence nodes computed a default average equal
		 * to posterior probability.
		 */
		checkMarkovAccuracy(q, name, 100000, 20000, maxErr, qnodes);
	}


	/**
	 * Test markov approximation accuracy when posterior nodes have evidence.
	 */
	public void testMarkovApproxPosteriorEvidence()
	{
		if (!doAllMarkovTests)
			return;

		System.out.println("running test: testMarkovApproxPosteriorEvidence");

		try
		{
			setupMedical();
			String[] qvars = new String[]
				{ "a" };
			runPosteriorMarkov("posterior evidence", qvars, 1.0, "d", "T", "e",
					"T");
			runPosteriorMarkov("posterior evidence", qvars, 1.0, "d", "T", "e",
					"F");
			runPosteriorMarkov("posterior evidence", qvars, 1.0, "d", "F", "e",
					"T");
			runPosteriorMarkov("posterior evidence", qvars, 1.0, "d", "F", "e",
					"F");
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	/**
	 * Test markov approximation accuracy when middle-level nodes have evidence.
	 */
	public void testMarkovApproxMidEvidence()
	{
		if (!doAllMarkovTests)
			return;

		System.out.println("running test: testMarkovApproxMidEvidence");

		try
		{
			setupMedical();
			String[] qvars = new String[]
				{ "a", "d", "e" };
			runPosteriorMarkov("mid evidence", qvars, 1.0, "b", "T", "c", "T");
			runPosteriorMarkov("mid evidence", qvars, 1.0, "b", "T", "c", "F");
			runPosteriorMarkov("mid evidence", qvars, 1.0, "b", "F", "c", "T");
			runPosteriorMarkov("mid evidence", qvars, 1.0, "b", "F", "c", "F");
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	/**
	 * Test markov approximation accuracy when both prior and posterior nodes
	 * have evidence.
	 */
	public void testMarkovApproxBothEvidence()
	{
		if (!doAllMarkovTests && !doTestMarkovBoth)
			return;

		System.out.println("running test: testMarkovApproxBothEvidence");

		try
		{
			setupMedical();
			String[] qvars = new String[]
				{ "b", "c" };
			runPosteriorMarkov("both evidence", qvars, 1.0, "a", "T", "d", "T",
					"e", "T");
			runPosteriorMarkov("both evidence", qvars, 1.0, "a", "T", "d", "T",
					"e", "F");
			runPosteriorMarkov("both evidence", qvars, 1.0, "a", "T", "d", "F",
					"e", "T");
			runPosteriorMarkov("both evidence", qvars, 1.0, "a", "T", "d", "F",
					"e", "F");
			runPosteriorMarkov("both evidence", qvars, 1.0, "a", "F", "d", "T",
					"e", "T");
			runPosteriorMarkov("both evidence", qvars, 1.0, "a", "F", "d", "T",
					"e", "F");
			runPosteriorMarkov("both evidence", qvars, 1.0, "a", "F", "d", "F",
					"e", "T");
			runPosteriorMarkov("both evidence", qvars, 1.0, "a", "F", "d", "F",
					"e", "F");
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	/**
	 * Tests a randomly-generated bayesian network by finding an exact solution
	 * and then testing the convergence of the markov approximation. Repeats
	 * several times with different parameter sets.
	 */
	public void testRandomMarkovConvergence()
	{
		if (!doAllMarkovTests)
			return;

		System.out.println("running test: testRandomMarkovConvergence");

		// TODO
	}


	public void testHeuristicOrdering()
	{
		System.out.println("running test: testHeuristicOrdering");

		try
		{
			setupMedical();
			a.observe("T");
			Ordering o = new Ordering(net);
			o.order("d", "e");
			BayesNode[] ord = o.getOrder();

			assertEquals(5, ord.length);
			assertEquals("a", ord[0].getVariable());
			assertEquals("b", ord[1].getVariable());
			assertEquals("c", ord[2].getVariable());
			assertEquals("d", ord[3].getVariable());
			assertEquals("e", ord[4].getVariable());
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	/**
	 * Test summing out of distributions.
	 */
	public void testSumOutFunc()
	{
		System.out.println("running test: testSumOutFunc");

		try
		{
			BayesNode n0 = new BayesNode("0", "0", "1", "2", "3");
			BayesNode n1 = new BayesNode("1", "0", "1", "2");
			BayesNode n2 = new BayesNode("2", "0", "1");
			List<BayesNode> nl = new ArrayList<BayesNode>(3);
			nl.add(n0);
			nl.add(n1);
			nl.add(n2);
			double[] dist = new double[24];
			double ctr = .1;
			for (int i = 0; i < 24; i++)
			{
				dist[i] = ctr;
				ctr += .1;
				if (ctr > .9)
					ctr = .1;
			}
			ProbFunction func = new ProbFunction(nl, dist);

			// +-+
			// 00:a+c+e:0.9
			// 01:b+d+f:1.2
			// 10:g+i+k:1.8
			// 11:h+j+l:1.2
			// 20:m+o+q:1.8
			// 21:n+p+r:2.1
			// 30:s+u+w:0.9
			// 31:t+v+x:1.2
			ProbFunction f1 = func.sumOut(n1);
			compareArrays(new double[]
				{ 0.9, 1.2, 1.8, 1.2, 1.8, 2.1, 0.9, 1.2 }, f1.getData());

			// +--
			// 0:a+b+c+d+e+f:2.1
			// 1:g+h+i+j+k+l:3.0
			// 2:m+n+o+p+q+r:3.9
			// 3:s+t+u+v+w+x:2.1
			ProbFunction f2 = func.sumOut(n1, n2);
			compareArrays(new double[]
				{ 2.1, 3.0, 3.9, 2.1 }, f2.getData());

			// -+-
			// 0:a+b+g+h+m+n+s+t:3.0
			// 1:c+d+i+j+o+p+u+v:3.7
			// 2:e+f+k+l+q+r+w+x:4.4
			ProbFunction f3 = func.sumOut(n0, n2);
			compareArrays(new double[]
				{ 3.0, 3.7, 4.4 }, f3.getData());

			// -++
			// 00:a+g+m+s:1.3
			// 01:b+h+n+t:1.7
			// 10:c+i+o+u:2.1
			// 11:d+j+p+v:1.6
			// 20:e+k+q+w:2.0
			// 21:f+l+r+x:2.4
			ProbFunction f4 = func.sumOut(n0);
			compareArrays(new double[]
				{ 1.3, 1.7, 2.1, 1.6, 2.0, 2.4 }, f4.getData());
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	public void testMultiplyFunc()
	{
		System.out.println("running test: testMultiplyFunc");

		try
		{
			BayesNode x = new BayesNode("X", "0", "1");
			BayesNode y = new BayesNode("Y", "0", "1");
			BayesNode z = new BayesNode("Z", "0", "1");
			List<BayesNode> l1 = new ArrayList<BayesNode>(2);
			List<BayesNode> l2 = new ArrayList<BayesNode>(2);
			l1.add(x);
			l1.add(y);
			l2.add(y);
			l2.add(z);

			// P(X:2,Y:2)
			// 00:a
			// 01:b
			// 10:c
			// 11:d
			ProbFunction f1 = new ProbFunction(l1, new double[]
				{ .2, .3, .5, .7 });

			// P(Y:2,Z:2)
			// 00:e
			// 01:f
			// 10:g
			// 11:h
			ProbFunction f2 = new ProbFunction(l2, new double[]
				{ 1.1, 1.3, 1.7, 1.9 });

			// mult:

			// P(X:2,Y:2,Z:2)

			// 000:a*e
			// 001:a*f
			// 010:b*g
			// 011:b*h
			// 100:c*e
			// 101:c*f
			// 110:d*g
			// 111:d*h

			ProbFunction f3 = ProbFunction.multiply(f1, f2);
			assertEquals("X", f3.getVar(0).getVariable());
			assertEquals("Y", f3.getVar(1).getVariable());
			assertEquals("Z", f3.getVar(2).getVariable());

			compareArrays(new double[]
				{ .22, .26, .51, .57, .55, .65, 1.19, 1.33 }, f3.getData());
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	public void testBucketElimMedical()
	{
		System.out.println("running test: testBucketElimMedical");

		try
		{
			setupMedical();
			a.observe("T");
			Query q = new Query("test", net, "d", "e");
			q.setQueried("d", "e");
			q.setObserved("a");
			q.setPrior();
			q.solve();

			if (blanketDebug)
			{
				System.out.println("a = T");
				q.showTotalDist();
				q.showDistForAll();
				q.showConditionalFor("d");
				q.showConditionalFor("e");
				System.out.printf("\n");
			}

			compareArrays(new double[]
				{ .68, .32 }, q.getMarginal("d").values);
			compareArrays(new double[]
				{ .64, .36 }, q.getMarginal("e").values);

			a.observe("F");
			q.solve();
			if (blanketDebug)
			{
				System.out.println("a = F");
				q.showTotalDist();
				q.showDistForAll();
				q.showConditionalFor("d");
				q.showConditionalFor("e");
				System.out.printf("\n");
			}
		}
		catch (BayesError e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
