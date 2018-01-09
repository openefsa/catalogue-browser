package ui_pending_action_listener;

import dcf_log.DcfResponse;
import dcf_pending_action.PendingActionStatus;
import messages.Messages;

/**
 * Messages for reserve operations
 * @author avonva
 *
 */
public class PendingReserveMessages implements PendingActionMessages {

	@Override
	public String getResponseMessage(DcfResponse response) {
		
		String msg = null;
		
		switch ( response ) {
		case OK:
			break;
		case ERROR:
			msg = Messages.getString( "Reserve.ErrorMessage" );
			break;
		case AP:
			msg = Messages.getString( "Reserve.ApMessage" );
			break;
		case FORBIDDEN:
			msg = Messages.getString( "Reserve.MinorErrorMessage" );
			break;
		default:
			break;
		}
		
		return msg;
	}

	@Override
	public String getStatusMessage(PendingActionStatus status) {
		
		String msg = null;
		
		// Warn user of the performed actions

		switch ( status ) {
		case ERROR:
			msg = Messages.getString( "Reserve.GeneralErrorMessage" );
			break;
			
		case IMPORTING_LAST_VERSION:
			msg = Messages.getString( "Reserve.OldMessage" );
			break;
		
			// warn user that we cannot use this version
		case INVALID_VERSION:
			msg = Messages.getString( "Reserve.InvalidVersionMessage" );
			break;
			
		case INVALID_RESPONSE:
			msg = Messages.getString( "Reserve.InvalidResponseMessage" );
			break;

		case QUEUED:
			msg = Messages.getString( "Reserve.BusyMessage" );
			break;
			
		case STARTED:
			msg = Messages.getString( "Reserve.StartedMessage" );
			break;
		case COMPLETED:
			msg = Messages.getString( "Reserve.OkMessage" );
			break;
			
		default:
			break;
		}
		
		return msg;
	}
}
