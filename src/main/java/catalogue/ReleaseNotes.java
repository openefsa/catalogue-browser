package catalogue;

import java.sql.Timestamp;
import java.util.Collection;

/**
 * Object which models the catalogue release notes
 * @author avonva
 *
 */
public class ReleaseNotes {

	private String description;
	private Timestamp date;
	private String internalVersion;
	private String internalVersionNote;
	private Collection<ReleaseNotesOperation> ops;
	
	/**
	 * Initialise a release note object
	 * @param collection the operation related to the release note
	 */
	public ReleaseNotes( String description, Timestamp date, 
			String internalVersion, String internalVersionNote,
			Collection<ReleaseNotesOperation> ops ) {
		
		this.description = description;
		this.date = date;
		this.internalVersion = internalVersion;
		this.internalVersionNote = internalVersionNote;
		this.ops = ops;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Timestamp getDate() {
		return date;
	}
	
	public String getInternalVersion() {
		return internalVersion;
	}
	
	public String getInternalVersionNote() {
		return internalVersionNote;
	}
	
	public void clear() {
		ops.clear();
	}
	
	/**
	 * Get the release note groups of operations.
	 * @return
	 */
	public Collection<ReleaseNotesOperation> getOperations() {
		return ops;
	}
	
	@Override
	public String toString() {
		return "RELEASE NOTES: description=" + description + ";date=" 
	+ date + ";internalVersion=" + internalVersion + ";internalVersionNote" + internalVersionNote; 
	}
}
