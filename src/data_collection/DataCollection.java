package data_collection;

import java.security.InvalidParameterException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Collection;

import javax.xml.soap.SOAPException;

import data_collection.DCDownloadListener.DownloadStep;
import dcf_manager.Dcf;
import utilities.GlobalUtil;

/**
 * Object which models a data collection
 * @author avonva
 *
 */
public class DataCollection {

	public static final String DATE_FORMAT = "yyyy-MM-ddX";
	
	private int id = -1;
	private String code;
	private String description;
	private String category;
	private Timestamp activeFrom;
	private Timestamp activeTo;
	private String resourceId;
	
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
	public boolean isValid() {
		
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
	 */
	public void download( DCDownloadListener listener ) throws SOAPException {
		
		if ( listener == null )
			throw new InvalidParameterException( "Cannot set listener to null" );
		
		System.out.println( "Downloading " + this );
		
		listener.nextStepStarted( DownloadStep.DOWNLOAD_CONFIG, 1 );
		listener.nextPhaseStarted();
		
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
			DCDownloadListener listener ) {
		
		if ( listener == null )
			throw new InvalidParameterException( "Cannot set listener to null" );
		
		if ( alreadyImported() ) {
			System.err.println( this + " already downloaded!" );
			return;
		}
		
		System.out.println( "Importing " + this );
		
		listener.nextStepStarted( DownloadStep.IMPORT_DC, 1 );
		listener.nextPhaseStarted();

		DCDAO dcDao = new DCDAO();
		this.id = dcDao.insert( this );

		listener.nextStepStarted( DownloadStep.IMPORT_TABLE, tables.size() );

		for ( DCTable table : tables ) {
			listener.nextPhaseStarted();
			table.makeImport( this );
		}
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
}
