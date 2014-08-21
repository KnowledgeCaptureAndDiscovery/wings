package edu.isi.wings.catalog.data.classes;

import java.util.ArrayList;

public class DataTreeNode {
	DataItem item;
	private ArrayList<DataTreeNode> children;

	public DataTreeNode(DataItem item) {
		this.item = item;
		this.children = new ArrayList<DataTreeNode>();
	}

	public DataItem getItem() {
		return item;
	}

	public void setItem(DataItem item) {
		this.item = item;
	}

	public ArrayList<DataTreeNode> getChildren() {
		return this.children;
	}

	public void addChild(DataTreeNode node) {
		this.children.add(node);
	}

	public void removeChild(DataTreeNode node) {
		this.children.remove(node);
	}
	
  public boolean hasChild(DataTreeNode searchnode, boolean direct) {
    if(searchnode == null)
      return false;
    for (DataTreeNode childnode : this.children) {
      if (childnode == searchnode)
        return true;
      if (!direct && childnode.hasChild(searchnode, direct))
        return true;
    }
    return false;
  }
}
