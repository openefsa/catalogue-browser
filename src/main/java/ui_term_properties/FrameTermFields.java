package ui_term_properties;

import java.util.ArrayList;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Attribute;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import dcf_user.User;
import detail_level.ContentProviderDetailLevel;
import detail_level.DetailLevelGraphics;
import detail_level.LabelProviderDetailLevel;
import i18n_messages.CBMessages;
import naming_convention.SpecialValues;
import term_type.ContentProviderTermType;
import term_type.LabelProviderTermType;
import term_type.TermType;
import ui_general_graphics.ComboTextBox;
import utilities.GlobalUtil;

/**
 * Frame which shows to the user a lot of information related to the selected
 * term
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class FrameTermFields {

	private Catalogue catalogue;

	private ComboTextBox termType;
	private ComboTextBox detailLevel;
	private Text termCode;
	private Text termExtCode;
	private Text termName;
	private Text termExtName;
	private Text termDisplayAs;
	private ScopenotesWithLinks scopenotes;
	private TableImplicitAttributes attributes;

	private Listener updateListener = null; // listener called when a term property is updated and saved

	private Term term;

	/**
	 * group component titles
	 */
	final String gr1 = "group1";
	final String gr2 = "group2";
	final String termTypeTitle = CBMessages.getString("TermProperties.TermTypeTitle");
	final String detailLevelTitle = CBMessages.getString("TermProperties.DetailLevelTitle");
	final String termCodeTitle = CBMessages.getString("TermProperties.TermCodeTitle");
	final String termExtCodeTitle = CBMessages.getString("TermProperties.TermFullCodeTitle");
	final String termNameTitle = CBMessages.getString("TermProperties.TermNameTitle");
	final String termExtNameTitle = CBMessages.getString("TermProperties.TermFullNameTitle");
	final String termDisplayAsTitle = CBMessages.getString("TermProperties.TermDisplayAsTitle");
	final String scopenoteTitle = CBMessages.getString("TermProperties.ScopenotesTitle");
	final String implAttrTitle = CBMessages.getString("TermProperties.ImplicitAttributes");

	private Composite parent;

	/**
	 * set the listener which is called when a term property is updated and saved
	 * 
	 * @param updateListener
	 */
	public void addUpdateListener(Listener updateListener) {
		this.updateListener = updateListener;
	}

	/**
	 * Call the update listener using the modified term as data
	 * 
	 * @param term
	 */
	private void callUpdateListener(Term term) {

		if (updateListener == null)
			return;

		Event e = new Event();
		e.data = term;
		updateListener.handleEvent(e);
	}

	/**
	 * update the current catalogue
	 * 
	 * @author shahaal
	 * @param newCat
	 */
	public void setCatalogue(Catalogue newCat) {

		// redraw combobox widgets only when different combobox
		if (!isSameCatalogue(newCat))
			redraw();

		this.catalogue = newCat;
	}

	/**
	 * return true if the current catalogue is the same type as new one
	 * 
	 * @author shahaal
	 * @param newCat
	 * @return
	 */
	public boolean isSameCatalogue(Catalogue newCat) {
		return (catalogue != null && newCat.sameAs(catalogue));
	}

	/**
	 * Redraw the widgets
	 */
	public void redraw() {

		if (catalogue == null)
			return;

		// note that the order is important since
		// the widget are recreated
		if (termType != null)
			termType.setCatalogue(catalogue);

		if (detailLevel != null)
			detailLevel.setCatalogue(catalogue);

	}

	/**
	 * Set the selected term and update the graphics
	 * 
	 * @author shahaal
	 * @author avonva
	 * @param term
	 */
	@SuppressWarnings("unlikely-arg-type")
	public void setTerm(Term term) {

		if (term == null) {
			reset();
			setEnabled(false);
			return;
		}

		// enable all the elements first
		setEnabled(true);

		this.term = term;

		// update also the catalogue
		setCatalogue(term.getCatalogue());

		if (termType != null) {
			if (catalogue.hasTermTypeAttribute()) {
				termType.setInput(catalogue.getTermTypes());

				// get the index of the current term type
				int index;

				if (term.getTermType() != null) {
					// get the index of the current term type
					index = catalogue.getTermTypes().indexOf(term.getTermType());
				} else
					// get the default term type
					index = catalogue.getTermTypes().indexOf(catalogue.getDefaultTermType());

				// set the selection accordingly
				if (index == -1 && term.getTermType() != null) {
					termType.setText("INVALID CODE: " + term.getTermType().getValue());
				} else if (catalogue.getTermTypes().size() > index && index >= 0) {

					termType.setSelection(new StructuredSelection(catalogue.getTermTypes().get(index)));

					termType.setText(catalogue.getTermTypes().get(index).getLabel());
				}
			} else {
				termType.setEnabled(false);
			}
		}

		// refresh elements according to the contents
		if (detailLevel != null) {
			if (catalogue.hasDetailLevelAttribute()) {

				// get all the detail levels and set them as input
				detailLevel.setInput(catalogue.getDetailLevels());

				int index;

				// if the term has got a detail level set
				if (term.getDetailLevel() != null)
					// get the index of the current level of detail
					index = catalogue.getDetailLevels().indexOf(term.getDetailLevel());
				else
					// otherwise, get the default detail level from the catalogue
					index = catalogue.getDetailLevels().indexOf(catalogue.getDefaultDetailLevel());

				// set the selection accordingly
				detailLevel.setSelection(new StructuredSelection(catalogue.getDetailLevels().get(index)));

				detailLevel.setText(catalogue.getDetailLevels().get(index).getLabel());
			} else {
				detailLevel.setEnabled(false);
			}
		}

		if (termCode != null)
			termCode.setText(term.getCode());

		// full code with base term and facets
		if (isWidgetAvailable(termExtCode))
			termExtCode.setText(term.getFullCode(true, true));

		if (termName != null)
			termName.setText(term.getName());

		// show the full code interpretation
		if (isWidgetAvailable(termExtName))
			termExtName.setText(term.getInterpretedExtendedName());

		if (isWidgetAvailable(termDisplayAs))
			termDisplayAs.setText(term.getShortName(false));

		if (scopenotes != null)
			scopenotes.setTerm(term);

		if (attributes != null)
			attributes.setTerm(term);

		refresh();
	}

	/**
	 * Refresh elements which can be refreshed
	 */
	public void refresh() {

		if (termType != null)
			termType.refresh(true);

		if (detailLevel != null)
			detailLevel.refresh(true);
	}

	/**
	 * Enable/disable the entire panel
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {

		if (termType != null)
			termType.setEnabled(enabled);

		if (detailLevel != null)
			detailLevel.setEnabled(enabled);

		if (termCode != null)
			termCode.setEnabled(enabled);

		if (isWidgetAvailable(termExtCode))
			termExtCode.setEnabled(enabled);

		if (termName != null)
			termName.setEnabled(enabled);

		if (isWidgetAvailable(termExtName))
			termExtName.setEnabled(enabled);

		if (isWidgetAvailable(termDisplayAs))
			termDisplayAs.setEnabled(enabled);

		if (scopenotes != null)
			scopenotes.setEnabled(enabled);

		if (attributes != null)
			attributes.setEnabled(enabled);
	}

	/**
	 * check if the widget that can be disposed is available or not (termExtCode,
	 * termExtName, termDisplayAs)
	 * 
	 * @author shahaal
	 * @param widget
	 * @return
	 */
	private boolean isWidgetAvailable(Text widget) {
		return widget != null && !widget.isDisposed();
	}

	/**
	 * Remove all the input from the graphical elements
	 */
	public void reset() {

		if (termType != null) {
			termType.setInput(null);
			termType.setText("");
		}

		if (detailLevel != null) {
			detailLevel.setInput(null);
			detailLevel.setText("");
		}

		if (termCode != null)
			termCode.setText("");

		if (isWidgetAvailable(termExtCode))
			termExtCode.setText("");

		if (termName != null)
			termName.setText("");

		if (isWidgetAvailable(termExtName))
			termExtName.setText("");

		if (isWidgetAvailable(termDisplayAs))
			termDisplayAs.setText("");

		if (scopenotes != null)
			scopenotes.setTerm(null);

		if (attributes != null)
			attributes.setTerm(null);
	}

	/**
	 * Make editable extended name, short name and scopenotes
	 */
	public void setEditable(boolean editable) {

		if (termName != null)
			termName.setEditable(editable);

		if (isWidgetAvailable(termDisplayAs))
			termDisplayAs.setEditable(editable);

		if (scopenotes != null)
			scopenotes.setEditable(editable);
	}

	/**
	 * Constructor, create a frame with the most important term characteristics We
	 * can select which properties to visualise using the properties array list.
	 * Possible values for properties are: type, detail, code, extname, shortname,
	 * scope notes, attributes
	 * 
	 * @param parent
	 */
	public FrameTermFields(Composite parent) {
		this.parent = parent;
	}

	/**
	 * Add the state flag box into the parent composite
	 * 
	 * @param parent
	 * @return
	 */
	private ComboTextBox addTermType(Composite parent) {

		// create the component group
		Group group = createGroup(parent, 1, termTypeTitle, false);

		ComboTextBox termType = new ComboTextBox(group, gr1);

		termType.setLabelProvider(new LabelProviderTermType());
		termType.setContentProvider(new ContentProviderTermType());

		termType.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {

				// get the object selection
				Object sel = event.getSelection();

				// if the selection is empty return
				if (sel == null || !(sel instanceof IStructuredSelection))
					return;

				IStructuredSelection selection = (IStructuredSelection) sel;

				// get the selected term type
				TermType tt = (TermType) selection.getFirstElement();

				String oldValue;

				// save the old term type value
				if (term.getTermType() != null)
					oldValue = term.getTermType().getValue();
				else
					oldValue = term.getCatalogue().getDefaultTermType().getCode();

				// if the same value was selected return
				if (oldValue.equals(tt.getCode()))
					return;

				// if the term type of the term is not set yet,
				// and we have set a different term type, we need
				// to add the term type also to the attributes
				if (term.getTermType() == null) {

					AttributeDAO attrDao = new AttributeDAO(term.getCatalogue());
					Attribute attribute = attrDao.getByName(SpecialValues.TERM_TYPE_NAME);
					TermAttribute termType = new TermAttribute(term, attribute, tt.getCode());

					term.setTermType(termType);
				} else {

					// update the term type value
					term.setTermTypeValue(tt.getCode());
				}

				// initialise term attribute dao
				TermAttributeDAO taDao = new TermAttributeDAO(term.getCatalogue());

				// update the term attributes
				taDao.updateByA1(term);

				// call the update listener to update the term in the tree
				callUpdateListener(term);
			}
		});

		return termType;
	}

	/**
	 * Add the corex flag into the parent composite
	 * 
	 * @param parent
	 * @return
	 */
	private ComboTextBox addDetailLevel(Composite parent) {

		// create the component group
		Group group = createGroup(parent, 1, detailLevelTitle, false);

		ComboTextBox detailLevel = new ComboTextBox(group, gr2);

		detailLevel.setLabelProvider(new LabelProviderDetailLevel());
		detailLevel.setContentProvider(new ContentProviderDetailLevel());

		detailLevel.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {

				// if the selection is empty clear the label
				if (event.getSelection().isEmpty() || !(event.getSelection() instanceof IStructuredSelection))
					return;

				IStructuredSelection selection = (IStructuredSelection) event.getSelection();

				DetailLevelGraphics dlg = (DetailLevelGraphics) selection.getFirstElement();

				String oldValue;

				if (term.getDetailLevel() != null)
					oldValue = term.getDetailLevel().getValue();
				else
					oldValue = term.getCatalogue().getDefaultDetailLevel().getCode();

				// if the same value was selected return
				if (oldValue.equals(dlg.getCode()))
					return;

				// if the detail level of the term is not set yet,
				// and we have set a different detail level, we need
				// to add the detail level also to the attributes
				if (term.getDetailLevel() == null) {

					AttributeDAO attrDao = new AttributeDAO(term.getCatalogue());
					Attribute attribute = attrDao.getByName(SpecialValues.DETAIL_LEVEL_NAME);
					TermAttribute detailLevel = new TermAttribute(term, attribute, dlg.getCode());

					term.setDetailLevel(detailLevel);
				} else {

					// update the term type value
					term.setDetailLevelValue(dlg.getCode());
				}

				// initialise term attribute dao
				TermAttributeDAO taDao = new TermAttributeDAO(term.getCatalogue());

				// update the value of the detail level
				taDao.updateByA1(term);

				// call the update listener to update the term in the tree
				callUpdateListener(term);
			}
		});

		return detailLevel;
	}

	/**
	 * Add the term code text box into the parent composite
	 * 
	 * @author shahaal
	 * @param parent
	 * @return
	 */
	private Text addTermCode(Composite parent, String label) {

		// create the component group
		Group group = createGroup(parent, 2, label, false);

		// create the text box
		final Text textTermCode = new Text(group, SWT.BORDER | SWT.READ_ONLY);
		textTermCode.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// add listener when pressed ctrl+a
		textTermCode.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
					textTermCode.selectAll();
					e.doit = false;
				}
			}
		});

		return textTermCode;
	}

	/**
	 * Add the term name text box into the parent composite
	 * 
	 * @author shahaal
	 * @param parent
	 * @return
	 */
	private Text addTermName(Composite parent) {

		// create the component group
		Group group = createGroup(parent, 2, termNameTitle, false);

		final Text textTermName = new Text(group, SWT.MULTI | SWT.BORDER | SWT.NONE);
		textTermName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Enable the focus select all function
		textTermName.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				if (term == null || !User.getInstance().canEdit(term.getCatalogue()))
					return;

				if ((textTermName.getText() == null) || (textTermName.getText().isEmpty())) {

					// if the text is null then I have to raise an error,
					// the old text has to be resumed
					GlobalUtil.showErrorDialog(parent.getShell(),
							CBMessages.getString("TermProperties.InputErrorTitle"),
							CBMessages.getString("TermProperties.InputErrorMessage"));

					// restore previous value
					textTermName.setText(term.getName());
					return;
				}
				
				// return if the name does not change at all
				if (textTermName.getText().equals(term.getName()))
					return;

				// get the current catalogue
				Catalogue currentCat = term.getCatalogue();

				TermDAO termDao = new TermDAO(currentCat);

				// if new name already exists in db it cannot be used
				if (!termDao.isTermNameUnique(term.getCode(), textTermName.getText(), true)) {
					// show error dialog
					GlobalUtil.showErrorDialog(parent.getShell(),
							CBMessages.getString("TermProperties.InputErrorTitle"),
							CBMessages.getString("TermProperties.InputErrorMessage2"));
					// set the original term name
					textTermName.setText(term.getName());
					return;

				}

				// set the new name
				term.setName(textTermName.getText());
				term.setLabel(textTermName.getText());
				
				// update the term in the DB
				termDao.update(term);

				// initialize term attribute dao
				TermAttributeDAO taDao = new TermAttributeDAO(currentCat);

				// update the term attributes
				taDao.updateByA1(term);

				// call the listener
				callUpdateListener(term);

				// update the fields
				setTerm(term);

			}
		});

		// Enable the ctrl-a key combination
		textTermName.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
					textTermName.selectAll();
					e.doit = false;
				}
			}
		});

		return textTermName;
	}

	/**
	 * Add the extended term interpretation field into the parent composite
	 * 
	 * @author shahaal
	 * @param parent
	 * @return
	 */
	private Text addTermExtendedName(final Composite parent) {

		// create the component group
		Group group = createGroup(parent, 2, termExtNameTitle, false);

		// create the text box
		final Text termExtNameText = new Text(group, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
		termExtNameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Enable the ctrl-a key combination
		termExtNameText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
					termExtNameText.selectAll();
					e.doit = false;
				}
			}
		});

		return termExtNameText;
	}

	/**
	 * Add the display as field into the parent composite
	 * 
	 * @author shahaal
	 * @param parent
	 * @return
	 */
	private Text addTermDisplayAs(final Composite parent) {

		// create the component group
		Group group = createGroup(parent, 2, termDisplayAsTitle, false);

		// create the text box
		final Text termDisplayAsText = new Text(group, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
		termDisplayAsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		termDisplayAsText.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {

				if (term == null || !User.getInstance().canEdit(term.getCatalogue()))
					return;

				// return if the name does not change at all
				if (termDisplayAsText.getText().equals(term.getShortName(false)))
					return;

				// if the text already exists in the database it cannot
				// be used
				// get the current catalogue
				TermDAO termDao = new TermDAO(term.getCatalogue());

				// set the new name
				term.setDisplayAs(termDisplayAsText.getText());

				// update the term in the DB
				termDao.update(term);

				// call the listener
				callUpdateListener(term);
			}
		});

		return termDisplayAsText;
	}

	/**
	 * Add the term scopenotes to the parent composite (also with links)
	 * 
	 * @param parent
	 * @return
	 */
	private ScopenotesWithLinks addTermScopenotes(final Composite parent) {

		// create the component group
		Group group = createGroup(parent, 2, scopenoteTitle, true);

		// create scopenotes and links
		final ScopenotesWithLinks scopenotesLink = new ScopenotesWithLinks(group);

		// add scopenotes into the UI
		final Text textScopenotes = scopenotesLink.getTextScopenotes();
		textScopenotes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// When the focus on the textScopenotes is lost this function is called
		textScopenotes.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {

				// Remove the selection from the textScopenotes if focus is lost
				textScopenotes.setSelection(0, 0);

				User user = User.getInstance();

				// return if no term or read only mode
				if (term == null || !user.canEdit(term.getCatalogue()))
					return;

				// return if no changes were made
				if (term.getScopenotes().equals(textScopenotes.getText()))
					return;

				// set the notes of the term
				term.setScopenotes(textScopenotes.getText());

				// update scopenotes
				scopenotesLink.setTerm(term);

				TermDAO termDao = new TermDAO(term.getCatalogue());

				// update the term in the DB
				termDao.update(term);

				// call the listener
				callUpdateListener(term);
			}
		});

		return scopenotesLink;
	}

	/**
	 * Add the term attribute table to the parent composite
	 * 
	 * @param parent
	 * @return
	 */
	private TableImplicitAttributes addTermAttributes(final Composite parent) {
		// create the component group
		Group group = createGroup(parent, 2, implAttrTitle, true);

		final TableImplicitAttributes termAttrTable = new TableImplicitAttributes(group);
		return termAttrTable;
	}

	/**
	 * create group ui component
	 * 
	 * @author shahaal
	 * @param parent
	 * @param span
	 * @param title
	 * @return
	 */
	private Group createGroup(final Composite parent, int span, String title, boolean extVertically) {
		// create the group with style parameters
		Group group = new Group(parent, SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, extVertically);
		gridData.horizontalSpan = span;
		group.setLayoutData(gridData);
		group.setLayout(new GridLayout(1, false));
		// set group title
		group.setText(title);

		return group;
	}

	public void buildWidgets(ArrayList<String> properties) {
		
		boolean includeAll = properties.isEmpty();

		// add the term type
		if (includeAll || properties.contains("type"))
			termType = addTermType(parent);

		// add the detail level
		if (includeAll || properties.contains("detail"))
			detailLevel = addDetailLevel(parent);

		// add term code
		if (includeAll || properties.contains("code"))
			termCode = addTermCode(parent, termCodeTitle);

		// add term code with implicit facets
		if (includeAll || properties.contains("extcode"))
			termExtCode = addTermCode(parent, termExtCodeTitle);

		// add term name
		if (includeAll || properties.contains("name"))
			termName = addTermName(parent);

		// add term extended name
		if (includeAll || properties.contains("extname"))
			termExtName = addTermExtendedName(parent);

		// add term display as (old short name)
		if (includeAll || properties.contains("shortname"))
			termDisplayAs = addTermDisplayAs(parent);

		// add term scope notes and links
		if (includeAll || properties.contains("scopenotes"))
			scopenotes = addTermScopenotes(parent);

		// add term attributes table
		if (includeAll || properties.contains("attributes"))
			attributes = addTermAttributes(parent);

		parent.layout(true, true);
	}

	/**
	 * rebuild the properties to be added to the ui based on the catalogue type
	 * 
	 * @author shahaal
	 * @return
	 */
	public ArrayList<String> rebuildProperties() {
		// add only specific attributes based on current catalogue
		ArrayList<String> properties = new ArrayList<>();
		// add default widgets
		properties.add("type");
		properties.add("detail");
		properties.add("code");
		properties.add("name");
		properties.add("scopenotes");
		properties.add("attributes");

		// if no catalogue is opened return default properties
		if (catalogue == null)
			return properties;

		// if mtx add ext name and ext code
		if (catalogue.isMTXCatalogue()) {
			properties.add("extcode");
			properties.add("extname");
		} else {
			// otherwise add only display as
			properties.add("shortname");
		}

		return properties;
	}

	/**
	 * dispose all children of given composite
	 * 
	 * @author shahaal
	 * @param comp
	 */
	public void disposePropChildren() {
		for (Control control : parent.getChildren())
			control.dispose();
	}
}
