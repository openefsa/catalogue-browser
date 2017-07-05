package dcf_user;

import java.util.ArrayList;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_generator.CatalogueDownloader;
import catalogue_generator.ThreadFinishedListener;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import dcf_manager.Dcf;
import ui_progress_bar.FormProgressBar;

/**
 * Class to ask to the DCF the users access level. In particular:
 * if the user username appears in the catusers catalogue then the user
 * can edit the related catalogues which are listed as attribute. Otherwise,
 * the user can only read the catalogue
 * @author avonva
 */

public class UserProfileChecker extends Thread {

	private static final String EDITABLE_CATALOGUE_ATTRIBUTE_NAME = "editCat";
	private ThreadFinishedListener doneListener;
	
	private FormProgressBar progressBar;

	@Override
	public void run() {
		setAccessLevel();
	}
	
	/**
	 * Set the progress bar
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	/**
	 * listener called when the thread finished its work
	 * @param doneListener
	 */
	public void addDoneListener ( ThreadFinishedListener doneListener ) {
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

		// add progress
		if ( progressBar != null )
			progressBar.addProgress( 10 );
		
		Catalogue catUsers = Catalogue.getCatUsersCatalogue();
		
		// download and import the catusers catalogue
		CatalogueDownloader downloader = new CatalogueDownloader( catUsers );
		
		// set the progress bar for the download process
		downloader.setProgressBar( progressBar );
		
		// actions performed when downlaod is finished
		downloader.setDoneListener( new ThreadFinishedListener() {
			
			@Override
			public void finished(Thread thread, int code ) {
				
				User user = User.getInstance();
				
				switch ( code ) {

				case OK:
					// set as catalogue manager
					System.out.println ( "User access level: catalogue manager" );

					// update the editable catalogues
					// of the user based on its username
					user.setEditableCat( getEditableCataloguesCodes() );

					// set the current user as catalogue manager
					user.setUserLevel( UserAccessLevel.CATALOGUE_MANAGER );
					
					break;
				
				case ERROR:
					// set the current user as data provider
					System.out.println ( "User access level: data provider" );
					user.setUserLevel( UserAccessLevel.DATA_PROVIDER );
					break;
					
				case EXCEPTION:
					System.out.println ( "User access level: connection error" );
					break;
				}
				
				// call the super.doneListener
				if ( doneListener != null )
					doneListener.finished( UserProfileChecker.this, code );
			}
		});
		
		downloader.start();
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

		// if nothing found return empty list
		if ( editCatAttr == null )
			return new ArrayList<>();
		
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
		
		if ( userTerm == null ) {
			System.err.println( "USER " + user.getUsername() + ": Found catalogue manager account but the " 
		+ catalogue.getCode() + " permissions catalogue does not contain it. Please add this account to the "
				+ "permissions catalogue" );
			return null;
		}

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
