package ui_user_console;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import pending_request.IPendingRequest;
import ui_console.ConsoleMessage;

public class UserConsoleDialog extends Dialog {

	private Shell dialog;
	private UserConsole userConsole;
	
	public UserConsoleDialog(Shell arg0, int arg1) {
		super(arg0, arg1);
		createContents();
	}

	public UserConsoleDialog(Shell arg0) {
		this(arg0, SWT.NONE);
	}
	
	private void createContents() {
		dialog = new Shell(getParent(), getStyle());
		dialog.setLayout(new FillLayout());
		userConsole = new UserConsole(dialog, SWT.NONE);
		dialog.pack();
	}
	
	public void open() {
		dialog.open();
	}
	
	public void close() {
		dialog.close();
	}
	
	public void addCloseListener(Listener listener) {
		this.dialog.addListener(SWT.Close, listener);
	}
	
	/**
	 * Set the title of the dialog
	 */
	public void setText(String text) {
		this.dialog.setText(text);
	}
	
	/**
	 * Open a tab of the console
	 * @param index
	 */
	public void selectTab(int index) {
		this.userConsole.selectTab(index);
	}
	
	/**
	 * Refresh the entire dialog contents
	 */
	public void refresh() {
		this.userConsole.refresh();
	}
	
	/**
	 * Refresh a single pending request in the request table
	 * @param request
	 */
	public void refresh(IPendingRequest request) {
		this.userConsole.refresh(request);
	}
	
	/**
	 * Make the dialog visible or not visible
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		dialog.setVisible(visible);
	}
	
	/**
	 * Check if the dialog is visible
	 * @return
	 */
	public boolean isVisible() {
		return dialog.isVisible();
	}
	
	/**
	 * Set the icon for the dialog
	 * @param image
	 */
	public void setImage(Image image) {
		this.dialog.setImage(image);
	}
	
	/**
	 * Add messages to the console
	 * @param messages
	 */
	public void add(ConsoleMessage... messages) {
		this.userConsole.getConsole().add(messages);
	}
	
	/**
	 * Add messages to the console. Text is coloured
	 * with the default colour
	 * @param message
	 */
	public void add(String... messages) {
		this.userConsole.getConsole().add(messages);
	}
	
	/**
	 * Add a message to the console. Text is coloured
	 * with the style defined by {@code colour}
	 * @param message
	 * @param colour
	 */
	public void add(String message, int colour) {
		this.userConsole.getConsole().add(message, colour);
	}
	
	/**
	 * Add a request to the request table
	 * @param request
	 */
	public void add(IPendingRequest... requests) {
		this.userConsole.getRequestTable().add(requests);
	}
	
	/**
	 * Get the user console component
	 * @return
	 */
	public UserConsole getUserConsole() {
		return userConsole;
	}
	
	/*
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("User console");
		
		Layout layout = new FillLayout();
        shell.setLayout(layout);
        
        shell.open();
        
        UserConsoleDialog dialog = new UserConsoleDialog(shell, SWT.RESIZE | SWT.DIALOG_TRIM);
        dialog.open();
        
        UserConsole userConsole = dialog.getUserConsole();
        Console console = userConsole.getConsole();
        
		console.add("MTX: Reserve sent", SWT.COLOR_GREEN);
		console.add("MTX: Reserve forced due to DCF unresponsiveness. MTX 9.0.1.1.TEMP created.", SWT.COLOR_YELLOW);
		console.add("MTX: Reserve failed", SWT.COLOR_RED);
		console.add("MTX: 9.0.1.1.TEMP invalidated to 9.0.1.1.NULL", SWT.COLOR_RED);
        
		PendingRequestTable requestTable = userConsole.getRequestTable();
		
		Map<String, String> data = new HashMap<>();
        data.put(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY, "MTX");
        
        PendingRequestMock req = new PendingRequestMock("20180130_001_WS", "Reserve", null);
        req.setStatus(PendingRequestStatus.DOWNLOADING);
        req.setData(data);
        
        PendingRequestMock req2 = new PendingRequestMock("Log_1032133", "Publish", null);
        req2.setStatus(PendingRequestStatus.QUEUED);
        req2.setData(data);
        
        PendingRequestMock req3 = new PendingRequestMock("Log_4214123", "Upload .xml changes", DcfResponse.ERROR);
        req3.setStatus(PendingRequestStatus.ERROR);
        req3.setData(data);
        
        PendingRequestMock req4 = new PendingRequestMock("Log_4343243", "Unreserve", DcfResponse.AP);
        req4.setStatus(PendingRequestStatus.COMPLETED);
        req4.setData(data);
        
        PendingRequestMock req5 = new PendingRequestMock("Log_4324332", "Unreserve", DcfResponse.OK);
        req5.setStatus(PendingRequestStatus.COMPLETED);
        req5.setData(data);

        requestTable.add(req);
        requestTable.add(req2);
        requestTable.add(req3);
        requestTable.add(req4);
        requestTable.add(req5);
		
		while(!shell.isDisposed()) {
			display.readAndDispatch();
		}
	}*/
}
