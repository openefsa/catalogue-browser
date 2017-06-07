package import_catalogue;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import folder_zipper.FolderZipper;
import messages.Messages;
import ui_progress_bar.FormProgressBar;
import utilities.GlobalUtil;
import xml_to_excel.XmlCatalogueToExcel;

/**
 * Class which manages all the import actions related to catalogues.
 * We can import the catalogues data from three different formats,
 * that are: .xml, .xlsx, .ecf files.
 * @author avonva
 *
 */
public class ImportActions {
	
	private FormProgressBar progressBar;
	private Catalogue localCat;
	
	/**
	 * Add a progress bar to the import process
	 * @param progressBar
	 */
	public void setProgressBar ( FormProgressBar progressBar ) {
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
	 * If we are importing a workbook into a local catalogue
	 * we need to specify which is the local catalogue, otherwise
	 * we will get errors in the import process due to the wrong
	 * catalogue code, which is defined by the user for local
	 * catalogues
	 * @param localCat
	 */
	public void setLocal( Catalogue localCat ) {
		this.localCat = localCat;
	}
	
	/**
	 * Import a catalogue starting from an xlsx format. 
	 * The process starts the {@link ImportThread} thread
	 * to read the xlsx file and insert the data into the
	 * database tables.
	 * @param dbPath the db path where the db will 
	 * be created. Set it to null to use the default,
	 * which is the path created using the catalogue
	 * code and version (CAT_CODE_VERSION) and the 
	 * official catalogues folder.
	 * @param filename the name of the xlsx file which 
	 * we want to import
	 * @param deleteInputFile true if the xlsx file should be 
	 * deleted at the end of the process
	 * @param doneListener listener called when the 
	 * import is finished
	 */
	public void importXlsx ( String dbPath, final String filename, 
			final boolean deleteInputFile, final Listener doneListener ) {

		// create a thread for the excel import
		ImportThread importThread = new ImportThread( dbPath, filename );

		importThread.setLocal( localCat );
		
		// set the progress bar if needed
		if ( progressBar != null )
			importThread.setProgressBar( progressBar );

		importThread.addDoneListener( new Listener() {

			@Override
			public void handleEvent(Event arg0) {

				if ( doneListener != null )
					doneListener.handleEvent(arg0);
				
				if ( deleteInputFile ) {
					new File ( filename ).delete();
				}
			}
		} );
		
		// start the thread
		importThread.start();
	}

	/**
	 * Import a catalogue starting from an xml format. 
	 * The process converts the xml into an excel file 
	 * and then uses {@link ImportActions#importXlsx(String, String, boolean, Listener)} to
	 * import the xlsx into the database.
	 * @param dbPath the db path where the db will 
	 * be created. Set it to null to use the default,
	 * which is the path created using the catalogue
	 * code and version (CAT_CODE_VERSION) and the 
	 * official catalogues folder.
	 * @param filename the name of the xml file which 
	 * we want to import
	 * @param deleteInputFile true if the xml file should be 
	 * deleted at the end of the process
	 * @param doneListener listener called when the 
	 * import is finished
	 */
	public void importXml ( String dbPath, final String filename, 
			final boolean deleteInputFile, final Listener doneListener ) {

		addProgress( 5 );
		setProgressLabel( Messages.getString( "ImportXml.Processing" ) );
		
		String outputFilename = filename + ".xlsx";

		// convert the xml into an excel
		XmlCatalogueToExcel converter = new XmlCatalogueToExcel( filename, outputFilename );

		try {

			// do the conversion
			converter.convertXmlToExcel();
			
			addProgress( 10 );
			setProgressLabel( Messages.getString( "ImportXml.Importing" ) );

			// import the catalogue from excel
			importXlsx( dbPath, outputFilename, deleteInputFile, new Listener() {
				
				@Override
				public void handleEvent(Event arg0) {
					
					if ( doneListener != null )
						doneListener.handleEvent( arg0 );

					// delete input file if required
					if ( deleteInputFile ) {
						new File ( filename ).delete();
					}
				}
			} );

		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Import a catalogue starting from .ecf format. 
	 * The process extracts the ecf into an xml and imports it 
	 * using the
	 * {@linkplain ImportActions#importXml(String, String, boolean, Listener)}. 
	 * procedure.
	 * @param dbPath the db path where the db will 
	 * be created. Set it to null to use the default,
	 * which is the path created using the catalogue
	 * code and version (CAT_CODE_VERSION) and the 
	 * official catalogues folder.
	 * @param filename the name of the ecf file which 
	 * we want to import
	 * @param deleteInputFile true if the generated xml file should be 
	 * deleted at the end of the process
	 * @param doneListener listener called when the 
	 * import is finished
	 */
	public void importEcf ( String dbPath, String filename, 
			boolean deleteInputFile, final Listener doneListener ) {
		
		addProgress( 1 );
		setProgressLabel( Messages.getString( "ImportEcf.Processing" ) );
		
		try {

			String outputFolder = filename + "_unzip";

			// unzip the ecf file into the xml
			FolderZipper.extractFolder( filename, outputFolder );

			final File unzippedFolder = new File( outputFolder );

			if ( unzippedFolder.listFiles().length <= 0 ) {
				System.err.println ( "Wrong file format, cannot find the xml file inside the .ecf" );
				return;
			}

			// get the xml file from the folder
			File xmlFile = unzippedFolder.listFiles()[0];

			// import the catalogue from the xml contained in the zip
			importXml( dbPath, xmlFile.getAbsolutePath(), deleteInputFile, new Listener() {

				// when the import is finished we refresh the ram data
				// and the user interface
				@Override
				public void handleEvent(Event event) {
					
					if ( doneListener != null )
						doneListener.handleEvent( event );
					
					// delete the zip folder and all the files which are inside it
					try {
						GlobalUtil.deleteFileCascade( unzippedFolder );
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} );

		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
