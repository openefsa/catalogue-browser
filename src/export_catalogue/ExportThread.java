package export_catalogue;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import catalogue.Catalogue;
import excel_file_management.ExcelThread;

public class ExportThread extends ExcelThread {

	private Catalogue catalogue;
	
	/**
	 * Start an export process.
	 * @param catalogue the catalogue we want to export
	 * @param filename the .xlsx file which will host the catalogue data
	 */
	public ExportThread( Catalogue catalogue, String filename ) {
		super(filename);
		this.catalogue = catalogue;
	}
	
	@Override
	public void run() {
		
		// prepare the import procedure
		final ExportCatalogueWorkbook exportCat = new ExportCatalogueWorkbook( getProgressForm() );
		
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
