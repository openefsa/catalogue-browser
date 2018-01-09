package data_collection;

import java.util.ArrayList;

public class DataCollectionList extends ArrayList<DataCollection> implements IDcfDataCollectionList {

	private static final long serialVersionUID = 6500446530133655903L;

	@Override
	public boolean addElem(IDcfDataCollection elem) {
		return super.add((DataCollection) elem);
	}

	@Override
	public IDcfDataCollection create() {
		return new DataCollection();
	}

}
