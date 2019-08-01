package sas_remote_procedures;

import catalogue.Catalogue;

/**
 * This class models a record of the CAT_UPDATES_XML table
 * @author avonva
 *
 */
public class XmlUpdateFile {
	
	private Catalogue catalogue;
	private String xmlFilename;
	
	public XmlUpdateFile(Catalogue catalogue, String xmlFilename) {
		this.catalogue = catalogue;
		this.xmlFilename = xmlFilename;
	}
	
	public Catalogue getCatalogue() {
		return catalogue;
	}
	
	/**
	 * Get the xml filename, note that this name should be
	 * without extension
	 * @return
	 */
	public String getXmlFilename() {
		return xmlFilename;
	}
}
