package ui_main_panel;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue.ReleaseNotes;
import catalogue.ReleaseNotesOperation;
import messages.Messages;
import utilities.GlobalUtil;

/**
 * This class is used to show all the catalogue
 * release notes.
 * @author avonva
 *
 */
public class FormReleaseNotes {

	private static final String UNKNOWN_VALUE = 
			Messages.getString( "FormReleaseNotes.Unknown" );
	
	private Shell shell;
	private Shell dialog;
	private Catalogue catalogue;
	
	public FormReleaseNotes( Catalogue catalogue, Shell shell ) {
		this.catalogue = catalogue;
		this.shell = shell;
	}
	
	public void display() {
		
		this.dialog = new Shell( shell , SWT.SHEET|SWT.APPLICATION_MODAL | SWT.WRAP | SWT.BORDER | SWT.TITLE );
		
		// set the dialog layout
		dialog.setLayout( new GridLayout( 1 , false ) );
		
		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.verticalAlignment = SWT.TOP;
		data.heightHint = 300;
		dialog.setLayoutData( data );
		
		ReleaseNotes rn = catalogue.getReleaseNotes();

		dialog.setText( Messages.getString("FormReleaseNotes.ReleaseDate") + " "
				+ getValue( rn.getDate() ) + " "
			+ Messages.getString("FormReleaseNotes.ReleaseVersion") + " "
			+ getValue( rn.getInternalVersion() ) );

		Composite layout = new Composite( dialog, SWT.NONE );
		layout.setLayout( new GridLayout( 1, false ) );
		
		Label description = new Label( layout, SWT.NONE );
		
		// increase size
		FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

		Font font = new Font( Display.getCurrent(), 
				new FontData( fontData.getName(), 
						fontData.getHeight() + 5, SWT.NORMAL ) );

		description.setFont ( font );
		description.setText( Messages.getString("FormReleaseNotes.Description") + " "
				+ getValue( rn.getDescription() ) );

		Label internalVersionNote = new Label( layout, SWT.NONE );
		internalVersionNote.setFont ( font );
		internalVersionNote.setText( Messages.getString("FormReleaseNotes.InternalNote") + " "
				+ getValue( rn.getInternalVersionNote() ) );

		TableViewer table = addReleaseNotesTable( layout );
		table.setInput( rn.getOperations() );
		
		// set the layout data of the table
		table.getTable().setLayoutData( data );

		dialog.pack();
		dialog.open();
	}
	
	/**
	 * Get the value of {@code field} in string format. 
	 * If null return {@value #UNKNOWN_VALUE}
	 * @param field
	 * @return
	 */
	private String getValue ( Object field ) {
		if ( field == null )
			return UNKNOWN_VALUE;
		
		return field.toString();
	}
	
	/**
	 * Add the table to the form to show operations info
	 * @param parent
	 * @return
	 */
	private TableViewer addReleaseNotesTable ( Composite parent ) {
		
		TableViewer table = new TableViewer ( parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | 
				SWT.FULL_SELECTION | SWT.SINGLE );
		
		table.setContentProvider( new NotesContentProvider() );
		
		// Add columns
		GlobalUtil.addStandardColumn( table, new NotesLabelProvider( NotesLabelProvider.NAME ), 
				Messages.getString("FormReleaseNotes.NameColumn"), 150, true, false );
		
		GlobalUtil.addStandardColumn( table, new NotesLabelProvider( NotesLabelProvider.DATE ), 
				Messages.getString("FormReleaseNotes.DateColumn"), 100, true, false );
		
		GlobalUtil.addStandardColumn( table, new NotesLabelProvider( NotesLabelProvider.INFO ), 
				Messages.getString("FormReleaseNotes.InfoColumn"), 300, true, false );
		
		// make the columns names visible
		table.getTable().setHeaderVisible( true );
		
		return table;
	}
	
	private class NotesContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object arg0) {

			if ( arg0 instanceof Collection<?> ) {

				return (  ( (Collection <Catalogue>) arg0).toArray() );
			}
			return null;
		}
	}
	
	/**
	 * Label provider for the catalogue reserved by
	 * @author avonva
	 *
	 */
	private class NotesLabelProvider extends ColumnLabelProvider {

		public static final int NAME = 0;
		public static final int DATE = 1;
		public static final int INFO = 2;
		
		private int columnKey;
		
		public NotesLabelProvider( int columnKey ) {
			this.columnKey = columnKey;
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

			ReleaseNotesOperation op = (ReleaseNotesOperation) arg0;
			
			String text = "";
			
			switch ( columnKey ) {
			case NAME:
				text = op.getOpName();
				break;
			case DATE:
				Timestamp ts = op.getOpDate();
				if ( ts != null ) {
					
					Date date = new Date();
					date.setTime( ts.getTime() );
					text = new SimpleDateFormat("yyyy-MM-dd").format( date );
				}
				break;
			case INFO:
				text = op.getOpInfo();
				break;
			default:
				break;
			}
			
			return text;
		}
	}
}
