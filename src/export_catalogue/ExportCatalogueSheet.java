package export_catalogue;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import catalogue_object.Catalogue;
import catalogue_object.Mappable;
import global_manager.GlobalManager;
import sheet_converter.SheetHeader;

/**
 * Class to export the current catalogue metadata into a catalogue worksheet
 * @author avonva
 *
 */
public class ExportCatalogueSheet extends SheetWriter {

	/**
	 * Create a sheet
	 * @param workbook
	 * @param sheetName
	 */
	public ExportCatalogueSheet( SXSSFWorkbook workbook, String sheetName ) {
		super ( workbook, sheetName );
	}

	/**
	 * Get the catalogue headers
	 */
	@Override
	public HashMap<String, SheetHeader> getHeaders() {

		HashMap<String, SheetHeader> headers = new HashMap<>();

		headers.put( "CAT_CODE",                      new SheetHeader(0, "code" ) );
		headers.put( "CAT_NAME",                      new SheetHeader(1, "name" ) );
		headers.put( "CAT_LABEL",                     new SheetHeader(2, "label" ) );
		headers.put( "CAT_SCOPENOTE",                 new SheetHeader(3, "scopeNote" ) );
		headers.put( "CAT_TERM_CODE_MASK",            new SheetHeader(4, "termCodeMask" ) );
		headers.put( "CAT_TERM_CODE_LENGTH",          new SheetHeader(5, "termCodeLength" ) );
		headers.put( "CAT_TERM_MIN_CODE",             new SheetHeader(6, "termMinCode" ) );
		headers.put( "CAT_ACCEPT_NON_STANDARD_CODES", new SheetHeader(7, "acceptNonStandardCodes" ) );
		headers.put( "CAT_GENERATE_MISSING_CODES",    new SheetHeader(8, "generateMissingCodes" ) );
		headers.put( "CAT_VERSION",                   new SheetHeader(9, "version" ) );
		headers.put( "CAT_GROUPS",                    new SheetHeader(10, "catalogueGroups" ) );
		headers.put( "CAT_LAST_UPDATE",               new SheetHeader(11, "lastUpdate" ) );
		headers.put( "CAT_VALID_FROM",                new SheetHeader(12, "validFrom" ) );
		headers.put( "CAT_VALID_TO",                  new SheetHeader(13, "validTo" ) );
		headers.put( "CAT_STATUS",                    new SheetHeader(14, "status" ) );
		headers.put( "CAT_DEPRECATED",                new SheetHeader(15, "deprecated" ) );

		return headers;
	}
	
	@Override
	public ArrayList< ? extends Mappable > getData() {
		
		// set the data to be inserted for the current sheet
		ArrayList<Mappable> data = new ArrayList<>();
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		data.add( currentCat );
		
		return data;
	}
}
