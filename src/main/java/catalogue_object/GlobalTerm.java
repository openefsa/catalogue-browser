package catalogue_object;

public class GlobalTerm implements Nameable {

	public static GlobalTerm Root = new GlobalTerm( "Root" );
	public static GlobalTerm AllTerms = new GlobalTerm( "All terms" );
	public static GlobalTerm AllFacets = new GlobalTerm( "Facets" );
	public static GlobalTerm AllHierarchies = new GlobalTerm( "Hierarchies" );
	
	String	name;
	Nameable parent;

	public GlobalTerm( String name ) {
		this.name = name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getLabel() {
		return name;
	}
}
