package ui_pending_action_listener;

import dcf_pending_action.PendingActionStatus;
import dcf_webservice.DcfResponse;
import messages.Messages;

public class PendingUploadDataMessages implements PendingActionMessages {

	@Override
	public String getResponseMessage(DcfResponse response) {

		String msg = null;
		
		switch ( response ) {
		case ERROR:
			msg = Messages.getString( "UpData.ErrorMessage" );
			break;
		case AP:
			msg = Messages.getString( "UpData.ApMessage" );
			break;
		case OK:
			msg = Messages.getString( "UpData.OkMessage" );
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
		case STARTED:
			msg = Messages.getString( "UpData.StartedMessage" );
			break;
		default:
			break;
		}
		
		return msg;
	}
}
