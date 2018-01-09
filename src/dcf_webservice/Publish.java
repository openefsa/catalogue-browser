package dcf_webservice;

import javax.xml.soap.SOAPException;

import catalogue.Catalogue;
import dcf_manager.Dcf.DcfType;
import dcf_pending_action.PendingPublish;
import soap.UploadCatalogueFile;
import user.IDcfUser;

public class Publish extends UploadCatalogueFile {
	
	private IDcfUser user;
	private DcfType type;
	
	/**
	 * Initialize a Publish request with the
	 * dcf type we want to work with
	 * @param type
	 */
	public Publish(IDcfUser user, DcfType type) {
		super(user);
		this.user = user;
		this.type = type;
	}

	/**
	 * Publish the {@code catalogue} with the required 
	 * publish level.
	 * @param catalogue
	 * @param level
	 * @throws SOAPException 
	 */
	public PendingPublish publish(Catalogue catalogue, PublishLevel level) throws SOAPException {
		
		System.out.println (level.getOp() + ": " + catalogue);
		
		String attachment = UploadMessages.getPublishMessage(catalogue.getCode(), level);
		
		String logCode = this.send(attachment);
		
		PendingPublish pp = PendingPublish.addPendingPublish(
				logCode, level, catalogue, 
				user.getUsername(), type);
		
		return pp;
	}
}
