package edu.isi.wings.workflow.template.classes.sets;

import java.io.Serializable;

public class SetCreationRule implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum SetType {
		WTYPE, STYPE
	};

	private SetType type;

	public SetCreationRule(SetType type) {
		this.type = type;
	}

	public SetType getType() {
		return this.type;
	}

	public void setType(SetType type) {
		this.type = type;
	}
}
