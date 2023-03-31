package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import utilities.GlobalUtil;

/**
 * Class to read an xml used to store the properties
 * 
 * @author avonva
 * @author shahaal
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
	private static final String APP_HELP_REPOSITORY_PROPERTY = "Application.HelpRepository";

	private static final String NOT_FOUND = "not found";
	
	/**
	 * Read the application properties from the xml file
	 * 
	 * @return
	 */
	public static Properties getProperties(String filename) {

		Properties properties = null;

		try {
			properties = new Properties();

			// fileStream from default properties xml file
			FileInputStream in = new FileInputStream(filename);
			properties.loadFromXML(in);

			in.close();
		} catch (IOException e) {
			LOGGER.error("The default properties file was not found. Please check!", e);
			e.printStackTrace();
		}

		return properties;
	}

	/**
	 * Get the application name from the properties file
	 * 
	 * @return
	 */
	public static String getAppName() {
		return getValue(APP_NAME_PROPERTY);
	}

	/**
	 * Get the version of the application from the properties file
	 * 
	 * @return
	 */
	public static String getAppVersion() {
		return getValue(APP_VERSION_PROPERTY);
	}

	/**
	 * Get the version of the application from the properties file
	 * 
	 * @return
	 */
	public static String getDcfXmlCreatorPath() {
		return getValue(DCF_XML_CREATOR);
	}

	public static String getXmlCreatorInputFolder() {
		return getValue(DCF_XML_CREATOR_IN);
	}

	public static String getXmlCreatorUpdateFolder() {
		return getValue(DCF_XML_CREATOR_UPDATE);
	}

	public static String getXmlCreatorOutputFolder() {
		return getValue(DCF_XML_CREATOR_OUT);
	}

	public static String getHelpRepositoryURL() {
		return getValue(APP_HELP_REPOSITORY_PROPERTY) + "/";
	}

	/**
	 * Get a property value given the key
	 * 
	 * @param property
	 * @return
	 */
	private static String getValue(String property) {

		Properties prop = AppConfig.getProperties(GlobalUtil.CONFIG_FILE);

		if (prop == null)
			return NOT_FOUND;

		return prop.getProperty(property);
	}
}
