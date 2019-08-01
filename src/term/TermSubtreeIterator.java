package term;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import catalogue_object.Hierarchy;
import catalogue_object.Term;

/**
 * Class used to iterate all the terms contained in the branch of a selected parent term.
 * We can use this class to make actions on the entire subtree of the term.
 * @author avonva
 *
 */
public class TermSubtreeIterator {

	Term parent;
	Hierarchy hierarchy;
	Queue<Term> children;
	
	/**
	 * Iterate all the parent subtree children. Use next() to get the next term iteratively
	 * @param parent
	 * @param hierarchy
	 */
	public TermSubtreeIterator( Term parent, Hierarchy hierarchy ) {
		
		// set the term from which getting the subtree
		this.parent = parent;
		
		// set the hierarchy in which searching children
		this.hierarchy = hierarchy;
		
		// queue which contains all the terms
		children = new LinkedList<Term>();
		
		// add the first level children
		initializeQueue();
	}
	
	/**
	 * Initialize the queue using the first level children of the parent term
	 */
	private void initializeQueue () {
		// get the term children in the current hierarchy as starting point
		children.addAll( parent.getAllChildren( hierarchy ) );
	}
	
	/**
	 * Get the next term contained the queue.
	 * @return the next term if there is one, otherwise null
	 */
	public Term next() {
		
		// return null if empty queue
		if ( children.isEmpty() )
			return null;
		
		// get the current child
		Term child = children.poll();
		
		if (child == null)
			return null;
		
		Collection<Term> list = child.getAllChildren(hierarchy);
		
		if ( list == null || list.isEmpty() )
			return child;
		
		// add the child children to the queue to go deeper in the tree
		children.addAll( list );
		
		return child;
	}
}
