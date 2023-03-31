package ui_describe;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import already_described_terms.DescribedTerm;
import catalogue.Catalogue;
import i18n_messages.CBMessages;
import session_manager.BrowserWindowPreferenceDao;
import utilities.GlobalUtil;
import window_restorer.RestoreableWindow;

/**
 * Template class used to manage the search terms windows and the Recently
 * described one
 * 
 * @author shahaal
 * @author avonva
 *
 */

public class FormDescribedTerms {

	private static final Logger LOGGER = LogManager.getLogger(FormDescribedTerms.class);

	private RestoreableWindow window;
	private static final String WINDOW_CODE = "FormDescribedTerms";

	private Shell shell;
	private Shell parent;
	private String title;

	private TableViewer baseTermsTable; // table which contains all the items
	private Text searchTextBox = null; // search the keyword in the list
	private Button findSearch = null; // button to start the search
	private Button clearSearch = null; // button to clear the search results

	// ten elements in cache
	ArrayList<?> describedTerms = new ArrayList<>();

	ViewerFilter searchViewerFilter = null;

	private Button loadButton;

	private SelectionListener loadListener;

	/**
	 * Constructor
	 * 
	 * @param parentShell, the shell of the form which calls this form
	 * @param title,       the shell title
	 * @param filename     the name of the file from which extracting the described
	 *                     term (recent or favourite)
	 * @param invertOrder  should the order of the term be reversed? (used for
	 *                     visualising recent terms starting from the more recent
	 */
	public FormDescribedTerms(Shell parentShell, String title, Catalogue catalogue, ArrayList<?> describedTerms) {

		shell = parentShell;
		this.title = title;
		this.describedTerms = describedTerms;
	}

	/**
	 * set a search filter. This is used for searching the selected term in the
	 * FoodEx2 main tree panel across all the element of the favourite pick list. In
	 * particular, we search across implicit and explicit facets
	 * 
	 * @param filter
	 */
	public void setSearchFilter(ViewerFilter filter) {

		searchViewerFilter = filter;
	}

	/**
	 * Remove the filter from the base terms table
	 */
	public void removeSearchFilter() {

		if (searchViewerFilter == null)
			return;

		// remove the filter
		baseTermsTable.removeFilter(searchViewerFilter);

		searchViewerFilter = null;

		// enable search panel
		if (searchTextBox != null)
			searchTextBox.setEnabled(true);

		if (findSearch != null)
			findSearch.setEnabled(true);

		if (clearSearch != null)
			clearSearch.setEnabled(true);
	}

	/**
	 * Method called when the form is created
	 */
	public void display(Catalogue catalogue) {

		// Set the layout of the form
		parent = new Shell(shell, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);

		window = new RestoreableWindow(parent, WINDOW_CODE);

		// window icon (on the top left)
		try {
			parent.setImage(
					new Image(parent.getDisplay(), FormDescribedTerms.class.getClassLoader().getResourceAsStream("Choose.gif")));
		} catch (Exception e) {
			LOGGER.error("Cannot get image", e);
			e.printStackTrace();
		}

		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.setLayout(new GridLayout(2, true));
		parent.setText(title);

		// search composite
		Group searchComposite = new Group(parent, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.horizontalSpan = 2;
		searchComposite.setLayoutData(data);
		searchComposite.setLayout(new GridLayout(3, false));
		searchComposite.setText(CBMessages.getString("FormRecentlyDescribe.SearchTermTitle"));

		searchTextBox = new Text(searchComposite, SWT.SEARCH);
		searchTextBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		searchTextBox.setMessage(CBMessages.getString("SearchBar.SearchTipText"));
		searchTextBox.setEditable(true);

		data = new GridData(SWT.FILL, SWT.FILL, false, true);
		data.minimumWidth = 150;
		data.widthHint = 150;

		findSearch = new Button(searchComposite, SWT.NONE);
		findSearch.setLayoutData(data);
		findSearch.setText(CBMessages.getString("FormRecentlyDescribe.FindTermsButton"));

		clearSearch = new Button(searchComposite, SWT.NONE);
		clearSearch.setLayoutData(data);
		clearSearch.setText(CBMessages.getString("FormRecentlyDescribe.ClearButton"));

		// group for the base term tables
		Group groupTable = new Group(parent, SWT.NONE);
		groupTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		groupTable.setLayout(new GridLayout(1, false));
		groupTable.setText(title + ":"); //$NON-NLS-1$

		// initialise the term table
		baseTermsTable = new TableViewer(new Table(groupTable, SWT.BORDER | SWT.VIRTUAL));
		baseTermsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		baseTermsTable.getTable().setLinesVisible(true);

		// set the providers
		baseTermsTable.setLabelProvider(new LabelProviderDescribedTerm(catalogue));
		baseTermsTable.setContentProvider(new ContentProviderDescribedTerms());

		// add the terms to the table viewer
		baseTermsTable.setInput(describedTerms);

		// TODO finish the method for adding multiple selection + copy and paste option
		// into the last searched window
		// baseTermsTable.add

		/* Add the search filter if it was set */
		if (searchViewerFilter != null) {
			// add the filter
			baseTermsTable.addFilter(searchViewerFilter);

			// disable search function in this case
			if (searchTextBox != null)
				searchTextBox.setEnabled(false);

			if (findSearch != null)
				findSearch.setEnabled(false);
		}

		// group for the base term tables
		Group codeComposite = new Group(parent, SWT.NONE);
		codeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		codeComposite.setLayout(new GridLayout(1, false));
		codeComposite.setText(CBMessages.getString("FormRecentlyDescribe.TermInfo"));

		// label for full code text box
		Label fullCodeLabel = new Label(codeComposite, SWT.NONE);
		fullCodeLabel.setText(CBMessages.getString("FormRecentlyDescribe.FullCodeLabel"));

		// text boxes to show the full code
		final Text fullCode = new Text(codeComposite, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		fullCode.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fullCode.setEditable(false);

		// label for interpreted text box
		Label interpLabel = new Label(codeComposite, SWT.NONE);
		interpLabel.setText(CBMessages.getString("FormRecentlyDescribe.InterpretedCodeLabel"));

		// text box for the interpreted code
		final Text interpretedCode = new Text(codeComposite,
				SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		interpretedCode.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		interpretedCode.setEditable(false);

		// composite which contains the buttons
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		GridData dataLayout = new GridData(SWT.CENTER, SWT.FILL, true, false);
		dataLayout.horizontalSpan = 2;
		buttonComposite.setLayoutData(dataLayout);
		buttonComposite.setLayout(new GridLayout(2, true));

		// open the recent element in the describe window
		loadButton = new Button(buttonComposite, SWT.PUSH);
		loadButton.setText(CBMessages.getString("FormRecentlyDescribe.LoadButton"));
		loadButton.setLayoutData(data);

		// cancel the operation
		Button cancelButton = new Button(buttonComposite, SWT.PUSH);
		cancelButton.setText(CBMessages.getString("FormRecentlyDescribe.CancelButton"));
		cancelButton.setLayoutData(data);

		// when an element is selected from the table
		baseTermsTable.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {

				if (!baseTermsTable.getSelection().isEmpty()) {

					IStructuredSelection selection = (IStructuredSelection) baseTermsTable.getSelection();

					// get the selected item
					DescribedTerm describedTerm = (DescribedTerm) selection.getFirstElement();

					fullCode.setText(describedTerm.getCode());

					// if not valid stop
					if (!describedTerm.isValid()) {
						GlobalUtil.showErrorDialog(shell, describedTerm.getCode(),
								CBMessages.getString("FormRecentlyDescribe.InvalidTermMessage"));
						return;
					}

					// create the interpreted code starting from the fullcode
					interpretedCode.setText(describedTerm.getTerm().getInterpretedCode());
				}
			}
		});

		/* SEARCH */

		// remove filter if pressed
		clearSearch.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				searchTextBox.setText("");
				baseTermsTable.addFilter(getSearchFilter(searchTextBox.getText()));
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		// search
		findSearch.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// filter the results with the keyword
				baseTermsTable.addFilter(getSearchFilter(searchTextBox.getText()));
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		// search also with the enter button
		searchTextBox.addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(TraverseEvent e) {

				// filter the results with the keyword
				baseTermsTable.addFilter(getSearchFilter(searchTextBox.getText()));
			}
		});

		// if ok button is pressed
		loadButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				if (loadListener != null)
					loadListener.widgetSelected(e);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		// cancel button is pressed?
		cancelButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				parent.close(); // close the dialog
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		parent.setMaximized(false);
		parent.pack();

		// restore previous dimensions
		window.restore(BrowserWindowPreferenceDao.class);
		window.saveOnClosure(BrowserWindowPreferenceDao.class);

		// show the dialog
		parent.open();

	}

	/**
	 * Retrieve the base term and the related facets from the full code, then open
	 * the describe window and load all the terms
	 * 
	 * @author shahaal
	 * @param describedTerm
	 * @return
	 */
	public DescribedTerm loadTermInDescribe() {

		// get the selected term
		IStructuredSelection selection = (IStructuredSelection) baseTermsTable.getSelection();

		// get the selected item
		DescribedTerm describedTerm = (DescribedTerm) selection.getFirstElement();

		// if not valid stop
		if (!describedTerm.isValid()) {
			GlobalUtil.showErrorDialog(shell, describedTerm.getCode(),
					CBMessages.getString("FormRecentlyDescribe.InvalidTermMessage"));
			return null;
		}

		// hide the current form
		parent.dispose();

		return describedTerm;

	}

	/**
	 * Get the search filter for the selected keywords
	 * 
	 * @param keyword
	 * @return
	 */
	private ViewerFilter getSearchFilter(String keyword) {

		ViewerFilter filter = new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parent, Object element) {

				// search keyword
				String key = searchTextBox.getText().toLowerCase();

				if (key == null || key.isEmpty())
					return true;

				// if an element contains the keyword then return it
				String elementName = ((DescribedTerm) element).getLabel().toLowerCase();

				if (elementName.contains(key))
					return true;

				return false;
			}
		};

		return filter;
	}

	/**
	 * set listener for load term button
	 * 
	 * @param actionListener
	 */
	public void setLoadListener(SelectionListener actionListener) {
		this.loadListener = actionListener;
	}
}
