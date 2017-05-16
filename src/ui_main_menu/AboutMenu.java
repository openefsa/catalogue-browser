package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import messages.Messages;
import term_code_generator.CodeGenerator;
import ui_licence.FormBrowser;
import ui_licence.Startup;

public class AboutMenu implements MainMenuItem {

	private MainMenu mainMenu;
	private Shell shell;
	private MenuItem aboutItem;
	
	public AboutMenu( MainMenu mainMenu, Menu menu ) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		aboutItem = create( menu );
	}

	/**
	 * Create the About menu in the main menu 
	 * and its sub menu items
	 * @param menu
	 */
	public MenuItem create ( Menu menu ) {
		
		MenuItem helpItem = new MenuItem( menu , SWT.CASCADE );
		helpItem.setText( Messages.getString("BrowserMenu.AboutMenuName") );
		
		Menu helpMenu = new Menu( menu );
		helpItem.setMenu( helpMenu );
		
		addDerbyLicenceMI ( helpMenu );
		addFoodexLicenceMI ( helpMenu );
		
		return helpItem;
	}
	

	/**
	 * Add a menu item which allows seeing the derby licence
	 * @param menu
	 */
	private void addDerbyLicenceMI ( Menu menu ) {
		
		MenuItem derbyItem = new MenuItem( menu , SWT.NONE );
		derbyItem.setText( Messages.getString("BrowserMenu.ApacheLicenceCmd") );
		
		derbyItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {

				FormBrowser bf = new FormBrowser( shell, 
						Messages.getString("BrowserMenu.ApacheLicenceWindowTitle"), 
						this.getClass().getClassLoader()
						.getResourceAsStream( "DerbyNotice.txt" ) );

				bf.display();
			}

		} );
	}
	
	
	
	/**
	 * Add a menu item which allows seeing the licence of the foodex browser
	 * @param menu
	 */
	private void addFoodexLicenceMI ( Menu menu ) {
		
		MenuItem aboutItem = new MenuItem( menu , SWT.NONE );
		aboutItem.setText( Messages.getString("BrowserMenu.Licence") );
		
		aboutItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {
				Startup startup = new Startup( shell.getDisplay() );
				startup.setDialog();
				startup.Display();
			}
		} );
	}

	@Override
	public void refresh() {}
}
