package sas_remote_procedures;

import xml_reader.PropertiesReader;

public class SasRemotePaths {

	/**
	 * Path of the sas procedure which converts the exported catalogue
	 * file into an .xml file which contains all its differences (in
	 * terms of dcf operations) with the official catalogue.
	 */
	public static final String XML_UPDATES_CREATOR_PATH = 
			PropertiesReader.getDcfXmlCreatorPath() + 
			System.getProperty("file.separator");
	
	/**
	 * Server folder where the input file should be submitted
	 */
	public static final String XML_UPDATES_CREATOR_INPUT_FOLDER = 
			XML_UPDATES_CREATOR_PATH + PropertiesReader.getXmlCreatorInputFolder() + 
			System.getProperty("file.separator");;
	
	/**
	 * Server folder where the .xml updates files are created
	 */
	public static final String XML_UPDATES_CREATOR_UPDATE_FOLDER = 
			XML_UPDATES_CREATOR_PATH + PropertiesReader.getXmlCreatorUpdateFolder() + 
			System.getProperty("file.separator");;
	
	/**
	 * Server folder where the processed .xlsx files are moved
	 */
	public static final String XML_UPDATES_CREATOR_OUTPUT_FOLDER = 
			XML_UPDATES_CREATOR_PATH + PropertiesReader.getXmlCreatorOutputFolder() + 
			System.getProperty("file.separator");;
}
