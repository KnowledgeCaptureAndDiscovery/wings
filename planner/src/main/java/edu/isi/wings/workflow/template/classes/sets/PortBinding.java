package edu.isi.wings.workflow.template.classes.sets;

import java.util.HashMap;

import edu.isi.wings.workflow.template.classes.Port;

public class PortBinding extends HashMap<Port, Binding> {
	private static final long serialVersionUID = 1L;

	public PortBinding() {
	}

	public PortBinding(PortBinding b) {
		super(b);
	}

	public Binding getById(String portid) {
		for (Port p : this.keySet()) {
			if (p.getID().equals(portid)) {
				return this.get(p);
			}
		}
		return null;
	}
}