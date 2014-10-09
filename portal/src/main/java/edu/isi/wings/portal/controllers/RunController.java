package edu.isi.wings.portal.controllers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletContext;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.planner.api.WorkflowGenerationAPI;
import edu.isi.wings.planner.api.impl.kb.WorkflowGenerationKB;
import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.html.CSSLoader;
import edu.isi.wings.portal.classes.html.HTMLLoader;
import edu.isi.wings.portal.classes.html.JSLoader;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import edu.isi.wings.workflow.template.api.Template;

public class RunController {
	private int guid;
	private Config config;
	private Gson json;

	Properties props;
	String userdir;
	String userConfigFile;
	Object writeLock;
	String dataScript;
	String templateScript;

	public RunController(int guid, Config config) {
		this.guid = guid;
		this.config = config;
		this.json = JsonHandler.createRunGson();
		this.props = config.getProperties();
		this.dataScript = config.getUserDomainUrl() + "/data";
		this.templateScript = config.getUserDomainUrl() + "/workflows";
	}

	public void show(PrintWriter out, String runid) {
		// Get Hierarchy
		try {
		  HTMLLoader.printHeader(out);
			out.println("<head>");
			out.println("<title>Access Run Results</title>");
			JSLoader.loadConfigurationJS(out, config);
			CSSLoader.loadRunViewer(out, config.getContextRootPath());
			JSLoader.loadRunViewer(out, config.getContextRootPath());
			out.println("</head>");

			out.println("<script>");
			out.println("var runViewer_" + guid + ";");
			out.println("Ext.onReady(function() {" 
			      + "runViewer_" + guid + " = new RunBrowser("
			        + "'" + guid + "', '" 
			        + runid + "', " + "'" 
			        + config.getScriptPath() + "', " 
			        + "'" + this.dataScript + "', " 
			        + "'" + this.templateScript + "' "
			        + ");\n" 
			      + "runViewer_" + guid + ".initialize();\n"
					+ "});");
			out.println("</script>");
			HTMLLoader.printFooter(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getRunListJSON() {
		ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
		ArrayList<Object> list = new ArrayList<Object>();
		for (RuntimePlan exe : monitor.getRunList()) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("runtimeInfo", exe.getRuntimeInfo());
			map.put("id", exe.getID());
			if (exe.getQueue() != null) {
				int numtotal = exe.getQueue().getAllSteps().size();
				int numdone = exe.getQueue().getFinishedSteps().size();
				ArrayList<RuntimeStep> running_steps = exe.getQueue().getRunningSteps();
				ArrayList<RuntimeStep> failed_steps = exe.getQueue().getFailedSteps();
				map.put("running_jobs", this.getStepIds(running_steps));
				map.put("failed_jobs", this.getStepIds(failed_steps));
				if(numtotal > 0) {
  				map.put("percent_done", numdone * 100.0 / numtotal);
  				map.put("percent_running", running_steps.size() * 100.0 / numtotal);
  				map.put("percent_failed", failed_steps.size() * 100.0 / numtotal);
				}
			}
			list.add(map);
		}
		return json.toJson(list);
	}

	private String getStepIds(ArrayList<RuntimeStep> steps) {
		ArrayList<String> ids = new ArrayList<String>();
		for (RuntimeStep stepexe : steps) {
			ids.add(stepexe.getName());
		}
		return ids.toString();
	}

	public String getRunJSON(String runid) {
		ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
		RuntimePlan plan = monitor.getRunDetails(runid);
		if(plan.getPlan() != null) {
  		for(ExecutionStep step : plan.getPlan().getAllExecutionSteps()) {
        for(ExecutionFile file : step.getInputFiles()) {
          file.loadMetadataFromLocation();
        }
  		  for(ExecutionFile file : step.getOutputFiles()) {
  		    file.loadMetadataFromLocation();
  		  }
  		}
		}
		return json.toJson(plan);
	}

	public String deleteRun(String rjson, ServletContext context) {
		HashMap<String, Object> ret = new HashMap<String, Object>();
		ret.put("success", false);
		JsonElement el = new JsonParser().parse(rjson);
		if (el == null)
			return json.toJson(ret);

		String runid = el.getAsJsonObject().get("id").getAsString();
		ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
		if (monitor.runExists(runid)) {
			this.stopRun(runid, context);
			if(!monitor.deleteRun(runid))
				return json.toJson(ret);
		}

		ret.put("success", true);
		return json.toJson(ret);
	}
	
	public boolean stopRun(String runid, ServletContext context) {
		ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
		if(monitor.getRunDetails(runid).getRuntimeInfo().getStatus() 
				== RuntimeInfo.Status.RUNNING) {
			PlanExecutionEngine engine = (PlanExecutionEngine) context.getAttribute("engine_" + runid);
			RuntimePlan rplan = (RuntimePlan) context.getAttribute("plan_" + runid);
			if(engine != null && rplan != null) {
				engine.abort(rplan);
				return true;
			}
		}
		return false;
	}

	public String runExpandedTemplate(String origtplid, String templatejson, 
	    String consjson, String seedjson, String seedconsjson, ServletContext context) {
		Gson json = JsonHandler.createTemplateGson();
		Template xtpl = JsonHandler.getTemplateFromJSON(json, templatejson, consjson);
		Template seedtpl = JsonHandler.getTemplateFromJSON(json, seedjson, seedconsjson);

		String requestid = UuidGen.generateAUuid("");
		WorkflowGenerationAPI wg = new WorkflowGenerationKB(props,
				DataFactory.getReasoningAPI(props), ComponentFactory.getReasoningAPI(props),
				ResourceFactory.getAPI(props), requestid);
		ExecutionPlan plan = wg.getExecutionPlan(xtpl);

		String seedid = UuidGen.generateURIUuid((URIEntity)seedtpl);
		if (plan != null) {
		  // Save the expanded template, seeded template and plan
		  if (!xtpl.save())
		    return "";
		  if (!seedtpl.saveAs(seedid))
		    return "";
		  plan.save();
			RuntimePlan rplan = new RuntimePlan(plan);
			rplan.setExpandedTemplateID(xtpl.getID());
			rplan.setOriginalTemplateID(origtplid);
			rplan.setSeededTemplateId(seedid);
			this.runExecutionPlan(rplan, context);
			return rplan.getID();
		}
		return "";
	}

	// Run the Runtime Plan
	public void runExecutionPlan(RuntimePlan rplan, ServletContext context) {
	  PlanExecutionEngine engine = config.getDomainExecutionEngine();
	  // "execute" below is an asynchronous call
	  engine.execute(rplan);

	  // Save the engine for an abort if needed
	  context.setAttribute("plan_" + rplan.getID(), rplan);
	  context.setAttribute("engine_" + rplan.getID(), engine);
	}
}
