
package poker.server.base.impl;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import bayes.Distribution;

/**
 * Player profile.
 * 
 * @author lowentropy
 */
public class Profile
{

	/** profile storage version */
	private static final int	VERSION		= 2;

	/** rate by which mood changes from old to new estimate */
	private static final double	MOOD_RATE	= 1.0;

	/** player name */
	private String				name;

	/** number of averages without showing down */
	private int					numFlatAvg;

	/** number of averages with showing down */
	private int					numShowAvg;

	/** average distribution without showing down */
	private Distribution		flatAvg;

	/** average distribution with showing down */
	private Distribution		showAvg;

	/** running average of player style, with and without showing down */
	private Distribution		moodAvg;

	/** whether profile has been loaded */
	private boolean				loaded;

	/** whether profile has been modified */
	private boolean				modified;


	/**
	 * Constructor.
	 * 
	 * @param name
	 *            name of player
	 */
	public Profile(String name)
	{
		this.name = name;
		this.loaded = false;
	}


	/**
	 * Add a style estimate (average it in).
	 * 
	 * @param style
	 * @param shown
	 */
	public void addEstimate(Distribution style, boolean shown)
	{
		init();

		double s = Math.sqrt((double) (shown ? numShowAvg : numFlatAvg));
		Distribution d = shown ? showAvg : flatAvg;
		d.avgMerge(s, style);

		addToMood(style);

		if (shown)
			numShowAvg++;
		else
			numFlatAvg++;

		modified = true;
	}


	/**
	 * @return current player style
	 */
	public Distribution getStyle()
	{
		init();

		return moodAvg;
	}


	/**
	 * Merge in the average in the other profile.
	 * 
	 * @param p
	 *            profile to merge
	 */
	public void merge(Profile p)
	{
		addToMood(p.moodAvg);

		int flatSum = numFlatAvg + p.numFlatAvg;
		int showSum = numShowAvg + p.numShowAvg;

		Distribution d = flatAvg.copyAndZero();
		d.addInMultiplied(flatAvg, (double) numFlatAvg / (double) flatSum);
		d.addInMultiplied(p.flatAvg, (double) p.numFlatAvg / (double) flatSum);
		d.normalize();
		flatAvg = d;
		numFlatAvg = flatSum;

		d = showAvg.copyAndZero();
		d.addInMultiplied(showAvg, (double) numShowAvg / (double) showSum);
		d.addInMultiplied(p.showAvg, (double) p.numShowAvg / (double) showSum);
		d.normalize();
		showAvg = d;
		numShowAvg = showSum;
		
		modified = true;
	}


	/**
	 * Delete the file associated with this profile.
	 * 
	 * @return whether a file was deleted
	 */
	public boolean delete()
	{
		if (exists())
			return getFile().delete();
		else
			return false;
	}


	/**
	 * @return whether profile exists in storage
	 */
	public boolean exists()
	{
		return getFile().exists();
	}


	/**
	 * @return whether profile contents were loaded
	 */
	public boolean isLoaded()
	{
		return loaded;
	}


	/**
	 * @return whether profile has unsaved modifications
	 */
	public boolean isModified()
	{
		return modified;
	}


	/**
	 * @return player name
	 */
	public String getName()
	{
		return name;
	}


	/**
	 * @return average style when hands were shown
	 */
	public Distribution getShownHandsAverage()
	{
		return showAvg;
	}


	/**
	 * @return combined average style
	 */
	public Distribution getHistoryAverage()
	{
		double s = (double) numFlatAvg / (double) numShowAvg;
		Distribution dist = showAvg.copyAndZero();
		dist.addInMultiplied(showAvg, 1.0);
		dist.addInMultiplied(flatAvg, s);
		dist.normalize();
		return dist;
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String prof = "profile for " + name + ":\n";
		prof += "\t     ";
		prof += "     N     ";
		prof += "     D     ";
		prof += "     T     ";
		prof += "     LP    ";
		prof += "     LA    \n";
		prof += distPrint("flat", flatAvg, numFlatAvg);
		prof += distPrint("show", showAvg, numShowAvg);
		prof += distPrint("hist", getHistoryAverage(), numFlatAvg + numShowAvg);
		prof += distPrint("mood", moodAvg, -1);
		return prof;
	}


	/**
	 * Load a profile from a file, not knowing the player's name yet.
	 * 
	 * @param file
	 *            file to load from
	 * @return profile object, or null on failure
	 */
	public static Profile load(File file)
	{
		Profile p = new Profile(null);
		p.internalLoad(file);
		if (p.loaded)
			return p;
		else
			return null;
	}


	/**
	 * Load the profile, if it is not already loaded.
	 */
	public void load()
	{
		internalLoad(getFile());
	}


	/**
	 * Save the profile if it has been modified.
	 */
	public void save()
	{
		if (!modified)
			return;

		try
		{
			DataOutputStream stream = new DataOutputStream(
					new FileOutputStream(getFile()));

			stream.writeInt(VERSION);
			stream.writeUTF(name);
			stream.writeInt(numFlatAvg);
			stream.writeInt(numShowAvg);
			flatAvg.write(stream);
			showAvg.write(stream);
			moodAvg.write(stream);

			stream.close();
		}
		catch (IOException e)
		{
			System.err.printf("error saving profile for %s:\n", name);
			return;
		}

		modified = false;
	}


	/**
	 * Make sure profile is initialized, perhaps by setting default values.
	 */
	private void init()
	{
		if (loaded || modified)
			return;

		if (exists())
		{
			load();
			return;
		}

		numFlatAvg = 0;
		numShowAvg = 0;
		flatAvg = getDefaultDistribution();
		showAvg = getDefaultDistribution();
		moodAvg = getDefaultDistribution();

		modified = true;
	}


	/**
	 * @return default style distribution
	 */
	private Distribution getDefaultDistribution()
	{
		return new Distribution("style", new String[] {"N", "D", "T", "LP",
				"LA"}, new double[] {1.0, 0.0, 0.0, 0.0, 0.0});
	}


	/**
	 * @return file associated with profile in storage
	 */
	private File getFile()
	{
		return new File("profiles/" + name + ".profile");
	}


	/**
	 * Load the profile from the given file, if it is not already loaded.
	 */
	private void internalLoad(File file)
	{
		if (loaded)
			return;

		try
		{
			DataInputStream stream = new DataInputStream(new FileInputStream(
					file));

			int version = stream.readInt();
			if (version != VERSION)
			{
				System.err.printf(
						"can't read profile for %s: version %d should be %d\n",
						name, version, VERSION);
				stream.close();
				return;
			}

			String name = stream.readUTF();
			if (this.name == null)
				this.name = name;
			else if (!name.equals(this.name))
			{
				System.err.printf("name mismatch: %s should be %s\n", name,
						this.name);
				stream.close();
				return;
			}

			numFlatAvg = stream.readInt();
			numShowAvg = stream.readInt();
			flatAvg = new Distribution(stream, "style");
			showAvg = new Distribution(stream, "style");
			moodAvg = new Distribution(stream, "style");

			stream.close();
		}
		catch (IOException e)
		{
			System.err.printf("error reading profile for %s:\n", name);
			e.printStackTrace();
			return;
		}

		loaded = true;
		modified = false;
	}


	/**
	 * Helper for toString(), prints one distribution.
	 * 
	 * @return pretty-print of distribution
	 */
	private String distPrint(String name, Distribution dist, int numInAvg)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.printf("\t%s:");
		for (double d : dist.values)
			pw.printf("  %.8f%%", d);
		if (numInAvg >= 0)
			pw.printf("  (%d)\n", numInAvg);
		else
			pw.printf("  (log)\n");
		return sw.getBuffer().toString();
	}


	/**
	 * Average a new distribution into the current mood, replacing default mood
	 * if no estimates had yet been added.
	 * 
	 * @param dist
	 *            distribution to average in
	 */
	private void addToMood(Distribution dist)
	{
		if (numShowAvg + numFlatAvg == 0)
			moodAvg.zero();
		moodAvg.addInMultiplied(dist, MOOD_RATE);
		moodAvg.normalize();
	}
}
