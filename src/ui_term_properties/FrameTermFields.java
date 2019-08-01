package ui_term_properties;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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
import messages.Messages;
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
	private Text code;
	private Text fullCode;
	private Text extName;
	private Text fullCodeDesc;
	private ScopenotesWithLinks scopenotes;
	private TableImplicitAttributes attributes;

	private Listener updateListener = null; // listener called when a term property is updated and saved

	private Term term;

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
	 * Set the current catalogue
	 * 
	 * @param catalogue
	 * @param forceRedraw true to redraw in any case the widgets
	 */
	private void setCatalogue(Catalogue newCat) {

		// avoid resetting the same catalogue (avoid
		// recreating widgets!)
		if (catalogue != null && newCat.getCode().equals(catalogue.getCode())
				&& newCat.getVersion().equals(catalogue.getVersion()))
			return;

		this.catalogue = newCat;

		redraw();
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

		if (code != null)
			code.setText(term.getCode());

		// full code with base term and facets
		if (fullCode != null)
			fullCode.setText(term.getFullCode(true, true));

		if (extName != null)
			extName.setText(term.getName());

		// show the full code interpretation
		if (fullCodeDesc != null)
			fullCodeDesc.setText(term.getInterpretedCode(true));

		if (scopenotes != null)
			scopenotes.setTerm(term);

		if (attributes != null)
			attributes.setTerm(term);

		// refresh elements according to the contents

		// get the current catalogue
		Catalogue currentCat = term.getCatalogue();

		if (detailLevel != null) {

			// if the catalogue contains the detail level attribute
			if (currentCat.hasDetailLevelAttribute()) {

				// get all the detail levels and set them as input
				detailLevel.setInput(currentCat.getDetailLevels());

				int index;

				// if the term has got a detail level set
				if (term.getDetailLevel() != null)
					// get the index of the current level of detail
					index = currentCat.getDetailLevels().indexOf(term.getDetailLevel());
				else
					// otherwise, get the default detail level from the catalogue
					index = currentCat.getDetailLevels().indexOf(currentCat.getDefaultDetailLevel());

				// set the selection accordingly
				detailLevel.setSelection(new StructuredSelection(currentCat.getDetailLevels().get(index)));

				detailLevel.setText(currentCat.getDetailLevels().get(index).getLabel());
			} else {
				detailLevel.setEnabled(false);
			}
		}

		if (termType != null) {

			if (currentCat.hasTermTypeAttribute()) {

				termType.setInput(currentCat.getTermTypes());

				// get the index of the current term type
				int index;

				if (term.getTermType() != null) {
					// get the index of the current term type
					index = currentCat.getTermTypes().indexOf(term.getTermType());
				} else
					// get the default term type
					index = currentCat.getTermTypes().indexOf(currentCat.getDefaultTermType());

				// set the selection accordingly
				if (index == -1 && term.getTermType() != null) {
					termType.setText("INVALID CODE: " + term.getTermType().getValue());
				} else if (currentCat.getTermTypes().size() > index && index >= 0) {

					termType.setSelection(new StructuredSelection(currentCat.getTermTypes().get(index)));

					termType.setText(currentCat.getTermTypes().get(index).getLabel());
				}
			}

			else {
				termType.setEnabled(false);
			}
		}

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

		if (code != null)
			code.setEnabled(enabled);

		if (fullCode != null)
			fullCode.setEnabled(enabled);

		if (extName != null)
			extName.setEnabled(enabled);

		if (fullCodeDesc != null)
			fullCodeDesc.setEnabled(enabled);

		if (scopenotes != null)
			scopenotes.setEnabled(enabled);

		if (attributes != null)
			attributes.setEnabled(enabled);
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

		if (code != null)
			code.setText("");

		if (fullCode != null)
			fullCode.setText("");

		if (extName != null)
			extName.setText("");

		if (fullCodeDesc != null)
			fullCodeDesc.setText("");

		if (scopenotes != null)
			scopenotes.setTerm(null);

		if (attributes != null)
			attributes.setTerm(null);
	}

	/**
	 * Make editable extended name, short name and scopenotes
	 */
	public void setEditable(boolean editable) {
		if (extName != null)
			extName.setEditable(editable);

		if (fullCodeDesc != null)
			fullCodeDesc.setEditable(editable);

		if (scopenotes != null)
			scopenotes.setEditable(editable);
	}

	public FrameTermFields(Composite parent) {
		this(parent, new ArrayList<String>());
	}

	/**
	 * Constructor, create a frame with the most important term characteristics We
	 * can select which properties to visualize using the properties array list.
	 * Possible values for properties are: type, detail, code, extname, shortname,
	 * scopenotes, attributes
	 * 
	 * @param parent
	 */
	public FrameTermFields(Composite parent, ArrayList<String> properties) {

		boolean includeAll = properties.isEmpty();

		// composite for first fields
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comp.setLayout(new GridLayout(2, true));

		// add the term type
		if (includeAll || properties.contains("type"))
			termType = addTermType(comp);

		// add the detail level
		if (includeAll || properties.contains("detail"))
			detailLevel = addDetailLevel(comp);
		// add term code
		if (includeAll || properties.contains("code"))
			code = addTermCodeTextBox(parent, Messages.getString("TermProperties.TermCodeTitle"));

		// add term code with implicit facets
		if (includeAll || properties.contains("fullCode"))
			fullCode = addTermCodeTextBox(parent, Messages.getString("TermProperties.TermCompleteCodeTitle"));

		// add term extended name
		if (includeAll || properties.contains("extname"))
			extName = addTermNameTextBox(parent);

		// add term short name
		if (includeAll || properties.contains("fullCodeDesc"))
			fullCodeDesc = addFullCodeDescTextBox(parent);

		// add term scopenotes and links
		if (includeAll || properties.contains("scopenotes"))
			scopenotes = addTermScopenotes(parent);

		// add term attributes table
		if (includeAll || properties.contains("attributes"))
			attributes = addTermAttributes(parent);

	}

	/**
	 * Add the state flag box into the parent composite
	 * 
	 * @param parent
	 * @return
	 */
	private ComboTextBox addTermType(Composite parent) {

		ComboTextBox termType = new ComboTextBox(parent, Messages.getString("TermProperties.TermTypeTitle"));

		termType.setLabelProvider(new LabelProviderTermType());
		termType.setContentProvider(new ContentProviderTermType());

		termType.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {

				// if the selection is empty return
				if (event.getSelection().isEmpty() || !(event.getSelection() instanceof IStructuredSelection))
					return;

				IStructuredSelection selection = (IStructuredSelection) event.getSelection();

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

				// initialize term attribute dao
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

		ComboTextBox detailLevel = new ComboTextBox(parent, Messages.getString("TermProperties.DetailLevelTitle"));

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

				// initialize term attribute dao
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
	private Text addTermCodeTextBox(Composite parent, String label) {

		// create a new group for term code
		Group groupTermCode = new Group(parent, SWT.NONE);
		// set the layout data
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalSpan = 2;
		groupTermCode.setLayoutData(gridData);
		groupTermCode.setLayout(new FillLayout());

		// set its name and layout
		groupTermCode.setText(label);

		// create the text box
		final Text textTermCode = new Text(groupTermCode, SWT.BORDER | SWT.READ_ONLY);

		textTermCode.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				parent.getDisplay().asyncExec(new Runnable() {
					public void run() {
						textTermCode.clearSelection();
					}
				});
			}

			@Override
			public void focusGained(FocusEvent arg0) {
				parent.getDisplay().asyncExec(new Runnable() {
					public void run() {
						textTermCode.selectAll();
					}
				});
			}
		});

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
	private Text addTermNameTextBox(Composite parent) {

		Group groupTermName = new Group(parent, SWT.NONE);

		// set the layout data
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalSpan = 2;
		groupTermName.setLayoutData(gridData);

		groupTermName.setText(Messages.getString("TermProperties.TermNameTitle"));

		groupTermName.setLayout(new FillLayout());

		final Text textTermName = new Text(groupTermName, SWT.MULTI | SWT.BORDER | SWT.NONE);

		textTermName.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {

				User user = User.getInstance();

				if (term == null || !user.canEdit(term.getCatalogue()))
					return;

				if ((textTermName.getText() == null) || (textTermName.getText().isEmpty())) {

					// if the text is null then I have to raise an error,
					// the old text has to be resumed
					GlobalUtil.showErrorDialog(parent.getShell(), Messages.getString("TermProperties.InputErrorTitle"),
							Messages.getString("TermProperties.InputErrorMessage"));

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

				// if the text already exists in the database it cannot
				// be used
				if (!termDao.isTermNameUnique(term.getCode(), textTermName.getText(), true)) {

					GlobalUtil.showErrorDialog(parent.getShell(), Messages.getString("TermProperties.InputErrorTitle"),
							Messages.getString("TermProperties.InputErrorMessage2"));

					textTermName.setText(term.getName());

					return;

				}

				// here the new name is acceptable

				// set the new name
				term.setName(textTermName.getText());

				// update the term in the DB
				termDao.update(term);

				// initialize term attribute dao
				TermAttributeDAO taDao = new TermAttributeDAO(currentCat);

				// update the term attributes
				taDao.updateByA1(term);

				// call the listener
				callUpdateListener(term);
			}
		});

		// shahaal: Enable the focus select all function
		textTermName.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				parent.getDisplay().asyncExec(new Runnable() {
					public void run() {
						textTermName.clearSelection();
					}
				});
			}

			@Override
			public void focusGained(FocusEvent arg0) {
				parent.getDisplay().asyncExec(new Runnable() {
					public void run() {
						textTermName.selectAll();
					}
				});
			}
		});

		// shahaal: Enable the ctrl-a key combination
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
	 * Add the full term code description text box into the parent composite
	 * 
	 * @author shahaal
	 * @param parent
	 * @return
	 */
	private Text addFullCodeDescTextBox(Composite parent) {

		// create a new group for term code
		Group groupShortName = new Group(parent, SWT.NONE);
		// set the layout data
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalSpan = 2;
		groupShortName.setLayoutData(gridData);

		// set its name and layout
		groupShortName.setText(Messages.getString("TermProperties.TermFullCodeDesc"));

		groupShortName.setLayout(new FillLayout());

		// create the text box
		final Text termFullCodeDesc = new Text(groupShortName, SWT.MULTI | SWT.BORDER | SWT.NONE);

		termFullCodeDesc.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {

				User user = User.getInstance();

				if (term == null || !user.canEdit(term.getCatalogue()))
					return;

				// return if the name does not change at all
				if (termFullCodeDesc.getText().equals(term.getInterpretedCode(true)))
					return;

				// if the text already exists in the database it cannot
				// be used
				// get the current catalogue
				TermDAO termDao = new TermDAO(term.getCatalogue());

				// here the new short name is acceptable

				// set the code description (including implicit facets)
				term.setFullCodeDescription(termFullCodeDesc.getText());

				// update the term in the DB
				termDao.update(term);

				// call the listener
				callUpdateListener(term);
			}
		});

		return termFullCodeDesc;
	}

	/**
	 * Add the term scopenotes to the parent composite (also with links)
	 * 
	 * @param parent
	 * @return
	 */
	private ScopenotesWithLinks addTermScopenotes(final Composite parent) {

		Group groupScopeNotes = new Group(parent, SWT.NONE);
		// set the layout data
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.minimumHeight = 150;
		gridData.heightHint = 200;
		gridData.horizontalSpan = 2;
		groupScopeNotes.setLayoutData(gridData);

		groupScopeNotes.setText(Messages.getString("TermProperties.ScopenotesTitle"));
		groupScopeNotes.setLayout(new GridLayout(1, false));

		// create scopenotes and links
		final ScopenotesWithLinks scopenotesLink = new ScopenotesWithLinks(groupScopeNotes);

		// add scopenotes into the UI
		final Text textScopenotes = scopenotesLink.getTextScopenotes();

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
	private TableImplicitAttributes addTermAttributes(Composite parent) {

		final TableImplicitAttributes termAttrTable = new TableImplicitAttributes(parent);
		return termAttrTable;
	}
}
