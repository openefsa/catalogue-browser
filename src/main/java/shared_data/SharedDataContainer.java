package shared_data;

import java.util.ArrayList;
import java.util.List;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;

public class SharedDataContainer {
	public static Hierarchy currentHierarchy;
	
	public static List<Hierarchy> facetsHierarchies;
	
	public static void updateFacetsHierarchies(Catalogue catalogue)
	{
		if (catalogue == null)
		{
			facetsHierarchies = new ArrayList<Hierarchy>();
		}
		else
		{
			facetsHierarchies = catalogue.getFacetHierarchies();	
		}
		
	}
}
