package ui_pending_request_list;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import pending_request.IPendingRequest;
import soap.UploadCatalogueFileImpl;

public class RequestLabelProvider extends ColumnLabelProvider implements ILabelProvider {
	
	public enum RequestLabelColumn {
		RESTART_TIME,
		LOG_CODE,
		TYPE,
		RESPONSE,
		CATALOGUE,
		STATUS;
	}
	
	private RequestLabelColumn key;
	public RequestLabelProvider(RequestLabelColumn key) {
		this.key = key;
	}
	
	@Override
	public Color getForeground(Object element) {
		
		int colour = SWT.COLOR_BLACK;
		
		if (key == RequestLabelColumn.STATUS && element instanceof IPendingRequest) {
			IPendingRequest request = (IPendingRequest) element;
			
			if (request.getStatus() != null) {
				switch(request.getStatus()) {
				case COMPLETED:
					colour = SWT.COLOR_DARK_GREEN;
					break;
				case WAITING:
				case DOWNLOADING:
				case QUEUED:
					colour = SWT.COLOR_DARK_YELLOW;
					break;
				case ERROR:
					colour = SWT.COLOR_RED;
					break;
				}
			}
		}
		
		if (key == RequestLabelColumn.RESPONSE && element instanceof IPendingRequest) {
			IPendingRequest request = (IPendingRequest) element;
			
			if (request.getResponse() != null) {
				switch(request.getResponse()) {
				case OK:
					colour = SWT.COLOR_DARK_GREEN;
					break;
				case AP:
				case ERROR:
					colour = SWT.COLOR_RED;
					break;
				}
			}
		}
		
		return Display.getDefault().getSystemColor(colour);
	}

	@Override
	public void addListener(ILabelProviderListener arg0) {}

	@Override
	public void dispose() {}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener arg0) {}

	@Override
	public Image getImage(Object arg0) {
		return null;
	}
	
	@Override
	public String getText(Object arg0) {
		
		String text = null;
		
		if (arg0 instanceof IPendingRequest) {
		
			IPendingRequest request = (IPendingRequest) arg0;
			
			switch(this.key) {
			case RESTART_TIME:
				if (request.getRestartTime() != -1) {
				    text = new SimpleDateFormat("HH:mm:ss").format(new Date(request.getRestartTime()));
				}
				else {
					text = "-";
				}
				break;
			case CATALOGUE:
				
				if (request.getData() != null)
					text = request.getData().get(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY);
				break;
			case LOG_CODE:
				text = request.getLogCode();
				break;
			case TYPE:
				text = request.getType();
				break;
			case STATUS:
				if (request.getStatus() != null)
					text = request.getStatus().toString();
				break;
			case RESPONSE:
				text = request.getResponse() == null ? "-" : request.getResponse().toString();
				break;
			}
		}
		
		return text;
	}

}
