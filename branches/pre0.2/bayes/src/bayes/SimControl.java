/*
 * SimControl.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

public interface SimControl
{

	/**
	 * @return whether to stop the simulation (temporarily)
	 */
	boolean stop();


	/**
	 * @return whether the simulation is truly done
	 */
	boolean done();


	/**
	 * @return whether net should log current iteration
	 */
	boolean log();


	/**
	 * Called when network finishes an iteration (after calling log())
	 */
	void iterDone();


	/**
	 * A network has begin using controller.
	 */
	void begin();


	/**
	 * The network has stopped using the controller.
	 */
	void end();

}
