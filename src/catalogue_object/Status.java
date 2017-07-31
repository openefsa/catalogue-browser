package catalogue_object;

/**
 * Class which model the status of a {@link BaseObject}.
 * @author avonva
 *
 */
public class Status {

	public enum StatusValues {
		DRAFT_MAJOR_RESERVED,
		DRAFT_MINOR_RESERVED,
		DRAFT_MAJOR_UNRESERVED,
		DRAFT_MINOR_UNRESERVED,
		PUBLISHED_MAJOR,
		PUBLISHED_MINOR,
		DEPRECATED,
		TEMPORARY,
		INTERNAL_VERSION,
		INVALID
	}
	
	private static final String DRAFT = "DRAFT";
	private static final String MAJOR = "MAJOR";
	private static final String MINOR = "MINOR";
	private static final String RESERVED = "RESERVED";
	private static final String UNRESERVED = "UNRESERVED";
	private static final String PUBLISHED = "PUBLISHED";
	private static final String DEPRECATED = "DEPRECATED";

	private static final String TEMPORARY = "TEMPORARY";
	private static final String INVALID = "INVALID";
	private static final String INTERNAL_VERSION = "INTERNAL VERSION";
	
	private String status;
	
	public Status( String status ) {
		this.status = status;
	}
	
	/**
	 * Get the status in string format
	 * @return
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * Update the status
	 * @param value
	 */
	public void markAs ( StatusValues value ) {
		
		switch ( value ) {
		
		case DRAFT_MAJOR_RESERVED:
			status = DRAFT + " " + MAJOR + " " + RESERVED;
			break;
		case DRAFT_MINOR_RESERVED:
			status = DRAFT + " " + MINOR + " " + RESERVED;
			break;
		case DRAFT_MAJOR_UNRESERVED:
			status = DRAFT + " " + MAJOR + " " + UNRESERVED;
			break;
		case DRAFT_MINOR_UNRESERVED:
			status = DRAFT + " " + MINOR + " " + UNRESERVED;
			break;
		case PUBLISHED_MAJOR:
			status = PUBLISHED + " " + MAJOR;
			break;
		case PUBLISHED_MINOR:
			status = PUBLISHED + " " + MINOR;
			break;
		case DEPRECATED:
			status = DEPRECATED;
			break;
		case TEMPORARY:
			status = TEMPORARY;
			break;
		case INVALID:
			status = INVALID;
			break;
		case INTERNAL_VERSION:
			status = INTERNAL_VERSION;
			break;
		default:
			break;
		}
	}
	
	/**
	 * Is the status in draft?
	 * @return
	 */
	public boolean isDraft() {
		return contains ( DRAFT );
	}
	
	/**
	 * Is the status in major?
	 * @return
	 */
	public boolean isMajor() {
		return contains ( MAJOR );
	}
	
	/**
	 * Is the status in minor?
	 * @return
	 */
	public boolean isMinor() {
		return contains ( MINOR );
	}
	
	/**
	 * Is the status in reserved?
	 * @return
	 */
	public boolean isReserved() {
		return contains ( RESERVED );
	}
	
	/**
	 * Is the status in unreserved?
	 * @return
	 */
	public boolean isUnreserved() {
		return contains ( UNRESERVED );
	}
	
	/**
	 * Is the status in published?
	 * @return
	 */
	public boolean isPublished() {
		return contains ( PUBLISHED );
	}
	
	/**
	 * Is the status deprecated?
	 * @return
	 */
	public boolean isDeprecated() {
		return contains ( DEPRECATED );
	}
	
	/**
	 * Check if the status contains a keyword
	 * or not. Note that this is case insensitive
	 * @param value
	 * @return
	 */
	private boolean contains( String value ) {
		return status.toUpperCase().contains( value.toUpperCase() );
	}
}
