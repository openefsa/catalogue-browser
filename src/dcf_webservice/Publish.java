package dcf_webservice;

import catalogue_object.Catalogue;

public class Publish extends UploadCatalogueFile {

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
	 */
	public void publish ( Catalogue catalogue, PublishLevel level ) {
		this.catalogue = catalogue;
		this.level = level;
	}

	@Override
	public String getAttachment() {
		return UploadMessages.getPublishMessage( catalogue.getCode(), level );
	}

	@Override
	public Object processResponse(String logCode) {
		// create a pending publish object
		return null;
	}
}
