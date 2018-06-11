package export_catalogue;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import catalogue.Catalogue;
import messages.Messages;
import naming_convention.Headers;
import progress_bar.IProgressBar;


/**
 * Export all the database related to the current catalogue in a workbook.
 * @author avonva
 *
 */
public class ExportCatalogueWorkbook {

	private static final Logger LOGGER = LogManager.getLogger(ExportCatalogueWorkbook.class);
	
	private IProgressBar progressBar;  // progress bar to show the export process to the user
	
	/**
	 * Set the progress bar if needed
	 * @param progressBar
	 */
	public void setProgressBar(IProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Export the catalogue into a workbook formatted as .xlsx
	 * Four sheets are created: catalogue, hierarchy, attribute, term
	 * @param catalogue the catalogue we want to export
	 * @param filename
	 * @param flag 
	 * @throws IOException
	 */
	public void exportCatalogue( Catalogue catalogue, String filename, Boolean flag ) throws IOException {
		
		long startTime = System.currentTimeMillis();
		
		LOGGER.info ( "Starting export process..." );
		
		// the workbook which will be created with the export
		SXSSFWorkbook workbook = new SXSSFWorkbook();

		// set that we want to maintain the temp files smaller
		workbook.setCompressTempFiles( true );

		//if exporting the info
		if (flag) {
			
			LOGGER.info("Exporting catalogue" + catalogue);
			
			// write the catalogue sheet
			ExportCatalogueSheet catSheet = new ExportCatalogueSheet( catalogue, 
					workbook, Headers.CAT_SHEET_NAME );
			
			if ( progressBar != null )
				catSheet.setProgressBar( progressBar, 1, 
						Messages.getString( "Export.CatalogueSheet" ) );
			
			catSheet.write();
		
			LOGGER.info( "Exporting hierarchies" );
			
			// write the hierarchy sheet
			ExportHierarchySheet hierarchySheet = new ExportHierarchySheet( catalogue, 
					workbook, Headers.HIER_SHEET_NAME );
			
			if ( progressBar != null )
				hierarchySheet.setProgressBar( progressBar, 4, 
						Messages.getString( "Export.HierarchySheet" ) );
			
			hierarchySheet.write();
			
			LOGGER.info ( "Exporting attributes" );
			
			// write the attribute sheet
			ExportAttributeSheet attrSheet = new ExportAttributeSheet( catalogue, 
					workbook, Headers.ATTR_SHEET_NAME, true );
			
			if ( progressBar != null )
				attrSheet.setProgressBar( progressBar, 5, 
						Messages.getString( "Export.AttributeSheet" ) );
			
			attrSheet.write();
			
			LOGGER.info ( "Exporting terms" );

			// write the term sheet
			ExportTermSheet termSheet = new ExportTermSheet( catalogue, 
					workbook, Headers.TERM_SHEET_NAME, true );
			
			if ( progressBar != null )
				termSheet.setProgressBar( progressBar, 80, 
						Messages.getString( "Export.TermSheet" ) );
			
			termSheet.write();
			
			LOGGER.info ( "Exporting release notes" );
			
			// write the term sheet
			ExportReleaseNotesSheet noteSheet = new ExportReleaseNotesSheet( 
					catalogue, workbook, Headers.NOTES_SHEET_NAME );
			
			if ( progressBar != null )
				noteSheet.setProgressBar( progressBar, 95, 
						Messages.getString( "Export.NotesSheet" ) );
			
			noteSheet.write();
			
		}else {
			
			//if needed just the interpreting and checking tool info
			LOGGER.info ( "Exporting attributes" );
			
			// write the attribute sheet
			ExportAttributeSheet attrSheet = new ExportAttributeSheet( catalogue, 
					workbook, Headers.ATTR_SHEET_NAME, false );
			
			if ( progressBar != null )
				attrSheet.setProgressBar( progressBar, 5, 
						Messages.getString( "Export.AttributeSheet" ) );
			
			attrSheet.write();
			
			LOGGER.info ( "Exporting terms" );

			// write the term sheet
			ExportTermSheet termSheet = new ExportTermSheet( catalogue, 
					workbook, Headers.TERM_SHEET_NAME, false);
			
			if ( progressBar != null )
				termSheet.setProgressBar( progressBar, 80, 
						Messages.getString( "Export.TermSheet" ) );
			
			termSheet.write();
		}
	
		
		// last operation
		if ( progressBar != null )
			progressBar.setLabel( Messages.getString( "Export.WriteSheet" ) );

		LOGGER.info ( "Creating excel file" );
		
		// write in the workbook
		OutputStream out = new FileOutputStream( filename );
		workbook.write( out );
		
		// close workbook and progress bar
		workbook.close();
		
		out.close();

		// fill progress bar
		if ( progressBar != null )
			progressBar.fillToMax();

		if ( progressBar != null )
			progressBar.close();

		LOGGER.info( "Export finished, statistics: overall time = " 
				+ (System.currentTimeMillis()-startTime)/1000.00 + " seconds" );
	}
	
	/*public static void main(String[] args) throws IOException {
		
		ExportCatalogueWorkbook export = new ExportCatalogueWorkbook();
		CatalogueDAO dao = new CatalogueDAO();
		Catalogue landuse = dao.getLastVersionByCode("LANDUSE", DcfType.LOCAL);
		landuse.loadData();
		export.exportCatalogue(landuse, "landuse_export.xlsx");
	}*/
}
