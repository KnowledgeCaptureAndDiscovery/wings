package edu.isi.wings.workflow.template.classes;

import edu.isi.wings.common.URIEntity;

public class Role extends URIEntity {
	private static final long serialVersionUID = 1L;
	public static int PARAMETER = 1;
	public static int DATA = 2;

	int type;
	private String roleid;
	private int dimensionality = 0;

	public Role(String id) {
		super(id);
		this.type = DATA;
	}

	public String toString() {
		return getID();
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

	public void setDimensionality(int dim) {
		this.dimensionality = dim;
	}

	public int getDimensionality() {
		return this.dimensionality;
	}

	public String getRoleId() {
		return roleid;
	}

	public void setRoleId(String roleid) {
		this.roleid = roleid;
	}
}
