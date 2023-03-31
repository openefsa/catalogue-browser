package user_preferences;

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import catalogue.Catalogue;
import i18n_messages.CBMessages;
import session_manager.BrowserWindowPreferenceDao;
import window_restorer.RestoreableWindow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Form which allows modifying the user preferences
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class FormUserPreferences {
	
	private static final Logger LOGGER = LogManager.getLogger(FormUserPreferences.class);

	private RestoreableWindow window;
	private static final String WINDOW_CODE = "FormUserPreferences";

	private Shell _shell;
	private Shell dialog;

	private ArrayList<Preference> preferences = new ArrayList<>();

	private TableViewer table;

	private Catalogue catalogue;

	// constructor
	public FormUserPreferences(Shell shell, Catalogue catalogue) {
		this.catalogue = catalogue;
		_shell = shell;

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

		// load the catalogue preferences
		preferences = prefDao.getAll();
	}

	/**
	 * Display the form and initialize all the graphics
	 */
	public void display() {

		this.dialog = new Shell(_shell, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);

		window = new RestoreableWindow(dialog, WINDOW_CODE);

		dialog.setText(CBMessages.getString("FormUserPreferences.OptionsLabel"));
		dialog.setSize(400, 350);
		dialog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dialog.setLayout(new GridLayout(1, false));

		Group g = new Group(dialog, SWT.NONE);
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		g.setLayout(new GridLayout(1, false));

		table = new TableViewer(g, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.getTable().setHeaderVisible(true);
		table.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		table.setContentProvider(new ContentProviderUserPref());
		table.setLabelProvider(new LabelProviderUserPref());

		// SETTINGS NAMES

		TableViewerColumn viewerColumnCode = new TableViewerColumn(table, SWT.NONE);

		viewerColumnCode.setEditingSupport(new EditingSupport(table) {

			@Override
			protected void setValue(Object arg0, Object arg1) {
			}

			@Override
			protected Object getValue(Object arg0) {

				// get the information from the user properties file
				String value = ((Preference) arg0).getValue();

				if (value == null)
					value = CBMessages.getString("FormUserPreferences.NotFound");

				return value;
			}

			@Override
			protected CellEditor getCellEditor(Object arg0) {
				return null;
			}

			@Override
			protected boolean canEdit(Object arg0) {
				return false;
			}
		});

		TableColumn colCode = viewerColumnCode.getColumn();
		colCode.setText(CBMessages.getString("FormUserPreferences.SettingColumn"));
		colCode.setWidth(200);
		colCode.setResizable(true);
		colCode.setMoveable(false);

		viewerColumnCode.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				String p = ((Preference) element).getKey();
				return p;
			}
		});

		// SETTINGS VALUES

		TableViewerColumn viewerColumnName = new TableViewerColumn(table, SWT.NONE);
		TableColumn colName = viewerColumnName.getColumn();
		colName.setText(CBMessages.getString("FormUserPreferences.ValueColumn"));
		colName.setWidth(250);
		colName.setResizable(true);
		colName.setMoveable(false);

		viewerColumnName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				String p = ((Preference) element).getValue();
				return p;
			}
		});

		viewerColumnName.setEditingSupport(new EditingSupport(table) {

			@Override
			protected void setValue(Object arg0, Object value) {

				Preference preference = (Preference) arg0;

				String newValue = null;

				// if we have a boolean => combo box with fixed values to be treated in a
				// different way
				if (preference.getType() == PreferenceType.BOOLEAN) {

					CellEditor e = getCellEditor(preference);
					if (e instanceof ComboBoxCellEditor) {

						String[] items = ((ComboBoxCellEditor) e).getItems();

						// avoid out of bounds exceptions (sometimes a -1 is returned if no element is
						// clicked)
						int intval = (int) value;
						if (intval >= 0 && intval < items.length)
							newValue = items[intval];
						else // otherwise return and do nothing
							return;
					}
				} else
					newValue = (String) value;

				// if integer and not integer input return
				if (preference.getType() == PreferenceType.INTEGER) {
					if (!isNumericInput(newValue)) {
						return;
					}
				}

				// set the new value
				preference.setValue(newValue);

				CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

				// update the preference into the database
				prefDao.update(preference);

				// refresh the table content
				table.refresh();
			}

			@Override
			protected Object getValue(Object arg0) {

				Preference pref = (Preference) arg0;

				// get the information from the user properties file
				Object value = pref.getValue();

				// get the cell editor for the preference
				CellEditor e = getCellEditor(pref);

				// if cell box combo => return the integer not the string
				if (e instanceof ComboBoxCellEditor) {

					// get the index of the selected item
					int index = 0;
					for (String item : ((ComboBoxCellEditor) e).getItems()) {

						// if we have found the right value, stop and save the index as value
						if (item.equals(pref.getValue()))
							break;

						index++;
					}

					value = index;
				}

				return value;
			}

			@Override
			protected CellEditor getCellEditor(Object arg0) {

				Preference pref = (Preference) arg0;

				if (pref.getType() == PreferenceType.BOOLEAN) {
					return new ComboBoxCellEditor(table.getTable(), new String[] { "true", "false" });
				}

				return new TextCellEditor(table.getTable());
			}

			@Override
			protected boolean canEdit(Object arg0) {

				// do not edit favourite picklist
				Preference pref = (Preference) arg0;

				return pref.isEditable();
			}
		});

		table.setInput(preferences);

		/* BUTTONS */

		Composite c = new Composite(dialog, SWT.NONE);
		c.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		c.setLayout(new GridLayout(2, false));

		Button bOk = new Button(c, SWT.PUSH);
		bOk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bOk.setText(CBMessages.getString("FormUserPreferences.OkButton")); 
		
		Button bCancel = new Button(c, SWT.PUSH);
		bCancel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bCancel.setText(CBMessages.getString("FormUserPreferences.CancelButton")); 

		c.pack();

		/* BUTTONS LISTENERS */

		// write to the xml file the chosen user preferences
		bOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				// close the dialog
				dialog.close();
			}
		});

		bCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				dialog.close();
			}

		});

		dialog.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
			}
		});

		dialog.pack();

		// restore old dimensions
		window.restore(BrowserWindowPreferenceDao.class);
		window.saveOnClosure(BrowserWindowPreferenceDao.class);

		dialog.open();
	}

	/**
	 * Check if numeric input
	 * 
	 * @param newValue
	 * @return
	 */
	private boolean isNumericInput(String newValue) {

		try {
			Integer.parseInt(newValue);
			return true;
		} catch (NumberFormatException e) {
			LOGGER.error("Error while parsing value ", e);
			return false;
		}
	}
}
