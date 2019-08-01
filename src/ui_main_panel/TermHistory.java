package ui_main_panel;

import java.util.ArrayDeque;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import catalogue_object.Term;

/**
 * The class provides a list of terms which has been selected in the term tree
 * view panel and allows to shift between them through the previous\next buttons
 * 
 * @author shahaal
 *
 */
public class TermHistory extends Observable implements Observer {

	private Button backBtn;

	private Button nextBtn;

	// parent composite
	private Composite parent;

	private ArrayDeque<Term> history;
	private int size;
	private int currentIndex = 0;
	private Term currentTerm;

	/**
	 * Constructor, create all the graphics under the parent composite and set
	 * length history to 10
	 * 
	 * @param parent
	 */
	public TermHistory(Composite parent) {
		this(parent, 10);
	}

	/**
	 * Constructor with a capacity
	 * 
	 * @param capacity
	 */
	public TermHistory(Composite parent, int capacity) {
		this.parent = parent;
		this.history = new ArrayDeque<Term>(capacity);
		this.size = capacity;
	}

	/**
	 * Display the hierarchy filter
	 */
	public void display() {
		
		// history
		Label label = new Label(parent, SWT.NONE);
		label.setText("History:");
		
		// Create the back button
		backBtn = new Button(parent, SWT.ARROW | SWT.LEFT);
		backBtn.setToolTipText("Previous term");

		backBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				currentTerm = getPrevious();
				
				setChanged();
				notifyObservers(currentTerm);

			}
		});

		// Create the forward button
		nextBtn = new Button(parent, SWT.ARROW | SWT.RIGHT);
		nextBtn.setToolTipText("Next term");

		nextBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				currentTerm = getNext();
				
				setChanged();
				notifyObservers(currentTerm);

			}
		});

		refresh();
	}

	/**
	 * method used for refreshing the UI
	 */
	private void refresh() {

		boolean notEmpty = !history.isEmpty();

		nextBtn.setEnabled((currentIndex != 0 && nextBtn != null) && notEmpty);

		backBtn.setEnabled(currentIndex != history.size() - 1 && backBtn != null && notEmpty);
	}

	/**
	 * Returns the size (length) of the history list
	 * 
	 * @return
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Adds a term to the terms history log
	 * 
	 * @param term
	 */
	public void add(Term term) {

		// if the current term is same as the new one then return
		if (currentTerm!= null && currentTerm.equals(term))
			return;

		// remove last if reached max size
		if (history.size() == size)
			history.pollLast();

		// add the new element at first position
		history.addFirst(term);

		// reset the index
		currentIndex = 0;

		// refresh ui
		refresh();

	}

	/**
	 * Gets the previous term in the history list
	 * 
	 * @return The previous term from the history list
	 */
	public Term getPrevious() {

		// if current index not out of boundary
		if (currentIndex >= 0 && currentIndex < history.size() - 1)
			currentIndex++;

		// return the term at the index
		Object[] list = history.toArray();
		Term t = (Term) list[currentIndex];

		refresh();

		return t;
	}

	/**
	 * Gets the next term in the history list
	 * 
	 * @return The next term from the history list
	 */
	public Term getNext() {

		// if current index not out of boundary
		if (currentIndex > 0 && currentIndex <= history.size())
			currentIndex--;

		// return the term at the index
		Object[] list = history.toArray();
		Term t = (Term) list[currentIndex];

		refresh();

		return t;
	}

	/**
	 * Clears the terms history list
	 */
	public void clear() {

		history.clear();
		currentIndex = 0;
	}

	@Override
	public void update(Observable o, Object arg) {

		// get updates on the selected term
		if (o instanceof TermsTreePanel) {
			// get the selected element in the tree
			Term term = ((TermsTreePanel) o).getFirstSelectedTerm();
			// add the term to the history
			add(term);
		}

	}

}