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
    if (searchnode == null) return false;
    for (DataTreeNode childnode : this.children) {
      if (childnode == searchnode) return true;
      if (!direct && childnode.hasChild(searchnode, direct)) return true;
    }
    return false;
  }
}
