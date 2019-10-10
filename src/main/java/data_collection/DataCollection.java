package data_collection;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_generator.CatalogueDownloader;
import dcf_manager.Dcf;
import i18n_messages.CBMessages;
import progress_bar.ProgressList;
import progress_bar.ProgressStep;
import progress_bar.ProgressStepListener;
import utilities.GlobalUtil;

/**
 * Object which models a data collection
 * @author avonva
 *
 */
public class DataCollection implements IDcfDataCollection {

	private static final Logger LOGGER = LogManager.getLogger(DataCollection.class);
	
	public static final String DATE_FORMAT = "yyyy-MM-ddX";

	private int id = -1;
	private String code;
	private String description;
	private String category;
	private Timestamp activeFrom;
	private Timestamp activeTo;
	private String resourceId;

	public DataCollection() {}
	
	/**
	 * Initialize a data collection (i.e. dc) object
	 * @param code the dc code
	 * @param description the dc description
	 * @param category
	 * @param activeFrom when the dc was started
	 * @param activeTo when the dc will end
	 * @param resourceId the resource linked to the dc (like STX)
	 */
	public DataCollection( String code, String description, 
			String category, Timestamp activeFrom, Timestamp activeTo,
			String resourceId ) {

		this.code = code;
		this.description = description;
		this.category = category;
		this.activeFrom = activeFrom;
		this.activeTo = activeTo;
		this.resourceId = resourceId;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
	public String getCode() {
		return code;
	}
	public String getDescription() {
		return description;
	}
	public String getCategory() {
		return category;
	}
	public Timestamp getActiveFrom() {
		return activeFrom;
	}
	public Timestamp getActiveTo() {
		return activeTo;
	}
	public String getResourceId() {
		return resourceId;
	}

	/**
	 * Check if the data collection is valid or not
	 * in terms of time validity
	 * @return
	 */
	public boolean isActive() {

		Timestamp today = new Timestamp( System.currentTimeMillis() );

		boolean started = activeFrom.before( today );
		boolean notOver = activeTo.after( today );

		return started && notOver;
	}

	/**
	 * Check if the data collection was already downloaded
	 * or not
	 * @return
	 */
	public boolean alreadyImported() {
		DCDAO dcDao = new DCDAO();
		return dcDao.contains( this );
	}

	/**
	 * Get a timestamp from a timestamp string using
	 * the data collection date format {@value #DATE_FORMAT}
	 * @param value
	 * @return
	 */
	public static Timestamp getTimestampFromString( String value ) {

		Timestamp ts = null;

		// convert the string to timestamp
		try {

			ts = GlobalUtil.getTimestampFromString( 
					value, DATE_FORMAT );
		}
		catch ( ParseException e ) {
			e.printStackTrace();
			LOGGER.error("Cannot parse timestamp=" + value + " using format=" + DATE_FORMAT, e);
		}

		return ts;
	}

	/**
	 * Get all the {@link DCTableConfig} related to this
	 * data collection.
	 * @return
	 */
	public Collection<DCTableConfig> getTableConfigs() {
		DCTableConfigDAO tConfDao = new DCTableConfigDAO();
		return tConfDao.getByDataCollection ( this );
	}

	/**
	 * Download the data collection:
	 * 1) insert the data collection object into the db
	 * 2) download the data collection config and insert it
	 *   into the db
	 * @throws SOAPException
	 * @throws XMLStreamException 
	 * @throws IOException 
	 */
	public void download( ProgressStepListener listener ) throws SOAPException, IOException, XMLStreamException {

		if ( listener == null )
			throw new InvalidParameterException( "Cannot set listener to null" );

		LOGGER.info( "Downloading " + this );

		// download tables
		Dcf dcf = new Dcf();
		Collection<DCTable> tables = dcf.getFile( resourceId );

		// import the data collection 
		makeImport( tables, listener );
	}

	/**
	 * Import the current data collection
	 * @param tables the data collection tables involved
	 * @param listener call back to be notified of import status
	 */
	public void makeImport( Collection<DCTable> tables, 
			ProgressStepListener listener ) {

		if ( listener == null )
			throw new InvalidParameterException( "Cannot set listener to null" );

		if ( alreadyImported() ) {
			LOGGER.warn( this + " already downloaded!" );
			return;
		}

		LOGGER.info( "Importing " + this );

		ProgressList list = new ProgressList ( 100 );
		list.addProgressListener( listener );

		// Insert the data collection in the db
		list.add( new ProgressStep( "dcInsert", 
				CBMessages.getString( "DCDownload.ImportDCStep" ) ) {

			@Override
			public void execute() throws Exception {
				DCDAO dcDao = new DCDAO();
				DataCollection.this.id = dcDao.insert( DataCollection.this );
			}
		});

		// create a progress step for each table
		for ( final DCTable table : tables ) {

			list.add( new ProgressStep( "import_" + table.getName(),
					CBMessages.getString( "DCDownload.ImportTablesStep" ) ) {

				@Override
				public void execute() throws Exception {
					table.makeImport( DataCollection.this );
				}
			});
		}

		list.start();
	}

	/**
	 * Download all the related catalogues
	 * @param listener
	 */
	public void downloadCatalogues ( ProgressStepListener listener ) {

		// second progress block for threads
		ProgressList list = new ProgressList ( 100 );
		list.addProgressListener( listener );

		// start downloading all the catalogues 
		// related to the data collection
		Collection<CatalogueDownloader> down = prepareDownloadThreads();

		for ( final CatalogueDownloader thread : down ) {

			// start the thread
			thread.start();

			// create a wait progress step for that thread
			list.add( new ProgressStep( "t_" + thread.getId(), 
					thread.getCatalogue().toString() ) {

				@Override
				public void execute() throws InterruptedException {
					while ( !thread.isFinished() ) {
						Thread.sleep( 100 );
					}
				}
			});
		}

		// start the execution of the steps
		// i.e. wait all the threads
		list.start();
	}

	/**
	 * Get all the catalogues related to this data collection
	 * @return
	 */
	public Collection<Catalogue> getCatalogues() {

		// get all the configurations related to this
		// data collection
		DCTableConfigDAO configDao = new DCTableConfigDAO();
		Collection<DCTableConfig> configs = configDao.getByDataCollection( this );

		Collection<Catalogue> catalogues = new ArrayList<>();

		// check which catalogues need to be downloaded
		for ( DCTableConfig config : configs ) {

			// get the catalogue code related to the config
			String catCode = config.getConfig().getCatalogueCode();

			// if no code skip
			if ( catCode == null )
				continue;

			// get the official catalogue using the
			// catalogue code
			Catalogue catalogue = Dcf.getCatalogueByCode ( catCode );

			// if not found skip
			if ( catalogue == null )
				continue;

			// if already inserted go on (avoid duplicates)
			if ( catalogues.contains( catalogue ) )
				continue;

			// add the catalogue to the out list
			catalogues.add( catalogue );
		}

		return catalogues;
	}

	/**
	 * Get all the catalogues related to this data collection
	 * which were not downloaded yet by the user (i.e. the missing
	 * catalogues)
	 * @return
	 */
	public Collection<Catalogue> getNewCatalogues() {

		Collection<Catalogue> catToDownload = new ArrayList<>();

		// get all the catalogues related to the dc
		Collection<Catalogue> catalogues = getCatalogues();

		// check which catalogues need to be downloaded
		for ( Catalogue cat : catalogues ) {

			// check if the catalogue was already downloaded
			// or not
			CatalogueDAO catDao = new CatalogueDAO();
			boolean present = catDao.contains( cat );

			// if already present go on
			if ( present )
				continue;

			// arrived here? then the catalogue should
			// be downloaded
			catToDownload.add( cat );
		}

		return catToDownload;
	}

	/**
	 * Start all the download processes which download the
	 * data collections catalogues
	 * @return a collection of {@link CataglogueDownloader} threads
	 * which were started
	 */
	private Collection<CatalogueDownloader> prepareDownloadThreads() {

		// get the catalogues which need to be downloaded
		Collection<Catalogue> catToDownload = getNewCatalogues();

		Collection<CatalogueDownloader> threads = new ArrayList<>();

		// download all the catalogues related to the configurations
		// if not already present in the db
		for ( Catalogue catalogue : catToDownload ) {

			// download the catalogue in a separate thread
			CatalogueDownloader downloader = new CatalogueDownloader( catalogue );
			threads.add( downloader );
		}

		return threads;
	}

	@Override
	public String toString() {
		return "DATA COLLECTION: id=" + (id == -1 ? "not defined yet" : id )
				+ ";code=" + code 
				+ ";description=" + description 
				+ ";category=" + category 
				+ ";activeFrom=" + activeFrom
				+ ";activeTo=" + activeTo
				+ ";resourceId=" + resourceId;
	}

	@Override
	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public void setActiveFrom(Timestamp activeFrom) {
		this.activeFrom = activeFrom;
	}

	@Override
	public void setActiveTo(Timestamp activeTo) {
		this.activeTo = activeTo;
	}

	@Override
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
}
