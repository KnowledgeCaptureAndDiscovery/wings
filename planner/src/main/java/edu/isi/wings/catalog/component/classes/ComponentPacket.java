package edu.isi.wings.catalog.component.classes;

import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.variables.*;

import java.util.ArrayList;
import java.util.HashMap;

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
	private HashMap<Role, Variable> roleMap;
	private ArrayList<KBTriple> requirements;

	// Keep a reverse mapping
	private HashMap<Variable, Role> variableMap;
	// Reasoner explanations (provided by component catalog)
	private ArrayList<String> explanations;
	// If the reasoner marked this ComponentDetails packet as invalid
	public boolean isInvalid;
	
	private ArrayList<String> inputRoles;

	public ComponentPacket(ComponentVariable component, HashMap<Role, Variable> roleMap,
			ArrayList<KBTriple> requirements) {
		this.component = component;
		this.roleMap = roleMap;
		this.variableMap = createReverseMap(roleMap);
		this.requirements = requirements;
		this.explanations = new ArrayList<String>();
		inputRoles = new ArrayList<String>();
		this.isInvalid = false;
	}

	public HashMap<Variable, Role> createReverseMap(HashMap<Role, Variable> map) {
		HashMap<Variable, Role> rmap = new HashMap<Variable, Role>();
		for (Role cp : map.keySet()) {
			rmap.put(map.get(cp), cp);
		}
		return rmap;
	}

	public HashMap<Role, Variable> createMapFromReverseMap(HashMap<Variable, Role> rmap) {
		HashMap<Role, Variable> map = new HashMap<Role, Variable>();
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
	public HashMap<Role, Variable> getRoleMap() {
		return roleMap;
	}

	/**
	 * Setter for property 'roleMap'.
	 * 
	 * @param roleMap
	 *            Value to set for property 'roleMap'.
	 */
	public void setRoleMap(HashMap<Role, Variable> inputMaps) {
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
	 * @return A HashMap of Variable <=> Role objects
	 */
	public HashMap<Variable, Role> getVariableMap() {
		return variableMap;
	}

	/**
	 * Get Input Role ID <=> Variable Mappings
	 * 
	 * @return Mapping Role IDs to Variable
	 */
	public HashMap<String, Variable> getStringRoleMaps() {
		HashMap<String, Variable> map = new HashMap<String, Variable>();
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
	public HashMap<String, Role> getStringVariableMap() {
		HashMap<String, Role> map = new HashMap<String, Role>();
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
	public void addExplanations(ArrayList<String> explanations) {
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
	public ArrayList<String> getExplanations() {
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
		return "ComponentDetails{" + "component=" + component + ", roleMap=" + roleMap
				+ ", requirements=" + requirements + '}';
	}
}
