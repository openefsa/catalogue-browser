package ui_describe;

import java.util.ArrayList;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import i18n_messages.CBMessages;
import ui_search_bar.TermTable;

/**
 * Form to show search results in a stand alone window. Double click a term to
 * select it.
 * 
 * @author avonva
 *
 */
public class FormDescribeSearchResult {

	private Shell shell;
	private Shell dialog;
	private String title;
	private TermTable table;
	private ArrayList<Term> searchResults;
	private Term selectedTerm;
	private Hierarchy hierarchy;
	private boolean hideDepr;
	private boolean hideNotInUse;

	/**
	 * Constructor, display the search results passed in the input.
	 * 
	 * @param parentShell
	 *            parent widget
	 * @param title
	 *            title of the window
	 * @param hierarchy
	 *            the hierarchy used to compute reportability of terms
	 * @param searchResults
	 *            array of terms which needs to be shown
	 */
	public FormDescribeSearchResult(Shell parentShell, String title, Hierarchy hierarchy,
			ArrayList<Term> searchResults) {

		this.shell = parentShell;
		this.title = title;
		this.hierarchy = hierarchy;
		this.searchResults = searchResults;
		this.hideDepr = false;
		this.hideNotInUse = false;
	}

	/**
	 * Display the window
	 */
	public void display(Catalogue catalogue) {

		/**
		 * Set the layout of the form
		 * @author shahaal
		 */
		//dialog = new Shell( shell , SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );
		dialog = new Shell(shell, SWT.SHELL_TRIM | SWT.MODELESS);
		
		// window icon (on the top left)
		dialog.setImage(
				new Image(Display.getCurrent(), this.getClass().getClassLoader().getResourceAsStream("icons/Choose.gif")));

		dialog.setMaximized(true);
		dialog.setText(title); // window title
		dialog.setLayout(new FillLayout()); // layout style
		dialog.setSize(500, 400); // default size

		// table to show the results
		table = new TermTable(dialog, catalogue);

		table.setHideDeprecated(hideDepr);
		table.setHideNotInUse(hideNotInUse);

		table.setCurrentHierarchy(hierarchy);

		// Set the results to be displayed in the table
		table.setInput(searchResults);

		// if an element is double clicked
		table.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent arg0) {

				// If an element is double clicked => return it to the SelectTermForm
				IStructuredSelection selection = (IStructuredSelection) arg0.getSelection();
				selectedTerm = (Term) selection.getFirstElement();
				dialog.close();
			}
		});

		Menu searchMenu = new Menu(dialog, SWT.POP_UP);
		MenuItem addItem = new MenuItem(searchMenu, SWT.PUSH);
		addItem.setText(CBMessages.getString("FormDescribeSearchResult.AddCmd")); //$NON-NLS-1$
		Image addIcon = new Image(Display.getCurrent(),
				this.getClass().getClassLoader().getResourceAsStream("icons/add-icon.png"));
		addItem.setImage(addIcon);

		// Set the menu
		table.addMenu(searchMenu);

		// if add term is selected
		addItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				selectedTerm = table.getFirstSelectedTerm();
				dialog.close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		dialog.setVisible(true); // show the window
		dialog.open();

		while (!dialog.isDisposed()) {
			if (!dialog.getDisplay().readAndDispatch())
				dialog.getDisplay().sleep();
		}
		
		dialog.dispose();
		
	}

	public void setHideDeprecated(boolean hide) {
		// update content provider settings
		hideDepr = hide;
	}

	public void setHideNotInUse(boolean hide) {
		// update content provider settings
		hideNotInUse = hide;
	}

	/**
	 * Get the selected term
	 * 
	 * @return
	 */
	public Term getSelectedTerm() {
		return (selectedTerm);
	}
}
