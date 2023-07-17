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
    return flatten(root);
  }

  public ArrayList<ComponentTreeNode> flatten(ComponentTreeNode top) {
    ArrayList<ComponentTreeNode> list = new ArrayList<ComponentTreeNode>();
    ArrayList<ComponentTreeNode> queue = new ArrayList<ComponentTreeNode>();
    queue.add(top);
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
      if (node.getCls().getID().equals(classid)) return node;
      queue.addAll(node.getChildren());
    }
    return null;
  }
}
