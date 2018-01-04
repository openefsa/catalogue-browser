package dcf_user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPException;

import catalogue.Catalogue;
import catalogue_browser_dao.DatabaseManager;
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
	
	private Collection<UserLevelChangedListener> userLevelListeners;

	/**
	 * Is the user logged into the application?
	 */
	private boolean logged;
	private boolean isReauth;

	/**
	 * Private constructor
	 */
	private User() {
		this.logged = false;
		this.isReauth = false;
		editableCat = new ArrayList<>();
		userLevelListeners = new ArrayList<>();
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
		
		for (UserLevelChangedListener l : userLevelListeners)
			l.userLevelChanged(userLevel);
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
	 * Check if the user is being reauthenticated
	 * @return
	 */
	public boolean isReauth() {
		return isReauth;
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

	public boolean areCredentialsStored() {
		
		// cannot reauthenticate without saved credentials
		String[] credentials = this.getSavedCredentials();
		
		return credentials != null;
	}
	
	/**
	 * Use stored credentials to perform a login without
	 * reasking the user for username and password
	 * @return
	 * @throws SOAPException 
	 */
	public boolean reauthenticate() throws SOAPException {

		// cannot reauthenticate without saved credentials
		String[] credentials = this.getSavedCredentials();
		if (credentials == null)
			return false;
		
		System.out.println("Reauthenticating user " + credentials[0]);

		this.isReauth = true;
		
		try {
			logged = this.tryPing(credentials[0], credentials[1]);
		}
		catch(SOAPException e) {
			this.isReauth = false;
			throw e;
		}

		// delete not valid credentials
		if (!logged)
			this.logout();
		
		this.isReauth = false;
		
		return logged;
	}
	/**
	 * Login to the dcf using the username and password
	 * passed in input.
	 * @return true if logged in successfully, otherwise false
	 * @throws SOAPException 
	 * @throws Exception
	 */
	public boolean login ( String username, String password, boolean save ) throws SOAPException {

		logged = tryPing(username, password);

		// if wrong credential => remove them 
		if ( logged ) {
			
			System.out.println( username + " successfully logged in to dcf");
			
			// delete information on the old account
			if (areCredentialsStored())
				logout();
			
			if (save)
				saveCredentials(username, password);
		}

		return logged;
	}

	private boolean tryPing(String username, String password) throws SOAPException {
		
		// save the credentials for future uses if the check was ok
		// we set the credential before the ping, otherwise the ping
		// has not the credential to connect to the dcf!
		this.username = username;
		this.password = password;

		// make a ping and check if the credential are ok
		Dcf dcf = new Dcf();
		
		boolean logged;
		try {
			logged = dcf.ping();
		} catch (SOAPException e) {
			e.printStackTrace();
			// check if wrong credentials
			if (e.getMessage().contains("401")
					|| e.getMessage().contains("403"))
				logged = false;
			else
				throw e;
		}

		// if wrong credential => remove them 
		if ( !logged ) {
			this.username = null;
			this.password = null;
		}

		return logged;
	}

	private String[] getSavedCredentials() {

		String[] out = null;
		String query = "select * from APP.USERS where DCF_TYPE = ?";

		try(Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query)) {
			
			stmt.setString(1, Dcf.dcfType.toString());
			
			try(ResultSet rs = stmt.executeQuery();) {

				if (rs.next())
					out = new String[] {rs.getString("USERNAME"), rs.getString("PASSWORD")};
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return out;
	}

	public void logout() {

		this.username = null;
		this.password = null;

		String query = "delete from APP.USERS where DCF_TYPE = ?";

		try(Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, Dcf.dcfType.toString());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void saveCredentials(String username, String password) {

		String query = "insert into APP.USERS (DCF_TYPE, USERNAME, PASSWORD) values (?, ?, ?)";

		try(Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, Dcf.dcfType.toString());
			stmt.setString(2, username);
			stmt.setString(3, password);

			stmt.execute();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Add a listener which will be called each time the user access level
	 * changes
	 * @param listener
	 */
	public void addUserLevelChangedListener(UserLevelChangedListener listener) {
		userLevelListeners.add(listener);
	}

	@Override
	public String toString() {
		return "USER: " + username;
	}
}
