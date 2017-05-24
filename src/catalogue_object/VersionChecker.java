package catalogue_object;

import java.sql.SQLException;

import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;

/**
 * Class used to manage the version of a catalogue. In particular,
 * we can increment the three different parts of a version (i.e.
 * major, minor and internal) in a trasparent way. In the end
 * you should call the function {@link VersionChecker#apply()}
 * to make the changes permanent in the catalogue. In particular, a new
 * database is created for the new version of the catalogue and the new
 * catalogue is returned.
 * @author avonva
 *
 */
public class VersionChecker {

	private Catalogue catalogue;           // the base catalogue
	private CatalogueVersion version;      // the version of the catalogue
	private CatalogueVersion oldVersion;   // the previous version of the catalogue
	
	/**
	 * Initialize a version checker with the catalogue
	 * we want to modify. we can change the catalogue version
	 * using this class.
	 * @param version
	 */
	public VersionChecker( Catalogue catalogue ) {
		this.catalogue = catalogue;
		
		// we use new to avoid to edit the version
		this.version = new CatalogueVersion( catalogue.getVersion() );
		this.oldVersion = new CatalogueVersion( catalogue.getVersion() );
	}

	/**
	 * Publish the major version, the minor will be
	 * set to 0 and the internal will be removed.
	 * @return the new catalogue with the new version
	 */
	public Catalogue publishMajor() {
		version.incrementMajor();
		version.confirm();
		return apply();
	}
	
	/**
	 * Publish the minor version, 
	 * the internal version will be removed.
	 * @return the new catalogue with the new version
	 */
	public Catalogue publishMinor() {
		version.incrementMinor();
		version.confirm();
		return apply();
	}
	
	/**
	 * Increment the internal version
	 * @return the new catalogue with the new version
	 */
	public Catalogue newInternalVersion() {
		version.incrementInternal();
		version.confirm();
		return apply();
	}
	
	/**
	 * Set the current version as dummy
	 * @return
	 */
	public Catalogue force() {
		
		version.force( catalogue.getForcedCount() );
		
		return apply();
	}

	/**
	 * Apply the changes to the catalogue
	 * if the version has not changed => no action
	 * is performed. If it is different indeed, a new
	 * catalogue is created with the new version and
	 * the related database is also created.
	 * @return the new catalogue with the new version
	 */
	private Catalogue apply() {
		
		String newVersion = version.getVersion();

		// if no changes at all => return the same catalogue
		if ( newVersion.equals( oldVersion ) )
			return catalogue;
		
		// clone the catalogue and set the new version
		Catalogue newVersionCat = catalogue.clone();
		newVersionCat.setCatalogueVersion( version );
		
		// set the backup db path with the old catalogue version db
		// if the old was dummy, we get as backup path its backup path
		// since the database of the dummy catalogue could be not consistent
		// for backups purposes
		if ( oldVersion.isForced() )
			newVersionCat.setBackupDbPath( catalogue.getBackupDbPath() );
		else
			newVersionCat.setBackupDbPath( catalogue.getDbFullPath() );
		
		// insert the new catalogue into the database
		// note that the db full path is also updated
		// here!
		CatalogueDAO catDao = new CatalogueDAO();
		catDao.insert( newVersionCat );

		// note that we don't create the standard
		// structure of the database since we create
		// a copy of the old catalogue!

		// copy the catalogue database in the path
		// of the new catalogue database (since we
		// have just incremented its version the new
		// catalogue is just a copy of the old one)
		//DatabaseManager.copyFolder( catalogue.getDbFullPath(), 
			//	newVersionCat.getDbFullPath() );
		
		try {
			DatabaseManager.backupCatalogue( catalogue, newVersionCat.getDbFullPath() );
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// return the new catalogue
		return newVersionCat;
	}
}
