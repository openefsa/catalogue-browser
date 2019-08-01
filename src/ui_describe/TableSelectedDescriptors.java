<<<<<<< HEAD
package ui_describe;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import messages.Messages;
import ui_search_bar.TermTable;

/**
 * Table which is used to display the selected descriptors in the FormSelectTerm
 * (multiple selection case only)
 * 
 * @author avonva
 *
 */
public class TableSelectedDescriptors {

	private TermTable table;
	private Listener addListener;
	private Listener removeListener;
	private Listener selectionListener;
	private Listener openListener;

	private MenuItem removeItem;
	private MenuItem openItem;

	/**
	 * Create a table which allows adding and removing terms
	 * 
	 * @param parent
	 */
	public TableSelectedDescriptors(Composite parent, Catalogue catalogue) {

		// If multiple selection, then show the group of selected elements
		Group selectedElemGroup = new Group(parent, SWT.NONE);
		selectedElemGroup.setLayout(new FillLayout());
		selectedElemGroup.setText(Messages.getString("FormSelectTerm.SelectedElemLabel"));

		// Data layout for selectedElemGroup
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.minimumHeight = 120;
		gridData.minimumWidth = 200;
		gridData.widthHint = 400;
		gridData.heightHint = 120;

		// set the height of the table
		selectedElemGroup.setLayoutData(gridData);

		// Create table viewer for displaying selected terms
		table = new TermTable(selectedElemGroup, catalogue);

		// add the menu to the table
		table.addMenu(createMenu(parent.getShell(), table));

		table.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {

				openItem.setEnabled(!table.isSelectionEmpty());
				removeItem.setEnabled(!table.isSelectionEmpty());

				if (table.isSelectionEmpty())
					return;

				Term selectedTerm = table.getFirstSelectedTerm();

				// call the selection listener if it was set
				if (selectionListener != null) {

					// return the selected term as event data
					Event event = new Event();
					event.data = selectedTerm;
					selectionListener.handleEvent(event);
				}
			}
		});
	}

	/**
	 * Add the listner which is called when a term is added to the table
	 * 
	 * @param listener
	 */
	public void addAddListener(Listener listener) {
		addListener = listener;
	}

	/**
	 * Add the listener which is called when a term is removed from the table
	 * 
	 * @param listener
	 */
	public void addRemoveListener(Listener listener) {
		removeListener = listener;
	}

	/**
	 * Add the listener which is called when Open is pressed
	 * 
	 * @param listener
	 */
	public void addOpenListener(Listener listener) {
		openListener = listener;
	}

	/**
	 * Add the listener which is called when a term is selected in the table
	 * 
	 * @param listener
	 */
	public void addSelectionListener(Listener listener) {
		selectionListener = listener;
	}

	/**
	 * Set the hierarchy we are working with
	 * 
	 * @param hierarchy
	 */
	public void setCurrentHierarchy(Hierarchy hierarchy) {
		table.setCurrentHierarchy(hierarchy);
	}

	/**
	 * Add a term into the table (will call add listener)
	 * 
	 * @param term
	 */
	public void addTerm(Term term) {

		if (term == null)
			return;

		// add the term
		table.addTerm(term);

		// a term was added, so the remove item can be used
		if (removeItem != null)
			removeItem.setEnabled(true);

		// if an add listener was set call it
		if (addListener != null) {

			// set as data the added term
			Event event = new Event();
			event.data = term;

			// call the listener
			addListener.handleEvent(event);
		}
	}

	/**
	 * Remove a term from the table (will call remove listener)
	 * 
	 * @param term
	 */
	public void removeTerm(Term term) {

		if (term == null)
			return;

		// remove the term from the table
		table.removeTerm(term);

		// a term was removed, if we have no more elements disable remove
		if (removeItem != null && table.getItemCount() == 0)
			removeItem.setEnabled(false);

		// if a remove listener was set call it
		if (removeListener != null) {

			// set as data the removed term
			Event event = new Event();
			event.data = term;

			// call the listener
			removeListener.handleEvent(event);
		}
	}

	/**
	 * Create a menu for the table (remove item)
	 * 
	 * @param shell
	 * @param table
	 * @return
	 */
	private Menu createMenu(Shell shell, final TermTable table) {

		// Right click menu for removing terms
		final Menu menu = new Menu(shell, SWT.POP_UP);

		openItem = new MenuItem(menu, SWT.PUSH);
		openItem.setText(Messages.getString("FormSelectTerm.OpenCmd"));

		removeItem = new MenuItem(menu, SWT.PUSH);
		removeItem.setText(Messages.getString("FormSelectTerm.RemoveCmd"));

		// set menu item icon
		Image removeIcon = new Image(Display.getCurrent(),
				this.getClass().getClassLoader().getResourceAsStream("remove-icon.png"));
		removeItem.setImage(removeIcon);
		removeItem.setEnabled(false);

		openItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// If the selection is not empty
				if (table.isSelectionEmpty())
					return;

				final Term term = table.getFirstSelectedTerm();

				if (openListener != null) {

					Event e = new Event();
					e.data = term;

					openListener.handleEvent(e);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// If remove command is pressed, remove the term selected
		removeItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// If the selection is not empty, then remove from the table the term selected
				if (table.isSelectionEmpty())
					return;

				final Term term = table.getFirstSelectedTerm();

				// remove the term from the table
				removeTerm(term);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		// if focus is lost => disable remove item from the menu
		table.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				removeItem.setEnabled(false);
				openItem.setEnabled(false);
			}

			@Override
			public void focusGained(FocusEvent e) {
			}
		});

		return menu;
	}

	/**
	 * Return true if the table contains the term
	 * 
	 * @param term
	 * @return
	 */
	public boolean contains(Term term) {
		return table.contains(term);
	}
}
=======
package ui_describe;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import messages.Messages;
import ui_search_bar.TermTable;

/**
 * Table which is used to display the selected descriptors in the FormSelectTerm
 * (multiple selection case only)
 * 
 * @author avonva
 *
 */
public class TableSelectedDescriptors {

	private TermTable table;
	private Listener addListener;
	private Listener removeListener;
	private Listener selectionListener;
	private Listener openListener;

	private MenuItem removeItem;
	private MenuItem openItem;

	/**
	 * Create a table which allows adding and removing terms
	 * 
	 * @param parent
	 */
	public TableSelectedDescriptors(Composite parent, Catalogue catalogue) {

		// If multiple selection, then show the group of selected elements
		Group selectedElemGroup = new Group(parent, SWT.NONE);
		selectedElemGroup.setLayout(new FillLayout());
		selectedElemGroup.setText(Messages.getString("FormSelectTerm.SelectedElemLabel"));

		// Data layout for selectedElemGroup
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.minimumHeight = 120;
		gridData.minimumWidth = 200;
		gridData.widthHint = 400;
		gridData.heightHint = 120;

		// set the height of the table
		selectedElemGroup.setLayoutData(gridData);

		// Create table viewer for displaying selected terms
		table = new TermTable(selectedElemGroup, catalogue);

		// add the menu to the table
		table.addMenu(createMenu(parent.getShell(), table));

		table.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {

				openItem.setEnabled(!table.isSelectionEmpty());
				removeItem.setEnabled(!table.isSelectionEmpty());

				if (table.isSelectionEmpty())
					return;

				Term selectedTerm = table.getFirstSelectedTerm();

				// call the selection listener if it was set
				if (selectionListener != null) {

					// return the selected term as event data
					Event event = new Event();
					event.data = selectedTerm;
					selectionListener.handleEvent(event);
				}
			}
		});
	}

	/**
	 * Add the listner which is called when a term is added to the table
	 * 
	 * @param listener
	 */
	public void addAddListener(Listener listener) {
		addListener = listener;
	}

	/**
	 * Add the listener which is called when a term is removed from the table
	 * 
	 * @param listener
	 */
	public void addRemoveListener(Listener listener) {
		removeListener = listener;
	}

	/**
	 * Add the listener which is called when Open is pressed
	 * 
	 * @param listener
	 */
	public void addOpenListener(Listener listener) {
		openListener = listener;
	}

	/**
	 * Add the listener which is called when a term is selected in the table
	 * 
	 * @param listener
	 */
	public void addSelectionListener(Listener listener) {
		selectionListener = listener;
	}

	/**
	 * Set the hierarchy we are working with
	 * 
	 * @param hierarchy
	 */
	public void setCurrentHierarchy(Hierarchy hierarchy) {
		table.setCurrentHierarchy(hierarchy);
	}

	/**
	 * Add a term into the table (will call add listener)
	 * 
	 * @param term
	 */
	public void addTerm(Term term) {

		if (term == null)
			return;

		// add the term
		table.addTerm(term);

		// a term was added, so the remove item can be used
		if (removeItem != null)
			removeItem.setEnabled(true);

		// if an add listener was set call it
		if (addListener != null) {

			// set as data the added term
			Event event = new Event();
			event.data = term;

			// call the listener
			addListener.handleEvent(event);
		}
	}

	/**
	 * Remove a term from the table (will call remove listener)
	 * 
	 * @param term
	 */
	public void removeTerm(Term term) {

		if (term == null)
			return;

		// remove the term from the table
		table.removeTerm(term);

		// a term was removed, if we have no more elements disable remove
		if (removeItem != null && table.getItemCount() == 0)
			removeItem.setEnabled(false);

		// if a remove listener was set call it
		if (removeListener != null) {

			// set as data the removed term
			Event event = new Event();
			event.data = term;

			// call the listener
			removeListener.handleEvent(event);
		}
	}

	/**
	 * Create a menu for the table (remove item)
	 * 
	 * @param shell
	 * @param table
	 * @return
	 */
	private Menu createMenu(Shell shell, final TermTable table) {

		// Right click menu for removing terms
		final Menu menu = new Menu(shell, SWT.POP_UP);

		openItem = new MenuItem(menu, SWT.PUSH);
		openItem.setText(Messages.getString("FormSelectTerm.OpenCmd"));

		removeItem = new MenuItem(menu, SWT.PUSH);
		removeItem.setText(Messages.getString("FormSelectTerm.RemoveCmd"));

		// set menu item icon
		Image removeIcon = new Image(Display.getCurrent(),
				this.getClass().getClassLoader().getResourceAsStream("remove-icon.png"));
		removeItem.setImage(removeIcon);
		removeItem.setEnabled(false);

		openItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// If the selection is not empty
				if (table.isSelectionEmpty())
					return;

				final Term term = table.getFirstSelectedTerm();

				if (openListener != null) {

					Event e = new Event();
					e.data = term;

					openListener.handleEvent(e);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// If remove command is pressed, remove the term selected
		removeItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// If the selection is not empty, then remove from the table the term selected
				if (table.isSelectionEmpty())
					return;

				final Term term = table.getFirstSelectedTerm();

				// remove the term from the table
				removeTerm(term);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		// if focus is lost => disable remove item from the menu
		table.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				removeItem.setEnabled(false);
				openItem.setEnabled(false);
			}

			@Override
			public void focusGained(FocusEvent e) {
			}
		});

		return menu;
	}

	/**
	 * Return true if the table contains the term
	 * 
	 * @param term
	 * @return
	 */
	public boolean contains(Term term) {
		return table.contains(term);
	}
}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
