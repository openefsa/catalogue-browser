package already_described_terms;

import java.util.ArrayList;
import java.util.StringTokenizer;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import global_manager.GlobalManager;
import ui_implicit_facet.FacetDescriptor;
import ui_implicit_facet.FacetType;

/**
 * Base class for a term which was already described before (code is a full code with zero or more facets)
 * @author avonva
 *
 */
public class DescribedTerm {

	private String code, label;

	// the code is a full-code baseTerm#facetHeader.facetCode$...
	public DescribedTerm( String code, String label ) {
		this.code = code;
		this.label = label;
	}

	public String getCode() {
		return code;
	}
	public String getLabel() {
		return label;
	}
	
	public String getBaseTermCode() {
		return code.split("#")[0];
	}
	
	public String getFacetsCodes() {
		return code.split("#")[1];
	}
	
	/**
	 * Get all the facets header and codes (F01.A07T2...)
	 * @return
	 */
	public ArrayList<String> getFullFacetCodes() {
		
		ArrayList<String> facetCodes = new ArrayList<>();
		
		// remove the base term
		String[] values = code.split("#");
		
		// return if no facets
		if ( values.length < 2 )
			return facetCodes;
		
		// parse the facet codes
		StringTokenizer st = new StringTokenizer ( values[1], "$" );
		
		while ( st.hasMoreTokens() )
			facetCodes.add( st.nextToken() );
		
		return facetCodes;
	}
	
	/**
	 * Get the base term of the recent term
	 * @return
	 */
	public Term getBaseTerm() {
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		TermDAO termDao = new TermDAO( currentCat );
		
		return termDao.getByCode( getBaseTermCode() );
	}
	
	/**
	 * Get the term that is referred by this described term
	 * @return
	 */
	public Term getTerm() {
		
		Term baseTerm = getBaseTerm();
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		// create the base term copy (to avoid ovverriding things! If we act directly to the base term
		// its implicit facets will be changed and we do not want that
		// we just need name and code, then we add the implicit facets.
		Term baseTermCopy = new Term( currentCat );
		baseTermCopy.setName( baseTerm.getName() );
		baseTermCopy.setShortName( baseTerm.getShortName() );
		baseTermCopy.setCode( baseTerm.getCode() );
		
		// initialize dao of attributes
		AttributeDAO attrDao = new AttributeDAO( currentCat );

		// for each explicit facet, we add the facet to the base term
		for ( String facetFullCode : getFullFacetCodes() ) {

			// get the facet header in order to retrieve the attribute
			// related to the current facet
			String facetHeader = facetFullCode.split( "\\." )[0];
			
			// create the term attribute related to the facet descriptor
			TermAttribute ta = new TermAttribute( baseTermCopy,
					attrDao.getByCode( facetHeader ), 
					facetFullCode );
			
			// add the descriptor to the term facet as explicit facet
			baseTermCopy.addImplicitFacet( new FacetDescriptor( baseTermCopy, ta, FacetType.EXPLICIT) );
		}
		
		return baseTermCopy;
	}
	
	
	/**
	 * Check if the term "term" is contained in the implicit or explicit facets of the described term
	 * @param term
	 * @return
	 */
	public boolean contains ( Term term ) {
		
		// check if the term is in the explicit facets or it is the base term
		boolean inExplicit = getCode().contains( term.getCode() );
		
		boolean inImplicit = false;
		Term baseTerm = getBaseTerm();
		
		// search if it is in the implicit facets, if they contain the term return true
		for ( FacetDescriptor fd : baseTerm.getImplicitFacets() ) {
			
			if ( fd.getTerm().equals ( baseTerm ) ) {
				inImplicit = true;
				break;
			}
		}
		
		return inExplicit || inImplicit;
	}
	
	
	
	@Override
	public String toString() {
		return label;
	}
}
