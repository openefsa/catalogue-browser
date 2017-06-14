package dcf_log_util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

public class LogNodesForm {
	
	private Shell shell;
	private Shell dialog;
	private DcfLog log;
	
	public LogNodesForm( Shell shell, DcfLog log ) {
		this.shell = shell;
		this.log = log;
	}
	
	public void display () {
		this.dialog = new Shell( shell , SWT.TITLE | SWT.APPLICATION_MODAL | SWT.SHELL_TRIM );
		dialog.setText( "Log errors:" );
		
		dialog.setLayout( new GridLayout(1,false) );
		
		LogNodesTableViewer table = new LogNodesTableViewer( dialog, log );
		
		dialog.open();
		dialog.pack();
	}
}
