package kokkodis.factory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PropertiesFactory {

	/**
	 * @param args
	 */

	private static PropertiesFactory propertiesFactory = null;
	private static Properties props=null;

	

	public Properties getProps() {
		if(props==null)
			initializeProperties();
		return props;
	}

	public static PropertiesFactory getInstance() {
		if (propertiesFactory == null){
			propertiesFactory = new PropertiesFactory();
			initializeProperties();
		}

		return propertiesFactory;
	}
	private static void initializeProperties()  {
		if(props ==null)
			{	props = new Properties();
			try {
				props.load(new FileInputStream("config.properties"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			}
		
	}

}
