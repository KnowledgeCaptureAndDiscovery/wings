package edu.isi.wings.catalog.component.classes;

import edu.isi.wings.common.URIEntity;

public class ComponentRole extends URIEntity {
	private static final long serialVersionUID = 1L;

	String role;
	String prefix;
	boolean isParam;
	String type;
	int dimensionality = 0;
	Object paramDefaultValue;

	public ComponentRole(String id) {
		super(id);
	}

	public String getRoleName() {
		return role;
	}

	public void setRoleName(String role) {
		this.role = role;
	}

	public String getType() {
		return type;
	}

	public void setType(String typeid) {
		this.type = typeid;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public int getDimensionality() {
		return dimensionality;
	}

	public void setDimensionality(int dimensionality) {
		this.dimensionality = dimensionality;
	}

	public boolean isParam() {
		return isParam;
	}

	public void setParam(boolean isParam) {
		this.isParam = isParam;
	}

	public Object getParamDefaultalue() {
		return paramDefaultValue;
	}

	public void setParamDefaultalue(Object paramDefaultalue) {
		this.paramDefaultValue = paramDefaultalue;
	}
}
