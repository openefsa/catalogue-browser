package ui_main_panel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * @author AlbyDev
 *
 */
public class NavButtons<T> {

	private static final int MAX_LIST_SIZE = 5;
	private static final int FIRST_INDEX_LIST = 0;
	private List<INavButtonsListener<T>> listeners;

	private List<T> indexes;
	private int currentIndex = 0;
	private Button backBtn, forwardBtn;

	public NavButtons(final Composite composite) {

		this.listeners = new ArrayList<>();
		this.indexes = new ArrayList<>();

		for (int i = 0; i < MAX_LIST_SIZE; ++i) {
			this.indexes.add(null);
		}

		// Create the back button
		backBtn = new Button(composite, SWT.ARROW | SWT.LEFT);

		backBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				currentIndex = Math.abs((currentIndex + 1) % 5);

				refresh();
				
				System.out.println(currentIndex+"- "+indexes.get(currentIndex));

				for (INavButtonsListener<T> l : listeners)
					l.backPressed(indexes.get(currentIndex));
			}
		});

		// Create the forward button
		forwardBtn = new Button(composite, SWT.ARROW | SWT.RIGHT);

		forwardBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				currentIndex = Math.abs((currentIndex - 1) % 5);
				
				refresh();

				System.out.println(currentIndex+"- "+indexes.get(currentIndex));
				
				for (INavButtonsListener<T> l : listeners)
					l.forwardPressed(indexes.get(currentIndex));
			}
		});
	}

	void addNewObject(T selectedTerm) {

		// make a shift to right
		Collections.rotate(indexes, 1);

		// add new term in position zero
		indexes.set(FIRST_INDEX_LIST, selectedTerm);
		
		System.out.println(Arrays.asList(indexes));

	}

	T returnObject() {
		return indexes.get(FIRST_INDEX_LIST);
	}

	private void refresh() {
		backBtn.setEnabled(currentIndex == FIRST_INDEX_LIST);
		forwardBtn.setEnabled(currentIndex == MAX_LIST_SIZE - 1);
	}

	public void addNavButtonsListener(INavButtonsListener<T> listener) {
		this.listeners.add(listener);
	}

	public interface INavButtonsListener<T> {
		void backPressed(T term);

		void forwardPressed(T term);
	}
}