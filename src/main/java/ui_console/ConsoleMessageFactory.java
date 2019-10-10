package ui_console;

import org.eclipse.swt.SWT;

import dcf_log.DcfResponse;
import i18n_messages.CBMessages;
import soap.UploadCatalogueFileImpl.PublishLevel;
import soap.UploadCatalogueFileImpl.ReserveLevel;

public class ConsoleMessageFactory {

	private String catalogueCode;
	public ConsoleMessageFactory(String catalogueCode) {
		this.catalogueCode = catalogueCode;
	}
	
	public ConsoleMessage getRestartedReserveMessage(ReserveLevel level) {
		String label = getReserveLabel(level);
		return this.getRestartedMessage(label);
	}
	
	public ConsoleMessage getRestartedPublishMessage(PublishLevel level) {
		String label = getPublishLabel(level);
		return this.getRestartedMessage(label);
	}
	
	public ConsoleMessage getRestartedUnreserveMessage() {
		return this.getRestartedMessage(CBMessages.getString("unreserve.label"));
	}
	
	public ConsoleMessage getRestartedXmlDataMessage() {
		return this.getRestartedMessage(CBMessages.getString("upload.xml.data.label"));
	}
	
	private ConsoleMessage getRestartedMessage(String operation) {
		return new ConsoleMessage(
				CBMessages.getString("upload.cat.file.relaunched", catalogueCode, operation),
				SWT.COLOR_DARK_YELLOW);
	}
	
	
	public ConsoleMessage getQueuedReserveMessage(ReserveLevel level) {
		String label = getReserveLabel(level);
		return this.getQueuedMessage(label);
	}
	
	public ConsoleMessage getQueuedPublishMessage(PublishLevel level) {
		String label = getPublishLabel(level);
		return this.getQueuedMessage(label);
	}
	
	public ConsoleMessage getQueuedUnreserveMessage() {
		return this.getQueuedMessage(CBMessages.getString("unreserve.label"));
	}
	
	public ConsoleMessage getQueuedXmlDataMessage() {
		return this.getQueuedMessage(CBMessages.getString("upload.xml.data.label"));
	}
	
	private ConsoleMessage getQueuedMessage(String operation) {
		return new ConsoleMessage(
				CBMessages.getString("upload.cat.file.queued", catalogueCode, operation),
				SWT.COLOR_DARK_YELLOW);
	}
	
	public String getReserveLabel(ReserveLevel level) {
		
		String levelLabel = null;
		
		if (level == ReserveLevel.MINOR)
			levelLabel = CBMessages.getString("reserve.minor.label");
		else if (level == ReserveLevel.MAJOR)
			levelLabel = CBMessages.getString("");
		
		return levelLabel;
	}
	
	public String getPublishLabel(PublishLevel level) {
		
		String levelLabel = null;
		
		if (level == PublishLevel.MINOR)
			levelLabel = CBMessages.getString("publish.minor.label");
		else if (level == PublishLevel.MAJOR)
			levelLabel = CBMessages.getString("publish.major.label");
		
		return levelLabel;
	}
	
	public ConsoleMessage getReserveCompletedMessage(DcfResponse response, ReserveLevel level) {
		
		String msg = null;
		int color = SWT.COLOR_DARK_GREEN;
		
		String levelLabel = getReserveLabel(level);

		switch (response) {
		case ERROR:
			msg = CBMessages.getString("reserve.error", catalogueCode, levelLabel);
			color = SWT.COLOR_DARK_RED;
			break;
		case AP:
			msg = CBMessages.getString("reserve.ap.response", catalogueCode, levelLabel);
			color = SWT.COLOR_DARK_RED;
			break;
		case OK:
			msg = CBMessages.getString("reserve.success", catalogueCode, levelLabel);
			color = SWT.COLOR_DARK_GREEN;
			break;
		default:
			break;
		}
		
		return new ConsoleMessage(msg, color);
	}
	
	public ConsoleMessage getUnreserveCompletedMessage(DcfResponse response) {
		
		String msg = null;
		int color = SWT.COLOR_DARK_GREEN;
		
		switch (response) {
		case ERROR:
			msg = CBMessages.getString("unreserve.error", catalogueCode);
			color = SWT.COLOR_DARK_RED;
			break;
		case AP:
			msg = CBMessages.getString("unreserve.ap.response", catalogueCode);
			color = SWT.COLOR_DARK_RED;
			break;
		case OK:
			msg = CBMessages.getString("unreserve.success", catalogueCode);
			color = SWT.COLOR_DARK_GREEN;
			break;
		default:
			break;
		}
		
		return new ConsoleMessage(msg, color);
	}
	
	public ConsoleMessage getXmlDataCompletedMessage(DcfResponse response) {
		
		String msg = null;
		int color = SWT.COLOR_DARK_GREEN;
		
		switch (response) {
		case ERROR:
			msg = CBMessages.getString("upload.xml.error", catalogueCode);
			color = SWT.COLOR_DARK_RED;
			break;
		case AP:
			msg = CBMessages.getString("upload.xml.ap.response", catalogueCode);
			color = SWT.COLOR_DARK_RED;
			break;
		case OK:
			msg = CBMessages.getString("upload.xml.success", catalogueCode);
			color = SWT.COLOR_DARK_GREEN;
			break;
		default:
			break;
		}
		
		return new ConsoleMessage(msg, color);
	}

	public ConsoleMessage getPublishCompletedMessage(DcfResponse response, PublishLevel level) {
		
		String msg = null;
		int color = SWT.COLOR_DARK_GREEN;
		
		String levelLabel = getPublishLabel(level);
		
		switch (response) {
		case ERROR:
			msg = CBMessages.getString("publish.error", catalogueCode, levelLabel);
			color = SWT.COLOR_DARK_RED;
			break;
		case AP:
			msg = CBMessages.getString("publish.ap.response", catalogueCode, levelLabel);
			color = SWT.COLOR_DARK_RED;
			break;
		case OK:
			msg = CBMessages.getString("publish.success", catalogueCode, levelLabel);
			color = SWT.COLOR_DARK_GREEN;
			break;
		default:
			break;
		}
		
		return new ConsoleMessage(msg, color);
	}
}
