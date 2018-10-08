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

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import edu.isi.kcap.ontapi.*;
import edu.isi.kcap.ontapi.rules.KBRuleList;
import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentInvocation;
import edu.isi.wings.catalog.component.classes.ComponentPacket;
import edu.isi.wings.catalog.component.classes.ComponentRole;
import edu.isi.wings.catalog.component.classes.requirements.ComponentRequirement;
import edu.isi.wings.catalog.data.classes.metrics.Metric;
import edu.isi.wings.catalog.data.classes.metrics.Metrics;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.workflow.template.api.ConstraintEngine;
import edu.isi.wings.workflow.template.api.impl.kb.ConstraintEngineKB;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.ValueBinding;
import edu.isi.wings.workflow.template.classes.sets.WingsSet;
import edu.isi.wings.workflow.template.classes.variables.ComponentVariable;
import edu.isi.wings.workflow.template.classes.variables.Variable;
import edu.isi.wings.workflow.template.classes.variables.VariableType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.*;

public class ComponentReasoningKB extends ComponentKB implements ComponentReasoningAPI {
	private Logger logger = Logger.getLogger(this.getClass());

	private HashMap<String, Component> ccache =
	    new HashMap<String, Component>();
	private HashMap<String, KBRuleList> rulescache =
      new HashMap<String, KBRuleList>();
  private HashMap<String, ArrayList<String>> abscache =
      new HashMap<String, ArrayList<String>>();	
  private HashMap<String, ArrayList<KBObject>> classcache =
      new HashMap<String, ArrayList<KBObject>>();   
	private HashMap<String, ArrayList<KBTriple>> kbcache =
      new HashMap<String, ArrayList<KBTriple>>();
	
	private ArrayList<KBTriple> metricTriples;
	private ArrayList<KBObject> metricProps;
	
	public ComponentReasoningKB(Properties props) {
		super(props, true, false, true, false);
		this.initializeMetrics();
	}
	
	private void initializeMetrics() {
	  this.start_read();
	  
    KBObject rdfsProp = this.kb.getProperty(KBUtils.RDFS + "subPropertyOf");
    KBObject dcProp = this.kb.getProperty(this.dcns + "hasMetrics");
    KBObject dcPropD = this.kb.getProperty(this.dcns + "hasDataMetrics");
    
    // Get all metric properties
    this.metricTriples = this.kb.genericTripleQuery(null, rdfsProp, dcProp);
    // Get all data metric properties
    this.metricTriples.addAll(this.kb.genericTripleQuery(null, rdfsProp, dcPropD));
    
    // Get a list of all metrics and datametrics properties in the catalog
    this.metricProps = this.kb
        .getSubPropertiesOf(this.objPropMap.get("hasMetrics"), false);
    this.metricProps.addAll(this.kb
        .getSubPropertiesOf(this.dataPropMap.get("hasDataMetrics"), false));
    
    this.end();
	}

	protected KBObject copyObjectIntoKB(String id, KBObject obj, KBAPI tkb, String includeNS,
			String excludeNS, boolean direct) {
		// Add component to the temporary KB (add all its classes explicitly)
		for(KBTriple triple : this.getTriplesForObject(id, obj, includeNS, excludeNS, direct)) {
		  tkb.addTriple(triple);
		}
		return tkb.getIndividual(id);
	}
	
	protected KBObject copyObjectClassesIntoKB(String id, KBObject obj, KBAPI tkb, String includeNS,
	    String excludeNS, boolean direct) {
	  // Add component to the temporary KB (add all its classes explicitly)
	  for(KBTriple triple : this.getObjectClassTriples(id, obj, includeNS, excludeNS, direct)) {
	    tkb.addTriple(triple);
	  }
	  KBObject ind = tkb.getIndividual(id);
	  if(ind == null)
	    ind = tkb.createIndividual(id);
	  return ind;
	}
	
	protected ArrayList<String> getConcreteComponentsForAbstract(String id) {
	  if(abscache.containsKey(id))
	    return abscache.get(id);
	  
    KBObject cls = this.kb.getClassOfInstance(this.kb.getIndividual(id));
    if (cls == null) {
      this.abscache.put(id, null);
      return null;
    }
    ArrayList<KBObject> insts = this.kb.getInstancesOfClass(cls, false);
    ArrayList<String> ids = new ArrayList<String>();
    for(KBObject inst : insts)
      ids.add(inst.getID());
    this.abscache.put(id, ids);
    return ids;
	}
	
	protected ArrayList<KBTriple> getTriplesForObject(String id, KBObject obj, 
	    String includeNS, String excludeNS, boolean direct) {
	  ArrayList<KBTriple> triples = new ArrayList<KBTriple>();
	  triples.add(ontologyFactory.getTriple(
	      ontologyFactory.getObject(id), 
	      ontologyFactory.getObject(KBUtils.RDF+"type"),
	      this.kb.getClassOfInstance(obj)));
	  triples.addAll(getObjectClassTriples(id, obj, includeNS, excludeNS, direct));
	  return triples;
	}

	protected ArrayList<KBTriple> getObjectClassTriples(String id, KBObject obj,
			String includeNS, String excludeNS, boolean direct) {
		// Add component to the temporary KB (add all its classes explicitly)
	  ArrayList<KBTriple> triples = new ArrayList<KBTriple>();
	  
    if(kbcache.containsKey(obj.getID())) {
      for(KBTriple triple : kbcache.get(obj.getID())) {
        triples.add(ontologyFactory.getTriple(
            ontologyFactory.getObject(id), 
            triple.getPredicate(), 
            triple.getObject()));
      }
      return triples;
    }
    
		KBObject tobj = ontologyFactory.getObject(id);
		ArrayList<KBObject> objclses = 
		    this.getAllCachedClassesOfInstance(obj.getID(), direct);
		for (KBObject objcls : objclses) {
			if ((includeNS != null && objcls.getNamespace().equals(includeNS))
					|| (excludeNS != null && !objcls.getNamespace().equals(excludeNS))) {
				triples.add(ontologyFactory.getTriple(
				    tobj, ontologyFactory.getObject(KBUtils.RDF + "type"), objcls));
			}
		}
    kbcache.put(id, triples);
		return triples;
	}

	private ArrayList<KBObject> getAllClassesOfInstance(KBAPI tkb, String id) {
	  ArrayList<KBObject> clses = new ArrayList<KBObject>();
	  for(KBTriple t : 
	    tkb.genericTripleQuery(tkb.getResource(id), 
	        tkb.getProperty(KBUtils.RDF+"type"), null)) {
	    clses.add(this.conceptMap.get(t.getObject().getID()));
	  }
	  return clses;
	}
	
	 private ArrayList<KBObject> getAllCachedClassesOfInstance(String id, boolean direct) {
	   String key = id + ":" + direct;
	   if(classcache.containsKey(key))
	     return classcache.get(key);
	   ArrayList<KBObject> clses = kb.getAllClassesOfInstance(kb.getResource(id), direct);
	   classcache.put(key, clses);
	   return clses;
	 }
	
	protected boolean checkTypeCompatibility(KBAPI tkb, String varid, String argid) {
	  this.start_read();
		ArrayList<KBObject> varclses = this.getAllClassesOfInstance(tkb, varid);
		ArrayList<KBObject> argclses = this.getAllCachedClassesOfInstance(argid, true);
		for (KBObject argcls : argclses) {
			if (argcls.getNamespace().equals(this.dcdomns)) {
				for (KBObject varcls : varclses) {
					if (varcls.getNamespace().equals(this.dcdomns)) {
						if (!kb.hasSubClass(argcls, varcls) && !kb.hasSubClass(varcls, argcls))
							return false;
					}
				}
			}
		}
		this.end();
		return true;
	}
	
	 protected boolean checkTypeCompatibility(ArrayList<String> varclassids, String argid) {
	   this.start_read();
	    ArrayList<KBObject> argclses = this.getAllCachedClassesOfInstance(argid, true);
	    for (KBObject argcls : argclses) {
	      if (argcls.getNamespace().equals(this.dcdomns)) {
	        for (String varclassid : varclassids) {
	          KBObject varcl = this.conceptMap.get(varclassid);
	          if (varcl.getNamespace().equals(this.dcdomns)) {
	            if (!kb.hasSubClass(argcls, varcl) && !kb.hasSubClass(varcl, argcls))
	              return false;
	          }
	        }
	      }
	    }
	    this.end();
	    return true;
	  }

	/**
	 * <b>Query 2.1</b><br/>
	 * Get a list of Specialized Components with their IO Data Requirements
	 * given a component (maybe abstract) and it's IO Data Requirements
	 * 
	 * @param details
	 *            A ComponentDetails Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return list of ComponentDetails Objects for each specialized component
	 */

	public ArrayList<ComponentPacket> specializeAndFindDataDetails(ComponentPacket details) {
		return findDataDetails(details, true, true);
	}

	/**
	 * <b>Query 2.1b</b><br/>
	 * Get inferred IO Data Requirements given a component (maybe abstract) and
	 * it's given IO Data Requirements
	 * 
	 * @param details
	 *            A ComponentDetails Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return ComponentDetails Object
	 */
	public ComponentPacket findDataDetails(ComponentPacket details) {
		ArrayList<ComponentPacket> list = findDataDetails(details, false, true);
		if (list.size() > 0)
			return list.get(0);

		return null;
	}

	/**
	 * Helper function which does the actual data requirement checking
	 * 
	 * @param details
	 * @param specialize
	 * @param useRules
	 * @return
	 */
	public ArrayList<ComponentPacket> findDataDetails(ComponentPacket details, boolean specialize,
			boolean useRules) {
		ArrayList<ComponentPacket> list = new ArrayList<ComponentPacket>();

		HashMap<String, KBObject> omap = this.objPropMap;
		HashMap<String, KBObject> dmap = this.dataPropMap;

		// Extract info from details object
		ComponentVariable c = details.getComponent();
		HashMap<String, Variable> roleMaps = details.getStringRoleMaps();
		HashMap<String, Role> varMaps = details.getStringVariableMap();
		ArrayList<KBTriple> redbox = details.getRequirements();

		// Get All component bindings
		String incompid = c.getID();
		Binding cb = c.getBinding();
		ArrayList<String> cbindings = new ArrayList<String>();
		if(!cb.isSet())
			cbindings.add(cb.getID());
		else {
			for(WingsSet s : cb) {
				cbindings.add(((Binding)s).getID());
			}
		}
		
    this.start_read();
    boolean batchok = this.start_batch_operation();
    
    try {
  		// Get List of all concrete components
  		ArrayList<Component> ccomps = new ArrayList<Component>();
  		for(String cbid : cbindings) {
  			Component comp = this.getCachedComponent(cbid);
  			if (comp == null) {
  				logger.debug(cbid + " is not a valid component");
  				details.addExplanations(cbid + " is not a valid component");
  				details.setInvalidFlag(true);
  				list.add(details);
  				return list;
  			}
  			boolean isConcrete = (comp.getType() == Component.CONCRETE);
  			if (!specialize) {
  				// If no specialization required, add component as is
  				ccomps.add(comp);
  			} else if (isConcrete) {
  				// If specialization required, but component is already concrete,
  				// add as is
  				ccomps.add(comp);
  			} else {
  				/* If the component is abstract, then get all it's concrete
  				 * components. Example of how components are structured in ontology:
  				 * absClass 
  				 * - [abs (isConcrete: false)] 
  				 * - conc1Class 
  				 * 	- [conc1 (isConcrete: true)] 
  				 * - conc2Class 
  				 * 	- [conc2 (isConcrete: true)] 
  				 * Note: Only 1 Component Instance per Class
  				 */
  			  ArrayList<String> concreteids = this.getConcreteComponentsForAbstract(comp.getID());
  				for (String concreteid : concreteids) {
  				  Component instcomp = this.getCachedComponent(concreteid);
  					if (instcomp != null && instcomp.getType() == Component.CONCRETE) {
  						ccomps.add(instcomp);
  					}
  				}
  			}
  		}
  
  		logger.debug("Available components to check validity: " + ccomps);
  
  		// For All concrete components :
  		// - Get mapping of specialized arguments to variables
  		// - Transfer "relevant" output variable properties to input variables
  		// - Pass back the specialized component + specialized mappings +
  		// modified red-box
  		// - Handle *NEW* Arguments as well -> Create *NEW* DataVariables
  
  		// Get Metrics property hierarchy triples for adding into the temporary
  		// kb
  
  		for (Component ccomp : ccomps) {
  			HashMap<Role, Variable> sRoleMap = new HashMap<Role, Variable>();
  			ArrayList<String> varids = new ArrayList<String>();
  
  			// Create a new temporary kb
  			KBAPI tkb = this.ontologyFactory.getKB(OntSpec.PLAIN);
  
  			// Add the redbox (i.e. datavariable constraints) to the temporary
  			// kb, along with domain knowledge about the data catalog
  			tkb.addTriples(redbox);
  			tkb.addTriples(domainKnowledge);
  
  			KBObject ccompobj = this.kb.getIndividual(ccomp.getID());
  			// Create a copy of the specialized component in the temporary kb
  			KBObject tcomp = this.copyObjectIntoKB(incompid, ccompobj, tkb, this.pcdomns, null,
  					false);
  
  			boolean typesOk = true;
  
  			// For all argument roles of the specialized component :
  			ArrayList<ComponentRole> allArgs = new ArrayList<ComponentRole>(ccomp.getInputs());
  			allArgs.addAll(ccomp.getOutputs());
  
  			HashSet<String> explanations = new HashSet<String>();
  			ComponentPacket cmr;
  			ComponentVariable concreteComponent = new ComponentVariable(incompid);
  			concreteComponent.setBinding(new Binding(ccomp.getID()));
  			if (specialize)
  				concreteComponent.setConcrete(true);
  			else
  				concreteComponent.setConcrete(c.isConcrete());
  
  			ArrayList<String> inputRoles = new ArrayList<String>();
  			
  			for (ComponentRole arg : allArgs) {
  				// Get the argument ID for the specialized argument
  				String argid = arg.getRoleName();
  
  				Variable var = roleMaps.get(argid);
  				String varid = null;
  				String roleid = null;
  				if (var == null) {
  					// Create a new Variable for role if none exists currently
  					// varid = arg.getID()+"_"+ccomp.getName()+"_Variable";
  					varid = ccomp.getNamespace() + argid;
  					roleid = ccomp.getID() + "_" + argid + "_role";
  					
  					short type = 0;
  					if (arg.isParam())
  						type = VariableType.PARAM;
  					else
  						type = VariableType.DATA;
  					var = new Variable(varid, type);
  				} else {
  					varid = var.getID();
  					roleid = varMaps.get(var.getID()).getID();
  					// Make sure that the variable has a type that is either
  					// subsumed by the argument type, or that the argument type
  					// is subsumed by the variable type
  					if (!checkTypeCompatibility(tkb, varid, arg.getID())) {
  						logger.debug(arg.getID() + " is not type compatible with variable: "
  								+ varid);
  						explanations.add("INFO "+ccomp + " is not selectable because " + arg.getID()
  								+ " is not type compatible with variable: " + varid);
  						typesOk = false;
  						break;
  					}
  				}
  
  				// Copy over the argument's classes to the variable
  				KBObject argobj = this.kb.getIndividual(arg.getID());
  				KBObject varobj = this.copyObjectClassesIntoKB(varid, argobj, tkb, this.dcdomns,
  						null, false);
  				if(varobj == null)
  				  continue;
  
  				// create hasArgumentID property for the variable
  				tkb.addTriple(varobj, dmap.get("hasArgumentID"), tkb.createLiteral(argid));
  
  				Role r = new Role(roleid);
  				r.setRoleId(argid);
  				r.setDimensionality(arg.getDimensionality());
  
  				// Set variable data binding
  				if (var.isDataVariable() && var.getBinding() != null
  						&& var.getBinding().getName() != null) {
  					tkb.addTriple(varobj, dmap.get("hasBindingID"),
  							tkb.createLiteral(var.getBinding().getName()));
  				} else {
  					tkb.addTriple(varobj, dmap.get("hasBindingID"),
  							tkb.createLiteral(""));
  				}
  				
  				// Set variable parameter binding (default if none set)
  				if(var.isParameterVariable()) {
  			        KBObject arg_value = null;
  			        ValueBinding parambinding = (ValueBinding) var.getBinding();
  			        if (parambinding != null && parambinding.getValue() != null) {
  			          arg_value = tkb.createXSDLiteral(parambinding.getValueAsString(), 
  			              parambinding.getDatatype());
  			        }
  			        else if(arg.getParamDefaultalue() != null) {
  			          arg_value = tkb.createLiteral(arg.getParamDefaultalue());
  			        }
  			        if (arg_value != null) {
  			          tkb.setPropertyValue(varobj, dmap.get("hasValue"), arg_value);
  			        }				  
  				}
  
  				// assign this variable as an input or output to the component
  				if (ccomp.getInputs().contains(arg)) {
  					inputRoles.add(r.getRoleId());
  					tkb.addTriple(tcomp, omap.get("hasInput"), varobj);
  				} else {
  					tkb.addTriple(tcomp, omap.get("hasOutput"), varobj);
  				}
  				sRoleMap.put(r, var);
  				varids.add(var.getID());
  			}
  
  			// Empty triple list returned if errors encountered below
  			ArrayList<KBTriple> empty = new ArrayList<KBTriple>();
  
  			// Return if there was some problem with types
  			if (!typesOk) {
  				logger.debug(ccomp + " is not selectable ");
  				explanations.add("INFO " + ccomp + " is not selectable ");
  				cmr = new ComponentPacket(concreteComponent, sRoleMap, empty);
  				cmr.setInputRoles(inputRoles);
  				cmr.addExplanations(explanations);
  				cmr.setInvalidFlag(true);
  				list.add(cmr);
  				continue;
  			}
  
  			// ** Run Rules **
  			if (useRules && ccomp.hasRules()) {
  				// Redirect output to a byte stream
  				ByteArrayOutputStream bost = new ByteArrayOutputStream();
  				PrintStream oldout = System.out;
  				System.setOut(new PrintStream(bost, true));
  
  				// Run propagation rules on the temporary kb
  				tkb.setRulePrefixes(this.rulePrefixes);
  				tkb.applyRules(this.getCachedComponentRules(ccomp));
  				//tkb.applyRulesFromString(allrules);
  
  				// Get printouts from Rules and store as Explanations
  				if (!bost.toString().equals("")) {
  					for (String exp : bost.toString().split("\\n")) {
  						explanations.add(exp);
  					}
  				}
  				// Set output back to original System.out
  				System.setOut(oldout);
  			}
  
  			// Checking for invalidity
  			KBObject invalidProp = tkb.getProperty(this.pcns + "isInvalid");
  			KBObject isInvalid = tkb.getPropertyValue(tcomp, invalidProp);
  			if (isInvalid != null && (Boolean) isInvalid.getValue()) {
  				logger.debug(ccomp + " is not selectable ");
  				explanations.add("INFO " + ccomp + " is not selectable ");
  				cmr = new ComponentPacket(concreteComponent, sRoleMap, empty);
  				cmr.setInputRoles(inputRoles);
  				cmr.addExplanations(explanations);
  				cmr.setInvalidFlag(true);
  				list.add(cmr);
  				continue;
  			}
  
  			// Set parameter values (if any)
  			for (Variable var : roleMaps.values()) {
  				if (var.isParameterVariable() && var.getBinding() == null) {
  					KBObject varobj = tkb.getResource(var.getID());
  					KBObject val = tkb.getPropertyValue(varobj, dmap.get("hasValue"));
  					if (val != null && val.getValue() != null) {
  						tkb.addTriple(varobj,
  								tkb.getResource(this.wflowns + "hasParameterValue"), val);
  						var.setBinding(new ValueBinding(val.getValue(), val.getDataType()));
  					}
  				}
  			}
  
  			// Create a constraint engine and get Relevant Constraints here
  			ConstraintEngine cons = new ConstraintEngineKB(tkb, "");
  			cons.addWhitelistedNamespace(this.dcdomns);
  			cons.addWhitelistedNamespace(this.dcns);
  			cons.addWhitelistedNamespace(this.wflowns);
  			ArrayList<String> blacklistedIds = new ArrayList<String>();
  			blacklistedIds.add(dmap.get("hasArgumentID").getID());
  			blacklistedIds.add(dmap.get("hasBindingID").getID());
  			blacklistedIds.add(this.dcns + "hasMetrics");
  			blacklistedIds.add(this.dcns + "hasDataMetrics");
  			blacklistedIds.add(this.pcns + "hasValue");
  
  			for (String id : blacklistedIds)
  				cons.addBlacklistedId(id);
  			ArrayList<KBTriple> constraints = cons.getConstraints(varids);
  			for (String id : blacklistedIds)
  				cons.removeBlacklistedId(id);
  
  			cmr = new ComponentPacket(concreteComponent, sRoleMap, constraints);
  			cmr.setInputRoles(inputRoles);
  			cmr.addExplanations(explanations);
  			list.add(cmr);
  		}
  		return list;
    }
    finally {
      if(batchok)
        this.stop_batch_operation();
      this.end();
    }
	}
  
	private Component getCachedComponent(String compid) {
    // Get Component
    Component comp = null;
    if(ccache.containsKey(compid)) { 
      comp = ccache.get(compid);
    }
    else {
      comp = this.getComponent(compid, true);
      ccache.put(compid, comp);
    }
    return comp;
	}
	
	private KBRuleList getCachedComponentRules(Component comp) {
	  if(rulescache.containsKey(comp.getID()))
	    return rulescache.get(comp.getID());
	  String rulestr = "";
	  for(String str : comp.getRules())
	    rulestr += str + "\n";
	  for(String str : comp.getInheritedRules())
	    rulestr += str + "\n";
	  KBRuleList rules = this.ontologyFactory.parseRules(rulestr);
	  rulescache.put(comp.getID(), rules);
	  return rules;
	}
	
	/**
	 * <b>Query 4.2</b><br/>
	 * This function is supposed to <b>SET</b> the DataSet Metrics, or Parameter
	 * Values for the Variables that are passed in via the input/output maps as
	 * part of details.<br/>
	 * Variables will already be bound to dataObjects, so the function will have
	 * to do something like the following :
	 * 
	 * <pre>
	 * If Variable.isParameterVariable() Variable.setParameterValue(value)
	 * If Variable.isDataVariable() Variable.getDataObjectBinding().setDataMetrics(xml)
	 * </pre>
	 * 
	 * @param details
	 *            A ComponentDetails Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return List of extra template variable descriptions (will mostly be
	 *         empty in Q4.2 though)
	 */
	public ArrayList<ComponentPacket> findOutputDataPredictedDescriptions(ComponentPacket details) {
		ArrayList<ComponentPacket> list = new ArrayList<ComponentPacket>();   
  
		HashMap<String, KBObject> omap = this.objPropMap;
		HashMap<String, KBObject> dmap = this.dataPropMap;
		
		// If the component has no rules, then simplify !!

		// Extract info from details object
		ComponentVariable c = details.getComponent();
		HashMap<String, Variable> sRoleMap = details.getStringRoleMaps();
		HashMap<String, Boolean> noParamBindings = new HashMap<String, Boolean>();

		ArrayList<KBTriple> redbox = details.getRequirements();

		Component comp = this.getCachedComponent(c.getBinding().getID());
		if (comp == null) {
			logger.debug(c.getBinding().getID() + " is not a valid component");
			details.addExplanations(c.getBinding().getID() + " is not a valid component");
			details.setInvalidFlag(true);
			list.add(details);
			return list;
		}
		
		c.setRequirements(comp.getComponentRequirement());

		boolean typesOk = true;
		
    // Set default parameter values (if not already set)
		// - Also recheck type compatibility
    ArrayList<String> inputRoles = new ArrayList<String>();
    for(ComponentRole role : comp.getInputs()) {
      inputRoles.add(role.getRoleName());
      Variable v = sRoleMap.get(role.getRoleName());
      if(role.isParam()) {
        if(v.getBinding() == null) {
          v.setBinding(new ValueBinding(role.getParamDefaultalue()));
          noParamBindings.put(v.getID(), true);
        }
        else if(v.getBinding().getValue() == null) {
          v.getBinding().setValue(role.getParamDefaultalue());
          noParamBindings.put(v.getID(), true);
        }
      }
      else {
        ArrayList<String> varclassids = new ArrayList<String>();     
        ArrayList<Metric> vartypes = 
            v.getBinding().getMetrics().getMetrics().get(KBUtils.RDF + "type");
        if(vartypes != null) {
          for(Metric m : vartypes) {
            varclassids.add(m.getValueAsString());
          }
          // Check type compatibility of roles
          if (!checkTypeCompatibility(varclassids, role.getID())) {
            details.addExplanations("INFO "+comp + " is not selectable because " + role.getID()
                + " is not type compatible with variable binding: " + v.getBinding());
            typesOk = false;
            break;
          }
        }
      }
    }
    details.setInputRoles(inputRoles);

    if(!typesOk) {
      details.setInvalidFlag(true);
      list.add(details);
      return list;
    }
    
		if(!comp.hasRules()) {
		  // No rules. Just set default parameter values (if not already set)
		  list.add(details);
		  return list;
		}
    
		// Create a new temporary KB store to run rules on
		KBAPI tkb = this.ontologyFactory.getKB(OntSpec.PLAIN);

		this.start_read();
		boolean batchok = this.start_batch_operation();
		
		try {
  		
  		KBObject compobj = this.kb.getIndividual(comp.getID());
  		
  		// Add component to the temporary KB store (add all its classes
  		// explicitly)
  		KBObject tcomp = this
  				.copyObjectIntoKB(comp.getID(), compobj, tkb, this.pcdomns, null, false);
      
  		// Keep a map of variable object to variable name
  		HashMap<Variable, String> variableNameMap = new HashMap<Variable, String>();
  
      for (String rolestr : sRoleMap.keySet()) {
        Variable var = sRoleMap.get(rolestr);
        // Map template variable to a temporary variable for running rules
        // - Reason is that the same variable may be used in multiple roles
        // and we want to distinguish them
        String variableName = var.getID() + "_" + rolestr;
        variableNameMap.put(var, variableName);
      }
      // Add the information from redbox to the temporary KB store
      // Cache varid to varobj
      HashMap<String, KBObject> varIDObjMap = new HashMap<String, KBObject>();
      for (Variable var : sRoleMap.values()) {
        KBObject varobj = tkb.getResource(variableNameMap.get(var));
        varIDObjMap.put(var.getID(), varobj);
      }
      // Add information from redbox
      for (KBTriple t : redbox) {
        KBObject subj = varIDObjMap.get(t.getSubject().getID());
        KBObject obj = varIDObjMap.get(t.getObject().getID());
        if (subj == null)
          subj = t.getSubject();
        if (obj == null)
          obj = t.getObject();
        tkb.addTriple(subj, t.getPredicate(), obj);
      }
      
      // Get a mapping of ArgID's to arg for the Component
      // Also note which roles are inputs
      HashMap<String, ComponentRole> argMaps = new HashMap<String, ComponentRole>();
      HashMap<String, Boolean> sInputRoles = new HashMap<String, Boolean>();
      for (ComponentRole role : comp.getInputs()) {
        argMaps.put(role.getRoleName(), role);
        sInputRoles.put(role.getRoleName(), true);
      }
      for (ComponentRole role : comp.getOutputs()) {
        argMaps.put(role.getRoleName(), role);
      }
      
  		// Convert metrics to Property assertions in the Temporary KB
  		for (String rolestr : sRoleMap.keySet()) {
  			Variable var = sRoleMap.get(rolestr);
  			ComponentRole arg = argMaps.get(rolestr);
  			if(arg == null) {
  				details.addExplanations("ERROR Component catalog cannot recognize role id "+rolestr);
  				continue;
  			}
  			String variableName = variableNameMap.get(var);
  
  			// Get a KBObject for the temporary variable
  			KBObject varobj = tkb.getResource(variableName);
  
  			if (var.isDataVariable()) {
  				// If the variable is a data variable (& is bound)
  				if (var.getBinding() != null) {
  					// Convert Metrics to PC properties in order to run rules
  					Metrics metrics = var.getBinding().getMetrics();
  					HashMap<String, ArrayList<Metric>> propValMap = metrics.getMetrics();
  					for (String propid : propValMap.keySet()) {
  						for(Metric tmp : propValMap.get(propid)) {
    						Object val = tmp.getValue();
    						String valstring = tmp.getValueAsString();
    						int type = tmp.getType();
    						String dtype = tmp.getDatatype();
    						KBObject metricProp = this.kb.getProperty(propid);
    						if (metricProp != null) {
    							//System.out.println(var.getName()+": " + propid + " = " +valstring);
    							if (type == Metric.URI) {
    								// Object Property
    								KBObject valobj = this.kb.getResource(valstring);
    								if (valobj == null) {
    									// TODO: Log and explain (make a utility
    									// function)
    									details.addExplanations("ERROR Cannot Recognize Metrics Value " + valstring);
    									continue;
    								}
    								// Copy over the object class into kb as well
    								// (except where the object itself is a class)
    								if (!metricProp.getID().equals(KBUtils.RDF + "type")) {
    									valobj = this.copyObjectIntoKB(valobj.getID(), valobj, tkb,
    											null, null, true);
    									// Remove any existing values first
    									for(KBTriple t : tkb.genericTripleQuery(varobj, metricProp, null))
    									  tkb.removeTriple(t);
    								}
    								// Add a Triple for the metric property value
    								tkb.addTriple(varobj, metricProp, valobj);
    							} else if (type == Metric.LITERAL && val != null) {
    								// Literal value
    								KBObject tobj = dtype != null ? tkb.createXSDLiteral(valstring, dtype) :
    													tkb.createLiteral(val);
    								if (tobj != null) {
    	                // Remove any existing values first
    	                for(KBTriple t : tkb.genericTripleQuery(varobj, metricProp, null))
    	                  tkb.removeTriple(t);
    	                 // Add a Triple for the metric propertyvalue
    									tkb.addTriple(varobj, metricProp, tobj);
    								} else {
    									details.addExplanations("ERROR Cannot Convert Metrics Value " + valstring);
    									continue;
    								}
    							}
    						} else {
    							// TODO: Log and explain (make a utility function)
    							details.addExplanations("ERROR No Such Metrics Property Known to Component Catalog : "
    											+ propid);
    							continue;
    						}
  						}
  					}
  
  					// Create other standard PC properties on variable
  					// - hasDimensionSizes
  					// - hasBindingID
  					if (var.getBinding().isSet()) {
  						String dimensionSizes = "";
  						ArrayList<Binding> vbs = new ArrayList<Binding>();
  						vbs.add(var.getBinding());
  						while (!vbs.isEmpty()) {
  							Binding vb = vbs.remove(0);
  							if (vb.isSet()) {
  								for (WingsSet vs : vb) {
  									vbs.add((Binding) vs);
  								}
  								if (!dimensionSizes.equals(""))
  									dimensionSizes += ",";
  								dimensionSizes += vb.getSize();
  							}
  						}
  						tkb.setPropertyValue(varobj, dmap.get("hasDimensionSizes"),
  						    tkb.createLiteral(dimensionSizes));
  					}
  
  					if (var.getBinding().getID() != null)
  						tkb.addTriple(varobj, dmap.get("hasBindingID"),
  						    tkb.createLiteral(var.getBinding().getName()));
  					else
  						tkb.addTriple(varobj, dmap.get("hasBindingID"),
  						    tkb.createLiteral(""));
  
  					// end if (var.getDataBinding() != null)
  				}
  				// end if (var.isDataVariable())
  			} else if (var.isParameterVariable()) {
  				// If the Variable/Argument is a Parameter
  				ValueBinding parambinding = (ValueBinding) var.getBinding();
  				if (parambinding != null && parambinding.getValue() != null) {
  					// If the template has any value specified, use that instead
  					//arg_value = tkb.createLiteral(var.getBinding().getValue());
  				  KBObject arg_value = tkb.createXSDLiteral(parambinding.getValueAsString(), 
  					    parambinding.getDatatype());
  				  tkb.setPropertyValue(varobj, dmap.get("hasValue"), arg_value);
  				}
  				if(dmap.containsKey("hasBindingID"))
  					// Set the hasBindingID term
  					tkb.addTriple(varobj, dmap.get("hasBindingID"),
  					    tkb.createLiteral("Param" + arg.getName()));
  			}
  
  			// Copy argument classes from Catalog as classes for the temporary
  			// variable in the temporary kb store
  	    KBObject argobj = kb.getIndividual(arg.getID());
  			this.copyObjectClassesIntoKB(varobj.getID(), argobj, tkb, null, null, true);
  
  			// Set the temporary variable's argumentID so rules can get/set
  			// triples based on the argument
  			tkb.addTriple(varobj, dmap.get("hasArgumentID"), tkb.createLiteral(rolestr));
  
  			// Set hasInput or hasOutput for the temporary Variable
  			if (sInputRoles.containsKey(rolestr)) {
  				tkb.addTriple(tcomp, omap.get("hasInput"), varobj);
  			} else {
  				tkb.addTriple(tcomp, omap.get("hasOutput"), varobj);
  			}
  			// end of for (String rolestr : sRoleMap.keySet())
  		}
      
  		// Add all metrics and datametrics properties to temporary store
      tkb.addTriples(metricTriples);
      
  		// Set current output variable metrics to do a diff with later
      for (String rolestr : sRoleMap.keySet()) {
        Variable var = sRoleMap.get(rolestr);
        if (var.isDataVariable() && !sInputRoles.containsKey(rolestr)) {
          Metrics metrics = new Metrics();
          KBObject varobj = tkb.getResource(variableNameMap.get(var));
          // Create Metrics from PC Properties
          for (KBObject metricProp : metricProps) {
            KBObject val = tkb.getPropertyValue(varobj, metricProp);
            if (val == null)
              continue;
            // Add value
            if (val.isLiteral())
              metrics.addMetric(metricProp.getID(), new Metric(Metric.LITERAL,
                  val.getValue(), val.getDataType()));
            else
              metrics.addMetric(metricProp.getID(),
                  new Metric(Metric.URI, val.getID()));
          }
          var.getBinding().setMetrics(metrics);
        }
      }
      
      KBRuleList rules = this.getCachedComponentRules(comp);
      if(rules.getRules().size() > 0) {
    		// Redirect Standard output to a byte stream
    		ByteArrayOutputStream bost = new ByteArrayOutputStream();
    		PrintStream oldout = System.out;
    		System.setOut(new PrintStream(bost, true));
    
    		// *** Run propagation rules on the temporary ontmodel ***
    		tkb.setRulePrefixes(this.rulePrefixes);
    		tkb.applyRules(rules);
    		//tkb.applyRulesFromString(allrules);
    
    		// Add printouts from rules as explanations
    		if (!bost.toString().equals("")) {
    			for (String exp : bost.toString().split("\\n")) {
    				details.addExplanations(exp);
    			}
    		}
    		// Reset the Standard output
    		System.setOut(oldout);
      }
      
  		// Check if the rules marked this component as invalid for
  		// the current component details packet
  		KBObject invalidProp = this.dataPropMap.get("isInvalid");
  		KBObject isInvalid = tkb.getPropertyValue(tcomp, invalidProp);
  		if (isInvalid != null && (Boolean) isInvalid.getValue()) {
  			details.addExplanations("INFO "+tcomp + " is not valid for its inputs");
  			logger.debug(tcomp + " is not valid for its inputs");
  			details.setInvalidFlag(true);
  			list.add(details);
  			return list;
  		}
  
  		// Check component dependencies
  		// If set, overwrite the component dependencies with these
  		ComponentRequirement req = this.getComponentRequirements(tcomp, tkb);
  		if(req != null) {
  		  if(req.getMemoryGB() != 0)
  		    c.getRequirements().setMemoryGB(req.getMemoryGB());
        if(req.getStorageGB() != 0)
          c.getRequirements().setStorageGB(req.getStorageGB());
  		}
  		
  		// Set values of variables by looking at values set by rules
  		// in temporary kb store
  		// - Only set if there isn't already a binding value for the variable
  		for (Variable var : sRoleMap.values()) {
  			if (var.isParameterVariable()
  					&& (noParamBindings.containsKey(var.getID()) || 
  					    var.getBinding() == null || 
  					    var.getBinding().getValue() == null)) {
  				KBObject varobj = tkb.getResource(variableNameMap.get(var));
  				KBObject origvarobj = tkb.getResource(var.getID());
  				KBObject val = tkb.getPropertyValue(varobj, dmap.get("hasValue"));
  				if (val != null && val.getValue() != null) {
  					tkb.addTriple(origvarobj,
  							tkb.getResource(this.wflowns + "hasParameterValue"), val);
  					var.setBinding(new ValueBinding(val.getValue(), val.getDataType()));
  				}
  			}
  		}
  
  		// To create the output Variable metrics, we go through the metrics
  		// property of the output data variables and get their metrics property
  		// values
  		for (String rolestr : sRoleMap.keySet()) {
  			Variable var = sRoleMap.get(rolestr);
  			if (var.isDataVariable() && !sInputRoles.containsKey(rolestr)) {
  		    Metrics curmetrics = var.getBinding().getMetrics();
  				Metrics metrics = new Metrics();
  				KBObject varobj = tkb.getResource(variableNameMap.get(var));
  
  				// Create Metrics from PC Properties
  				for (KBObject metricProp : metricProps) {
  				  ArrayList<KBObject> vals = tkb.getPropertyValues(varobj, metricProp);
  				  if(vals == null)
  				    continue;
  				  for(KBObject val : vals) {
  				    if(vals.size() > 1) {
  				      if(!curmetrics.getMetrics().containsKey(metricProp.getID()))
  				        continue;
  				      // If multiple values present, ignore value that is equal to current value
  				      for(Metric mval : curmetrics.getMetrics().get(metricProp.getID())) {
    				      if(!val.isLiteral() && val.getID().equals(mval.getValue()))
    				        continue;
    				      else if(val.isLiteral() && val.getValue().equals(mval.getValue()))
    				        continue;
  				      }
  				    }
  				    // Add value
              if (val.isLiteral())
                metrics.addMetric(metricProp.getID(), new Metric(Metric.LITERAL,
                    val.getValue(), val.getDataType()));
              else
                metrics.addMetric(metricProp.getID(),
                    new Metric(Metric.URI, val.getID()));
  				  }
  				}
  				ArrayList<KBObject> clses = this.getAllClassesOfInstance(tkb, varobj.getID());
  				for (KBObject cls : clses)
  					metrics.addMetric(KBUtils.RDF + "type", new Metric(Metric.URI, cls.getID()));
          
  				// Set metrics for the Binding
  				if (var.getBinding() != null)
  					var.getBinding().setMetrics(metrics);
  
  				// -- Dealing with Collections --
  				// User other Properties for creating output binding collections
  				// and setting the collection item metrics as well
  				// PC Properties used:
  				// - hasDimensionSizes
  				// - hasDimensionIndexProperties
  				int dim = 0;
  				final int maxdims = 10; // not more than 10 dimensions
  				int[] dimSizes = new int[maxdims];
  				String[] dimIndexProps = new String[maxdims];
  				KBObject dimSizesObj = tkb.getPropertyValue(varobj, dmap.get("hasDimensionSizes"));
  				KBObject dimIndexPropsObj = tkb.getPropertyValue(varobj,
  						dmap.get("hasDimensionIndexProperties"));
  
  				// Parse dimension sizes string (can be given as a comma-separated list)
  				// Example 2,3
  				// - This will create a 2x3 matrix
  				if (dimSizesObj != null && dimSizesObj.getValue() != null) {
  					if (dimSizesObj.getValue().getClass().getName().equals("java.lang.Integer")) {
  						dimSizes[0] = (Integer) dimSizesObj.getValue();
  						dim = 1;
  					} else {
  						String dimSizesStr = (String) dimSizesObj.getValue();
  						for (String dimSize : dimSizesStr.split(",")) {
  							try {
  								int size = Integer.parseInt(dimSize);
  								dimSizes[dim] = size;
  								dim++;
  							} catch (Exception e) {
  							}
  						}
  					}
  				}
  
  				// Parse dimension index string (can be given as a comma
  				// separated list)
  				// Example hasXIndex, hasYIndex
  				// - This will set each output item's
  				// - first dimension index using property hasXIndex
  				// - second dimension index using property hasYIndex
  				// Example output:
  				// - output
  				// - output0 (hasXIndex 0)
  				// - output00 (hasXIndex 0, hasYIndex 0)
  				// - output01 (hasXIndex 0, hasYIndex 1)
  				// - output0 (hasXIndex 1)
  				// - output10 (hasXIndex 1, hasYIndex 0)
  				// - output11 (hasXIndex 1, hasYIndex 1)
  
  				if (dimIndexPropsObj != null && dimIndexPropsObj.getValue() != null) {
  					int xdim = 0;
  					String dimIndexPropsStr = (String) dimIndexPropsObj.getValue();
  					for (String dimIndexProp : dimIndexPropsStr.split(",")) {
  						try {
  							dimIndexProps[xdim] = dimIndexProp;
  							xdim++;
  						} catch (Exception e) {
  						}
  					}
  				}
  
  				// If the output is a collection
  				// dim = 1 is a List
  				// dim = 2 is a Matrix
  				// dim = 3 is a Cube
  				// .. and so on
  				if (dim > 0) {
  					int[] dimCounters = new int[dim];
  					dimCounters[0] = 1;
  					for (int k = 1; k < dim; k++) {
  						int perms = 1;
  						for (int l = k - 1; l >= 0; l--)
  							perms *= dimSizes[l];
  						dimCounters[k] = dimCounters[k - 1] + perms;
  					}
  
  					Binding b = var.getBinding();
  					ArrayList<Binding> vbs = new ArrayList<Binding>();
  					vbs.add(b);
  					int counter = 0;
  					while (!vbs.isEmpty()) {
  						Binding vb = vbs.remove(0);
  						if (vb.getMetrics() == null)
  							continue;
  						int vdim = 0;
  						for (vdim = 0; vdim < dim; vdim++) {
  							if (counter < dimCounters[vdim])
  								break;
  						}
  						if (vdim < dim) {
  							for (int i = 0; i < dimSizes[vdim]; i++) {
  								Binding cvb = new Binding(b.getNamespace()
  										+ UuidGen.generateAUuid("" + i));
  								// Copy over metrics from parent variable binding
                  Metrics tmpMetrics = new Metrics(vb.getMetrics());
  								// Add dimension index (if property set)
  								String prop = dimIndexProps[vdim];
  								if (prop != null && !prop.equals("")) {
  									Metric nm = new Metric(Metric.LITERAL, i, KBUtils.XSD+"integer");
  									tmpMetrics.addMetric(this.dcdomns + prop, nm);
  								}
                  cvb.setMetrics(tmpMetrics);
  								vb.add(cvb);
  								vbs.add(cvb);
  							}
  						}
  						counter++;
  					}
  				}
  				// end if(dim > 0)
  			}
  		}
  		// FIXME: Handle multiple configurations
  		list.add(details);
  		return list;
		}
		finally {
		  if(batchok)
		    this.stop_batch_operation();
		  this.end();
		}
	}

	public ComponentInvocation getComponentInvocation(ComponentPacket details) {
		HashMap<String, KBObject> omap = this.objPropMap;
		HashMap<String, KBObject> dmap = this.dataPropMap;

		// Extract info from details object
		ComponentVariable c = details.getComponent();
		Map<Variable, Role> varMap = details.getVariableMap();

		// Get Component
		KBObject comp = this.kb.getResource(c.getBinding().getID());
		String exepath = this.getComponentLocation(comp.getID());
		String exedir = null;
		if(exepath != null) {
			File f = new File(exepath);
			if(f.isDirectory()) {
			  exedir = exepath;
				File shexef = new File(exepath + File.separator + "run");
				File winexef = new File(exepath + File.separator + "run.bat");
				if(SystemUtils.IS_OS_WINDOWS && winexef.exists())
					exepath = winexef.getAbsolutePath();
				else
					exepath = shexef.getAbsolutePath();
			}
		}
		
		ComponentInvocation invocation = new ComponentInvocation();
		invocation.setComponentId(comp.getID());
		invocation.setComponentLocation(exepath);
		invocation.setComponentDirectory(exedir);

		this.start_read();
		boolean batchok = this.start_batch_operation();
		
		ArrayList<KBObject> inputs = this.kb.getPropertyValues(comp, omap.get("hasInput"));
		ArrayList<KBObject> outputs = this.kb.getPropertyValues(comp, omap.get("hasOutput"));
		ArrayList<KBObject> args = new ArrayList<KBObject>(inputs);
		args.addAll(outputs);
		
		for (KBObject arg : args) {
			KBObject argid = this.kb.getDatatypePropertyValue(arg, dmap.get("hasArgumentID"));
			String role = (String) argid.getValue();
			for(Variable var : varMap.keySet()) {
			  Role r = varMap.get(var);
				if(r.getRoleId().equals(role))
					setInvocationArguments(invocation, arg, var, inputs.contains(arg));
			}
		}
		
		if(batchok)
		  this.stop_batch_operation();
		this.end();
		
		return invocation;
	}

	private void setInvocationArguments(ComponentInvocation invocation, KBObject arg,
			Variable var, boolean isInput) {
		
		HashMap<String, KBObject> dmap = this.dataPropMap;
		KBObject defaultValue = this.kb.getPropertyValue(arg, dmap.get("hasValue"));
		KBObject prefixobj = this.kb.getPropertyValue(arg, dmap.get("hasArgumentName"));
		String prefix = (String) prefixobj.getValue();

		ArrayList<Binding> bindings = new ArrayList<Binding>();
		bindings.add(var.getBinding());
		while (!bindings.isEmpty()) {
			Binding b = bindings.remove(0);
			if (b.isSet()) {
				for (WingsSet s : b) {
					bindings.add((Binding) s);
				}
			} else {
				if (var.isDataVariable()) {
					invocation.addArgument(prefix, b, var.getID(), isInput);
				} else if (var.isParameterVariable()) {
					if (var.getBinding() != null) {
						invocation.addArgument(prefix, ((ValueBinding)var.getBinding()).getValueAsString(),  
							var.getID(), isInput);
					} else if (defaultValue != null && defaultValue.getValue() != null) {
						invocation.addArgument(prefix, defaultValue.getValueAsString(), 
							var.getID(), isInput);
					}
				}
			}
		}
	}
}
