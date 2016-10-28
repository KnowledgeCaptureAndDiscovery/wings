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

package edu.isi.wings.planner.api.impl.kb;

import org.apache.log4j.Logger;

import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.component.api.impl.kb.TemplateReasoningKB;
import edu.isi.wings.catalog.component.classes.ComponentInvocation;
import edu.isi.wings.catalog.component.classes.ComponentPacket;
import edu.isi.wings.catalog.data.api.DataReasoningAPI;
import edu.isi.wings.catalog.data.classes.VariableBindings;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.metrics.Metric;
import edu.isi.wings.catalog.data.classes.metrics.Metrics;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.common.SerializableObjectCloner;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.common.logging.LogEvent;
import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.planner.api.WorkflowGenerationAPI;
import edu.isi.wings.workflow.plan.PlanFactory;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionCode;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import edu.isi.wings.workflow.template.*;
import edu.isi.wings.workflow.template.api.ConstraintEngine;
import edu.isi.wings.workflow.template.api.Seed;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.impl.kb.TemplateKB;
import edu.isi.wings.workflow.template.classes.Link;
import edu.isi.wings.workflow.template.classes.Node;
import edu.isi.wings.workflow.template.classes.Port;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.ComponentSetCreationRule;
import edu.isi.wings.workflow.template.classes.sets.PortBinding;
import edu.isi.wings.workflow.template.classes.sets.PortBindingList;
import edu.isi.wings.workflow.template.classes.sets.PortSetCreationRule;
import edu.isi.wings.workflow.template.classes.sets.PortSetRuleHandler;
import edu.isi.wings.workflow.template.classes.sets.ValueBinding;
import edu.isi.wings.workflow.template.classes.sets.WingsSet;
import edu.isi.wings.workflow.template.classes.sets.SetCreationRule.SetType;
import edu.isi.wings.workflow.template.classes.variables.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Name: WorkflowGenerationKB
 */
public class WorkflowGenerationKB implements WorkflowGenerationAPI {
	private Logger logger;

	public Seed currentSeed;

	public DataReasoningAPI dc;

	public ComponentReasoningAPI pc;

	public ComponentReasoningAPI tc;
	
	public ResourceAPI rc;

	public ArrayList<String> explanations;

	LogEvent curLogEvent;

	public String request_id;

	String dataNS;
	String wNS;
	String exPrefix;

	Properties props;
	
	/**
	 * base constructor
	 * 
	 * @param uriPrefix
	 *            the uriPrefix
	 * @param dc
	 *            the dc
	 * @param pc
	 *            the pc
	 * @param baseDirectory
	 *            the base directory of the ontologies
	 * @param templateLibraryDomain
	 *            the domain of the template library
	 */

	public WorkflowGenerationKB(Properties props, DataReasoningAPI dc, 
	    ComponentReasoningAPI pc, ResourceAPI rc, String ldid) {
		this.props = props;
		this.request_id = ldid;
		this.logger = Logger.getLogger(this.getClass().getName());

		this.dc = dc;
		this.pc = pc;
		this.rc = rc;
		this.tc = new TemplateReasoningKB(this);
		this.dataNS = props.getProperty("lib.domain.data.url") + "#";
		this.wNS = props.getProperty("ont.workflow.url") + "#";
		this.exPrefix = props.getProperty("domain.executions.dir.url");
		this.explanations = new ArrayList<String>();
	}


	public Seed loadSeed(String seedid) {
		Seed seed = TemplateFactory.getSeed(props, seedid);
		seed.setID(seed.getID() + this.request_id);
		return seed;
	}

	@Override
	public Template loadTemplate(String templateid) {
		Template template = TemplateFactory.getTemplate(props, templateid);

		return template;
	}

	@Override
	public void useDataService(DataReasoningAPI dc) {
		this.dc = dc;
	}

	@Override
	public void useComponentService(ComponentReasoningAPI pc) {
		this.pc = pc;
	}

	@Override
	public ArrayList<String> getExplanations() {
		return this.explanations;
	}

	/**
	 * Step 2
	 * 
	 * @param template
	 *            a candiate template from step 1.
	 * @return a list of specialized templates customized to seed constraints
	 */
	public ArrayList<Template> specializeTemplates(Template template) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_SPECIALIZE);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));

		ComponentReasoningAPI pc = this.pc;

		ArrayList<Template> templates = new ArrayList<Template>();
		ArrayList<Template> processedTemplates = new ArrayList<Template>();
		// ArrayList<Template> rejectedTemplates = new ArrayList<Template>();

		HashMap<Template, ArrayList<String>> done = new HashMap<Template, ArrayList<String>>();

		if (template == null)
			return templates;

		Template tmp = template.createCopy();
		tmp.setID(UuidGen.generateURIUuid((URIEntity)template));
		templates.add(tmp);

		while (!templates.isEmpty()) {
			logger.info(event.createLogMsg().addList(LogEvent.QUEUED_TEMPLATES, templates));
			logger.info(event.createLogMsg().addList(LogEvent.SPECIALIZED_TEMPLATES_Q,
					processedTemplates));

			Template currentTemplate = templates.remove(0);

			ArrayList<String> nodesDone = done.get(currentTemplate);
			if (nodesDone == null) {
				nodesDone = new ArrayList<String>();
			}

			ArrayList<Link> links = new ArrayList<Link>();
			Link[] linkArray = currentTemplate.getOutputLinks();
			for (Link link : linkArray) {
				links.add(link);
			}

			while (!links.isEmpty()) {
				HashMap<Role, Variable> roleMap = new HashMap<Role, Variable>();

				Link currentLink = links.remove(0);
				if (currentLink.isInputLink()) {
					// continue;
					// no op
				} else {
					roleMap.put(currentLink.getOriginPort().getRole(), currentLink.getVariable());
					ArrayList<String> variableIds = new ArrayList<String>();
					Node originNode = currentLink.getOriginNode();

					Link[] outputLinks = currentTemplate.getOutputLinks(originNode);
					for (Link outputLink : outputLinks) {
						Variable variable = outputLink.getVariable();
						roleMap.put(outputLink.getOriginPort().getRole(), variable);
						variableIds.add(variable.getID());
						links.remove(outputLink);
					}

					Link[] inputLinks = currentTemplate.getInputLinks(originNode);
					for (Link inputLink : inputLinks) {
						Variable variable = inputLink.getVariable();
						roleMap.put(inputLink.getDestinationPort().getRole(), variable);
						variableIds.add(variable.getID());
						links.add(inputLink);
					}

					if (nodesDone.contains(originNode.getID())) {
						// continue;
						// no op
					} else {
						ArrayList<KBTriple> redBox = currentTemplate.getConstraintEngine()
								.getConstraints(variableIds);

						ComponentVariable component = originNode.getComponentVariable();
						if (component.isTemplate())
							pc = this.tc;
						else
							pc = this.pc;

						ComponentPacket sentMapsComponentDetails = new ComponentPacket(component,
								roleMap, redBox);

						if (logger.isInfoEnabled()) {
							HashMap<String, Object> args = new HashMap<String, Object>();
							args.put("component", component);
							args.put("roleMap", roleMap);
							args.put("redBox", redBox);
							logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "2.1")
									.addMap(LogEvent.QUERY_ARGUMENTS, args));
						}
						ArrayList<ComponentPacket> allcmrs = pc
								.specializeAndFindDataDetails(sentMapsComponentDetails);
						ArrayList<ComponentPacket> componentDetailsList = new ArrayList<ComponentPacket>();
						for (ComponentPacket cmr : allcmrs) {
							this.addExplanations(cmr.getExplanations());
							if (!cmr.getInvalidFlag())
								componentDetailsList.add(cmr);
							else {
								// Template t = currentTemplate.createCopy();
								// rejectedTemplates.add(t);
							}
						}

						if (componentDetailsList.isEmpty()) {
							logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "2.1")
									.addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
							currentTemplate = null;
							break;
						} else {
							if (logger.isInfoEnabled()) {
								ArrayList<ComponentVariable> components = new ArrayList<ComponentVariable>();
								for (ComponentPacket componentMapsAndRequirement : componentDetailsList) {
									components.add(componentMapsAndRequirement.getComponent());
								}
								logger.info(event
										.createLogMsg()
										.addWQ(LogEvent.QUERY_NUMBER, "2.1")
										.addList(LogEvent.QUERY_RESPONSE + ".components",
												components));
							}

							nodesDone.add(originNode.getID());
							done.put(currentTemplate, nodesDone);

							// note this is over the rest of the cmrs
							ComponentSetCreationRule crule = originNode.getComponentSetRule();
							if (crule == null || crule.getType() == SetType.WTYPE) {
								for (int i = 1; i < componentDetailsList.size(); i++) {
									ComponentPacket cmr = componentDetailsList.get(i);
									this.addExplanations(cmr.getExplanations());
									Template specializedTemplate = currentTemplate.createCopy();
									specializedTemplate.setID(
											UuidGen.generateURIUuid((URIEntity)currentTemplate));
									Node specializedNode = specializedTemplate.getNode(originNode
											.getID());
									boolean ok = this.modifyTemplate(specializedTemplate,
											specializedNode,
											new ComponentPacket[] { componentDetailsList.get(i) });
									if (ok) {
										templates.add(specializedTemplate);
									}
									done.put(specializedTemplate, new ArrayList<String>(nodesDone));
								}
								ComponentPacket firstCmr = componentDetailsList.get(0);
								boolean ok = this.modifyTemplate(currentTemplate, originNode,
										new ComponentPacket[] { firstCmr });
								if (!ok) {
									currentTemplate = null;
									break;
								}
							} else if (crule != null && crule.getType() == SetType.STYPE) {
								boolean ok = this.modifyTemplate(currentTemplate, originNode,
										componentDetailsList.toArray(new ComponentPacket[0]));
								if (!ok) {
									currentTemplate = null;
									break;
								}
							}
						}
					}
				}
			}
			if (currentTemplate != null) {
				currentTemplate.autoUpdateTemplateRoles(); // If any new input/output
				// variables have been
				// created
				currentTemplate.fillInDefaultSetCreationRules();
				processedTemplates.add(currentTemplate);
			}
		}
		logger.info(event.createEndLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));
		return processedTemplates;
	}

	/**
	 * step 3
	 * 
	 * @param specializedTemplate
	 *            a specialized template
	 * @return a list of partially specified template instances - input data
	 *         objects bound
	 */
	public ArrayList<Template> selectInputDataObjects(Template specializedTemplate) {

		LogEvent event = this.curLogEvent;
		if (event == null) {
			event = this.getEvent(LogEvent.EVENT_WG_DATA_SELECTION);
		}
		logger.info(event.createLogMsg().addWQ(LogEvent.TEMPLATE, "" + specializedTemplate));

		DataReasoningAPI dc = this.dc;
		ArrayList<Template> boundTemplates = new ArrayList<Template>();

		Variable[] variables = specializedTemplate.getVariables();
		ArrayList<String> blacklist = new ArrayList<String>(variables.length);
		for (Variable variable : variables) {
			blacklist.add(variable.getID());
		}

		// Data Filtering properties
		String hdbPropId = this.wNS + "hasDataBinding";
		blacklist.add(hdbPropId);

		Variable[] inputVariables = specializedTemplate.getInputVariables();
		ArrayList<String> inputVariableIds = new ArrayList<String>();
		ArrayList<String> nonCollectionIds = new ArrayList<String>();

		HashMap<String, HashSet<String>> varUserBindings = new HashMap<String, HashSet<String>>();
		HashMap<String, ArrayList<String>> varEquality = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> varInequality = new HashMap<String, ArrayList<String>>();

		for (Variable inputVariable : inputVariables) {
			if (inputVariable.isDataVariable()) {
				Role r = specializedTemplate.getInputRoleForVariable(inputVariable);
				if (r.getDimensionality() == 0) {
					nonCollectionIds.add(inputVariable.getID());
				} else if (r.getDimensionality() > 1) {
					this.addExplanation("ERROR No Support for " + r.getDimensionality()
							+ "-D Template Input roles (" + r.getName() + ")");
					continue;
				}
				String variableId = inputVariable.getID();
				inputVariableIds.add(variableId);
				blacklist.remove(variableId);

				Binding b = inputVariable.getBinding();
				if (b != null) {
					HashSet<String> userBindings = new HashSet<String>();
					if (b.isSet()) {
						if (b.getMaxDimension() > 1) {
							this.addExplanation("ERROR No Support for " + b.getMaxDimension()
									+ "-D Input Data");
							continue;
						}
						for (WingsSet s : b)
							userBindings.add(((Binding) s).getID());
					} else {
						userBindings.add(b.getID());
					}
					varUserBindings.put(variableId, userBindings);
				}
			}
		}
		Collections.sort(nonCollectionIds);

		ConstraintEngine engine = specializedTemplate.getConstraintEngine();
		for (String id : blacklist)
			engine.addBlacklistedId(id);
		ArrayList<KBTriple> allInputConstraints = engine.getConstraints(inputVariableIds);
		for (String id : blacklist)
			engine.removeBlacklistedId(id);

		ArrayList<KBTriple> inputConstraints = new ArrayList<KBTriple>();
		for (KBTriple t : allInputConstraints) {
			if (t.getPredicate().getID()
					.equals(this.wNS + "hasSameDataAs")) {
				String var1 = t.getObject().getID();
				String var2 = t.getSubject().getID();
				ArrayList<String> equality1 = varEquality.get(var1);
				ArrayList<String> equality2 = varEquality.get(var2);
				if (equality1 == null)
					equality1 = new ArrayList<String>();
				if (equality2 == null)
					equality2 = new ArrayList<String>();
				equality1.add(var2);
				equality2.add(var1);
				varEquality.put(var1, equality1);
				varEquality.put(var2, equality2);
			} else if (t.getPredicate().getID()
					.equals(this.wNS + "hasDifferentDataFrom")) {
				String var1 = t.getObject().getID();
				String var2 = t.getSubject().getID();
				ArrayList<String> inequality1 = varInequality.get(var1);
				ArrayList<String> inequality2 = varInequality.get(var2);
				if (inequality1 == null)
					inequality1 = new ArrayList<String>();
				if (inequality2 == null)
					inequality2 = new ArrayList<String>();
				inequality1.add(var2);
				inequality2.add(var1);
				varInequality.put(var1, inequality1);
				varInequality.put(var2, inequality2);
			} else {
				inputConstraints.add(t);
			}
		}

		/*HashMap<String, ArrayList<KBTriple>> varConstraints = 
		    new HashMap<String, ArrayList<KBTriple>>();
    HashMap<String, HashSet<String>> varGroups = 
        new HashMap<String, HashSet<String>>();
		HashMap<String, HashSet<String>> varDeps = 
		    new HashMap<String, HashSet<String>>();
		HashMap<String, HashSet<String>> depVars =
		    new HashMap<String, HashSet<String>>();
		
		for(String vid : inputVariableIds) {
		  ArrayList<KBTriple> constraints = 
		      specializedTemplate.getConstraintEngine().getConstraints(vid);
	    
		  HashSet<String> vargroup = new HashSet<String>();
		  varDeps.put(vid, new HashSet<String>());
		  for(KBTriple triple : constraints) {
		    if(!triple.getObject().isLiteral()) {
		      String objid = triple.getObject().getID();
		      if(triple.getPredicate().getNamespace().equals(this.wNS))
		        vargroup.add(objid);
		      else if(objid.startsWith(triple.getSubject().getNamespace())) {
		        varDeps.get(vid).add(objid);
		        if(!depVars.containsKey(objid))
		          depVars.put(objid, new HashSet<String>());
		        depVars.get(objid).add(vid);
		      }
		    }
		  }
      varConstraints.put(vid, constraints);
		  varGroups.put(vid, vargroup);
		}
		
		System.out.println("----------------");
		System.out.println(varGroups);
		System.out.println(depVars);
		System.out.println(varDeps);
		
		// 1. Create a map of:
		//    - varcons: inputVariableId with Constraints
		//    - vardeps: inputVariableId with other dependencies (wflow namespace terms)
		//    - depvars: dependencyId terms with inputVariableIds
		//
		// 2. For each input variable id, check vardeps:
		//    - queryvarids: array of inputVariableIds. Start with just one.
		//    - For each queryvid in queryvarids:
		//      - For each depid in vardeps[queryvid]:
		//        - queryvarids.addAll(depvars[depid]) // Remove duplicates
		//        - unset depvars[depid]
		//    - For each queryvarid in queryvarids:
		//      - queryconstraints.addAll(queryvarid)
		//    - Send query to DC with queryvarids & queryconstraints
		//    - Remove queryvarids from input variable id list

		for(String vid : inputVariableIds) {
		  System.out.println(vid);
		  for(KBTriple triple : specializedTemplate.getConstraintEngine().getConstraints(vid))
		    System.out.println(triple.shortForm());
		}*/
		
		logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "3.1")
				.addList(LogEvent.QUERY_ARGUMENTS, inputConstraints));

		this.addExplanation("Querying the DataReasoningAPI with the following constraints: <br/>"
				+ inputConstraints.toString().replaceAll(",", "<br/>"));
		ArrayList<VariableBindingsList> listsOfVariableDataObjectMappings = dc
				.findDataSources(inputConstraints);

		//ArrayList<VariableBindingsList> listsOfVariableDataObjectMappings = null;
		if (listsOfVariableDataObjectMappings == null
				|| listsOfVariableDataObjectMappings.isEmpty()) {
			logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "3.1")
					.addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
			this.addExplanation("ERROR: The DataReasoningAPI did not return any matching datasets");
		} else {
			logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "3.1")
					.addList(LogEvent.QUERY_RESPONSE, listsOfVariableDataObjectMappings));

			// Filter Datasets
			ArrayList<VariableBindingsList> filteredList = filterVariableDataObjectMappings(
					listsOfVariableDataObjectMappings, inputVariableIds, varUserBindings,
					varEquality, varInequality);

			// Group Datasets
			ArrayList<VariableBindingsList> groupedList = groupVariableDataObjectMappings(
					filteredList, nonCollectionIds, inputVariableIds);

			// New Template for each group
			for (VariableBindingsList mapping : groupedList) {
				HashMap<String, HashSet<String>> variableBindings = new HashMap<String, HashSet<String>>();
				Template t = specializedTemplate.createCopy();
				t.setID(UuidGen.generateURIUuid((URIEntity)t));

				for (VariableBindings dvobinding : mapping) {
					KBObject dv = dvobinding.getDataVariable();
					HashSet<KBObject> objs = dvobinding.getDataObjects();

					// Ignore temporary variables (not input data variables)
					if (!inputVariableIds.contains(dv.getID())) {
						// This could be a temporary variable (skolem variable)
						// created by component rules.
						// Remove all constraints for the temporary variable as
						// it has fulfilled it's purpose i.e. data selection)
						t.getConstraintEngine().removeObjectAndConstraints(dv);

						// Replace all occurences of it in the constraint engine
						// with the bound value
						/*
						 * for (KBObject obj : objs) {
						 * t.getConstraintEngine().replaceSubjectInConstraints
						 * (dv, obj);
						 * t.getConstraintEngine().replaceObjectInConstraints
						 * (dv, obj); }
						 */
						continue;
					}

					Variable v = specializedTemplate.getVariable(dv.getID());
					HashSet<String> tmp = variableBindings.get(v.getID());
					if (tmp == null)
						tmp = new HashSet<String>();

					for (KBObject obj : objs)
						tmp.add(obj.getID());

					variableBindings.put(v.getID(), tmp);
				}

				Variable[] ivs = t.getInputVariables();
				boolean unboundDataVariableP = false;
				for (Variable iv : ivs) {
					if (iv.isDataVariable()) {
						HashSet<String> bindingIds = variableBindings.get(iv.getID());
						if (bindingIds != null && !bindingIds.isEmpty()) {
							Binding b = new Binding();

							Binding ivb = iv.getBinding();
							if (ivb != null) {

								// Check the case where the template already has
								// a binding
								boolean ok = false;
								if (!ivb.isSet()) {
									if (bindingIds.contains(ivb.getID())) {
										b = (Binding) SerializableObjectCloner.clone(ivb);
										ok = true;
									} else {
										this.addExplanation("INFO " + ivb.getName() + " cannot be bound to "
												+ iv.getID());
									}
								} else {
									for (WingsSet s : ivb) {
										Binding civb = (Binding)s;
										if (bindingIds.contains(civb.getID())) {
											b.add((Binding) SerializableObjectCloner.clone(civb));
											ok = true;
										} else {
											this.addExplanation("INFO " + civb.getName()
													+ " cannot be bound to "
													+ iv.getID());
										}
									}
								}
								if (ok)
									iv.setBinding(b);
								else {
									unboundDataVariableP = true;
									iv.setBinding(null);
									break;
								}
							} else {
								for (String bindingId : bindingIds)
									b.add(new Binding(bindingId));
								iv.setBinding(b);
							}
						} else {
							unboundDataVariableP = true;
							break;
						}
					}
				}
				if (!unboundDataVariableP) {
					boundTemplates.add(t);
				}
			}
		}

		return boundTemplates;
	}

	/**
	 * Step 4: 4.1 sets the data metrics for the partially instantiated
	 * candidate instances
	 * 
	 * @param partialCandidateInstances
	 *            a list of candidate instances with input data variables bound
	 */
	public void setDataMetricsForInputDataObjects(ArrayList<Template> partialCandidateInstances) {
		HashMap<String, Metrics> dataObjectNameToDataMetricsMap = new HashMap<String, Metrics>();
		LogEvent event = curLogEvent;

		for (Template partialCandidateInstance : partialCandidateInstances) {
			Variable[] inputVariables = partialCandidateInstance.getInputVariables();
			for (Variable inputVariable : inputVariables) {
				if (inputVariable.isDataVariable()) {
					Binding binding = inputVariable.getBinding();
					setBindingMetrics(binding, dataObjectNameToDataMetricsMap, event);
				}
			}
		}
	}
	
	/**
	 * Step 4
	 * 
	 * @param template
	 *            a specialized template
	 * @return configured candidate workflows with all paramter values set
	 */

	public ArrayList<Template> configureTemplates(Template template) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_CONFIGURE);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));

		ArrayList<Template> templates = new ArrayList<Template>();
		ArrayList<Template> processedTemplates = new ArrayList<Template>();

		HashMap<Template, ArrayList<String>> done = new HashMap<Template, ArrayList<String>>();

		Template t = template.createCopy();
		t.setID(UuidGen.generateURIUuid((URIEntity)t));
		templates.add(t);

		// Configuration Step
		while (!templates.isEmpty()) {
			logger.info(event.createLogMsg().addList(LogEvent.QUEUED_TEMPLATES, templates));
			logger.info(event.createLogMsg().addList(LogEvent.CONFIGURED_TEMPLATES_Q,
					processedTemplates));

			Template currentTemplate = templates.remove(0);

			ArrayList<String> nodesDone = done.get(currentTemplate);
			if (nodesDone == null) {
				nodesDone = new ArrayList<String>();
			}

			ArrayList<Link> links = new ArrayList<Link>();
			for (Link link : currentTemplate.getInputLinks()) {
				links.add(link);
			}

			while (!links.isEmpty()) {
				if (currentTemplate == null)
					break;

				HashMap<Role, Variable> roleMap = new HashMap<Role, Variable>();

				Link currentLink = links.remove(0);

				if (!currentLink.isOutputLink()) {
					roleMap.put(currentLink.getDestinationPort().getRole(),
							currentLink.getVariable());
					ArrayList<String> variableIds = new ArrayList<String>();
					Node destNode = currentLink.getDestinationNode();

					// If this node hasn't been processed yet
					if (!nodesDone.contains(destNode.getID())) {

						Link[] inputLinks = currentTemplate.getInputLinks(destNode);
						Link[] outputLinks = currentTemplate.getOutputLinks(destNode);

						// Check that all inputs have data bindings
						boolean comebacklater = false;
						for (Link inputLink : inputLinks) {
							Variable invar = inputLink.getVariable();
							if (invar.isDataVariable() && invar.getBinding() == null) {
								comebacklater = true;
								break;
							}
						}
						if (comebacklater) {
							links.add(currentLink);
							continue;
						}

						HashMap<String, String> prospectiveIds = new HashMap<String, String>();
						HashMap<String, String> portVariableIds = new HashMap<String, String>();
						HashMap<String, String> opPortVariableIds = new HashMap<String, String>();

						// Add all output links to queue
						// Set temporary output dataset id
						for (Link outputLink : outputLinks) {
							Variable variable = outputLink.getVariable();
							if (variable.getBinding() == null) {
								String prospectiveId = dataNS + UuidGen.generateAUuid("");
								variable.setBinding(new Binding(prospectiveId));
								prospectiveIds.put(variable.getID(), prospectiveId);
							}

							roleMap.put(outputLink.getOriginPort().getRole(), variable);
							variableIds.add(variable.getID());
							links.add(outputLink);
							portVariableIds.put(outputLink.getOriginPort().getID(),
									variable.getID());
							opPortVariableIds.put(outputLink.getOriginPort().getID(),
									variable.getID());
						}

						// Remove all input links from queue
						for (Link inputLink : inputLinks) {
							Variable variable = inputLink.getVariable();
							roleMap.put(inputLink.getDestinationPort().getRole(), variable);
							variableIds.add(variable.getID());
							links.remove(inputLink);
							portVariableIds.put(inputLink.getDestinationPort().getID(),
									variable.getID());
						}

						ArrayList<KBTriple> redBox = currentTemplate.getConstraintEngine()
								.getConstraints(variableIds);

						ComponentVariable component = destNode.getComponentVariable();

						// TODO: Check the port rules & variable bindings here
						// - For now, ignore all parameters in port rules
						// (add port-parameter-rules later)
						// - Have to get constraints for bindings with the
						// correct dimensionality here ? (or maybe PC will
						// handle it from the metrics)

						ComponentPacket cmr = new ComponentPacket(component, roleMap, redBox);

						PortBindingList ipblist = PortSetRuleHandler.normalizePortBindings(
								destNode, currentTemplate);

						// PortBindingList opblist = configureBindings(pblist);
						// Recursively go through the whole pblist and create
						// input/output maps and get parameter bindings for each
						// portbinding
						// - Maybe check rules at this point too in the future ?

						// constraints for these bindings needed or will the
						// metrics be enough ?
						// - should we translate metrics to rdf constraints ?
						// - should we represent metrics as they are (strings)
						// in the rdf ?

						// Check that opblist is not empty

						PortSetCreationRule prule = destNode.getPortSetRule();
						nodesDone.add(destNode.getID());

						if (ipblist.isEmpty() && ipblist.getPortBinding() == null) {
							currentTemplate = null;
							break;
						}

						if (prule.getType() == SetType.WTYPE) {
							ipblist = PortSetRuleHandler.flattenPortBindingList(ipblist, 0);

							for (int i = ipblist.size() - 1; i >= 0; i--) {
								// Todo: Parallelize: have independent threads
								// for each iteration

								PortBindingList ipb = ipblist.get(i);

								Template configuredTemplate = currentTemplate;
								ComponentVariable c = component;
								Node n = destNode;
								if (i > 0) {
									configuredTemplate = currentTemplate.createCopy();
									configuredTemplate.setID(
											UuidGen.generateURIUuid((URIEntity)currentTemplate));
									n = configuredTemplate.getNode(destNode.getID());
									c = n.getComponentVariable();
								}

								// Clone cmr before sending ? We basically need
								// to have separate variable bindings
								ComponentPacket pcmr = cloneCMRBindings(cmr);
								pcmr.setComponent(c);

								PortBindingList pblist = configureBindings(ipb, destNode, n, c,
										pcmr, event, prospectiveIds);
								PortBinding pb = PortSetRuleHandler.deNormalizePortBindings(pblist);

								if (pb == null) {
									if (i == 0)
										currentTemplate = null;
									continue;
								}

								this.removeComponentBindingsWithNoData(c);

								// CHANGED: (6/6/2011)
								// Extract bindings only for output variables or
								// parameter variables
								// Earlier we were doing this for all variables
								for (Port p : pb.keySet()) {
									Variable cv = configuredTemplate.getVariable(portVariableIds
											.get(p.getID()));
									if (cv.isParameterVariable()
											|| opPortVariableIds.containsKey(p.getID())) {
										Binding b = pb.get(p);
										cv.setBinding(b);
									}
								}

								if (i > 0) {
									templates.add(configuredTemplate);
									done.put(configuredTemplate, new ArrayList<String>(nodesDone));
								} else {
									logger.info(event.createLogMsg().addWQ(LogEvent.MSG,
											"Configured Template: " + currentTemplate));
									done.put(currentTemplate, nodesDone);
								}

							}

						} else if (prule.getType() == SetType.STYPE) {
							PortBindingList pblist = configureBindings(ipblist, destNode, destNode,
									component, cmr, event, prospectiveIds);
							PortBinding pb = PortSetRuleHandler.deNormalizePortBindings(pblist);

							if (pb == null) {
								currentTemplate = null;
								break;
							}

							this.removeComponentBindingsWithNoData(component);

							// CHANGED: (6/6/2011)
							// Extract bindings only for output variables or
							// parameter variables
							// Earlier we were doing this for all variables
							for (Port p : pb.keySet()) {
								Variable cv = currentTemplate.getVariable(portVariableIds.get(p
										.getID()));
								if (cv.isParameterVariable()
										|| opPortVariableIds.containsKey(p.getID())) {
									Binding b = pb.get(p);
									cv.setBinding(b);
								}
							}

							logger.info(event.createLogMsg().addWQ(LogEvent.MSG,
									"Configured Template: " + currentTemplate));
							done.put(currentTemplate, nodesDone);
						}

						// System.exit(0);
					} else if (currentTemplate != null) {
						Link[] outputLinks = currentTemplate.getOutputLinks(destNode);
						for (Link ol : outputLinks) {
							links.add(ol);
						}
					}
				}
			}
			if (currentTemplate != null) {
				// Check that all component bindings in the template
				// have portbindings
				/*boolean ok = true;
				for (Node n : currentTemplate.getNodes()) {
					if (hasEmptyPortBindings(n.getComponentVariable().getBinding())) {
						ok = false;
						break;
					}
				}*/
				
				// Check in-out links to see that all the data that is
				// produced, is actually consumed by a component on the other
				// side of the link
				// - If not, then remove the producer component
				// - TODO: Should this be a configurable option ?
				if(removeProducersWithNoConsumers(currentTemplate))
					processedTemplates.add(currentTemplate);
			}
		}
		
		// Run template rules (if any)
		ArrayList<Template> configuredTemplates = new ArrayList<Template>(); 
		for (Template instance : processedTemplates) {
			if (instance.getRules() != null && instance.getRules().getRulesText() != null) {
				// Check template invalidity
				instance = instance.applyRules();
				if (instance == null) {
					logger.warn(event.createLogMsg().addWQ(
							LogEvent.MSG,
							"Invalid Workflow Instance " + instance
									+ " : Template Rules not satisfied"));
					continue;
				}
			}
			configuredTemplates.add(instance);
		}

		logger.info(event.createEndLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));

		return configuredTemplates;
	}

	/**
	 * Add inferences to a template without
	 * - No specialization  or configuration
	 */
	public Template getInferredTemplate(Template template) {
		ComponentReasoningAPI pc = this.pc;

		HashMap<Template, ArrayList<String>> done = new HashMap<Template, ArrayList<String>>();

		Template currentTemplate = template.createCopy();
		currentTemplate.setID(
				UuidGen.generateURIUuid((URIEntity)template));

		int MAXITERATIONS = 2; // Only 2 max iterations for now to keep it fast

		// Keep Sweeping until we reach a stable state (to keep it simple, we're
		// just checking that the number of constraints don't change)
		int numConstraints = 0;
		int iteration = 0;
		while (true) {
			// Get constraints for any bound datasets
			// --------------------------------------
			// Do this only on the initial iteration
			if (iteration == 0) {
				for (Link link : currentTemplate.getInputLinks()) {
					Variable var = link.getVariable();
					if (var.isDataVariable() && var.getBinding() != null) {
						// -- If data bindings are set, then get constraint
						// intersections
						ConstraintEngine engine = currentTemplate.getConstraintEngine();
						ArrayList<KBTriple> newConstraints = fetchDatasetConstraints(
								var.getBinding(), var);

						ArrayList<KBTriple> curConstraints = engine.getConstraints(var.getID());
						for (KBTriple cons : curConstraints) {
							for (KBTriple ncons : newConstraints) {
								if (cons.getPredicate().getID()
										.equals(ncons.getPredicate().getID())) {
									if (!cons.getObject().isLiteral()) {
									  if(ncons.getObject().isLiteral()
									      || !ncons.getObject().getNamespace().equals(
									          cons.getObject().getNamespace())
									      ) {
  										// If this value is already bound to a
  										// variable.
  										// - Then replace all occurences of the
  										// variable with the value
  										/*this.addExplanation("Setting ?"
  												+ cons.getObject().getName() + " " + " to "
  												+ ncons.getObject().getValue() + " because "
  												+ cons.getPredicate().getName() + " of "
  												+ var.getBinding().getName() + " is "
  												+ ncons.getObject().getValue());*/
  										engine.replaceObjectInConstraints(cons.getObject(),
  												ncons.getObject());
									  }
									}
									else if (!cons.getObject().getValue()
											.equals(ncons.getObject().getValue())) {
										// If this is already bound to a value,
										// then check that it is the same
										this.addExplanation("ERROR: Expecting the "
												+ cons.getPredicate().getName() + " of "
												+ var.getBinding().getName() + " to be "
												+ cons.getObject().getValue() + ", but it is "
												+ ncons.getObject().getValue());
										return null;
									}
								}
							}
						}
						engine.addConstraints(newConstraints);
					}
				}
			}

			ArrayList<String> nodesDone = new ArrayList<String>();
			ArrayList<Link> links = new ArrayList<Link>();

			// Do a light forward sweep
			// --------------------------------
			ArrayList<Variable> processedVars = new ArrayList<Variable>();
			for (Link link : currentTemplate.getInputLinks()) {
				Variable var = link.getVariable();
				processedVars.add(var);
				links.add(link);
			}
			while (!links.isEmpty()) {
				Link currentLink = links.remove(0);
				if (currentLink.isOutputLink())
					continue;

				Node originNode = currentLink.getDestinationNode();
				Link[] inputLinks = currentTemplate.getInputLinks(originNode);

				// Check that all inputs have been processed
				boolean allinputs_processed = true;
				for (Link inputLink : inputLinks) {
					Variable variable = inputLink.getVariable();
					if (!processedVars.contains(variable)) {
						links.add(currentLink);
						allinputs_processed = false;
						break;
					}
				}
				if (!allinputs_processed)
					continue;

				HashMap<Role, Variable> roleMap = new HashMap<Role, Variable>();

				ArrayList<String> variableIds = new ArrayList<String>();

				for (Link inputLink : inputLinks) {
					Variable variable = inputLink.getVariable();
					roleMap.put(inputLink.getDestinationPort().getRole(), variable);
					variableIds.add(variable.getID());
					links.remove(inputLink);
				}

				Link[] outputLinks = currentTemplate.getOutputLinks(originNode);
				for (Link outputLink : outputLinks) {
					Variable variable = outputLink.getVariable();
					roleMap.put(outputLink.getOriginPort().getRole(), variable);
					variableIds.add(variable.getID());
					processedVars.add(variable);
					links.add(outputLink);
				}

				if (nodesDone.contains(originNode.getID())) {
					// continue;
					// no op
				} else {
					ArrayList<KBTriple> redBox = currentTemplate.getConstraintEngine()
							.getConstraints(variableIds);

					ComponentVariable component = originNode.getComponentVariable();

					ComponentPacket sentMapsComponentDetails = new ComponentPacket(component,
							roleMap, redBox);
					ComponentPacket map = pc.findDataDetails(sentMapsComponentDetails);

					if (map == null) {
						currentTemplate = null;
						break;
					} else {
						this.addExplanations(map.getExplanations());
						if (map.isInvalid) {
							currentTemplate = null;
							break;
						}
						nodesDone.add(originNode.getID());
						done.put(currentTemplate, nodesDone);

						currentTemplate.getConstraintEngine().addConstraints(map.getRequirements());
						// this.modifyTemplate(currentTemplate, originNode, new
						// ComponentDetails[] { map });
					}
				}
			}
			if (currentTemplate == null)
				return null;

			// Do a light backward sweep
			// --------------------------------
			nodesDone.clear();
			links.clear();

			for (Link link : currentTemplate.getOutputLinks())
				links.add(link);

			while (!links.isEmpty()) {
				HashMap<Role, Variable> roleMap = new HashMap<Role, Variable>();

				Link currentLink = links.remove(0);
				if (currentLink.isInputLink()) {
					// continue;
					// no op
				} else {
					roleMap.put(currentLink.getOriginPort().getRole(), currentLink.getVariable());
					ArrayList<String> variableIds = new ArrayList<String>();
					Node originNode = currentLink.getOriginNode();

					Link[] outputLinks = currentTemplate.getOutputLinks(originNode);
					for (Link outputLink : outputLinks) {
						Variable variable = outputLink.getVariable();
						roleMap.put(outputLink.getOriginPort().getRole(), variable);
						variableIds.add(variable.getID());
						links.remove(outputLink);
					}

					Link[] inputLinks = currentTemplate.getInputLinks(originNode);
					for (Link inputLink : inputLinks) {
						Variable variable = inputLink.getVariable();
						roleMap.put(inputLink.getDestinationPort().getRole(), variable);
						variableIds.add(variable.getID());
						links.add(inputLink);
					}

					if (nodesDone.contains(originNode.getID())) {
						// continue;
						// no op
					} else {
						ArrayList<KBTriple> redBox = currentTemplate.getConstraintEngine()
								.getConstraints(variableIds);

						ComponentVariable component = originNode.getComponentVariable();

						ComponentPacket sentMapsComponentDetails = new ComponentPacket(component,
								roleMap, redBox);
						ComponentPacket map = pc.findDataDetails(sentMapsComponentDetails);

						if (map == null) {
							currentTemplate = null;
							break;
						} else {
							this.addExplanations(map.getExplanations());
							if (map.isInvalid) {
								currentTemplate = null;
								break;
							}
							nodesDone.add(originNode.getID());
							done.put(currentTemplate, nodesDone);

							currentTemplate.getConstraintEngine().addConstraints(
									map.getRequirements());
							// this.modifyTemplate(currentTemplate, originNode,
							// new ComponentDetails[] { map });
						}
					}
				}
			}

			if (currentTemplate == null)
				return null;

			iteration++;

			int newNumConstraints = currentTemplate.getConstraintEngine().getConstraints().size();
			if (newNumConstraints == numConstraints)
				break;

			if (iteration == MAXITERATIONS)
				break;
			numConstraints = newNumConstraints;
		}

		return currentTemplate;
	}

	
	@Override
	/**
	 * Create an Execution Plan from the Expanded Template
	 * @param template The Expanded Template
	 * @return The Execution Plan
	 */
	public ExecutionPlan getExecutionPlan(Template template) {
		try {
			String planid = UuidGen.generateURIUuid((URIEntity)template);
			ExecutionPlan plan = PlanFactory.createExecutionPlan(planid, props);

			HashMap<Node, ExecutionStep> nodeMap = new HashMap<Node, ExecutionStep>();
			for(Node n : template.getNodes()) {
			  if(n.isInactive()) {
			    plan.setIsIncomplete(true);
			    continue;
			  }
			  
				ExecutionStep step = PlanFactory.createExecutionStep(n.getID(), props);
				step.setMachineIds(n.getMachineIds());
				// TODO: Plan should save internally using runOn/canRunOn
				
				ComponentVariable c = n.getComponentVariable();

				LinkedHashMap<Role, Variable> roleMap = new LinkedHashMap<Role, Variable>();
				for (Link outputLink : template.getOutputLinks(n)) {
				  Role or = outputLink.getOriginPort().getRole();
				  Role r = new Role(or.getID());
				  r.setRoleId(or.getRoleId());
					roleMap.put(r, outputLink.getVariable());
				}
				for (Link inputLink : template.getInputLinks(n)) {
          Role dr = inputLink.getDestinationPort().getRole();
          Role r = new Role(dr.getID());
          r.setRoleId(dr.getRoleId());
					roleMap.put(dr, inputLink.getVariable());
				}

				ComponentPacket mapsComponentDetails = new ComponentPacket(c, roleMap, new ArrayList<KBTriple>());

				// Query 4.5
				ComponentInvocation invocation = this.pc.getComponentInvocation(mapsComponentDetails);

				if(invocation == null) {
					System.err.println("Cannot create invocation for "+c.getBinding());
					return null;
				}
				
				ExecutionCode code = new ExecutionCode(invocation.getComponentId());
				code.setLocation(invocation.getComponentLocation());
				code.setCodeDirectory(invocation.getComponentDirectory());
				step.setCodeBinding(code);
				
				HashMap<String, ArrayList<Object>> argMaps = new HashMap<String, ArrayList<Object>>(); 
				for(ComponentInvocation.Argument arg : invocation.getArguments()) {
					ArrayList<Object> cur = argMaps.get(arg.getName());
					if(cur == null)
						cur = new ArrayList<Object>();
					if(arg.getValue() instanceof Binding) {
						Binding b = (Binding)arg.getValue();
						String varid = arg.getVariableid();
						ExecutionFile file = new ExecutionFile(varid);
						
						String location = dc.getDataLocation(b.getID());
						if(location == null) {
							location = dc.getDefaultDataLocation(b.getID());
						}
						file.setLocation(location);
						file.setBinding(b.getName());
						if(arg.isInput())
							step.addInputFile(file);
						else
							step.addOutputFile(file);
						cur.add(file);
					}
					else {
						cur.add(arg.getValue().toString());
					}
					argMaps.put(arg.getName(), cur);
				}
				step.setInvocationArguments(argMaps);
				plan.addExecutionStep(step);
				nodeMap.put(n,  step);
			}
			// Add Parent Steps
			for(Node n : template.getNodes()) {
			  if(n.isInactive())
			    continue;
				ExecutionStep step = nodeMap.get(n);
				for(Link l : template.getInputLinks(n)) {
					ExecutionStep parentStep = nodeMap.get(l.getOriginNode());
					if(parentStep != null)
					  step.addParentStep(parentStep);
				}
			}
			return plan;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Return an Expanded template
	 * (i.e. a template with all component and data collections expanded)
	 * Note this template differs slightly from other templates in the following ways:
	 * 		- No collections at all (component or data)
	 * 		- There can be more than one link to a port (i.e. expansion of a collection as an input to that port)
	 */
	@SuppressWarnings("unchecked")
  public Template getExpandedTemplate(Template template) {
		Template curt = new TemplateKB((TemplateKB)template);
		curt.setID(UuidGen.generateURIUuid((URIEntity)template));
		// Convert to execution prefix id
		curt.setID(this.exPrefix + "/" + curt.getName() + ".owl#" + curt.getName());
		
		String ns = curt.getNamespace();
		
		int jobCounter = 0;
		ArrayList<Node> nodesDone = new ArrayList<Node>();
		HashMap<String, Variable> newVariables = new HashMap<String, Variable>();
		
		// Investigate input links
		ArrayList<Link> investigateLinks = new ArrayList<Link>();
		for (Link link : template.getInputLinks()) {
			investigateLinks.add(link);
		}
		while (!investigateLinks.isEmpty()) {
			Link currentLink = investigateLinks.remove(0);
			if (!currentLink.isOutputLink()) {
				Node destNode = currentLink.getDestinationNode();
				
				if (!nodesDone.contains(destNode)) {
					// Check all input links to this node have been already processed
					// If not, re-add back to queue
					Link[] inputLinks = template.getInputLinks(destNode);
					boolean all_inputs_processed = true;
					for (Link inputLink : inputLinks) {
						if(inputLink.getOriginNode() != null && !nodesDone.contains(inputLink.getOriginNode())) {
							all_inputs_processed = false;
							break;
						}
					}
					if(!all_inputs_processed) {
						investigateLinks.add(currentLink);
						continue;
					}
					nodesDone.add(destNode);
					ComponentVariable component = destNode.getComponentVariable();
			     
					boolean compCollection = false;
					// Expand the component bindings into multiple nodes
					ArrayList<Binding> cbindings = new ArrayList<Binding>();
					cbindings.add(component.getBinding());
					while (!cbindings.isEmpty()) {
						Binding cbinding = cbindings.remove(0);
						if (cbinding.isSet()) {
						  if(cbinding.size() > 1)
						    compCollection = true;
							for (WingsSet s : cbinding) {
								cbindings.add((Binding) s);
							}
						} else {
							ComponentVariable c = new ComponentVariable(ns+cbinding.getName());
							c.setConcrete(true);
							c.setBinding(new Binding(cbinding.getID()));
							//c.setBinding(new Binding(ns + cbinding.getName()));
							
							// Create a new Node
							Node newNode = curt.addNode(c);
							newNode.setDerivedFrom(destNode.getID());
							
							//newNode.addComponentSetRule(destNode.getComponentSetRule());
							//newNode.addPortSetRule(destNode.getPortSetRule());
							newNode.setComment(destNode.getComment());

							newNode.setMachineIds((ArrayList<String>)
							    cbinding.getData("machineIds"));
							
							// Get data bindings for this component binding
							PortBindingList pb = (PortBindingList) cbinding.getData();
							
							// Check all input links to this node
							for (Link inputLink : inputLinks) {
								Variable variable = inputLink.getVariable();
								
								Port oldport = inputLink.getDestinationPort();
								String portid = ns + oldport.getName() + "_" + 
								    String.format("%04d", jobCounter);
								
								Port newPort = newNode.findInputPort(portid);
								// Create a new port
								if(newPort == null) {
									newPort = new Port(portid);
									Role r = new Role(ns + oldport.getRole().getName());
									r.setDimensionality(0);
									r.setRoleId(oldport.getRole().getRoleId());
									r.setType(oldport.getRole().getType());
									newPort.setRole(r);
									newNode.addInputPort(newPort);
								}
								
								// Get port bindings
								Binding xb = getPortBinding(pb.getPortBinding(), inputLink.getDestinationPort());
								
								// Check if we've already created a variable for this binding 
								// (or it's sub-bindings in case the variable binding is a collection)
								ArrayList<Binding> queue = new ArrayList<Binding>();
								queue.add(xb);
								boolean dataCollection = false;
								while(!queue.isEmpty()) {
									Binding cb = queue.remove(0);
									// Expanding collections into multiple variables
									if(cb.isSet()) {
									  if(cb.size() > 1)
									    dataCollection = true;
										for(WingsSet sb : cb) {
											queue.add((Binding)sb);
										}
										continue;
									}
									
	                if(variable.isBreakpoint()) {
	                  ExecutionFile file = new ExecutionFile(cb.getID());
	                  String loc = dc.getDataLocation(cb.getID());
	                  if(loc == null)
	                    loc = dc.getDefaultDataLocation(cb.getID());
	                  file.setLocation(loc);
	                  file.loadMetadataFromLocation();
	                  // If there is no metadata for a breakpoint
	                  // Then mark the destination as inactive
	                  if(file.getMetadata().isEmpty() && destNode != null)
	                    newNode.setInactive(true);
	                  //System.out.println(cb.getName()+"="+file.getMetadata());
	                }
                  
									String varkey = cb.isValueBinding() ? variable.getName() : "";
									varkey += cb.toString();
									
									Variable newVariable = newVariables.get(varkey);
									if(newVariable != null) {
										// Variable exists. Get it's links and modify them
										Link[] curlinks = curt.getLinks(newVariable);
										// If current link has no destination
										// - Modify link to point to the new node as destination
										// Else, add new links
										for(Link l : curlinks) {
		                  // If origin in inactive, set destination as inactive
		                  if(l.getOriginNode() != null &&
		                      l.getOriginNode().isInactive()) {
		                    newNode.setInactive(true);
		                  }
											if(l.getDestinationNode() == null) {
												l.setDestinationNode(newNode);
												l.setDestinationPort(newPort);
											}
											else {
												Link curl = curt.getLink(l.getOriginNode(), newNode,  l.getOriginPort(), newPort);
												if(curl == null || !newVariable.equals(curl.getVariable())) 
													curt.addLink(l.getOriginNode(), newNode, l.getOriginPort(), newPort, newVariable);
											}
										}
									}
									else {
										// Input Variable doesn't exist. Create a new variable and link
										newVariable = curt.addVariable(ns + variable.getName(), 
										    variable.getVariableType(),
										    dataCollection || compCollection);
										newVariable.setBinding(cb);
										newVariable.setDerivedFrom(variable.getID());
										newVariables.put(varkey, newVariable);
										
										// Add new input link
										curt.addLink(null, newNode, null, newPort, newVariable);
										
										// Add Binding metrics as constraints
										curt.getConstraintEngine().addConstraints(
												this.convertMetricsToTriples(cb.getMetrics(), newVariable.getID()));
                  }
								}
							}

							// Get outputs from this node
							Link[] outputLinks = template.getOutputLinks(destNode);
							for (Link outputLink : outputLinks) {
								Variable variable = outputLink.getVariable();
								
								Port oldport = outputLink.getOriginPort();
								String portid = ns + oldport.getName() + "_" + jobCounter;
								Port newPort = newNode.findOutputPort(portid);
								// Create a new port
								if(newPort == null) {
									newPort = new Port(portid);
									Role r = new Role(ns + oldport.getRole().getName());
									r.setDimensionality(0);
									r.setRoleId(oldport.getRole().getRoleId());
									r.setType(oldport.getRole().getType());
									newPort.setRole(r);
									newNode.addOutputPort(newPort);
								}
								
								// Get port bindings
								Binding xb = getPortBinding(pb.getPortBinding(), outputLink.getOriginPort());

								// Add links for all non-collection variable bindings
								ArrayList<Binding> queue = new ArrayList<Binding>();
								queue.add(xb);
								boolean dataCollection = false;
								while(!queue.isEmpty()) {
									Binding cb = queue.remove(0);
									if(cb == null)
										continue;
									if(cb.isSet()) {
									  if(cb.size() > 1)
									    dataCollection = true;
										for(WingsSet sb : cb)
											queue.add((Binding)sb);
										continue;
									}

									// Check for existing variable
									String varkey = cb.isValueBinding() ? variable.getName() : "";
									varkey += cb.toString();
									
									Variable newVariable = newVariables.get(varkey);
									if (newVariable == null) {
										// Create a new variable
										newVariable = curt.addVariable(ns + variable.getName(), 
										    variable.getVariableType(),
										    compCollection || dataCollection);
										newVariable.setBinding(cb);
										newVariable.setDerivedFrom(variable.getID());
										newVariables.put(varkey, newVariable);
									}
									
									// Add new output link
									curt.addLink(newNode, null, newPort, null, newVariable);

									// Add Binding metrics as constraints
									curt.getConstraintEngine().addConstraints(
											this.convertMetricsToTriples(cb.getMetrics(), newVariable.getID()));
								}

								// Add outputLink to Navigate and investigate further
								if(!investigateLinks.contains(outputLink))
									investigateLinks.add(outputLink);
							}

							jobCounter++;
						}
					}
				}
			}
		}
		return curt;
	}
	

	/*
	 * 
	 * Private Helper functions below
	 * 
	 */
	
	@SuppressWarnings("unused")
	private boolean hasEmptyPortBindings(Binding cb) {
		ArrayList<Binding> cbindings = new ArrayList<Binding>();
		cbindings.add(cb);
		while (!cbindings.isEmpty()) {
			Binding cbinding = cbindings.remove(0);
			if (cbinding.isSet()) {
				for (WingsSet s : cbinding) {
					cbindings.add((Binding)s);
				}
			} else {
				// Get data bindings for this component binding
				PortBindingList pb = (PortBindingList) cbinding.getData();
				if(pb.isEmpty())
					return true;
			}
		}
		return false;
	}
	
	private Binding getPortBinding(PortBinding pb, Port port) {
		for (Port p : pb.keySet()) {
			if (p.getID().equals(port.getID()))
				return pb.get(p);
		}
		return null;
	}
	
	private void addExplanations(ArrayList<String> exp) {
		this.explanations.addAll(exp);
	}

	private void addExplanation(String exp) {
		this.explanations.add(exp);
	}
	
	
	/**
	 * modifies the template by replacing abstract component (if any) with a
	 * concrete component and adds constraints. Note: For component sets: Union
	 * the variables, and intersect the constraints
	 * 
	 * @param template
	 *            the template to modify
	 * @param node
	 *            the node to modify
	 * @param cmrs
	 *            an array of ComponentMapAndRequirements (the component,
	 *            roleMap, outputMaps, and constraints)
	 */
	private boolean modifyTemplate(Template template, Node node, ComponentPacket[] cmrs) {

		Node tNode = template.getNode(node.getID());

		HashSet<String> curbindingIds = new HashSet<String>();
		if (tNode.getComponentVariable().getBinding() != null)
			for (WingsSet b : tNode.getComponentVariable().getBinding()) {
				curbindingIds.add(((Binding) b).getID());
			}

		// Combine all cmrs into one cmr:
		// -- union of variables (maps), and intersection of their constraints
		// (redboxes)

		HashMap<String, Variable> uMap = new HashMap<String, Variable>();
		HashMap<String, Boolean> inputRoles = new HashMap<String, Boolean>();

		ArrayList<KBTriple> uRedbox = new ArrayList<KBTriple>();
		Binding uBinding = null;

		int i = 0;
		for (ComponentPacket cmr : cmrs) {
			ComponentVariable component = cmr.getComponent();

			// FIXME: Handle Template Bindings ?
			if (!curbindingIds.isEmpty() && !curbindingIds.contains(component.getBinding().getID()))
				continue;
			if (!component.isTemplate()) {
				if (uBinding == null)
					uBinding = new Binding();
				uBinding.add(new Binding(component.getBinding().getID()));
			} else {
				if (uBinding == null)
					uBinding = new ValueBinding();
				uBinding.add(new ValueBinding(component.getTemplate()));
			}

			HashMap<String, Variable> map = cmr.getStringRoleMaps();
			uMap.putAll(map);

			for (Role r : cmr.getRoleMap().keySet()) {
				inputRoles.put(r.getRoleId(), cmr.isInputRole(r.getRoleId()));
			}

			if (i > 0) {
				// TODO: Mark variables that are not common (?)
			}

			ArrayList<KBTriple> redbox = cmr.getRequirements();
			ArrayList<String> redboxStr = new ArrayList<String>();
			for (KBTriple kbTriple : redbox) {
				redboxStr.add(kbTriple.fullForm());
			}

			if (i == 0) {
				uRedbox.addAll(redbox);
			} else {
				for (KBTriple kbTriple : new ArrayList<KBTriple>(uRedbox)) {
					if (!redboxStr.contains(kbTriple.fullForm())) {
						uRedbox.remove(kbTriple);
					}
				}
			}
			i++;
		}
		if (uBinding == null)
			return false;

		tNode.getComponentVariable().setBinding(uBinding);
		tNode.getComponentVariable().setConcrete(true);

		HashMap<String, String> idChanged = new HashMap<String, String>();

		// Specialize Link ComponentParams, & Create New Links if there are any
		// *New* variables introduced
		HashMap<String, Role> nodeRoles = new HashMap<String, Role>();
		for (Port p : node.getInputPorts())
			nodeRoles.put(p.getRole().getRoleId(), p.getRole());
		for (Port p : node.getOutputPorts())
			nodeRoles.put(p.getRole().getRoleId(), p.getRole());

		for (String sRoleId : uMap.keySet()) {
			Variable svar = uMap.get(sRoleId);
			Role r = nodeRoles.get(sRoleId);
			if (r == null) {
				// If new argument found, then create a new Link
				
				boolean isInput = inputRoles.get(sRoleId);
				
				// Create a new variable
				String varid = template.getNamespace() + svar.getName();
				Variable var = template.addVariable(varid, svar.getVariableType());
				idChanged.put(svar.getID(), varid);

				// Create a new Port
				int count = 1;
				String ptype = isInput ? "ip" : "op";
				String p_prefix = node.getID() + "_" + sRoleId + "_" + ptype;
				while ((isInput && node.findInputPort(p_prefix + count) != null)
						|| (!isInput && node.findOutputPort(p_prefix + count) != null)) {
					count++;
				}
				Port p = new Port(p_prefix + count);
				
				// Create a new role for the port
				r = new Role(p.getID() + "_role");
				r.setRoleId(sRoleId);
				p.setRole(r);

				// Create a new Link
				if (isInput)
					template.addLink(null, node, null, p, var);
				else
					template.addLink(node, null, p, null, var);
				
			} else {
				// If a variable already exists for this argument/role
				// Copy over data object binding to existing variable
				Variable itvar = template.getVariable(svar.getID());
				if (itvar != null) {
					itvar.setBinding(svar.getBinding());
				}
				else {
					// This isn't good
					System.err.println("Unknown variable in template !");
				}
			}
		}
		// End for (String scp : uMap.keySet())

		ArrayList<KBTriple> newRedBox = new ArrayList<KBTriple>();
		for (KBTriple kbTriple : uRedbox) {
			if (idChanged.containsKey(kbTriple.getSubject().getID())) {
				String newid = idChanged.get(kbTriple.getSubject().getID());
				kbTriple.setSubject(template.getConstraintEngine().getResource(newid));
			}
			if (idChanged.containsKey(kbTriple.getObject().getID())) {
				String newid = idChanged.get(kbTriple.getObject().getID());
				kbTriple.setObject(template.getConstraintEngine().getResource(newid));
			}
			newRedBox.add(kbTriple);
		}

		template.getConstraintEngine().addConstraints(newRedBox);
		return true;
	}

	private LogEvent getEvent(String evid) {
		return new LogEvent(evid, "Wings", LogEvent.REQUEST_ID, this.request_id);
	}

	/**
	 * Helper function to Group variable object bindings by the variable ids
	 * passed in
	 * 
	 * @param listOfVariableObjectBindings
	 *            the list of variable object bindings received from the Data
	 *            Catalog
	 * @param groupByVarIds
	 *            list of variable ids that the bindings should be grouped by
	 * 
	 * @return the list of variable object bindings grouped by groupByIds
	 */

	private ArrayList<VariableBindingsList> groupVariableDataObjectMappings(
			ArrayList<VariableBindingsList> listsOfVariableDataObjectMappings,
			ArrayList<String> groupByVarIds, ArrayList<String> variableIds) {
		HashMap<String, VariableBindingsList> bindingsByGroupByVarIds = new HashMap<String, VariableBindingsList>();

		for (VariableBindingsList mapping : listsOfVariableDataObjectMappings) {
			HashMap<String, HashSet<KBObject>> varBindings = new HashMap<String, HashSet<KBObject>>();
			for (VariableBindings dvobinding : mapping) {
				KBObject dv = dvobinding.getDataVariable();
				HashSet<KBObject> objs = dvobinding.getDataObjects();
				varBindings.put(dv.getID(), objs);
			}
			String groupKey = "";
			for (String varId : groupByVarIds) {
				groupKey += varBindings.get(varId) + "|";
			}

			VariableBindingsList om = bindingsByGroupByVarIds.get(groupKey);
			if (om == null) {
				om = mapping;
			} else {
				for (VariableBindings dvobinding : om) {
					KBObject dv = dvobinding.getDataVariable();
					HashSet<KBObject> obj = dvobinding.getDataObjects();
					if (variableIds.contains(dv.getID()) && !groupByVarIds.contains(dv.getID())) {
						obj.addAll(varBindings.get(dv.getID()));
					}
					varBindings.put(dv.getID(), obj);
					dvobinding.setDataObjects(obj);
				}
			}
			bindingsByGroupByVarIds.put(groupKey, om);
		}
		ArrayList<VariableBindingsList> groupedList = new ArrayList<VariableBindingsList>();
		for (VariableBindingsList vals : bindingsByGroupByVarIds.values()) {
			groupedList.add(vals);
		}
		return groupedList;
	}

	/**
	 * Helper function to Filter variable object bindings by any explicit
	 * binding constraint specified in the template
	 * 
	 * @param listOfVariableObjectBindings
	 *            the list of variable object bindings received from the Data
	 *            Catalog
	 * @param groupByVarIds
	 *            list of variable ids that the bindings should be grouped by
	 * 
	 * @return the list of variable object bindings grouped by groupByIds
	 */

	private ArrayList<VariableBindingsList> filterVariableDataObjectMappings(
			ArrayList<VariableBindingsList> fullList, ArrayList<String> variableIds,
			HashMap<String, HashSet<String>> userBindings,
			HashMap<String, ArrayList<String>> varEquality,
			HashMap<String, ArrayList<String>> varInequality) {
		ArrayList<VariableBindingsList> filteredList = new ArrayList<VariableBindingsList>();

		for (VariableBindingsList mapping : fullList) {
			HashMap<String, String> varBindings = new HashMap<String, String>();
			for (VariableBindings dvobinding : mapping) {
				KBObject dv = dvobinding.getDataVariable();
				HashSet<KBObject> objs = dvobinding.getDataObjects();
				for (KBObject obj : objs)
					varBindings.put(dv.getID(), obj.getID());
			}

			boolean ok = true;
			for (String dvid : variableIds) {
				String dvB = varBindings.get(dvid);

				// Check that the dc provided bindings follow any user defined
				// databinding constraints
				HashSet<String> udvB = userBindings.get(dvid);
				if (udvB != null) {
					if (!udvB.contains(dvB)) {
						ok = false;
						break;
					}
				}

				// Check that the dc provided bindings follow any varEquality
				// and varInequality constraints
				ArrayList<String> eqvars = varEquality.get(dvid);
				if (eqvars != null) {
					for (String eqv : eqvars) {
						if (!dvB.equals(varBindings.get(eqv))) {
							ok = false;
							break;
						}
					}
					if (!ok)
						break;
				}
				ArrayList<String> ineqvars = varInequality.get(dvid);
				if (ineqvars != null) {
					for (String ineqv : ineqvars) {
						if (dvB.equals(varBindings.get(ineqv))) {
							ok = false;
							break;
						}
					}
					if (!ok)
						break;
				}
			}
			if (ok)
				filteredList.add(mapping);
		}
		return filteredList;
	}


	private void setBindingMetrics(Binding binding, HashMap<String, Metrics> metricsMap,
			LogEvent event) {
		if (binding == null)
			return;

		if (binding.isSet()) {
			for (WingsSet b : binding) {
				setBindingMetrics((Binding) b, metricsMap, event);
			}
			return;
		}

		if (!binding.getID().startsWith(this.dataNS))
			return;

		String dataObjectName = binding.getName();
		Metrics metrics = metricsMap.get(dataObjectName);
		if (metrics == null) {
			String dataObjectId = binding.getID();
			if (event != null)
				logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.1")
						.addWQ(LogEvent.QUERY_ARGUMENTS, dataObjectId));
			metrics = dc.findDataMetricsForDataObject(dataObjectId);
			if (metrics != null) {
				metricsMap.put(dataObjectName, metrics);
				if (event != null)
					logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.1")
							.addWQ(LogEvent.QUERY_RESPONSE, "<metrics not shown>"));
			} else if (event != null) {
				logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.1")
						.addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
			}
		}
		binding.setMetrics(metrics);
	}

	private ComponentPacket cloneCMRBindings(ComponentPacket cmr) {
		// Clone cmr before sending.
		// We just need to have separate variable bindings
		HashMap<Role, Variable> roleMap = cmr.getRoleMap();
		HashMap<Role, Variable> sendMap = new HashMap<Role, Variable>();

		for (Role r : roleMap.keySet()) {
			Variable var = roleMap.get(r);
			Variable sendVar = new Variable(var.getID(), var.getVariableType());
			sendVar.setBreakpoint(var.isBreakpoint());
			if (var.getBinding() != null)
				sendVar.setBinding((Binding) var.getBinding().clone());
			sendMap.put(r, sendVar);
		}
		ComponentPacket pcmr = new ComponentPacket(cmr.getComponent(), sendMap,
				cmr.getRequirements());
		pcmr.addExplanations(cmr.getExplanations());
		pcmr.setInvalidFlag(cmr.getInvalidFlag());
		return pcmr;
	}

	private KBObject fetchVariableTypeFromCMR(Variable v, ComponentPacket cmr) {
		KBObject vtype = null;
		for (KBTriple t : cmr.getRequirements()) {
			if (t.getSubject().getID().equals(v.getID())
					&& t.getPredicate().getName().equals("type")) {
				KBObject tobj = t.getObject();
				if (vtype == null)
					vtype = tobj;
				else if (dc.checkDatatypeSubsumption(vtype.toString(), tobj.toString())) {
					vtype = tobj;
				}
			}
		}
		return vtype;
	}

	// TODO: Add Another function for comparison purposes, where the components
	// are evaluated last
	// -- i.e. innermost loop

	/*
	 * Helper function - Takes input portbindinglist - Returns output
	 * portbindinglist
	 * 
	 * Currently ignores variable constraint forward propagation - relies on
	 * binding metrics forward propagation
	 * 
	 * Handles component sets inside newNode
	 * 
	 * TODO: Handle multiple parameters and parameter SetCreationRules
	 * 
	 * TODO: Handle extra ports for specialized components (whether bindings
	 * exist for them or not)
	 */
	private PortBindingList configureBindings(PortBindingList ipblist, Node origNode, Node newNode,
			ComponentVariable component, ComponentPacket cmr, LogEvent event,
			HashMap<String, String> prospectiveIds) {

		PortBindingList pblist = new PortBindingList();

		// Handle the input port binding list
		if (ipblist.isList()) {
			// For all elements in the port binding list
			// - add a new component binding (component instantiation)
			// - call configureBindings
			// - add the returned output binding list to our own output
			// binding list

			Binding listb = new Binding();
			Binding origB = component.getBinding();
			for (PortBindingList ipbl : ipblist) {
				ComponentPacket pcmr = cloneCMRBindings(cmr);
				pcmr.setComponent(component);
				component.setBinding(origB);
				PortBindingList opbl = configureBindings(ipbl, origNode, newNode, component, pcmr,
						event, prospectiveIds);

				if ((opbl.isList() && !opbl.isEmpty())
						|| (!opbl.isList() && opbl.getPortBinding() != null)) {
					pblist.add(opbl);
					listb.add(component.getBinding());
				}
			}
			component.setBinding(listb);
		} else {
			// if(component.getBinding() == null) return opblist;

			// For all component bindings
			int len = component.getBinding().size();

			Binding allCB = component.getBinding();
			component.setBinding(new Binding());

			for (WingsSet cbs : allCB) {
				PortBindingList cpblist = new PortBindingList();
				Binding compBinding = (Binding) cbs;

				Binding cb;
				if (!component.isTemplate())
					cb = new Binding(compBinding.getID());
				else
					cb = new ValueBinding((Template) compBinding.getValue());

				ComponentVariable c;
				if (component.isTemplate())
					c = new ComponentVariable(component.getTemplate());
				else {
					c = new ComponentVariable(component.getID());
					c.setBinding(new Binding(compBinding.getID()));
				}
				c.setBinding(cb);

				ComponentPacket ccmr = cloneCMRBindings(cmr);
				ccmr.setComponent(c);

				PortBinding pb = ipblist.getPortBinding();
				HashMap<String, Boolean> inputRoles = new HashMap<String, Boolean>();
				for (Port p : pb.keySet()) {
					inputRoles.put(p.getRole().getRoleId(), true);
				}

				// Create new Ports
				HashMap<String, Port> new_sRolePortMap = new HashMap<String, Port>();
				for (Port p : newNode.getInputPorts())
					new_sRolePortMap.put(p.getRole().getRoleId(), p);
				for (Port p : newNode.getOutputPorts())
					new_sRolePortMap.put(p.getRole().getRoleId(), p);

				HashMap<String, Port> old_sRolePortMap = new HashMap<String, Port>();
				for (Port p : origNode.getInputPorts())
					old_sRolePortMap.put(p.getRole().getRoleId(), p);
				for (Port p : origNode.getOutputPorts())
					old_sRolePortMap.put(p.getRole().getRoleId(), p);

				// Create/modify the roleMap
				HashMap<Role, Variable> roleMap = ccmr.getRoleMap();
				for (Role r : roleMap.keySet()) {
					// The new node's input variable binding is now set to
					// the portBinding of the old node's port
					// (The portBindings are split up when collections are
					// created)
					if (inputRoles.containsKey(r.getRoleId()))
						roleMap.get(r).setBinding(pb.get(old_sRolePortMap.get(r.getRoleId())));
				}

				if (logger.isInfoEnabled()) {
					HashMap<String, Object> args = new HashMap<String, Object>();
					args.put("component", ccmr.getComponent());
					args.put("roleMap", ccmr.getRoleMap());
					logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.2")
							.addMap(LogEvent.QUERY_ARGUMENTS, args));
				}

				ComponentReasoningAPI pc = this.pc;
				if (ccmr.getComponent().isTemplate())
					pc = this.tc;
        
				// No new roles introduced by the forward sweep call
				ArrayList<ComponentPacket> allcmrs = pc.findOutputDataPredictedDescriptions(ccmr);
        
				ArrayList<ComponentPacket> rcmr = new ArrayList<ComponentPacket>();
        for (ComponentPacket acmr : allcmrs) {
          this.addExplanations(acmr.getExplanations());
          if (!acmr.getInvalidFlag())
            rcmr.add(acmr);
          else {
            // Do something with the invalid components
          }
        }

				if (rcmr.isEmpty()) {
					logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.2")
							.addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
					continue;
				} else {
					logger.info(event
							.createLogMsg()
							.addWQ(LogEvent.QUERY_NUMBER, "4.2")
							.addWQ(LogEvent.QUERY_RESPONSE,
									"Returned " + rcmr.size() + " responses"));

					// FIXME: Handle parameter sets properly ! (use rules too !)

					// Handle multiple return values
					for (int i = 0; i < rcmr.size(); i++) {
						ComponentPacket m = rcmr.get(i);

						ArrayList<String> machineIds = 
						    this.rc.getMatchingMachineIds(
						        m.getComponent().getRequirements());
						if(machineIds.size() == 0) {
						  this.addExplanation("ERROR: Could not find a suitable machine "+
						      "to run "+m.getComponent().getName());
						  continue;
						}
            cb.setData("machineIds", machineIds);

						PortBinding newpb = new PortBinding();
						// PortBinding opb = new PortBinding();
						HashMap<Role, Variable> mRoleMap = m.getRoleMap();
						for (Role r : mRoleMap.keySet()) {
							if(m.isInputRole(r.getRoleId())) {
								Variable v = mRoleMap.get(r);
								newpb.put(new_sRolePortMap.get(r.getRoleId()), v.getBinding());
							}
						}
						String sortedInputs = getInputRoleStr(newpb, ccmr.getComponent());

						for (Role r : mRoleMap.keySet()) {
							// For outputs
							if(!m.isInputRole(r.getRoleId())) {
								Variable v = mRoleMap.get(r);

								Binding b = v.getBinding();
								newpb.put(new_sRolePortMap.get(r.getRoleId()), b);
								// opb.put(newRolePort.get(r.getRoleId()), b);

								// Rename outputs
								ArrayList<Binding> dbs = new ArrayList<Binding>();
								HashMap<String, String> indices = new HashMap<String, String>(); 
								dbs.add(b);
								while (!dbs.isEmpty()) {
									Binding db = dbs.remove(0);
									String sfx = indices.get(db.getID());
									sfx = sfx != null ? sfx : "";
									if (db.isSet()) {
										int ind = 1;
										for (WingsSet s : db) {
											Binding sb = (Binding) s;
											sb.setID(db.getID() + "-" + ind);
											dbs.add(sb);
											indices.put(sb.getID(), sfx + "-" +ind);
											ind++;
										}
									} else {
										KBObject vtype = fetchVariableTypeFromCMR(v, ccmr);
										Binding ds = createNewBinding(v, db, vtype, r,
												sortedInputs, event);
										dc.getDataLocation(ds.getID());
										db.setID(ds.getID() + sfx);
										
										// For each output, fetch actual metrics from .met file 
										// and override predicted metrics
										Metrics newm = this.dc.fetchDataMetricsForDataObject(db.getID());
					          logger.info(event
					              .createLogMsg()
					              .addWQ(LogEvent.QUERY_NUMBER, "4.x")
					              .addWQ(LogEvent.QUERY_RESPONSE,
					                  "Returned " + newm));
										db.getMetrics().getMetrics().putAll(newm.getMetrics());
									}
								}
							}
						}
						if (rcmr.size() == 1 && i == 0) {
							// Return the portbindings
							cpblist = new PortBindingList(newpb);

							// Associate the port binding with this particular
							// component binding
							cb.setData(cpblist);
						} else {
							PortBindingList cbl = (PortBindingList) compBinding.getData();
							if (cbl == null)
								cbl = new PortBindingList();
							PortBindingList tmp = new PortBindingList(newpb);
							cbl.add(tmp);
							cpblist.add(tmp);
							cb.setData(cbl);
						}
						component.getBinding().add(cb);
					}
				}
				if (len > 1) {
					if (!cpblist.isEmpty() || (cpblist.getPortBinding() != null))
						pblist.add(cpblist);
				} else {
					pblist = cpblist;

					if (component.getBinding().size() == 1)
						component.setBinding((Binding) component.getBinding().get(0));
				}
			}
		}

		return pblist;
	}

	private void removeComponentBindingsWithNoData(ComponentVariable c) {
		HashMap<Binding, Binding> parentOf = new HashMap<Binding, Binding>();
		ArrayList<Binding> ocbs = new ArrayList<Binding>();
		ocbs.add(c.getBinding());

		while (!ocbs.isEmpty()) {
			Binding ocb = ocbs.remove(0);

			if (ocb.isSet()) {
				for (WingsSet os : ocb) {
					Binding osb = (Binding) os;
					ocbs.add(osb);
					parentOf.put(osb, ocb);
				}
			} else {
				PortBindingList opblist = (PortBindingList) ocb.getData();
				if (opblist == null)
					parentOf.get(ocb).remove(ocb);
			}
		}
	}

	// Do a multi-dimensional binding dependency deletion (not completely
	// necessary, but useful for efficiency)
	private boolean removeProducersWithNoConsumers(Template template) {
		ArrayList<String> nodesDone = new ArrayList<String>();

		ArrayList<Link> links = new ArrayList<Link>();
		for (Link link : template.getOutputLinks()) {
			links.add(link);
		}

		while (!links.isEmpty()) {
			Link l = links.remove(0);
			if (l.isInputLink())
				continue;

			Node on = l.getOriginNode();
			if (nodesDone.contains(on.getID()))
				continue;
			nodesDone.add(on.getID());

			if (l.isInOutLink()) {
				Node dn = l.getDestinationNode();
				ComponentVariable oc = on.getComponentVariable();
				ComponentVariable dc = dn.getComponentVariable();

				ArrayList<Binding> ocbs = new ArrayList<Binding>();
				ocbs.add(oc.getBinding());

				HashMap<Binding, Binding> parentb = new HashMap<Binding, Binding>();

				Port op = l.getOriginPort();
				//Port dp = l.getDestinationPort();
				//Get all destination ports that are fed the same variable
				Link[] all_links = template.getLinks(on, dn);
				ArrayList<Port> dps = new ArrayList<Port>();
				for(Link link: all_links) {
				  if(link.getVariable().getID().equals(l.getVariable().getID()))
				    dps.add(link.getDestinationPort());
				}

				while (!ocbs.isEmpty()) {
					Binding ocb = ocbs.remove(0);
					Binding oxb = null;
					
					if (ocb.isSet()) {
						for (WingsSet os : ocb) {
							Binding osb = (Binding) os;
							ocbs.add(osb);
							parentb.put(osb, ocb);
						}
					} else {
						boolean ok = false;
						PortBindingList opblist = (PortBindingList) ocb.getData();

						if (opblist != null) {
							PortBinding opb = opblist.getPortBinding();
							oxb = opb.getById(op.getID());
							if (oxb == null)
								continue;

							// For each port-binding in dcb, check that there
							// exists a corresponding port-binding in ocb
							// - If not, then that ocb item must be deleted
							
							ArrayList<Binding> dcbs = new ArrayList<Binding>();
							dcbs.add(dc.getBinding());

							while (!dcbs.isEmpty()) {
								Binding dcb = dcbs.remove(0);
								if (dcb.isSet()) {
									for (WingsSet ds : dcb) {
										Binding dsb = (Binding) ds;
										dcbs.add(dsb);
									}
								} else {
									PortBindingList dpblist = (PortBindingList) dcb.getData();
									PortBinding dpb = dpblist.getPortBinding();
									for(Port dp : dps) {
  									Binding dxb = dpb.getById(dp.getID());
  
  									// Currently, just assuming that all higher
  									// dimension data is consumed
  									if (oxb.getMaxDimension() > dxb.getMaxDimension()) {
  										ok = true;
  										break;
  									}
  
  									ArrayList<Binding> dxbs = new ArrayList<Binding>();
  									dxbs.add(dxb);
  
  									while (!dxbs.isEmpty()) {
  										dxb = dxbs.remove(0);
  										if (dxb.isSet()
  												&& dxb.getMaxDimension() > oxb.getMaxDimension()) {
  											for (WingsSet ds : dxb)
  												dxbs.add((Binding) ds);
  										} else {
  											if (dxb != null
  													&& dxb.toString().equals(oxb.toString())) {
  												ok = true;
  												break;
  											}
  										}
  									}
									}
									if (ok)
										break;
								}
							}
						} else {
							this.addExplanation("ERROR " + ocb + " has no PortBinding !");
						}
						if (!ok) {
							Binding parent = parentb.get(ocb);
							Binding child = ocb;
							while (parent != null) {
								Binding grandparent = parentb.get(parent);
								parent.remove(child);
								parentb.put(parent, grandparent);
								if (parent.isEmpty()) {
									child = parent;
									parent = grandparent;
								} else {
									break;
								}
							}
						}
					}
				}
        if (oc.getBinding() == null)
          return false;
				if (oc.getBinding().isSet() && oc.getBinding().isEmpty())
					return false;
			}

			for (Link ln : template.getInputLinks(on))
				links.add(ln);
		}
		return true;
	}

	private ArrayList<KBTriple> convertMetricsToTriples(Metrics metrics, String varId) {
		OntFactory ontfac = new OntFactory(OntFactory.JENA);
		KBAPI tkb = ontfac.getKB(OntSpec.PLAIN);

		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();
		KBObject varObject = tkb.getResource(varId);

		HashMap<String, ArrayList<Metric>> propm = metrics.getMetrics();
		for (String propid : propm.keySet()) {
			for(Metric m : propm.get(propid)) {
  			KBObject prop = tkb.getResource(propid);
  			KBObject val = null;
  			if (m.getType() == Metric.LITERAL && m.getValue() != null) {
  				if(m.getDatatype() != null)
  					val = tkb.createXSDLiteral(m.getValueAsString(), m.getDatatype());
  				else
  					val = tkb.createLiteral(m.getValue());
  			}
  			else if (m.getType() == Metric.URI)
  				val = tkb.getResource(m.getValueAsString());
  			constraints.add(tkb.addTriple(varObject, prop, val));
			}
		}

		return constraints;
	}

	private ArrayList<KBTriple> fetchDatasetConstraints(Binding b, Variable v) {
		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();

		ArrayList<Binding> dbs = new ArrayList<Binding>();
		dbs.add(b);
		int i = 0;
		while (!dbs.isEmpty()) {
			Binding db = dbs.remove(0);
			if (db.isSet()) {
				for (WingsSet s : db) {
					Binding sb = (Binding) s;
					dbs.add(sb);
				}
			} else {
				Metrics metrics = dc.findDataMetricsForDataObject(db.getID());
				ArrayList<KBTriple> redbox = this.convertMetricsToTriples(metrics, v.getID());
				ArrayList<String> redboxStr = new ArrayList<String>();
				for (KBTriple kbTriple : redbox) {
					redboxStr.add(kbTriple.fullForm());
				}
				if (i == 0) {
					constraints.addAll(redbox);
				} else {
					for (KBTriple con : new ArrayList<KBTriple>(constraints)) {
						if (!redboxStr.contains(con.fullForm())) {
							constraints.remove(con);
						}
					}
				}
				i++;
			}
		}
		return constraints;
	}

	private Binding createNewBinding(Variable v, Binding db, KBObject vtype, Role param,
			String prefix, LogEvent event) {
		//String key = prefix + param.getRoleId() + db.getName();
		String key = prefix + param.getRoleId();

		// Q4.3a Here
		logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.3")
				.addWQ(LogEvent.QUERY_ARGUMENTS, key));

		String id = dc.createDataIDFromKey(key, v.getName());

		String opid = null;
		if (db != null && vtype != null && db.getMetrics() != null)
			opid = dc.createDataIDFromMetrics(id, vtype.getID(), db.getMetrics());

		if (opid == null)
			opid = id;

		logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.3")
				.addWQ(LogEvent.QUERY_RESPONSE, opid));

		return new Binding(dataNS + opid);
	}

	private String getInputRoleStr(PortBinding pb, ComponentVariable c) {
		// Create a hashmap of input (role + ":"+ dsid/param)
		// - to uniquely identify the outputs
		TreeMap<String, String> iRoleVals = new TreeMap<String, String>();

		for (Port p : pb.keySet()) {
			iRoleVals.put(p.getRole().getName(), pb.get(p).toString());
		}
		String sortedInputs = (c.isTemplate() ? c.getID() : c.getBinding().getName()) + ":";
		for (String irole : iRoleVals.keySet()) {
			sortedInputs += irole + ":" + iRoleVals.get(irole) + ",";
		}
		return sortedInputs;
	}
}
