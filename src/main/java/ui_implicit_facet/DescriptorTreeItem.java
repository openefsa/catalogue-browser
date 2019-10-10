package ui_implicit_facet;

import java.util.ArrayList;

import catalogue_object.Term;

/**
 * This class represent a node of a tree. In particular, each node is a term plus a facet descriptor, that is,
 * a term which is used as facet. We set also the information related to the parent of the descriptor
 * and to its children
 * @author avonva
 *
 */
public class DescriptorTreeItem {

	private Term term;                                // term contained in the node
	private FacetDescriptor descriptor;               // facet descriptor related to the term
	private DescriptorTreeItem parent;                // parent of the node
	private ArrayList<DescriptorTreeItem> children;   // children of the node
	private boolean inherited = false;                // is the facet inherited?
	
	/**
	 * Create a node of the tree passing the base
	 * term on which we are adding descriptors and
	 * the related descriptor.
	 * @param term the base term
	 * @param descriptor the descriptor added to the term
	 * @param inherited true if the facet is inherited by parents
	 */
	public DescriptorTreeItem( Term term, FacetDescriptor descriptor, boolean inherited ) {
		this.term = term;
		this.descriptor = descriptor;
		this.inherited = inherited;
		children = new ArrayList<>();
	}
	
	/**
	 * Get the term contained in this tree node
	 * @return
	 */
	public Term getTerm() {
		return term;
	}
	
	/**
	 * Get the facet descriptor contained in this tree node
	 * @return
	 */
	public FacetDescriptor getDescriptor() {
		return descriptor;
	}
	
	/**
	 * Get the parent of this node
	 * @return
	 */
	public DescriptorTreeItem getParent() {
		return parent;
	}
	
	/**
	 * Get the children of this node
	 * @return
	 */
	public ArrayList<DescriptorTreeItem> getChildren() {
		return children;
	}
	
	/**
	 * Set the parent of the tree node
	 * @param parent
	 */
	public void setParent(DescriptorTreeItem parent) {
		this.parent = parent;
	}
	
	/**
	 * Set the children of the tree node
	 * @param children
	 */
	public void setChildren(ArrayList<DescriptorTreeItem> children) {
		this.children = children;
	}
	
	/**
	 * Add a child to the children of the node
	 * @param child
	 */
	public void addChild( DescriptorTreeItem child ) {
		this.children.add( child );
	}
	
	/**
	 * Get if the node is a leaf of the tree
	 * @return
	 */
	public boolean isLeaf() {
		return children.isEmpty();
	}
	
	/**
	 * Is this node inherited from a parent?
	 * @return
	 */
	public boolean isInherited() {
		return inherited;
	}
	/**
	 * Two nodes are equal if same term contained in the node
	 */
	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof DescriptorTreeItem )
			return term.equals( ( (DescriptorTreeItem) obj ).getTerm() );
		return false;
	}
}
