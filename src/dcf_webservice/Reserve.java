package dcf_webservice;

import javax.xml.soap.SOAPException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import catalogue.Catalogue;
import dcf_manager.Dcf.DcfType;
import dcf_pending_action.PendingReserve;
import soap.UploadCatalogueFile;
import user.IDcfUser;

/**
 * Class used to reserve/unreserve catalogues through the dcf web service.
 * @author avonva
 *
 */
public class Reserve extends UploadCatalogueFile {
	
	private static final Logger LOGGER = LogManager.getLogger(Reserve.class);
	
	private DcfType type;
	private IDcfUser user;

	/**
	 * Initialize the reserve with the DcfType we want
	 * to work with
	 * @param type
	 */
	public Reserve(IDcfUser user, DcfType type) {
		super(user);
		this.user = user;
		this.type = type;
	}
	
	/**
	 * Reserve a catalogue with a major or minor reserve operation or unreserve it. 
	 * An additional description on why we reserve the catalogue is mandatory.
	 * Set reserve level to None to unreserve the catalogue.
	 * @param catalogue the catalogue we want to (un)reserve
	 * @param reserveLevel the reserve level we want to set (none, minor, major)
	 * @param reserveDescription a description of why we are (un)reserving
	 * @return the pending reserve which contains all the information related
	 * to this reserve request
	 * @throws SOAPException 
	 */
	public PendingReserve reserve(Catalogue catalogue, ReserveLevel reserveLevel, 
			String reserveDescription) throws SOAPException {
		
		LOGGER.info (reserveLevel.getOp() + ": " + catalogue);
		
		String attachment = UploadMessages.getReserveMessage(
				catalogue.getCode(), reserveLevel, reserveDescription);

		// send the request and get back the log code
		String logCode = this.send(attachment);
		
		// add a pending reserve object to the db
		// to save the request
		PendingReserve pr = PendingReserve.addPendingReserve(catalogue,
				logCode, user.getUsername(), reserveDescription, reserveLevel, type);
		
		return pr;
	}
}
