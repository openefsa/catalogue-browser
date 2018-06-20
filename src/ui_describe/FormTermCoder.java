package ui_describe;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
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
import messages.Messages;
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
 * @author
 * 
 */
public class FormTermCoder {

	private static final String WINDOW_CODE = "FormTermCoder";
	// Author: AlbyDev
	public static Boolean instanceExists = false;
	//

	private RestoreableWindow window;

	// the catalogue we want to use
	private Catalogue catalogue;

	private Shell _shell;
	private Shell _dialog;
	private String _title;
	private Text fullCode = null;
	private Text textinterp = null;
	private boolean _copyImplicit = false; // should the implicit facets be shown?
	private boolean _enableBR = true; // should the business rules be enabled?
	private Term _tempTerm = null; // this is the term which is currently created descriptor by descriptor
	private Term _baseTerm = null; // this is the chosen base term

	private ArrayList<String> warnings = new ArrayList<>();

	private TableViewer warningsTable = null; // console of the warnings messages
	private Canvas semaphore = null; // semaphore for the warning messages
	private WarningUtil warnUtils = null; // object which manages the warnings messages

	// public FacetFilter facetFilter = new FacetFilter();

	private Clipboard clipboard;

	private DescribedTerm describedTerm = null; // name of the term selected from the favourite form

	private FrameTermImplicitFacets implicitFacets;

	// start the form
	public FormTermCoder(Shell shell, String title, Catalogue catalogue) {

		_shell = shell;
		_title = title;

		this.catalogue = catalogue;

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

		// get the copy implicit facet preference
		_copyImplicit = prefDao.getPreferenceBoolValue(CataloguePreference.copyImplicitFacets, false);

		// get the enable business rules preference if we have the MTX catalogue
		// this boolean is false if checks are disabled or we are not using the MTX
		_enableBR = catalogue.isMTXCatalogue()
				&& prefDao.getPreferenceBoolValue(CataloguePreference.enableBusinessRules, false);

		// Author: AlbyDev
		// var used to check if an instance of the class already exists
		FormTermCoder.instanceExists = true;
		//
	}

	/**
	 * Load an already described term. In particular we can set the base term and
	 * its extended version (the described one) starting from the described term
	 * 
	 * @param describedTerm
	 */
	void loadDescribedTerm(DescribedTerm describedTerm) {

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
		_tempTerm.setShortName(_baseTerm.getShortName(true));

		// to be working the tempTerm is added as child of baseTerm only in memory
		Applicability app = new Applicability(_tempTerm, _baseTerm, catalogue.getMasterHierarchy(), 1, true);
		_tempTerm.addApplicability(app, false);
	}

	// Show the window and display all the graphical elements
	public void display(final Catalogue catalogue) {

		// _dialog = new Shell( _shell , SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );
		// Author: AlbyDev
		_dialog = new Shell(_shell, SWT.SHELL_TRIM | SWT.MODELESS);
		//

		_dialog.setImage(
				new Image(Display.getCurrent(), this.getClass().getClassLoader().getResourceAsStream("Choose.gif")));
		_dialog.setMaximized(true);

		_dialog.setText(_title);
		_dialog.setLayout(new GridLayout(1, false));

		window = new RestoreableWindow(_dialog, WINDOW_CODE);

		// if the dialog is closed => save the described term in the recently used terms
		// file
		_dialog.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {

				// AlbyDev: reset the instance flag
				FormTermCoder.instanceExists = false;
				//

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
		SashForm sashForm = new SashForm(_dialog, SWT.HORIZONTAL);

		sashForm.setLayout(new GridLayout(1, false));
		GridData gData = new GridData();
		gData.grabExcessHorizontalSpace = true;
		gData.grabExcessVerticalSpace = true;
		gData.verticalAlignment = SWT.FILL;
		gData.horizontalAlignment = SWT.FILL;
		sashForm.setLayoutData(gData);

		// implicit facets tree viewer, we set that new facets will be considered as
		// explicit
		implicitFacets = new FrameTermImplicitFacets(sashForm, FacetType.EXPLICIT, catalogue);

		implicitFacets.setHierarchy(catalogue.getMasterHierarchy());

		implicitFacets.setTerm(_tempTerm);

		implicitFacets.addMenu(); // add the contextual menu

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

		// the composite which contains: full code, interpreter, semaphore, console
		// (warning messages)
		Group rightSide = new Group(sashForm, SWT.NONE);
		rightSide.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		rightSide.setLayout(new GridLayout(1, true));

		/////////////////full code 
		Label l = new Label(rightSide, SWT.NONE);
		l.setText(Messages.getString("FormTermCoder.FullCodeLabel"));

		int operations = DND.DROP_MOVE | DND.DROP_COPY;
		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };

		DragSource srcFull = new DragSource(l, operations);
		srcFull.setTransfer(types);

		// if the text is dragged => copy it
		srcFull.addDragListener(new DragSourceListener() {

			public void dragStart(DragSourceEvent event) {
			}

			public void dragSetData(DragSourceEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					event.data = fullCode.getText();
				}
			}

			public void dragFinished(DragSourceEvent event) {
			}
		});

		fullCode = new Text(rightSide, SWT.BORDER | SWT.READ_ONLY);

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.minimumWidth = 200;
		gridData.minimumHeight=20;
		gridData.widthHint = 310;
		gridData.heightHint=20;
		fullCode.setLayoutData(gridData);

		// when the full code is modified, then check if some warnings
		// have to be raised
		fullCode.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {

				// current code without implicit for warning utilities
				String currentCode = _tempTerm.getFullCode(false, true);

				// raise warnings if necessary
				// if the business rules are enabled
				if (_enableBR)
					warnUtils.refreshWarningsTable(currentCode);
			}
		});

		// Drag and Drop functionality
		DragSource source = new DragSource(fullCode, operations);

		source.setTransfer(types);
		source.addDragListener(new DragSourceListener() {

			public void dragStart(DragSourceEvent event) {
			}

			public void dragSetData(DragSourceEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					event.data = fullCode.getSelectionText();
				}
			}

			public void dragFinished(DragSourceEvent event) {
			}
		});

		/* intepreted code */
		Label interp = new Label(rightSide, SWT.NONE);
		interp.setText(Messages.getString("FormTermCoder.InterpretedCodeLabel")); //$NON-NLS-1$

		// textinterp = new Text( c , SWT.BORDER | SWT.READ_ONLY | SWT.MULTI |
		// SWT.H_SCROLL );
		textinterp = new Text(rightSide,
				SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		// textinterp.setText( _tempTerm.getName() );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.minimumWidth = 200;
		gridData.widthHint = 310;
		gridData.heightHint=100;
		gridData.minimumHeight = 80;
		textinterp.setLayoutData(gridData);

		////////////////// WARNING COMPOSITE: semaphore + warnings log

		// Semaphore
		Label semaphoreLabel = new Label(rightSide, SWT.NONE);
		semaphoreLabel.setText(Messages.getString("FormTermCoder.OverallWarningLevelLabel")); //$NON-NLS-1$
		
		// create a rectangle
		semaphore = new Canvas(rightSide, SWT.NONE);

		// layout of the rectangle
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.minimumWidth = 175;
		gridData.widthHint = 475;
		gridData.heightHint=50;
		gridData.minimumHeight = 50;
		gridData.heightHint = 50;

		semaphore.setLayoutData(gridData);

		// Warning log
		Label warningLabel = new Label(rightSide, SWT.NONE);
		warningLabel.setText(Messages.getString("FormTermCoder.MessageLogLabel")); //$NON-NLS-1$

		// Table with warnings:
		warningsTable = new TableViewer(rightSide,
				SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.NONE);

		// layout of the rectangle
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.minimumWidth = 300;
		gridData.widthHint = 450;
		gridData.heightHint = 450;
		gridData.minimumHeight = 200;

		warningsTable.getTable().setLayoutData(gridData);

		// Set the content based
		warningsTable.setContentProvider(new ContentProviderWarning());

		warningsTable.setInput(warnings);

		warnUtils = new WarningUtil(warningsTable, semaphore);

		// analyze preliminary warnings (for the baseterm)
		// get the complete code without implicit facets for warning purposes
		String currentCode = _tempTerm.getFullCode(false, true);

		// if the business rules are enabled
		if (_enableBR)
			warnUtils.refreshWarningsTable(currentCode);

		/////////////////Three Buttons = COPY + COPY DESCRIPTION + BOTH

		Group buttonsComposite = new Group(_dialog, SWT.NONE);
		buttonsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		buttonsComposite.setLayout(new GridLayout(3, true));

		Button copy = new Button(buttonsComposite, SWT.PUSH);
		copy.setText(Messages.getString("FormTermCoder.CopyCodeButton"));
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		copy.setLayoutData(gridData);

		/* setting up copy description(description of code interpreted) */
		Button copyDesc = new Button(buttonsComposite, SWT.PUSH);
		copyDesc.setText(Messages.getString("FormTermCoder.CopyDescriptionButton"));
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		copyDesc.setLayoutData(gridData);

		/* setting up copy code + description */
		Button copyCodeDesc = new Button(buttonsComposite, SWT.PUSH);
		copyCodeDesc.setText(Messages.getString("FormTermCoder.CopyCodeDescrButton"));
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		copyCodeDesc.setLayoutData(gridData);

		// copy button is selected:
		copy.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				TermClipboard clip = new TermClipboard();

				ArrayList<Term> sources = new ArrayList<>();
				sources.add(_tempTerm);

				clip.copyFullCode(sources, _copyImplicit);
			}
		});

		// copy description button is selected:
		copyDesc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				TermClipboard clip = new TermClipboard();

				ArrayList<Term> sources = new ArrayList<>();
				sources.add(_tempTerm);

				clip.copyDescription(sources, _copyImplicit);

			}
		});

		// copy code and description button is selected:
		copyCodeDesc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				TermClipboard clip = new TermClipboard();

				ArrayList<Term> sources = new ArrayList<>();
				sources.add(_tempTerm);

				clip.copyFullCodeAndDescription(sources, _copyImplicit);
			}
		});

		// default action is to copy the code
		setDefaultButton(copy);

		sashForm.setWeights(new int[] { 1, 2 });

		_dialog.setMaximized(false);
		_dialog.pack();

		// restore previous dimensions of the window
		window.restore(BrowserWindowPreferenceDao.class);
		window.saveOnClosure(BrowserWindowPreferenceDao.class);

		// show the window
		_dialog.open();

		Display d = Display.getCurrent();
		clipboard = new Clipboard(d);
		_dialog.addListener(SWT.KeyDown, new Listener() {

			public void handleEvent(Event e) {
				if (((e.stateMask & SWT.CTRL) == SWT.CTRL) && (e.keyCode == 'c')) {

					String content = fullCode.getText();
					Point selection = fullCode.getSelection();
					String data = content.substring(selection.x, selection.y);

					if (data.equals("")) { //$NON-NLS-1$
						data = content;
					}
					clipboard.setContents(new Object[] { data }, new Transfer[] { TextTransfer.getInstance() });
				}
			}
		});

		// set the initial text of the textboxes
		updateTextFields();

		while (!_dialog.isDisposed()) {
			if (!_dialog.getDisplay().readAndDispatch())
				_dialog.getDisplay().sleep();
		}

		FormTermCoder.instanceExists = false;
		_dialog.dispose();
	}

	/**
	 * Update all the text fields of the describe
	 */
	private void updateTextFields() {

		String newCode = _tempTerm.getFullCode(_copyImplicit, true);

		if (!_dialog.isDisposed()) {
			fullCode.setText(newCode);
			textinterp.setText(_tempTerm.getInterpretedCode(_copyImplicit));
		}
	}

	/**
	 * sets default Button on the GUI.
	 * 
	 * @param buttonPush
	 *            button SWT.Push
	 */
	private void setDefaultButton(Button buttonPush) {
		_dialog.setDefaultButton(buttonPush);
	}
}
