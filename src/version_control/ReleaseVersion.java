package version_control;
import java.sql.Timestamp;
import java.util.Calendar;

public class ReleaseVersion {

	private static final int CHECKED_IN = 0;
	private static final int CHECKED_OUT = 1;
	private static final int NOT_DEFINED = -1;

	private int				_majorRelease;
	private int				_minorRelease;
	private int				_operationId;
	private Timestamp		_checkOutDate;
	private Timestamp		_checkInDate;
	private String			_comment;

	public void setMajorRelease ( int majorRelease ) {
		_majorRelease = majorRelease;
	}

	public void setMinorRelease ( int minorRelease ) {
		_minorRelease = minorRelease;
	}

	public void setOperationId ( int operationId ) {
		_operationId = operationId;
	}

	public void setCheckOutDate ( Timestamp checkOutDate ) {
		_checkOutDate = checkOutDate;
	}

	public void setCheckInDate ( Timestamp checkInDate ) {
		_checkInDate = checkInDate;
	}

	public void setComment ( String comment ) {
		_comment = comment;
	}

	public int getMajorRelease ( ) {
		return _majorRelease;
	}

	public int getMinorRelease ( ) {
		return _minorRelease;
	}

	public int getOperationId ( ) {
		return _operationId;
	}

	public Timestamp getCheckOutDate ( ) {
		return _checkOutDate;
	}

	public Timestamp getCheckInDate ( ) {
		return _checkInDate;
	}

	public String getComment ( ) {
		return _comment;
	}

	public static int	_state	= NOT_DEFINED;

	/* perform the application check-in to check-out state change */
	/**
	 * perform the application check-in to check-out state change
	 */
	public void checkOutNewOperation ( ) {
		/* the application status is checked in i can perform the check out */
		if ( _state == NOT_DEFINED )
			_state = NOT_DEFINED;
		else {
			if ( _state == CHECKED_OUT )
				_state = CHECKED_OUT;
			else {
				/* Put the last check in date in check out */
				/* I assign the check out date */
				Calendar cal = Calendar.getInstance();
				_checkOutDate = new Timestamp( cal.getTimeInMillis() );
				/* I reset the check in date */
				_checkInDate = null;
				/* I increment the operation */
				_operationId++;
				//foodexDAO.saveCheckedOut();
				_state = CHECKED_OUT;
			}
		}
	}

	public void checkOutNewMinorRelease ( ) {
		/* the application status is checked in i can perform the check out */
		if ( _state == NOT_DEFINED )
			_state = NOT_DEFINED;
		else {
			if ( _state == CHECKED_OUT )
				_state = CHECKED_OUT;
			else {
				/* Put the last check in date in check out */
				/* I assign the check out date */
				Calendar cal = Calendar.getInstance();
				_checkOutDate = new Timestamp( cal.getTimeInMillis() );
				/* I increment the minor release and reset the operation */
				/* I reset the check in date */
				_checkInDate = null;
				_minorRelease++;
				_operationId = 1;
				//foodexDAO.saveCheckedOut();
				_state = CHECKED_OUT;
			}
		}
	}

	public void checkOutNewMajorRelease ( ) {
		/* the application status is checked in i can perform the check out */
		if ( _state == NOT_DEFINED )
			_state = NOT_DEFINED;
		else {
			if ( _state == CHECKED_OUT )
				_state = CHECKED_OUT;
			else {
				/* Put the last check in date in check out */
				/* I assign the check out date */
				Calendar cal = Calendar.getInstance();
				_checkOutDate = new Timestamp( cal.getTimeInMillis() );
				/* I reset the check in date */
				_checkInDate = null;
				/* I increment the minor release and reset the operation */
				_majorRelease++;
				_operationId = 1;
				//foodexDAO.saveCheckedOut();
				_state = CHECKED_OUT;
			}
		}
	}

	/**
	 * Metodo per salvare la modalita' check in. Aggiunge una linea check in nel
	 * DB.
	 */
	public void checkin ( ) {
		if ( _state == NOT_DEFINED )
			_state = NOT_DEFINED;
		else {
			if ( _state == CHECKED_IN )
				_state = CHECKED_IN;
			else {
				/* Add a check in line to the database */
				Calendar cal = Calendar.getInstance();
				_checkInDate = new Timestamp( cal.getTimeInMillis() );
				//foodexDAO.saveCheckedIn();
				_state = CHECKED_IN;
			}
		}
	}

	public int getState ( ) {
		return _state;
	}

	public void setStateForce ( int state ) {
		_state = state;
	}

	/**
	 * Metodo che restituisce la stringa formata dalla versione del programma in
	 * uso.
	 * 
	 * @return
	 */
	public String getFullVersion ( ) {
		return String.format( "%02d", _majorRelease ) + "." + String.format( "%02d", _minorRelease ) + "."
				+ String.format( "%03d", _operationId );
	}

}
