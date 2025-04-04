package ui_implicit_facet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import catalogue.Catalogue;
import catalogue_object.Attribute;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import shared_data.SharedDataContainer;

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
			Term tempRoot = term;//new Term(term.getCatalogue(), term.getId(), term.getCode(), term.getName(), term.getLabel(), term.getScopenotes(), term.getStatus(), term.getVersion(), term.getLastUpdate(), term.getValidFrom(), term.getValidTo(), term.isDeprecated());

			ArrayList<FacetDescriptor> tempImplicit = (ArrayList<FacetDescriptor>) temp.getImplicitFacets().clone();
			ArrayList<TermAttribute> tempAttribute = (ArrayList<TermAttribute>) temp.getAttributes().clone();

			//ArrayList<FacetDescriptor> tempImplicitAdjusted = (ArrayList<FacetDescriptor>) tempImplicit.clone();
			//ArrayList<TermAttribute> tempAttributeAdjusted = (ArrayList<TermAttribute>) tempAttribute.clone();
			
			for (FacetDescriptor x : term.getImplicitFacets()) {
				//(!tempImplicit.stream().anyMatch(z -> temp.getCatalogue().getTermByCode(x.getFacetCode()).hasAncestor(tempRoot, null) (temp.getCatalogue().getTermByCode(z.getFacetCode()), SharedDataContainer.currentHierarchy)))
				Term termToAnalyze = temp.getCatalogue().getTermByCode(x.getFacetCode());
				List<String> termParentsCodes = termToAnalyze.getAncestors(termToAnalyze, SharedDataContainer.currentHierarchy).stream().map(z -> z.getCode()).collect(Collectors.toList());
				
				if (!tempImplicit.contains(x))
				{
					if (tempImplicit.stream().anyMatch(z -> termParentsCodes.contains(z.getFacetCode())))
					{
						tempImplicit = tempImplicit.stream().filter(z -> !(termParentsCodes.contains(z.getFacetCode()) && z.getFacetHeader().equals(x.getFacetHeader()))).collect(Collectors.toCollection(ArrayList::new));
						tempAttribute = tempAttribute.stream().filter(z -> !(z.getValue().contains(x.getFacetCode()))).collect(Collectors.toCollection(ArrayList::new));
					}
					tempImplicit.add(x);
				}
			}

			for (TermAttribute x : term.getAttributes()) {
				//if (!tempAttribute.contains(x)) {
					tempAttribute.add(x);
				//}
			}
			
			//tempImplicit.addAll(term.getImplicitFacets());
			//tempAttribute.addAll(tempRoot.getAttributes());
			
			//tempImplicit = tempImplicit.stream().map(x -> x.).collect(Collectors.toCollection(ArrayList::new)); //tempImplicit.stream().flatMap(x -> temp.getCatalogue().getTermByCode(x.getFacetCode()).getDescriptorsByCategory(x.getAttribute(), false).stream()).collect(Collectors.toCollection(ArrayList::new));
			
			tempRoot.setImplicitFacets(tempImplicit);
			tempRoot.setTermAttributes(tempAttribute);
			tempRoot.setApplicabilities(temp.getApplicabilities());
			tempRoot.setTermType(temp.getTermType());
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
