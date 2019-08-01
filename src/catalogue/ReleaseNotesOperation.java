package catalogue;

import java.sql.Timestamp;

import catalogue_object.Mappable;
import data_transformation.DateTrimmer;

/**
 * Class to host the information of a single release
 * note operation, which is composed of a name, a date and
 * the information related to the operation.
 * @author avonva
 *
 */
public class ReleaseNotesOperation implements Mappable {

	private int id;
	private String opName;
	private Timestamp opDate;
	private String opInfo;
	private int groupId;

	/**
	 * Create a single release note operation related to a catalogue
	 * @param opName
	 * @param opDate
	 * @param opInfo
	 * @param groupId the id which identifies the group (operationsDetail group)
	 * this operation belongs to.
	 */
	public ReleaseNotesOperation( String opName, Timestamp opDate, 
			String opInfo, int groupId ) {
		this.opName = opName;
		this.opDate = opDate;
		this.opInfo = opInfo;
		this.groupId = groupId;
	}

	/**
	 * Set the database id for this operation
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Get the database id of this operation
	 * @return
	 */
	public int getId() {
		return id;
	}
	public String getOpName() {
		return opName;
	}
	public Timestamp getOpDate() {
		return opDate;
	}
	public String getOpInfo() {
		return opInfo;
	}
	public int getGroupId() {
		return groupId;
	}

	@Override
	public String getValueByKey(String key) {

		String value = null;

		switch ( key ) {
		case "OP_NAME":
			if ( opName != null )
				value = opName;
			break;
			
		case "OP_DATE":
			if ( opDate != null )
				value = DateTrimmer.dateToString( opDate );
			break;
			
		case "OP_INFO":
			if ( opInfo != null )
				value = opInfo;
			break;
			
		case "OP_GROUP_ID":
			value = String.valueOf( groupId );
			break;
			
		default:
			break;
		}

		return value;
	}
}
