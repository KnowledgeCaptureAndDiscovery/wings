package edu.isi.wings.catalog.component.classes;

import edu.isi.wings.common.URIEntity;

/**
 * Component Holder Class
 * - Provides a hierarchical structure to the components
 */
public class ComponentHolder extends URIEntity {
	private static final long serialVersionUID = 1L;

	// NOTE: Only 1 component (or none) per component class
	Component component;

	public ComponentHolder(String id) {
		super(id);
	}

	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}

}
