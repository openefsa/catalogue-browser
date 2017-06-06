package import_catalogue;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import catalogue.Catalogue;
import excel_file_management.ExcelThread;
import utilities.GlobalUtil;

/**
 * Thread used to import an .xlsx file which contains
 * catalogue data into the derby database.
 * @author avonva
 *
 */
public class ImportThread extends ExcelThread {

	private String path;  // path where the db will be created
	private boolean deleteFiles = false;
	private Catalogue localCat;
	private boolean local = false;
	
	/**
	 * Initialize the import thread
	 * @param path the path where the new database should be created
	 * @param filename the name of the .xlsx file we want to import
	 */
	public ImportThread( String path, String filename ) {
		super( filename );
		this.path = path;
	}
	
	/**
	 * Set if we want to delete the excel file at 
	 * the end of the import process
	 */
	public void deleteFilesAtEnd() {
		deleteFiles = true;
	}
	
	/**
	 * Set to true if we want to import an
	 * excel file for a local catalogue. Pass
	 * also the local catalogue
	 * @param local
	 */
	public void setLocal( Catalogue localCat, boolean local ) {
		this.localCat = localCat;
		this.local = local;
	}

	/**
	 * Run the import thread
	 */
	public void run () {
		
		try {

			// prepare the import procedure
			final ImportExcelCatalogue importCat = new ImportExcelCatalogue();
			importCat.setProgressBar( getProgressForm() );

			// start import, return false if wrong catalogue
			final boolean check;

			if ( !local ) 
				check = importCat.importCatalogue( path, getFilename() );
			else
				check = importCat.importLocalCatalogue( localCat, path, getFilename() );

			Display.getDefault().syncExec( new Runnable() {
				
				@Override
				public void run ( ) {

					// if wrong catalogue => error
					if ( !check ) {
						handleError();
						return;
					}

					System.out.println( "Import completed" );
					
					// delete files if necessary
					if ( deleteFiles ) {
						try {
							GlobalUtil.deleteFileCascade( getFilename() );
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					// finish the operations
					handleDone();
				}
			});

		} catch ( final Exception e ) {
			e.printStackTrace();
		}
	}
}
