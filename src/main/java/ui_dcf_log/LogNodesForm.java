package ui_dcf_log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

import dcf_log.DcfLog;
import i18n_messages.CBMessages;

public class LogNodesForm {
	
	private Shell shell;
	private Shell dialog;
	private DcfLog log;
	
	public LogNodesForm(Shell shell, DcfLog log) {
		this.shell = shell;
		this.log = log;
	}
	
	public void open() {

		this.dialog = new Shell( shell , SWT.TITLE | SWT.APPLICATION_MODAL | SWT.SHELL_TRIM );
		dialog.setText( CBMessages.getString( "LogNodesForm.Title" ) );
		
		dialog.setLayout(new GridLayout(1, false));
		
		// add the generic log information
		new LogMacroOperationViewer(dialog, log);
		
		// create the table
		new LogNodesTableViewer(dialog, log);
		
		dialog.open();
		dialog.pack();
	}
}
