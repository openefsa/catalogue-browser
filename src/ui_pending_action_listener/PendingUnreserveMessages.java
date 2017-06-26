package ui_pending_action_listener;

import dcf_pending_action.PendingActionStatus;
import dcf_webservice.DcfResponse;
import messages.Messages;

public class PendingUnreserveMessages implements PendingActionMessages {

	@Override
	public String getResponseMessage(DcfResponse response) {
		
		String msg = null;
		
		switch ( response ) {
		case OK:
			msg = Messages.getString( "Unreserve.OkMessage" );
			break;
		case ERROR:
			msg = Messages.getString( "Unreserve.ErrorMessage" );
			break;
		case AP:
			msg = Messages.getString( "Unreserve.ApMessage" );
			break;
		case FORBIDDEN:
			msg = Messages.getString( "Unreserve.MinorErrorMessage" );
			break;
		default:
			break;
		}
		
		return msg;
	}

	@Override
	public String getStatusMessage(PendingActionStatus status) {
		
		String msg = null;

		switch ( status ) {
		
		case QUEUED:
			msg = Messages.getString( "Unreserve.BusyMessage" );
			break;
			
		case STARTED:
			msg = Messages.getString( "Unreserve.StartedMessage" );
			break;

		default:
			break;
		}


		return msg;
	}
}
