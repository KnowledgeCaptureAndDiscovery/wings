/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.isi.wings.catalog.data.classes;

import java.util.ArrayList;
import java.util.HashMap;

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
      if (node.getItem().getID().equals(nodeid)) return node;
      queue.addAll(node.getChildren());
    }
    return null;
  }

  public HashMap<String, String> getParents() {
    HashMap<String, String> parents = new HashMap<String, String>();
    ArrayList<DataTreeNode> queue = new ArrayList<DataTreeNode>();
    queue.add(root);
    while (!queue.isEmpty()) {
      DataTreeNode node = queue.remove(0);
      for (DataTreeNode cnode : node.getChildren()) {
        parents.put(cnode.getItem().getID(), node.getItem().getID());
        queue.add(cnode);
      }
    }
    return parents;
  }
}
