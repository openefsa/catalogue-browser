package dcf_webservice;

import javax.xml.soap.SOAPException;

import catalogue_object.Catalogue;
import dcf_reserve_util.PendingReserve;
import dcf_user.User;

/**
 * Class used to reserve/unreserve catalogues through the dcf web service.
 * @author avonva
 *
 */
public class Reserve extends UploadCatalogueFile {

	// the catalogue we want to reserve/unreserve
	private Catalogue catalogue;
	
	// the reserve level we want to apply to the catalogue
	private ReserveLevel reserveLevel;
	
	// the description of why we are reserving/unreserving
	private String reserveDescription;

	/**
	 * Reserve a catalogue with a major or minor reserve operation or unreserve it. 
	 * An additional description on why we reserve the catalogue is mandatory.
	 * Set reserve level to None to unreserve the catalogue.
	 * @param catalogue the catalogue we want to (un)reserve
	 * @param reserveLevel the reserve level we want to set (none, minor, major)
	 * @param reserveDescription a description of why we are (un)reserving
	 * @return the pending reserve which containes all the information related
	 * to this reserve request
	 */
	public PendingReserve reserve( Catalogue catalogue, ReserveLevel reserveLevel, 
			String reserveDescription ) {
		
		this.catalogue = catalogue;
		this.reserveLevel = reserveLevel;
		this.reserveDescription = reserveDescription;

		System.out.println ( reserveLevel.getOp() + ": " + catalogue );

		PendingReserve pr = null;

		try {
			
			// start the reserve operation
			pr = (PendingReserve) makeRequest();
			
		} catch (SOAPException e) {
			
			e.printStackTrace();
		}
		
		return pr;
	}

	@Override
	public String getAttachment() {
		
		// add attachment to the request into the node <rowData>
		// using the right message for the related reserve operation
		String attachmentData = UploadMessages.getReserveMessage(
				catalogue.getCode(), reserveLevel, reserveDescription );
		
		return attachmentData;
	}

	/**
	 * Once the process is finished and we have retrieved
	 * the log code of the upload catalogue file request,
	 * we create a pending reserve related to this request and
	 * we return it.
	 * @return the pending reserve related to this request,
	 * please cast it to use it
	 */
	@Override
	public Object processResponse(String logCode) {

		// add a pending reserve object to the db
		// to save the request
		PendingReserve pr = PendingReserve.addPendingReserve( 
				logCode, reserveLevel, catalogue, 
				User.getInstance().getUsername() );

		return pr;
	}
}
