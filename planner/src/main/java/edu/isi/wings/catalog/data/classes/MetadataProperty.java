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

import edu.isi.wings.common.URIEntity;
import java.util.ArrayList;

public class MetadataProperty extends URIEntity {

  private static final long serialVersionUID = 1L;

  int type;
  ArrayList<String> domains;
  String range;

  public static int DATATYPE = 1;
  public static int OBJECT = 2;

  public MetadataProperty(String id, int type) {
    super(id);
    this.type = type;
    this.domains = new ArrayList<String>();
  }

  public void setType(int type) {
    this.type = type;
  }

  public int getType() {
    return this.type;
  }

  public boolean isDatatypeProperty() {
    if (this.type == DATATYPE) return true;
    return false;
  }

  public boolean isObjectProperty() {
    if (this.type == OBJECT) return true;
    return false;
  }

  public ArrayList<String> getDomains() {
    return this.domains;
  }

  public String getRange() {
    return this.range;
  }

  public void addDomain(String id) {
    this.domains.add(id);
  }

  public void setRange(String id) {
    this.range = id;
  }

  public String toString() {
    String str = "";
    str +=
    "\n" +
    getName() +
    "(" +
    type +
    ")\nDomains:" +
    domains +
    "\nRange:" +
    range +
    "\n";
    return str;
  }
}
