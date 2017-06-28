package dcf_user;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import dcf_manager.Dcf;
import import_catalogue.ImportCatalogueThread;
import import_catalogue.ImportCatalogueThread.ImportFileFormat;
import utilities.GlobalUtil;

/**
 * Class to ask to the DCF the users access level. In particular:
 * if the user username appears in the catusers catalogue then the user
 * can edit the related catalogues which are listed as attribute. Otherwise,
 * the user can only read the catalogue
 * @author avonva
 */

public class UserProfileChecker extends Thread {

	private static final String EDITABLE_CATALOGUE_ATTRIBUTE_NAME = "editCat";
	private Listener doneListener;
	private Listener errorListener;

	@Override
	public void run() {
		setAccessLevel();
	}

	/**
	 * Listener called if no attachment is found for the
	 * cat users catalogue
	 * @param errorListener
	 */
	public void addErrorListener(Listener errorListener) {
		this.errorListener = errorListener;
	}

	/**
	 * listener called when the thread finished its work
	 * @param doneListener
	 */
	public void addDoneListener ( Listener doneListener ) {
		this.doneListener = doneListener;
	}
	/**
	 * Set the user access level for the current user, that is, if he can modify
	 * catalogues and which catalogues he can modify
	 * @param filename
	 * @param doneListener
	 */
	public void setAccessLevel() {

		System.out.println( "Checking user access level..." );

		final String filename = GlobalUtil.getTempDir() + "catUsersCatalogue.xml";

		// ask for exporting catalogue to the dcf
		// download the cat users catalogue to check 
		// the access level of the user
		Dcf dcf = new Dcf();
		boolean success;
		try {
			success = dcf.exportCatalogue( 
					Catalogue.getCatUsersCatalogue(), filename );
		} catch (SOAPException e) {
			
			success = false;
			
			// call error listener if no catalogue is found
			if ( errorListener != null )
				errorListener.handleEvent( null );
		}

		// if failed to download => return we are a data provider! we were not
		// able to download this file
		if ( !success ) {

			System.out.println ( "User access level: data provider" );

			// set the current user as data provider
			User user = User.getInstance();
			user.setUserLevel( UserAccessLevel.DATA_PROVIDER );

			if ( doneListener != null )
				doneListener.handleEvent( new Event() );

			return;
		}

		// import the catusers catalogue
		ImportCatalogueThread importCat = new ImportCatalogueThread(
				filename, ImportFileFormat.XML );
		
		importCat.addDoneListener( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {

				System.out.println ( "User access level: catalogue manager" );

				User user = User.getInstance();

				// update the editable catalogues
				// of the user based on its username
				user.setEditableCat( getEditableCataloguesCodes() );

				// set the current user as catalogue manager
				user.setUserLevel( UserAccessLevel.CATALOGUE_MANAGER );

				try {
					GlobalUtil.deleteFileCascade( new File( filename ) );
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if ( doneListener != null )
					doneListener.handleEvent( arg0 );
			}
		});
		
		importCat.start();
	}


	/**
	 * Get all the editable catalogues codes
	 * for the current catalogue manager user
	 * @return
	 */
	private ArrayList<String> getEditableCataloguesCodes () {

		CatalogueDAO catDao = new CatalogueDAO();

		// get the users catalogue with all the correct information
		// (we fetch it since using Catalogue.getCatUsersCatalogue
		// we have only the catalogue code and version)
		Catalogue users = catDao.getLastVersionByCode(
				Catalogue.getCatUsersCatalogue().getCode(), 
				Dcf.dcfType );

		// load the catalogue data into RAM
		users.loadData();

		// get the attribute related to the access levels
		TermAttribute editCatAttr = getEditCatAttribute( users );

		// return all the catalogues codes in a list
		return editCatAttr.getRepeatableValues();
	}

	/**
	 * Get the catalogue attribute related to 
	 * the user access levels
	 * @param catalogue the catalogue containing the user
	 * access level attribute
	 * @return
	 */
	private TermAttribute getEditCatAttribute ( Catalogue catalogue ) {

		TermAttribute editCat = null;

		TermDAO termDao = new TermDAO( catalogue );

		User user = User.getInstance();

		// get the term related to the current user
		// using its username
		Term userTerm = termDao.getByName( user.getUsername() );

		// initialize term attribute dao
		TermAttributeDAO taDao = new TermAttributeDAO( catalogue );

		// we get the term attributes related to the user
		// because we want to discover which catalogues
		// can be modified by this user
		ArrayList<TermAttribute> tas = taDao.getByA1( userTerm );

		// for each attribute of the term
		for ( TermAttribute ta : tas ) {

			// if the attribute is indeed the cat users attribute
			if ( ta.getAttribute().getName().equals( EDITABLE_CATALOGUE_ATTRIBUTE_NAME ) ) {

				editCat = ta;
				break;
			}
		}

		return editCat;
	}
}
