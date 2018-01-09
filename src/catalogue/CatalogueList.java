package catalogue;

import java.util.ArrayList;

import dcf_manager.Dcf;

public class CatalogueList extends ArrayList<Catalogue> implements IDcfCatalogueList {

	private static final long serialVersionUID = -8229407671007640454L;

	@Override
	public boolean addElem(IDcfCatalogue elem) {
		return super.add((Catalogue) elem);
	}

	@Override
	public IDcfCatalogue create() {
		Catalogue cat = new Catalogue();
		cat.setCatalogueType(Dcf.dcfType);
		return cat;
	}
}
