package import_catalogue;

import java.util.Collection;

import catalogue.Catalogue;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Hierarchy;
import catalogue_object.HierarchyBuilder;
import excel_file_management.ResultDataSet;

/**
 * Import the hierarchy sheet
 * @author avonva
 *
 */
public class HierarchySheetImporter extends SheetImporter<Hierarchy> {

	private Catalogue catalogue;

	/**
	 * Initialize the import of the hierarchy sheet
	 * @param catalogue the catalogue which contains the hierarchies
	 * @param hierData the sheet hierarchies data
	 */
	public HierarchySheetImporter( Catalogue catalogue, ResultDataSet hierData ) {
		super ( hierData );
		this.catalogue = catalogue;
	}

	@Override
	public Hierarchy getByResultSet(ResultDataSet rs) {
		
		// get the hierarchy code
		String code = rs.getString ( "code" );

		// if empty ignore
		if ( code.isEmpty() )
			return null;

		boolean isMaster = code.equals( catalogue.getCode() );

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
			builder.setName( rs.getString ( "name" ) );
			builder.setLabel( rs.getString ( "label" ) );
		}

		builder.setScopenotes( rs.getString ( "scopeNote" ) );
		builder.setApplicability( rs.getString ( "hierarchyApplicability" ) );

		builder.setOrder( rs.getInt ( "hierarchyOrder", 1 ) );
		builder.setStatus( rs.getString ( "hierarchyStatus" ) );

		// set the is_master field as true if the hierarchy code
		// is the same as the catalogue code (convention)
		// otherwise false
		builder.setMaster( isMaster );

		builder.setLastUpdate( rs.getTimestamp( "lastUpdate" ) );
		builder.setValidFrom( rs.getTimestamp( "validFrom" ) );
		builder.setValidTo( rs.getTimestamp( "validTo" ) );
		builder.setVersion( rs.getString ( "version" ) );

		builder.setDeprecated( rs.getBoolean( "deprecated", false ) );
		builder.setGroups( rs.getString ( "hierarchyGroups" ) );

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
