package export_catalogue;

import java.util.Collection;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Workbook;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Mappable;
import sheet_converter.SheetHeader;

/**
 * Export the terms, attribute values for each term and the term applicabilities
 * in the term sheet.
 * @author avonva
 *
 */
public class ExportTermSheet extends SheetWriter {

	private Catalogue catalogue;
	
	public ExportTermSheet( Catalogue catalogue, 
			Workbook workbook, String sheetName) {
		
		super(workbook, sheetName);
		this.catalogue = catalogue;
	}

	@Override
	public HashMap<String, SheetHeader> getHeaders() {

		HashMap<String, SheetHeader> headers = new HashMap<>();

		int i = 0;
		
		headers.put( "TERM_CODE",              new SheetHeader(i++, "termCode" ) );
		headers.put( "TERM_EXTENDED_NAME",     new SheetHeader(i++, "termExtendedName" ) );
		headers.put( "TERM_SHORT_NAME",        new SheetHeader(i++, "termShortName" ) );
		headers.put( "TERM_SCOPENOTE",         new SheetHeader(i++, "termScopeNote" ) );
		headers.put( "TERM_VERSION",           new SheetHeader(i++, "version" ) );
		headers.put( "TERM_LAST_UPDATE",       new SheetHeader(i++, "lastUpdate" ) );
		headers.put( "TERM_VALID_FROM",        new SheetHeader(i++, "validFrom" ) );
		headers.put( "TERM_VALID_TO",          new SheetHeader(i++, "validTo" ) );
		headers.put( "TERM_STATUS",            new SheetHeader(i++, "status" ) );
		headers.put( "TERM_DEPRECATED",        new SheetHeader(i++, "deprecated" ) );

		AttributeDAO attrDao = new AttributeDAO( catalogue );

		// for each attribute add the header with the attribute name
		for ( Attribute attr : attrDao.fetchNonCatalogueAttributes() ) {
			headers.put( "attribute_" + attr.getName(), 
					new SheetHeader( i++, attr.getName() ) );
		}
		
		// for each hierarchy add 4 headers regarding the flag, the parent code
		// the order and the reportable flag
		// in the export file we also add the hierarchy code of terms! We do not
		// add the hierarchy code as attribute since if we import the exported
		// excel the hierarchy code would be inserted into the database
		// and then if we reexport it would be reinserted into the excel twice
		// and so on...
		
		HierarchyDAO hierDao = new HierarchyDAO( catalogue );
		
		for ( Hierarchy hierarchy : hierDao.getAll() ) {

			String code = hierarchy.isMaster() ? Hierarchy.MASTER_HIERARCHY_CODE : hierarchy.getCode();

			headers.put( "flag_" + code,
					new SheetHeader( i++, code + "Flag" ) );

			headers.put( "parent_" + code,
					new SheetHeader( i++, code + "ParentCode" ) );

			headers.put( "order_" + code,
					new SheetHeader( i++, code + "Order" ) );

			headers.put( "reportable_" + code,
					new SheetHeader( i++, code + "Reportable" ) );

			headers.put( "hierarchyCode_" + code,
					new SheetHeader( i++, code + "HierarchyCode" ) );
		}

		return headers;
	}

	@Override
	public Collection<? extends Mappable> getData() {
		return catalogue.getTerms();
	}
}
