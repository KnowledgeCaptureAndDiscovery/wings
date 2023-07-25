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

package edu.isi.wings.workflow.template.classes.sets;

import java.util.ArrayList;

public class PortBindingList extends ArrayList<PortBindingList> {

  private static final long serialVersionUID = 1L;

  PortBinding pb;

  public PortBindingList() {
    super();
  }

  public PortBindingList(PortBinding pb) {
    super();
    // this.addPortBinding(pb);
    this.pb = pb;
  }

  // public void addPortBinding(PortBinding pb) {
  // if(this.pb == null)
  // this.pb = pb;
  // else {
  // this.add(new PortBindingList(this.pb));
  // this.add(new PortBindingList(pb));
  // this.pb = null;
  // }
  // }

  public boolean isList() {
    return (!isEmpty());
  }

  public PortBinding getPortBinding() {
    return this.pb;
  }

  public String toString() {
    if (pb != null) return pb.toString(); else return super.toString();
  }
}
