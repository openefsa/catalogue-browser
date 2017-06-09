package import_catalogue;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_browser_dao.ReleaseNotesDAO;
import catalogue_browser_dao.ReleaseNotesOperationDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Attribute;
import catalogue_object.AttributeBuilder;
import catalogue_object.Hierarchy;
import catalogue_object.HierarchyBuilder;
import catalogue_object.Term;
import catalogue_object.TermBuilder;
import excel_file_management.ResultDataSet;
import excel_file_management.XLSXFormat;
import messages.Messages;
import term_type.TermType;
import ui_progress_bar.FormProgressBar;
import ui_search_bar.SearchOptionDAO;
import user_preferences.CataloguePreferenceDAO;

/**
 * Class which is used to import an xlsx catalogue
 * into the database.
 * @author avonva
 *
 */
@Deprecated
public class ImportExcelCatalogue {

	FormProgressBar progressBar;

	XSSFWorkbook _wb;

	private HashMap< String, Integer > termIds = null;  // contains the id of terms (db ids)
	private HashMap< String, Integer > attrIds = null;  // contains the id of attributes (db ids)
	private HashMap< String, Integer > hierIds = null;  // contains the id of hierarchies (db ids)
	Catalogue currentCatalogue;                         // the current catalogue, used for check which hierarchy is the master
	XLSXFormat rawData;


	private boolean local = false;
	private Catalogue localCat;
	
	/**
	 * Set the progress bar for the import process
	 * @param progressBar
	 */
	public void setProgressBar( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}

	/**
	 * Get the catalogue from the result set
	 * @param catData
	 * @return
	 * @throws SQLException
	 */
	private Catalogue getCatalogue ( ResultDataSet catData ) throws SQLException {
		
		// point to the first row
		catData.next();

		Catalogue catalogue = CatalogueSheetImporter.getCatalogueFromExcel ( catData );
		
		// get the catalogue from the data
		return catalogue;
	}
	
	/**
	 * Update the progress bar progress and label
	 * @param progress
	 * @param label
	 */
	private void updateProgressBar ( int progress, String label ) {
		
		if ( progressBar == null )
			return;
		
		progressBar.addProgress( progress );
		progressBar.setLabel( label );
	}

	/**
	 * Import a local catalogue
	 * @param dbPath
	 * @param fileName
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws ImportExcelException
	 */
	public boolean importLocalCatalogue ( Catalogue localCat, String dbPath, String fileName ) 
			throws InvalidFormatException, IOException, ImportExcelException {
		this.local = true;
		this.localCat = localCat;
		return importCatalogue ( dbPath, fileName );
	}
	
	/**
	 * This method import all the workbook data in the {@code dbPath}.
	 * @param dbPath where the import data should be inserted
	 * @param fileName name and path to retrieve the import file.
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws ImportExcelException 
	 */
	public boolean importCatalogue ( String dbPath, String fileName ) 
			throws InvalidFormatException, IOException, ImportExcelException {

		ResultDataSet catData;
		ResultDataSet termData;
		ResultDataSet attrData;
		ResultDataSet hierData;
		ResultDataSet rnData;  // release notes data
		try {
			
			// get the excel data
			rawData = new XLSXFormat( fileName );
			
			// get the catalogue sheet and check if the catalogues are compatible
			// (the open catalogue and the one we want to import)
			catData = rawData.processSheetName( "catalogue" );

			// get the catalogue from the excel
			this.currentCatalogue = getCatalogue( catData );
			
			// if anything was found => create a new catalogue
			// as default we create the catalogue using the official folder and the catalogue code and version
			// obtained from the excel sheet
			if ( dbPath == null )
				dbPath = currentCatalogue.buildDBFullPath( DatabaseManager.OFFICIAL_CAT_DB_FOLDER + 
						System.getProperty("file.separator") + "CAT_" + currentCatalogue.getCode() + "_DB" );

			// update the db path of the catalogue
			currentCatalogue.setDbFullPath( dbPath );
			
			// try to connect to the database. If it is not present we have an exception and thus we
			// create the database starting from scrach
			try {
				
				Connection con = currentCatalogue.getConnection();
				con.close();
				
				// if no exception was thrown => the database exists and we have to delete it
				
				// delete the content of the old catalogue database
				System.out.println( "Deleting the database located in " + dbPath );

				updateProgressBar( 10, Messages.getString("ImportExcelXLSX.DeleteDBLabel") );
				
				CatalogueDAO catDao = new CatalogueDAO();
				catDao.deleteDBRecords ( currentCatalogue );
				
				// set the id to the catalogue
				int id = catDao.getCatalogue( currentCatalogue.getCode(), 
						currentCatalogue.getVersion() ).getId();
				
				currentCatalogue.setId( id );
			}
			catch ( SQLException e ) {
				
				// otherwise the database does not exist => we create it
				
				System.out.println ( "Add " + currentCatalogue.getLabel() + " - " + currentCatalogue.getVersion() +
						" data to the CATALOGUE table");
				
				CatalogueDAO catDao = new CatalogueDAO();
				
				// set the id to the catalogue
				int id = catDao.insert( currentCatalogue );
				
				currentCatalogue.setId( id );
				
				// create the standard database structure for
				// the new catalogue
				catDao.createDBTables( currentCatalogue.getDbFullPath() );
			}
			
			System.out.println( currentCatalogue.getReleaseNotes() );
			
			// add the catalogue information
			ReleaseNotesDAO notesDao = new ReleaseNotesDAO( currentCatalogue );
			notesDao.insert( currentCatalogue.getReleaseNotes() );
			
			// here the database exists for sure and we insert the data

			updateProgressBar( 10, Messages.getString("ImportExcelXLSX.ImportHierarchyLabel") );
			
			System.out.println( "Importing HIERARCHY table" );
			
			// get the hierarchy sheet
			hierData = rawData.processSheetName( "hierarchy" );
			
			// import the hierarchy sheet into the DB of the current catalogue
			importHierarchies ( currentCatalogue, hierData );
			
			
			updateProgressBar( 10, Messages.getString("ImportExcelXLSX.ImportAttributeLabel") );

			
			System.out.println( "Importing ATTRIBUTE table" );
			
			// get the attribute sheet
			attrData = rawData.processSheetName( "attribute" );
			
			// import the attribute sheet into the DB of the current catalogue
			importAttributes ( currentCatalogue, attrData );
			
			// import all the term types from the attributes, note that this method
			// should be called only after creating the attribute table
			
			importTermTypes( currentCatalogue, getTermTypeValues( currentCatalogue ) );

			
			updateProgressBar( 20, Messages.getString("ImportExcelXLSX.ImportTermLabel") );
			
			System.out.println( "Importing TERM table" );
			
			// get the term sheet
			termData = rawData.processSheetName( "term" );
	
			// import the term sheet into the DB of the current catalogue
			importTerms ( currentCatalogue, termData );

			
			// import the attributes values for each term
			System.out.println( "Importing TERM ATTRIBUTES table" );
			
			updateProgressBar( 15, Messages.getString("ImportExcelXLSX.ImportTermAttrLabel") );
			
			// reset the iterators to the beginning
			termData.initScan();
			attrData.initScan();
			importTermsAttributes ( currentCatalogue, termData, attrData );
			
			
			
			System.out.println( "Importing PARENT TERM table" );
			
			updateProgressBar( 15, 
					Messages.getString("ImportExcelXLSX.ImportTermParents") );
			
			// reset the iterators to the beginning
			termData.initScan();
			hierData.initScan();
			importParentTerms( currentCatalogue, termData, hierData );

			System.out.println( "Importing RELEASE NOTES table" );
			
			updateProgressBar( 15, 
					Messages.getString("ImportExcelXLSX.ImportReleaseNotes") );
			
			// import the release notes operations
			try {
				rnData = rawData.processSheetName( "releaseNotes" );
				importReleaseNotesOperations ( currentCatalogue, rnData );
			} catch ( Exception e ) {
				System.err.println( "Release notes not found for " + currentCatalogue );
			}

			// end
			updateProgressBar( 100, "" );
			
			// close dataset
			catData.close();
			termData.close();
			attrData.close();
			hierData.close();
			
			
			rawData.close();
			
			// after having imported the excel, we can insert the default preferences
			System.out.println ( "Insert default preferences values into the database" );
			
			CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( currentCatalogue );
			prefDao.insertDefaultPreferences();
			
			// insert the default search options
			SearchOptionDAO optDao = new SearchOptionDAO ( currentCatalogue );
			optDao.insertDefaultSearchOpt();
			
		} catch ( Exception e ) {
			e.printStackTrace();
			throw new ImportExcelException( e.getMessage() );
		}

		if ( progressBar != null )
			progressBar.close();
		
		return true;
	}

	/**
	 * Import the hierarchy table into the datbase
	 * @param rs
	 * @param DBPath
	 * @return
	 * @throws Exception
	 */
	private void importHierarchies ( Catalogue catalogue, ResultDataSet rs ) throws Exception {

		// hierarchies codes
		ArrayList<String> codes = new ArrayList<>();
		
		Collection<Hierarchy> hierarchies = new ArrayList<>();
		
		// get the data and create the hierarchies
		while ( rs.next() ) {
			
			// get the hierarchy code
			String code = rs.getString ( "code" );
			
			// if empty go on
			if ( code.isEmpty() )
				continue;
			
			// add the code to the cache for future uses (insert terms)
			codes.add( code );
			
			boolean isMaster = code.equals( catalogue.getCode() );
			
			HierarchyBuilder builder = new HierarchyBuilder();
			
			builder.setCatalogue( catalogue );

			// for local catalogues the master
			// should have the same name of the
			// local catalogue
			if ( this.local && isMaster ) {
				builder.setCode( localCat.getCode() );
				builder.setName( localCat.getName() );
				builder.setLabel( localCat.getLabel() );
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
			
			hierarchies.add( builder.build() );
		}
		
		HierarchyDAO hierDao = new HierarchyDAO( catalogue );
		
		// insert all the hierarchies into the database
		hierDao.insertHierarchies( hierarchies );
		
		// get the generated hierarchies ids from the DB
		hierIds = createIdHashMap ( catalogue, "HIERARCHY_ID", "HIERARCHY_CODE", "APP.HIERARCHY" );
	}

	
	
	
	/**
	 * Import the attribute sheet into the DB of the currently open catalogue
	 * Return true if everything went ok
	 * @param rs
	 * @param DBPath
	 * @return
	 * @throws Exception
	 */
	private void importAttributes ( Catalogue catalogue, ResultDataSet rs ) throws Exception  { 

		// attributes codes
		ArrayList<String> codes = new ArrayList<>();
		Collection<Attribute> attrs = new ArrayList<>();
		
		// get all the attributes contained in the data
		while ( rs.next() ) {

			// save the code for future use (ids)
			String code = rs.getString ( "code" );
			
			if ( code.isEmpty() )
				continue;
			
			codes.add( code );
			
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
			
			attrs.add( builder.build() );
		}
		
		AttributeDAO attrDao = new AttributeDAO( catalogue );
		
		attrDao.insertAttributes( attrs );
		
		// get the generated attributes ids from the DB
		attrIds = createIdHashMap ( catalogue, "ATTR_ID", "ATTR_CODE", "APP.ATTRIBUTE" );
	}
	
	
	/**
	 * Create the table related to the term types retrieved from the term type scopenote
	 */
	private static ArrayList< TermType > getTermTypeValues ( Catalogue catalogue ) {
		
		
		// output array
		ArrayList< TermType > termTypes = new ArrayList<>();
		
		Connection con;
		
		String scopenoteQuery = "select ATTR_SCOPENOTE from APP.ATTRIBUTE where ATTR_NAME = ?";
		
		
		try {
			
			con = catalogue.getConnection();
			
			PreparedStatement getScopenoteQuery = con.prepareStatement( scopenoteQuery );
			
			
			getScopenoteQuery.clearParameters();
			
			// get only the term type attribute
			getScopenoteQuery.setString( 1, "termType" );
			
			ResultSet rs = getScopenoteQuery.executeQuery();
			
			// get the scopenote
			if ( rs.next() ) {
				
				// parse the attribute scopenote using the $ separator
				StringTokenizer st = new StringTokenizer( rs.getString( "ATTR_SCOPENOTE" ), "$" );
				
				int tokenCount = 0;
				
				// for each term type code-description
				while ( st.hasMoreTokens() ) {
					
					tokenCount++;
					
					// get the pair code=description
					String token = st.nextToken();

					// skip if we are reading the scopenote!
					if ( tokenCount == 1 ) {
						continue;
					}
					
					// get the code and description separately
					String[] values = token.split( "=" );
					
					// if wrong number of elements return
					if ( values.length != 2 ) {
					
						System.err.println ( "Wrong term type syntax in scopenotes, found : " + token + " expected: "
								+ "code=description. Check also the white spaces." );
						
						return null;
					}
					
					// add the term type code, description to the hashmap
					// the id is not important here, we give 1 as default
					termTypes.add ( new TermType( 1, values[0], values[1] ) );
				}
			}
			
			rs.close();
			getScopenoteQuery.close();
			con.close();

		}
		catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return termTypes;
	}



	/**
	 * Import into the database all the term types which were passed as input
	 * @param termTypes
	 * @param DBPath
	 */
	private void importTermTypes ( Catalogue catalogue, ArrayList< TermType > termTypes ) {

		Connection con;

		String typeQuery = "insert into APP.TERM_TYPE (TERM_TYPE_CODE, TERM_TYPE_LABEL) values (?, ?)";

		try {

			con = catalogue.getConnection();
			
			PreparedStatement insertTypeQuery = con.prepareStatement( typeQuery );

			// for each term type
			for ( TermType type : termTypes ) {

				// clear the parameters
				insertTypeQuery.clearParameters();

				// add the term type code
				insertTypeQuery.setString( 1, type.getCode() );

				// add the term type description
				insertTypeQuery.setString( 2, type.getLabel() );

				// add the batch
				insertTypeQuery.addBatch();
			}

			// insert all the term types into the database
			insertTypeQuery.executeBatch();

			// close the connection
			insertTypeQuery.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Import the terms sheet into the catalogue db
	 * @param rs
	 * @param DBPath
	 * @return
	 * @throws Exception
	 */
	private void importTerms ( Catalogue catalogue, ResultDataSet rs ) throws Exception {
		
		// term codes
		ArrayList<String> codes = new ArrayList<>();
		ArrayList<Term> terms = new ArrayList<>();
		
		// get all the terms from data
		while ( rs.next() ) {

			// skip if no term code
			if ( rs.getString ( "termCode" ).isEmpty() )
				continue;

			// save the code in order to be able to use it later
			// for retrieving terms ids
			String code = rs.getString ( "termCode" );
			codes.add( code );
			
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

			terms.add( builder.build() );
		}
		
		TermDAO termDao = new TermDAO( catalogue );

		// insert the batch of terms into the db
		termDao.insertTerms( terms );
		
		// get the generated term ids from the DB
		termIds = createIdHashMap ( catalogue, "TERM_ID", "TERM_CODE", "APP.TERM" );
	}

	

	/**
	 * Import the attributes values of the terms into the catalogue db
	 * @param rs
	 * @param DBPath
	 * @return
	 * @throws Exception
	 */
	private boolean importTermsAttributes ( Catalogue catalogue, 
			ResultDataSet termData, ResultDataSet attrData ) throws SQLException {
		
		Connection con;

		// create the base query for each record
		String query = "INSERT INTO APP.TERM_ATTRIBUTE (TERM_ID, ATTR_ID, " 
				+ "ATTR_VALUE ) VALUES ("
				+ "?, ?, ? )";

		try {

			// open the DB connection with the catalogue db path which is currently opened
			con = catalogue.getConnection();

			// create the sql base statement
			PreparedStatement stmt = con.prepareStatement( query );

			// get the attributes (id and names)
			ArrayList<Property> attributes = getAttributes( attrData );
			
			// get the records one by one and insert them into the database
			while ( termData.next() ) {

				// get the term code
				String termCode = termData.getString ( "termCode" );

				// skip if no term code was found
				if ( termCode.isEmpty() )
					continue;
				
				// get the term id using the term code from the hashmap (global var)
				int termId = termIds.get( termCode );

				// for each attribute we create a record for the term attributes table
				for ( Property attr : attributes ) {
					
					// clear parameters
					stmt.clearParameters();
					
					// get the attribute value in the term sheet using the attribute name
					String attrValue = termData.getString ( attr.getKey() );
					
					// continue only if there is indeed a value
					if ( attrValue == null || attrValue.isEmpty() )
						continue;
					
					
					// if repeatable we insert a record for each single value
					if ( attr.isRepeatable() ) {
						
						// parse the repeatable attribute
						StringTokenizer st = new StringTokenizer( attrValue, "$" );

						while ( st.hasMoreTokens() ) {

							stmt.clearParameters();
							
							// get the current token value
							String singleValue = st.nextToken();
							
							// set the term id
							stmt.setInt   ( 1, termId );
							
							// set the attribute id
							stmt.setInt   ( 2, attr.getId() );
							
							// set the value parameter
							stmt.setString( 3, singleValue );
							
							// add the record to the batch
							stmt.addBatch();
						}
					}
					else {  // if single attribute
						
						// set the term id
						stmt.setInt   ( 1, termId );
						
						// set the attribute id
						stmt.setInt   ( 2, attr.getId() );
						
						// set the value
						stmt.setString( 3, attrValue );
						
						// add the record to the batch
						stmt.addBatch();
					}
				}
			}  // end term while

			// execute the batch of insertions
			stmt.executeBatch();

			// close connection
			stmt.close();
			con.close();
			
			return true;

		} catch (SQLException | NumberFormatException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	
	
	/**
	 * Import the attributes values of the terms into the catalogue db
	 * @param rs
	 * @param DBPath
	 * @return
	 * @throws Exception
	 */
	private boolean importParentTerms ( Catalogue catalogue, ResultDataSet termData, ResultDataSet data ) throws Exception {
		
		Connection con;

		// create the base query for each record
		String query = "INSERT INTO APP.PARENT_TERM (TERM_ID, HIERARCHY_ID, " 
				+ "PARENT_TERM_ID, TERM_FLAG, TERM_ORDER, TERM_REPORTABLE ) VALUES ("
				+ "?, ?, ?, ?, ?, ? )";

		try {

			// open the DB connection with the catalogue db path which is currently opened
			con = catalogue.getConnection();

			// create the sql base statement
			PreparedStatement stmt = con.prepareStatement( query );

			// get the hierarchies from data
			ArrayList<Property> hierarchies = getHierarchies( data );
			
			// get the records one by one and insert them into the database
			while ( termData.next() ) {
				
				// get the term code
				String termCode = termData.getString ( "termCode" );

				// skip if no term code was found
				if ( termCode.isEmpty() )
					continue;
				
				// get the term id using the term code from the hashmap (global var)
				int termId = termIds.get( termCode );

				// for each attribute we create a record for the term attributes table
				for ( Property hierarchy : hierarchies ) {
					
					// add the current hierarchy to the statement
					addHierarchyToStatement ( termId, hierarchy, termData, stmt );
				}
			}  // end term while

			// execute the batch of insertions
			stmt.executeBatch();

			// close connection
			stmt.close();
			con.close();
			
			return true;

		} catch (SQLException | NumberFormatException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Import into the catalogue database all the release note operations
	 * @param catalogue
	 * @param data
	 * @return
	 */
	private boolean importReleaseNotesOperations( Catalogue catalogue, 
			ResultDataSet data ) {

		try {

			ReleaseNotesOperationDAO opDao = new 
					ReleaseNotesOperationDAO( catalogue );

			// do until we have data to parse
			while ( data.next() ) {
				
				// get all the ops related to the current excel row
				// separating the operation info (they are $ separated)
				ReleaseNotesOperation op = NotesSheetImporter
						.getByExcelResultSet( data );
				
				int id = opDao.insert( op );
				op.setId( id );
			}
			
			catalogue.refreshReleaseNotes();
			
			return true;

		} catch (SQLException | NumberFormatException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Add the parameters to the statemtent for parent terms
	 * @param termId
	 * @param hierarchy
	 * @param termData
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	private boolean addHierarchyToStatement ( int termId, Property hierarchy, ResultDataSet termData, 
			PreparedStatement stmt ) throws SQLException {

		// set the term id
		stmt.setInt( 1, termId );

		// set the hierarchy id
		stmt.setInt( 2, hierarchy.getId() );

		// get the parent term code 
		String parentCode = termData.getString ( getHierarchyFieldName( hierarchy, "ParentCode" ) );

		// next if no parent term is found
		if ( parentCode == null || parentCode.isEmpty() )
			return false;

		// check if we have the root term or not
		boolean isRoot = parentCode.equalsIgnoreCase( "ROOT" );
			
		// get the parent term id from the code
		Integer parentId = termIds.get( parentCode );

		// if not root and parent id not found => error
		if ( parentId == null && !isRoot ) {
			
			System.err.println ( "The parent term " + parentCode + " is not present in the DB, please check!" );
			
			return false;
		}

		// set the parent term id parameter
		// if root set null as convention
		if ( isRoot )
			stmt.setNull( 3, java.sql.Types.INTEGER );
		else 
			stmt.setInt( 3, parentId );
		
		// set the term flag
		boolean flag = termData.getBoolean( getHierarchyFieldName( hierarchy, "Flag" ), true );
		stmt.setBoolean( 4, flag );

		// set the term order
		int order = termData.getInt( getHierarchyFieldName( hierarchy, "Order" ), 0 );
		stmt.setInt( 5, order );

		// set the term reportability
		boolean reportable = termData.getBoolean( getHierarchyFieldName( hierarchy, "Reportable" ), true );
		stmt.setBoolean( 6, reportable );

		// add the record to the batch
		stmt.addBatch();
		
		return true;

	}

	/**
	 * Get the name of the column which contains the data related to the field "field".
	 * Example, we want the flag column related to the Reporting hierarchy => it returns reportingFlag
	 * if the hierarchy is the master, the convention says that we should search with the master keyword!
	 * In the previous example we would search masterFlag. The master code is variable, so we need this check.
	 * @param hierarchy
	 * @param field
	 * @return
	 */
	private String getHierarchyFieldName ( Property hierarchy, String field ) {
		
		String columnName;
		
		// if it is master hierarchy
		if ( hierarchy.getKey().equals( currentCatalogue.getCode() ) )
			columnName = Hierarchy.MASTER_HIERARCHY_CODE + field;
		else  // else, standard hierarchy
			columnName = hierarchy.getKey() + field; 
		
		return columnName;
	}
	
	/**
	 * Create an hashmap to save (key, ids) values (used to save DB ids when we insert the term)
	 * @param rs
	 * @param keys
	 * @return
	 * @throws SQLException
	 * @throws NoCatalogueOpenException 
	 */
	private HashMap<String, Integer> createIdHashMap ( Catalogue catalogue, String idField, String codeField, String tableName ) 
			throws SQLException {
		
		// output hashmap
		HashMap<String, Integer> hash = new HashMap<>();

		Connection con = catalogue.getConnection();
		
		// get the ids and codes
		PreparedStatement stmt = con.prepareStatement( "SELECT " + idField + "," + codeField + " FROM " + tableName );
		
		// get the results
		ResultSet rs = stmt.executeQuery();
		
		// for each element, set the id and the code and add to the hash map the element
		while ( rs.next() ) {
			
			// get the auto generated id
			int id = rs.getInt( idField );
			
			// get the current key
			String key = rs.getString( codeField );
			
			// add the pair code id into the hash map
			hash.put( key, id );
		}
		
		rs.close();
		stmt.close();
		con.close();
		
		return hash;
	}
	
	/**
	 * Get all the attributes id and names, in order to be able to retrieve
	 * their values for each term
	 * @param attrData
	 * @param catalogueAttr => Do we want catalogue attributes or the other attributes?
	 * @return
	 */
	private ArrayList<Property> getAttributes( ResultDataSet attrData ) {
		return getAttributes( attrData, false );
	}
	
	private ArrayList<Property> getAttributes( ResultDataSet attrData, boolean catalogueAttr ) {
		
		// output
		ArrayList<Property> attributes = new ArrayList<>();
		
		// for each attribute
		while ( attrData.next() ) {
			
			// get the attribute code
			String attrCode = attrData.getString ( "code" );
			
			// skip if no attr code
			if ( attrCode.isEmpty() )
				continue;
			
			// get the attr id using the attr code
			int attrId = attrIds.get( attrCode );
			
			// we save the name of the attribute into the attributes array list
			String attrName = attrData.getString( "name" );
			
			// get if the attribute is repeatable
			boolean attrRepeatable = attrData.getString( "attributeSingleOrRepeatable" ).
					equals( Attribute.cardinalityRepeatable );
			
			// add the attribute to the array list (id and name)
			attributes.add ( new Property( attrId, attrName, attrRepeatable ) );
			
		}  // end attr while
		
		return attributes;
	}

	
	/**
	 * Get all the hierarchies (id,code) from the sheet
	 * @param attrData
	 * @param catalogueAttr => Do we want catalogue attributes or the other attributes?
	 * @return
	 */

	private ArrayList<Property> getHierarchies( ResultDataSet hierData ) {
		
		// output
		ArrayList<Property> hierarchies = new ArrayList<>();
		
		// for each hierarchy
		while ( hierData.next() ) {
			
			// get the hierarchy code
			String code = hierData.getString ( "code" );
			
			// skip if no attr code
			if ( code.isEmpty() )
				continue;
			
			int id;
			
			// get the hierarchy id using the hierarchy code
			if ( this.local )
				id = hierIds.get( localCat.getCode() );
			else
				id = hierIds.get( code );
			
			// add the attribute to the array list (id and code)
			hierarchies.add ( new Property( id, code ) );
			
		}  // end while
		
		return hierarchies;
	}
	
	
	/**
	 * Class which models a record with only ID and key
	 * @author avonva
	 *
	 */
	private class Property {
		
		private int id;
		private String key;
		private boolean repeatable;
		
		public Property( int id, String key, boolean repeatable ) {
			this.id = id;
			this.key = key;
			this.repeatable = repeatable;
		}
		
		public Property( int id, String key ) {
			this.id = id;
			this.key = key;
			this.repeatable = false;
		}
		
		public int getId() {
			return id;
		}
		public String getKey() {
			return key;
		}
		public boolean isRepeatable() {
			return repeatable;
		}
	}
}
