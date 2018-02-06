package ui_main_panel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import dcf_log.DcfResponse;
import messages.Messages;
import pending_request.IPendingRequest;
import sas_remote_procedures.XmlChangesService;
import utilities.GlobalUtil;

public class PendingRequestMessages {

	public void show(Shell shell, String title, String requestType, DcfResponse response) {

		switch(requestType) {
		case IPendingRequest.TYPE_PUBLISH_MAJOR:
		case IPendingRequest.TYPE_PUBLISH_MINOR:
			showPublish(shell, title, response);
			break;
		case IPendingRequest.TYPE_RESERVE_MAJOR:
		case IPendingRequest.TYPE_RESERVE_MINOR:
			showReserve(shell, title, response);
			break;
		case IPendingRequest.TYPE_UNRESERVE:
			showUnreserve(shell, title, response);
			break;
		case XmlChangesService.TYPE_UPLOAD_XML_DATA:
			showXmlData(shell, title, response);
			break;
		}
	}
	
	private void showReserve(Shell shell, String title, DcfResponse response) {

		int icon = SWT.ICON_ERROR;
		String msg = null;
		switch ( response ) {
		case ERROR:
			msg = Messages.getString("reserve.error");
			break;
		case AP:
			msg = Messages.getString("reserve.ap.response");
			break;
		case OK:
			msg = Messages.getString("reserve.success");
			icon = SWT.ICON_INFORMATION;
			break;
		default:
			break;
		}
		
		GlobalUtil.showDialog(shell, title, msg, icon);
	}
	
	
	private void showUnreserve(Shell shell, String title, DcfResponse response) {

		int icon = SWT.ICON_ERROR;
		String msg = null;
		switch ( response ) {
		case ERROR:
			msg = Messages.getString("unreserve.error");
			break;
		case AP:
			msg = Messages.getString("unreserve.ap.response");
			break;
		case OK:
			msg = Messages.getString("unreserve.success");
			icon = SWT.ICON_INFORMATION;
			break;
		default:
			break;
		}
		
		GlobalUtil.showDialog(shell, title, msg, icon);
	}
	
	private void showXmlData(Shell shell, String title, DcfResponse response) {

		int icon = SWT.ICON_ERROR;
		String msg = null;
		switch ( response ) {
		case ERROR:
			msg = Messages.getString("upload.xml.error");
			break;
		case AP:
			msg = Messages.getString("upload.xml.ap.response");
			break;
		case OK:
			msg = Messages.getString("upload.xml.success");
			icon = SWT.ICON_INFORMATION;
			break;
		default:
			break;
		}
		
		GlobalUtil.showDialog(shell, title, msg, icon);
	}
	
	private void showPublish(Shell shell, String title, DcfResponse response) {

		int icon = SWT.ICON_ERROR;
		String msg = null;
		switch ( response ) {
		case ERROR:
			msg = Messages.getString("publish.error");
			break;
		case AP:
			msg = Messages.getString("publish.ap.response");
			break;
		case OK:
			msg = Messages.getString("publish.success");
			icon = SWT.ICON_INFORMATION;
			break;
		default:
			break;
		}
		
		GlobalUtil.showDialog(shell, title, msg, icon);
	}
}
