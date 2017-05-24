package xml_reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import dcf_manager.Dcf.DcfType;
import utilities.GlobalUtil;

/**
 * Class to read an xml used to store the properties
 * @author avonva
 *
 */
public class PropertiesReader {

	private static final String DCF_PROPERTY = "Dcf.EnableTest";
	private static final String YES = "YES";
	
	/**
	 * Read the application properties from the xml file
	 * @return
	 */
	public static Properties getProperties( String filename ) {
		
		Properties properties = null;

		try {
			properties = new Properties();

			// fileStream from default properties xml file
			FileInputStream in = new FileInputStream( filename );
			properties.loadFromXML( in );

			in.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		
		return properties;
	}

	/**
	 * Get the dcf type from the preference file
	 * @return
	 */
	public static DcfType getDcfType () {
		
		Properties prop = PropertiesReader.getProperties( 
				GlobalUtil.appPropertiesFile );
		
		String value = prop.getProperty( DCF_PROPERTY );
		
		if ( value.equalsIgnoreCase( YES ) )
			return DcfType.TEST;
		else
			return DcfType.PRODUCTION;
	}
}
