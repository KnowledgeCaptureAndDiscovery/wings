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

import edu.isi.wings.common.URIEntity;

public class ComponentRole extends URIEntity {

  private static final long serialVersionUID = 1L;

  String role;
  String prefix;
  boolean isParam;
  String type;
  int dimensionality = 0;
  Object paramDefaultValue;

  public ComponentRole(String id) {
    super(id);
  }

  public String getRoleName() {
    return role;
  }

  public void setRoleName(String role) {
    this.role = role;
  }

  public String getType() {
    return type;
  }

  public void setType(String typeid) {
    this.type = typeid;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public int getDimensionality() {
    return dimensionality;
  }

  public void setDimensionality(int dimensionality) {
    this.dimensionality = dimensionality;
  }

  public boolean isParam() {
    return isParam;
  }

  public void setParam(boolean isParam) {
    this.isParam = isParam;
  }

  public Object getParamDefaultalue() {
    return paramDefaultValue;
  }

  public void setParamDefaultalue(Object paramDefaultalue) {
    this.paramDefaultValue = paramDefaultalue;
  }
}
