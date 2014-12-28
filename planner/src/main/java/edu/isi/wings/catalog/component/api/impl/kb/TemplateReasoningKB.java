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

package edu.isi.wings.catalog.component.api.impl.kb;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.component.classes.ComponentInvocation;
import edu.isi.wings.catalog.component.classes.ComponentPacket;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.planner.api.impl.kb.WorkflowGenerationKB;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.variables.ComponentVariable;
import edu.isi.wings.workflow.template.classes.variables.Variable;

public class TemplateReasoningKB implements ComponentReasoningAPI {

	WorkflowGenerationKB swg;
	String requestId;
	OntFactory ontfac;

	public TemplateReasoningKB(WorkflowGenerationKB swg) {
		this.swg = swg;
		setRequestId(requestId);
		this.ontfac = new OntFactory(OntFactory.JENA);
	}

	public boolean componentSubsumes(String subsumerClassID, String subsumedClassID) {
		// TODO: Maybe can be done later
		return false;
	}

	public ComponentPacket findDataDetails(ComponentPacket details) {
		// TODO: Should run a light backward,forward sweep ?
		return null;
	}

	public ComponentPacket findDataTypeDetails(ComponentPacket details) {
		// TODO: Should run a type-only light backward,forward sweep ?
		return null;
	}

	// TODO/FIXME: Ignore Variable constraints for now
	public ArrayList<ComponentPacket> findOutputDataPredictedDescriptions(ComponentPacket cmr) {
		// Do a forward sweep
		ArrayList<ComponentPacket> list = new ArrayList<ComponentPacket>();

		ComponentVariable c = cmr.getComponent();
		HashMap<String, Variable> sRoleMap = cmr.getStringRoleMaps();

		Template t = (Template) c.getBinding().getValue();
		if (t == null)
			return null;

		HashMap<String, Role> tRoles = new HashMap<String, Role>(t.getInputRoles());
		tRoles.putAll(t.getOutputRoles());

		// TODO: Role variables current do not equate to Template Variables

		// Transfer bindings
		for (String varid : tRoles.keySet()) {
			Role r = tRoles.get(varid);
			Variable v = sRoleMap.get(r.getID());
			if (v != null)
				t.getVariable(varid).setBinding(v.getBinding());
		}

		// Do forward sweep on the sub-template
		ArrayList<Template> templates = swg.configureTemplates(t);
		int i = 0;
		for (Template ct : templates) {
			ct.setID(ct.getID() + "_" + i);

			HashMap<String, Role> ctRoles = new HashMap<String, Role>(ct.getInputRoles());
			ctRoles.putAll(ct.getOutputRoles());

			ComponentVariable cv = new ComponentVariable(ct);
			cv.setBinding(new Binding(ct.getID()));

			HashMap<Role, Variable> rMap = new HashMap<Role, Variable>();

			// Transfer bindings
			for (String varid : ctRoles.keySet()) {
				Role r = ctRoles.get(varid);
				Variable vv = ct.getVariable(varid);
				Variable v = new Variable(sRoleMap.get(r.getID()).getID(), vv.getVariableType());
				v.setBinding(vv.getBinding());
				rMap.put(r, v);
			}
			ComponentPacket cmap = new ComponentPacket(cv, rMap, cmr.getRequirements());
			list.add(cmap);
			i++;
		}

		return list;
	}

	public ArrayList<ComponentVariable> getAllComponentTypes() {
		// TODO: Return all templates ?
		return null;
	}

	public ArrayList<Role> getComponentInputs(ComponentVariable c) {
		// TODO: Return template's input roles
		return null;
	}

	public ArrayList<Role> getComponentOutputs(ComponentVariable c) {
		// TODO: Return template's output roles
		return null;
	}

	public ComponentInvocation getComponentInvocation(ComponentPacket details) {
		// TODO: Unsure
		return null;
	}

	public void setRequestId(String id) {
		this.requestId = id;
	}

	public ArrayList<ComponentPacket> specializeAndFindDataDetails(ComponentPacket cmr) {
		// Do a backward sweep

		ArrayList<ComponentPacket> list = new ArrayList<ComponentPacket>();

		ComponentVariable c = cmr.getComponent();
		HashMap<String, Variable> sRoleMap = cmr.getStringRoleMaps();
		HashMap<String, Role> sVarMap = cmr.getStringVariableMap();

		Template t = c.getTemplate();
		if (t == null)
			return null;
		HashMap<String, Role> tRoles = new HashMap<String, Role>(t.getInputRoles());
		tRoles.putAll(t.getOutputRoles());

		HashMap<String, String> rvars = new HashMap<String, String>();
		for (String varid : tRoles.keySet()) {
			Role r = tRoles.get(varid);
			rvars.put(r.getID(), tRoles.get(r).getID());
		}

		// Transfer constraints to the sub-template
		ArrayList<KBTriple> triplesp = cmr.getRequirements();
		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();

		for (KBTriple triple : triplesp) {
			KBObject obj = triple.getObject();
			KBObject subj = triple.getSubject();
			if (subj != null && sVarMap.containsKey(subj.getID())) {
				subj = ontfac.getObject(rvars.get(sVarMap.get(subj.getID()).getID()));
			}
			if (obj != null && sVarMap.containsKey(obj.getID())) {
				obj = ontfac.getObject(rvars.get(sVarMap.get(obj.getID()).getID()));
			}
			triple.setSubject(subj);
			triple.setObject(obj);
			constraints.add(triple);
		}
		t.getConstraintEngine().addConstraints(constraints);

		// Do backward sweep on the sub-template
		ArrayList<Template> templates = swg.specializeTemplates(t);
		int i = 0;
		for (Template ct : templates) {
			ct.setID(ct.getID() + "_" + i);
			
			HashMap<String, Role> ctRoles = new HashMap<String, Role>(ct.getInputRoles());
			ctRoles.putAll(ct.getOutputRoles());

			ComponentVariable cv = new ComponentVariable(ct);
			cv.setBinding(new Binding(ct.getID()));

			ArrayList<String> inputRoles = new ArrayList<String>();
			
			HashMap<Role, Variable> rMap = new HashMap<Role, Variable>();
			HashMap<String, String> varMaps = new HashMap<String, String>();
			for (String varid : ctRoles.keySet()) {
				Role r = ctRoles.get(varid);
				Variable v = sRoleMap.get(r.getID());
				Variable vv = ct.getVariable(varid);
				if (v == null) {
					v = new Variable(t.getNamespace() + vv.getName(), vv.getVariableType());
				}
				varMaps.put(vv.getID(), v.getID());
				// The receiving template should know if this role was input or
				// output
				if (ct.getInputRoles().containsKey(r))
					inputRoles.add(r.getRoleId());
				rMap.put(r, v);
			}

			ArrayList<String> varids = new ArrayList<String>(varMaps.keySet());
			ArrayList<KBTriple> triples = ct.getConstraintEngine().getConstraints(varids);
			ArrayList<KBTriple> req = new ArrayList<KBTriple>();
			for (KBTriple triple : triples) {
				KBObject obj = triple.getObject();
				KBObject subj = triple.getSubject();
				if (subj != null && varMaps.containsKey(subj.getID())) {
					subj = ontfac.getObject(varMaps.get(subj.getID()));
				}
				if (obj != null && varMaps.containsKey(obj.getID())) {
					obj = ontfac.getObject(varMaps.get(obj.getID()));
				}
				triple.setSubject(subj);
				triple.setObject(obj);
				req.add(triple);
			}
			ComponentPacket cmap = new ComponentPacket(cv, rMap, req);
			cmap.setInputRoles(inputRoles);
			list.add(cmap);
			i++;
		}

		return list;
	}
}
