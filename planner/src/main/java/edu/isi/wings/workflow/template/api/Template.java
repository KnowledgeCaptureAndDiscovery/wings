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
	
	void deleteNode(Node n);

	Link addLink(Node fromN, Node toN, Port fromPort, Port toPort, Variable var);

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
	
	boolean save();
	
	boolean saveAs(String newid);
	
	boolean delete();
	
	void end();
}
