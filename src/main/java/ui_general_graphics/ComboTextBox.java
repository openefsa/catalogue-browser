package ui_general_graphics;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import catalogue.Catalogue;
import dcf_user.User;

/**
 * 
 * ComboBox/Text the class has the aim to show one of the item based on the
 * privilege (only viewer or editor).
 * 
 * @author shahaal
 *
 */
public class ComboTextBox {

	// class attributes
	private boolean onlyViewer;
	private String id;
	private Group group;
	private Catalogue catalogue;
	private Text textBox;
	private ComboViewer comboBox;

	// listeners
	private ISelectionChangedListener listener;
	private IContentProvider contentProvider;
	private IBaseLabelProvider labelProvider;

	/**
	 * method used for instantiating the class attributes
	 * 
	 * @param parent
	 * @param title
	 * @param id
	 */
	public ComboTextBox(final Group g, String i) {
		// set class attributes
		group = g;
		id = i;
		// create and display items
		display();
	}

	/**
	 * method used for displaying GUI
	 */
	private void display() {

		// flag if the user is only viewer
		onlyViewer = (catalogue == null || !User.getInstance().canEdit(catalogue));

		if (onlyViewer) {
			// text box if read only
			textBox = new Text(group, SWT.READ_ONLY | SWT.BORDER);
			textBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			textBox.setData(id);
			textBox.setText("");
		} else {
			// combo box if editing
			comboBox = new ComboViewer(group, SWT.READ_ONLY);
			comboBox.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			comboBox.getCombo().setData(id);
			// set liseteners
			if (listener != null)
				addSelectionChangedListener(listener);
			if (contentProvider != null)
				comboBox.setContentProvider(contentProvider);
			if (labelProvider != null)
				comboBox.setLabelProvider(labelProvider);
		}
	}

	/**
	 * Set the catalogue
	 * 
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {
		// set the new catalogue
		this.catalogue = catalogue;
		// remove inner items of group
		dispose();
		// display the new items with updated data
		display();
		// notify to refresh the parent UI
		group.layout(true, true);
	}

	/**
	 * Dispose the element
	 */
	public void dispose() {
		// dispose 1st item
		group.getChildren()[0].dispose();
		// empty combo
		comboBox = null;
		// empty text
		textBox = null;
	}

	/**
	 * Enable/disable the element according to the read only flag
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {

		if (onlyViewer)
			textBox.setEnabled(enabled);
		else
			comboBox.getCombo().setEnabled(enabled);
		//group.layout(true, true);
	}

	/**
	 * Set the text according to the read only flag
	 * 
	 * @param text
	 */
	public void setText(String text) {

		if (onlyViewer)
			textBox.setText(text);
		else
			comboBox.getCombo().setText(text);
	}

	/**
	 * Add the sorter for the combo box - only editing mode
	 * 
	 * @param sorter
	 */
	public void setSorter(ViewerSorter sorter) {

		if (!onlyViewer)
			comboBox.setSorter(sorter);
	}

	/**
	 * Set the input for the combo box - only editing mode
	 * 
	 * @param input
	 */
	public void setInput(Object input) {
		if (!onlyViewer)
			comboBox.setInput(input);
	}

	/**
	 * Set the combo box selection - only editing mode
	 * 
	 * @param selection
	 */
	public void setSelection(StructuredSelection selection) {

		if (!onlyViewer)
			comboBox.setSelection(selection);
	}

	/**
	 * Refreshes the combo box - only editing mode
	 * 
	 */
	public void refresh(boolean updateLabels) {

		if (!onlyViewer)
			comboBox.refresh(updateLabels);
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
		if (!onlyViewer)
			comboBox.addSelectionChangedListener(listener);
	}

	/**
	 * Set the label provider for the combo box - only editing mode
	 * 
	 * @param lblProv
	 */
	public void setLabelProvider(IBaseLabelProvider lblProv) {

		labelProvider = lblProv;

		if (!onlyViewer)
			comboBox.setLabelProvider(lblProv);
	}

	/**
	 * Set the content provider for the combo box - only editing mode
	 * 
	 * @param cntProv
	 */
	public void setContentProvider(IContentProvider cntProv) {

		contentProvider = cntProv;

		if (!onlyViewer)
			comboBox.setContentProvider(cntProv);
	}
}
