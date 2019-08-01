package export_catalogue;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Workbook;

import catalogue.Catalogue;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Mappable;
import naming_convention.Headers;
import sheet_header.SheetHeader;

public class ExportHierarchySheet extends SheetWriter {

	private Catalogue catalogue;
	
	public ExportHierarchySheet( Catalogue catalogue, 
			Workbook workbook, String sheetName) {
		super(workbook, sheetName);
		this.catalogue = catalogue;
	}

	@Override
	public HashMap<String, SheetHeader> getHeaders() {
		
		HashMap<String, SheetHeader> headers = new HashMap<>();

		headers.put( "HIERARCHY_CODE",          new SheetHeader(0, Headers.CODE ) );
		headers.put( "HIERARCHY_NAME",          new SheetHeader(1, Headers.NAME ) );
		headers.put( "HIERARCHY_LABEL",         new SheetHeader(2, Headers.LABEL ) );
		headers.put( "HIERARCHY_SCOPENOTE",     new SheetHeader(3, Headers.SCOPENOTE ) );
		headers.put( "HIERARCHY_APPLICABILITY", new SheetHeader(4, Headers.HIER_APPL ) );
		headers.put( "HIERARCHY_ORDER",         new SheetHeader(5, Headers.HIER_ORDER ) );
		headers.put( "HIERARCHY_VERSION",       new SheetHeader(6, Headers.VERSION ) );
		headers.put( "HIERARCHY_LAST_UPDATE",   new SheetHeader(7, Headers.LAST_UPDATE ) );
		headers.put( "HIERARCHY_VALID_FROM",    new SheetHeader(8, Headers.VALID_FROM ) );
		headers.put( "HIERARCHY_VALID_TO",      new SheetHeader(9, Headers.VALID_TO ) );
		headers.put( "HIERARCHY_STATUS",        new SheetHeader(10, Headers.STATUS ) );
		headers.put( "HIERARCHY_DEPRECATED",    new SheetHeader(11, Headers.DEPRECATED ) );
		headers.put( "HIERARCHY_GROUPS",        new SheetHeader(12, Headers.HIER_GROUPS ) );

		return headers;
	}

	@Override
	public ArrayList< ? extends Mappable > getData() {

		HierarchyDAO hierDao = new HierarchyDAO( catalogue );

		// get all the hierarchies as data
		return hierDao.getAll();
	}

	
}
