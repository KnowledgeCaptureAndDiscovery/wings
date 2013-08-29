package edu.isi.wings.catalog.data.classes;

public class MetadataValue {
	public final static int OBJECT = 1;
	public final static int DATATYPE = 2;

	String propertyId;
	Object value;
	int type;

	public MetadataValue(String propertyId, Object value, int type) {
		this.propertyId = propertyId;
		this.value = value;
		this.type = type;
	}

	public String getPropertyId() {
		return propertyId;
	}

	public void setPropertyId(String propertyId) {
		this.propertyId = propertyId;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
