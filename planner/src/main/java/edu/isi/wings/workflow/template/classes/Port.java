package edu.isi.wings.workflow.template.classes;

import edu.isi.wings.common.URIEntity;

public class Port extends URIEntity {
	private static final long serialVersionUID = 1L;

	private Role role;

	public Port(String id) {
		super(id);
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
}
