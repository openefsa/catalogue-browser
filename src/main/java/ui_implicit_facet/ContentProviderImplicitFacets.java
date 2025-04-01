package ui_implicit_facet;
import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import catalogue.Catalogue;
import catalogue_object.Attribute;
import catalogue_object.Term;
import catalogue_object.TermAttribute;

/**
 * This class is a using name properties to display the facets.
 * 
 * @author thomm
 * 
 */

public class ContentProviderImplicitFacets implements ITreeContentProvider {

	private Term _rootTerm = null;

	public void dispose () {}

	// if the input changes we set as root term the new term
	public void inputChanged ( Viewer arg0 , Object oldTerm , Object newTerm ) {

		if ( newTerm instanceof Term ) {

			Term term = (Term) newTerm;
			Term temp = term.getCatalogue().getTermByCode(term.getCode());
			Term tempRoot = new Term(term.getCatalogue(), term.getId(), term.getCode(), term.getName(), term.getLabel(), term.getScopenotes(), term.getStatus(), term.getVersion(), term.getLastUpdate(), term.getValidFrom(), term.getValidTo(), term.isDeprecated());

			ArrayList<FacetDescriptor> tempImplicit = (ArrayList<FacetDescriptor>) temp.getImplicitFacets().clone();
			ArrayList<TermAttribute> tempAttribute = (ArrayList<TermAttribute>) temp.getAttributes().clone();

			for (FacetDescriptor x : term.getImplicitFacets()) {
				if (!tempImplicit.contains(x)) {
					tempImplicit.add(x);
				}
			}

			for (TermAttribute x : term.getAttributes()) {
				if (!tempAttribute.contains(x)) {
					tempAttribute.add(x);
				}
			}
			
			//tempImplicit.addAll(term.getImplicitFacets());
			//tempAttribute.addAll(tempRoot.getAttributes());
			tempRoot.setImplicitFacets(tempImplicit);
			tempRoot.setTermAttributes(tempAttribute);
			tempRoot.setApplicabilities(temp.getApplicabilities());

			/*
			 * for (FacetDescriptor x : tempRoot.getImplicitFacets()) {
			 * tempRoot.addImplicitFacet(x); }
			 */

			_rootTerm = tempRoot;
		}

	}

	// check if there are any children for the object arg0
	public boolean hasChildren ( Object arg0 ) {
		if ( arg0 != null ) {
			Object[] ch = getChildren( arg0 );
			if ( ch != null )
				return ( ch.length > 0 ) ? true : false;
		}
		return false;

	}

	/**
	 * Get the children of the descriptortreeitem arg0
	 */
	public Object[] getChildren ( Object arg0 ) {

		ArrayList< DescriptorTreeItem > ret = new ArrayList<>();
		ArrayList< DescriptorTreeItem > ret_new = new ArrayList<>();
		
		// if no root term we return (we cannot fetch facets from nothing)
		if ( _rootTerm == null )
			return null;

		// arg0 is the facet category, then we get the facets descriptors related to 
		// that category with the implicit facets tree
		if ( arg0 != null ) {
			if ( arg0 instanceof Attribute ) {
				Attribute facetCategory = (Attribute) arg0;
				ret = _rootTerm.getInheritedImplicitFacets( facetCategory );
				
				
			}
		}

		return ret.toArray();
	}

	public Object[] getElements ( Object arg0 ) {
		
		if ( _rootTerm == null )
			return null;
		
		// get the current catalogue
		Catalogue currentCat = _rootTerm.getCatalogue();
		
		// if the current catalogue does not have facets
		if ( currentCat == null )
			return null;
		
		// get all the categories of facets (the attributes)
		return currentCat.getInUseFacetCategories().toArray();
	}

	/**
	 * In this object I have to manage: - name properties containing the facet
	 * descriptors - term attributes defining the facets used
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object
	 *      )
	 */
	public Object getParent ( Object arg0 ) {

		if ( arg0 != null ) {

			if ( arg0 instanceof DescriptorTreeItem ) {
				/*
				 * for simplicity I do not show the entire hierarchy but only
				 * the descriptors, therefore the parent is the term attribute
				 * for with the term is specified
				 */
				DescriptorTreeItem child = (DescriptorTreeItem) arg0;
				return child.getParent();
			}
		}
		return null;
	}
}
