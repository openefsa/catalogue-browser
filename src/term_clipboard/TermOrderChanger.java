package term_clipboard;

import java.util.ArrayList;

import catalogue_object.Hierarchy;
import catalogue_object.Term;

/**
 * This class is used to manage all the changes of terms order (move up, move down, move level up, drag&drop)
 * How to use: 
 * Instantiate the term order changer
 * Call the method you need to change terms order
 * @author avonva
 *
 */
public class TermOrderChanger {

	/**
	 * Move up all the selected terms if possible
	 * @param sources
	 * @return true if the order was successfully updated
	 */
	public boolean moveUp ( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		
		// if we cannot move up sources => return
		if ( !canMoveUp( sources, sourceHierarchy ) )
			return false;
		
		// for each selected term we move up
		for ( Term term : sources )
			term.moveUp( sourceHierarchy );
		
		return true;
	}
	
	/**
	 * Move up all the selected terms if possible
	 * @param sources
	 * @return true if the order was successfully updated
	 */
	public boolean moveDown ( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		
		// if we cannot move up sources => return
		if ( !canMoveDown( sources, sourceHierarchy ) )
			return false;
		
		// Invert the term order for moving down terms
		// since otherwise this would happen if two adiacent terms are moved down:
		// the first term is moved down replacing the second selected term
		// the second term is moved down replacing the FIRST selected term
		// and therefore in the end nothing happen because the two terms
		// are swapped two times. Inverting the order guarantees that
		// we first move down the LAST term and then we move the first
		// avoiding the problem
		
		// for each selected term we move down
		for ( int i = sources.size() - 1; i >= 0; i-- )
			sources.get( i ).moveDown( sourceHierarchy );
		
		return true;
	}
	
	/**
	 * Move up all the selected terms if possible
	 * @param sources
	 * @return true if the order was successfully updated
	 */
	public boolean moveLevelUp ( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		
		// if we cannot move up sources => return
		if ( !canMoveLevelUp( sources, sourceHierarchy ) )
			return false;
		
		// for each selected term we move up
		for ( Term term : sources )
			term.moveLevelUp( sourceHierarchy );
		
		return true;
	}
	
	/**
	 * Check if the sources can be moved up in the order or not
	 * @return
	 */
	public boolean canMoveUp ( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		
		boolean canMoveUp = true;
		
		// check if all the sources can move up or not (i.e. they have an above sibling)
		for ( Term source : sources )
			canMoveUp = canMoveUp && source.hasAboveSibling( sourceHierarchy );
		
		return canMoveUp;
	}
	
	/**
	 * Check if the sources can be moved down in the order or not
	 * @return
	 */
	public boolean canMoveDown( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		
		boolean canMoveDown = true;
		
		// check if all the sources can move down or not (i.e. they have a below sibling)
		for ( Term source : sources )
			canMoveDown = canMoveDown && source.hasBelowSibling( sourceHierarchy );

		return canMoveDown;
	}
	
	/**
	 * Check if we can move the sources one level up into the source hierarchy
	 * @return
	 */
	public boolean canMoveLevelUp( ArrayList<Term> sources, Hierarchy sourceHierarchy ) {
		
		boolean canLevelUp = true;
		
		// check if all the sources can move one level up or not. We cannot move a source 
		// one level up if we are at the top level of the hierarchy!
		for ( Term source : sources )
			canLevelUp = canLevelUp && source.hasParent( sourceHierarchy );

		return canLevelUp;
	}
}
