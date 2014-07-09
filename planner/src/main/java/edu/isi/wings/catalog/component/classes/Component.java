package edu.isi.wings.catalog.component.classes;

import java.util.ArrayList;

import edu.isi.wings.catalog.component.classes.requirements.ComponentRequirement;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.ontapi.rules.KBRule;
import edu.isi.wings.ontapi.rules.KBRuleList;

public class Component extends URIEntity {
  private static final long serialVersionUID = 1L;

  public static int ABSTRACT = 1;
  public static int CONCRETE = 2;

  private String rulesText; // Not used directly, just for transferring info
                            // from Client

  public String getRulesText() {
    return rulesText;
  }

  int type;
  ArrayList<ComponentRole> inputs;
  ArrayList<ComponentRole> outputs;
  ArrayList<String> rules;
  ArrayList<String> inheritedRules;
  String location;
  String documentation;
  ComponentRequirement requirement;

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
      this.rules.add(rule.getInternalRuleObject().toString());
    }
  }

  public void setInheritedRules(KBRuleList rules) {
    for (KBRule rule : rules.getRules()) {
      this.inheritedRules.add(rule.getInternalRuleObject().toString());
    }
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
}