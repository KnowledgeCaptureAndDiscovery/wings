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

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;

import edu.isi.wings.portal.classes.config.Publisher;
import edu.isi.wings.portal.classes.util.ComponentExecutingThread;
import edu.isi.wings.portal.classes.util.PlanningAPIBindings;
import edu.isi.wings.portal.classes.util.PlanningAndExecutingThread;
import edu.isi.wings.portal.classes.util.TemplateBindings;

import org.apache.commons.io.FileUtils;
import org.apache.http.cookie.Cookie;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.wings.opmm.WorkflowExecutionExport;
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
import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.variables.Variable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class RunController {
  public Config config;
  public Gson json;

  public String dataUrl;
  public String templateUrl;

  private Properties props;

  public static ExecutorService executor;
  public static HashMap<String, PlanningAPIBindings> apiBindings = new HashMap<String, PlanningAPIBindings>();

  public RunController(Config config) {
    if (config == null)
      System.out.println("Config is null");
    this.config = config;
    this.json = JsonHandler.createRunGson();
    this.props = config.getProperties();
    this.dataUrl = config.getUserDomainUrl() + "/data";
    this.templateUrl = config.getUserDomainUrl() + "/workflows";

    if (executor == null) {
      // System.out.println("Parallel:" + config.getPlannerConfig().getParallelism());
      executor = Executors.newFixedThreadPool(config.getPlannerConfig().getParallelism());
    }
  }

  public void end() {

  }

  /**
   * Get the run list json.
   *
   * @param pattern optional, a pattern to filter
   * @param status  optional, a pattern to filter complete runs
   * @param start   optional, start offset (for paging) (set to -1 to ignore)
   * @param limit   optional, number of runs to return (for paging) (set to -1 to
   *                ignore)
   * @return
   */
  public String getRunListJSON(String pattern, String status, int start, int limit) {
    HashMap<String, Object> result = new HashMap<String, Object>();
    int numberOfRuns = this.getNumberOfRuns(pattern, status, null);
    boolean fasterQuery = numberOfRuns > 1000;
    result.put("success", true);
    result.put("results", numberOfRuns);
    result.put("rows", this.getRunList(pattern, status, start, limit, fasterQuery));
    return json.toJson(result);
  }

  public String getRunListSimpleJSON(String pattern, String status, int start, int limit, Date started_after) {
    HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("success", true);
    result.put("results", this.getNumberOfRuns(pattern, status, started_after));
    result.put("rows", this.getRunListSimple(pattern, status, start, limit, started_after));
    return json.toJson(result);
  }

  public ArrayList<HashMap<String, Object>> getRunListSimple(String pattern, String status,
      int start, int limit, Date started_after) {
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    for (RuntimePlan exe : monitor.getRunListSimple(pattern, status, start, limit, started_after)) {
      HashMap<String, Object> map = new HashMap<String, Object>();
      map.put("runtimeInfo", exe.getRuntimeInfo());
      map.put("template_id", exe.getOriginalTemplateID());
      map.put("id", exe.getID());
      list.add(map);
    }
    return list;
  }

  public ArrayList<HashMap<String, Object>> getRunList(String pattern, String status, int start, int limit,
      boolean fasterQuery) {
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    for (RuntimePlan exe : monitor.getRunList(pattern, status, start, limit, fasterQuery)) {
      HashMap<String, Object> map = new HashMap<String, Object>();

      map.put("runtimeInfo", exe.getRuntimeInfo());
      map.put("template_id", exe.getOriginalTemplateID());
      map.put("id", exe.getID());

      if (exe.getQueue() != null) {
        int numtotal = exe.getQueue().getAllSteps().size();
        int numdone = exe.getQueue().getFinishedSteps().size();
        ArrayList<RuntimeStep> running_steps = exe.getQueue().getRunningSteps();
        ArrayList<RuntimeStep> failed_steps = exe.getQueue().getFailedSteps();
        map.put("running_jobs", this.getStepIds(running_steps));
        map.put("failed_jobs", this.getStepIds(failed_steps));
        if (numtotal > 0) {
          map.put("percent_done", numdone * 100.0 / numtotal);
          map.put("percent_running", running_steps.size() * 100.0 / numtotal);
          map.put("percent_failed", failed_steps.size() * 100.0 / numtotal);
        }
      }
      list.add(map);
    }
    return list;
  }

  public int getNumberOfRuns(String pattern, String status, Date started_after) {
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    return monitor.getNumberOfRuns(pattern, status, started_after);
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
    Map<String, Object> returnmap = new HashMap<String, Object>();
    RuntimePlan planexe = monitor.getRunDetails(runid);
    if (planexe != null && planexe.getPlan() != null) {
      for (ExecutionStep step : planexe.getPlan().getAllExecutionSteps()) {
        for (ExecutionFile file : step.getInputFiles()) {
          file.loadMetadataFromLocation();
        }
        for (ExecutionFile file : step.getOutputFiles()) {
          file.loadMetadataFromLocation();
        }
      }

      TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);
      Template tpl = tc.getTemplate(planexe.getExpandedTemplateID());
      tc.end();

      Map<String, Object> variables = new HashMap<String, Object>();
      variables.put("input", tpl.getInputVariables());
      variables.put("intermediate", tpl.getIntermediateVariables());
      variables.put("output", tpl.getOutputVariables());
      returnmap.put("variables", variables);
      returnmap.put("constraints", this.getShortConstraints(tpl));
    }
    returnmap.put("execution", planexe);
    returnmap.put("published_url", this.getPublishedURL(runid));

    return json.toJson(returnmap);
  }

  public String getRunPlanJSON(String runid) {
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    RuntimePlan planexe = monitor.getRunDetails(runid);
    return json.toJson(planexe);
  }

  private String getPublishedURL(String runid) {
    Publisher publisher = config.getPublisher();
    if (publisher == null)
      return null;

    /* TODO: Return already published url for the run id if possible */
    /*
     * Mapper opmm = new Mapper();
     * String tstoreurl = publisher.getTstorePublishUrl();
     * String puburl = publisher.getUrl();
     * opmm.setPublishExportPrefix(puburl);
     *
     * String rname = runid.substring(runid.indexOf('#') + 1);
     * String runurl = opmm.getRunUrl(rname);
     *
     * // Check if run already published
     * if (graphExists(tstoreurl, runurl))
     * return runurl;
     */

    return null;
  }

  private Map<String, Object> getShortConstraints(Template tpl) {
    Map<String, Object> varbindings = new HashMap<String, Object>();
    for (Variable v : tpl.getVariables()) {
      List<Object> constraints = new ArrayList<Object>();
      if (v.isParameterVariable())
        continue;
      for (KBTriple t : tpl.getConstraintEngine().getConstraints(v.getID())) {
        Map<String, Object> cons = new HashMap<String, Object>();
        if (t.getPredicate().getName().equals("hasDataBinding"))
          continue;
        cons.put("p", t.getPredicate().getName());
        cons.put("o", t.getObject());
        constraints.add(cons);
      }
      varbindings.put(v.getID(), constraints);
    }
    return varbindings;
  }

  public String deleteRuns(String rjson, ServletContext context) {
    HashMap<String, Object> ret = new HashMap<String, Object>();
    ret.put("success", false);
    JsonElement listel = new JsonParser().parse(rjson);
    if (listel == null)
      return json.toJson(ret);

    if (listel.isJsonObject()) {
      return this.deleteRun(rjson, context);
    }

    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();

    JsonArray list = listel.getAsJsonArray();
    for (int i = 0; i < list.size(); i++) {
      JsonElement el = list.get(i);
      String runid = el.getAsJsonObject().get("id").getAsString();
      monitor.deleteRun(runid, config.isDeleteRunOutputs());
    }

    ret.put("success", true);
    return json.toJson(ret);
  }

  public String deleteRun(String rjson, ServletContext context) {
    HashMap<String, Object> ret = new HashMap<String, Object>();
    ret.put("success", false);
    JsonElement el = new JsonParser().parse(rjson);
    if (el == null)
      return json.toJson(ret);

    String runid = el.getAsJsonObject().get("id").getAsString();
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    if (!monitor.deleteRun(runid, config.isDeleteRunOutputs()))
      return json.toJson(ret);
    /*
     * if (monitor.runExists(runid)) {
     * this.stopRun(runid, context);
     * if (!monitor.deleteRun(runid))
     * return json.toJson(ret);
     * }
     */

    ret.put("success", true);
    return json.toJson(ret);
  }

  public boolean stopRun(String runid, ServletContext context) {
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    if (monitor.getRunDetails(runid).getRuntimeInfo().getStatus() == RuntimeInfo.Status.RUNNING) {
      PlanExecutionEngine engine = (PlanExecutionEngine) context.getAttribute("engine_" + runid);
      RuntimePlan rplan = (RuntimePlan) context.getAttribute("plan_" + runid);
      if (engine != null && rplan != null) {
        engine.abort(rplan);
        return true;
      }
    }
    return false;
  }

  /*
   * Utility function to expand and run the first expanded template
   * - Immediately returns a run id
   * - Puts the rest of the processing in a Queue to be processed sequentially
   */
  public ArrayList<String> expandAndRunTemplate(TemplateBindings template_bindings, ServletContext context) {
    // Create a runid
    String ex_prefix = props.getProperty("domain.executions.dir.url");
    String template_id = template_bindings.getTemplateId();

    PlanningAPIBindings apis = null;
    if (apiBindings.containsKey(ex_prefix)) {
      apis = apiBindings.get(ex_prefix);
    } else {
      apis = new PlanningAPIBindings(props);
      apiBindings.put(ex_prefix, apis);
    }

    // Submit the planning and execution thread
    try {
      PlanningAndExecutingThread thread = new PlanningAndExecutingThread(ex_prefix, template_id,
          this.config, config.getPlannerConfig().getMaxQueueSize(), template_bindings, apis, executor, context);
      executor.submit(thread).get();

      // Return the runids
      return thread.getRunids();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public Future<?> runComponent(String cid, HashMap<String, Binding> role_bindings,
      String callbackUrl, Cookie[] callbackCookies, ServletContext context) {
    PlanningAPIBindings apis = null;
    String exPrefix = props.getProperty("domain.executions.dir.url");
    if (apiBindings.containsKey(exPrefix)) {
      apis = apiBindings.get(exPrefix);
    } else {
      apis = new PlanningAPIBindings(props);
      apiBindings.put(exPrefix, apis);
    }

    // Submit the planning and execution thread
    return executor
        .submit(new ComponentExecutingThread(cid, this.config, role_bindings, apis, callbackUrl, callbackCookies));
  }

  public static void invalidateCachedAPIs() {
    apiBindings.clear();
  }

  public String runExpandedTemplate(String origtplid, String templatejson,
      String consjson, String seedjson, String seedconsjson, String callbackUrl,
      ServletContext context) {

    Gson json = JsonHandler.createTemplateGson();
    Template xtpl = JsonHandler.getTemplateFromJSON(json, templatejson, consjson);
    xtpl.autoLayout();
    Template seedtpl = JsonHandler.getTemplateFromJSON(json, seedjson, seedconsjson);

    return createPlan(origtplid, context, xtpl, seedtpl, callbackUrl);
  }

  private String createPlan(String origtplid,
      ServletContext context, Template xtpl, Template seedtpl, String callbackUrl) {
    String requestid = UuidGen.generateAUuid("");
    WorkflowGenerationAPI wg = new WorkflowGenerationKB(props,
        DataFactory.getReasoningAPI(props), DataFactory.getCreationAPI(props),
        ComponentFactory.getReasoningAPI(props), ComponentFactory.getCreationAPI(props),
        ResourceFactory.getAPI(props), requestid);

    ExecutionPlan plan = wg.getExecutionPlan(xtpl);

    String seedid = UuidGen.generateURIUuid((URIEntity) seedtpl);
    if (plan != null) {
      // Save the expanded template, seeded template and plan
      if (!xtpl.save())
        return "";

      if (!seedtpl.saveAs(seedid))
        return "";

      if (plan.save()) {
        RuntimePlan rplan = new RuntimePlan(plan);
        rplan.setExpandedTemplateID(xtpl.getID());
        rplan.setOriginalTemplateID(origtplid);
        rplan.setSeededTemplateId(seedid);
        rplan.setCallbackUrl(callbackUrl);
        this.runExecutionPlan(rplan, context);
        return rplan.getID();
      }
    }
    return "";
  }

  public Response reRunPlan(String run_id, ServletContext context) {
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    RuntimePlan plan = monitor.getRunDetails(run_id);

    TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);
    String orig_tp_id = plan.getOriginalTemplateID();
    Template xtpl = tc.getTemplate(plan.getExpandedTemplateID());
    Template seedtpl = tc.getTemplate(plan.getSeededTemplateID());
    String callbackUrl = plan.getCallbackUrl();
    tc.end();

    if (createPlan(orig_tp_id, context, xtpl, seedtpl, callbackUrl) == "")
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Internal error").build();
    return Response.status(Response.Status.CREATED).entity("CREATED").build();
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

  /**
   * Publish a run to the Tstore
   *
   * @param runid
   * @return
   * @throws MalformedURLException
   */
  public String publishRun(String runid) throws MalformedURLException, IOException {
    PublisherRunController pc = new PublisherRunController(config, props, runid);
    pc.setPublishTriples(true);
    WorkflowExecutionExport exp = pc.generateProvenance(runid, null);
    HashMap<String, Object> response = pc.publish(exp);
    return json.toJson(response);
  }

  /**
   * Get provenance for a run
   *
   * @param
   * @return
   * @return
   * @throws IOException
   */
  public String getProvenance(String runid, String format) throws IOException {
    System.out.println("Starting provenance export");
    PublisherRunController pc = new PublisherRunController(config, props, runid);
    pc.setPublishTriples(true);
    WorkflowExecutionExport exp = pc.generateProvenance(runid, null);
    System.out.println("Generate provenance");
    HashMap<String, Object> response = pc.publish(exp);
    System.out.println("Publish");
    String executionPath = pc.executionFilePath;
    File executionFile = new File(executionPath);
    System.out.println("file ok" + executionFile.getAbsolutePath());
    if (executionFile.exists()) {
      String execution = FileUtils.readFileToString(executionFile);
      System.out.println("read file ok");
      response.put("execution", execution);
    }
    System.out.println("End provenance export");
    return json.toJson(response);
  }
}
