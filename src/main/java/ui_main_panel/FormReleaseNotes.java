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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue.ReleaseNotes;
import catalogue.ReleaseNotesOperation;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * This class is used to show all the catalogue release notes.
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class FormReleaseNotes {

	private static final String UNKNOWN_VALUE = CBMessages.getString("FormReleaseNotes.Unknown");

	private Shell shell;
	private Shell dialog;
	private Catalogue catalogue;

	public FormReleaseNotes(Catalogue catalogue, Shell shell) {
		this.catalogue = catalogue;
		this.shell = shell;
	}

	public void display() {

		this.dialog = new Shell(shell, SWT.SHEET | SWT.APPLICATION_MODAL | SWT.WRAP | SWT.BORDER | SWT.TITLE);
		dialog.setSize(720, 500);

		// set the dialog title
		ReleaseNotes rn = catalogue.getReleaseNotes();
		dialog.setText(CBMessages.getString("FormReleaseNotes.ReleaseDate") + " " + getValue(rn.getDate()) + " "
				+ CBMessages.getString("FormReleaseNotes.ReleaseVersion") + " " + getValue(rn.getInternalVersion()));

		// set the dialog layout
		dialog.setLayout(new FillLayout());

		Composite layout = new Composite(dialog, SWT.NONE);
		layout.setLayout(new GridLayout(1, false));
		layout.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label description = new Label(layout, SWT.NONE);
		description.setText(CBMessages.getString("FormReleaseNotes.Description") + " " + getValue(rn.getDescription()));

		Label internalVersionNote = new Label(layout, SWT.NONE);
		internalVersionNote.setText(
				CBMessages.getString("FormReleaseNotes.InternalNote") + " " + getValue(rn.getInternalVersionNote()));

		TableViewer table = addReleaseNotesTable(layout);
		table.setInput(rn.getOperations());

		dialog.open();
	}

	/**
	 * Get the value of {@code field} in string format. If null return
	 * {@value #UNKNOWN_VALUE}
	 * 
	 * @param field
	 * @return
	 */
	private String getValue(Object field) {
		if (field == null)
			return UNKNOWN_VALUE;

		return field.toString();
	}

	/**
	 * Add the table to the form to show operations info
	 * 
	 * @param parent
	 * @return
	 */
	private TableViewer addReleaseNotesTable(Composite parent) {

		TableViewer table = new TableViewer(parent,
				SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE);

		table.setContentProvider(new NotesContentProvider());

		// Add columns
		GlobalUtil.addStandardColumn(table, new NotesLabelProvider(NotesLabelProvider.NAME),
				CBMessages.getString("FormReleaseNotes.NameColumn"), 150, true, false);

		GlobalUtil.addStandardColumn(table, new NotesLabelProvider(NotesLabelProvider.DATE),
				CBMessages.getString("FormReleaseNotes.DateColumn"), 100, true, false);

		GlobalUtil.addStandardColumn(table, new NotesLabelProvider(NotesLabelProvider.INFO),
				CBMessages.getString("FormReleaseNotes.InfoColumn"), 300, true, false);

		// make the columns names visible
		table.getTable().setHeaderVisible(true);

		table.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return table;
	}

	private class NotesContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object arg0) {

			if (arg0 instanceof Collection<?>) {

				return (((Collection<Catalogue>) arg0).toArray());
			}
			return null;
		}
	}

	/**
	 * Label provider for the catalogue reserved by
	 * 
	 * @author avonva
	 *
	 */
	private class NotesLabelProvider extends ColumnLabelProvider {

		public static final int NAME = 0;
		public static final int DATE = 1;
		public static final int INFO = 2;

		private int columnKey;

		public NotesLabelProvider(int columnKey) {
			this.columnKey = columnKey;
		}

		@Override
		public void addListener(ILabelProviderListener arg0) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {
		}

		@Override
		public Image getImage(Object arg0) {
			return null;
		}

		@Override
		public String getText(Object arg0) {

			ReleaseNotesOperation op = (ReleaseNotesOperation) arg0;

			String text = "";

			switch (columnKey) {
			case NAME:
				text = op.getOpName();
				break;
			case DATE:
				Timestamp ts = op.getOpDate();
				if (ts != null) {

					Date date = new Date();
					date.setTime(ts.getTime());
					text = new SimpleDateFormat("yyyy-MM-dd").format(date);
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
