package dcf_log;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import catalogue.Catalogue;
import dcf_webservice.DcfResponse;
import utilities.GlobalUtil;

/**
 * Builder to create a {@link DcfLog} step by step. This
 * is useful while parsing the .xml log document, in order
 * to create it step by step.
 * @author avonva
 *
 */
public class DcfLogBuilder {

	private String action;
	private Timestamp transmissionDate;
	private Timestamp processingDate;
	private String uploadedFilename;
	private String catalogueCode;
	private String catalogueVersion;
	private String catalogueStatus;
	private String macroOpName;
	private DcfResponse macroOpResult;
	private Collection<String> macroOpLogs;
	private Collection<LogNode> logNodes;
	private Collection<LogNode> validationErrors;
	
	/**
	 * Initialize the dcf log builder memory
	 */
	public DcfLogBuilder() {
		macroOpLogs = new ArrayList<String>();
		logNodes = new ArrayList<LogNode>();
		validationErrors = new ArrayList<LogNode>();
	}
	
	/**
	 * Get the timestamp contained in the string
	 * @param date
	 * @return
	 */
	private Timestamp getTimestamp ( String date ) {
		
		Timestamp ts = null;
		try {
			ts = GlobalUtil.getTimestampFromString( date, Catalogue.ISO_8601_24H_FULL_FORMAT );
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return ts;
	}

	public void setAction(String action) {
		this.action = action;
	}
	public void setTransmissionDate(String transmissionDate) {
		this.transmissionDate = getTimestamp ( transmissionDate );
	}
	public void setProcessingDate(String processingDate) {
		this.processingDate = getTimestamp ( processingDate );
	}
	public void setUploadedFilename(String uploadedFilename) {
		this.uploadedFilename = uploadedFilename;
	}
	public void setCatalogueCode(String catalogueCode) {
		this.catalogueCode = catalogueCode;
	}
	public void setCatalogueVersion(String catalogueVersion) {
		this.catalogueVersion = catalogueVersion;
	}
	public void setCatalogueStatus(String catalogueStatus) {
		this.catalogueStatus = catalogueStatus;
	}
	public void setMacroOpName(String macroOpName) {
		this.macroOpName = macroOpName;
	}
	public void setMacroOpResult(String macroOpResult) {
	    try {
	    	this.macroOpResult = DcfResponse.valueOf( macroOpResult );
	    } catch (IllegalArgumentException e) {
	    	this.macroOpResult = DcfResponse.ERR;
	    }
	}
	public void setMacroOpLogs(Collection<String> macroOpLogs) {
		this.macroOpLogs = macroOpLogs;
	}
	public void setLogNodes(Collection<LogNode> logNodes) {
		this.logNodes = logNodes;
	}
	public void addMacroOpLog(String macroOpLog) {
		this.macroOpLogs.add( macroOpLog );
	}
	public void addLogNode( LogNode node ) {
		this.logNodes.add( node );
	}
	public void addValidationErrorNode ( LogNode node ) {
		this.validationErrors.add( node );
	}

	
	/**
	 * Build the log document
	 * @return
	 */
	public DcfLog build() {
		return new DcfLog(action, transmissionDate, processingDate, uploadedFilename, 
				catalogueCode, catalogueVersion, catalogueStatus, macroOpName, 
				macroOpResult, macroOpLogs, logNodes, validationErrors);
	}
}
