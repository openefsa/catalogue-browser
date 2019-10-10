package export_catalogue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Workbook;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Mappable;
import catalogue_object.Term;
import naming_convention.Headers;
import sheet_header.SheetHeader;
import term.CodeSorter;

/**
 * Export the terms, attribute values for each term and the term applicabilities
 * in the term sheet.
 * @author avonva
 * @author shahaal
 */
public class ExportTermSheet extends SheetWriter {

	private Catalogue catalogue;
	//flag used for the interpreting and checking tool
	private boolean flag; 
	private List<String> IctColumns = Stream.of("allFacets", "matrixCode", "foodexOldCode", "detailLevel", "termType").collect(Collectors.toList());
	
	public ExportTermSheet( Catalogue catalogue, Workbook workbook, String sheetName, boolean b) {
		
		super(workbook, sheetName);
		this.catalogue = catalogue;
		this.flag = b;
	}

	@Override
	public HashMap<String, SheetHeader> getHeaders() {

		HashMap<String, SheetHeader> headers = new HashMap<>();

		int i = 0;
		
		if(flag) {//if all info needed 
			headers.put( "TERM_CODE",              new SheetHeader(i++, Headers.TERM_CODE ) );
			headers.put( "TERM_EXTENDED_NAME",     new SheetHeader(i++, Headers.TERM_EXT_NAME ) );
			headers.put( "TERM_SHORT_NAME",        new SheetHeader(i++, Headers.TERM_SHORT_NAME ) );
			headers.put( "TERM_SCOPENOTE",         new SheetHeader(i++, Headers.TERM_SCOPENOTE ) );
			headers.put( "TERM_VERSION",           new SheetHeader(i++, Headers.VERSION ) );
			headers.put( "TERM_LAST_UPDATE",       new SheetHeader(i++, Headers.LAST_UPDATE ) );
			headers.put( "TERM_VALID_FROM",        new SheetHeader(i++, Headers.VALID_FROM ) );
			headers.put( "TERM_VALID_TO",          new SheetHeader(i++, Headers.VALID_TO ) );
			headers.put( "TERM_STATUS",            new SheetHeader(i++, Headers.STATUS ) );
			headers.put( "TERM_DEPRECATED",        new SheetHeader(i++, Headers.DEPRECATED ) );
		}else {//if just for ICT
			headers.put( "TERM_CODE",              new SheetHeader(i++, Headers.TERM_CODE ) );
			headers.put( "TERM_EXTENDED_NAME",     new SheetHeader(i++, Headers.TERM_EXT_NAME ) );
		}

		AttributeDAO attrDao = new AttributeDAO( catalogue );

		// for each attribute add the header with the attribute name
		if(flag) {
			for ( Attribute attr : attrDao.getNonFacetAttributes() ) {
				headers.put( "attribute_" + attr.getName(), 
						new SheetHeader( i++, attr.getName() ) );
			}
		}else {
			for ( Attribute attr : attrDao.getNonFacetAttributes() ) {
				if(IctColumns.contains(attr.getName())){
					headers.put( "attribute_" + attr.getName(), 
							new SheetHeader( i++, attr.getName() ) );
				}
			}
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
					new SheetHeader( i++, code + Headers.SUFFIX_FLAG ) );
			
			if(flag) {
				headers.put( "parent_" + code,
						new SheetHeader( i++, code + Headers.SUFFIX_PARENT_CODE ) );
	
				headers.put( "order_" + code,
						new SheetHeader( i++, code + Headers.SUFFIX_ORDER ) );
	
				headers.put( "reportable_" + code,
						new SheetHeader( i++, code + Headers.SUFFIX_REPORT ) );
	
				headers.put( "hierarchyCode_" + code,
						new SheetHeader( i++, code + Headers.SUFFIX_HIER_CODE ) );
			}
		}

		return headers;
	}

	@Override
	public Collection<? extends Mappable> getData() {
		
		List< Term > terms = new ArrayList<>( catalogue.getTerms() );
		
		Collections.sort( terms, new CodeSorter() );
		
		return terms;
	}
}
