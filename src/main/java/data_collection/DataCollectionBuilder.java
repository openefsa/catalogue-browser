package data_collection;

import java.sql.Timestamp;

public class DataCollectionBuilder {

	private String code;
	private String description;
	private String category;
	private Timestamp activeFrom;
	private Timestamp activeTo;
	private String resourceId;
	
	public void setCode(String code) {
		this.code = code;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public void setActiveFrom(Timestamp activeFrom) {
		this.activeFrom = activeFrom;
	}
	public void setActiveTo(Timestamp activeTo) {
		this.activeTo = activeTo;
	}
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	
	public DataCollection build() {
		return new DataCollection( code, description, category, 
				activeFrom, activeTo, resourceId );
	}
}
