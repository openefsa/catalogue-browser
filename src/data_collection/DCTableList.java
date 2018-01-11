package data_collection;

import java.util.ArrayList;

public class DCTableList extends ArrayList<DCTable> implements IDcfDCTableLists {
	private static final long serialVersionUID = 5513941494969774696L;

	@Override
	public boolean addElem(IDcfDCTable elem) {
		return super.add((DCTable) elem);
	}

	@Override
	public IDcfDCTable create() {
		return new DCTable();
	}

	@Override
	public IDcfCatalogueConfig createConfig() {
		return new CatalogueConfiguration();
	}

}
