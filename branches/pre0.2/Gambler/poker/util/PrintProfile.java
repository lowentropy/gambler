
package poker.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import poker.server.base.impl.Profile;

/**
 * Main class.
 * 
 * @author lowentropy
 */
public class PrintProfile
{

	/**
	 * Pretty-print one or more profiles.
	 * 
	 * @param args
	 *            filenames
	 */
	public static void main(String[] args)
	{
		List<Profile> profiles = new ArrayList<Profile>();
		
		for (String fname : args)
		{
			Profile p = Profile.load(new File(fname));
			if (p != null)
				profiles.add(p);
		}
		
		for (Profile p : profiles)
			System.out.printf("%s\n", p.toString());
	}

}
