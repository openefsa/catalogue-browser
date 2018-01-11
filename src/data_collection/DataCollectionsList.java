package data_collection;

import java.util.ArrayList;

public class DataCollectionsList extends ArrayList<DataCollection> implements IDcfDataCollectionsList {

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
