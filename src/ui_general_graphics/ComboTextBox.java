<<<<<<< HEAD
package ui_general_graphics;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import catalogue.Catalogue;
import dcf_user.User;

/**
 * This class is used to use in the same way a text box and a combo box. In
 * particular, a text box is shown when the user is in ReadOnly mode, while the
 * combo box in editing mode. This is useful since the combo box allows
 * modifying the values while the text box is read only.
 * 
 * @author avonva
 * @author shahaal
 */
public class ComboTextBox {

	private Catalogue catalogue;

	private Composite parent;
	private String title;

	private Text textBox;
	private ComboViewer comboBox;

	private ISelectionChangedListener listener;

	private IContentProvider contentProvider;
	private IBaseLabelProvider labelProvider;

	/**
	 * Enable/disable the element according to the read only boolean
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {

		if (comboBox == null)
			textBox.setEnabled(enabled);
		else
			comboBox.getCombo().setEnabled(enabled);
	}

	/**
	 * Set the text according to the read only boolean
	 * 
	 * @param text
	 */
	public void setText(String text) {

		if (comboBox == null)
			textBox.setText(text);
		else
			comboBox.getCombo().setText(text);
	}

	/**
	 * Add the selection changed listener to the combo box. Only editing mode
	 * 
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {

		// save the listener
		this.listener = listener;

		// set the listener
		if (comboBox != null)
			comboBox.addSelectionChangedListener(listener);
	}

	/**
	 * Set the label provider for the combo box. Only editing mode
	 * 
	 * @param lblProv
	 */
	public void setLabelProvider(IBaseLabelProvider lblProv) {

		labelProvider = lblProv;

		if (comboBox != null)
			comboBox.setLabelProvider(lblProv);
	}

	/**
	 * Set the content provider for the combo box. Only editing mode
	 * 
	 * @param cntProv
	 */
	public void setContentProvider(IContentProvider cntProv) {

		contentProvider = cntProv;

		if (comboBox != null)
			comboBox.setContentProvider(cntProv);
	}

	/**
	 * Add the sorter for the combo box
	 * 
	 * @param sorter
	 */
	public void setSorter(ViewerSorter sorter) {

		if (comboBox != null)
			comboBox.setSorter(sorter);
	}

	/**
	 * Set the input for the combo box. Only editing mode
	 * 
	 * @param input
	 */
	public void setInput(Object input) {

		if (comboBox != null)
			comboBox.setInput(input);
	}

	/**
	 * Set the combo box selection. Used only in editing mode, the text box cannot
	 * use this function
	 * 
	 * @param selection
	 */
	public void setSelection(StructuredSelection selection) {

		if (comboBox != null)
			comboBox.setSelection(selection);
	}

	/**
	 * Refreshes the combo box. it has effect only in editing mode the text box
	 * cannot be refreshed
	 */
	public void refresh(boolean updateLabels) {

		if (comboBox != null)
			comboBox.refresh(updateLabels);
	}

	/**
	 * Istantiate the graphical components
	 * 
	 * @param parent
	 * @param groupText
	 * @param input
	 * @param ReadOnly
	 */
	public ComboTextBox(Composite parent, String title) {

		this.parent = parent;
		this.title = title;
		display();
	}

	/**
	 * Set the catalogue
	 * 
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {

		dispose();
		this.catalogue = catalogue;
		display();

		// notify to refresh the parent ui
		parent.layout(true);
	}

	/**
	 * Dispose the element
	 */
	public void dispose() {

		for (Control ctrl : parent.getChildren()) {
			if (ctrl.getData().equals("group")) {
				ctrl.dispose();
				break;
			}
		}

		comboBox = null;
		textBox = null;
	}

	/**
	 * Display the graphics
	 * 
	 * @param parent
	 */
	private void display() {

		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new FillLayout());

		group.setData("group");
		group.setText(title);

		User user = User.getInstance();

		// if null catalogue or non editable catalogue
		if (catalogue == null || !user.canEdit(catalogue)) {

			// text box if read only
			textBox = new Text(group, SWT.READ_ONLY | SWT.BORDER);
			textBox.setText("HHHHHHHHHHHHHHHHHHHHHHHHHHHH");
			textBox.pack();
			textBox.setText("");
			textBox.setData("obj");
		}

		else {

			// combo box if editing
			comboBox = new ComboViewer(group, SWT.READ_ONLY);
			comboBox.getCombo().setData("obj");

			if (listener != null)
				addSelectionChangedListener(listener);

			if (contentProvider != null)
				comboBox.setContentProvider(contentProvider);

			if (labelProvider != null)
				comboBox.setLabelProvider(labelProvider);
		}
	}
}
=======
package ui_general_graphics;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import catalogue.Catalogue;
import dcf_user.User;

/**
 * This class is used to use in the same way a text box and a combo box. In
 * particular, a text box is shown when the user is in ReadOnly mode, while the
 * combo box in editing mode. This is useful since the combo box allows
 * modifying the values while the text box is read only.
 * 
 * @author avonva
 * @author shahaal
 */
public class ComboTextBox {

	private Catalogue catalogue;

	private Composite parent;
	private String title;

	private Text textBox;
	private ComboViewer comboBox;

	private ISelectionChangedListener listener;

	private IContentProvider contentProvider;
	private IBaseLabelProvider labelProvider;

	/**
	 * Enable/disable the element according to the read only boolean
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {

		if (comboBox == null)
			textBox.setEnabled(enabled);
		else
			comboBox.getCombo().setEnabled(enabled);
	}

	/**
	 * Set the text according to the read only boolean
	 * 
	 * @param text
	 */
	public void setText(String text) {

		if (comboBox == null)
			textBox.setText(text);
		else
			comboBox.getCombo().setText(text);
	}

	/**
	 * Add the selection changed listener to the combo box. Only editing mode
	 * 
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {

		// save the listener
		this.listener = listener;

		// set the listener
		if (comboBox != null)
			comboBox.addSelectionChangedListener(listener);
	}

	/**
	 * Set the label provider for the combo box. Only editing mode
	 * 
	 * @param lblProv
	 */
	public void setLabelProvider(IBaseLabelProvider lblProv) {

		labelProvider = lblProv;

		if (comboBox != null)
			comboBox.setLabelProvider(lblProv);
	}

	/**
	 * Set the content provider for the combo box. Only editing mode
	 * 
	 * @param cntProv
	 */
	public void setContentProvider(IContentProvider cntProv) {

		contentProvider = cntProv;

		if (comboBox != null)
			comboBox.setContentProvider(cntProv);
	}

	/**
	 * Add the sorter for the combo box
	 * 
	 * @param sorter
	 */
	public void setSorter(ViewerSorter sorter) {

		if (comboBox != null)
			comboBox.setSorter(sorter);
	}

	/**
	 * Set the input for the combo box. Only editing mode
	 * 
	 * @param input
	 */
	public void setInput(Object input) {

		if (comboBox != null)
			comboBox.setInput(input);
	}

	/**
	 * Set the combo box selection. Used only in editing mode, the text box cannot
	 * use this function
	 * 
	 * @param selection
	 */
	public void setSelection(StructuredSelection selection) {

		if (comboBox != null)
			comboBox.setSelection(selection);
	}

	/**
	 * Refreshes the combo box. it has effect only in editing mode the text box
	 * cannot be refreshed
	 */
	public void refresh(boolean updateLabels) {

		if (comboBox != null)
			comboBox.refresh(updateLabels);
	}

	/**
	 * Istantiate the graphical components
	 * 
	 * @param parent
	 * @param groupText
	 * @param input
	 * @param ReadOnly
	 */
	public ComboTextBox(Composite parent, String title) {

		this.parent = parent;
		this.title = title;
		display();
	}

	/**
	 * Set the catalogue
	 * 
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {

		dispose();
		this.catalogue = catalogue;
		display();

		// notify to refresh the parent ui
		parent.layout(true);
	}

	/**
	 * Dispose the element
	 */
	public void dispose() {

		for (Control ctrl : parent.getChildren()) {
			if (ctrl.getData().equals("group")) {
				ctrl.dispose();
				break;
			}
		}

		comboBox = null;
		textBox = null;
	}

	/**
	 * Display the graphics
	 * 
	 * @param parent
	 */
	private void display() {

		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new FillLayout());

		group.setData("group");
		group.setText(title);

		User user = User.getInstance();

		// if null catalogue or non editable catalogue
		if (catalogue == null || !user.canEdit(catalogue)) {

			// text box if read only
			textBox = new Text(group, SWT.READ_ONLY | SWT.BORDER);
			textBox.setText("HHHHHHHHHHHHHHHHHHHHHHHHHHHH");
			textBox.pack();
			textBox.setText("");
			textBox.setData("obj");
		}

		else {

			// combo box if editing
			comboBox = new ComboViewer(group, SWT.READ_ONLY);
			comboBox.getCombo().setData("obj");

			if (listener != null)
				addSelectionChangedListener(listener);

			if (contentProvider != null)
				comboBox.setContentProvider(contentProvider);

			if (labelProvider != null)
				comboBox.setLabelProvider(labelProvider);
		}
	}
}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
