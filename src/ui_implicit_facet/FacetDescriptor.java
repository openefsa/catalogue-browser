package ui_implicit_facet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue_object.Attribute;
import catalogue_object.Term;
import catalogue_object.TermAttribute;

/**
 * This class specify more the term attribute class (which is an attribute with its value associated to a term)
 * In particular, we define the term attribute of type facet descriptor which is used when we have implicit facets.
 * We define the concept of facet code and header.
 * @author avonva
 *
 */
public class FacetDescriptor {
	
	private static final Logger LOGGER = LogManager.getLogger(FacetDescriptor.class);
	
	private Term descriptor;
	private String header = null;
	private String facetCode = null;
	private FacetType facetType;
	
	private TermAttribute termAttribute;
	
	public FacetDescriptor( Term descriptor, TermAttribute termAttribute, FacetType facetType ) {
		
		// pass as value the full code of the term
		this.termAttribute = termAttribute;
		this.descriptor = descriptor;
		this.facetType = facetType;
		
		header = getFacetHeader();
		facetCode = getFacetCode();
	}
	
	
	/**
	 * get the term which is the facet descriptor
	 * of the base term related to this class
	 * @return
	 */
	public Term getDescriptor() {
		return descriptor;
	}
	
	/**
	 * Get the term attribute related to this
	 * facet descriptor, that is, a term attribute
	 * which has as attribute the implicit facets
	 * attribute, as term the base term and as
	 * value the facet full code (facetheader.facetcode)
	 * @return
	 */
	public TermAttribute getTermAttribute() {
		return termAttribute;
	}
	
	/**
	 * Get the base term related to this descriptor
	 * @return
	 */
	public Term getTerm() {
		return termAttribute.getTerm();
	}
	
	/**
	 * Get the value of the descriptor, that is,
	 * full facet code (facetheader.facetcode)
	 * @return
	 */
	public String getValue() {
		return termAttribute.getValue();
	}
	
	/**
	 * Get the attribute, this should be always
	 * be the implicit facets attribute
	 * @return
	 */
	public Attribute getAttribute() {
		return termAttribute.getAttribute();
	}
	
	/**
	 * Get the facet header (e.g. F01)
	 * @return
	 */
	public String getFacetHeader () {
		
		try {
			return splitValue()[0];
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Cannot get facet header for facet=" + facetCode, e);
			return null;
		}
	}
	
	/**
	 * Get the facet code (e.g. A040T)
	 * @return
	 */
	public String getFacetCode () {
		
		try {
			return splitValue()[1];
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get the facet type, it is implicit or explicit?
	 * @return
	 */
	public FacetType getFacetType() {
		return facetType;
	}
	
	/**
	 * The attribute for the facet descriptor is the catalogue attribute
	 * related to the facet (otherwise we will get as attribute for the
	 * descriptors always the "implicitFacets" dcf attribute)
	 */
	public Attribute getFacetCategory() {

		// search for the correct attribute
		for ( Attribute attr : descriptor.getCatalogue().getFacetCategories() ) {
			if ( attr.getCode().equals( header ) ) {
				return attr;
			}
		}
		return null;
	}
	
	
	/**
	 * Split the descriptor attribtue value in header and code
	 * @return
	 * @throws Exception
	 */
	private String[] splitValue () throws Exception {
		
		// split the descriptor code ( e.g. F01.A040T in F01 and A040T )
		String[] split = termAttribute.getValue().split( "\\." );
		
		// if we have not two elements there is an error
		if ( split.length != 2 )
			throw new Exception ( "Wrong facet descriptor code format. It should be Fxx.yyyyy! Found: " + termAttribute.getValue() );
		
		return split;
	}
	
	/**
	 * Create the facet descriptor code format starting from the facet category and the term descriptor
	 * The code of implicit facets attributes is the F01, F02...
	 * @param header
	 * @param code
	 * @return
	 */
	public static String getFullFacetCode ( Term descriptor, Attribute facet ) {
		return facet.getCode() + "." + descriptor.getCode();
	}
	
	/**
	 * Get the full code of the facet descriptor
	 * @return
	 */
	public String getFullFacetCode () {
		return header + "." + facetCode;
	}
	
	@Override
	public String toString() {
		return "FACET DESCRIPTOR: header " + header + " code " + facetCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		// if facet descriptor check header and facet code
		if ( obj instanceof FacetDescriptor ) {
			
			FacetDescriptor fd = (FacetDescriptor) obj;

			return header.equals( fd.getFacetHeader() ) && facetCode.equals( fd.getFacetCode() );
		}
		
		// otherwise default
		return super.equals(obj);
		
	}
	
}
