package edu.isi.wings.catalog.data.classes;

import java.util.ArrayList;

public class DataTree {
	DataTreeNode root;

	public DataTree(DataTreeNode root) {
		this.root = root;
	}

	public DataTreeNode getRoot() {
		return root;
	}

	public void setRoot(DataTreeNode root) {
		this.root = root;
	}

	public ArrayList<DataTreeNode> flatten() {
		ArrayList<DataTreeNode> list = new ArrayList<DataTreeNode>();
		ArrayList<DataTreeNode> queue = new ArrayList<DataTreeNode>();
		queue.add(root);
		while (!queue.isEmpty()) {
			DataTreeNode node = queue.remove(0);
			list.add(node);
			queue.addAll(node.getChildren());
		}
		return list;
	}

	public DataTreeNode findNode(String nodeid) {
		ArrayList<DataTreeNode> queue = new ArrayList<DataTreeNode>();
		queue.add(root);
		while (!queue.isEmpty()) {
			DataTreeNode node = queue.remove(0);
			if (node.getItem().getID().equals(nodeid))
				return node;
			queue.addAll(node.getChildren());
		}
		return null;
	}
}
