package data_collection;

import java.util.ArrayList;

public class DCTableList extends ArrayList<DCTable> implements IDcfDCTableLists<DCTable> {
	private static final long serialVersionUID = 5513941494969774696L;

	@Override
	public DCTable create() {
		return new DCTable();
	}

	@Override
	public IDcfCatalogueConfig createConfig() {
		return new CatalogueConfiguration();
	}

}
