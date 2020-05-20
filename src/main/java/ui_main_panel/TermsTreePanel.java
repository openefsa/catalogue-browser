package ui_main_panel;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import already_described_terms.DescribedTerm;
import already_described_terms.Picklist;
import already_described_terms.PicklistDAO;
import already_described_terms.RecentTermDAO;
import catalogue.Catalogue;
import catalogue.ReservedCatalogue;
import catalogue_browser_dao.DatabaseManager;
import catalogue_browser_dao.ForceCatEditDAO;
import catalogue_browser_dao.ReservedCatDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.TxtTermsParser;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import dcf_user.User;
import global_manager.GlobalManager;
import i18n_messages.CBMessages;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import term_clipboard.TermClipboard;
import term_clipboard.TermOrderChanger;
import term_code_generator.TermCodeException;
import ui_describe.FormDescribedTerms;
import ui_describe.FormTermCoder;
import ui_general_graphics.DialogSingleText;
import ui_search_bar.HierarchyChangedListener;
import ui_search_bar.HierarchyEvent;
import ui_search_bar.SearchBar;
import user_preferences.CataloguePreferenceDAO;
import utilities.GlobalUtil;

/**
 * Class to manage the main terms tree viewer (the one you can find in the main
 * page of the tool, at the center of the screen) and its contextual menu.
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class TermsTreePanel extends Observable implements Observer {

	private static final Logger LOGGER = LogManager.getLogger(TermsTreePanel.class);

	private Catalogue catalogue;
	private MultiTermsTreeViewer tree;
	private Shell shell;
	private Hierarchy selectedHierarchy; // current hierarchy, retrieved from observable

	private MenuItem otherHierarchies, deprecateTerm, termMoveDown, termMoveUp, termLevelUp, describe, recentTerms,
			addTerm, cutTerm, copyNode, copyBranch, copyCode, copyTerm, fullCopyTerm, pasteTerm, prefSearchTerm,
			favouritePicklist, addRootTerm, addTxtTerms, pasteRootTerm;

	private Listener updateListener;
	private HierarchyChangedListener changeHierarchyListener;
	private ISelectionChangedListener selectionListener;

	// term clip board to manage all the cut copy paste operations on terms
	TermClipboard termClip;

	// order changer to manage move up/move down/move level up/drag&drop operations
	// on terms
	TermOrderChanger termOrderChanger;

	// describe window
	private FormTermCoder tcf;

	/**
	 * Constructor
	 * 
	 * @param shell
	 * @param tree
	 */
	public TermsTreePanel(Composite parent, Catalogue catalogue) {

		this.shell = parent.getShell();

		this.catalogue = catalogue;

		// initialise the term clip board object
		termClip = new TermClipboard();

		// initialise the term order changer object
		termOrderChanger = new TermOrderChanger();

		// add the main tree viewer
		tree = createTreeViewer(parent);

		// create the describe
		// tcf = new FormTermCoder(shell, Messages.getString("FormTermCoder.Title"),
		// this.catalogue);

	}

	/**
	 * Set focus on tree
	 * 
	 * @author shahaal
	 */
	public void setTreeFocus() {
		this.tree.getTreeViewer().getControl().setFocus();
	}

	/**
	 * Called when something needs to be refreshed
	 * 
	 * @param listener
	 */
	public void addUpdateListener(Listener listener) {
		this.updateListener = listener;
	}

	/**
	 * Called when something needs to be refreshed
	 * 
	 * @param listener
	 */
	public void addDropListener(Listener listener) {
		tree.addDropFinishedListener(listener);
	}

	/**
	 * Called when the see in other hierarchies button is pressed in the event data
	 * there is the selected hierarchy
	 * 
	 * @param listener
	 */
	public void addChangeHierarchyListener(HierarchyChangedListener listener) {
		changeHierarchyListener = listener;
	}

	/**
	 * Called when the selection of the tree viewer changed
	 * 
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	/**
	 * Add the contextual menu to the tree
	 */
	public void addContextualMenu(boolean forceCreation) {

		Menu menu = null;

		// set the menu if no empty selection
		if (tree.isSelectionEmpty())
			menu = createEmptyMenu();
		// if the menu is not set yet, create it
		else
			menu = createTreeMenu();

		if (menu != null)
			tree.setMenu(menu);
		else
			tree.removeMenu();

		// when the tools menu is showed update the menu items status
		if (menu != null) {
			menu.addListener(SWT.Show, new Listener() {

				public void handleEvent(Event event) {
					refreshMenu();
				}
			});
		}
	}

	/**
	 * Get the selected terms from the tree viewer
	 * 
	 * @return
	 */
	public ArrayList<Term> getSelectedTerms() {
		return tree.getSelectedTerms();
	}

	/**
	 * Get the selected terms from the tree viewer
	 * 
	 * @return
	 */
	public ArrayList<Nameable> getSelectedObjs() {
		return tree.getSelectedObjs();
	}

	/**
	 * Get the first selected term of the tree if it is present
	 * 
	 * @return
	 */
	public Term getFirstSelectedTerm() {
		return tree.getFirstSelectedTerm();
	}

	/**
	 * Check if the tree has an empty selection or not
	 * 
	 * @return
	 */
	public boolean isSelectionEmpty() {
		return tree.isSelectionEmpty();
	}

	/**
	 * Refresh the tree viewer
	 */
	public void refresh(boolean label) {
		tree.refresh(label);
	}

	/**
	 * Refresh the tree viewer
	 */
	public void refresh() {
		tree.refresh();
	}

	/**
	 * Refresh a specific object of the tree
	 * 
	 * @param term
	 */
	public void refresh(Nameable term) {
		tree.refresh(term);
	}

	/**
	 * Select a term of the tree viewer
	 * 
	 * @param term
	 */
	public void selectTerm(Nameable term) {
		tree.selectTerm(term, 0);
	}

	/**
	 * Select a term of the tree viewer, expand tree to the selected level
	 * 
	 * @param term
	 */
	public void selectTerm(Nameable term, int level) {
		tree.selectTerm(term, level);
	}

	/**
	 * Expand all the selected terms
	 * 
	 * @param level
	 */
	public void expandSelectedTerms(int level) {

		for (Nameable term : getSelectedTerms())
			selectTerm(term, level);
	}

	/**
	 * Collapse all the selected terms
	 * 
	 * @param level
	 */
	public void collapseSelectedTerms(int level) {

		for (Nameable term : getSelectedTerms())
			collapseToLevel(term, level);
	}

	/**
	 * Collapse the tree to the selected object
	 * 
	 * @param term
	 * @param level
	 */
	public void collapseToLevel(Nameable term, int level) {
		tree.collapseToLevel(term, level);
	}

	/**
	 * Collapse the entire tree
	 */
	public void collapseAll() {
		tree.collapseAll();
	}

	/**
	 * Set the tree input
	 * 
	 * @param input
	 */
	public void setInput(Object input) {
		tree.setInput(input);
	}

	/**
	 * Create the main tree viewer in the parent composite
	 * 
	 * @param parent
	 * @param ReadOnly
	 * @return
	 */
	public MultiTermsTreeViewer createTreeViewer(Composite parent) {

		MultiTermsTreeViewer tree = new MultiTermsTreeViewer(parent, false,
				SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL, catalogue);

		// allow drag n drop
		tree.addDragAndDrop();

		// set the focus when enter the tree
		// check before if the search field is not selected
		tree.getTreeViewer().getTree().addListener(SWT.MouseEnter, new Listener() {
			public void handleEvent(Event event) {
				if (!SearchBar.flag)
					setTreeFocus();
			}
		});

		// single click
		tree.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {

				// if the selection is empty or bad instance return
				if (arg0.getSelection().isEmpty() || !(arg0.getSelection() instanceof IStructuredSelection))
					return;

				setChanged();
				notifyObservers();

				// add the menu to the tree if it was not set before
				addContextualMenu(false);

				if (selectionListener != null)
					selectionListener.selectionChanged(arg0);

			}
		});

		// double click on the term for directly opening the describe window
		tree.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent arg0) {
				openDescribeWindow();
			}
		});

		return tree;
	}

	/**
	 * Create an empty menu with only commands which can be executed without having
	 * data.
	 * 
	 * @return
	 */
	public Menu createEmptyMenu() {

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();

		if (!manager.isReadOnly()) {

			Menu termMenu = new Menu(shell, SWT.POP_UP);

			// new term operation
			addRootTerm = addNewRootTermMI(termMenu);

			return termMenu;
		} else
			return null;
	}

	/**
	 * Refresh the menu content
	 */
	public void refreshMenu() {

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();

		// if editing mode update edit mode buttons
		if (!manager.isReadOnly()) {

			ReserveLevel level = null;

			// if the editing of the catalogue is forced
			// we use the forced editing level to refresh
			// the UI, otherwise we get the standard
			// reserve level we have on the catalogue
			String username = User.getInstance().getUsername();

			ForceCatEditDAO forcedDao = new ForceCatEditDAO();
			ReservedCatDAO resDao = new ReservedCatDAO();

			ReserveLevel forcedLevel = forcedDao.getEditingLevel(catalogue, username);

			if (forcedLevel == null) {
				ReservedCatalogue reservedCat = resDao.getById(catalogue.getId());

				if (reservedCat != null)
					level = reservedCat.getLevel();
			} else {
				level = forcedLevel;
			}

			updateEditModeMI(level);
		}

		// update read only buttons
		updateReadOnlyMI();
	}

	/**
	 * Create a right click contextual menu for a tree which contains terms
	 */
	public Menu createTreeMenu() {

		// This will prevent accidental stacktrace error in case of closing the app when
		// the describe window is open

		/* Menu for the tree */
		Menu termMenu = null;

		if (!shell.isDisposed())
			termMenu = new Menu(shell, SWT.POP_UP);
		else
			return termMenu;
		//

		/* Menu which helps browsing the hierarchies among terms */

		otherHierarchies = addChangeHierarchyMI(termMenu);

		// Add edit buttons if we are in editing mode
		if (User.getInstance().canEdit(catalogue) && !catalogue.hasUpdate()) {

			new MenuItem(termMenu, SWT.SEPARATOR);

			// term order operations
			termMoveUp = addTermMoveUpMI(termMenu);

			termMoveDown = addTermMoveDownMI(termMenu);

			termLevelUp = addTermLevelUpMI(termMenu);

			new MenuItem(termMenu, SWT.SEPARATOR);

			// cut copy paste operations
			cutTerm = addCutBranchMI(termMenu);

			copyNode = addCopyNodeMI(termMenu);

			copyBranch = addCopyBranchMI(termMenu);

			pasteTerm = addPasteMI(termMenu);

			pasteRootTerm = addPasteRootMI(termMenu);

			new MenuItem(termMenu, SWT.SEPARATOR);

			// new term operation
			addTerm = addNewTermMI(termMenu);

			// new term operation
			addRootTerm = addNewRootTermMI(termMenu);

			// add import list of terms function
			addTxtTerms = addImportTermMI(termMenu);

			deprecateTerm = addDeprecateTermMI(termMenu);

			new MenuItem(termMenu, SWT.SEPARATOR);
		}

		// add copy term code
		copyCode = addCopyCodeMI(termMenu);

		// add copy term
		copyTerm = addCopyCodeNameMI(termMenu);

		// add copy full code
		fullCopyTerm = addCopyTermFullcodeMI(termMenu);

		// separator for cut paste elements and describe function
		new MenuItem(termMenu, SWT.SEPARATOR);

		// Describe term : describe function to add explicit facets to the terms
		describe = addDescribeMI(termMenu);

		// add recently described terms
		recentTerms = addRecentlyDescribedTermsMI(termMenu);

		// add favourite picklist
		favouritePicklist = addFavouritePicklistMI(termMenu);

		// search term in picklist
		prefSearchTerm = addSearchTermInPicklistMI(termMenu);

		return termMenu;
	}

	/**
	 * Update the menu item of the menu based on the selected term/hierarchy ONLY
	 * FOR EDITING MODE BUTTONS
	 */
	private void updateEditModeMI(ReserveLevel reserveLevel) {

		if (selectedHierarchy == null)
			return;

		boolean canEdit = User.getInstance().canEdit(catalogue) && !catalogue.hasUpdate();

		if (!canEdit) // buttons are not created if edit mode disabled
			return;

		boolean canEditMajor = catalogue.isLocal()
				|| (canEdit && reserveLevel != null && reserveLevel == ReserveLevel.MAJOR);

		boolean canAddTerm = canEdit && selectedHierarchy.isMaster();

		if (addRootTerm != null)
			addRootTerm.setEnabled(canAddTerm);

		// enable add only in master hierarchy
		if (addTerm != null)
			addTerm.setEnabled(!isSelectionEmpty() && canAddTerm);

		// enable is it is possible to add term
		if (addTxtTerms != null)
			addTxtTerms.setEnabled(canAddTerm);

		// can paste only if we are cutting/copying and we are pasting under a single
		// term
		if (pasteTerm != null)
			pasteTerm.setEnabled(canEdit && !termClip.getSources().isEmpty()
					&& termClip.canPaste(getFirstSelectedTerm(), selectedHierarchy));

		boolean canPasteAsRoot = true;
		for (Term source : termClip.getSources()) {
			if (source.isRootTerm(selectedHierarchy)) {
				canPasteAsRoot = false;
				break;
			}
		}

		// can paste only if we are cutting/copying and we are pasting in a hierarchy
		if (pasteRootTerm != null)
			pasteRootTerm.setEnabled(canEdit && canPasteAsRoot && !termClip.getSources().isEmpty()
					&& termClip.canPaste(selectedHierarchy, selectedHierarchy));

		// the others need a selected term
		if (isSelectionEmpty())
			return;

		// refresh deprecate term text
		if (getFirstSelectedTerm().isDeprecated()) {

			deprecateTerm.setText(CBMessages.getString("BrowserTreeMenu.RemoveDeprecation"));

			// allow only if the term has not deprecated parents
			// allow only for major releases
			deprecateTerm.setEnabled(canEditMajor && !getFirstSelectedTerm().hasDeprecatedParents());
		} else {
			deprecateTerm.setText(CBMessages.getString("BrowserTreeMenu.DeprecateTerm"));

			// allow only if the term has all the subtree deprecated, allow only for
			// major releases
			deprecateTerm.setEnabled(canEditMajor && getFirstSelectedTerm().hasAllChildrenDeprecated());
		}

		// check if the selected terms can be moved in the current hierarchy
		termMoveUp.setEnabled(canEdit && termOrderChanger.canMoveUp(getSelectedTerms(), selectedHierarchy));

		termMoveDown.setEnabled(canEdit && termOrderChanger.canMoveDown(getSelectedTerms(), selectedHierarchy));

		termLevelUp.setEnabled(canEdit && termOrderChanger.canMoveLevelUp(getSelectedTerms(), selectedHierarchy));

		cutTerm.setEnabled(canEdit);

		copyNode.setEnabled(canEdit);

		copyBranch.setEnabled(canEdit);
	}

	/**
	 * Update the menu item of the menu based on the selected term/hierarchy ONLY
	 * FOR NON EDITING MODE BUTTONS
	 */
	private void updateReadOnlyMI() {

		if (selectedHierarchy == null || tree.isSelectionEmpty())
			return;

		otherHierarchies.setEnabled(true);

		boolean hasFacetCategories = catalogue.hasImplicitFacetCategories() && (tcf == null || tcf.canOpen());

		// enable describe/recent terms and picklists only if we have facets
		describe.setEnabled(hasFacetCategories);
		recentTerms.setEnabled(hasFacetCategories);

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

		// Enable favourite picklist only if a favourite picklist was set
		favouritePicklist.setEnabled(hasFacetCategories && prefDao.hasFavouritePicklist());

		copyCode.setEnabled(true);
		copyTerm.setEnabled(true);
		fullCopyTerm.setEnabled(true);

		// search term in picklist, update text and enable
		prefSearchTerm.setEnabled(hasFacetCategories && prefDao.hasFavouritePicklist());
		prefSearchTerm.setText(CBMessages.getString("BrowserTreeMenu.SearchTermInPicklistPt1")
				+ getFirstSelectedTerm().getTruncatedName(10, true)
				+ CBMessages.getString("BrowserTreeMenu.SearchTermInPicklistPt2"));
	}

	/*
	 * ============================
	 * 
	 * TREE CONTEXTUAL MENU MENU ITEMS (SINGLE ELEMENTS)
	 * 
	 * 
	 * ============================/
	 * 
	 * /** Add a menu item which allows to browse the available hierarchies of the
	 * selected term
	 * 
	 * @param menu
	 */
	private MenuItem addChangeHierarchyMI(Menu menu) {

		// Change hierarchy menu item of the menu when right clicking item in the main
		// tree
		final MenuItem changeHierarchy = new MenuItem(menu, SWT.CASCADE);
		changeHierarchy.setText(CBMessages.getString("BrowserTreeMenu.SeeInOtherHierarchiesCmd"));

		// Initialize the menu"
		final Menu changeHierarchyMenu = new Menu(shell, SWT.DROP_DOWN);

		// Set the menu
		changeHierarchy.setMenu(changeHierarchyMenu);

		// Listener to the menu of hierarchies
		// when the menu is selected open the menu with the menuitem
		// the menuitem are the hierarchies that own the selected term
		changeHierarchyMenu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {

				// Return if no term is selected
				if (isSelectionEmpty())
					return;

				// reset the item of the menu, in order to update with the current term
				// hierarchies
				for (MenuItem item : changeHierarchyMenu.getItems())
					item.dispose();

				// get the hierarchies of the term
				ArrayList<Hierarchy> applHierarchies = getFirstSelectedTerm().getApplicableHierarchies();

				// if no hierarchy is found => return
				if (applHierarchies == null)
					return;

				// insert the all the applicable hierarchies in the menu
				for (int i = 0; i < applHierarchies.size(); i++) {

					// get the current hierarchy
					final Hierarchy hierarchy = applHierarchies.get(i);

					// create the menu item with the hierarchy label name
					MenuItem mi = new MenuItem(changeHierarchyMenu, SWT.PUSH);
					mi.setText(hierarchy.getLabel());

					// if a hierarchy is selected => go to the selected hierarchy (selected term)
					mi.addSelectionListener(new SelectionAdapter() {

						public void widgetSelected(SelectionEvent e) {

							if (changeHierarchyListener != null) {

								HierarchyEvent event = new HierarchyEvent();
								event.setHierarchy(hierarchy);
								event.setTerm(getFirstSelectedTerm());

								changeHierarchyListener.hierarchyChanged(event);
							}
						}
					});
				}
			}
		});

		changeHierarchy.setEnabled(false);

		return changeHierarchy;
	}

	/**
	 * Add a menu item which allows moving a term up
	 * 
	 * @param menu
	 */
	private MenuItem addTermMoveUpMI(Menu menu) {

		MenuItem termMoveUp = new MenuItem(menu, SWT.NONE);
		termMoveUp.setText(CBMessages.getString("BrowserTreeMenu.MoveUpCmd")); //$NON-NLS-1$

		termMoveUp.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// move up the selected terms
				termOrderChanger.moveUp(getSelectedTerms(), selectedHierarchy);

				// refresh tree
				tree.refresh();

				if (updateListener != null) {
					updateListener.handleEvent(new Event());
				}
			}
		});

		termMoveUp.setEnabled(false);

		return termMoveUp;
	}

	/**
	 * Add a menu item which allows moving down a term
	 * 
	 * @param menu
	 */
	private MenuItem addTermMoveDownMI(Menu menu) {

		MenuItem termMoveDown = new MenuItem(menu, SWT.NONE);
		termMoveDown.setText(CBMessages.getString("BrowserTreeMenu.MoveDownCmd")); //$NON-NLS-1$

		termMoveDown.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// move down the selected terms
				termOrderChanger.moveDown(getSelectedTerms(), selectedHierarchy);

				// refresh tree
				tree.refresh();

				if (updateListener != null) {
					updateListener.handleEvent(new Event());
				}
			}
		});

		termMoveDown.setEnabled(false);

		return termMoveDown;
	}

	/**
	 * Add a menu item which allows moving a term one level up
	 * 
	 * @param menu
	 */
	private MenuItem addTermLevelUpMI(Menu menu) {

		MenuItem termMoveLevelUp = new MenuItem(menu, SWT.NONE);
		termMoveLevelUp.setText(CBMessages.getString("BrowserTreeMenu.MoveLevelUpCmd")); //$NON-NLS-1$

		termMoveLevelUp.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				if (isSelectionEmpty())
					return;

				// move one level up the selected terms
				termOrderChanger.moveLevelUp(getSelectedTerms(), selectedHierarchy);

				// refresh the tree
				tree.refresh();
			}
		});

		termMoveLevelUp.setEnabled(false);

		return termMoveLevelUp;
	}

	/**
	 * Add a menu item which allows adding a new term as child of the selected term
	 * 
	 * @param menu
	 */
	private MenuItem addNewTermMI(Menu menu) {

		MenuItem termAdd = new MenuItem(menu, SWT.NONE);
		termAdd.setText(CBMessages.getString("BrowserTreeMenu.AddNewTermCmd"));

		termAdd.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				Term parent = getFirstSelectedTerm();

				if (parent == null)
					return;

				Term child = addNewTerm(parent, selectedHierarchy);

				// refresh tree
				tree.refresh();

				// if the update listener was set call it
				if (updateListener != null) {

					// pass as data the new term
					Event event = new Event();
					event.data = child;

					updateListener.handleEvent(event);
				}
			}
		});

		termAdd.setEnabled(false);

		return termAdd;
	}

	/**
	 * Add a menu item which allows adding a new term as child of the selected term
	 * 
	 * @param menu
	 */
	private MenuItem addNewRootTermMI(Menu menu) {

		MenuItem termAdd = new MenuItem(menu, SWT.NONE);
		termAdd.setText(CBMessages.getString("BrowserTreeMenu.AddNewRootTermCmd"));

		termAdd.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				Term child = addNewTerm(selectedHierarchy, selectedHierarchy);

				if (child == null)
					return;

				// refresh tree
				tree.refresh();

				// if the update listener was set call it
				if (updateListener != null) {

					// pass as data the new term
					Event event = new Event();
					event.data = child;

					updateListener.handleEvent(event);
				}
			}
		});

		termAdd.setEnabled(false);

		return termAdd;
	}

	/**
	 * Add a menu item which allows importing a txt file containing a list of terms
	 * The file must be exported from excel as Tab delimiter txt file with the
	 * following columns: [parentCode, termExtendedName, termScopenote,
	 * termScientificName]
	 * 
	 * @author shahaal
	 * @param menu
	 */
	private MenuItem addImportTermMI(Menu menu) {

		MenuItem termAdd = new MenuItem(menu, SWT.NONE);
		termAdd.setText(CBMessages.getString("BrowserTreeMenu.AddImportTermsTxt"));

		termAdd.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				String mes = CBMessages.getString("BrowserTreeMenu.WarningMessage");
				String title = CBMessages.getString("BrowserTreeMenu.ShellTitle");

				// ask the user before continuing the operation
				boolean confirmation = MessageDialog.openQuestion(shell, title, mes);

				if (!confirmation)
					return;

				// create dialog
				FileDialog fd = new FileDialog(shell, SWT.OPEN);

				// set dialog title
				fd.setText(title);

				// get the working directory from the user preferences
				fd.setFilterPath(DatabaseManager.MAIN_CAT_DB_FOLDER);

				// select only txt files
				String[] filterExt = { "*.txt" };
				fd.setFilterExtensions(filterExt);

				// open dialog a listen to get the selected filename
				String filename = fd.open();

				if (filename != null && !filename.isEmpty()) {

					GlobalUtil.setShellCursor(shell, SWT.CURSOR_WAIT);

					// parse the file as a csv semicolon separated file
					TxtTermsParser parse = new TxtTermsParser(catalogue, filename);

					// for each term in txt add it
					int termsAdded = 0;

					try {
						termsAdded = parse.startToImportTerms();
					} catch (TermCodeException e1) {
						GlobalUtil.showDialog(shell, title, e1.getMessage(), SWT.ICON_ERROR);
					}

					mes = "Imported " + termsAdded + " Terms.";
					GlobalUtil.showDialog(shell, title, mes, SWT.ICON_INFORMATION);

					GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);

					// refresh tree
					tree.refresh();
				}
			}

		});

		termAdd.setEnabled(false);

		return termAdd;
	}

	private Term addNewTerm(Nameable parent, Hierarchy hierarchy) {

		Term child;

		// if we do not have a term code mask we need to ask to the
		// user the term code
		if (catalogue.getTermCodeMask() == null || catalogue.getTermCodeMask().isEmpty()) {

			String code = askTermCode();

			if (code == null)
				return null;

			child = catalogue.addNewTerm(code, parent, selectedHierarchy);

		} else {

			// create a new default term with default attributes as child
			// of the selected term in the selected hierarchy
			try {
				child = catalogue.addNewTerm(parent, selectedHierarchy);
			} catch (TermCodeException e) {
				e.printStackTrace();
				LOGGER.error("Max term code reached", e);

				GlobalUtil.showErrorDialog(shell, CBMessages.getString("NewTerm.MaxCodeReachedTitle"),
						CBMessages.getString("NewTerm.MaxCodeReachedMessage"));

				String code = askTermCode();

				if (code == null)
					return null;

				child = catalogue.addNewTerm(code, parent, selectedHierarchy);
			}
		}

		return child;
	}

	private String askTermCode() {

		TermDAO termDao = new TermDAO(catalogue);

		DialogSingleText dialog = new DialogSingleText(shell, 1);
		dialog.setTitle(CBMessages.getString("NewTerm.Title"));
		dialog.setMessage(CBMessages.getString("NewTerm.Message"));
		String code = dialog.open();

		if (code == null)
			return null;

		// check if the selected code is already present or not in the db
		if (termDao.getByCode(code) != null) {

			GlobalUtil.showErrorDialog(shell, CBMessages.getString("NewTerm.DoubleCodeTitle"),
					CBMessages.getString("NewTerm.DoubleCodeMessage"));
			return null;
		}

		return code;
	}

	/**
	 * Add a menu item which allows to deprecate or to remove deprecation from the
	 * selected term
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addDeprecateTermMI(Menu menu) {

		MenuItem deprecateTerm = new MenuItem(menu, SWT.NONE);
		deprecateTerm.setText(CBMessages.getString("BrowserTreeMenu.DeprecateTerm"));

		deprecateTerm.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// invert the deprecated status
				getFirstSelectedTerm().setDeprecated(!getFirstSelectedTerm().isDeprecated());

				TermDAO termDao = new TermDAO(catalogue);

				// update the term into the database
				termDao.update(getFirstSelectedTerm());

				// refresh tree
				tree.refresh();

				// if the update listener was set call it
				if (updateListener != null) {

					// pass as data the new term
					Event event = new Event();
					event.data = getFirstSelectedTerm();

					updateListener.handleEvent(event);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		deprecateTerm.setEnabled(false);

		return deprecateTerm;
	}

	/**
	 * Add a menu item which allows cutting a term
	 * 
	 * @param menu
	 */
	private MenuItem addCutBranchMI(Menu menu) {

		MenuItem cutTerm = new MenuItem(menu, SWT.NONE);
		cutTerm.setText(CBMessages.getString("BrowserTreeMenu.CutCmd"));

		cutTerm.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// cut branches for all the selected terms
				termClip.cutBranch(getSelectedTerms(), selectedHierarchy);
			}
		});

		cutTerm.setEnabled(false);

		return cutTerm;
	}

	/**
	 * Add a menu item which allows copying a term without its subtree in other
	 * hierarchies
	 * 
	 * @param menu
	 */
	private MenuItem addCopyNodeMI(Menu menu) {

		MenuItem copyNode = new MenuItem(menu, SWT.NONE);
		copyNode.setText(CBMessages.getString("BrowserTreeMenu.CopyNodeCmd"));

		copyNode.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// copy only the selected terms without the subtree
				termClip.copyNode(getSelectedTerms(), selectedHierarchy);
			}
		});

		copyNode.setEnabled(false);

		return copyNode;
	}

	/**
	 * Add a menu item which allows copying a term with all its subtree in other
	 * hierarchies
	 * 
	 * @param menu
	 */
	private MenuItem addCopyBranchMI(Menu menu) {

		MenuItem copyBranch = new MenuItem(menu, SWT.NONE);
		copyBranch.setText(CBMessages.getString("BrowserTreeMenu.CopyBranchCmd"));

		copyBranch.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// copy the entire branches under the selected terms
				termClip.copyBranch(getSelectedTerms(), selectedHierarchy);
			}
		});

		copyBranch.setEnabled(false);

		return copyBranch;
	}

	/**
	 * Add a menu item which allows pasting a previously copied term
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addPasteMI(Menu menu) {

		MenuItem pasteTerm = new MenuItem(menu, SWT.NONE);
		pasteTerm.setText(CBMessages.getString("BrowserTreeMenu.PasteCmd"));
		pasteTerm.setEnabled(false);

		pasteTerm.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// paste the previous term under the new selected term under the new selected
				// hierarchy
				termClip.paste(getFirstSelectedTerm(), selectedHierarchy);

				// refresh tree
				tree.refresh();

				// call the update listener if it was set
				if (updateListener != null) {
					updateListener.handleEvent(new Event());
				}
			}
		});

		pasteTerm.setEnabled(false);

		return pasteTerm;
	}

	/**
	 * Add a menu item which allows pasting a previously copied term
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addPasteRootMI(Menu menu) {

		MenuItem pasteTerm = new MenuItem(menu, SWT.NONE);
		pasteTerm.setText(CBMessages.getString("BrowserTreeMenu.PasteRootCmd"));
		pasteTerm.setEnabled(false);

		pasteTerm.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// paste the previous term under the new selected term under the new selected
				// hierarchy
				termClip.paste(selectedHierarchy, selectedHierarchy);

				// refresh tree
				tree.refresh();

				// call the update listener if it was set
				if (updateListener != null) {
					updateListener.handleEvent(new Event());
				}
			}
		});

		pasteTerm.setEnabled(false);

		return pasteTerm;
	}

	/**
	 * Add a menu item which allows copying a term code
	 * 
	 * @param menu
	 */
	private MenuItem addCopyCodeMI(Menu menu) {

		/* setting copy only code in menu item */
		MenuItem copycode = new MenuItem(menu, SWT.NONE);
		copycode.setText(CBMessages.getString("BrowserTreeMenu.CopyCmd"));

		copycode.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// copy the term code
				termClip.copyCode(getSelectedTerms());
			}

		});

		copycode.setEnabled(false);

		return copycode;
	}

	/**
	 * Add a menu item which allows copying a term
	 * 
	 * @param menu
	 */
	private MenuItem addCopyCodeNameMI(Menu menu) {

		/* setting copy and name in menu item */
		MenuItem copyCodeName = new MenuItem(menu, SWT.NONE);
		copyCodeName.setText(CBMessages.getString("BrowserTreeMenu.CopyCodeNameCmd"));

		copyCodeName.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// copy the term code and name
				termClip.copyCodeName(getSelectedTerms());
			}
		});

		copyCodeName.setEnabled(false);

		return copyCodeName;
	}

	/**
	 * Add a menu item which allows copying the full code of a term
	 * 
	 * @param menu
	 */
	private MenuItem addCopyTermFullcodeMI(Menu menu) {

		MenuItem fullCopyTerm = new MenuItem(menu, SWT.NONE);
		fullCopyTerm.setText(CBMessages.getString("BrowserTreeMenu.CopyFullCodeNameCmd"));

		fullCopyTerm.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// copy the term full code and name
				termClip.copyFullCodeName(getSelectedTerms());
			}
		});

		fullCopyTerm.setEnabled(false);

		return fullCopyTerm;
	}

	/**
	 * Add a menu item which allows opening the describe on the selected term
	 * 
	 * @author shahaal
	 * @param menu
	 */
	private MenuItem addDescribeMI(Menu menu) {

		MenuItem describeTerm = new MenuItem(menu, SWT.NONE);

		describeTerm.setText(CBMessages.getString("BrowserTreeMenu.DescribeCmd"));

		// if describe is clicked and not describe instance exists
		describeTerm.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				openDescribeWindow();
			}

		});

		describeTerm.setEnabled(false);
		return describeTerm;
	}

	/**
	 * method used for checking the catalogue and if MTX open the describe window
	 * 
	 */
	protected void openDescribeWindow() {

		// return if non mtx catalogue
		if (!catalogue.isMTXCatalogue()) {
			showCatalogueError();
			return;
		}

		// initialize the describe window if null
		if (tcf == null)
			tcf = new FormTermCoder(shell, CBMessages.getString("FormTermCoder.Title"), catalogue);

		// set the base term
		tcf.setBaseTerm(getFirstSelectedTerm());

		// open the window if not opened or null
		if (tcf.canOpen())
			tcf.display(catalogue);
	}

	/**
	 * Add a menu item which allows opening the recently described terms form
	 * 
	 * @param menu
	 */
	private MenuItem addRecentlyDescribedTermsMI(Menu menu) {

		final MenuItem recentlyDescribeTerm = new MenuItem(menu, SWT.NONE);
		recentlyDescribeTerm.setText(CBMessages.getString("BrowserTreeMenu.RecentTermCmd"));

		recentlyDescribeTerm.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// return if non mtx catalogue
				if (!catalogue.isMTXCatalogue()) {
					showCatalogueError();
					return;
				}

				RecentTermDAO recentDao = new RecentTermDAO(catalogue);

				// remove all the old terms from the recent terms
				// before showing them to the user
				recentDao.removeOldTerms();

				// load the list of terms: favourite terms or recently described terms
				// (invertOrder is used to
				// make the recent results in inverse order, that is, from the more recent to
				// the less recent)
				ArrayList<DescribedTerm> describedTerms = recentDao.getAll();

				// show the window which allows to retrieve the last ten described terms
				FormDescribedTerms rdt = new FormDescribedTerms(shell,
						CBMessages.getString("BrowserTreeMenu.RecentTermWindowTitle"), catalogue, describedTerms);

				// display the window
				rdt.display(catalogue);

				// add listener to load button
				rdt.setLoadListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent arg0) {

						// initialise the describe window if null
						if (tcf == null)
							tcf = new FormTermCoder(shell, CBMessages.getString("FormTermCoder.Title"), catalogue);

						// return if cannot open the describe window
						if (!tcf.canOpen())
							return;

						// get the selected term
						DescribedTerm term = rdt.loadTermInDescribe();

						if (term != null) {

							// load the described term into the describe window
							tcf.loadDescribedTerm(term);

							// show the window and add the facet
							tcf.display(catalogue);
						}

					}

					@Override
					public void widgetDefaultSelected(SelectionEvent arg0) {

					}
				});
			}
		});

		return recentlyDescribeTerm;
	}

	/**
	 * method used to show warning regarding the catalogue currently in use
	 */
	protected void showCatalogueError() {
		GlobalUtil.showDialog(shell, CBMessages.getString("TableFacetApplicability.AddFacetWarningTitle"),
				CBMessages.getString("TableFacetApplicability.AddFacetWarningMessage"), SWT.ICON_WARNING);
	}

	/**
	 * Add a menu item which allows opening the favourite pick list form
	 * 
	 * @param menu
	 */
	private MenuItem addFavouritePicklistMI(Menu menu) {

		MenuItem picklistMenuItem = new MenuItem(menu, SWT.NONE);
		picklistMenuItem.setText(CBMessages.getString("BrowserTreeMenu.PicklistCmd"));

		picklistMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

				// get the current picklist
				Picklist picklist = prefDao.getFavouritePicklist();

				// show the window which shows all the terms of a favourite pick list
				FormDescribedTerms rdt = new FormDescribedTerms(shell,
						CBMessages.getString("BrowserTreeMenu.PicklistWindowTitle"), catalogue, picklist.getTerms());

				rdt.display(catalogue);
			}
		});

		picklistMenuItem.setEnabled(false);

		return picklistMenuItem;
	}

	/**
	 * Add a menu item which allows searching a term inside a picklist
	 * 
	 * @param menu
	 */
	private MenuItem addSearchTermInPicklistMI(Menu menu) {

		// Tab to search the selected term into the favourite picklist
		// in particular, the search should find all the terms which contains
		// the selectedTerm as implicit or explicit facets

		MenuItem prefSearchTerm = new MenuItem(menu, SWT.NONE);
		prefSearchTerm.setText(CBMessages.getString("BrowserTreeMenu.SearchTermInPicklistCmd")); //$NON-NLS-1$

		prefSearchTerm.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

				// get the current picklist
				final Picklist picklist = prefDao.getFavouritePicklist();

				PicklistDAO pickDao = new PicklistDAO(catalogue);

				// show the window which shows all the terms of a favourite pick list
				FormDescribedTerms rdt = new FormDescribedTerms(shell,
						CBMessages.getString("BrowserTreeMenu.PicklistWindowTitle"), catalogue,
						pickDao.searchTermInPicklist(picklist, getFirstSelectedTerm()));

				rdt.display(catalogue);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		prefSearchTerm.setEnabled(false);

		return prefSearchTerm;
	}

	/**
	 * set the new catalogue
	 * 
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Called by the observable
	 * 
	 * @author shahaal
	 */
	@Override
	public void update(Observable o, Object data) {

		tree.update(o, data);

		// update current hierarchy
		if (o instanceof HierarchySelector) {

			selectedHierarchy = ((HierarchySelector) o).getSelectedHierarchy();

			tree.setHierarchy(selectedHierarchy);
		}

		// update current catalogue
		if (data instanceof Catalogue) {

			catalogue = (Catalogue) data;

			if (catalogue != null) {
				// set the hierarchy of the tree based on the new catalogue
				tree.setHierarchy(catalogue.getDefaultHierarchy());
				// initialise the describe window when catalogue is changed
				if (tcf != null)
					tcf = null;
			}
		}

		// update selected element from history
		// if (o instanceof TermHistory) {
		// Term term = (Term) data;
		// tree.selectTerm(term);
		// }

	}
}
