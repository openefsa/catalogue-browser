package export_catalogue;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import catalogue.Catalogue;
import open_xml_reader.ExcelThread;

public class ExportCatalogueThread extends ExcelThread {

	private Catalogue catalogue;
	
	/**
	 * Start an export process.
	 * @param catalogue the catalogue we want to export
	 * @param filename the .xlsx file which will host the catalogue data
	 */
	public ExportCatalogueThread( Catalogue catalogue, String filename ) {
		super(filename);
		this.catalogue = catalogue;
	}
	
	@Override
	public void run() {
		
		// prepare the import procedure
		final ExportCatalogueWorkbook exportCat = new ExportCatalogueWorkbook();
		
		if ( getProgressForm() != null )
			exportCat.setProgressBar( getProgressForm() );
		
		// export the catalogue
		try {
			exportCat.exportCatalogue( catalogue, getFilename() );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		Display.getDefault().syncExec( new Runnable() {
			
			@Override
			public void run ( ) {
				
				// finish the operations
				handleDone();
			}
		});
	}

}
