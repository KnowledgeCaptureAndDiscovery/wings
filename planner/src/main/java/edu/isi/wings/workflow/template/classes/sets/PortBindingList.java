package edu.isi.wings.workflow.template.classes.sets;

import java.util.ArrayList;

public class PortBindingList extends ArrayList<PortBindingList> {
	private static final long serialVersionUID = 1L;

	PortBinding pb;

	public PortBindingList() {
		super();
	}

	public PortBindingList(PortBinding pb) {
		super();
		// this.addPortBinding(pb);
		this.pb = pb;
	}

	// public void addPortBinding(PortBinding pb) {
	// if(this.pb == null)
	// this.pb = pb;
	// else {
	// this.add(new PortBindingList(this.pb));
	// this.add(new PortBindingList(pb));
	// this.pb = null;
	// }
	// }

	public boolean isList() {
		return (!isEmpty());
	}

	public PortBinding getPortBinding() {
		return this.pb;
	}

	public String toString() {
		if (pb != null)
			return pb.toString();
		else
			return super.toString();
	}
}
