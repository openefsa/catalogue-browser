package already_described_terms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import ui_implicit_facet.FacetDescriptor;
import ui_implicit_facet.FacetType;

/**
 * Base class for a term which was already described before (code is a full code with zero or more facets)
 * @author avonva
 *
 */
public class DescribedTerm {

	private Catalogue catalogue;
	private String code, label;

	// the code is a full-code baseTerm#facetHeader.facetCode$...
	public DescribedTerm( Catalogue catalogue, String code, String label ) {
		this.catalogue = catalogue;
		this.code = code;
		this.label = label;
	}

	/**
	 * Get the full code of the described term
	 * @return
	 */
	public String getCode() {
		return code;
	}
	public String getLabel() {
		return label;
	}
	
	/**
	 * Get the base term code of the described term
	 * @return
	 */
	public String getBaseTermCode() {
		
		String[] split = code.split("#");
		
		if ( split.length > 0 )
			return split[0];
		
		return null;
	}
	
	/**
	 * Get the facets full code of the described term
	 * @return
	 */
	public String getFacetsCodes() {
		
		String[] split = code.split("#");
		
		if ( split.length > 1 )
			return split[1];
		
		return null;
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
		return getTerm ( getBaseTermCode() );
	}
	
	/**
	 * Get a term from the database given its code
	 * @param code
	 * @return
	 */
	private Term getTerm ( String code ) {
		
		TermDAO termDao = new TermDAO( catalogue );
		return termDao.getByCode( code );
	}
	
	/**
	 * Get the term that is referred by this described term
	 * @return
	 */
	public Term getTerm() {
		
		Term baseTerm = getBaseTerm();
		
		// create the base term copy (to avoid ovverriding things! If we act directly to the base term
		// its implicit facets will be changed and we do not want that
		// we just need name and code, then we add the implicit facets.
		Term baseTermCopy = new Term( catalogue );
		baseTermCopy.setName( baseTerm.getName() );
		baseTermCopy.setFullCodeDescription( baseTerm.getShortName(false) );
		baseTermCopy.setCode( baseTerm.getCode() );
		
		// initialize dao of attributes
		AttributeDAO attrDao = new AttributeDAO( catalogue );

		// for each explicit facet, we add the facet to the base term
		for ( String facetFullCode : getFullFacetCodes() ) {

			// get the facet header in order to retrieve the attribute
			// related to the current facet
			String facetHeader = getFacetHeader( facetFullCode );
			
			// create the term attribute related to the facet descriptor
			TermAttribute ta = new TermAttribute( baseTermCopy,
					attrDao.getByCode( facetHeader ), 
					facetFullCode );
			
			String facetCode = getFacetCode ( facetFullCode );

			// get the descriptor term
			Term descriptor = getTerm( facetCode );

			// add the descriptor to the term facet as explicit facet
			baseTermCopy.addImplicitFacet( new FacetDescriptor( descriptor, ta, FacetType.EXPLICIT) );
		}

		return baseTermCopy;
	}
	
	
	/**
	 * Get the facet header from full code
	 * @param facetFullCode
	 * @return
	 */
	private String getFacetHeader( String facetFullCode ) {
		
		String[] split = facetFullCode.split( "\\." );
		
		if ( split.length < 1 )
			return null;
				
		// get the facet header in order to retrieve the attribute
		// related to the current facet
		return split[0];
	}
	
	/**
	 * Get the facet code from full code
	 * @param facetFullCode
	 * @return
	 */
	private String getFacetCode( String facetFullCode ) {
		
		String[] split = facetFullCode.split( "\\." );
		
		if ( split.length < 2 )
			return null;
				
		// get the facet code
		return split[1];
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
	
	/**
	 * Check if all the terms referred by this described term
	 * are present into the database or not.
	 * @return
	 */
	public boolean isValid () {

		// if invalid base term return false
		if ( getBaseTerm() == null )
			return false;
		
		// if invalid facets return false
		Collection<String> facets = getFullFacetCodes();
		for ( String fullFacetCode : facets ) {
			
			String facetCode = getFacetCode ( fullFacetCode );
			if ( getTerm( facetCode ) == null )
				return false;
		}
		
		return true;
	}
	
	
	@Override
	public String toString() {
		return label;
	}
}
