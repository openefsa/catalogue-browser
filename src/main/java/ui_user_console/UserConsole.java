package ui_user_console;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import i18n_messages.CBMessages;
import pending_request.IPendingRequest;
import sas_remote_procedures.XmlChangesService;
import soap.UploadCatalogueFileImpl;
import soap.UploadCatalogueFileImpl.PublishLevel;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import ui_console.Console;
import ui_console.ConsoleMessage;
import ui_console.ConsoleMessageFactory;
import ui_pending_request_list.PendingRequestTable;
import ui_pending_request_list.PendingRequestTableRelaunchListener;

public class UserConsole extends Composite {

	private TabFolder tabs;
	private Console console;
	private PendingRequestTable requestTable;
	
	public UserConsole(Composite parent, int style) {
		super(parent, style);
		createContents();
	}
	
	private void createContents() {
		
		this.setLayout(new FillLayout());
		
		// create tabs
		tabs = new TabFolder(this, SWT.NONE);
		
		// create composites
		this.console = new Console(tabs, SWT.NONE);
		this.requestTable = new PendingRequestTable(tabs, SWT.NONE);
		
		// assign composites to tabs
		TabItem consoleTab = new TabItem(tabs, SWT.NONE);
		consoleTab.setText(CBMessages.getString("console.tab.title"));
		consoleTab.setControl(console);
		
		TabItem requestsTab = new TabItem(tabs, SWT.NONE);
		requestsTab.setText(CBMessages.getString("requests.tab.title"));
		requestsTab.setControl(requestTable);
		
		requestTable.addRelaunchListener(new PendingRequestTableRelaunchListener() {
			
			@Override
			public void relaunched(IPendingRequest request) {
				
				String catalogueCode = request.getData()
						.get(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY);
				
				ConsoleMessageFactory factory = new ConsoleMessageFactory(catalogueCode);
				ConsoleMessage message = null;
				switch(request.getType()) {
				case IPendingRequest.TYPE_PUBLISH_MAJOR:
					message = factory.getRestartedPublishMessage(PublishLevel.MAJOR);
					break;
				case IPendingRequest.TYPE_PUBLISH_MINOR:
					message = factory.getRestartedPublishMessage(PublishLevel.MINOR);
					break;
				case IPendingRequest.TYPE_RESERVE_MAJOR:
					message = factory.getRestartedReserveMessage(ReserveLevel.MAJOR);
					break;
				case IPendingRequest.TYPE_RESERVE_MINOR:
					message = factory.getRestartedReserveMessage(ReserveLevel.MINOR);
					break;
				case IPendingRequest.TYPE_UNRESERVE:
					message = factory.getRestartedUnreserveMessage();
					break;
				case XmlChangesService.TYPE_UPLOAD_XML_DATA:
					message = factory.getRestartedXmlDataMessage();
					break;
				}
				
				// add message to the console
				console.add(message);
				
				// open tab of console
				selectTab(0);
			}
		});
	}
	
	public void selectTab(int index) {
		if (index >= tabs.getItemCount())
			throw new IllegalArgumentException("Cannot select tab " 
					+ index + ". Maximum index allowed=" + (tabs.getItemCount() - 1));
		
		tabs.setSelection(index);
	}
	
	public void refresh() {
		this.console.refresh();
		this.requestTable.refresh();
	}
	
	public void refresh(IPendingRequest request) {
		this.requestTable.refresh(request);
	}
	
	/**
	 * Get the console
	 * @return
	 */
	public Console getConsole() {
		return console;
	}
	
	/**
	 * Get the table of pending requests
	 * @return
	 */
	public PendingRequestTable getRequestTable() {
		return requestTable;
	}
}
