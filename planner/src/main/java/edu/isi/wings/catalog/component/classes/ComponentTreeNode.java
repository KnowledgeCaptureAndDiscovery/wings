package edu.isi.wings.catalog.component.classes;

import java.util.ArrayList;

public class ComponentTreeNode {
	ComponentHolder cls;
	private ArrayList<ComponentTreeNode> children;

	public ComponentTreeNode(ComponentHolder cls) {
		this.cls = cls;
		this.children = new ArrayList<ComponentTreeNode>();
	}

	public ComponentHolder getCls() {
		return cls;
	}

	public void setCls(ComponentHolder cls) {
		this.cls = cls;
	}

	public ArrayList<ComponentTreeNode> getChildren() {
		return this.children;
	}

	public void addChild(ComponentTreeNode node) {
		this.children.add(node);
	}

	public void removeChild(ComponentTreeNode node) {
		this.children.remove(node);
	}
}
