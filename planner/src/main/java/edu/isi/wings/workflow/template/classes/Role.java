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

package edu.isi.wings.workflow.template.classes;

import edu.isi.wings.common.URIEntity;

public class Role extends URIEntity {

  private static final long serialVersionUID = 1L;
  public static int DATA = 1;
  public static int PARAMETER = 2;

  int type;
  private String roleid;
  private int dimensionality = 0;

  public Role(String id) {
    super(id);
    this.type = DATA;
  }

  public String toString() {
    return getID();
  }

  public void setType(int type) {
    this.type = type;
  }

  public int getType() {
    return this.type;
  }

  public void setDimensionality(int dim) {
    this.dimensionality = dim;
  }

  public int getDimensionality() {
    return this.dimensionality;
  }

  public String getRoleId() {
    return roleid;
  }

  public void setRoleId(String roleid) {
    this.roleid = roleid;
  }
}
