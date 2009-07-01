
package poker.server.session.house;

import poker.server.session.model.data.Field;
import poker.server.session.model.data.List;
import poker.server.session.model.data.ListItem;


public interface ScreenEventHandler
{

	/**
	 * Call a trigger on the given screen object. There is no default behavior
	 * for this function.
	 * 
	 * @param name
	 *            name of screen object triggered
	 */
	public void trigger(String name);


	/**
	 * At start of screen swap, initialize the screen. The default behavior is
	 * to wait until all verify objects are satisfied, and then to load default
	 * triggers and start auto-updating if specified.
	 * 
	 * @param name
	 *            name of new screen
	 */
	public boolean initScreen(String name);


	/**
	 * When a window is expected to become active, initialize its state. The
	 * default behavior is to locate the window by looking for it or waiting
	 * until all verify objects are satisfied (in either order).
	 */
	public boolean initWindow();


	/**
	 * Messages have been sent to the VNC server.
	 * 
	 * @param msgs
	 *            short descriptions of messages
	 */
	public void messagesSent(String[] msgs);


	/**
	 * Messages have been recieved from the VNC server.
	 * 
	 * @param msgs
	 *            short descriptions of messages
	 */
	public void messagesRecieved(String[] msgs);


	/**
	 * Wait for the specified number of milliseconds. Default behavior just
	 * makes the thread wait.
	 * 
	 * @param milli
	 *            milliseconds
	 */
	public void wait(int milli);


	/**
	 * Notification that mode has switched to or from auto-update recieval.
	 * 
	 * @param auto
	 *            whether auto-update is now enabled
	 */
	public void switchedMode(boolean auto);


	/**
	 * Store the value of the given field to the given target. The default
	 * behavior actually makes the storage.
	 * 
	 * @param target
	 *            target field
	 * @param src
	 *            source object (either label or validator)
	 * @param value
	 *            value of source
	 */
	public void store(Field target, Object src, Object value);


	/**
	 * Store the given set of values (updated for the given set of sources) into
	 * the given list at the given index. The default behavior uses the target
	 * names from the source objects to select fields within the list item.
	 * 
	 * @param target
	 *            target list
	 * @param idx
	 *            index of list to store in
	 * @param src
	 *            source objects
	 * @param value
	 *            values
	 */
	public void store(List target, int idx, Object[] src, Object[] value);


	/**
	 * A scan was completed on the given grid.
	 * 
	 * @param name
	 *            name of grid scanned
	 * @param num
	 *            number of scanned items (less than or equal to the requested
	 *            number)
	 */
	public void scanDone(String name, int num);


	/**
	 * Notification that triggers have been cleared out.
	 */
	public void triggersCleared();


	/**
	 * Screen coherency lost while performing given task.
	 * 
	 * @param task
	 *			task which was being done when failure occurred
	 * @param exc
	 *			exception before failure
	 */
	public void fail(String task, Throwable exc);


	public boolean stopAfter(ListItem item);
}
