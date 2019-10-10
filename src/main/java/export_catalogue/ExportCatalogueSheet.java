package export_catalogue;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import catalogue.Catalogue;
import catalogue_object.Mappable;
import naming_convention.Headers;
import sheet_header.SheetHeader;

/**
 * Class to export the current catalogue metadata into a catalogue worksheet
 * @author avonva
 *
 */
public class ExportCatalogueSheet extends SheetWriter {

	private Catalogue catalogue;
	
	/**
	 * Create a sheet
	 * @param catalogue the catalogue meta data we want to export
	 * @param workbook
	 * @param sheetName
	 */
	public ExportCatalogueSheet( Catalogue catalogue, 
			SXSSFWorkbook workbook, String sheetName ) {
		
		super ( workbook, sheetName );
		
		this.catalogue = catalogue;
	}

	/**
	 * Get the catalogue headers
	 */
	@Override
	public HashMap<String, SheetHeader> getHeaders() {

		HashMap<String, SheetHeader> headers = new HashMap<>();

		headers.put( "CAT_CODE",                      new SheetHeader(0, Headers.CODE ) );
		headers.put( "CAT_NAME",                      new SheetHeader(1, Headers.NAME ) );
		headers.put( "CAT_LABEL",                     new SheetHeader(2, Headers.LABEL ) );
		headers.put( "CAT_SCOPENOTE",                 new SheetHeader(3, Headers.SCOPENOTE ) );
		headers.put( "CAT_TERM_CODE_MASK",            new SheetHeader(4, Headers.CAT_CODE_MASK ) );
		headers.put( "CAT_TERM_CODE_LENGTH",          new SheetHeader(5, Headers.CAT_CODE_LENGTH ) );
		headers.put( "CAT_TERM_MIN_CODE",             new SheetHeader(6, Headers.CAT_MIN_CODE ) );
		headers.put( "CAT_ACCEPT_NON_STANDARD_CODES", new SheetHeader(7, Headers.CAT_ACCEPT_NOT_STD ) );
		headers.put( "CAT_GENERATE_MISSING_CODES",    new SheetHeader(8, Headers.CAT_GEN_MISSING ) );
		headers.put( "CAT_VERSION",                   new SheetHeader(9, Headers.VERSION ) );
		headers.put( "CAT_GROUPS",                    new SheetHeader(10, Headers.CAT_GROUPS ) );
		headers.put( "CAT_LAST_UPDATE",               new SheetHeader(11, Headers.LAST_UPDATE ) );
		headers.put( "CAT_VALID_FROM",                new SheetHeader(12, Headers.VALID_FROM ) );
		headers.put( "CAT_VALID_TO",                  new SheetHeader(13, Headers.VALID_TO ) );
		headers.put( "CAT_STATUS",                    new SheetHeader(14, Headers.STATUS ) );
		headers.put( "CAT_DEPRECATED",                new SheetHeader(15, Headers.DEPRECATED ) );
		headers.put( "CAT_RN_DESCRIPTION",            new SheetHeader(16, Headers.NOTES_DESCRIPTION ) );
		headers.put( "CAT_RN_VERSION_DATE",           new SheetHeader(17, Headers.NOTES_DATE ) );
		headers.put( "CAT_RN_INTERNAL_VERSION",       new SheetHeader(18, Headers.NOTES_VERSION ) );
		headers.put( "CAT_RN_INTERNAL_VERSION_NOTE",  new SheetHeader(19, Headers.NOTES_NOTE ) );
		return headers;
	}
	
	@Override
	public ArrayList< ? extends Mappable > getData() {
		
		// set the data to be inserted for the current sheet
		ArrayList<Mappable> data = new ArrayList<>();
		data.add( catalogue );
		
		return data;
	}
}
