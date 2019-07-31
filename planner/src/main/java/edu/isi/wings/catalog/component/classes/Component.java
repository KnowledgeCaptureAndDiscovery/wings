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

import edu.isi.kcap.ontapi.rules.KBRule;
import edu.isi.kcap.ontapi.rules.KBRuleList;
import edu.isi.wings.catalog.component.classes.requirements.ComponentRequirement;
import edu.isi.wings.common.URIEntity;

public class Component extends URIEntity {
  private static final long serialVersionUID = 1L;

  public static int ABSTRACT = 1;
  public static int CONCRETE = 2;

  private String rulesText; // Not used directly, just for transferring info
                            // from Client
  
  int type;
  ArrayList<ComponentRole> inputs;
  ArrayList<ComponentRole> outputs;
  ArrayList<String> rules;
  ArrayList<String> inheritedRules;
  String location;
  String documentation;
  ComponentRequirement requirement;
  String componentType;

  public Component(String id, int type) {
    super(id);
    this.type = type;
    inputs = new ArrayList<ComponentRole>();
    outputs = new ArrayList<ComponentRole>();
    rules = new ArrayList<String>();
    inheritedRules = new ArrayList<String>();
    requirement = new ComponentRequirement();
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }
  
  public String getRulesText() {
    return rulesText;
  }

  public ArrayList<ComponentRole> getInputs() {
    return inputs;
  }

  public void addInput(ComponentRole input) {
    this.inputs.add(input);
  }

  public ArrayList<ComponentRole> getOutputs() {
    return outputs;
  }

  public void addOutput(ComponentRole output) {
    this.outputs.add(output);
  }

  public ArrayList<String> getRules() {
    return rules;
  }

  public void setRules(KBRuleList rules) {
    for (KBRule rule : rules.getRules()) {
      this.rules.add(rule.toShortString());
    }
  }

  public ArrayList<String> getInheritedRules() {
    return this.inheritedRules;
  }
  
  public void setInheritedRules(KBRuleList rules) {
    for (KBRule rule : rules.getRules()) {
      this.inheritedRules.add(rule.toShortString());
    }
  }
  
  public boolean hasRules() {
    return (this.rules.size() + this.inheritedRules.size()) > 0;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getDocumentation() {
    return documentation;
  }

  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public ComponentRequirement getComponentRequirement() {
    return requirement;
  }

  public void setComponentRequirement(ComponentRequirement componentRequirement) {
    this.requirement = componentRequirement;
  }

  public String getComponentType() {
    return componentType;
  }

  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }
}
