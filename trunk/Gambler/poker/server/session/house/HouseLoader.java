/*
 * HouseLoader.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.session.house;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


public class HouseLoader
{

	private static final String			HOUSES_FILENAME	= "conf/houses";

	static
	{
		loadHouses();
	}

	private static Map<String, House>	houses;


	private static void loadHouses()
	{
		houses = new HashMap<String, House>();
		readFile();
	}


	private static void readFile()
	{
		FileInputStream fis = null;
		houses.clear();

		try
		{
			fis = new FileInputStream(HOUSES_FILENAME);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		BufferedReader r = new BufferedReader(new InputStreamReader(fis));
		String line;

		try
		{
			while ((line = r.readLine()) != null)
			{
				int idx = line.indexOf(',');
				String name = line.substring(0, idx);
				String cn = line.substring(idx + 1);
				houses.put(name, readHouse(cn));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		try
		{
			r.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}


	private static void writeFile()
	{
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(HOUSES_FILENAME);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		PrintWriter w = new PrintWriter(fos);
		for (String name : houses.keySet())
		{
			w.print(name);
			w.print(',');
			w.println(houses.get(name).getClass().getName());
		}

		w.close();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args[0].equals("add"))
		{
			if (args.length != 3)
				usage();
			addHouse(args[1], args[2]);
		}
		else if (args[0].equals("del"))
		{
			if (args.length != 2)
				usage();
			deleteHouse(args[1]);
		}
		else if (args[0].equals("list"))
		{
			if (args.length != 1)
				usage();
			listHouses();
		}
	}


	private static void usage()
	{
		System.out.println("usage: java HouseLoader ...\n" + "\tlist"
				+ "\tadd name class\n" + "\tdel name\n");
		System.exit(1);
	}


	private static void listHouses()
	{
		for (String name : houses.keySet())
			System.out.printf("%s : %s\n", name, houses.get(name).getClass()
					.getName());
	}


	private static void addHouse(String name, String className)
	{
		if (houses.containsKey(name))
		{
			System.out.println("duplicate house '" + name + "'");
			return;
		}

		houses.put(name, readHouse(className));
		writeFile();
	}


	private static House readHouse(String className)
	{
		Class<House> houseClass = null;

		try
		{
			houseClass = (Class<House>) Class.forName(className);
			Class[] intrs = houseClass.getInterfaces();
			if (intrs.length != 1
					|| !intrs[0].getName().equals("poker.server.session.house.House"))
			{
				System.out.println("class '" + className
						+ "' doesn't implement 'House'");
				System.exit(1);
			}
		}
		catch (ClassNotFoundException e)
		{
			System.out.println("can't find class '" + className + "'!");
			System.exit(1);
		}

		House house = null;

		try
		{
			house = houseClass.newInstance();
		}
		catch (InstantiationException e)
		{
			System.err
					.println("Error instantiating class '" + className + "':");
			e.printStackTrace();
			System.exit(1);
		}
		catch (IllegalAccessException e)
		{
			System.err.println("Illegal access instantiating class '"
					+ className + "':");
			e.printStackTrace();
			System.exit(1);
		}

		return house;
	}


	private static void deleteHouse(String name)
	{
		if (houses.remove(name) == null)
			System.out.println("house '" + name + "' not found.");
		else
			writeFile();
	}


	public static House getHouse(String name)
	{
		return houses.get(name);
	}
}
