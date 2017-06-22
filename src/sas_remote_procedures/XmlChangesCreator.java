package sas_remote_procedures;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import dcf_manager.Dcf.DcfType;
import dcf_webservice.UploadData;
import export_catalogue.ExportActions;
import ui_progress_bar.FormProgressBar;

/**
 * This class is used to compute the differences
 * between the local catalogue and the official
 * version of the catalogue (storing them
 * in a .xml file), in order to be able
 * to update the changes in a second step with an
 * {@link UploadData} action. In particular, the .xml
 * file contains several instructions which can be used to
 * upload the official catalogue directly.
 * @author avonva
 *
 */
public class XmlChangesCreator {

	
	/**
	 * The export format of the exported file as soon
	 * as the export is finished
	 */
	private static final String START_FORMAT = ".start";
	
	/**
	 * The correct export format of the catalogue data
	 */
	private static final String CORRECT_FORMAT = ".xlsx";
	
	/**
	 * The format of the file which will be produced
	 * by the sas procedure
	 */
	private static final String REMOTE_CORRECT_FORMAT = ".xml";
	
	/**
	 * The format of the lock file which will be created
	 * at the end of the process
	 */
	private static final String END_FORMAT = ".end";
	
	/**
	 * The format of the lock file which will be created
	 * at the end of the process
	 */
	private static final String REMOTE_END_FORMAT = ".process.end";
	
	private FormProgressBar progressBar;
	private Listener abortListener;
	private Listener doneListener;

	/**
	 * Ask to the sas server to create the .xml file 
	 * which contains the differences
	 * between {@code catalogue} and its official version
	 * hosted on the dcf. The .xml file will be created on
	 * the remote folder, which is defined in
	 * {@link #CHANGES_CREATOR_PATH}.
	 * @param catalogue
	 */
	public void createXml( final Catalogue catalogue ) {

		System.out.println( "Starting xml creation procedure for " + catalogue );
		
		final String filename = createUniqueCode( catalogue );
		
		// TODO define a conventional filename
		//final String filename = catalogue.getCode() + "_" + catalogue.getVersion();
		final String startFilename = filename + START_FORMAT;
		
		// export the catalogue into the start file
		// and make actions when it has finished
		ExportActions export = new ExportActions();
		export.setProgressBar( progressBar );
		export.exportAsync( catalogue, startFilename, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {

				String basepath = SasRemotePaths.CHANGES_CREATOR_PATH;
				
				// file involved into the process
				File startFile = new File ( startFilename );
				File remoteStartFile = new File ( basepath + startFilename );
				File remoteXlsxFile = new File ( basepath + filename + CORRECT_FORMAT );
				File remoteEndFile = new File ( basepath + filename + END_FORMAT );
				
				// copy the start file into the remote folder
				// where the sas procedure can read the file
				if ( !copy ( startFile, remoteStartFile ) )
					return;
				
				// delete the local .start file since we
				// do not need it anymore
				startFile.delete();

				// change the extension in .xlsx
				if ( !rename ( remoteStartFile, remoteXlsxFile ) )
					return;

				// create the .end file in the remote folder
				if ( !createEndFile( remoteEndFile ) )
					return;
				
				// save the xml filename into the database
				saveXmlFilename ( catalogue, filename );
				
				System.out.println( "Process finished" );
				
				// call the done listener
				if ( doneListener != null ) {
					doneListener.handleEvent( null );
				}
			}
		} );
	}
	
	/**
	 * Download the xml file
	 * @param catalogue
	 * @return the xml file
	 */
	public File downloadXml ( Catalogue catalogue ) {
		
		System.out.println( "Downloading xml" );
		
		// get the xml filename related to this catalogue
		XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
		XmlUpdateFile xmlUpdateFile = xmlDao.getById( catalogue.getId() );
		
		if ( xmlUpdateFile == null ) {
			System.err.println( "Cannot find an xml filename for " + catalogue );
			return null;
		}
		
		System.out.println( "Filename " + xmlUpdateFile.getXmlFilename() );
		
		String endFilename = SasRemotePaths.CHANGES_CREATOR_PATH + 
				xmlUpdateFile.getXmlFilename() + REMOTE_END_FORMAT;
		
		File endFile = new File ( endFilename );
		
		System.out.println( "Searching " + endFilename );
		
		// search for the lock file
		// wait 5 seconds each time
		while ( !endFile.exists() ) {
			
			try {
				Thread.sleep( 5000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println( "File " + endFilename + " found!" );
		
		// here the file exists therefore we can go on
		
		String xmlFilename = SasRemotePaths.CHANGES_CREATOR_PATH + 
				xmlUpdateFile.getXmlFilename() + REMOTE_CORRECT_FORMAT;
		File xmlFile = new File ( xmlFilename );
		
		// copy the file in the local directory
		if ( !copy( xmlFile, new File (".") ) )
			return null;
		
		System.out.println( "Process terminated" );
		
		return xmlFile;
	}

	/**
	 * Create a unique code for the current catalogue, in order
	 * to be able to correctly retrieve the correct xml file
	 * @param catalogue
	 * @return
	 */
	private String createUniqueCode ( Catalogue catalogue ) {
		
		StringBuilder builder = new StringBuilder();
		builder.append( catalogue.getCode() );
		builder.append( "_" );
		builder.append( catalogue.getVersion() );
		builder.append( "_" );
		
		// use today date as encoding to use a unique id
		Date date = Calendar.getInstance().getTime();
	    SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd_hhmmss" );
		String encodedDate = sdf.format( date );
		
		builder.append( encodedDate );
		builder.append( "_" );
		builder.append( "BR" );
		
		return builder.toString();
	}
	
	/**
	 * Copy the source file into the target file
	 * @param source
	 * @param target
	 * @return
	 */
	private boolean copy ( File source, File target ) {
		
		System.out.println( "Copying " + source + " into " + target );
		
		// copy the .start file into the sas folder
		try {
			DatabaseManager.copyFolder( source, target );
		} catch (IOException e) {
			System.err.println( "Cannot copy " + source + " into " + target );
			abort( e.getMessage() );
			return false;
		}
		
		return true;
	}
	
	/**
	 * Rename a file with a new name
	 * @param file
	 * @param newFile
	 * @return
	 */
	private boolean rename ( File file, File newFile ) {
		
		System.out.println( "Renaming " + file + " into " + newFile );
		
		boolean success = file.renameTo( newFile );
		
		if ( !success ) {
			
			String error = "Cannot rename " + file + 
					" in " + newFile + ". Aborting operation...";
			
			System.out.println( error );
			abort( error );
		}
		
		return success;
	}
	
	/**
	 * Create the end file, which is the file that allows
	 * the sas operation to start.
	 * @param endFile
	 * @return
	 */
	private boolean createEndFile ( File endFile ) {

		// create the .end file to start the
		// sas application
		System.out.println( "Creating lock file " + endFile );
		
		try {
			endFile.createNewFile();
		} catch (IOException e) {
			System.err.println( "Cannot create " + endFile );
			abort( e.getMessage() );
			return false;
		}
		
		return true;
	}
	
	/**
	 * Save the just created xml filename in the db, in order to be able
	 * to use it subsequently in the application.
	 * @param catalogue
	 * @param xmlFilename
	 */
	private void saveXmlFilename ( Catalogue catalogue, String xmlFilename ) {
		
		XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
		XmlUpdateFile xml = new XmlUpdateFile( catalogue, xmlFilename );
		
		// remove if present and then insert
		xmlDao.remove( xml );
		xmlDao.insert( new XmlUpdateFile( catalogue, xmlFilename ) );
	}
	
	/**
	 * Abort the process
	 */
	private void abort( String message ) {
		
		if ( progressBar != null )
			progressBar.close();
		
		if ( abortListener != null ) {
			Event event = new Event();
			event.data = message;
			abortListener.handleEvent( event );
		}
	}
	
	/**
	 * Set the progress bar for the export process
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Called if something went wrong during the process
	 * @param abortListener
	 */
	public void setAbortListener(Listener abortListener) {
		this.abortListener = abortListener;
	}
	
	/**
	 * Called if the process terminated correctly
	 * @param doneListener
	 */
	public void setDoneListener(Listener doneListener) {
		this.doneListener = doneListener;
	}
	
	public static void main ( String args[] ) {

		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue cat = catDao.getCatalogue( "ABUNDANCE", "4.5", DcfType.TEST );
		cat.loadData();
		XmlChangesCreator creator = new XmlChangesCreator ();
		creator.createXml( cat );
	}
}
