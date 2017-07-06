package ui_progress_bar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import messages.Messages;

/**
 * Table used to show several processes progresses.
 * See {@link FormMultipleProgress} to get more details.
 * @author avonva
 *
 */
public class TableMultipleProgress {
	
	private Composite parent;
	private Table table;

	public TableMultipleProgress( Composite parent ) {
		this.parent = parent;
		createTable();
	}

	/**
	 * Create the table
	 */
	private void createTable() {
		
		// create table
		table = new Table( parent, SWT.BORDER );
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// add two columns
		for (int i = 0; i < 3; i++) {
			new TableColumn(table, SWT.NONE);
		}
		
		// set columns names
		table.getColumn(0).setText( Messages.getString("ProgressTable.TaskCol") );
		table.getColumn(1).setText( Messages.getString("ProgressTable.ProgressCol") );
		table.getColumn(2).setText( Messages.getString("ProgressTable.StatusCol") );
	}

	
	/**
	 * Set the table input
	 * @param tasks
	 */
	public TableRow addRow ( String taskName ) {

		// for each step add a record with bar
		TableRow row = new TableRow( table, taskName );
		
		table.getColumn(0).pack();
	    table.getColumn(1).setWidth(128);
	    table.getColumn(2).setWidth(256);
	    
	    return row;
	}
	
	/**
	 * Class which represents a row in the table
	 * @author avonva
	 *
	 */
	public static class TableRow {
		
		// status of the progress
		public static String READY = Messages.getString( "ProgressTable.Ready" );
		public static String ONGOING = Messages.getString( "ProgressTable.Ongoing" );
		public static String COMPLETED = Messages.getString( "ProgressTable.Completed" );
		public static String ABORTED = Messages.getString( "ProgressTable.Aborted" );
		
		private Shell shell;
		private String name;
		private String status;
		private CustomProgressBar bar;
		private Table table;
		private TableItem row;
		private TableEditor editor;
		
		public TableRow( Table table, String name ) {
			this.name = name;
			this.status = READY;
			this.table = table;
			this.shell = table.getShell();
			display();
		}
		
		public void display() {

			row = new TableItem( table, SWT.NONE );
			row.setText( 0, name );

			// add progress bar
			bar = new CustomProgressBar( table, SWT.NONE );
			
			bar.addProgressListener( new ProgressListener() {
				
				@Override
				public void progressChanged(double currentProgress, 
						double maxProgress, IProgressBar bar) {

					// if started
					if ( currentProgress > 0 )
						setStatus ( ONGOING );
					
					// if completed
					if ( currentProgress == maxProgress ) {
						setStatus ( COMPLETED );
					}
				}

				@Override
				public void progressStopped( String message, IProgressBar bar) {
					
					// if aborted
					setStatus ( ABORTED + ": " + message );
				}
			});
			
			editor = new TableEditor( table );
			editor.grabHorizontal = editor.grabVertical = true;
			editor.setEditor( bar.getProgressBar(), row, 1 );

			setStatus ( status );
		}
		
		public TableItem getRow() {
			return row;
		}
		
		public CustomProgressBar getBar() {
			return bar;
		}
		
		public TableEditor getEditor() {
			return editor;
		}
		
		public void setStatus( final String status) {
			
			// guarantee that we are using the ui thread
			// since this method is called by other threads
			shell.getDisplay().asyncExec( new Runnable() {
				
				@Override
				public void run() {
					TableRow.this.status = status;
					row.setText( 2, status );
				}
			});
		}
	}
}
