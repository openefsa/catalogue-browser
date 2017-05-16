package version_control;
import java.sql.Timestamp;
import java.util.Date;

public class VersioningModel {
	Date	vFrom;
	Date	vTo;
	String	lastVersion	= "";
	Date	lastUpdate;

	public static Timestamp getTimestamp ( Date date ) {
		return new Timestamp( date.getTime() );
	}

	public Date getvFrom ( ) {
		return vFrom;
	}

	public void setvFrom ( Date vFrom ) {
		this.vFrom = vFrom;
	}

	public Date getvTo ( ) {
		return vTo;
	}

	public void setvTo ( Date vTo ) {
		this.vTo = vTo;
	}

	public String getLastVersion ( ) {
		return lastVersion;
	}

	public void setLastVersion ( String lastVersion ) {
		this.lastVersion = lastVersion;
	}

	public Date getLastUpdate ( ) {
		return lastUpdate;
	}

	public void setLastUpdate ( Date lastUpdate ) {
		this.lastUpdate = lastUpdate;
	}

}
