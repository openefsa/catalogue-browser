package term_tree_item;

import java.util.ArrayList;

import catalogue_object.Catalogue;
import catalogue_object.GlobalTerm;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;

/**
 * Object which is used in tree viewers that display
 * terms/hierarchies and global terms.
 * TODO not working...
 * @author avonva
 *
 */
 @Deprecated
public class TermTreeItem implements Nameable {
	
	private Catalogue catalogue;
	private Nameable nameable;
	private Hierarchy hierarchy;
	
	public TermTreeItem( Catalogue catalogue, Nameable nameable, Hierarchy hierarchy ) {
		this.catalogue = catalogue;
		this.nameable = nameable;
		this.hierarchy = hierarchy;
	}
	
	/**
	 * Create a tree item with a term node. We need to
	 * specify the hierarchy
	 * which contains the term.
	 * @param term the term
	 * @param hierarchy the hierarchy which contains the node
	 */
	public TermTreeItem( Term term, Hierarchy hierarchy ) {
		this ( term.getCatalogue(), term, hierarchy );
	}
	
	/**
	 * Create a tree item with a hierarchy node.
	 * @param hierarchy
	 */
	public TermTreeItem( Hierarchy hierarchy ) {
		this ( hierarchy.getCatalogue(), hierarchy, hierarchy );
	}
	
	/**
	 * Create a tree item with a global term as node.
	 * We need the catalogue we are working with since
	 * the global term does not have this information
	 * @param catalogue
	 * @param nameable
	 */
	public TermTreeItem( Catalogue catalogue, GlobalTerm globalTerm ) {
		this ( catalogue, globalTerm, null );
	}
	
	public Nameable getNameable() {
		return nameable;
	}

	public Hierarchy getHierarchy() {
		return hierarchy;
	}

	@Override
	public String getLabel() {
		return nameable.getLabel();
	}
	
	/**
	 * Check if the inner object is a term
	 * @return
	 */
	public boolean isTerm() {
		return nameable instanceof Term;
	}
	
	/**
	 * Get the term if there is one
	 * @return
	 */
	public Term getTerm() {
		
		if ( !isTerm() )
			return null;
		
		return (Term) nameable;
	}
	
	/**
	 * Check if the inner object is a hierarchy
	 */
	public boolean isHierarchy() {
		return nameable instanceof Hierarchy;
	}
	
	/**
	 * Check if the inner object is a global term
	 */
	public boolean isGlobalTerm() {
		return nameable instanceof GlobalTerm;
	}
	
	@Override
	public boolean equals(Object obj) {

		if ( obj instanceof TermTreeItem ) {

			TermTreeItem tti = (TermTreeItem) obj;
			
			boolean sameNameable = nameable.equals( tti.getNameable() );
			boolean sameHierarchy = true;
			
			if ( hierarchy != null && tti.getHierarchy() != null )
				sameHierarchy = hierarchy.equals( tti.getHierarchy() );
			System.out.println ( nameable + " " + tti.getNameable()  );
			return sameNameable && sameHierarchy;
		}
		
		if ( obj instanceof Term ) {
			return nameable.equals( obj );
		}
		
		// TODO all the cases are needed
		return false;
	}
	
	/**
	 * Get the children of the term tree item
	 * @param hideDeprecated
	 * @param hideNotReportable
	 * @return
	 */
	public ArrayList<TermTreeItem> getChildren( boolean hideDeprecated, boolean hideNotReportable ) {
		
		ArrayList<TermTreeItem> treeChildren = new ArrayList<>();
		
		// if the inner object is a term get all its
		// children as children
		if ( nameable instanceof Term ) {
			
			ArrayList<Term> children = ( (Term) nameable).getChildren(hierarchy, hideDeprecated, hideNotReportable);

			for ( Term child : children )
				treeChildren.add( new TermTreeItem( child, hierarchy) );
		}
		
		// if the inner object is a hierarchy get all its
		// first level terms as children
		else if ( nameable instanceof Hierarchy ) {

			Hierarchy hierarchy = (Hierarchy) nameable;

			for ( Term term : (hierarchy.getFirstLevelNodes(hideDeprecated, hideNotReportable) ) )
				treeChildren.add( new TermTreeItem( term, hierarchy ) );
		}
		else if ( nameable instanceof GlobalTerm ) {

			// if we have root add global term: all terms
			if ( nameable == GlobalTerm.Root ) {
				treeChildren.add( new TermTreeItem( catalogue, GlobalTerm.AllTerms ) );
			}

			// if we have all terms we add global terms: all hierarchies and all facets
			else if ( nameable == GlobalTerm.AllTerms ) {
				treeChildren.add( new TermTreeItem( catalogue, GlobalTerm.AllHierarchies ) );
				treeChildren.add( new TermTreeItem( catalogue, GlobalTerm.AllFacets ) );
			}

			// if we have allhierarchies we add all the
			// catalogue base hierarchies
			else if ( nameable == GlobalTerm.AllHierarchies ) {

				// Here we add the BASE hierarchies
				for ( Hierarchy hierarchy : catalogue.getHierarchies() ) {
					if ( hierarchy.isHierarchy() ) {
						treeChildren.add( new TermTreeItem(hierarchy) );
					}
				}
			}

			// if we have allfacets we add all the
			// catalogue facet hierarchies
			else if ( nameable == GlobalTerm.AllFacets ) {

				// Here we add the FACET hierarchies
				for ( Hierarchy hierarchy : catalogue.getHierarchies() ) {
					if ( hierarchy.isFacet() ) {
						treeChildren.add( new TermTreeItem(hierarchy) );
					}
				}
			}
		}
		
		return treeChildren;
	}
}
