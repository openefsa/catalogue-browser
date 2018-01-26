package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import utilities.GlobalUtil;

/**
 * Class to read an xml used to store the properties
 * @author avonva
 *
 */
public class AppConfig {

	private static final Logger LOGGER = LogManager.getLogger(AppConfig.class);
	
	private static final String APP_NAME_PROPERTY = "Application.Name";
	private static final String APP_VERSION_PROPERTY = "Application.Version";
	private static final String DCF_XML_CREATOR = "DcfXmlUpdatesCreator.RemotePath";
	private static final String DCF_XML_CREATOR_IN = "DcfXmlUpdatesCreator.InputFolder";
	private static final String DCF_XML_CREATOR_UPDATE = "DcfXmlUpdatesCreator.UpdateFolder";
	private static final String DCF_XML_CREATOR_OUT = "DcfXmlUpdatesCreator.OutputFolder";
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
			LOGGER.error( "The default properties file was not found. Please check!", e );
		}
		
		return properties;
	}
	
	/**
	 * Get the application name from the properties file
	 * @return
	 */
	public static String getAppName() {
		return getValue ( APP_NAME_PROPERTY, "not found" );
	}
	
	/**
	 * Get the version of the application from the 
	 * properties file
	 * @return
	 */
	public static String getAppVersion() {
		return getValue( APP_VERSION_PROPERTY, "not found" );
	}
	
	/**
	 * Get the version of the application from the 
	 * properties file
	 * @return
	 */
	public static String getDcfXmlCreatorPath() {
		return getValue( DCF_XML_CREATOR, "not found" );
	}
	

	public static String getXmlCreatorInputFolder() {
		return getValue( DCF_XML_CREATOR_IN, "not found" );
	}
	
	public static String getXmlCreatorUpdateFolder() {
		return getValue( DCF_XML_CREATOR_UPDATE, "not found" );
	}
	
	public static String getXmlCreatorOutputFolder() {
		return getValue( DCF_XML_CREATOR_OUT, "not found" );
	}
	
	/**
	 * Get a property value given the key
	 * @param property
	 * @return
	 */
	private static String getValue ( String property, String defaultValue ) {
		
		Properties prop = AppConfig.getProperties( 
				GlobalUtil.CONFIG_FILE );
		
		if ( prop == null )
			return defaultValue;
		
		return prop.getProperty( property );
	}
}
