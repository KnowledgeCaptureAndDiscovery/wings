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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.variables.*;

/**
 * Class used during workflow generation to pass component information to and
 * from the component catalog to the workflow planner
 * 
 * <ul>
 * <li>component : Component Variable
 * <li>roleMap : Component Input Role <=> Template Variable
 * <li>requirements : Also known as DOD's (Data Object Descriptions). These are
 * basically a list of KBTriple Objects
 * </ul>
 */
public class ComponentPacket {
	private ComponentVariable component;
	private LinkedHashMap<Role, Variable> roleMap;
	private ArrayList<KBTriple> requirements;

	// Keep a reverse mapping
	private LinkedHashMap<Variable, Role> variableMap;
	// Reasoner explanations (provided by component catalog)
	private HashSet<String> explanations;
	// If the reasoner marked this ComponentDetails packet as invalid
	public boolean isInvalid;
	
	private ArrayList<String> inputRoles;

	public ComponentPacket(ComponentVariable component, Map<Role, Variable> roleMap,
			ArrayList<KBTriple> requirements) {
		this.component = component;
		this.roleMap = new LinkedHashMap<Role, Variable>(roleMap);
		this.variableMap = createReverseMap(roleMap);
		this.requirements = requirements;
		this.explanations = new HashSet<String>();
		inputRoles = new ArrayList<String>();
		this.isInvalid = false;
	}

	public LinkedHashMap<Variable, Role> createReverseMap(Map<Role, Variable> map) {
		LinkedHashMap<Variable, Role> rmap = new LinkedHashMap<Variable, Role>();
		for (Role cp : map.keySet()) {
			rmap.put(map.get(cp), cp);
		}
		return rmap;
	}

	public LinkedHashMap<Role, Variable> createMapFromReverseMap(Map<Variable, Role> rmap) {
		LinkedHashMap<Role, Variable> map = new LinkedHashMap<Role, Variable>();
		for (Variable cp : rmap.keySet()) {
			map.put(rmap.get(cp), cp);
		}
		return map;
	}

	/**
	 * Getter for property 'component'.
	 * 
	 * @return Value for property 'component'.
	 */
	public ComponentVariable getComponent() {
		return component;
	}

	/**
	 * Setter for property 'component'.
	 * 
	 * @param component
	 *            Value to set for property 'component'.
	 */
	public void setComponent(ComponentVariable component) {
		this.component = component;
	}

	/**
	 * Getter for property 'roleMap'.
	 * 
	 * @return Value for property 'roleMap'.
	 */
	public LinkedHashMap<Role, Variable> getRoleMap() {
		return roleMap;
	}

	/**
	 * Setter for property 'roleMap'.
	 * 
	 * @param roleMap
	 *            Value to set for property 'roleMap'.
	 */
	public void setRoleMap(LinkedHashMap<Role, Variable> inputMaps) {
		this.roleMap = inputMaps;
		this.variableMap = createReverseMap(inputMaps);
	}

	/**
	 * Getter for property 'requirements'.
	 * 
	 * @return Value for property 'requirements'.
	 */
	public ArrayList<KBTriple> getRequirements() {
		return requirements;
	}

	/**
	 * Setter for property 'requirements'.
	 * 
	 * @param requirements
	 *            Value to set for property 'requirements'.
	 */
	public void setRequirements(ArrayList<KBTriple> requirements) {
		this.requirements = requirements;
	}

	/**
	 * Get Variable <=> Input Role Mappings
	 * 
	 * @return A LinkedHashMap of Variable <=> Role objects
	 */
	public LinkedHashMap<Variable, Role> getVariableMap() {
		return variableMap;
	}

	/**
	 * Get Input Role ID <=> Variable Mappings
	 * 
	 * @return Mapping Role IDs to Variable
	 */
	public LinkedHashMap<String, Variable> getStringRoleMaps() {
		LinkedHashMap<String, Variable> map = new LinkedHashMap<String, Variable>();
		for (Role cp : roleMap.keySet()) {
			map.put(cp.getRoleId(), roleMap.get(cp));
		}
		return map;
	}

	/**
	 * Get VarID <=> Input Role Mappings
	 * 
	 * @return Mapping Variable IDs to Roles
	 */
	public LinkedHashMap<String, Role> getStringVariableMap() {
		LinkedHashMap<String, Role> map = new LinkedHashMap<String, Role>();
		for (Variable var : variableMap.keySet()) {
			map.put(var.getID(), variableMap.get(var));
		}
		return map;
	}

	/**
	 * Add Explanations
	 * 
	 * @param explanations
	 *            The reasoning explaining the contents of this CMR (usually
	 *            returned from the Catalog)
	 */
	public void addExplanations(HashSet<String> explanations) {
		this.explanations.addAll(explanations);
	}

	public void addExplanations(String explanation) {
		this.explanations.add(explanation);
	}

	/**
	 * Set Roles to be inputs
	 */
	public void setInputRoles(ArrayList<String> roleid) {
		this.inputRoles = roleid;
	}
	
	public void addInputRole(String roleid) {
		this.inputRoles.add(roleid);
	}

	/**
	 * Retrieve roles as input or output
	 */
	public boolean isInputRole(String roleid) {
		return this.inputRoles.contains(roleid);
	}
	
	/**
	 * Get Explanations
	 * 
	 * @return The reasoning explaining the contents of this CMR (usually
	 *         returned from the Catalog)
	 */
	public HashSet<String> getExplanations() {
		return this.explanations;
	}

	/**
	 * Set Invalid Flag
	 * 
	 * @param isInvalid
	 *            Mark this CMR as invalid (i.e. not to be used)
	 */
	public void setInvalidFlag(boolean isInvalid) {
		this.isInvalid = isInvalid;
	}

	/**
	 * Get Invalid Flag
	 * 
	 * @return The reasoning explaining the contents of this CMR (usually
	 *         returned from the Catalog)
	 */
	public boolean getInvalidFlag() {
		return this.isInvalid;
	}

	@Override
	public String toString() {
		return "ComponentDetails{invalid=" + this.isInvalid 
		    + ",component=" + component + ", roleMap=" + roleMap
				+ ", requirements=" + requirements + '}';
	}
	
  public String toKey() {
    ArrayList<String> metricstrs = new ArrayList<String>();
    ArrayList<String> rolestrs = new ArrayList<String>();
    for(Role role : roleMap.keySet()) {
      Variable v = roleMap.get(role);
      String rolekey = role.getName() + "=" + v.getName();
      String metricskey = "";
      if(v.isParameterVariable()) {
        rolekey += "(" + v.getBinding() + ")";
        rolestrs.add(rolekey);
      }
      else if(v.isDataVariable() && v.getBinding() != null) {
        for(String prop : v.getBinding().getMetrics().getMetrics().keySet()) {
          v.getBinding().getMetrics().getMetrics().get(prop);
        }
        metricskey += "{" + v.getName() + ":" + v.getBinding().getMetrics() + "}";
        metricstrs.add(metricskey);
      }
    }
    metricstrs.sort(null);
    rolestrs.sort(null);
    return component.getBinding() + rolestrs.toString() + metricstrs.toString();
  }
  
  public ComponentPacket clone() {
    // Clone the packet
    // We just need to have separate variable bindings
    HashMap<Role, Variable> roleMap = this.getRoleMap();
    HashMap<Role, Variable> sendMap = new HashMap<Role, Variable>();

    for (Role r : roleMap.keySet()) {
      Variable var = roleMap.get(r);
      Variable sendVar = new Variable(var.getID(), var.getVariableType());
      sendVar.setBreakpoint(var.isBreakpoint());
      if (var.getBinding() != null)
        sendVar.setBinding((Binding) var.getBinding().clone());
      sendMap.put(r, sendVar);
    }
    ComponentPacket pthis = new ComponentPacket(this.getComponent(), sendMap,
        this.getRequirements());
    pthis.addExplanations(this.getExplanations());
    pthis.setInvalidFlag(this.getInvalidFlag());
    return pthis;
  }
}
