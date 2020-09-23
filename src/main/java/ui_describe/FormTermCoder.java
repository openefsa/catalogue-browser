package ui_describe;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import already_described_terms.DescribedTerm;
import already_described_terms.RecentTermDAO;
import business_rules.ContentProviderWarning;
import business_rules.WarningUtil;
import catalogue.Catalogue;
import catalogue_object.Applicability;
import catalogue_object.Term;
import i18n_messages.CBMessages;
import session_manager.BrowserWindowPreferenceDao;
import term_clipboard.TermClipboard;
import ui_implicit_facet.FacetType;
import ui_implicit_facet.FrameTermImplicitFacets;
import user_preferences.CataloguePreference;
import user_preferences.CataloguePreferenceDAO;
import window_restorer.RestoreableWindow;

/**
 * This class creates a Describing pop-up for Term selected. In this pop-up we
 * will add facets to the term.
 * 
 * @author avonva
 * @author shahaal
 * 
 */
public class FormTermCoder {

	private static final String WINDOW_CODE = "FormTermCoder";

	private RestoreableWindow window;

	// the catalogue we want to use
	private Catalogue catalogue;

	private Shell shell;
	private Shell dialog;
	private String title;
	private Text fullCode;
	private Text textinterp;

	// initial base term
	private Term _baseTerm;
	// term composed
	private Term _tempTerm;

	private boolean copyImplicit = false; // should the implicit facets be shown?
	private boolean enableBR = true; // should the business rules be enabled?

	private ArrayList<String> warnings = new ArrayList<>();

	private TableViewer warningsTable;
	private Canvas semaphore;
	private WarningUtil warnUtils;

	// public FacetFilter facetFilter = new FacetFilter();

	// name of the term selected from the favourite form
	private DescribedTerm describedTerm;

	private FrameTermImplicitFacets implicitFacets;

	// constructor
	public FormTermCoder(Shell shell, String title, Catalogue catalogue) {

		this.shell = shell;
		this.title = title;

		this.catalogue = catalogue;

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

		// get the copy implicit facet preference
		copyImplicit = prefDao.getPreferenceBoolValue(CataloguePreference.copyImplicitFacets, false);

		// get the enable business rules preference if we have the MTX catalogue
		// this boolean is false if checks are disabled or we are not using the MTX
		enableBR = catalogue.isMTXCatalogue()
				|| prefDao.getPreferenceBoolValue(CataloguePreference.enableBusinessRules, false);
	}

	/**
	 * Load an already described term. In particular we can set the base term and
	 * its extended version (the described one) starting from the described term
	 * 
	 * @param describedTerm
	 */
	public void loadDescribedTerm(DescribedTerm describedTerm) {

		this.describedTerm = describedTerm;

		_baseTerm = describedTerm.getBaseTerm();
		_tempTerm = describedTerm.getTerm();

		// to be working the tempTerm is added as child of baseTerm only in memory
		Applicability app = new Applicability(_tempTerm, _baseTerm, catalogue.getMasterHierarchy(), 1, true);
		_tempTerm.addApplicability(app, false);

	}

	/**
	 * Set the base term for this class.
	 * 
	 * @param baseTerm
	 */
	public void setBaseTerm(Term baseTerm) {

		// set the global base term
		_baseTerm = baseTerm;

		_tempTerm = new Term(catalogue);
		_tempTerm.setCode(_baseTerm.getCode());
		_tempTerm.setName(_baseTerm.getName());
		_tempTerm.setDisplayAs(_baseTerm.getShortName(true));

		// to be working the tempTerm is added as child of baseTerm only in memory
		Applicability app = new Applicability(_tempTerm, _baseTerm, catalogue.getMasterHierarchy(), 1, true);
		_tempTerm.addApplicability(app, false);
	}

	// Show the window and display all the graphical elements
	public void display(final Catalogue catalogue) {

		// _dialog = new Shell( _shell , SWT.SHELL_TRIM | SWT.APPLICATION_MODAL ); // if
		// not allow surf main page
		dialog = new Shell(shell, SWT.SHELL_TRIM | SWT.MODELESS);

		dialog.setImage(new Image(dialog.getDisplay(), FormTermCoder.class.getClassLoader().getResourceAsStream("Choose.gif")));
		dialog.setMaximized(true);

		dialog.setText(title);
		dialog.setLayout(new GridLayout(1, false));

		window = new RestoreableWindow(dialog, WINDOW_CODE);

		// if the dialog is closed => save the described term in the recently used terms
		// file
		dialog.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {

				// if there are not facet in the describe => return, nothing to be saved
				if (fullCode.getText().split("#").length <= 1)
					return;

				// avoid to save already saved elements
				if (describedTerm != null && describedTerm.getCode().equals(fullCode.getText()))
					return;

				RecentTermDAO recentDao = new RecentTermDAO(catalogue);

				// insert a new Recent term created with the full code of the selected term and
				// with the
				recentDao.insert(new DescribedTerm(catalogue, fullCode.getText(), _tempTerm.getInterpretedCode()));
			}
		});

		// sash form to resize panels
		SashForm sashForm = new SashForm(dialog, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sashForm.setLayout(new GridLayout(1, false));

		// the composite which contains the facet groups
		Group leftSide = new Group(sashForm, SWT.NONE);
		leftSide.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		leftSide.setLayout(new GridLayout(1, true));
		leftSide.setText("Facet groups");

		// implicit facets tree viewer, the new facets will be considered as explicit
		implicitFacets = new FrameTermImplicitFacets(leftSide, FacetType.EXPLICIT, catalogue);

		implicitFacets.setHierarchy(catalogue.getMasterHierarchy());

		implicitFacets.setTerm(_tempTerm);

		implicitFacets.addMenu(true); // add the contextual menu

		implicitFacets.addUpdateListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				updateTextFields();
			}
		});

		implicitFacets.addRemoveDescriptorListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				updateTextFields();
			}
		});

		// the composite which contains the described term information
		Group rightSide = new Group(sashForm, SWT.NONE);
		rightSide.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		rightSide.setLayout(new GridLayout(1, true));
		rightSide.setText("Description information");

		// full code
		Label l = new Label(rightSide, SWT.NONE);
		l.setText(CBMessages.getString("FormTermCoder.FullCodeLabel"));

		fullCode = new Text(rightSide, SWT.BORDER | SWT.READ_ONLY);
		fullCode.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// when the full code is modified, then check if some warnings
		// have to be raised
		fullCode.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {

				// current code without implicit for warning utilities
				String currentCode = _tempTerm.getFullCode(false, true);

				// raise warnings if necessary
				// if the business rules are enabled
				if (enableBR) {
					warnUtils.refreshWarningsTable(currentCode);
				}
			}
		});

		/*
		 * 
		 * int operations = DND.DROP_MOVE | DND.DROP_COPY; Transfer[] types = new
		 * Transfer[] { TextTransfer.getInstance() };
		 * 
		 * DragSource srcFull = new DragSource(l, operations);
		 * srcFull.setTransfer(types);
		 * 
		 * // if the text is dragged => copy it srcFull.addDragListener(new
		 * DragSourceListener() {
		 * 
		 * public void dragStart(DragSourceEvent event) { }
		 * 
		 * public void dragSetData(DragSourceEvent event) { if
		 * (TextTransfer.getInstance().isSupportedType(event.dataType)) { event.data =
		 * fullCode.getText(); } }
		 * 
		 * public void dragFinished(DragSourceEvent event) { } });
		 * 
		 * // Drag and Drop functionality DragSource source = new DragSource(fullCode,
		 * operations);
		 * 
		 * source.setTransfer(types); source.addDragListener(new DragSourceListener() {
		 * 
		 * public void dragStart(DragSourceEvent event) { }
		 * 
		 * public void dragSetData(DragSourceEvent event) { if
		 * (TextTransfer.getInstance().isSupportedType(event.dataType)) { event.data =
		 * fullCode.getSelectionText(); } }
		 * 
		 * public void dragFinished(DragSourceEvent event) { } });
		 *
		 */

		// layout for interpreted and canvas
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.heightHint = 100;
		gridData.minimumHeight = 100;

		// interpreted code
		Label interp = new Label(rightSide, SWT.NONE);
		interp.setText(CBMessages.getString("FormTermCoder.InterpretedCodeLabel"));

		// interpreted description
		textinterp = new Text(rightSide, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		textinterp.setLayoutData(gridData);

		// Semaphore label
		Label semaphoreLabel = new Label(rightSide, SWT.NONE);
		semaphoreLabel.setText(CBMessages.getString("FormTermCoder.OverallWarningLevelLabel"));

		// create a rectangle
		semaphore = new Canvas(rightSide, SWT.NONE);
		semaphore.setLayoutData(gridData);

		// Warning log
		Label warningLabel = new Label(rightSide, SWT.NONE);
		warningLabel.setText(CBMessages.getString("FormTermCoder.MessageLogLabel")); //$NON-NLS-1$

		// Table with warnings:
		warningsTable = new TableViewer(rightSide, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.NONE);
		warningsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// set content provider and input
		warningsTable.setContentProvider(new ContentProviderWarning());
		warningsTable.setInput(warnings);

		warnUtils = new WarningUtil(warningsTable, semaphore);
		
		// composite for last buttons
		Composite buttonsComposite = new Composite(dialog, SWT.NONE);
		buttonsComposite.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		buttonsComposite.setLayout(new GridLayout(3, true));

		// layout for the buttons
		gridData = new GridData();
		gridData.minimumWidth = 200;
		gridData.widthHint = 200;

		Button copy = new Button(buttonsComposite, SWT.PUSH);
		copy.setLayoutData(gridData);
		copy.setText(CBMessages.getString("FormTermCoder.CopyCodeButton"));

		Button copyDesc = new Button(buttonsComposite, SWT.PUSH);
		copyDesc.setLayoutData(gridData);
		copyDesc.setText(CBMessages.getString("FormTermCoder.CopyDescriptionButton"));

		Button copyCodeDesc = new Button(buttonsComposite, SWT.PUSH);
		copyCodeDesc.setLayoutData(gridData);
		copyCodeDesc.setText(CBMessages.getString("FormTermCoder.CopyCodeDescrButton"));

		// copy button is selected:
		copy.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				TermClipboard clip = new TermClipboard();

				ArrayList<Term> sources = new ArrayList<>();
				sources.add(_tempTerm);

				clip.copyFullCode(sources, copyImplicit);
			}
		});

		// copy description button is selected:
		copyDesc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				TermClipboard clip = new TermClipboard();

				ArrayList<Term> sources = new ArrayList<>();
				sources.add(_tempTerm);

				clip.copyDescription(sources, copyImplicit);

			}
		});

		// copy code and description button is selected:
		copyCodeDesc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				TermClipboard clip = new TermClipboard();

				ArrayList<Term> sources = new ArrayList<>();
				sources.add(_tempTerm);

				clip.copyFullCodeAndDescription(sources, copyImplicit);
			}
		});

		// default action is to copy the code
		setDefaultButton(copy);

		sashForm.setWeights(new int[] { 1, 2 });

		dialog.setMaximized(false);
		dialog.pack();

		// restore previous dimensions of the window
		window.restore(BrowserWindowPreferenceDao.class);
		window.saveOnClosure(BrowserWindowPreferenceDao.class);

		// show the window
		dialog.open();

		/*
		 * clipboard = new Clipboard(Display.getCurrent());
		 * dialog.addListener(SWT.KeyDown, new Listener() {
		 * 
		 * public void handleEvent(Event e) { if (((e.stateMask & SWT.CTRL) == SWT.CTRL)
		 * && (e.keyCode == 'c')) {
		 * 
		 * String content = fullCode.getText(); Point selection =
		 * fullCode.getSelection(); String data = content.substring(selection.x,
		 * selection.y);
		 * 
		 * if (data.equals("")) { //$NON-NLS-1$ data = content; }
		 * clipboard.setContents(new Object[] { data }, new Transfer[] {
		 * TextTransfer.getInstance() }); } } });
		 */

		// set the initial text of the text boxes
		updateTextFields();

		while (!dialog.isDisposed()) {
			if (!dialog.getDisplay().readAndDispatch())
				dialog.getDisplay().sleep();
		}

		dialog.dispose();
	}

	/**
	 * return if is possible to open the shell
	 * 
	 * @return
	 */
	public boolean canOpen() {
		return dialog == null || dialog.isDisposed();
	}

	/**
	 * Update all the text fields of the describe
	 */
	private void updateTextFields() {

		String newCode = _tempTerm.getFullCode(copyImplicit, true);
		
		if (!dialog.isDisposed()) {
			fullCode.setText(newCode);
			textinterp.setText(_tempTerm.getInterpretedCode(copyImplicit));
		}
	}

	/**
	 * sets default Button on the GUI.
	 * 
	 * @param buttonPush button SWT.Push
	 */
	private void setDefaultButton(Button buttonPush) {
		dialog.setDefaultButton(buttonPush);
	}

	/**
	 * update the catalogue
	 * 
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}
}
