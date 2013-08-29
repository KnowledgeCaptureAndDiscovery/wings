package edu.isi.wings.catalog.data.classes;

import java.util.ArrayList;

import edu.isi.wings.common.URIEntity;

public class MetadataProperty extends URIEntity {
	private static final long serialVersionUID = 1L;

	int type;
	ArrayList<String> domains;
	String range;

	public static int DATATYPE = 1;
	public static int OBJECT = 2;

	public MetadataProperty(String id, int type) {
		super(id);
		this.type = type;
		this.domains = new ArrayList<String>();
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

	public boolean isDatatypeProperty() {
		if (this.type == DATATYPE)
			return true;
		return false;
	}

	public boolean isObjectProperty() {
		if (this.type == OBJECT)
			return true;
		return false;
	}

	public ArrayList<String> getDomains() {
		return this.domains;
	}

	public String getRange() {
		return this.range;
	}

	public void addDomain(String id) {
		this.domains.add(id);
	}

	public void setRange(String id) {
		this.range = id;
	}

	public String toString() {
		String str = "";
		str += "\n" + getName() + "(" + type + ")\nDomains:" + domains + "\nRange:" + range + "\n";
		return str;
	}
}
