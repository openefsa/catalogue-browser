package ui_main_panel;

import java.util.Observable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import messages.Messages;
import user_preferences.Preference;
import user_preferences.PreferenceNotFoundException;
import user_preferences.UIPreference;
import user_preferences.UIPreferenceDAO;

/**
 * Class used to create the UI for a visualization filter.
 * In particular, we use this UI to create a filter in the
 * main tree of the browser, to hide deprecated terms or
 * not reportable terms.
 * @author avonva
 *
 */
public class TermFilter extends Observable {

	private Composite parent;
	private Button hideDeprecated;
	private Button hideNotInUse;
	
	private String deprCode;
	private String reprCode;
	
	/**
	 * Initialize the term filter
	 * @param parent
	 */
	public TermFilter( Composite parent ) {
		this.parent = parent;
	}
	
	/**
	 * Display the term filter
	 * @param deprCode the code of the preference related to
	 * hide deprecated terms (in order to restore its value each time)
	 * @param reprCode the code of the preference related to 
	 * hide not reportable terms
	 */
	public void display ( String deprCode, String reprCode ) {
		
		this.deprCode = deprCode;
		this.reprCode = reprCode;
		
		// label which says to choose view options
		Label applicabilityTitle = new Label( parent , SWT.NONE );
		applicabilityTitle.setText( Messages.getString("Browser.ViewTerms") );

		// switch between seeing and not seeing deprecated terms
		hideDeprecated = new Button( parent , SWT.CHECK );
		hideDeprecated.setEnabled( false );
		hideDeprecated.setText( Messages.getString("Browser.HideDeprecatedTermsButton") );

		hideDeprecated.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				try {
					saveStatus();
				} catch (PreferenceNotFoundException e1) {
					e1.printStackTrace();
				}
				
				setChanged();
				notifyObservers();
			}
		} );


		// switch between seeing and not seeing reportable terms
		hideNotInUse = new Button( parent , SWT.CHECK );
		hideNotInUse.setEnabled( false );
		hideNotInUse.setText( Messages.getString("Browser.HideNonReportableTermsButton") );

		hideNotInUse.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				try {
					saveStatus();
				} catch (PreferenceNotFoundException e1) {
					e1.printStackTrace();
				}
				
				setChanged();
				notifyObservers();
			}
		} );
		
		// set the status of the checkboxes
		// using the last used.
		//restoreStatus();
	}
	
	/**
	 * Restore the status of the two checkboxes
	 * using the last ui preferences which refers
	 * to the last status of the preference.
	 * We use this method only in {@link this#display() }
	 * since we do not know the last status of the
	 * checkboxes
	 */
	public void restoreStatus() {
		
		// load the last ui preferences from
		// the ui table
		
		UIPreferenceDAO prefDao = new UIPreferenceDAO();
		boolean hideDepr = prefDao.getPreferenceBoolValue( 
				deprCode, false );

		boolean hideNotRepr = prefDao.getPreferenceBoolValue( 
				reprCode, false );
		
		hideDeprecated.setSelection( hideDepr );
		hideNotInUse.setSelection( hideNotRepr );
		
		// notify that it is changed
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Save the status of the checkboxes in the db
	 * @throws PreferenceNotFoundException
	 */
	private void saveStatus () throws PreferenceNotFoundException {
		
		saveStatus ( deprCode, 
				hideDeprecated.getSelection() );
		
		saveStatus ( reprCode, 
				hideNotInUse.getSelection() );
	}
	
	/**
	 * Update the status of the preference (identified by the key)
	 * with the new value.
	 * @throws PreferenceNotFoundException if a preference is not found in the db
	 */
	private void saveStatus( String key, boolean value ) throws PreferenceNotFoundException {
		
		// save the checkboxes status
		UIPreferenceDAO prefDao = new UIPreferenceDAO();
		
		Preference pref = prefDao.getPreference( key );
		
		pref.setValue( value );
		prefDao.update( pref );
	}
	
	/**
	 * Enable/disable filters
	 * @param enabled
	 */
	public void setEnabled( boolean enabled ) {
		hideDeprecated.setEnabled( enabled );
		hideNotInUse.setEnabled( enabled );
	}
	
	/**
	 * Get if we are hiding the deprecated terms
	 * @return
	 */
	public boolean isHidingDeprecated() {
		return hideDeprecated.getSelection();
	}
	
	/**
	 * Get if we are hiding not reportable terms
	 * @return
	 */
	public boolean isHidingNotReportable() {
		return hideNotInUse.getSelection();
	}
}
