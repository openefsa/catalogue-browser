package ui_main_panel;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import catalogue.Catalogue;
import global_manager.GlobalManager;
import messages.Messages;

public class CatalogueLabel implements Observer {

	private Composite composite;
	private Label label;
	private Catalogue catalogue;
	
	/**
	 * Initialize and display the label in the parent composite
	 * @param parent
	 */
	public CatalogueLabel( Composite parent ) {
		
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

		Font italicBoldFold = new Font( Display.getCurrent(), 
				new FontData( fontData.getName(), fontData.getHeight() + 5, SWT.ITALIC | SWT.BOLD ) );

		label.setFont ( italicBoldFold );

		refresh();
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
		this.composite.getParent().layout();
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
			
			// update the catalogue label
			refresh();
		}
	}
}
