package export_catalogue;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Workbook;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_object.Mappable;
import naming_convention.Headers;
import sheet_header.SheetHeader;

/**
 * Export the current catalogue attributes into the attribute sheet
 * @author avonva
 *
 */
public class ExportAttributeSheet extends SheetWriter {

	private Catalogue catalogue;
	private boolean flag;
	
	public ExportAttributeSheet( Catalogue catalogue, Workbook workbook, String sheetName, boolean b) {
		
		super(workbook, sheetName);
		this.catalogue = catalogue;
		this.flag=b;
	}

	@Override
	public HashMap<String, SheetHeader> getHeaders() {
		
		HashMap<String, SheetHeader> headers = new HashMap<>();
		
		//if full attributes needed
		if(flag) {
			
			headers.put( "ATTR_CODE",              new SheetHeader(0, Headers.CODE ) );
			headers.put( "ATTR_NAME",              new SheetHeader(1, Headers.NAME ) );
			headers.put( "ATTR_LABEL",             new SheetHeader(2, Headers.LABEL ) );
			headers.put( "ATTR_SCOPENOTE",         new SheetHeader(3, Headers.SCOPENOTE ) );
			headers.put( "ATTR_REPORTABLE",        new SheetHeader(4, Headers.ATTR_REPORT ) );
			headers.put( "ATTR_VISIBLE",           new SheetHeader(5, Headers.ATTR_VISIB ) );
			headers.put( "ATTR_SEARCHABLE",        new SheetHeader(6, Headers.ATTR_SEARCH ) );
			headers.put( "ATTR_ORDER",             new SheetHeader(7, Headers.ATTR_ORDER ) );
			headers.put( "ATTR_TYPE",              new SheetHeader(8, Headers.ATTR_TYPE ) );
			headers.put( "ATTR_MAX_LENGTH",        new SheetHeader(9, Headers.ATTR_MAX_LENGTH ) );
			headers.put( "ATTR_PRECISION",         new SheetHeader(10, Headers.ATTR_PRECISION ) );
			headers.put( "ATTR_SCALE",             new SheetHeader(11, Headers.ATTR_SCALE ) );
			headers.put( "ATTR_CAT_CODE",          new SheetHeader(12, Headers.ATTR_CAT_CODE ) );
			headers.put( "ATTR_SINGLE_REPEATABLE", new SheetHeader(13, Headers.ATTR_SR ) );
			headers.put( "ATTR_INHERITANCE",       new SheetHeader(14, Headers.ATTR_INHERIT ) );
			headers.put( "ATTR_UNIQUENESS",        new SheetHeader(15, Headers.ATTR_UNIQUE ) );
			headers.put( "ATTR_TERM_CODE_ALIAS",   new SheetHeader(16, Headers.ATTR_ALIAS ) );
			headers.put( "ATTR_VERSION",           new SheetHeader(17, Headers.VERSION ) );
			headers.put( "ATTR_LAST_UPDATE",       new SheetHeader(18, Headers.LAST_UPDATE ) );
			headers.put( "ATTR_VALID_FROM",        new SheetHeader(19, Headers.VALID_FROM ) );
			headers.put( "ATTR_VALID_TO",          new SheetHeader(20, Headers.VALID_TO ) );
			headers.put( "ATTR_STATUS",            new SheetHeader(21, Headers.STATUS ) );
			headers.put( "ATTR_DEPRECATED",        new SheetHeader(22, Headers.DEPRECATED ) );
			
		} else {//if IECT
			
			headers.put( "ATTR_CODE",              new SheetHeader(0, Headers.CODE ) );
			headers.put( "ATTR_NAME",              new SheetHeader(1, Headers.NAME ) );
			headers.put( "ATTR_LABEL",             new SheetHeader(2, Headers.LABEL ) );
			
		}
		
		return headers;
	}

	@Override
	public ArrayList<? extends Mappable> getData() {

		// initialize dao of attributes
		AttributeDAO attrDao = new AttributeDAO( catalogue );
		
		return attrDao.getAll();
	}

}
