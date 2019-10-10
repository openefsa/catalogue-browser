package catalogue;

import java.util.ArrayList;

import dcf_manager.Dcf;

public class CataloguesList extends ArrayList<Catalogue> implements IDcfCataloguesList<Catalogue> {

	private static final long serialVersionUID = -8229407671007640454L;

	@Override
	public Catalogue create() {
		Catalogue cat = new Catalogue();
		cat.setCatalogueType(Dcf.dcfType);
		return cat;
	}
}
