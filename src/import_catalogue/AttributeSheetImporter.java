package import_catalogue;

import java.util.Collection;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_object.Attribute;
import catalogue_object.AttributeBuilder;
import excel_file_management.ResultDataSet;

/**
 * Import the attribute sheet into the database
 * @author avonva
 *
 */
public class AttributeSheetImporter extends SheetImporter<Attribute> {

	private Catalogue catalogue;
	
	/**
	 * Initialize the attribute sheet importer
	 * @param catalogue the catalogue which contains the attributes
	 * @param attrData the sheet data related to the attributes
	 */
	public AttributeSheetImporter( Catalogue catalogue, ResultDataSet attrData ) {
		super ( attrData );
		this.catalogue = catalogue;
	}

	@Override
	public Attribute getByResultSet(ResultDataSet rs) {
		
		// save the code for future use (ids)
		String code = rs.getString ( "code" );

		// ignore if no code
		if ( code.isEmpty() )
			return null;

		AttributeBuilder builder = new AttributeBuilder();

		builder.setCatalogue( catalogue );
		builder.setCode( code );
		builder.setName( rs.getString ( "name" ) );
		builder.setLabel( rs.getString ( "label" ) );
		builder.setScopenotes( rs.getString ( "scopeNote" ) );
		builder.setReportable( rs.getString ( "attributeReportable" ) );
		builder.setVisible( rs.getBoolean ( "attributeVisible", true ) );
		builder.setSearchable( rs.getBoolean ( "attributeSearchable", true ) );
		builder.setOrder( rs.getInt ( "attributeOrder", 1 ) );
		builder.setType( rs.getString ( "attributeType" ) );
		builder.setMaxLength( rs.getInt ( "attributeMaxLength", 200 ) );
		builder.setPrecision( rs.getInt ( "attributePrecision", 10 ) );
		builder.setScale( rs.getInt ( "attributeScale", 0 ) );
		builder.setCatalogueCode( rs.getString ( "attributeCatalogueCode" ) );
		builder.setSingleOrRepeatable( rs.getString ( "attributeSingleOrRepeatable" ) );
		builder.setInheritance( rs.getString ( "attributeInheritance" ) );
		builder.setUniqueness( rs.getBoolean ( "attributeUniqueness", false ) );
		builder.setTermCodeAlias( rs.getBoolean ( "attributeTermCodeAlias", false ) );
		builder.setLastUpdate( rs.getTimestamp ( "lastUpdate" ) );
		builder.setValidFrom( rs.getTimestamp ( "validFrom" ) );
		builder.setValidTo( rs.getTimestamp ( "validTo" ) );
		builder.setStatus( rs.getString ( "status" ) );
		builder.setDeprecated( rs.getBoolean ( "deprecated", false ) );
		builder.setVersion( rs.getString ( "version" ) );
		
		return builder.build();
	}

	@Override
	public void insert( Collection<Attribute> attrs ) {
		
		// insert the attributes into the database
		AttributeDAO attrDao = new AttributeDAO( catalogue );
		attrDao.insertAttributes( attrs );
	}

	@Override
	public Collection<Attribute> getAllByResultSet(ResultDataSet rs) {
		return null;
	}
}
