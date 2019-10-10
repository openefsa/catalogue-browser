package ui_main_panel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import catalogue_object.Hierarchy;
import catalogue_object.Term;
import i18n_messages.CBMessages;
import user_preferences.GlobalPreferenceDAO;
import user_preferences.Preference;
import user_preferences.PreferenceNotFoundException;

/**
 * Class used to create the UI for a visualization filter. In particular, we use
 * this UI to create a filter in the main tree of the browser, to hide
 * deprecated terms or not reportable terms.
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class TermFilter extends Observable {

	private Composite parent;
	private Button hideDeprecated;
	private Button hideNotInUse;
	private Button hideTermCode;

	private String deprCode;
	private String reprCode;
	private String termCode;

	/**
	 * Initialize the term filter
	 * 
	 * @param parent
	 */
	public TermFilter(Composite parent) {
		this.parent = parent;
	}

	/**
	 * Display the term filter
	 * 
	 * @param deprCode the code of the preference related to hide deprecated terms
	 *                 (in order to restore its value each time)
	 * @param reprCode the code of the preference related to hide not reportable
	 *                 terms
	 */
	public void display(String deprCode, String reprCode, String termCode) {

		this.deprCode = deprCode;
		this.reprCode = reprCode;
		this.termCode = termCode;

		// composite to which add the hide options
		Composite hideComp = new Composite(parent, SWT.NONE);
		RowLayout layout = new RowLayout();
	    layout.center = true;
		hideComp.setLayout(layout);
		
		// Hide
		Label label = new Label(hideComp, SWT.NONE);
		label.setText(CBMessages.getString("TermFilter.Title"));

		// switch between seeing and not seeing deprecated terms
		hideDeprecated = new Button(hideComp, SWT.CHECK);
		hideDeprecated.setEnabled(false);
		hideDeprecated.setText(CBMessages.getString("TermFilter.HideDeprecatedTermsButton"));
		hideDeprecated.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				try {
					saveStatus();
				} catch (PreferenceNotFoundException e1) {
					e1.printStackTrace();
				}

				setChanged();
				notifyObservers();
			}
		});

		// switch between seeing and not seeing reportable terms
		hideNotInUse = new Button(hideComp, SWT.CHECK);
		hideNotInUse.setEnabled(false);
		hideNotInUse.setText(CBMessages.getString("TermFilter.HideNonReportableTermsButton"));
		hideNotInUse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				try {
					saveStatus();
				} catch (PreferenceNotFoundException e1) {
					e1.printStackTrace();
				}

				setChanged();
				notifyObservers();
			}
		});

		hideTermCode = new Button(hideComp, SWT.CHECK);
		hideTermCode.setEnabled(false);
		hideTermCode.setText(CBMessages.getString("TermFilter.HideTermCodesButton"));
		hideTermCode.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				try {
					saveStatus();
				} catch (PreferenceNotFoundException e1) {
					e1.printStackTrace();
				}

				setChanged();
				notifyObservers();
			}
		});
		// set the status of the checkboxes
		// using the last used.
		// restoreStatus();
	}

	/**
	 * Restore the status of the two checkboxes using the last ui preferences which
	 * refers to the last status of the preference. We use this method only in
	 * {@link this#display() } since we do not know the last status of the
	 * checkboxes
	 */
	public void restoreStatus() {

		// load the last ui preferences from
		// the ui table

		GlobalPreferenceDAO prefDao = new GlobalPreferenceDAO();
		boolean hideDepr = prefDao.getPreferenceBoolValue(deprCode, false);

		boolean hideNotRepr = prefDao.getPreferenceBoolValue(reprCode, false);

		boolean hideCode = prefDao.getPreferenceBoolValue(termCode, false);

		hideDeprecated.setSelection(hideDepr);
		hideNotInUse.setSelection(hideNotRepr);
		hideTermCode.setSelection(hideCode);

		// notify that it is changed
		setChanged();
		notifyObservers();
	}

	/**
	 * Save the status of the checkboxes in the db
	 * 
	 * @throws PreferenceNotFoundException
	 */
	private void saveStatus() throws PreferenceNotFoundException {

		saveStatus(deprCode, hideDeprecated.getSelection());

		saveStatus(reprCode, hideNotInUse.getSelection());
	}

	/**
	 * Update the status of the preference (identified by the key) with the new
	 * value.
	 * 
	 * @throws PreferenceNotFoundException if a preference is not found in the db
	 */
	private void saveStatus(String key, boolean value) throws PreferenceNotFoundException {

		// save the checkboxes status
		GlobalPreferenceDAO prefDao = new GlobalPreferenceDAO();

		Preference pref = prefDao.getPreference(key);

		pref.setValue(value);
		prefDao.update(pref);
	}

	/**
	 * Enable/disable filters
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		hideDeprecated.setEnabled(enabled);
		hideNotInUse.setEnabled(enabled);
		hideTermCode.setEnabled(enabled);
	}

	/**
	 * Get if we are hiding the deprecated terms
	 * 
	 * @return
	 */
	public boolean isHidingDeprecated() {
		return hideDeprecated.getSelection();
	}

	/**
	 * Get if we are hiding not reportable terms
	 * 
	 * @return
	 */
	public boolean isHidingNotReportable() {
		return hideNotInUse.getSelection();
	}

	/**
	 * Get if we are hiding terms codes
	 * 
	 * @return
	 */
	public boolean isHidingTermCode() {
		return hideTermCode.getSelection();
	}

	/**
	 * Filter the terms by their deprecated and dismissed flag
	 * 
	 * @param objs
	 * @return
	 */
	public static ArrayList<Term> filterByFlag(boolean hideDepr, boolean hideNotInUse, Collection<Term> objs,
			Hierarchy currentHierarchy) {

		ArrayList<Term> out = new ArrayList<>();

		for (Term obj : objs) {

			if (hideDepr && obj.isDeprecated())
				continue;

			if (hideNotInUse && obj.isDismissed(currentHierarchy))
				continue;

			out.add(obj);
		}

		return out;
	}
}
