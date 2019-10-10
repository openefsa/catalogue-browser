package dcf_pending_request;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;

import dcf_log.DcfResponse;
import dcf_user.User;
import pending_request.IPendingRequest;
import pending_request.PendingRequestStatus;
import pending_request.PendingRequestStatusChangedEvent;
import sas_remote_procedures.XmlChangesService;
import soap.DetailedSOAPException;
import soap.UploadCatalogueFileImpl;
import ui_main_panel.MainPanel;

public class MainPanelTest {

	private Display display;
	private Shell shell;
	private BrowserPendingRequestWorkerMock worker;
	private MainPanel panel;
	private String catalogueCode;
	
	@Before
	public void init() {
		
		display = new Display();
		shell = new Shell(display);
		
		this.worker = new BrowserPendingRequestWorkerMock();
		
		this.panel = new MainPanel(shell, worker);
		this.panel.initGraphics();

		shell.open();
		
		this.catalogueCode = "AMRPROG";
		
		User.getInstance().login("username", "general_pswd");
	}
	
	@Test
	public void testMainPanelListeningPendingRequestsStatusChanges() throws DetailedSOAPException {
		
		PendingRequestMock errorRequest = new PendingRequestMock("LOG_1932213", "type", DcfResponse.OK);
		
		// put catalogue data
		Map<String, String> data = new HashMap<>();
		data.put(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY, catalogueCode);
		errorRequest.setData(data);
		
		// simulate a request finished for the main panel
		worker.statusChanged(new PendingRequestStatusChangedEvent(errorRequest, 
				PendingRequestStatus.DOWNLOADING, 
				PendingRequestStatus.ERROR));
		
		// test every possibility
		DcfResponse[] responses = new DcfResponse[] {DcfResponse.OK, DcfResponse.AP, DcfResponse.ERROR};
		String[] types = new String[] {IPendingRequest.TYPE_RESERVE_MINOR, 
				IPendingRequest.TYPE_RESERVE_MAJOR, IPendingRequest.TYPE_PUBLISH_MINOR,
				IPendingRequest.TYPE_PUBLISH_MAJOR, IPendingRequest.TYPE_UNRESERVE,
				XmlChangesService.TYPE_UPLOAD_XML_DATA};
		
		for (String type : types) {
			
			for (DcfResponse response : responses) {
				// create a fake request
				PendingRequestMock request = new PendingRequestMock("LOG_1932213", type, response);
				
				// put catalogue data
				Map<String, String> data2 = new HashMap<>();
				data2.put(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY, catalogueCode);
				request.setData(data2);
				
				// simulate a request finished for the main panel
				worker.statusChanged(new PendingRequestStatusChangedEvent(request, 
						PendingRequestStatus.DOWNLOADING, 
						PendingRequestStatus.COMPLETED));
			}
		}
		
		// Event loop
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
}
