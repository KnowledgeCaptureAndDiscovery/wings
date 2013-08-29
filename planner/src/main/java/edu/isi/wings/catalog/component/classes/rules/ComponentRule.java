package edu.isi.wings.catalog.component.classes.rules;

import java.util.ArrayList;

public class ComponentRule {
	public static enum Type {
		InversePrecondition, MetadataPropagation, ParameterConfiguration, CollectionConfiguration, Miscellaneous
	};

	String componentId;
	Type type;
	ArrayList<Precondition> preconditions;
	ArrayList<Effect> effects;

	public ComponentRule(String componentId) {
		this.componentId = componentId;
		this.preconditions = new ArrayList<Precondition>();
		this.effects = new ArrayList<Effect>();
	}
	
	public ComponentRule(String componentId, Type type) {
		this(componentId);
		this.type = type;
	}
	
	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	public String getComponentId() {
		return this.componentId;
	}
	
	public void setComponentId(String id) {
		this.componentId = id;
	}
	
}
