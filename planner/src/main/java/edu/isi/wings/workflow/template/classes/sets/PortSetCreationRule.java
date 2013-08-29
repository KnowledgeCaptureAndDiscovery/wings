package edu.isi.wings.workflow.template.classes.sets;

public class PortSetCreationRule extends SetCreationRule {
	private static final long serialVersionUID = 1L;
	private SetExpression expr;

	public PortSetCreationRule(SetType type, SetExpression expr) {
		super(type);
		this.expr = expr;
	}

	public SetExpression getSetExpression() {
		return this.expr;
	}

	public String toString() {
		return (this.getType() == SetType.STYPE ? "S[" : "W[") + expr + "]";
	}
}
