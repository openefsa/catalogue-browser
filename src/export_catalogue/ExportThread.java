package export_catalogue;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import excel_file_management.ExcelThread;

public class ExportThread extends ExcelThread {

	public ExportThread(String filename) {
		super(filename);
	}
	
	@Override
	public void run() {
		
		// prepare the import procedure
		final ExportCatalogueWorkbook exportCat = new ExportCatalogueWorkbook( getProgressForm() );
		
		// export the catalogue
		try {
			exportCat.exportCatalogue( getFilename() );
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
