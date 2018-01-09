package dcf_webservice;

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