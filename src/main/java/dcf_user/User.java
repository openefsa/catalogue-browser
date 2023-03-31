package dcf_user;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.AttachmentNotFoundException;
import catalogue.Catalogue;
import catalogue.ReservedCatalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_browser_dao.ForceCatEditDAO;
import catalogue_browser_dao.ReservedCatDAO;
import config.Config;
import dcf_manager.Dcf;
import pending_request.DcfPendingRequestsList;
import pending_request.IDcfPendingRequestsList;
import pending_request.IPendingRequest;
import pending_request.PendingRequestDao;
import sas_remote_procedures.XmlUpdateFile;
import sas_remote_procedures.XmlUpdateFileDAO;
import soap.DetailedSOAPException;
import soap.UploadCatalogueFileImpl;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import soap.UploadCatalogueFilePersistentImpl;
import ui_main_panel.BrowserPendingRequestWorker;
import ui_main_panel.FormOpenapiLogin;
import user.DcfUser;

/**
 * Class to model the application user. Note that this class follows the
 * singleton design pattern since we can have only an user using the local
 * application and it is a global concept in the software.
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class User extends DcfUser {

	private static final Logger LOGGER = LogManager.getLogger(User.class);

	// inner instance
	private static User user;

	private ArrayList<String> editableCat;
	private UserAccessLevel userLevel;

	private Collection<UserListener> userLevelListeners;

	/**
	 * Is the user logged into the application?
	 */
	private boolean logged;
	private boolean isReauth;
	private boolean loggedOpenapi;

	private String[] credentials;

	/**
	 * Private constructor
	 */
	private User() {
		this.logged = false;
		this.loggedOpenapi = false;
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
		if (user == null)
			user = new User();

		return user;
	}

	/**
	 * Set the editable catalogues codes for the this user
	 * 
	 * @param editableCat
	 */
	public void setEditableCat(ArrayList<String> editableCat) {
		this.editableCat = editableCat;
	}

	/**
	 * Get the catalogues which can be edited by this user. Note this check is only
	 * to see which catalogues the user can reserve and it is not supposed to be
	 * used as check if we can directly edit the catalogues or not.
	 * 
	 * @return
	 */
	public ArrayList<String> getEditableCat() {
		return editableCat;
	}

	/**
	 * Check if the user is a catalogue manager of the catalogue. If so, the user is
	 * potentially able to reserve the catalogue and make editing operations, unless
	 * the catalogue is not reserved by another user.
	 * 
	 * @param catalogue the catalogue we want to reserve
	 * @return true if the user is a catalogue manager of this catalogue
	 */
	public boolean isCatManagerOf(Catalogue catalogue) {
		return isCatManager() && (editableCat.contains("ALL") || editableCat.contains(catalogue.getCode()));
	}

	/**
	 * Check if the current user has reserved the catalogue
	 * 
	 * @param catalogue
	 * @return
	 */
	public boolean hasReserved(String catalogueCode) {

		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue last = catDao.getLastVersionByCode(catalogueCode, Dcf.dcfType);

		if (last == null)
			return false;

		ReservedCatDAO resDao = new ReservedCatDAO();

		ReservedCatalogue rc = resDao.getById(last.getId());

		// if not present the catalogue is not reserved
		if (rc == null)
			return false;

		// check that the user who reserved the catalogue
		// is the one passed in input
		return rc.getUsername().equals(this.getUsername());
	}

	/**
	 * Check if the user has some active {@link IPendingRequest} for the chosen
	 * catalogue
	 * 
	 * @param catalogue
	 * @return
	 */
	public boolean hasPendingRequestsFor(Catalogue catalogue) {

		String dbUrl = DatabaseManager.createMainDBURL();

		PendingRequestDao<IPendingRequest> dao = new PendingRequestDao<>(dbUrl);

		UploadCatalogueFilePersistentImpl ucf = new UploadCatalogueFilePersistentImpl(dao);

		IDcfPendingRequestsList<IPendingRequest> output = new DcfPendingRequestsList();
		try {
			ucf.getUserPendingRequests(this, output);
		} catch (SQLException | IOException e) {
			LOGGER.error("Cannot retrieve information related to pending requests", e);
			e.printStackTrace();
			return false;
		}

		for (IPendingRequest req : output) {
			String catCode = req.getData().get(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY);
			if (catalogue.getCode().equals(catCode))
				return true;
		}

		return false;
	}

	/**
	 * Enum used to get the specific problem which is blocking a reserve action on
	 * this catalogue
	 * 
	 * @author avonva
	 *
	 */
	public enum CatalogueStatus {
		OK, INV_VERSION, WAITING_PENDING_REQ, RESERVED_BY_CURRENT, RESERVED_BY_OTHER, NOT_LAST_VERSION, LOCAL,
		DEPRECATED, RESERVED_FORCED
	};

	/**
	 * Get the status of the catalogue
	 * 
	 * @return
	 */
	public CatalogueStatus checkCatalogue(Catalogue catalogue) {

		CatalogueStatus problem = CatalogueStatus.OK;

		boolean editingForced = hasForcedReserveOf(catalogue) != null;

		ReservedCatDAO dao = new ReservedCatDAO();
		boolean isReserved = dao.getById(catalogue.getId()) != null;
		boolean reservedByCurrent = hasReserved(catalogue.getCode());

		// if the catalogue is reserved by someone which is not me
		// then we cannot reserve
		boolean reservedByOther = !reservedByCurrent && isReserved;

		// no problem if no user had reserved the catalogue
		// and the catalogue is the last available
		// version of the catalogue and the catalogue
		// is not local and it is not deprecated
		if (catalogue.getCatalogueVersion().isInvalid())
			problem = CatalogueStatus.INV_VERSION;
		else if (hasPendingRequestsFor(catalogue))
			problem = CatalogueStatus.WAITING_PENDING_REQ;
		else if (reservedByOther)
			problem = CatalogueStatus.RESERVED_BY_OTHER;
		else if (reservedByCurrent)
			problem = CatalogueStatus.RESERVED_BY_CURRENT;
		else if (catalogue.hasUpdate())
			problem = CatalogueStatus.NOT_LAST_VERSION;
		else if (catalogue.isLocal())
			problem = CatalogueStatus.LOCAL;
		else if (catalogue.isDeprecated())
			problem = CatalogueStatus.DEPRECATED;
		else if (editingForced)
			problem = CatalogueStatus.RESERVED_FORCED;

		return problem;
	}

	/**
	 * Check if the user can reserve the catalogue
	 * 
	 * @param catalogue
	 * @return
	 */
	public boolean canReserve(Catalogue catalogue) {
		return isCatManagerOf(catalogue) && checkCatalogue(catalogue) == CatalogueStatus.OK;
	}

	/**
	 * Check if the user can unreserve the catalogue
	 * 
	 * @param catalogue
	 * @return
	 */
	public boolean canUnreserve(Catalogue catalogue) {
		return isCatManagerOf(catalogue) && checkCatalogue(catalogue) == CatalogueStatus.RESERVED_BY_CURRENT;
	}

	/**
	 * Check if the user can create the .xlsx changes file in the server
	 * 
	 * @param catalogue
	 * @return
	 */
	public boolean canCreateChangesFile(Catalogue catalogue) {
		return isCatManagerOf(catalogue) && checkCatalogue(catalogue) == CatalogueStatus.RESERVED_BY_CURRENT;
	}

	/**
	 * Check if the user can upload the .xml changes file in dcf
	 * 
	 * @param catalogue
	 * @return
	 */
	public boolean canUploadXmlChangesFile(Catalogue catalogue) {

		XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
		XmlUpdateFile xml = xmlDao.getById(catalogue.getId());

		return xml != null && isCatManagerOf(catalogue)
				&& checkCatalogue(catalogue) == CatalogueStatus.RESERVED_BY_CURRENT;
	}

	/**
	 * Check if the user can currently edit this catalogue or not
	 * 
	 * @param catalogue
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public boolean canEdit(Catalogue catalogue) {

		boolean hasPendingRequest = hasPendingRequestsFor(catalogue);

		// is the catalogue reserved? (i.e. is it reserved
		// by this user and the dcf is not reserving)
		boolean isReserved = hasReserved(catalogue.getCode());

		// is the user a cm of this catalogue?
		boolean isCM = isCatManagerOf(catalogue);

		// is editing forced to be enabled?
		boolean editingForced = hasForcedReserveOf(catalogue) != null;

		boolean isLocal = catalogue.isLocal();

		boolean isInvalidVersion = catalogue.getCatalogueVersion().isInvalid();

		// we can edit the catalogue only if we are a catalogue
		// manager of that catalogue and if we have reserved the
		// catalogue with our username or we have the forced editing
		// we cannot edit a catalogue if the catalogue is being
		// reserved but the operation is not finished
		// or we can edit the catalogue if it is a local catalogue
		boolean editable = isInvalidVersion || isLocal
				|| (isCM && ((isReserved && !hasPendingRequest) || editingForced));

		return editable;
	}

	public ReserveLevel hasForcedReserveOf(Catalogue catalogue) {
		ForceCatEditDAO forcedDao = new ForceCatEditDAO();
		ReserveLevel forcedLevel = forcedDao.getEditingLevel(catalogue, this.getUsername());
		return forcedLevel;
	}

	/**
	 * Get edit level on the catalogue
	 * 
	 * @param catalogue
	 * @return
	 */
	public ReserveLevel getEditLevelOf(Catalogue catalogue) {

		ReserveLevel level = null;

		ForceCatEditDAO forcedDao = new ForceCatEditDAO();
		ReservedCatDAO resDao = new ReservedCatDAO();

		ReserveLevel forcedLevel = forcedDao.getEditingLevel(catalogue, this.getUsername());

		if (forcedLevel == null) {
			ReservedCatalogue reservedCat = resDao.getById(catalogue.getId());

			if (reservedCat != null)
				level = reservedCat.getLevel();
		} else {
			level = forcedLevel;
		}

		return level;
	}

	/**
	 * Check if the user access level was retrieved or not (i.e. if we know if the
	 * user is a catalogue manager or a data provider)
	 * 
	 * @return
	 */
	public boolean isUserLevelDefined() {
		return userLevel != null;
	}

	/**
	 * Remove the user instance
	 * 
	 * @author shahaal
	 * @return
	 */
	public void removeUser() {
		user = null;
	}

	/**
	 * Set the user access level of this user
	 * 
	 * @param userLevel
	 */
	public void setUserLevel(UserAccessLevel userLevel) {
		this.userLevel = userLevel;

		for (UserListener l : userLevelListeners) {
			l.userLevelChanged(userLevel);
		}
	}

	/**
	 * Get the user level if it was defined
	 * 
	 * @return
	 */
	public UserAccessLevel getUserLevel() {
		return userLevel;
	}

	/**
	 * Get if we are searching the user access level in this moment.
	 * 
	 * @return
	 */
	public boolean isGettingUserLevel() {

		// true if we don't know the user level but we
		// already know the username
		return !isUserLevelDefined() && super.getUsername() != null;
	}

	/**
	 * Set the user as logged or not logged into the dcf
	 * 
	 * @param logged
	 */
	public void setLogged(boolean logged) {
		this.logged = logged;

		for (UserListener l : userLevelListeners)
			l.connectionChanged(logged);
	}

	/**
	 * Set the user as logged from openapi or not if yes then disable the log in
	 * from dcf
	 * 
	 * @param logged
	 */
	public void setLoggedOpenApi(boolean logged) {
		this.loggedOpenapi = logged;

		for (UserListener l : userLevelListeners)
			l.connectionChanged(logged);
	}

	/**
	 * Check if the user is logged into the application. Note that there could be a
	 * short period of time during which the user is logged into the application but
	 * its user level is not defined yet.
	 * 
	 * @return
	 */
	public boolean isLoggedIn() {
		return logged;
	}

	/**
	 * Check if the user is logged into the application using the openapi portal.
	 * Note that could be that for a short time the user is logged but its user
	 * level is not defined yet.
	 * 
	 * @return
	 */
	public boolean isLoggedInOpenAPI() {
		return loggedOpenapi;
	}

	/**
	 * Check if the user is being reauthenticated
	 * 
	 * @return
	 */
	public boolean isReauth() {
		return isReauth;
	}

	/**
	 * Check if the current user is a catalogue manager or not.
	 * 
	 * @return
	 */
	public boolean isCatManager() {
		return userLevel != null && userLevel == UserAccessLevel.CATALOGUE_MANAGER;
	}

	public boolean areCredentialsStored() {

		// cannot reauthenticate without saved credentials
		String[] credentials = this.getSavedCredentials();

		return credentials != null;
	}

	/**
	 * start al the pending requests of the user
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void startPendingRequests() throws SQLException, IOException {

		String dbUrl = DatabaseManager.createMainDBURL();

		PendingRequestDao<IPendingRequest> dao = new PendingRequestDao<>(dbUrl);

		UploadCatalogueFilePersistentImpl uploadCatFile = new UploadCatalogueFilePersistentImpl(dao);

		IDcfPendingRequestsList<IPendingRequest> output = new DcfPendingRequestsList();
		uploadCatFile.getUserPendingRequests(this, output);

		for (IPendingRequest request : output)
			BrowserPendingRequestWorker.getInstance().startPendingRequests(request);
	}

	/**
	 * Use stored credentials to perform a login without reasking the user for
	 * username and password
	 * 
	 * @return
	 * @throws SOAPException
	 */
	public boolean reauthenticateWithDCF() throws SOAPException {

		// return if credentials are empty
		if (credentials == null)
			return false;

		LOGGER.info("Reauthenticating user " + credentials[0]);

		this.isReauth = true;

		try {
			setLogged(super.verifiedLogin(Config.getEnvironment(), credentials[0], credentials[1]));
		} catch (SOAPException e) {
			LOGGER.error("Error during authentication with dcf ", e);
			e.printStackTrace();
			
			this.isReauth = false;
			super.logout(); // connection error only!
			throw e;
		}

		// delete not valid credentials
		if (!logged) {
			this.deleteCredentials();
			super.logout();
		}

		this.isReauth = false;

		return logged;
	}

	/**
	 * Use stored credentials to perform a login without re-asking the user token
	 * 
	 * TODO the method is not used since the starter program has a limitation of 5
	 * calls/minutes instead when making a call if receiving error then notify the
	 * user
	 * 
	 * @author shahaal
	 * @return
	 * @throws SOAPException
	 */
	public boolean reauthenticateWithOpenAPI() throws SOAPException {

		// return if credentials are empty
		if (credentials == null)
			return false;

		LOGGER.info("Reauthenticating user in openapi portal " + credentials[0]);

		this.isReauth = true;

		try {
			String catUsers = Catalogue.getCatUsersCatalogue().getCode();
			setLoggedOpenApi(
					super.verifiedLoginOpenapi(Config.getEnvironment(), credentials[0], credentials[1], catUsers));
		} catch (SOAPException e) {
			LOGGER.error("Error during authentication with open api ", e);
			e.printStackTrace();
			
			this.isReauth = false;
			super.logout(); // connection error only!
			throw e;
		}

		// delete not valid credentials
		if (!loggedOpenapi) {
			this.deleteCredentials();
			super.logout();
		}

		this.isReauth = false;

		return loggedOpenapi;
	}

	/**
	 * Login to the dcf using the username and password passed in input.
	 * 
	 * @return true if logged in successfully, otherwise false
	 * @throws SOAPException
	 * @throws Exception
	 */
	public boolean loginWithDcf(String username, String password, boolean save) throws DetailedSOAPException {

		setLogged(super.verifiedLogin(Config.getEnvironment(), username, password));

		// if wrong credential => remove them
		if (this.logged) {

			LOGGER.info(username + " successfully logged in to dcf");

			// delete information on the old account
			if (areCredentialsStored())
				deleteCredentials();

			if (save)
				saveCredentials(username, password);
		}

		return this.logged;
	}

	/**
	 * Login with the openapi portal using the (dft) usrname and token by
	 * downloading the dump catalogue CATUSERS
	 * 
	 * @return true if logged in successfully, otherwise false
	 * @throws DetailedSOAPException
	 * @throws AttachmentNotFoundException
	 * @throws SOAPException
	 * @throws Exception
	 */
	public boolean loginWithOpenapi(String username, String token, boolean save) throws DetailedSOAPException {

		// code of the catusers catalogue
		String catUsers = Catalogue.getCatUsersCatalogue().getCode();

		setLoggedOpenApi(super.verifiedLoginOpenapi(Config.getEnvironment(), username, token, catUsers));

		if (this.loggedOpenapi) {

			LOGGER.info(username + " successfully logged into the portal.");

			// delete information on the old account
			if (areCredentialsStored())
				deleteCredentials();

			if (save)
				saveCredentials(username, token);
		}

		return this.loggedOpenapi;
	}
	
	/**
	 * get openapi token
	 * 
	 * @author shahaal
	 * @return
	 */
	public boolean getOpeanAPICredentials() {
		// get the credentials
		this.credentials = this.getSavedCredentials();
		if (this.credentials == null)
			return false;

		// if the username is equal to Opena api Guest
		return this.credentials[0].equals(FormOpenapiLogin.getOapiusr());
	}

	/**
	 * get DCF saved credentials
	 * 
	 * @return
	 */
	private String[] getSavedCredentials() {

		String[] out = null;
		String query = "select * from APP.USERS where DCF_TYPE = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query)) {

			stmt.setString(1, Dcf.dcfType.toString());

			try (ResultSet rs = stmt.executeQuery();) {

				if (rs.next())
					out = new String[] { rs.getString("USERNAME"), rs.getString("PASSWORD") };

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("Cannot retrieve saved credentials", e);
			e.printStackTrace();
		}

		return out;
	}

	public void deleteCredentials() {
		
		LOGGER.info("Delete Credentials");

		String query = "delete from APP.USERS where DCF_TYPE = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, Dcf.dcfType.toString());
			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("Cannot delete user credentials", e);
			e.printStackTrace();
		}
	}

	private void saveCredentials(String username, String password) {
		
		LOGGER.info("Save Credentials");

		String query = "insert into APP.USERS (DCF_TYPE, USERNAME, PASSWORD) values (?, ?, ?)";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, Dcf.dcfType.toString());
			stmt.setString(2, username);
			stmt.setString(3, password);

			stmt.execute();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("Cannot save user credentials", e);
			e.printStackTrace();
		}
	}

	/**
	 * Add an user listener
	 * 
	 * @param listener
	 */
	public void addUserListener(UserListener listener) {
		userLevelListeners.add(listener);
	}

	@Override
	public String toString() {
		return "USER: " + getUsername();
	}
}
