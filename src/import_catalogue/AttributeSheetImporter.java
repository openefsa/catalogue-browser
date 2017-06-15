package import_catalogue;

import java.util.Collection;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_object.Attribute;
import catalogue_object.AttributeBuilder;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;

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
	public AttributeSheetImporter( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}

	@Override
	public Attribute getByResultSet(ResultDataSet rs) {
		
		// save the code for future use (ids)
		String code = rs.getString ( Headers.CODE );

		// ignore if no code
		if ( code.isEmpty() )
			return null;

		AttributeBuilder builder = new AttributeBuilder();

		builder.setCatalogue( catalogue );
		builder.setCode( code );
		builder.setName( rs.getString ( Headers.NAME ) );
		builder.setLabel( rs.getString ( Headers.LABEL ) );
		builder.setScopenotes( rs.getString ( Headers.SCOPENOTE ) );
		builder.setReportable( rs.getString ( Headers.ATTR_REPORT ) );
		builder.setVisible( rs.getBoolean ( Headers.ATTR_VISIB, true ) );
		builder.setSearchable( rs.getBoolean ( Headers.ATTR_SEARCH, true ) );
		builder.setOrder( rs.getInt ( Headers.ATTR_ORDER, 1 ) );
		builder.setType( rs.getString ( Headers.ATTR_TYPE ) );
		builder.setMaxLength( rs.getInt ( Headers.ATTR_MAX_LENGTH, 200 ) );
		builder.setPrecision( rs.getInt ( Headers.ATTR_PRECISION, 10 ) );
		builder.setScale( rs.getInt ( Headers.ATTR_SCALE, 0 ) );
		builder.setCatalogueCode( rs.getString ( Headers.ATTR_CAT_CODE ) );
		builder.setSingleOrRepeatable( rs.getString ( Headers.ATTR_SR ) );
		builder.setInheritance( rs.getString ( Headers.ATTR_INHERIT ) );
		builder.setUniqueness( rs.getBoolean ( Headers.ATTR_UNIQUE, false ) );
		builder.setTermCodeAlias( rs.getBoolean ( Headers.ATTR_ALIAS, false ) );
		builder.setLastUpdate( rs.getTimestamp ( Headers.LAST_UPDATE ) );
		builder.setValidFrom( rs.getTimestamp ( Headers.VALID_FROM ) );
		builder.setValidTo( rs.getTimestamp ( Headers.VALID_TO ) );
		builder.setStatus( rs.getString ( Headers.STATUS ) );
		builder.setDeprecated( rs.getBoolean ( Headers.DEPRECATED, false ) );
		builder.setVersion( rs.getString ( Headers.VERSION ) );
		
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

	@Override
	public void end() {}
}
