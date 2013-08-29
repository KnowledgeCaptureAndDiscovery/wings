package edu.isi.wings.workflow.template.classes;

import java.util.Collection;
import java.util.HashMap;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.workflow.template.classes.sets.ComponentSetCreationRule;
import edu.isi.wings.workflow.template.classes.sets.PortSetCreationRule;
import edu.isi.wings.workflow.template.classes.variables.*;

public class Node extends URIEntity {
	private static final long serialVersionUID = 1L;
	private ComponentVariable componentVariable;

	private String comment;

	private HashMap<String, Port> inputPorts;
	private HashMap<String, Port> outputPorts;

	private PortSetCreationRule prule;
	private ComponentSetCreationRule crule;

	public Node(String id) {
		super(id);
		inputPorts = new HashMap<String, Port>();
		outputPorts = new HashMap<String, Port>();
	}

	public void setComponentVariable(ComponentVariable componentVariable) {
		this.componentVariable = componentVariable;
	}

	public ComponentVariable getComponentVariable() {
		return componentVariable;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String str) {
		this.comment = str;
	}

	public void addComponentSetRule(ComponentSetCreationRule crule) {
		this.crule = crule;
	}

	public ComponentSetCreationRule getComponentSetRule() {
		return this.crule;
	}

	public void addPortSetRule(PortSetCreationRule prule) {
		this.prule = prule;
	}

	public PortSetCreationRule getPortSetRule() {
		return this.prule;
	}

	public void setInputPorts(HashMap<String, Port> inputPorts) {
		this.inputPorts = inputPorts;
	}

	public void setOutputPorts(HashMap<String, Port> outputPorts) {
		this.outputPorts = outputPorts;
	}

	public void addInputPort(Port inputPort) {
		this.inputPorts.put(inputPort.getID(), inputPort);
	}

	public void addOutputPort(Port outputPort) {
		this.outputPorts.put(outputPort.getID(), outputPort);
	}

	public void deleteInputPort(Port port) {
		this.inputPorts.remove(port.getID());
	}

	public void deleteOutputPort(Port port) {
		this.outputPorts.remove(port.getID());
	}

	public Collection<Port> getInputPorts() {
		return this.inputPorts.values();
	}

	public Collection<Port> getOutputPorts() {
		return this.outputPorts.values();
	}

	public Port findInputPort(String id) {
		return inputPorts.get(id);
	}

	public Port findOutputPort(String id) {
		return outputPorts.get(id);
	}

	public String toString() {
		return this.getID();
	}
}
