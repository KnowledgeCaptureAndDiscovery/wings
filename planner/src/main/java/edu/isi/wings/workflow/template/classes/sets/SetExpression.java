package edu.isi.wings.workflow.template.classes.sets;

import java.util.ArrayList;

import edu.isi.wings.workflow.template.classes.Port;

public class SetExpression extends ArrayList<SetExpression> {
	private static final long serialVersionUID = 1L;
	protected Port port;

	public enum SetOperator {
		XPRODUCT, NWISE, INCREASEDIM, REDUCEDIM, SHIFT
	};

	private SetOperator op;

	public SetExpression(SetOperator op) {
		super();
		this.op = op;
	}

	public SetExpression(SetOperator op, SetExpression s) {
		this.add(s);
		this.op = op;
	}

	public SetExpression(SetOperator op, Port obj) {
		this.port = obj;
		this.op = op;
	}

	public SetExpression(SetOperator op, Port[] objs) {
		this.op = op;
		for (Port p : objs) {
			this.add(new SetExpression(op, p));
		}
	}

	public SetOperator getOperator() {
		return this.op;
	}

	public void setOperator(SetOperator op) {
		this.op = op;
	}

	public boolean isSet() {
		if (!isEmpty())
			return true;
		return false;
	}

	public int hashCode() {
		if (isSet())
			return super.hashCode();
		else if (port != null)
			return port.hashCode();
		return 0;
	}

	public Port getPort() {
		return this.port;
	}

	/*
	 * public String toString() { if (isSet()) return (op==SetOperator.XPRODUCT
	 * ? "( x: ": "( ||: ")+super.toString()+" )"; else return port.getName(); }
	 */

	public String toString() {
		if (!isSet())
			return port.getName();
		int i = 0;
		String str = (op == SetOperator.SHIFT ? "Shift" : "");
		str += (op == SetOperator.INCREASEDIM ? "[ " : (op == SetOperator.REDUCEDIM ? "/" : "( "));
		for (SetExpression s : this) {
			if (i > 0)
				str += (op == SetOperator.XPRODUCT ? " x " : (op == SetOperator.NWISE ? " || "
						: " "));
			str += s.toString();
			i++;
		}
		str += (op == SetOperator.INCREASEDIM ? "]" : (op == SetOperator.REDUCEDIM ? "/" : ")"));
		return str;
	}
}
