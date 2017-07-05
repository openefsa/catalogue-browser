package data_collection;

import java.util.ArrayList;
import java.util.Collection;

public class DCTableBuilder {

	private String name;
	private Collection<CatalogueConfiguration> configs;
	
	public DCTableBuilder() {
		configs = new ArrayList<>();
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void addConfig(CatalogueConfiguration config ) {
		this.configs.add( config );
	}
	
	public DCTable build() {
		DCTable table = new DCTable( name );
		table.setConfigs( configs );
		return table;
	}
}
