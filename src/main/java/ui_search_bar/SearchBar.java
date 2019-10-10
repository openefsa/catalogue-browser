package ui_search_bar;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import global_manager.GlobalManager;
import i18n_messages.CBMessages;
import ui_main_panel.HierarchySelector;
import ui_main_panel.TermFilter;
import user_preferences.CataloguePreference;
import user_preferences.CataloguePreferenceDAO;

/**
 * This class implements a search bar which allows to perform a search in the
 * database In particular, you can write the keywords you want to search and set
 * some settings: - search as exact match, any word or all words - search in the
 * selected hierarchy or globally
 * 
 * When the search button is pressed, the search is performed. When the results
 * are ready to be used by the program, a listener is called to update the main
 * thread that the search is finished and it can use the results.
 * 
 * @author avonva
 * @author shahaal
 */
public class SearchBar implements Observer {

	private Catalogue catalogue;

	private Term rootTerm;
	private Text textSearch;
	private Combo comboOptSearch;
	private Button buttonSearch;
	private Button localSearch;
	private Button globalSearch;
	private boolean hideDeprecated;
	private boolean hideNotInUse;
	public static boolean flag; // used for determinate the focus over the main UI

	private SearchListener listener;

	// hierarchy which is currently opened in the browser
	private Hierarchy currentHierarchy;

	// hierarchy in which we make the search
	private Hierarchy searchHierarchy;

	// Array list which contains the results of the search (terms)
	private ArrayList<Term> searchResults = new ArrayList<>();

	// composite which contains the search bar
	Composite parent;
	boolean addGlobalSearch; // should global search button be added?
	boolean globalSearchEnabled = false;

	/**
	 * @wbp.parser.entryPoint
	 */
	public SearchBar(Composite parent, boolean addGlobalSearch) {
		this.parent = parent;
		this.addGlobalSearch = addGlobalSearch;
	}

	/**
	 * Set the catalogue on which we want to search
	 * 
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Set the entire search panel enabled or disabled Note that if a catalogue was
	 * not set, the search will give errors.
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {

		comboOptSearch.setEnabled(enabled);
		buttonSearch.setEnabled(enabled);
		textSearch.setEnabled(enabled);

		if (catalogue != null)
			textSearch.setText("");

		if (addGlobalSearch) {

			// reset the default settings
			localSearch.setSelection(true);
			globalSearch.setSelection(false);

			localSearch.setEnabled(enabled);
			globalSearch.setEnabled(enabled);
		}
	}

	/**
	 * Restrict the search space to a single hierarchy
	 */
	public void setCurrentHierarchy(Hierarchy hierarchy) {
		this.currentHierarchy = hierarchy;
	}

	/**
	 * Get the hierarchy in which the search was performed null if no search was
	 * performed yet
	 * 
	 * @return
	 */
	public Hierarchy getSearchHierarchy() {
		return searchHierarchy;
	}

	/**
	 * Get the written keyword (before clean it)
	 * 
	 * @author shahaal
	 * @return
	 */
	public String getKeyword() {

		if (textSearch == null)
			return "";

		String textTyped = textSearch.getText();

		///////// TEXT CLEANER (used in AI browser)
		// 1-replace multiple white spaces with a single one
		textTyped = textTyped.replaceAll("\\s+", " ");
		// 2-remove irrelevant punctuation (not numbers, '-', '_')
		//textTyped = textTyped.replaceAll("[\\p{Punct}&&[^_-]]+", " ").toLowerCase();
		// 3-trim all group of spaces generated with a single one
		//textTyped = textTyped.trim().replaceAll("\\s{2,}", " ");
		return textTyped;
	}

	/**
	 * Get the search option which was selected before
	 * 
	 * @return
	 */
	public SearchType getSearchMode() {

		if (comboOptSearch == null)
			return SearchType.EXACT_MATCH;

		// default
		SearchType type = SearchType.EXACT_MATCH;

		switch (comboOptSearch.getSelectionIndex()) {
		case 0:
			type = SearchType.EXACT_MATCH;
			break;
		case 1:
			type = SearchType.ANY_WORD;
			break;
		case 2:
			type = SearchType.ALL_WORDS;
			break;
		default:
			break;
		}
		;

		return type;
	}

	/**
	 * Get the search results ( it is filled only when the search button is pressed
	 * )
	 * 
	 * @return
	 */
	public ArrayList<Term> getSearchResults() {

		return searchResults;
	}

	/**
	 * Get the search button
	 * 
	 * @return
	 */
	public Button getButton() {
		return buttonSearch;
	}

	/**
	 * Get the search text box
	 * 
	 * @return
	 */
	public Text getText() {
		return textSearch;
	}

	/**
	 * Set the listener for the search
	 * 
	 * @param listener
	 */
	public void setListener(SearchListener listener) {
		this.listener = listener;
	}

	/**
	 * Update the search globally feature (used to restore previous state)
	 * 
	 * @param searchGlobally
	 */
	public void setSearchGlobally(boolean searchGlobally) {

		// if global search is not allowed return
		if (!addGlobalSearch)
			return;

		// otherwise update the selection of the radio buttons
		globalSearch.setSelection(searchGlobally);
		localSearch.setSelection(!searchGlobally);
	}

	/**
	 * Get the minimum number of characters needed to perform a search
	 * 
	 * @return
	 */
	private int getMinSearchChar() {

		// if we have a catalogue open check which is the min search char preference
		// otherwise always allows
		int minSearchChar = -1;

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

		minSearchChar = prefDao.getPreferenceIntValue(CataloguePreference.minSearchChar, 3);

		return minSearchChar;

	}

	/**
	 * Check if we can perform a search or not based on the number of character we
	 * inputed in the text box
	 * 
	 * @return
	 */
	private boolean canSearch(int inputedCharacters) {
		return inputedCharacters >= getMinSearchChar();
	}

	/**
	 * Display the search bar, instantiate the UI
	 */
	public void display() {
		
		// Setting the "search" widget
		Composite searchComposite = new Composite(parent, SWT.NONE);
		searchComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// if no global buttons place all the element on one row
		int numberOfColumns = addGlobalSearch ? 3 : 4;

		searchComposite.setLayout(new GridLayout(numberOfColumns, false));

		// Search label
		Label labelSearch = new Label(searchComposite, SWT.NONE);
		labelSearch.setText(CBMessages.getString("SearchBar.SearchLabel"));

		// Search text box (where you write keywords)
		textSearch = addSearchTextBox(searchComposite);

		// listener for text changes (enable/disable button search)
		textSearch.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {

				buttonSearch.setEnabled(canSearch(textSearch.getText().trim().length()) && textSearch.isEnabled());
			}
		});

		/**
		 * Remove the selection if the focus is lost
		 * 
		 * @author shahaal
		 */
		textSearch.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				parent.getDisplay().asyncExec(new Runnable() {
					public void run() {
						flag = textSearch.isFocusControl();
						textSearch.clearSelection();
					}
				});
			}

			@Override
			public void focusGained(FocusEvent arg0) {
				parent.getDisplay().asyncExec(new Runnable() {
					public void run() {
						// set a public flag to true so to dont loose the focus if the user move the
						// mouse around the ui
						flag = textSearch.isFocusControl();
						textSearch.selectAll();

					}
				});
			}
		});

		// search options, all words, any word...
		comboOptSearch = addSearchOptions(searchComposite);

		// add global search button if required
		if (addGlobalSearch) {

			Composite g = new Composite(searchComposite, SWT.NONE);
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			gridData.horizontalSpan = 3;
			g.setLayoutData(gridData);
			RowLayout r = new RowLayout();
			r.justify = true;
			r.center = true;
			g.setLayout(r);

			// add global search radio button
			addGlobalSearch(g);

			buttonSearch = new Button(g, SWT.PUSH);// searchGroup
		} else
			buttonSearch = new Button(searchComposite, SWT.PUSH);// searchGroup

		buttonSearch.setAlignment(SWT.CENTER);
		buttonSearch.setText(CBMessages.getString("SearchBar.GoButton"));
		buttonSearch.setEnabled(false); // until a keyword is added, disabled
		buttonSearch.pack();

		buttonSearch.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// change the cursor to wait
				Cursor cursor = new Cursor(parent.getDisplay(), SWT.CURSOR_WAIT);
				parent.getShell().setCursor(cursor);

				// call the external listener to say that the search results are ready to be
				// used
				searchResults = search(getKeyword(), getSearchMode());

				// dispose the old cursor and instantiate the new one
				if (cursor != null)
					cursor.dispose();

				// reload the old cursor, the search is finished
				cursor = new Cursor(parent.getDisplay(), SWT.CURSOR_ARROW);
				parent.getShell().setCursor(cursor);

				if (listener != null) {

					SearchEvent event = new SearchEvent();
					event.setResults(searchResults);

					// call the search listener
					listener.searchPerformed(event);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	/**
	 * Add global search button if required
	 * 
	 * @param parent
	 */
	private void addGlobalSearch(Composite parent) {

		localSearch = new Button(parent, SWT.RADIO);
		localSearch.setText(CBMessages.getString("SearchBar.SearchCurrentButton"));

		globalSearch = new Button(parent, SWT.RADIO);
		globalSearch.setText(CBMessages.getString("SearchBar.SearchDictionaryButton"));

		/* setting local/global search */
		localSearch.setSelection(true);
		globalSearch.setSelection(false);

		globalSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				globalSearch.setSelection(true);
				localSearch.setSelection(false);
				globalSearchEnabled = true;
			}
		});

		localSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				localSearch.setSelection(true);
				globalSearch.setSelection(false);
				globalSearchEnabled = false;
			}
		});
	}

	/**
	 * Add a text box for search
	 * 
	 * @param parent
	 * @return
	 */
	private Text addSearchTextBox(Composite parent) {

		Text textSearch = new Text(parent, SWT.BORDER);
		textSearch.setMessage(CBMessages.getString("SearchBar.SearchTipText"));

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		textSearch.setLayoutData(gridData);
		textSearch.setSize(100, 50);

		return textSearch;
	}

	/**
	 * Add a combo box for searching with options
	 * 
	 * @param parent
	 * @return
	 */
	private Combo addSearchOptions(Composite parent) {

		Combo comboOptSearch = new Combo(parent, SWT.READ_ONLY);
		String items[] = { CBMessages.getString("SearchBar.SearchOption1"), CBMessages.getString("SearchBar.SearchOption2"),
				CBMessages.getString("SearchBar.SearchOption3") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		comboOptSearch.setItems(items);
		comboOptSearch.select(0);
		comboOptSearch.pack();
		comboOptSearch.setToolTipText(CBMessages.getString("SearchBar.SearchTip1") //$NON-NLS-1$
				+ CBMessages.getString("SearchBar.SearchTip2") //$NON-NLS-1$
				+ CBMessages.getString("SearchBar.SearchTip3") //$NON-NLS-1$
				+ CBMessages.getString("SearchBar.SearchTip4")); //$NON-NLS-1$

		return comboOptSearch;
	}

	/**
	 * The search method check the query string and search the terms who match the
	 * query string. Method added for reducing and avoiding duplication code in the
	 * system. Return an array list of terms (search results), which is empty if no
	 * results are found
	 */
	private ArrayList<Term> search(String keyword, SearchType type) {

		// output array
		ArrayList<Term> searchResults = new ArrayList<>();

		// if the number of characters of the search are less than the minimum number of
		// characters
		if (!canSearch(keyword.trim().length()))
			return searchResults;

		SearchDAO searchDao = new SearchDAO(catalogue);

		// Set root term for the search
		if (rootTerm != null)
			searchDao.setRootTerm(rootTerm);

		// get the hierarchy in which we have to search
		searchHierarchy = globalSearchEnabled ? catalogue.getMasterHierarchy() : currentHierarchy;

		searchResults = searchDao.startSearch(keyword, type, searchHierarchy);

		// filter deprecated and not in use terms
		searchResults = TermFilter.filterByFlag(hideDeprecated, hideNotInUse, searchResults, searchHierarchy);

		// return the results
		return searchResults;
	}

	/**
	 * Set a root term for the search. All the terms which are not under the sub
	 * tree of the the root term will be excuded from the results.
	 * 
	 * @param rootTerm
	 */
	public void setRootTerm(Term rootTerm) {
		this.rootTerm = rootTerm;
	}

	/**
	 * What to do if the selected hierarchy is changed? Save the selected hierarchy
	 */
	@Override
	public void update(Observable o, Object data) {

		if (o instanceof HierarchySelector) {
			setCurrentHierarchy((Hierarchy) data);
		}

		// if the check boxes for visualizing
		// terms are changed
		if (o instanceof TermFilter) {

			hideDeprecated = ((TermFilter) o).isHidingDeprecated();
			hideNotInUse = ((TermFilter) o).isHidingNotReportable();
		}

		if (o instanceof GlobalManager && data instanceof Catalogue) {

			this.catalogue = (Catalogue) data;
		}
	}
}
