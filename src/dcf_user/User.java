package dcf_user;

import java.util.ArrayList;

import catalogue.Catalogue;
import dcf_manager.Dcf;

/**
 * Class to model the application user. Note that this
 * class follows the singleton design pattern
 * since we can have only an user using the
 * local application and it is a global concept
 * in the software.
 * @author avonva
 *
 */
public class User {

	// inner instance
	private static User user;
	
	private String username;
	private String password;
	private ArrayList<String> editableCat;
	private UserAccessLevel userLevel;
	
	/**
	 * Is the user logged into the application?
	 */
	private boolean logged;
	
	/**
	 * Private constructor
	 */
	private User() {
		this.logged = false;
		editableCat = new ArrayList<>();
	}
	
	/**
	 * Get an instance of the current user
	 */
	public static User getInstance() {
		
		// get the instance if it is present
		// or create it otherwise
		if ( user == null )
			user = new User();
		
		return user;
	}
	
	/**
	 * Save the dcf username
	 * @param username
	 */
	public void setUsername( String username ) {
		this.username = username;
	}
	
	/**
	 * Get the saved dcf username
	 * @return
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Save the dcf password
	 * @param password the dcf password
	 */
	public void setPassword( String password ) {
		this.password = password;
	}
	
	/**
	 * Get the saved dcf password
	 * @return
	 */
	public String getPassword() {
		return password;
	}
	
	/**
	 * Set the editable catalogues codes 
	 * for the this user
	 * @param editableCat
	 */
	public void setEditableCat( ArrayList<String> editableCat ) {
		this.editableCat = editableCat;
	}
	
	/**
	 * Get the catalogues which can be edited by
	 * this user. Note this check is only to see
	 * which catalogues the user can reserve and it
	 * is not supposed to be used as check if we can
	 * directly edit the catalogues or not.
	 * @return
	 */
	public ArrayList<String> getEditableCat() {
		return editableCat;
	}
	
	/**
	 * Check if the user is a catalogue manager of the catalogue.
	 * If so, the user is potentially able to reserve the catalogue and
	 * make editing operations, unless the catalogue is not reserved
	 * by another user.
	 * @param catalogue the catalogue we want to reserve
	 * @return true if the user is a catalogue manager of this catalogue
	 */
	public boolean isCatManagerOf ( Catalogue catalogue ) {
		return isCatManager() && ( editableCat.contains( "ALL" ) || 
				editableCat.contains( catalogue.getCode() ) );
	}
	
	/**
	 * Check if the user can currently edit this catalogue or not
	 * @param catalogue
	 * @return
	 */
	public boolean canEdit ( Catalogue catalogue ) {
		
		// is the catalogue reserved? (i.e. is it reserved
		// by this user and the dcf is not reserving)
		boolean isReserved = catalogue.isReservedBy( this )
				&& !catalogue.isRequestingAction();
		
		// is the user a cm of this catalogue?
		boolean isCM = isCatManagerOf( catalogue );
		
		// is editing forced to be enabled?
		boolean editingForced = catalogue.isForceEdit( username );

		boolean isLocal = catalogue.isLocal();
		
		// we can edit the catalogue only if we are a catalogue
		// manager of that catalogue and if we have reserved the
		// catalogue with our username or we have the forced editing
		// we cannot edit a catalogue if the catalogue is being
		// reserved but the operation is not finished
		// or we can edit the catalogue if it is a local catalogue
		boolean editable = isLocal || ( isCM && ( isReserved || editingForced ) );

		return editable;
	}
	
	/**
	 * Check if the user access level was retrieved or not (i.e.
	 * if we know if the user is a catalogue manager or a data provider)
	 * @return
	 */
	public boolean isUserLevelDefined() {
		return userLevel != null;
	}
	
	/**
	 * Set the user access level of this user
	 * @param userLevel
	 */
	public void setUserLevel(UserAccessLevel userLevel) {
		this.userLevel = userLevel;
	}
	
	/**
	 * Get the user level if it was defined
	 * @return
	 */
	public UserAccessLevel getUserLevel() {
		return userLevel;
	}
	
	/**
	 * Get if we are searching the user access level in this moment.
	 * @return
	 */
	public boolean isGettingUserLevel() {
		
		// true if we don't know the user level but we
		// already know the username
		return !isUserLevelDefined() && username != null;
	}
	
	/**
	 * Set the user as logged or not logged
	 * into the dcf
	 * @param logged
	 */
	public void setLogged(boolean logged) {
		this.logged = logged;
	}
	
	/**
	 * Check if the user is logged into
	 * the application. Note that there could be
	 * a short period of time during which the user
	 * is logged into the application but its
	 * user level is not defined yet.
	 * @return
	 */
	public boolean isLogged() {
		return logged;
	}
	
	/**
	 * Check if the current user is a catalogue
	 * manager or not.
	 * @return
	 */
	public boolean isCatManager() {
		return userLevel != null && 
				userLevel == UserAccessLevel.CATALOGUE_MANAGER;
	}
	
	
	/**
	 * Login to the dcf using the username and password
	 * passed in input.
	 * @return true if logged in successfully, otherwise false
	 * @throws Exception
	 */
	public boolean login ( String username, String password ) {

		// save the credentials for future uses if the check was ok
		// we set the credential before the ping, otherwise the ping
		// has not the credential to connect to the dcf!
		this.username = username;
		this.password = password;
		
		// make a ping and check if the credential are ok
		Dcf dcf = new Dcf();
		logged = dcf.ping();
		
		// if wrong credential => remove them 
		if ( !logged ) {
			this.username = null;
			this.password = null;
		} else {
			System.out.println( username + " successfully logged in to dcf");
		}

		return logged;
	}
	
	@Override
	public String toString() {
		return "USER: " + username;
	}
}
