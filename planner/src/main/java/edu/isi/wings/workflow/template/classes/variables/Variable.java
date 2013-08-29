package edu.isi.wings.workflow.template.classes.variables;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.workflow.template.classes.sets.Binding;

public class Variable extends URIEntity {
	private static final long serialVersionUID = 1L;

	private Binding binding;
	private String comment;
	private short type;

	public Variable(String id, short type) {
		super(id);
		this.type = type;
	}

	public void setVariableType(short type) {
		this.type = type;
	}

	public short getVariableType() {
		return this.type;
	}

	public boolean isDataVariable() {
		return type == VariableType.DATA;
	}

	public boolean isParameterVariable() {
		return type == VariableType.PARAM;
	}

	public boolean isComponentVariable() {
		return type == VariableType.COMPONENT;
	}

	public Binding getBinding() {
		return this.binding;
	}

	public void setBinding(Binding binding) {
		this.binding = binding;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String str) {
		this.comment = str;
	}

	public String toString() {
		return getID() + (binding != null ? " (" + binding.toString() + ")" : "");
	}
}
