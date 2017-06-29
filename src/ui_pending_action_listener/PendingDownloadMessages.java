package ui_pending_action_listener;

import dcf_pending_action.PendingActionStatus;
import dcf_webservice.DcfResponse;
import messages.Messages;

public class PendingDownloadMessages implements PendingActionMessages {

	@Override
	public String getResponseMessage(DcfResponse response) {
		return null;
	}

	@Override
	public String getStatusMessage(PendingActionStatus status) {
		
		String msg = null;
		
		switch ( status ) {
		case STARTED:
			msg = Messages.getString( "DownloadXml.StartedMessage" );
			break;
		default:
			break;
		}
		
		return msg;
	}
}
