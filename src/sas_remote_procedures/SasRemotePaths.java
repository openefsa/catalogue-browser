package sas_remote_procedures;

import xml_reader.PropertiesReader;

public class SasRemotePaths {

	/**
	 * Path of the sas procedure which converts the exported catalogue
	 * file into an .xml file which contains all its differences (in
	 * terms of dcf operations) with the official catalogue.
	 */
	public static final String CHANGES_CREATOR_PATH = 
			PropertiesReader.getDcfXmlCreatorPath() + 
			System.getProperty("file.separator");
}
