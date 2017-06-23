package dcf_webservice;

import javax.xml.soap.SOAPException;

import catalogue.Catalogue;
import dcf_manager.Dcf.DcfType;
import dcf_pending_action.PendingPublish;
import dcf_user.User;

public class Publish extends UploadCatalogueFile {

	/**
	 * Initialize a Publish request with the
	 * dcf type we want to work with
	 * @param type
	 */
	public Publish( DcfType type ) {
		super( type );
	}

	public enum PublishLevel {
		
		MAJOR,
		MINOR;
		
		/**
		 * Get the publish operation as string to
		 * parametrize the request in {@link UploadMessages#getPublishMessage(String, PublishLevel)}
		 * @return
		 */
		public String getOp() {
			
			String op = null;
			
			switch ( this ) {
			case MAJOR:
				op = "publishMajor";
				break;
			case MINOR:
				op = "publishMinor";
			}
			
			return op;
		}
	};
	
	// the catalogue we want to publish
	private Catalogue catalogue;
	private PublishLevel level;
	
	/**
	 * Publish the {@code catalogue} with the required 
	 * publish level.
	 * @param catalogue
	 * @param level
	 * @throws SOAPException 
	 */
	public PendingPublish publish ( Catalogue catalogue, PublishLevel level ) throws SOAPException {
		
		this.catalogue = catalogue;
		this.level = level;
		
		System.out.println ( level.getOp() + ": " + catalogue );

		PendingPublish pp = (PendingPublish) upload();

		return pp;
	}

	@Override
	public CatalogueAttachment getAttachment() {
		
		String content = UploadMessages.getPublishMessage( catalogue.getCode(), level );
		
		CatalogueAttachment att = new CatalogueAttachment( 
				AttachmentType.ATTACHMENT, content );
		
		return att;
	}

	@Override
	public Object processResponse(String logCode) {
		
		// create a pending publish object
		
		PendingPublish pp = PendingPublish.addPendingPublish(
				logCode, level, catalogue, 
				User.getInstance().getUsername(), getType() );
		
		return pp;
	}
}
