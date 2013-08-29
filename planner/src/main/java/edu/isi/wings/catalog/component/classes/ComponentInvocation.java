package edu.isi.wings.catalog.component.classes;

import java.util.ArrayList;

public class ComponentInvocation {
	String componentId;
	String componentLocation;
	ArrayList<Argument> arguments;

	public ComponentInvocation() {
		this.arguments = new ArrayList<Argument>();
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getComponentLocation() {
		return componentLocation;
	}

	public void setComponentLocation(String componentLocation) {
		this.componentLocation = componentLocation;
	}

	public ArrayList<Argument> getArguments() {
		return arguments;
	}

	public void setArguments(ArrayList<Argument> arguments) {
		this.arguments = arguments;
	}

	public void addArgument(Argument arg) {
		this.arguments.add(arg);
	}

	public void addArgument(String name, Object value, String varid, boolean isInput) {
		this.arguments.add(new Argument(name, value, varid, isInput));
	}
	
	public class Argument {
		String name;
		String variableid;
		Object value;
		boolean isInput;

		public Argument(String name, Object value, String varid, boolean isInput) {
			this.name = name;
			this.value = value;
			this.variableid = varid;
			this.isInput = isInput;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public boolean isInput() {
			return isInput;
		}

		public void setInput(boolean isInput) {
			this.isInput = isInput;
		}

		public String getVariableid() {
		    return variableid;
		}

		public void setVariableid(String variableid) {
		    this.variableid = variableid;
		}
	}
}
