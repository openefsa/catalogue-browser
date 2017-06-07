package import_catalogue;

import java.util.Collection;

import catalogue.Catalogue;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Term;
import catalogue_object.TermBuilder;
import excel_file_management.ResultDataSet;

/**
 * Importer of the term sheet
 * @author avonva
 *
 */
public class TermSheetImporter extends SheetImporter<Term> {

	private Catalogue catalogue;
	
	/**
	 * Initialize the term sheet importer
	 * @param catalogue the catalogue which contains the terms
	 * @param termData the sheet term data
	 */
	public TermSheetImporter( Catalogue catalogue, ResultDataSet termData ) {
		super ( termData );
		this.catalogue = catalogue;
	}
	
	@Override
	public Term getByResultSet(ResultDataSet rs) {
		
		// skip if no term code
		if ( rs.getString ( "termCode" ).isEmpty() )
			return null;

		// save the code in order to be able to use it later
		// for retrieving terms ids
		String code = rs.getString ( "termCode" );

		TermBuilder builder = new TermBuilder();

		builder.setCatalogue( catalogue );
		builder.setCode( code );
		builder.setName( rs.getString ( "termExtendedName" ) );
		builder.setLabel( rs.getString ( "termShortName" ) );
		builder.setScopenotes( rs.getString ( "termScopeNote" ) );
		builder.setDeprecated( rs.getBoolean ( "deprecated", false ) );
		builder.setLastUpdate( rs.getTimestamp ( "lastUpdate", true ) );
		builder.setValidFrom( rs.getTimestamp ( "validFrom", true ) );
		builder.setValidTo( rs.getTimestamp( "validTo", true ) );
		builder.setStatus( rs.getString( "status" ) );

		return builder.build();
	}

	@Override
	public void insert( Collection<Term> terms ) {
		
		TermDAO termDao = new TermDAO( catalogue );

		// insert the batch of terms into the db
		termDao.insertTerms( terms );
	}

	@Override
	public Collection<Term> getAllByResultSet(ResultDataSet rs) {
		return null;
	}
}
