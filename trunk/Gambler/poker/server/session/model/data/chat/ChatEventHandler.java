
package poker.server.session.model.data.chat;

/**
 * ChatEventHandler objects handle the resulting 'function' calls from formatted
 * chat message interpretation.
 * 
 * @author lowentropy
 */
public interface ChatEventHandler
{

	/**
	 * Handle a successful parse.
	 * 
	 * @param value
	 *            value tree of event
	 */
	public void handleEvent(ChatValue value);


	/**
	 * Handle a failed parse.
	 * 
	 * @param text
	 *            text which failed
	 */
	public void handleInvalid(String text);
}
