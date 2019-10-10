package data_collection;

import java.util.ArrayList;

public class DataCollectionsList extends ArrayList<DataCollection> implements IDcfDataCollectionsList<DataCollection> {

	private static final long serialVersionUID = 6500446530133655903L;

	@Override
	public DataCollection create() {
		return new DataCollection();
	}

}
