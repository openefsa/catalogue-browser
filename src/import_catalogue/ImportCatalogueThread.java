package import_catalogue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.transform.TransformerException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import folder_zipper.FolderZipper;
import messages.Messages;
import ui_progress_bar.FormProgressBar;
import utilities.GlobalUtil;
import xml_to_excel.XmlCatalogueToExcel;

/**
 * Thread used to import a catalogue from three different formats:
 * .ecf, .xml and .xlsx. Note that the real import process involves
 * only the .xlsx file. If an .ecf or an .xml file are used, they are
 * first converted to an .xlsx file to import it.
 * Import pipeline:
 * .ecf => .xml => .xlsx => import .xlsx
 * @author avonva
 *
 */
public class ImportCatalogueThread extends Thread {

	private String filename;  // path of the file
	private String path;  // path where the db will be created
	private ImportFileFormat format;  // the format of the file

	private Catalogue openedCat;
	
	// called when import is finished
	private Listener doneListener;
	
	// progress bar used to notify the user
	private FormProgressBar progressBar;
	
	// list of temporary files which need to
	// be deleted at the end of the process
	private ArrayList<String> garbage;
	
	/**
	 * Enumerator to specify the format
	 * of the file we want to import
	 * into the catalogue database
	 * @author avonva
	 *
	 */
	public enum ImportFileFormat {
		ECF,
		XML,
		XLSX;
	}
	
	/**
	 * Initialize the import thread
	 * @param path the path where the new database should be created
	 * @param filename path of the file we want to import
	 * @param format in which format is the file that we want to import
	 */
	public ImportCatalogueThread( String path, String filename, 
			ImportFileFormat format ) {

		this.filename = filename;
		this.path = path;
		this.format = format;
		this.garbage = new ArrayList<>();
	}
	
	/**
	 * Run the import thread
	 */
	public void run () {

		switch ( format ) {
		case ECF:
			importEcf( filename );
			break;

		case XML:
			importXml( filename );
			break;
			
		case XLSX:
			importXlsx( filename );
			break;

		default:
			break;
		}
	}

	/**
	 * Process an ecf file and extract the xml catalogue
	 * contained in it.
	 * @param filename
	 * @return the created xml file
	 */
	private String processEcf ( String filename ) {

		addProgress(1);
		setProgressLabel( Messages.getString( "ImportEcf.Processing" ) );
		
		String outputFile = null;
		
		try {

			String outputFolder = filename + "_unzip";

			// unzip the ecf file into the xml
			FolderZipper.extractFolder( filename, outputFolder );

			final File unzippedFolder = new File( outputFolder );

			if ( unzippedFolder.listFiles().length <= 0 ) {
				System.err.println ( "Wrong file format, "
						+ "cannot find the xml file inside the .ecf" );
				return null;
			}

			// add the unzipped folder to the garbage to
			// delete it at the end of the process
			garbage.add( unzippedFolder.getAbsolutePath() );
			
			// get the xml file from the folder
			File xmlFile = unzippedFolder.listFiles()[0];

			outputFile = xmlFile.getAbsolutePath();
			
			return outputFile;

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Process an .xml file to create a .xlsx catalogue file
	 * @param filename xml filename
	 * @return the created xlsx file
	 */
	private String processXml ( String filename ) {

		addProgress(5);
		setProgressLabel( Messages.getString( "ImportXml.Processing" ) );

		try {

			String outputFilename = filename + ".xlsx";

			// convert the xml into an excel
			XmlCatalogueToExcel converter = 
					new XmlCatalogueToExcel( filename, outputFilename );
			
			// do the conversion
			converter.convertXmlToExcel();
			
			addProgress( 10 );
			setProgressLabel( Messages.getString( "ImportXml.Importing" ) );

			return outputFilename;

		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	
	/**
	 * Import an .ecf catalogue
	 * @param filename the absolute path of the .ecf file
	 */
	private void importEcf( String filename ) {
		
		String xmlFile = processEcf( filename );
		
		// at the end of the process delete the
		// .xml temporary file
		garbage.add( xmlFile );
		
		// import the .xml file
		importXml( xmlFile );
	}
	
	/**
	 * Import a .xml catalogue
	 * @param filename the absolute path of the .xml catalogue
	 */
	private void importXml( String filename ) {

		String xlsxFile = processXml( filename );
		
		// at the end of the process delete the
		// temporary xlsx file
		garbage.add( xlsxFile );
		
		// import the xlsx catalogue
		importXlsx( xlsxFile );
	}
	
	/**
	 * Import a .xlsx catalogue
	 * @param filename the absolute path of the .xlsx catalogue
	 */
	private void importXlsx( final String filename ) {
		
		try {

			// instantiate the workbook importer and set
			// some settings
			CatalogueWorkbookImporter importer = new CatalogueWorkbookImporter();
			importer.setProgressBar( progressBar );

			if ( openedCat != null )
				importer.setOpenedCatalogue( openedCat );
			
			// import the catalogue contained in the
			// xlsx file into the specified path (db path)
			importer.importWorkbook( path, filename );

		} catch ( final Exception e ) {
			e.printStackTrace();
		}
		
		// end the import process
		endProcess();
	}
	
	/**
	 * End the import process
	 */
	private void endProcess() {

		// delete all the temporary files
		deleteGarbage();
		
		// end the import process
		Display.getDefault().syncExec( new Runnable() {
			
			@Override
			public void run ( ) {

				// call the UI listener
				handleDone();
			}
		});
	}
	

	/**
	 * Delete all the file in the garbage
	 * @throws IOException
	 */
	private void deleteGarbage() {
		for ( String filename : garbage ) {
			try {
				GlobalUtil.deleteFileCascade( new File( filename ) );
			} catch (IOException e) {}
		}
	}

	/**
	 * Call the done listener if it was set
	 * Pass as data the xlsx filename
	 */
	private void handleDone() {

		if ( doneListener != null ) {
			Event event = new Event();
			event.data = filename;
			doneListener.handleEvent( event );
		}
	}
	
	/**
	 * Called when all the operations are finished
	 * @param doneListener
	 */
	public void addDoneListener ( Listener doneListener ) {
		this.doneListener = doneListener;
	}
	
	/**
	 * Set the progress bar for the thread
	 * @param progressForm
	 */
	public void setProgressBar( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Set the current progress of the progress bar
	 * @param progress
	 */
	private void addProgress ( int progress ) {

		if ( progressBar != null ) {
			progressBar.addProgress( progress );
		}
	}
	
	/**
	 * Set the progress bar label
	 * @param label
	 */
	private void setProgressLabel ( String label ) {
		
		if ( progressBar != null ) {
			progressBar.setLabel( label );
		}
	}
	
	/**
	 * If we are importing a workbook into an opened catalogue
	 * we need to specify which is the catalogue, otherwise
	 * we will get errors in the import process due to the wrong
	 * db path of the catalogue (which is determined by the
	 * catalogue code + version)
	 * @param localCat
	 */
	public void setOpenedCatalogue( Catalogue openedCat ) {
		this.openedCat = openedCat;
	}
}
