package export_catalogue;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import catalogue.Catalogue;
import messages.Messages;
import naming_convention.Headers;
import ui_progress_bar.FormProgressBar;


/**
 * Export all the database related to the current catalogue in a workbook.
 * @author avonva
 *
 */
public class ExportCatalogueWorkbook {

	private FormProgressBar progressBar;  // progress bar to show the export process to the user
	
	/**
	 * Constructor, we instantiate the progress bar
	 * @param progressBar
	 */
	public ExportCatalogueWorkbook( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Export the catalogue into a workbook formatted as .xlsx
	 * Four sheets are created: catalogue, hierarchy, attribute, term
	 * @param catalogue the catalogue we want to export
	 * @param filename
	 * @throws IOException
	 */
	public void exportCatalogue( Catalogue catalogue, String filename ) throws IOException {
		
		System.out.println ( "Starting export process..." );
		
		// the workbook which will be created with the export
		SXSSFWorkbook workbook = new SXSSFWorkbook( new XSSFWorkbook() , 100 , true , true );
		
		// set that we want to maintain the temp files smaller
		workbook.setCompressTempFiles( true );
		
		// write the catalogue sheet
		ExportCatalogueSheet catSheet = new ExportCatalogueSheet( catalogue, workbook, Headers.CAT_SHEET_NAME );
		catSheet.setProgressBar( progressBar, 1, Messages.getString( "Export.CatalogueSheet" ) );
		catSheet.write();

		// write the hierarchy sheet
		ExportHierarchySheet hierarchySheet = new ExportHierarchySheet( catalogue, workbook, Headers.HIER_SHEET_NAME );
		hierarchySheet.setProgressBar( progressBar, 4, Messages.getString( "Export.HierarchySheet" ) );
		hierarchySheet.write();
		
		// write the attribute sheet
		ExportAttributeSheet attrSheet = new ExportAttributeSheet( catalogue, workbook, Headers.ATTR_SHEET_NAME );
		attrSheet.setProgressBar( progressBar, 5, Messages.getString( "Export.AttributeSheet" ) );
		attrSheet.write();

		// write the term sheet
		ExportTermSheet termSheet = new ExportTermSheet( catalogue, workbook, Headers.TERM_SHEET_NAME );
		termSheet.setProgressBar( progressBar, 80, Messages.getString( "Export.TermSheet" ) );
		termSheet.write();
		
		// write the term sheet
		ExportReleaseNotesSheet noteSheet = new ExportReleaseNotesSheet( 
				catalogue, workbook, Headers.NOTES_SHEET_NAME );
		
		noteSheet.setProgressBar( progressBar, 95, Messages.getString( "Export.NotesSheet" ) );
		noteSheet.write();
		
		// last operation
		progressBar.setLabel( Messages.getString( "Export.WriteSheet" ) );

		// write in the workbook
		OutputStream out = new FileOutputStream( filename );
		workbook.write( out );
		
		// fill progress bar
		progressBar.fillBar();
		
		// close workbook and progress bar
		workbook.close();
		progressBar.close();
		
		System.out.println ( "Export finished" );
	}
}
