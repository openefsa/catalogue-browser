package ui_pending_action_listener;

import dcf_pending_action.PendingReserveStatus;
import dcf_webservice.DcfResponse;
import messages.Messages;

public class PendingPublishMessages implements PendingActionMessages {

	@Override
	public String getResponseMessage(DcfResponse response) {
		
		String msg = null;

		switch ( response ) {
		case ERROR:
			msg = Messages.getString( "Publish.ErrorMessage" );
			break;
		case AP:
			msg = Messages.getString( "Publish.ApMessage" );
			break;
		case FORBIDDEN:
			msg = Messages.getString( "Publish.MinorErrorMessage" );
			break;
		case OK:
			msg = Messages.getString( "Publish.OkMessage" );
			break;
		default:
			break;
		}

		return msg;
	}

	/**
	 * No status message is shown for publish
	 */
	@Override
	public String getStatusMessage(PendingReserveStatus status) {
		
		String msg = null;
		
		switch ( status ) {
		case STARTED:
			msg = Messages.getString( "Reserve.StartedMessage" );
			break;
		default:
			break;
		}
		
		return msg;
	}
}
