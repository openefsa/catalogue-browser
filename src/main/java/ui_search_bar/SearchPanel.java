package ui_search_bar;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * This class displays a search bar and a table to host the search results.
 * 
 * @author shahaal
 * @author avonva
 */
public class SearchPanel implements Observer {

	private SearchBar searchBar; // search bar for making searches
	private TermTable table; // the table viewer which contains the search results

	public TermTable getTable() {
		return table;
	}

	private SearchListener searchListener;
	private HierarchyChangedListener hierarchyListener;
	private Listener infoListener;

	private Menu menu;

	/**
	 * Constructor, creates the table viewer and its menu (with their listeners)
	 * 
	 * @param parent
	 * @param ReadOnly
	 * @param listener : called if a menu item is clicked
	 */
	public SearchPanel(Composite parent, boolean addGlobalSearch, Catalogue catalogue) {

		// create the search bar
		searchBar = new SearchBar(parent, addGlobalSearch);

		// create the graphics of the search bar
		searchBar.display();

		// at the beginning we set all disabled (no catalogue is opened)
		searchBar.setEnabled(false);

		// table to show the results
		table = new TermTable(parent, catalogue);

		searchBar.setListener(new SearchListener() {

			@Override
			public void searchPerformed(SearchEvent event) {

				table.removeAll();

				// get the search results from the searchBar
				ArrayList<Term> terms = event.getResults();

				table.setCurrentHierarchy(searchBar.getSearchHierarchy());

				// Update the list search input with the
				table.setInput(terms);

				// call the caller listener
				if (searchListener != null) {
					searchListener.searchPerformed(event);
				}
			}
		});

		// create the menu
		menu = addSearchMenu(parent.getShell(), table);
		// add it to the table
		table.addMenu(menu);

	}

	/**
	 * Add a contextual menu to the search results table
	 * 
	 * @param parent
	 * @param tree
	 * @return
	 */
	private Menu addSearchMenu(Shell parent, final TermTable table) {

		// Right click menu for search results
		final Menu tableMenu = new Menu(parent.getShell(), SWT.POP_UP);

		// if the menu is shown, add the menu item
		// we show the applicable hierarchies of the selected term
		// and if the menu item is pressed a listener is called
		// to perform external actions (as moving the tree viewer
		// to the selected term in the selected hierarchy)
		tableMenu.addListener(SWT.Show, new Listener() {

			public void handleEvent(Event event) {

				// dispose the menu items of the menu
				GlobalUtil.disposeMenuItems(tableMenu);

				// return if the selection is empty
				if (table.isSelectionEmpty())
					return;

				// get selected term
				final Term selectedTerm = table.getFirstSelectedTerm();

				// create the menu items for the applicable hierarchies

				// get the applicable hierarchies of the term
				ArrayList<Hierarchy> termApplicableHierarchies = selectedTerm.getApplicableHierarchies();

				// if there are applicable hierarchies then
				if (termApplicableHierarchies != null) {

					// for each applicable hierarchy
					for (int i = 0; i < termApplicableHierarchies.size(); i++) {

						// get the current hierarchy
						final Hierarchy selectedHierarchy = termApplicableHierarchies.get(i);

						// add a menu item for each applicable hierarchy
						// if a menu item is clicked an external listener is called passing
						// as parameter the selected hierarchy and the selected term
						addAppHierarchyMI(tableMenu, selectedHierarchy, selectedTerm);
					}

					// add the show info option
					addShowTermInfoMI(tableMenu, selectedTerm);
				}
			}
		});

		return tableMenu;
	}

	/**
	 * Add to the Menu a menu item which will display an applicable hierarchy If the
	 * menu item is clicked the external listener is called passing the selected
	 * term and the selected hierarchy, in order to perform external operations
	 * based on the menu item selection
	 * 
	 * @param menu
	 * @param currentApplicableHierarchy
	 * @param selectedTerm
	 * @return
	 */
	private void addAppHierarchyMI(Menu menu, final Hierarchy currentApplicableHierarchy, final Term selectedTerm) {

		// create a menu item named with the current applicable hierarchy name
		MenuItem mi = new MenuItem(menu, SWT.PUSH);
		mi.setText(currentApplicableHierarchy.getLabel());

		// if the menu item is pressed, we call the external listener
		// passing as data the selected term and the selected hierarchy
		// in an array list. The caller of this class will fill the listener
		// for example with code for updating the main tree panel position
		// of the Catalogue Browser
		mi.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				if (hierarchyListener != null) {

					// create the event
					HierarchyEvent event = new HierarchyEvent();
					event.setHierarchy(currentApplicableHierarchy);
					event.setTerm(selectedTerm);

					// call the external listener to update the
					// main thread
					hierarchyListener.hierarchyChanged(event);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

	}

	/**
	 * add the option to the menu which allow to see only the info of the selected
	 * term without expanding it into the tree viewer
	 * 
	 * @author shahaal
	 * 
	 * @param menu
	 * @param currentApplicableHierarchy
	 * @param selectedTerm
	 */
	private void addShowTermInfoMI(Menu menu, final Term selectedTerm) {

		// create a menu item named with the current applicable hierarchy name
		MenuItem mi = new MenuItem(menu, SWT.PUSH);
		mi.setText(CBMessages.getString("SearchBar.ShowTermInfo"));

		// if the menu item is pressed, we call the external listener
		// passing as data the selected term. The caller of this class will fill the
		// listener
		// for example with code for updating the main term information panel
		mi.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				if (infoListener != null)
					infoListener.handleEvent(e);
			}
		});

	}

	/**
	 * Enable/disable the widget
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		searchBar.setEnabled(enabled);
		table.setEnabled(enabled);
	}

	/**
	 * Refresh the panel
	 * 
	 * @param label
	 */
	public void refresh(boolean label) {
		table.refresh(label);
	}

	/**
	 * Remove all the input
	 */
	public void removeAll() {
		table.removeAll();
		table.setInput(null);
	}

	/**
	 * Add listener to the event: a search was performed The listener has in its
	 * data the search results
	 * 
	 * @param listener
	 */
	public void addSearchListener(SearchListener searchListener) {
		this.searchListener = searchListener;
	}

	/**
	 * Add listener to: a term is selected in the search results table
	 * 
	 * @param listener
	 */
	public void addSelectionListener(Listener listener) {
		this.table.addSelectionListener(listener);
	}

	/**
	 * Add listener if right clicked on an item of the table
	 * 
	 * @param mousedown
	 * 
	 * @param listener
	 */
	public void addMenuListener(Listener listener) {
		this.table.addMenuListener(listener);
	}

	/**
	 * Add listener to: a hierarchy is selected from the applicable hierarchies of a
	 * term using the contextual menu in the search results table.
	 * 
	 * @param hierarchyListener
	 */
	public void addHierarchyListener(HierarchyChangedListener hierarchyListener) {
		this.hierarchyListener = hierarchyListener;
	}

	/**
	 * Add listener to: show all term info in the term panel
	 * 
	 * @param hierarchyListener
	 */
	public void addTermInfoListener(Listener infoListener) {
		this.infoListener = infoListener;
	}

	/**
	 * Set the search button as the default for the input shell.
	 * 
	 * @return
	 */
	public void addDefaultButton(Shell shell) {
		shell.setDefaultButton(searchBar.getButton());
	}

	public void setCatalogue(Catalogue catalogue) {
		searchBar.setCatalogue(catalogue);
	}

	/**
	 * make the menu visible
	 * 
	 * @author shahaal
	 */
	public void showMenu() {
		menu.setVisible(true);
	}

	@Override
	public void update(Observable arg0, Object arg1) {

		// update the search bar
		searchBar.update(arg0, arg1);

		// update table
		table.update(arg0, arg1);
	}
}
