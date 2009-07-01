
package poker.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import sun.net.www.protocol.https.HttpsURLConnectionImpl;

public class AppletGrabber
{

	private static boolean	dbg			= false;

	private static int		MAX_TRIES	= 5;

	private static long		TIMEOUT		= 10000;


	public static boolean grab(String site, Map<String, String> params,
			String tgt)
	{
		URL url = null;
		int max_tries = MAX_TRIES;

		try
		{
			url = new URL(site);
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
			return false;
		}
		String applet;

		int i = 1;
		while (max_tries-- > 0)
		{
			System.out.printf("applet grab try %d\n", i++);
			if ((applet = tryGrab(url, params, TIMEOUT)) != null)
			{
				if (applet.equals("err"))
					return false;

				try
				{
					DataOutputStream out = new DataOutputStream(
							new FileOutputStream(tgt));
					out.writeBytes(applet);
					out.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return false;
				}
				
				System.out.printf("grab ok\n");

				return true;
			}
		}

		System.out.printf("out of tries for applet grab\n");
		return false;
	}


	private static String tryGrab(URL url, Map<String, String> params, long to)
	{
		GrabThread t = new GrabThread(url, params);
		long now = System.currentTimeMillis();
		t.start();
		while ((System.currentTimeMillis() - now) < to)
			if (t.done)
			{
				System.out.printf("applet grab returned %s in %.2fs\n",
						t.ok ? "success" : "error", (float) (System
								.currentTimeMillis() - now) / 1000.0f);
				return t.ok ? t.applet : "err";
			}
		t.stop();
		return null;
	}


	static class GrabThread extends Thread
	{

		URL					url;

		boolean				done;

		boolean				ok;

		Map<String, String>	params;

		String				applet;


		public GrabThread(URL url, Map<String, String> params)
		{
			this.url = url;
			this.params = params;
		}


		public void run()
		{
			URLConnection urlConn;
			DataOutputStream out = null;
			DataInputStream in;
			
			try
			{
				urlConn = url.openConnection();

				urlConn.setDoInput(true);
				urlConn.setDoOutput(true);
				urlConn.setUseCaches(false);
				urlConn.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");

				out = new DataOutputStream(urlConn.getOutputStream());
				int i = 0;
				for (String name : params.keySet())
					out.writeBytes((i++ > 0 ? "&" : "") + name + "="
							+ URLEncoder.encode(params.get(name)));
				out.flush();
				out.close();

				in = new DataInputStream(urlConn.getInputStream());
				String str;
				StringBuilder sb = new StringBuilder();

				while (null != ((str = in.readLine())))
					sb.append(str + "\n");
				applet = sb.toString();
				in.close();

				ok = true;
				done = true;
			}
			catch (IOException e)
			{
				e.printStackTrace();
				ok = false;
				done = true;
			}
		}
	}


	public static boolean defaultModify(String fname, boolean doReal)
	{
		return modify(fname, "applet", doReal, new String[] {
				"applet:delattr:codebase", "param:deltag:name:noncachedurl",
				"param:modattr:name:isRealMoney:value:doReal"});
	}


	public static boolean modify(String fname, String crop, boolean doReal,
			String[] actions)
	{
		File file = new File(fname);
		DataInputStream in;
		StringBuffer sb = new StringBuffer();
		
		System.out.printf("beginning modify of %s\n", fname);

		try
		{
			in = new DataInputStream(new FileInputStream(file));
			String str;
			while ((str = in.readLine()) != null)
				sb.append(str + "\n");
			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
		System.out.println("read input file");

		if (crop != null)
		{
			int idx1 = sb.indexOf("<" + crop);
			int idx2 = sb.indexOf("</" + crop + ">");
			if ((idx1 == -1) || (idx2 == -1))
			{
				System.err.printf("can't find crop point '%s'\n", crop);
				return false;
			}
			sb.delete(idx2 + crop.length() + 3, sb.length());
			sb.delete(0, idx1);
			System.out.println("cropped file");
		}
		else
			System.out.println("skipping crop");

		for (String action : actions)
		{
			System.out.printf("performing action: %s\n", action);
			String[] parts = action.split(":");
			String tag = parts[0];
			String mode = parts[1];
			String attr1 = parts[2];

			int idx = sb.indexOf("<" + tag);
			while (idx != -1)
			{
				int idx3 = sb.indexOf(attr1 + "=", idx);
				if (idx3 == -1)
				{
					if (dbg)
						System.out.printf("couldn't find attr '%s' of '%s'\n",
								attr1, tag);
					int idx2 = sb.indexOf(">", idx);
					idx = sb.indexOf("<" + tag, idx2);
					continue;
				}

				int idx4 = sb.indexOf("\"", idx3 + attr1.length() + 2);
				if (mode.equals("delattr"))
				{
					sb.delete(idx3, idx4 + 1);
					if (dbg)
						System.out.printf("deleting attr '%s' in '%s'\n",
								attr1, tag);
				}
				else
				{
					int idx5 = sb.indexOf("\"", idx3);
					String val = sb.substring(idx5 + 1, idx4);
					if (val.equals(parts[3]))
					{
						if (mode.equals("deltag"))
						{
							sb.delete(idx, sb.indexOf(">", idx) + 1);
							if (dbg)
								System.out.printf(
										"deleting tag '%s' because of %s=%s\n",
										tag, attr1, val);
						}
						else
						{
							String attr2 = parts[4];
							String val2 = parts[5];
							if (val2.equals("doReal"))
								val2 = doReal ? "true" : "false";

							idx3 = sb.indexOf(attr2 + "=", idx);
							if (idx3 == -1)
							{
								System.err.printf(
										"couldn't find attr '%s' of '%s'\n",
										attr2, tag);
								return false;
							}
							idx5 = sb.indexOf("\"", idx3);
							idx4 = sb.indexOf("\"", idx5 + 1);

							if (dbg)
								System.out
										.printf(
												"changing '%s' to '%s' in '%s' because of %s=%s\n",
												sb.substring(idx5 + 1, idx4),
												val2, tag, attr1, val);
							sb.replace(idx5 + 1, idx4, val2);
						}
					}
				}
				int idx2 = sb.indexOf(">", idx);
				idx = sb.indexOf("<" + tag, idx2);
			}
		}
		
		System.out.println("writing output");

		DataOutputStream out;
		try
		{
			out = new DataOutputStream(new FileOutputStream(file));
			out.writeBytes(sb.toString());
			out.flush();
			out.close();
			
			System.out.printf("end (ok) modify of %s\n", fname);

			return true;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return false;
	}


	public static void main(String[] args)
	{
		dbg = true;

		if (args.length < 3)
		{
			System.err
					.println("usage: java AppletGrabber site file doReal [name val ...]");
			System.exit(1);
		}

		Map<String, String> map = new HashMap<String, String>();
		for (int i = 3; i < args.length; i += 2)
			map.put(args[i], args[i + 1]);

		for (String name : map.keySet())
			System.out.printf("%s -> %s\n", name, map.get(name));

		if (!grab(args[0], map, args[1]))
			return;
		System.out.printf("grabbed %s to %s\n", args[0], args[1]);
		if (!defaultModify(args[1], Boolean.parseBoolean(args[2])))
			return;
		System.out.printf("modified %s\n", args[1]);
	}
}
