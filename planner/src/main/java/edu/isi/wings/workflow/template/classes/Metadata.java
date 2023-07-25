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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Metadata implements Serializable {

  private static final long serialVersionUID = 1L;
  public Date lastUpdateTime;
  public String tellme;
  public String documentation;
  public ArrayList<String> contributors = new ArrayList<String>();
  public ArrayList<String> createdFrom = new ArrayList<String>();

  public Metadata() {}

  /*
   * Metadata Properties
   */
  public void addContributor(String username) {
    if (
      username != null && !this.contributors.contains(username)
    ) this.contributors.add(username);
  }

  public ArrayList<String> getContributors() {
    return this.contributors;
  }

  public void setLastUpdateTime() {
    this.lastUpdateTime = new Date();
  }

  public void setLastUpdateTime(Date datetime) {
    this.lastUpdateTime = datetime;
  }

  public Date getLastUpdateTime() {
    return this.lastUpdateTime;
  }

  public void setDocumentation(String doc) {
    this.documentation = doc;
  }

  public String getDocumentation() {
    return this.documentation;
  }

  public void addCreationSource(String name) {
    if (!this.createdFrom.contains(name)) this.createdFrom.add(name);
  }

  public ArrayList<String> getCreationSources() {
    return this.createdFrom;
  }

  public String getTellme() {
    return tellme;
  }

  public void setTellme(String tellme) {
    this.tellme = tellme;
  }
}
