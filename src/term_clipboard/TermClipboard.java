package term_clipboard;

import java.util.ArrayList;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import catalogue_object.Applicability;
import catalogue_object.CatalogueObject;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import term.TermSubtreeIterator;

/**
 * Class to manage all the cut copy paste operations related to terms.
 * We can cut/copy a term and then paste it as a child of another term.
 * Or we can copy the term code, name, full code into the system clipboard.
 * How to use:
 * Instantiate the TermClipboard object
 * use the operation you need to perform as cut copy paste...
 * @author avonva
 *
 */
public class TermClipboard {

	// the clipboard possible operations, see setTermOperation for more information
	private enum ClipboardOp { WAIT, CUT_BRANCH, COPY_NODE, COPY_BRANCH, 
		COPY_CODE, COPY_FULLCODE, COPY_FULLCODE_WITH_IMPLICIT, COPY_DESCRIPTION,
		COPY_DESCRIPTION_WITH_IMPLICIT, COPY_FULLCODE_AND_DESC, 
		COPY_FULLCODE_AND_DESC_WITH_IMPLICIT,
		COPY_CODE_NAME, COPY_FULLCODE_NAME }
	
	// initial state of the clipboard
	ClipboardOp clipOp = ClipboardOp.WAIT;
	
	// the terms which are cutted/copied
	private ArrayList<Term> sources = new ArrayList<>();
	
	// the hierarchy in which the term was cutted/copied
	private Hierarchy sourceHierarchy;
	
	public ArrayList<Term> getSources() {
		return sources;
	}
	
	/**
	 * Set an operation to be performed for multiple terms
	 * allowed clipboard operations for this method:
	 * - WAIT: no operation
	 * - CUT_BRANCH: cut a term and its subtree, only allowed in the same hierarchy
	 * - COPY_NODE: copy a node without subtree, only allowed for different hierarchies
	 * - COPY_BRANCH: copy a term and its subtree, only allowed for different hierarchies
	 * @param source
	 * @param sourceHierarchy
	 * @param op
	 */
	private void setTermOperation( ArrayList<Term> sources, 
			Hierarchy sourceHierarchy, ClipboardOp op ) {
		
		// set the involved terms and hierarchies
		this.sources = sources;
		this.sourceHierarchy = sourceHierarchy;
		
		// set the operation
		clipOp = op;
	}
	
	/**
	 * Cut an entire branch
	 * @param source
	 * @param sourceHierarchy
	 */
	public void cutBranch ( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		setTermOperation( sources, sourceHierarchy, ClipboardOp.CUT_BRANCH );
	}
	
	/**
	 * Copy a node
	 * @param source
	 * @param sourceHierarchy
	 * @param destination
	 * @param destinationHierarchy
	 */
	public void copyNode ( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		setTermOperation( sources, sourceHierarchy, ClipboardOp.COPY_NODE );
	}
	
	/**
	 * Copy an entire branch of terms
	 * @param source
	 * @param sourceHierarchy
	 */
	public void copyBranch ( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		setTermOperation( sources, sourceHierarchy, ClipboardOp.COPY_BRANCH );
	}
	
	
	/**
	 * Copy the terms code in the clipboard, one code per line
	 * @param source
	 */
	public String copyCode ( ArrayList<Term> sources ) {
		return copyTextInClipboard( sources, ClipboardOp.COPY_CODE );
	}
	
	/**
	 * Copy the terms full code in the clipboard, one code per line.
	 * Set copyImplicit to true if you want also implicit facets.
	 * This code contains all the explicit facets code.
	 * @param source
	 */
	public String copyFullCode ( ArrayList<Term> sources, boolean copyImplicit ) {
		
		if ( copyImplicit )
			return copyTextInClipboard( sources, ClipboardOp.COPY_FULLCODE_WITH_IMPLICIT );
		else
			return copyTextInClipboard( sources, ClipboardOp.COPY_FULLCODE );
	}

	/**
	 * Copy the terms interpreted code. Set copyImplicit to true if you want
	 * also implicit facets in the code.
	 * One string per line
	 * @param source
	 */
	public String copyDescription ( ArrayList<Term> sources, boolean copyImplicit ) {
		
		if ( copyImplicit )
			return copyTextInClipboard( sources, ClipboardOp.COPY_DESCRIPTION_WITH_IMPLICIT );
		else
			return copyTextInClipboard( sources, ClipboardOp.COPY_DESCRIPTION );
	}
	
	/**
	 * Copy the terms full code and description in a tab separated format. 
	 * Set copyImplicit to true if you want
	 * also implicit facets in the code.
	 * One string per line
	 * @param source
	 */
	public String copyFullCodeAndDescription ( ArrayList<Term> sources, boolean copyImplicit ) {
		
		if ( copyImplicit )
			return copyTextInClipboard( sources, ClipboardOp.COPY_FULLCODE_AND_DESC_WITH_IMPLICIT );
		else
			return copyTextInClipboard( sources, ClipboardOp.COPY_FULLCODE_AND_DESC );
	}

	/**
	 * Copy the code and name of the terms, tab separated in the clipboard. One string per line.
	 * @param source
	 */
	public String copyCodeName ( ArrayList<Term> sources ) {
		return copyTextInClipboard( sources, ClipboardOp.COPY_CODE_NAME );
	}
	
	/**
	 * Copy the terms full code (i.e. with implicit facets) and name, tab separated, in the clipboard
	 * One string per line
	 * @param source
	 */
	public String copyFullCodeName ( ArrayList<Term> sources ) {
		return copyTextInClipboard( sources, ClipboardOp.COPY_FULLCODE_NAME );
	}

	
	/**
	 * Copy a text field of the term into the clipboard depending on the clipboard operation
	 * Multiple terms can be passed to copy several codes in one time.
	 * Return the created string
	 * @param source
	 * @param operation
	 */
	private String copyTextInClipboard( ArrayList<Term> sources, ClipboardOp operation ) {
		
		clipOp = operation;
		
		StringBuilder strings = new StringBuilder();
		
		// for each source get its copied field and add to the data string
		for ( int i = 0; i < sources.size(); i++ ) {
			
			// add the copied field to the data
			strings.append( getCopiedField ( sources.get(i) ) );
			
			// if it is not the last source, add the \n
			if ( i < sources.size() - 1 )
				strings.append( "\n" );
		}
		
		// get the system clipboard
		Clipboard clipboard = new Clipboard( Display.getCurrent() );
		
		// set the clipboard contents
		clipboard.setContents( new Object[] { strings.toString() }, 
				new Transfer[] { TextTransfer.getInstance() } );
		
		// dispose the clipboard
		clipboard.dispose();
		
		// return the crafted string
		return strings.toString();
	}
	
	
	/**
	 * Get the string which has to be copied from the source based on the clipboard operation
	 * @param operation
	 * @return
	 */
	private String getCopiedField ( Term source ) {

		String data = "";
		
		switch ( clipOp ) {

		// get term code
		case COPY_CODE:
			data = source.getCode();
			break;
			
		// get term full code without implicit
		case COPY_FULLCODE:
			data = source.getFullCode( false, true );
			break;

		// get term full code with implicit
		case COPY_FULLCODE_WITH_IMPLICIT:
			data = source.getFullCode( true, true );
			break;
		
		case COPY_DESCRIPTION:
			data = source.getInterpretedCode( false );
			break;	
		
		case COPY_FULLCODE_AND_DESC:
			data = source.getFullCode( false, true ) + "\t" + source.getInterpretedCode( false );
			break;
			
		case COPY_DESCRIPTION_WITH_IMPLICIT:
			data = source.getFullCode( true, true ) + "\t" + source.getInterpretedCode( true );
			break;
		
		case COPY_FULLCODE_AND_DESC_WITH_IMPLICIT:
			data = source.getInterpretedCode( true );
			break;
			
		// get term code and name
		case COPY_CODE_NAME:
			data = source.getCode() + "\t" + source.getName();
			break;

		// get the term full code (i.e. with implicit facets) and name
		case COPY_FULLCODE_NAME:
			data = source.getFullCode( true, true ) + "\t" + source.getName();
			break;

		default:
			break;
		}

		return data;
	}
	
	/**
	 * Can we make a paste operation?
	 * @param destination
	 * @param destinationHierarchy
	 * @return
	 */
	public boolean canPaste ( CatalogueObject target, Hierarchy destinationHierarchy ) {
		
		// if wrong operation
		if ( destinationHierarchy == null || target == null )
			return false;
		
		// check if we can paste every source
		for ( Term source : sources ) {
			if ( !canPasteSource( source, target, destinationHierarchy ) )
				return false;
		}
		
		return true;
	}
	

	/**
	 * Paste the selected sources as children of the destination term
	 * @param destination
	 * @param destinationHierarchy
	 */
	public void paste( CatalogueObject target, Hierarchy destinationHierarchy ) {

		// return if we cannot paste the sources
		if ( !canPaste( target, destinationHierarchy ) )
			return;
		
		// for each source we make a paste operation
		for ( Term term : sources ) {
			pasteSingleSource ( term, target, destinationHierarchy );
		}
		
		// reset operation
		clipOp = ClipboardOp.WAIT;
	}

	
	/**
	 * Paste operation:
	 * If we have copied a node we paste a node
	 * If we have cut a branch we paste the entire branch
	 * If we have copied a branch we paste the entire branch
	 * 
	 * NOTE: cut branch is only usable inside the same hierarchy, since we cannot remove terms from hierarchies
	 *       we can only move them.
	 *       
	 *       copy node and copy branch are only usable in other hierarchies, since
	 *       we cannot copy terms in the same hierarchy from which we have copied them
	 *       (otherwise it would result in having equal terms in the same hierarchy)
	 * 
	 * @param source, the term which will be the child of the parent
	 * @param destination, the new parent for the cut/copied term
	 * @param destinationHierarchy, the new hierarchy for the cut/copied term
	 */
	private void pasteSingleSource ( Term source, CatalogueObject target, 
			Hierarchy destinationHierarchy ) {

		// perform the chosen operation
		switch ( clipOp ) {

		case WAIT:
			break;

		case CUT_BRANCH:
			
			// here source hierarchy = destination hierarchy (otherwise cannot cut)

			// move the term under the target
			source.moveAsChild( target, sourceHierarchy );

			// NOTE: All the subtree of the cut term
			// is automatically moved since we are in the same hierarchy (the sub tree
			// terms have already set the applicability for the same hierarchy!)

			break;

		case COPY_NODE:
			
			// here we create the new applicability in the new hierarchy
			// set the copied term as child of the selected parent term in the selected hierarchy
			pasteNode ( target, source, destinationHierarchy, 
					Term.getFirstAvailableChildrenOrder(target, destinationHierarchy ),
					source.isReportable( sourceHierarchy ) );

			break;

		case COPY_BRANCH:

			// paste the root
			pasteNode ( target, source, destinationHierarchy, 
					Term.getFirstAvailableChildrenOrder(target, destinationHierarchy ),
					source.isReportable( sourceHierarchy ) );

			// paste the root subtree
			pasteSubtree ( target, source, destinationHierarchy );

			break;

		default:
			break;
		}
	}
	
	
	/**
	 * Paste a single term under the parent term. We set the 'child' term as child of the 'parent' term
	 * in the selected hierarchy
	 * @return the child with the new applicability added
	 */
	private Term pasteNode ( CatalogueObject parent, Term child, 
			Hierarchy hierarchy, int order, boolean reportable ) {
		
		// create a new applicability in the new hierarchy
		Applicability appl = new Applicability( child, parent, hierarchy, order, reportable );

		// add the new applicability to the term
		child.addApplicability( appl, true );

		return child;
	}
	

	/**
	 * Paste an entire branch of terms (the root term and the subtree).
	 * Paste the child and all its subtree under the parent term inside the selected hierarchy
	 * @param parent
	 * @param child
	 * @param hierarchy
	 */
	private void pasteSubtree( CatalogueObject parent, Term child, 
			Hierarchy hierarchy ) {

		// iterate all the clip term subtree in the old hierarchy
		TermSubtreeIterator iterator = new TermSubtreeIterator( child, sourceHierarchy );

		Term subtreeTerm;
		while ( ( subtreeTerm = iterator.next() ) != null ) {

			// get the parent of the child in its hierarchy 
			Term subtreeParent = subtreeTerm.getParent( sourceHierarchy );

			// link the subtree parent and the child the new hierarchy
			// for subtree elements, just maintain the original order
			pasteNode ( subtreeParent, subtreeTerm, hierarchy, 
					subtreeTerm.getOrder(sourceHierarchy),
					subtreeTerm.isReportable( sourceHierarchy ) );
		}
	}
	
	
	/**
	 * Can we paste the term under the selected parent?
	 * @return
	 */
	private boolean canPasteSource ( Term source, CatalogueObject target, 
			Hierarchy destinationHierarchy ) {
		
		// we can paste only if the term is not already present
		// and if we have chosen a correct hierarchy to paste
		return termCheck( source, target, destinationHierarchy ) 
				&& hierarchyCheck ( destinationHierarchy );
	}
	
	
	/**
	 * Check if the terms can be correctly added to the hierarchy or not
	 * @param destinationHierarchy
	 * @return
	 */
	private boolean termCheck ( Term source, CatalogueObject target, 
			Hierarchy destinationHierarchy ) {
		
		boolean termCheck;
		
		switch ( clipOp ) {
		
		// if wait => we have no source we cannot check
		
		// if cut => we check that the term is not cut under itself
		case CUT_BRANCH:
			termCheck = !source.equals( target );
			break;
			
			// if copy we check that the term is not already present into the hierarchy
		case COPY_NODE:
		case COPY_BRANCH:
			termCheck = !source.belongsToHierarchy ( destinationHierarchy );
			break;
		default:
			termCheck = false;
			break;
		}

		return termCheck;
	}
	
	
	/**
	 * Check if the destination hierarchy is compatible for making a paste
	 * @param destinationHierarchy
	 * @return
	 */
	private boolean hierarchyCheck ( Hierarchy destinationHierarchy ) {
		
		// check if the new hierarchy is correct respect to the one we have copied from
		boolean hierarchyCheck = false;

		switch ( clipOp ) {

		// cannot paste if we haven't cutted/copied anything
		case WAIT:
			hierarchyCheck = false;
			break;

			// if we are cutting and we paste on a different hierarchy STOP we cannot do this
		case CUT_BRANCH:
			hierarchyCheck = sourceHierarchy.equals( destinationHierarchy );
			break;

			// if we are copying and we try to copy a term into the same
			// hierarchy stop. We cannot have two equal term in the same hierarchy!
		case COPY_NODE:
		case COPY_BRANCH:
			hierarchyCheck = !sourceHierarchy.equals( destinationHierarchy );
			break;
		
		default:
			break;
		}
		
		return hierarchyCheck;

	}
}
