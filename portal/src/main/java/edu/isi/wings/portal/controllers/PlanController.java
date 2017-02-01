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

package edu.isi.wings.portal.controllers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataReasoningAPI;
import edu.isi.wings.catalog.data.classes.VariableBindings;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.metrics.Metric;
import edu.isi.wings.catalog.data.classes.metrics.Metrics;
import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.execution.engine.api.impl.local.LocalExecutionEngine;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.planner.api.WorkflowGenerationAPI;
import edu.isi.wings.planner.api.impl.kb.WorkflowGenerationKB;
import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;
import edu.isi.wings.workflow.template.classes.Node;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.ValueBinding;
import edu.isi.wings.workflow.template.classes.sets.WingsSet;
import edu.isi.wings.workflow.template.classes.variables.ComponentVariable;
import edu.isi.wings.workflow.template.classes.variables.Variable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("unused")
public class PlanController {
	private DataReasoningAPI dc;
	private ComponentReasoningAPI cc;
	private ResourceAPI rc;
	private TemplateCreationAPI tc;
	private WorkflowGenerationAPI wg;

	private Config config;
	private PrintWriter out;
	
	private Gson json;
	private Properties props;

	private String wliburl;
	private String dcdomns;
	private String dclibns;
	private String pcdomns;
	private String wflowns;
	private String resontns;
	
	public PlanController(Config config, PrintWriter out) {
		this.config = config;
		this.out = out;
		this.json = JsonHandler.createTemplateGson();
		this.props = config.getProperties();

		tc = TemplateFactory.getCreationAPI(props);
		cc = ComponentFactory.getReasoningAPI(props);
		dc = DataFactory.getReasoningAPI(props);
		rc = ResourceFactory.getAPI(props);
		  
		wg = new WorkflowGenerationKB(props, dc, cc, rc, UuidGen.generateAUuid(""));

		this.wliburl = (String) props.get("domain.workflows.dir.url");
		this.dcdomns = (String) props.get("ont.domain.data.url") + "#";
		this.dclibns = (String) props.get("lib.domain.data.url") + "#";
		this.pcdomns = (String) props.get("ont.domain.component.ns");
		this.wflowns = (String) props.get("ont.workflow.url") + "#";
		this.resontns = (String) props.get("ont.resource.url") + "#";
		
    this.setMachineWhitelist();
	}
	
	private void setMachineWhitelist() {
    if(config.getDomainExecutionEngine().getClass().
        equals(LocalExecutionEngine.class)) {
  	  ArrayList<String> machineWhiteList = new ArrayList<String>();
  	  machineWhiteList.add(this.resontns + "Localhost");
  	  this.rc.setMachineWhitelist(machineWhiteList);
    }
	}
	
	@SuppressWarnings("rawtypes")
	public void printSuggestedDataJSON(String tplid, Map keyvals, boolean noexplain) {
		printPlannerJSON(tplid, keyvals, "getData", noexplain);
	}

	@SuppressWarnings("rawtypes")
	public void printSuggestedParametersJSON(String tplid, Map keyvals, boolean noexplain) {
		printPlannerJSON(tplid, keyvals, "getParameters", noexplain);
	} 
	
	@SuppressWarnings("rawtypes")
	public void printExpandedTemplatesJSON(String tplid, Map keyvals, boolean noexplain) {
		printPlannerJSON(tplid, keyvals, "getExpansions", noexplain);
	}

	public void printElaboratedTemplateJSON(String tplid, String templatejson, String consjson) {
		Template tpl = JsonHandler.getTemplateFromJSON(this.json, templatejson, consjson);
		tpl = wg.getInferredTemplate(tpl);
		tpl.setID(tplid);
		
		HashMap<String, Object> extra = new HashMap<String, Object>();
		extra.put("explanations", wg.getExplanations());
		this.out.println(JsonHandler.getTemplateJSON(json, tpl, extra));
	}
	
	@SuppressWarnings("rawtypes")
	private void printPlannerJSON(String tplid, Map keyvals, String op, boolean noexplain) {
		Template tpl = tc.getTemplate(tplid);
		this.addTemplateBindings(tpl, keyvals);
		
		Template itpl = wg.getInferredTemplate(tpl);
		ArrayList<Template> candidates = wg.specializeTemplates(itpl);
		if(candidates.size() == 0) {
			printError();
			return;
		}
		
		ArrayList<Template> bts = new ArrayList<Template>();
    ArrayList<VariableBindingsList> allbindings = new ArrayList<VariableBindingsList>();
    
    if(config.isLightReasoner())
      bts = candidates;
    else {
  		for(Template t : candidates) {
  		  ArrayList<VariableBindingsList> bindings = wg.selectInputDataObjects(t);
  		  if(bindings == null)
  		    continue;
  		  
        allbindings.addAll(bindings);
        // Do not create bound templates if only asking for data suggestions
        // -- gets memory intensive
  		  if(!op.equals("getData")) {
  		    for(VariableBindingsList binding : bindings)
  		      bts.add(wg.bindTemplate(t, binding));
  		  }
  		}
  		if(allbindings.size() == 0) {
  			printError();
  			return;
  		}
    }
    if(op.equals("getData")) {
      if(config.isLightReasoner())
        printError();
      else
        printDataBindingsJSON(allbindings, noexplain);
      return;
    }
		
		wg.setDataMetricsForInputDataObjects(bts);

		ArrayList<Template> cts = new ArrayList<Template>();
		for(Template bt : bts)
		  cts.addAll(wg.configureTemplates(bt));
		if(cts.size() == 0) {
		  printError();
		  return;
		}
    if(op.equals("getParameters")) {
      printParameterBindingsJSON(cts, noexplain);
      return;
    }

		ArrayList<Template> ets = new ArrayList<Template>();
		for(Template ct : cts)
			ets.add(wg.getExpandedTemplate(ct));
		if(ets.size() == 0) {
			printError();
			return;
		}
		if(op.equals("getExpansions")) {
			printTemplatesJSON(ets, tplid, tpl, noexplain);
			return;
		}
		
		printError();
	}

	private HashMap<String, Object> getTemplateDetails(Template t) {
    ArrayList<String> varids = new ArrayList<String>();
    for(Variable v : t.getVariables()) varids.add(v.getID());
    HashMap<String, Object> tstore = new HashMap<String, Object>();
    tstore.put("template",  t);
    tstore.put("constraints",  t.getConstraintEngine().getConstraints(varids));
    //tstore.put("time", this.getEstimatedExecutionTime(t, tplid));
    return tstore;
	}
	
	private void printTemplatesJSON(ArrayList<Template> ts, String tplid,
	    Template seedtpl, boolean noexplain) {
		ArrayList<Object> template_stores = new ArrayList<Object>();
		for(Template t : ts) {
			template_stores.add(this.getTemplateDetails(t));
		}
		HashMap<String, Object> map = new HashMap<String, Object>();
		if(!noexplain)
		  map.put("explanations", wg.getExplanations());
		map.put("error",  false);
		map.put("templates", template_stores);
    map.put("output",  "");
		map.put("seed", this.getTemplateDetails(seedtpl));
		
		this.printEncodedResults(map); 
	}
	
	private void printDataBindingsJSON(ArrayList<VariableBindingsList> bts, boolean noexplain) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		if(!noexplain)
		  map.put("explanations", wg.getExplanations());
		map.put("error",  false);
		map.put("bindings", getDataBindings(bts));
		map.put("output",  "");
		this.printEncodedResults(map); 
	}
	
	private void printParameterBindingsJSON(ArrayList<Template> cts, boolean noexplain) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		if(!noexplain)
		  map.put("explanations", wg.getExplanations());
		map.put("error",  false);
		map.put("bindings", getParameterBindings(cts));
		map.put("output",  "");
		this.printEncodedResults(map); 
	}
	
	private void printError(Template tpl) {
		tpl.delete();
		printError();
	}
	
	private void printError() {
		HashMap<String, Object> map = new HashMap<String, Object>(); 
		map.put("explanations", wg.getExplanations());
		map.put("error",  true);
		map.put("bindings", new ArrayList<String>());
		this.printEncodedResults(map);
	}
	
	private void printEncodedResults(HashMap<String, Object> map) {
		Boolean error = (Boolean) map.get("error");
		HashMap<String, Object> results = new HashMap<String, Object>();
		results.put("success", (Boolean)!error);
		results.put("data", map);
		json.toJson(results, this.out);
	}
	
	private ArrayList<TreeMap<String, Binding>> getDataBindings(
	    ArrayList<VariableBindingsList> bindings) {
		ArrayList<TreeMap<String, Binding>> blist = new ArrayList<TreeMap<String, Binding>>();
		HashMap<String, Boolean> bindingstrs = new HashMap<String, Boolean>(); 
		
		for(VariableBindingsList binding: bindings) {
      TreeMap<String, Binding> xbindings = new TreeMap<String, Binding>();

		  for(VariableBindings vb : binding) {
		    String vname = vb.getDataVariable().getName();
		    Binding b = new Binding();
		    for(KBObject obj : vb.getDataObjects()) {
		      Binding cb = new Binding(obj.getID());
		      if(vb.getDataObjects().size() == 1)
		        b = cb;
		      else
		        b.add(cb);
		    }
		    xbindings.put(vname, b);
		  }
			String bstr = "";
			for(String v : xbindings.keySet()) {
				bstr += xbindings.get(v).toString() + ",";
			}
			if(!bindingstrs.containsKey(bstr)) {
				bindingstrs.put(bstr, true);
				blist.add(xbindings);
			}
		}
		return blist;
	}
	
	private ArrayList<TreeMap<String, Binding>> getParameterBindings(ArrayList<Template> cts) {
		ArrayList<TreeMap<String, Binding>> bindings_b = new ArrayList<TreeMap<String, Binding>>();
		for(Template bt: cts) {
			TreeMap<String, Binding> binding_b = new TreeMap<String, Binding>();
			for(Variable v : bt.getInputVariables()) {
				if(v.isParameterVariable() && v.getBinding() != null) {
					binding_b.put(v.getName(), v.getBinding());
				}
			}
			bindings_b.add(binding_b);
		}
		
		// Expanding collections into multiple configurations
		// FIXME: Cannot handle parameter collections right now
		ArrayList<TreeMap<String, Binding>> bindings = new ArrayList<TreeMap<String, Binding>>();
		HashMap<String, Boolean> bstrs = new HashMap<String, Boolean>();
		while(!bindings_b.isEmpty()) {
			boolean hasSets = false;
			TreeMap<String, Binding> binding_b = bindings_b.remove(0);
			TreeMap<String, Binding> binding = new TreeMap<String, Binding>();

			for(String v : binding_b.keySet()) {
				Binding b = binding_b.get(v);
        if (b.isSet() && b.size() > 1) {
          for (WingsSet cb : b) {
            TreeMap<String, Binding> binding_x = new TreeMap<String, Binding>();
            for (String v1 : binding_b.keySet()) {
              Binding b1 = binding_b.get(v1);
              binding_x.put(v1, b1);
            }
            binding_x.put(v, (Binding) cb);
            bindings_b.add(binding_x);
          }
          hasSets = true;
        } 
        else if (b.isSet() && b.size() == 1) {
          Binding tmpb = (Binding) b.get(0);
          while(tmpb.isSet() && tmpb.size() == 1) {
            tmpb = (Binding) tmpb.get(0);
          }
          ValueBinding vb = (ValueBinding) tmpb;
          binding.put(v, new ValueBinding(vb.getValue(), vb.getDatatype()));
        }
        else if (!b.isSet()){
          ValueBinding vb = (ValueBinding) b;
          binding.put(v, new ValueBinding(vb.getValue(), vb.getDatatype()));
        }
			}
			if(!hasSets) {
				String bstr = "";
				for(String v : binding.keySet()) {
					bstr += binding.get(v).toString() + ",";
				}
				if(!bstrs.containsKey(bstr)) {
					bstrs.put(bstr, true);
					bindings.add(binding);
				}
			}
		}
		
		return bindings;
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addTemplateBindings(Template tpl, Map keyvals) {
		String pdtypejson = (String) ((Object[])keyvals.get("__paramdtypes"))[0];
		HashMap<String, String> paramdtypes = json.fromJson(pdtypejson, HashMap.class);

		String tplns = tpl.getNamespace();
		
		// Set Component bindings from cbindings
		String[] cbindingsarr = (String[]) keyvals.get("__cbindings");
		if (cbindingsarr != null && cbindingsarr.length > 0) {
			JsonElement cbindings = new JsonParser().parse(cbindingsarr[0]);
			JsonObject cbindingsobj = cbindings.getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : cbindingsobj.entrySet()) {
				String compid = entry.getKey();
				ComponentVariable cv = tpl.getComponentVariable(compid);
				if (cv == null)
					continue;

				// Create binding
				JsonElement bindingel = entry.getValue();
				Binding b = new Binding();
				if (bindingel.isJsonArray()) {
					for (JsonElement cb : bindingel.getAsJsonArray()) {
						b.add(new Binding(cb.getAsString()));
					}
				} else if (bindingel.isJsonPrimitive()) {
					b.setID(bindingel.getAsString());
				}
				cv.setBinding(b);
			}
		}

		// Set Data and Parameter Bindings from keyvals
		for(Object key : keyvals.keySet()) {
			if(((String)key).startsWith("__")) continue;
			String varid = tplns + key;
			Variable var = tpl.getVariable(varid);
			if(var != null) {
				Object[] vals = (Object[])keyvals.get(key);
				Binding b = var.isDataVariable() ? new Binding() : new ValueBinding();
				for(Object val : vals) {
					if(val.equals("")) continue;
					if(var.isDataVariable()) 
						b.add(new Binding(val.toString()));
					else {
						String dtype = paramdtypes.get(key);
						if(dtype == null)
							b.add(new ValueBinding(val.toString()));
						else
							b.add(new ValueBinding(val.toString(), dtype));
					}
				}	
				if(b.size() == 0)  continue;
				if(b.size() == 1) b = (Binding) b.get(0);
				tpl.setVariableBinding(var, b);
			}
		}
	}
	
	private Template createTemporaryTemplate(Template tpl) {
		// Create a temporary template
		String name = tpl.getName() + UuidGen.generateAUuid("");
		String newid = wliburl + "/" + name + ".owl#" + name;
		tpl.setID(newid);
		tc.saveTemplate(tpl);
		// Now re-read the temporary template
		return tc.getTemplate(newid);
	}
	
	
	private Double getEstimatedExecutionTime(Template t, String tplid) {
		// FIXME: Hardcoding this for now
		ArrayList<String> userparams = new ArrayList<String>();
		ArrayList<String> usermetrics = new ArrayList<String>();
		String it = t.getNamespace() + "Iterations";
		String nl = this.dcdomns + "numberOfLines";
		userparams.add(it);
		usermetrics.add(nl);
		
		//FIXME: Hardcoding the coefficients as well for now
		HashMap<String, HashMap<String, Double>> tcoeffs = 
				new HashMap<String, HashMap<String, Double>>();
		HashMap<String, Double> onlineLDA = new HashMap<String, Double>();
		HashMap<String, Double> parallelLDA = new HashMap<String, Double>();
		HashMap<String, Double> malletLDA = new HashMap<String, Double>();
		onlineLDA.put(it, 0.17353792269248);
		onlineLDA.put(nl, 0.004892162061446);
		parallelLDA.put(it, 0.69177335591743);
		parallelLDA.put(nl, 0.025435229162505);
		malletLDA.put(it, 0.099795864183221);
		malletLDA.put(nl, 0.0073234367875418);
		tcoeffs.put("1d90bfab2801010b0566a46d30320318", onlineLDA);
		tcoeffs.put("0e0e1dfc9cf00a6108915a49cdd6e7c9", parallelLDA);
		tcoeffs.put("110535af4146092c9fec1e6952aa9bb2", malletLDA);
		
		HashMap<String, Integer> rvals = 
				getRegressionVariableValues(t, usermetrics, userparams); 
		String tsig = this.getTemplateSignature(t, tplid);
		
		double time = 0.0;
		if(tcoeffs.containsKey(tsig)) {
			HashMap<String, Double> coeffs = tcoeffs.get(tsig);
			for(String var : rvals.keySet()) {
				if(coeffs.containsKey(var)) {
					time += 1.0 * rvals.get(var) * coeffs.get(var);
				}
			}
		}
		return time;
	}
	
	private HashMap<String, Integer> getRegressionVariableValues(Template t, 
			ArrayList<String> usermetrics, ArrayList<String> userparams) {
		// FIXME: Assuming value is Integer
		HashMap<String, Integer> regressionVariables = new HashMap<String, Integer>(); 
		for(Variable v : t.getInputVariables()) {
			if(v.isDataVariable() && v.getBinding() != null) {
				Metrics metrics = v.getBinding().getMetrics();
				for(String key : metrics.getMetrics().keySet()) {
					if(usermetrics.contains(key)) {
					  for(Metric m : metrics.getMetrics().get(key)) {
  						// FIXME: Assuming value is Integer
  						Integer val = 
  								Integer.valueOf(m.getValue().toString());
  						regressionVariables.put(key, val);
					  }
					}
				}
			}
			else if(v.isParameterVariable() && v.getBinding() != null) {
				if(userparams.contains(v.getID())) {
					// FIXME: Assuming value is Integer
					regressionVariables.put(v.getID(), 
							Integer.valueOf(v.getBinding().getValue().toString()));
				}
			}
		}
		return regressionVariables;
	}
	
	private String getTemplateSignature(Template t, String tplid) {
		ArrayList<String> compBindings = new ArrayList<String>();
		for(Node n : t.getNodes()) {
			if(n.getComponentVariable() != null) {
				String cbinding = n.getComponentVariable().getBinding().toString();
				compBindings.add(cbinding);
			}
		}
		Collections.sort(compBindings);
		
		String sigstring = tplid+"_"+compBindings;
		return DigestUtils.md5Hex(sigstring);
	}
}
