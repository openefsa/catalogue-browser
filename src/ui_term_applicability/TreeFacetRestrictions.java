package ui_term_applicability;
import java.util.ArrayList;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import catalogue.Catalogue;
import messages.Messages;
import term.LabelProviderTerm;
import term.TermElem;

/**
 * Create a tree viewer which contains all the facet restrictions of the selected term
 * @author avonva
 *
 */
@SuppressWarnings("deprecation")
public class TreeFacetRestrictions {

	private TreeViewer tree;  // tree which contains the restrictions

	/**
	 * Get the treeViewer
	 * @return
	 */
	public TreeViewer getTreeViewer() {
		return tree;
	}
	
	/**
	 * Set the input for the restriction tree
	 * @param input
	 */
	public void setInput ( Object input ) {
		tree.setInput( input );
		
		if ( input == null )
			tree.getTree().setMenu( null );
	}

	/**
	 * Constructor, create the treeviewer with the restrictions
	 * @param parent
	 */
	public TreeFacetRestrictions( Composite parent, Catalogue catalogue ) {

		// create the group
		Group groupTermAttributeApplicability = new Group( parent , SWT.NONE );
		groupTermAttributeApplicability.setText( Messages.getString("TreeFacetRestrictions.Title") ); //$NON-NLS-1$

		// Create the tree to show the restrictions
		tree = new TreeViewer( groupTermAttributeApplicability , SWT.SINGLE
				| SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );

		// Set the content and the label provider
		tree.setContentProvider( new ContentProviderTermAttributeApplicability() );
		tree.setLabelProvider( new LabelProviderTermAttributeApplicability( catalogue ) );


		// set the layout data
		GridData gridData = new GridData();

		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.minimumHeight = 100;
		gridData.heightHint = 100;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		groupTermAttributeApplicability.setLayoutData( gridData );
		groupTermAttributeApplicability.setLayout( new FillLayout() );

	}



	/**
	 * Content provider of the tree viewer
	 * @author avonva
	 *
	 */
	private class ContentProviderTermAttributeApplicability implements ITreeContentProvider {

		public void dispose ( ) {}

		public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {}

		/**
		 * Get the children of the object arg0
		 */
		public Object[] getChildren ( Object arg0 ) {

			System.err.println( "retrieving the root elements for constraints" ); //$NON-NLS-1$
			System.err.println( arg0 );

			ArrayList< TermElem > teChildren = null;

			if ( arg0 != null ) {
				TermElem te = (TermElem) arg0;
				if ( te.children != null ) {
					teChildren = te.children;
				}
			}

			if ( teChildren == null )
				teChildren = new ArrayList< TermElem >();

			return teChildren.toArray();
		}


		/*
		 * in this get element the list of the first nodes is passed and not a root
		 * element
		 */
		@SuppressWarnings("unchecked")
		public Object[] getElements ( Object arg0 ) {
			System.err.println( "retrieving the root elements for constraints" ); //$NON-NLS-1$
			System.err.println( arg0 );

			ArrayList< TermElem > teChildren = null;
			if ( arg0 != null ) {
				teChildren = (ArrayList< TermElem >) arg0;
			}

			if ( teChildren == null )
				teChildren = new ArrayList< TermElem >();
			return teChildren.toArray();
		}

		/**
		 * Get the parent of the element arg0
		 */
		public Object getParent ( Object arg0 ) {
			TermElem te = null;
			if ( arg0 != null ) {
				te = ( (TermElem) arg0 ).parent;
			}
			return te;

		}

		/**
		 * Check if arg0 has children or not
		 */
		public boolean hasChildren ( Object arg0 ) {

			ArrayList< TermElem > teChildren = null;
			if ( arg0 != null ) {
				TermElem te = (TermElem) arg0;
				if ( te.children != null ) {
					teChildren = te.children;
				}
			}
			if ( teChildren == null )
				teChildren = new ArrayList< TermElem >();
			return teChildren.size() > 0 ? true : false;
		}

	}




	/**
	 * This class is responsible for providing an image ad text for each item
	 * contained in the tree viewer
	 * 
	 * @author
	 * 
	 */
	private class LabelProviderTermAttributeApplicability implements ILabelProvider {

		LabelProviderTerm	_termLabelProvider;

		public LabelProviderTermAttributeApplicability( Catalogue catalogue ) {
			try {
				_termLabelProvider = new LabelProviderTerm();
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}

		public void addListener ( ILabelProviderListener arg0 ) {}

		public void dispose ( ) {}

		public boolean isLabelProperty ( Object arg0 , String arg1 ) {
			return false;
		}

		public void removeListener ( ILabelProviderListener arg0 ) {}

		/**
		 * This method answers an SWT Image to be used when displaying the domain
		 * object
		 */
		public Image getImage ( Object arg0 ) {
			if ( arg0 != null ) {
				return _termLabelProvider.getImage( ( (TermElem) arg0 ).term );
			}
			return null;
		}

		/**
		 * The getText method answers a string that represents the label for the
		 * domain object, element.
		 */
		public String getText ( Object arg0 ) {

			if ( arg0 != null ) {
				return _termLabelProvider.getText( ( (TermElem) arg0 ).term );
			}
			return null;
		}

	}
}
