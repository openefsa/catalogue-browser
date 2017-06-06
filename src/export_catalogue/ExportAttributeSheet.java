package export_catalogue;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Workbook;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_object.Mappable;
import sheet_converter.SheetHeader;

/**
 * Export the current catalogue attributes into the attribute sheet
 * @author avonva
 *
 */
public class ExportAttributeSheet extends SheetWriter {

	private Catalogue catalogue;
	
	public ExportAttributeSheet( Catalogue catalogue, 
			Workbook workbook, String sheetName) {
		
		super(workbook, sheetName);
		this.catalogue = catalogue;
	}

	@Override
	public HashMap<String, SheetHeader> getHeaders() {
		
		HashMap<String, SheetHeader> headers = new HashMap<>();
		
		headers.put( "ATTR_CODE",              new SheetHeader(0, "code" ) );
		headers.put( "ATTR_NAME",              new SheetHeader(1, "name" ) );
		headers.put( "ATTR_LABEL",             new SheetHeader(2, "label" ) );
		headers.put( "ATTR_SCOPENOTE",         new SheetHeader(3, "scopeNote" ) );
		headers.put( "ATTR_REPORTABLE",        new SheetHeader(4, "attributeReportable" ) );
		headers.put( "ATTR_VISIBLE",           new SheetHeader(5, "attributeVisible" ) );
		headers.put( "ATTR_SEARCHABLE",        new SheetHeader(6, "attributeSearchable" ) );
		headers.put( "ATTR_ORDER",             new SheetHeader(7, "attributeOrder" ) );
		headers.put( "ATTR_TYPE",              new SheetHeader(8, "attributeType" ) );
		headers.put( "ATTR_MAX_LENGTH",        new SheetHeader(9, "attributeMaxLength" ) );
		headers.put( "ATTR_PRECISION",         new SheetHeader(10, "attributePrecision" ) );
		headers.put( "ATTR_SCALE",             new SheetHeader(11, "attributeScale" ) );
		headers.put( "ATTR_CAT_CODE",          new SheetHeader(12, "attributeCatalogueCode" ) );
		headers.put( "ATTR_SINGLE_REPEATABLE", new SheetHeader(13, "attributeSingleOrRepeatable" ) );
		headers.put( "ATTR_INHERITANCE",       new SheetHeader(14, "attributeInheritance" ) );
		headers.put( "ATTR_UNIQUENESS",        new SheetHeader(15, "attributeUniqueness" ) );
		headers.put( "ATTR_TERM_CODE_ALIAS",   new SheetHeader(16, "attributeTermCodeAlias" ) );
		headers.put( "ATTR_VERSION",           new SheetHeader(17, "version" ) );
		headers.put( "ATTR_LAST_UPDATE",       new SheetHeader(18, "lastUpdate" ) );
		headers.put( "ATTR_VALID_FROM",        new SheetHeader(19, "validFrom" ) );
		headers.put( "ATTR_VALID_TO",          new SheetHeader(20, "validTo" ) );
		headers.put( "ATTR_STATUS",            new SheetHeader(21, "status" ) );
		headers.put( "ATTR_DEPRECATED",        new SheetHeader(22, "deprecated" ) );
		return headers;
	}

	@Override
	public ArrayList<? extends Mappable> getData() {

		// initialize dao of attributes
		AttributeDAO attrDao = new AttributeDAO( catalogue );
		
		return attrDao.getAll();
	}

}
