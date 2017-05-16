package catalogue_object;

public class TermBuilder extends CatalogueObjectBuilder {

	@Override
	public Term build() {
		return new Term( catalogue, -1, code, name, label, 
				scopenotes, status, version, lastUpdate, 
				validFrom, validTo, deprecated);
	}
}
