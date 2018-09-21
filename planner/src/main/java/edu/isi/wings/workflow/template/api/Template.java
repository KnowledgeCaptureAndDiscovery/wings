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

package edu.isi.wings.workflow.template.api;

import java.io.Serializable;
import java.util.HashMap;

import edu.isi.wings.workflow.template.classes.Link;
import edu.isi.wings.workflow.template.classes.Metadata;
import edu.isi.wings.workflow.template.classes.Node;
import edu.isi.wings.workflow.template.classes.Port;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.Rules;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.ValueBinding;
import edu.isi.wings.workflow.template.classes.variables.ComponentVariable;
import edu.isi.wings.workflow.template.classes.variables.Variable;

public interface Template extends Serializable {
	// Link Queries
	Link[] getLinks();

	Link[] getInputLinks();

	Link[] getOutputLinks();

	Link[] getIntermediateLinks();

	Link getLink(Node fromN, Node toN, Port fromPort, Port toPort);

	Link[] getLinks(Node fromN, Node toN);

	Link[] getLinks(Variable v);

	Link[] getInputLinks(Node n);

	Link[] getOutputLinks(Node n);

	Link getLink(String id);

	// Node Queries
	Node[] getNodes();

	Node getNode(String id);

	// Variable Queries
	Variable[] getVariables();

	Variable[] getInputVariables();
	
	Variable[] getInputDataVariables();	

	Variable[] getOutputVariables();

	Variable[] getIntermediateVariables();

	Variable[] getInputVariables(Node n);

	Variable[] getOutputVariables(Node n);

	Variable getVariable(String id);

	void deleteVariable(Variable v);

	Role getInputRoleForVariable(Variable v);

	Role getOutputRoleForVariable(Variable v);

	ComponentVariable getComponentVariable(String variableid);
	
	// Input Output roles of the template itself
	HashMap<String, Role> getInputRoles();

	HashMap<String, Role> getOutputRoles();

	void addInputRole(String vid, Role r);

	void addOutputRole(String vid, Role r);

	void deleteInputRoleForVariable(String vid);

	void deleteOutputRoleForVariable(String vid);

	// Automatically add roles based on input/output variables
	void autoUpdateTemplateRoles();

	// Automatically add set creation rules
	// (component/port set rules for expanding components during bw/fw sweeping)
	void fillInDefaultSetCreationRules();

	// Constraint Queries
	ConstraintEngine getConstraintEngine();

	// Template Editing Functions
	Node addNode(ComponentVariable c);

	Variable addVariable(String varid, short type); // type comes from VariableType
	
	Variable addVariable(String varid, short type, boolean isCollectionItem);
	
	void deleteNode(Node n);

	Link addLink(Node fromN, Node toN, Port fromPort, Port toPort, Variable var);
	
	void updateLinkDetails(Link l);
	
	void deleteLink(Link l);

	void setVariableBinding(Variable v, Binding b);
	
	void setVariableBinding(Variable v, ValueBinding b);

	Template createCopy();

	String getInternalRepresentation();

	String serialize();
	
	void resetInternalRepresentation();

	String getID();

	void setID(String templateId);

	void setCreatedFrom(Template createdFrom);

	Template getCreatedFrom();

	void setParent(Template parent);

	Template getParent();

	String getName();

	String getNamespace();

	String getURL();

	Metadata getMetadata();

	Rules getRules();

	Template applyRules();
	
	void autoLayout();
	
	// Save/Delete
	boolean save();
	
	boolean saveAs(String newid);
	
	boolean delete();
}
