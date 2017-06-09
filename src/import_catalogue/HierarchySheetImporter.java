package import_catalogue;

import java.util.Collection;

import catalogue.Catalogue;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Hierarchy;
import catalogue_object.HierarchyBuilder;
import excel_file_management.ResultDataSet;
import sheet_converter.Headers;

/**
 * Import the hierarchy sheet
 * @author avonva
 *
 */
public class HierarchySheetImporter extends SheetImporter<Hierarchy> {

	private Catalogue catalogue;
	private String masterCode;
	
	/**
	 * Initialize the import of the hierarchy sheet
	 * @param catalogue the catalogue which contains the hierarchies
	 * @param hierData the sheet hierarchies data
	 */
	public HierarchySheetImporter( Catalogue catalogue, ResultDataSet hierData ) {
		super ( hierData );
		this.catalogue = catalogue;
	}

	/**
	 * Set the master hierarchy code (which is the
	 * catalogue code).
	 * @param masterCode
	 */
	public void setMasterCode( String masterCode ) {
		this.masterCode = masterCode;
	}
	
	@Override
	public Hierarchy getByResultSet(ResultDataSet rs) {
		
		// get the hierarchy code
		String code = rs.getString ( Headers.CODE );

		// if empty ignore
		if ( code.isEmpty() )
			return null;

		boolean isMaster = code.equals( masterCode );

		HierarchyBuilder builder = new HierarchyBuilder();

		builder.setCatalogue( catalogue );
		
		// for local catalogues the master
		// should have the same name of the
		// local catalogue
		if ( catalogue.isLocal() && isMaster ) {
			builder.setCode( catalogue.getCode() );
			builder.setName( catalogue.getName() );
			builder.setLabel( catalogue.getLabel() );
		}
		else {
			builder.setCode( code );
			builder.setName( rs.getString ( Headers.NAME ) );
			builder.setLabel( rs.getString ( Headers.LABEL ) );
		}

		builder.setScopenotes( rs.getString ( Headers.SCOPENOTE ) );
		builder.setApplicability( rs.getString ( Headers.HIER_APPL ) );

		builder.setOrder( rs.getInt ( Headers.HIER_ORDER, 1 ) );
		builder.setStatus( rs.getString ( Headers.STATUS ) );

		// set the is_master field as true if the hierarchy code
		// is the same as the catalogue code (convention)
		// otherwise false
		builder.setMaster( isMaster );

		builder.setLastUpdate( rs.getTimestamp( Headers.LAST_UPDATE ) );
		builder.setValidFrom( rs.getTimestamp( Headers.VALID_FROM ) );
		builder.setValidTo( rs.getTimestamp( Headers.VALID_TO ) );
		builder.setVersion( rs.getString ( Headers.VERSION ) );

		builder.setDeprecated( rs.getBoolean( Headers.DEPRECATED, false ) );
		builder.setGroups( rs.getString ( Headers.HIER_GROUPS ) );

		return builder.build();
	}

	@Override
	public void insert( Collection<Hierarchy> hierarchies ) {
		
		HierarchyDAO hierDao = new HierarchyDAO( catalogue );

		// insert all the hierarchies into the database
		hierDao.insertHierarchies( hierarchies );
	}

	@Override
	public Collection<Hierarchy> getAllByResultSet(ResultDataSet rs) {
		return null;
	}
}
