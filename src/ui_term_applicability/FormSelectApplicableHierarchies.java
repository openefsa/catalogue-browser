package ui_term_applicability;

import java.util.ArrayList;
import messages.Messages;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import catalogue_object.Hierarchy;
import catalogue_object.Term;

/**
 * This form allows selecting one or more hierarchies among the hierarchies which contains the parent
 * but do not contain the child. We use this form in the applicability addition.
 * @author avonva
 *
 */
public class FormSelectApplicableHierarchies {

	private Term parentTerm, child;
	private Composite parent;
	private Listener doneListener;
	
	public FormSelectApplicableHierarchies( Term parentTerm, Term child, Composite parent ) {
		this.parentTerm = parentTerm;
		this.child = child;
		this.parent = parent;
	}
	
	/**
	 * Add a listener which is called when the ok button is pressed. Contains all the selected hierarchies in data
	 * @param listener
	 */
	public void addDoneListener ( Listener listener ) {
		doneListener = listener;
	}
	
	/**
	 * Method called when the form is created
	 */
	public void display ( ) {
		
		// Set the layout of the form
		final Shell dialog = new Shell( parent.getShell(), SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );
		
		// window icon (on the top left)
		dialog.setImage( new Image( Display.getCurrent() , this.getClass().getClassLoader()
				.getResourceAsStream( "Choose.gif" ) ) );
		
		dialog.setText( Messages.getString( "FormSelectApplicabilityHierarchy.Title" ) );  // window title
		
		dialog.setLayout( new GridLayout(1, false) );  // layout style
		
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		
		dialog.setLayoutData( gridData );
		
		Label label = new Label ( dialog, SWT.NONE );
		label.setText( Messages.getString("FormSelectApplicabilityHierarchy.Message") );
		
		final CheckboxTableViewer table = CheckboxTableViewer.newCheckList( dialog, SWT.NONE );

		table.setContentProvider( new TableContentProvider() );
		table.setLabelProvider( new TableLabelProvider() );

		// set as input all the hierarchies where the parent is present
		// but the child is not present
		table.setInput( parentTerm.getNewHierarchies(child) );
		table.setAllChecked( true );
		
		Button ok = new Button( dialog, SWT.PUSH );
		ok.setText ( Messages.getString("FormSelectApplicabilityHierarchy.OkButton") );
		
		ok.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// call the listeneer if it was set
				if ( doneListener != null ) {
					
					Event event = new Event();
					
					ArrayList<Hierarchy> selectedHierarchies = new ArrayList<>();
					
					// for each hierarchy put it into the array list
					for ( Object item : table.getCheckedElements() ) {
						selectedHierarchies.add( (Hierarchy) item );
					}
		
					// set the data
					event.data = selectedHierarchies;
					
					doneListener.handleEvent( event );
					
				}
				
				dialog.close();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		// show the dialog
		dialog.pack();
		dialog.open();
		dialog.setVisible(true);  
	}
	
	/**
	 * Table content provider for the class table
	 * @author avonva
	 *
	 */
	private class TableContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

		@Override
		public Object[] getElements(Object arg0) {

			@SuppressWarnings("unchecked")
			ArrayList<Hierarchy> hierarchies = (ArrayList<Hierarchy>) arg0;

			return hierarchies.toArray();
		}
	}
	
	/**
	 * Label provider for the table of the class
	 * @author avonva
	 *
	 */
	private class TableLabelProvider implements ILabelProvider {

		@Override
		public void addListener(ILabelProviderListener arg0) {}

		@Override
		public void dispose() {}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {}

		@Override
		public Image getImage(Object arg0) {
			return null;
		}

		@Override
		public String getText(Object arg0) {

			Hierarchy hierarchy = (Hierarchy) arg0;
			
			return hierarchy.getLabel();
		}
		
	}
	
}
