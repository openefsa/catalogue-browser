package ui_main_menu;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import i18n_messages.CBMessages;
import user_interface.ProxySettingsDialog;

public class ProxyMenu implements MainMenuItem {

	private Shell shell;
	
	public ProxyMenu(Menu menu, Shell shell) {
		this.shell = shell;
		create(menu);
	}
	
	@Override
	public MenuItem create(Menu menu) {
		
		MenuItem item = new MenuItem(menu , SWT.PUSH);

		item.setText(CBMessages.getString("proxy.config.menu"));
		item.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ProxySettingsDialog dialog = new ProxySettingsDialog(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
				try {
					dialog.open();
				} catch (IOException e) {
					e.printStackTrace();
					// TODO warning
				}
			}
		});
		
		return item;
	}

	@Override
	public void refresh() {}
}
