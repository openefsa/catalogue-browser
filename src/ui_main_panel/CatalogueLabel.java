package ui_main_panel;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolTip;

import catalogue.Catalogue;
import dcf_user.User;
import dcf_user.UserAccessLevel;
import dcf_user.UserLevelChangedListener;
import global_manager.GlobalManager;
import messages.Messages;
import ui_main_menu.FileActions;

public class CatalogueLabel implements Observer {

	private Composite composite;
	private Composite buttonComp;
	private Button updateBtn;
	private Label label;
	private Catalogue catalogue;
	private ToolTip toolTip;
	
	/**
	 * Initialize and display the label in the parent composite
	 * @param parent
	 */
	public CatalogueLabel( final Composite parent ) {
		
		composite = new Composite( parent, SWT.NONE );
		composite.setLayout( new GridLayout(1, false) );
		
		GridData gd = new GridData();
		gd.exclude = true;
		composite.setVisible(false);
		composite.setLayoutData(gd);
		
		// label which shows the label of the current opened catalogue
		label = new Label( composite, SWT.NONE );
		
		// set the label font to italic and bold
		FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

		Font font = new Font( Display.getCurrent(), 
				new FontData( fontData.getName(), fontData.getHeight() + 5, SWT.ITALIC | SWT.BOLD ) );

		label.setFont ( font );
		
		addUpdatePanel();

		refresh();
		
		registerForUserLevel();
	}
	
	private void addUpdatePanel() {
		
		toolTip = new ToolTip(composite.getShell(), SWT.ICON_INFORMATION | SWT.BALLOON);
		
		buttonComp = new Composite(composite, SWT.NONE);
		buttonComp.setLayout( new GridLayout(2, false) );
		
		GridData gd = new GridData();
		gd.exclude = true;
		buttonComp.setVisible(false);
		buttonComp.setLayoutData(gd);
		
		Label newVersionAvailable = new Label(buttonComp, SWT.NONE);
		newVersionAvailable.setText(Messages.getString("CatalogueLabel.UpdateLabel"));
		
		// set the label font to italic and bold
		FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

		Font font = new Font( Display.getCurrent(), 
				new FontData( fontData.getName(), fontData.getHeight() + 3, SWT.BOLD) );

		newVersionAvailable.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));
		
		newVersionAvailable.setFont ( font );
		
		updateBtn = new Button(buttonComp, SWT.PUSH);
		updateBtn.setText(Messages.getString("CatalogueLabel.UpdateButton"));
		
		updateBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// download the last catalogue release
				// and open it
				
				FileActions.downloadLastVersion(composite.getShell(), 
						catalogue, null);
			}
		});
	}

	/**
	 * Refresh composite each time the user level changes
	 */
	private void registerForUserLevel() {
		
		User.getInstance().addUserLevelChangedListener(new UserLevelChangedListener() {
			
			@Override
			public void userLevelChanged(UserAccessLevel newLevel) {
				
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						
						// refresh the label and the update button
						if (!label.isDisposed())
							refresh();
					}
				});

			}
		});
	}
	
	/**
	 * Set the default label
	 */
	public void setDefaultLabel () {
		label.setText( Messages.getString( "CatalogueLabel.EmptyLabel" ) );
	}
	
	/**
	 * Set the label text using the catalogue information
	 * @param catalogue
	 */
	public void setText( Catalogue catalogue ) {

		this.catalogue = catalogue;
		
		String text = "";
		
		// display only the name if the catalogue is local
		if ( catalogue.isLocal() )
			text = catalogue.getLabel();
		else
			text = catalogue.getVersion() + " " + catalogue.getLabel();
		
		label.setText( text );
	}
	
	private void open() {
		this.composite.setVisible(true);
		((GridData) this.composite.getLayoutData()).exclude = false;
		
		// show update button and tool tip if there is an update
		if (addUpdateButton()) {
			
			this.buttonComp.setVisible(true);
			((GridData) this.buttonComp.getLayoutData()).exclude = false;
			final String current = this.catalogue.getCode();

			Display.getDefault().timerExec(5000, new Runnable() {
				
				@Override
				public void run() {
					
					// if the catalogue was changed
					if (!current.equals(catalogue.getCode()))
						return;
					
					if (toolTip.isDisposed())
						return;
					
					toolTip.setText(Messages.getString("CatalogueLabel.ToolTipTitle"));
					toolTip.setMessage(Messages.getString("CatalogueLabel.ToolTipMessage"));
					toolTip.setLocation(updateBtn.toDisplay(16, 16));
					toolTip.setVisible(true);
					toolTip.setAutoHide(false);
					
					
					// start auto hide after other 5 seconds
					Display.getDefault().timerExec(10000, new Runnable() {
						
						@Override
						public void run() {
							
							// if the catalogue was changed
							if (!current.equals(catalogue.getCode()))
								return;
							
							if (toolTip.isDisposed())
								return;
							
							toolTip.setAutoHide(true);
						}
					});
				}
			});
		}
		else {
			this.buttonComp.setVisible(false);
			((GridData) this.buttonComp.getLayoutData()).exclude = true;
		}
		
		this.composite.getParent().layout();
	}
	
	private boolean addUpdateButton() {
		return catalogue != null && catalogue.hasUpdate() 
				&& !catalogue.isLastReleaseAlreadyDownloaded();
	}
	
	/**
	 * Refresh the label
	 */
	public void refresh() {
		if ( catalogue != null )
			setText ( catalogue );
		else
			setDefaultLabel();
		
		open();
	}
	
	/**
	 * Redraw the label
	 */
	public void redraw() {
		label.redraw();
	}
	
	@Override
	public void update(Observable o, Object arg) {

		// update current catalogue
		if ( o instanceof GlobalManager ) {
			
			if ( arg instanceof Catalogue )
				this.catalogue = (Catalogue) arg;
			else
				this.catalogue = null;
			
			this.toolTip.setVisible(false);
			
			// update the catalogue label
			refresh();
		}
	}
}
