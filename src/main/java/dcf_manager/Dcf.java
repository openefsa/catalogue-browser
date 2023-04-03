package dcf_manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue.CataloguesList;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_generator.ThreadFinishedListener;
import config.Config;
import config.Environment;
import data_collection.DCTable;
import data_collection.DCTableList;
import data_collection.DataCollection;
import data_collection.DataCollectionsList;
import dcf_user.User;
import dcf_user.UserAccessLevel;
import dcf_user.UserProfileChecker;
import progress_bar.FormProgressBar;
import soap.DetailedSOAPException;
import soap.ExportCatalogueFile;
import soap.GetCataloguesList;
import soap.GetDataCollectionTables;
import soap.GetDataCollectionsList;
import soap.Ping;
import utilities.GlobalUtil;

/**
 * Class to model the DCF. Here we can download catalogues and perform web
 * service operations.
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class Dcf {

	private static final Logger LOGGER = LogManager.getLogger(Dcf.class);

	// get the dcf type and store it for the whole program
	public static final DcfType dcfType = Config.isProductionEnvironment() ? DcfType.PRODUCTION : DcfType.TEST;

	/**
	 * List of downloaded data collections
	 */
	private static ArrayList<DataCollection> dataCollections = null;

	/**
	 * A list which contains all the published dcf catalogues
	 */
	private static ArrayList<Catalogue> catalogues = null;

	/**
	 * True if we are currently getting catalogue updates false otherwise
	 */
	private static boolean gettingUpdates = false;

	/**
	 * Enumerator to identify the dcf as test or production type.
	 * 
	 * @author avonva
	 *
	 */
	public enum DcfType {
		PRODUCTION, TEST, LOCAL; // if we are using a local catalogue, a "local" dcf

		// TODO to be removed
		public static DcfType fromEnvironment(Environment env) {
			return env == Environment.PRODUCTION ? PRODUCTION : TEST;
		}
	}

	// progress bar
	private FormProgressBar progressBar;

	/**
	 * Get all the catalogues which can be downloaded from the dcf
	 * 
	 * @return
	 */
	public static ArrayList<Catalogue> getCatalogues() {
		return catalogues;
	}

	/**
	 * Get a catalogue from the official catalogues list by its code
	 * 
	 * @param code
	 * @return
	 */
	public static Catalogue getCatalogueByCode(String code) {
		for (Catalogue cat : catalogues) {
			if (cat.getCode().equals(code))
				return cat;
		}

		return null;
	}

	/**
	 * Get all the dc which were downloaded
	 * 
	 * @return
	 */
	public static ArrayList<DataCollection> getDataCollections() {
		return dataCollections;
	}

	/**
	 * Get the list of downloadable data collections
	 * 
	 * @return
	 */
	public static ArrayList<DataCollection> getDownloadableDC() {

		ArrayList<DataCollection> out = new ArrayList<>();

		if (dataCollections == null)
			return out;

		for (DataCollection dc : dataCollections) {
			if (dc.isActive() && !dc.alreadyImported())
				out.add(dc);
		}

		return out;
	}

	/**
	 * Get the codes of the catalogues which need to be updated or are new compared
	 * to the ones that I already have downloaded in my pc.
	 * 
	 * A list which contains all the catalogues which can be downloaded compared to
	 * the ones we have already downloaded in our local machine. This means that
	 * this list contains all the catalogues we don't have and the catalogues which
	 * need an update compared to the version we have in our machine.
	 * 
	 * @return
	 */
	public static ArrayList<Catalogue> getDownloadableCat() {

		// list of catalogues from which the user can select
		// we want them to be only the catalogues which have not been downloaded yet
		// or catalogues updates
		ArrayList<Catalogue> catalogueToShow = new ArrayList<>();

		if (catalogues == null || catalogues.isEmpty())
			return catalogueToShow;

		CatalogueDAO catDao = new CatalogueDAO();

		// get the catalogues which are currently
		// present into the user database
		// at their last release status!
		ArrayList<Catalogue> myCatalogues = catDao.getMyCatalogues(dcfType);

		// Check for each official catalogues
		// if we already have it downloaded or not
		for (Catalogue cat : catalogues) {

			boolean addCat = true;

			// for each already downloaded catalogue
			for (Catalogue myCat : myCatalogues) {

				// skip if same code and version (i.e.,
				// the same catalogue is already downloaded)
				if (myCat.sameAs(cat)) {
					addCat = false;
					continue;
				}
			}

			// if we can add the catalogue add it
			if (addCat)
				catalogueToShow.add(cat);
		}

		// sort catalogues by label and version
		Collections.sort(catalogueToShow);

		return catalogueToShow;
	}

	/**
	 * Is the application getting the catalogues updates from the dcf?
	 * 
	 * @return
	 */
	public static boolean isGettingUpdates() {
		return gettingUpdates;
	}

	/**
	 * Are the catalogue meta data being downloaded now?
	 * 
	 * @param gettingUpdates
	 */
	public static void setGettingUpdates(boolean gettingUpdates) {
		Dcf.gettingUpdates = gettingUpdates;
	}

	/**
	 * Get the last release of a catalogue
	 * 
	 * @param catalogue
	 * @return
	 */
	public static Catalogue getLastPublishedRelease(Catalogue catalogue) {

		if (catalogues == null) {
			return null;
		}

		// get the catalogue in the dcf list
		// using only its code
		int index = catalogues.indexOf(catalogue);

		// if not found
		if (index == -1)
			return null;

		return catalogues.get(index);
	}

	/**
	 * Refresh the catalogues of the dcf downloading their meta data. Refresh also
	 * the downloadable catalogues.
	 */
	public void refreshCatalogues() {

		// flag to say that we are getting the updates
		gettingUpdates = true;

		// get all the dcf catalogues and save them
		catalogues = getCataloguesList();

		if (catalogues == null)
			return;

		// sort catalogues by label and version
		Collections.sort(catalogues);

		// we have finished to get updates
		gettingUpdates = false;
	}

	/**
	 * Refresh the data collections list in background
	 */
	public void refreshDataCollections() {

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {

				gettingUpdates = true;

				dataCollections = getDataCollectionsList();

				gettingUpdates = false;
			}
		});

		t.start();
	}

	/**
	 * Set a progress bar which is called for webservices.
	 * 
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	/**
	 * Set the progress bar title
	 * 
	 * @param title
	 */
	public void setProgressBarTitle(String title) {
		if (progressBar != null)
			progressBar.setLabel(title);
	}

	/**
	 * Make a ping to the dcf
	 * 
	 * @return true if the dcf is responding correctly
	 * @throws SOAPException
	 */
	public boolean ping() throws SOAPException {
		Ping ping = new Ping();
		return ping.ping(Config.getEnvironment(), User.getInstance());
	}

	/**
	 * Start the thread which checks the user access level (see
	 * {@link UserAccessLevel} ).
	 * 
	 * @param doneListener {@link Listener } called when the thread has finished its
	 *                     work.
	 */
	public void setUserLevel(ThreadFinishedListener doneListener) {

		// set the access level of the user
		final UserProfileChecker userLevel = new UserProfileChecker();

		userLevel.addDoneListener(doneListener);

		userLevel.setProgressBar(progressBar);

		userLevel.start();
	}

	/**
	 * Get the catalogues updates from the dcf. In particular, we download the all
	 * the published catalogues (only metadata) and refresh the dcf catalogues cache
	 * ( {@link Dcf#catalogues} )
	 * 
	 * @param doneListener
	 */
	public void checkUpdates(Listener doneListener) {

		// start downloading the catalogues updates (meta data only)
		final UpdatesChecker catUpdates = new UpdatesChecker();

		// set the listener
		catUpdates.setUpdatesListener(doneListener);

		catUpdates.start();
	}

	/**
	 * Get the list of all the dcf catalogues (only published) Note that these
	 * catalogues has as {@link DcfType} the one used in the dcf (either
	 * {@link DcfType#TEST} or {@link DcfType#PRODUCTION})
	 * 
	 * @return array list of dcf published catalogues
	 */
	public ArrayList<Catalogue> getCataloguesList() {

		CataloguesList list = new CataloguesList();

		try {
			User user = User.getInstance();

			GetCataloguesList<Catalogue> req = new GetCataloguesList<>(user.isOpeanapi());
			req.getList(Config.getEnvironment(), user, list);
		} catch (Exception e) {
			LOGGER.error("Cannot get catalogues list", e);
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * Get all the data collections related to the current user
	 * 
	 * @return
	 */
	public ArrayList<DataCollection> getDataCollectionsList() {

		DataCollectionsList list = new DataCollectionsList();

		try {
			User user = User.getInstance();

			GetDataCollectionsList<DataCollection> req = new GetDataCollectionsList<>(user.isLoggedInOpenAPI());
			req.getList(Config.getEnvironment(), user, list);

		} catch (Exception e) {

			// show the error message for OpenAPI users
			if (e instanceof DetailedSOAPException) {

				String[] warning = GlobalUtil.getSOAPWarning((DetailedSOAPException) e);
				Display display = new Display();
				GlobalUtil.showErrorDialog(new Shell(display), warning[0], warning[1]);
			}

			LOGGER.error("Cannot get data collections", e);
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * Get a resource file using its resource id
	 * 
	 * @param resourceId
	 * @return
	 * @throws SOAPException
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	public Collection<DCTable> getFile(String resourceId) throws SOAPException, IOException, XMLStreamException {

		DCTableList output = new DCTableList();

		GetDataCollectionTables<DCTable> req = new GetDataCollectionTables<>();
		req.getTables(Config.getEnvironment(), User.getInstance(), resourceId, output);

		return output;
	}

	/**
	 * Download a dcf catalogue into the local machine. The catalogue is downloaded
	 * in xml format.
	 * 
	 * @param catalogue the catalogue we want to download
	 * @param filename  the xml filename
	 * @return true if the export was successful
	 * @throws SOAPException
	 */
	public File exportCatalogue(Catalogue catalogue) throws SOAPException {
		// export the catalogue and save its attachment into an xml file
		ExportCatalogueFile export = new ExportCatalogueFile();
		return export.exportCatalogue(Config.getEnvironment(), User.getInstance(), catalogue.getCode());
	}

	/**
	 * Export a log from the dcf given its code
	 * 
	 * @param logCode the code of the log which needs to be downloaded
	 * @return the file which points to the log file
	 * @throws SOAPException
	 */
	public File exportLog(String logCode) throws SOAPException {

		// ask for the log to the dcf
		ExportCatalogueFile export = new ExportCatalogueFile();

		// write the log document in xml format
		return export.exportLog(Config.getEnvironment(), User.getInstance(), logCode);
	}

	/**
	 * Export the last internal version of a catalogue into the selected filename.
	 * If no internal version is retrieved no action is performed.
	 * 
	 * @param catalogueCode the code which identifies the catalogue we want to
	 *                      download
	 * @param filename      the filename in which we want to store the last internal
	 *                      version of the catalogue
	 * @return the file which was created
	 * @throws IOException
	 * @throws SOAPException
	 */
	public File exportCatalogueInternalVersion(String catalogueCode) throws IOException, SOAPException {

		// ask for the log to the dcf
		ExportCatalogueFile export = new ExportCatalogueFile();

		// get the catalogue xml as input stream
		File file = export.exportLastInternalVersion(Config.getEnvironment(), User.getInstance(), catalogueCode);

		return file;
	}
}
