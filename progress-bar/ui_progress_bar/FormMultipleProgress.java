package ui_progress_bar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import messages.Messages;
import ui_progress_bar.TableMultipleProgress.TableRow;

/**
 * Form which displays several progress bars related to
 * several background processes. For each progress,
 * create a row in the table by using {@link #addRow(String)}.
 * This method returns the created row, from which it is
 * possible to access the progress bar using {@link TableRow#getBar()}.
 * This progress bar can be shared in threads and updated
 * directly by them using {@link IProgressBar#addProgress(double)}.
 * @author avonva
 *
 */
public class FormMultipleProgress {

	private Shell shell;
	private TableMultipleProgress table;
	private Shell dialog;
	private Button okBtn;
	private Listener closeListener;
	
	public FormMultipleProgress( Shell shell ) {
		this.shell = shell;
		init();
	}

	/**
	 * Initialize graphics
	 */
	public void init() {
		
		dialog = new Shell( shell, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );

		dialog.setLayout( new GridLayout( 1, false ) );

		dialog.setSize( 500, 300 );

		// do not close this window until finished
		closeListener = new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				arg0.doit = false;
				return;
			}
		};
		
		// block closure of window
		dialog.addListener( SWT.Close, closeListener );
		
		table = new TableMultipleProgress ( dialog );
		
		okBtn = new Button ( dialog, SWT.NONE );
		okBtn.setText( Messages.getString( "ProgressTable.CloseBtn" ) );
		okBtn.setEnabled( false );

		okBtn.setLayoutData( new GridData(SWT.CENTER, SWT.CENTER, true, false) );
		
		// close if clicked
		okBtn.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				dialog.close();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}
	
	/**
	 * Open and visualize the window
	 */
	public void open() {
		dialog.open();
	}
	
	/**
	 * Add a row to the table
	 * @param taskName
	 * @return
	 */
	public TableRow addRow ( String taskName ) {
		return table.addRow(taskName);
	}

	/**
	 * Make the dialog closeable by the user
	 */
	public void done() {
		dialog.removeListener( SWT.Close, closeListener );
		okBtn.setEnabled( true );
	}
}
