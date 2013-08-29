package edu.isi.wings.catalog.component.classes;

import java.util.ArrayList;

public class ComponentTree {
	ComponentTreeNode root;

	public ComponentTree(ComponentTreeNode root) {
		this.root = root;
	}

	public ComponentTreeNode getRoot() {
		return root;
	}

	public void setRoot(ComponentTreeNode root) {
		this.root = root;
	}

	public ArrayList<ComponentTreeNode> flatten() {
		ArrayList<ComponentTreeNode> list = new ArrayList<ComponentTreeNode>();
		ArrayList<ComponentTreeNode> queue = new ArrayList<ComponentTreeNode>();
		queue.add(root);
		while (!queue.isEmpty()) {
			ComponentTreeNode node = queue.remove(0);
			list.add(node);
			queue.addAll(node.getChildren());
		}
		return list;
	}

	public ComponentTreeNode findClass(String classid) {
		ArrayList<ComponentTreeNode> queue = new ArrayList<ComponentTreeNode>();
		queue.add(root);
		while (!queue.isEmpty()) {
			ComponentTreeNode node = queue.remove(0);
			if (node.getCls().getID().equals(classid))
				return node;
			queue.addAll(node.getChildren());
		}
		return null;
	}
}
