
package poker.server.session.model.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class DataModel
{

	private Map<String, ScreenData>	screens;


	public DataModel()
	{
		screens = new HashMap<String, ScreenData>();
	}


	public void addScreenData(String name, ScreenData data)
	{
		screens.put(name, data);
	}


	public ScreenData getScreenData(String name)
	{
		return screens.get(name);
	}


	public Collection<ScreenData> getScreens()
	{
		return screens.values();
	}

}
