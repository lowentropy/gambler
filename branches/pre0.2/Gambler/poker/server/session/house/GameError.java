package poker.server.session.house;


public class GameError extends Exception
{

	private static final long	serialVersionUID	= 3257002146673997365L;

	public GameError(String msg)
	{
		super(msg);
	}
	
	public GameError(String msg, Throwable exc)
	{
		super(msg, exc);
	}
}
