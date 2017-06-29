package sas_remote_procedures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import catalogue.Catalogue;
import dcf_pending_action.PendingUploadData;
import utilities.GlobalUtil;

/**
 * This class models a record of the CAT_UPDATES_XML table
 * @author avonva
 *
 */
public class XmlUpdateFile {

	/**
	 * The export format of the exported file as soon
	 * as the export is finished
	 */
	static final String START_FORMAT = ".start";
	
	/**
	 * The correct export format of the catalogue data
	 */
	static final String LOCAL_EXPORT_FORMAT = ".xlsx";
	
	/**
	 * The format of the file which will be produced
	 * by the sas procedure
	 */
	static final String REMOTE_OUT_FORMAT = ".xml";
	
	/**
	 * The format of the lock file which will be created
	 * at the end of the process
	 */
	static final String END_FORMAT = ".end";
	
	/**
	 * The format of the lock file which will be created
	 * when the remote procedure starts processing the
	 * .xlsx file to create the .xml file
	 */
	static final String REMOTE_START_FORMAT = ".process.start";
	
	/**
	 * The format of the file which confirms that the
	 * procedure has terminated the conversion
	 */
	static final String REMOTE_END_FORMAT = ".process.end";
	
	private Catalogue catalogue;
	private String xmlFilename;
	
	public XmlUpdateFile( Catalogue catalogue, String xmlFilename ) {
		this.catalogue = catalogue;
		this.xmlFilename = xmlFilename;
	}
	
	/**
	 * Download the xml file if needed. If the file was already downloaded
	 * the function returns the already downloaded file.
	 * @param interAttemptsTime time between one download attempt and the other
	 * @return the xml file
	 */
	public File downloadXml ( long interAttemptsTime ) throws IOException {
		
		System.out.println( "Downloading xml" );
		
		// get the xml filename related to this catalogue
		XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
		XmlUpdateFile xmlUpdateFile = xmlDao.getById( catalogue.getId() );
		
		if ( xmlUpdateFile == null ) {
			throw new FileNotFoundException( "Xml update filename for " + 
					catalogue + " not found in the main database" );
		}
		
		System.out.println( "Filename found: " + xmlUpdateFile.getXmlFilename() );
		
		String processEndFilename = SasRemotePaths.XML_UPDATES_CREATOR_INPUT_FOLDER + 
				xmlUpdateFile.getXmlFilename() + REMOTE_END_FORMAT;
		
		String remoteXmlFilename = SasRemotePaths.XML_UPDATES_CREATOR_UPDATE_FOLDER + 
				xmlUpdateFile.getXmlFilename() + REMOTE_OUT_FORMAT;
		
		String localXmlFilename = xmlUpdateFile.getXmlFilename() + REMOTE_OUT_FORMAT;
		
		File processEndFile = new File ( processEndFilename );
		File remoteXmlFile = new File ( remoteXmlFilename );
		File localXmlFile = new File ( localXmlFilename );
		
		// if the file was already downloaded simply return it
		if ( localXmlFile.exists() ) {
			System.out.println( "The file " + localXmlFile + " was already downloaded, returning it..." );
			return localXmlFile;
		}
		
		System.out.println( "Searching " + processEndFilename );
		
		// POLLING
		// search for the lock file
		// wait 5 seconds each time
		while ( !processEndFile.exists() ) {
			
			try {
				Thread.sleep( interAttemptsTime );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println( "File " + processEndFilename + " found!" );
		
		// here the file exists therefore we can go on

		// copy the file in the local directory
		GlobalUtil.copyFile( remoteXmlFile, localXmlFile );
		
		// delete the .process.end file from the server
		Files.delete( Paths.get( processEndFilename ) );
		
		System.out.println( "Download xml process finished" );
		
		return localXmlFile;
	}
	
	/**
	 * Delete the xml update file from disk and from db.
	 * This will be called from {@link PendingUploadData#processResponse(dcf_webservice.DcfResponse)}
	 */
	public void delete() {

		// delete file on disk
		try {
			GlobalUtil.deleteFileCascade( xmlFilename + 
					XmlUpdateFile.REMOTE_OUT_FORMAT );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// delete record on db
		XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
		xmlDao.remove( this );
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
	
	/**
	 * Get the xml file related to this object
	 * Note that if the file do not exist null is returned.
	 * @return
	 */
	public File getXmlFile() {
		
		String name = xmlFilename + XmlUpdateFile.REMOTE_OUT_FORMAT;
		
		File file = new File ( name );
		
		if ( !file.exists() )
			return null;
		
		return file;
	}
	
	/**
	 * Update the xml file into the db
	 */
	public void update() {
		XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
		xmlDao.remove( this );
		xmlDao.insert( this );
	}
}
