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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import edu.isi.wings.opmm.Catalog;
import edu.isi.wings.portal.classes.config.Publisher;
import edu.isi.wings.portal.classes.config.ServerDetails;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import edu.isi.wings.opmm.WorkflowExecutionExport;
import edu.isi.wings.opmm.WorkflowTemplateExport;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.catalog.resource.classes.GridkitCloud;
import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimeInfo.Status;
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
import edu.isi.wings.workflow.template.classes.variables.Variable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class RunController {
  public Config config;
  public Gson json;

  public String dataUrl;
  public String templateUrl;

  private Properties props;

  public RunController(Config config) {
    this.config = config;
    this.json = JsonHandler.createRunGson();
    this.props = config.getProperties();
    this.dataUrl = config.getUserDomainUrl() + "/data";
    this.templateUrl = config.getUserDomainUrl() + "/workflows";
  }

  public String getRunListJSON() {
    return json.toJson(this.getRunList());
  }

  public ArrayList<HashMap<String, Object>> getRunList() {
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    for (RuntimePlan exe : monitor.getRunList()) {
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

  private String getPublishedURL(String runid) {
    Publisher publisher = config.getPublisher();
    if(publisher == null)
      return null;

    /* TODO: Return already published url for the run id if possible */
    /*
    Mapper opmm = new Mapper();
    String tstoreurl = publisher.getTstorePublishUrl();
    String puburl = publisher.getUrl();
    opmm.setPublishExportPrefix(puburl);

    String rname = runid.substring(runid.indexOf('#') + 1);
    String runurl = opmm.getRunUrl(rname);

    // Check if run already published
    if (graphExists(tstoreurl, runurl))
      return runurl;*/

    return null;
  }

  private Map<String, Object> getShortConstraints(Template tpl) {
    Map<String, Object> varbindings = new HashMap<String, Object>();
    for(Variable v : tpl.getVariables()) {
      List<Object> constraints = new ArrayList<Object>();
      if(v.isParameterVariable())
        continue;
      for(KBTriple t : tpl.getConstraintEngine().getConstraints(v.getID())) {
        Map<String, Object> cons = new HashMap<String, Object>();
        if(t.getPredicate().getName().equals("hasDataBinding"))
          continue;
        cons.put("p", t.getPredicate().getName());
        cons.put("o", t.getObject());
        constraints.add(cons);
      }
      varbindings.put(v.getID(), constraints);
    }
    return varbindings;
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
      if (!monitor.deleteRun(runid))
        return json.toJson(ret);
    }

    ret.put("success", true);
    return json.toJson(ret);
  }

  public boolean stopRun(String runid, ServletContext context) {
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    if (monitor.getRunDetails(runid).getRuntimeInfo().getStatus()
        == RuntimeInfo.Status.RUNNING) {
      PlanExecutionEngine engine = (PlanExecutionEngine) context.getAttribute("engine_" + runid);
      RuntimePlan rplan = (RuntimePlan) context.getAttribute("plan_" + runid);
      if (engine != null && rplan != null) {
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
    xtpl.autoLayout();
    Template seedtpl = JsonHandler.getTemplateFromJSON(json, seedjson, seedconsjson);

    String requestid = UuidGen.generateAUuid("");
    WorkflowGenerationAPI wg = new WorkflowGenerationKB(props,
        DataFactory.getReasoningAPI(props), ComponentFactory.getReasoningAPI(props),
        ResourceFactory.getAPI(props), requestid);
    
    ExecutionPlan plan = wg.getExecutionPlan(xtpl);
    
    String seedid = UuidGen.generateURIUuid((URIEntity) seedtpl);
    if (plan != null) {
      // Save the expanded template, seeded template and plan
      if (!xtpl.save())
        return "";
      
      if (!seedtpl.saveAs(seedid))
        return "";
      
      if(plan.save()) {
        RuntimePlan rplan = new RuntimePlan(plan);
        rplan.setExpandedTemplateID(xtpl.getID());
        rplan.setOriginalTemplateID(origtplid);
        rplan.setSeededTemplateId(seedid);
        this.runExecutionPlan(rplan, context);
        return rplan.getID();
      }
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

  public String publishRun(String runid) {
    HashMap<String, String> retmap = new HashMap<String, String>();
    ExecutionMonitorAPI monitor = config.getDomainExecutionMonitor();
    RuntimePlan plan = monitor.getRunDetails(runid);
    if (plan.getRuntimeInfo().getStatus() != Status.SUCCESS) {
      retmap.put("error", "Can only publish successfully completed runs");
    } else {
      try {
        //Mapper opmm = new Mapper();

        Publisher publisher = config.getPublisher();

        String tstoreurl = publisher.getTstorePublishUrl();
        String tstorequery = publisher.getTstoreQueryUrl();
        String exportName = publisher.getExportName();
        String upurl = publisher.getUploadServer().getUrl();

        //opmm.setPublishExportPrefix(puburl);

        String rname = runid.substring(runid.indexOf('#') + 1);
        //String runurl = opmm.getRunUrl(rname);

        // Fetch expanded template (to get data binding ids)
        TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);
        Template xtpl = tc.getTemplate(plan.getExpandedTemplateID());
        HashMap<String, String> varBindings = new HashMap<String, String>();
        for(Variable var: xtpl.getVariables()) {
          varBindings.put(var.getID(), var.getBinding().getID());
        }

        // Create a temporary directory to upload/move
        File _tmpdir = File.createTempFile("temp", "");
        File tempdir = new File(_tmpdir.getParent() + "/" + rname);
        FileUtils.deleteQuietly(tempdir);
        if(!_tmpdir.delete() || !tempdir.mkdirs())
          throw new Exception("Cannot create temp directory");


        /*
        File datadir = new File(tempdir.getAbsolutePath() + "/data");                
        File codedir = new File(tempdir.getAbsolutePath() + "/code");
        datadir.mkdirs();
        codedir.mkdirs();
        
        String tupurl = upurl + "/" + tempdir.getName();
        String dataurl  = tupurl + "/data";
        String codeurl  = tupurl + "/code";
        String cdir = props.getProperty("lib.domain.code.storage");
        String ddir = props.getProperty("lib.domain.data.storage");
        
        // Get files to upload && modify "Locations" to point to uploaded urls
        HashSet<ExecutionFile> uploadFiles = new HashSet<ExecutionFile>();
        HashSet<ExecutionCode> uploadCodes = new HashSet<ExecutionCode>();

        for (ExecutionStep step : plan.getPlan().getAllExecutionSteps()) {
          for(ExecutionFile file : step.getInputFiles())
            uploadFiles.add(file);
          for(ExecutionFile file : step.getOutputFiles())
            uploadFiles.add(file);
          uploadCodes.add(step.getCodeBinding());
        }

        for(ExecutionFile file : uploadFiles) {
          File copyfile = new File(file.getLocation());

          // Only upload files below a threshold file size
          long maxsize = publisher.getUploadServer().getMaxUploadSize();
          if(copyfile.length() > 0 &&
              (maxsize == 0 || copyfile.length() < maxsize)) {
            // Copy over file to temp directory
            FileUtils.copyFileToDirectory(copyfile, datadir);
            // Change file path in plan to the web accessible one 
            file.setLocation(file.getLocation().replace(ddir, dataurl));
          }
          else {
            String bindingid = varBindings.get(file.getID());
            file.setLocation(config.getServerUrl() + this.dataUrl+
                "/fetch?data_id=" + URLEncoder.encode(bindingid, "UTF-8"));
          }
        }
        for(ExecutionCode code : uploadCodes) {
          File copydir = null;
          if(code.getCodeDirectory() != null) {
            copydir = new File(code.getCodeDirectory());
            // Change path in plan to the web accessible one
            code.setCodeDirectory(code.getCodeDirectory().replace(cdir, codeurl));
          }
          else {
            File f = new File(code.getLocation());
            copydir = f.getParentFile();
          }
          // Copy over directory to temp directory
          FileUtils.copyDirectoryToDirectory(copydir, codedir);

          // Change path in plan to the web accessible one
          code.setLocation(code.getLocation().replace(cdir, codeurl));                  
        }

        // Upload data/code and delete tempdir
        uploadDirectory(publisher.getUploadServer(), tempdir);
        FileUtils.deleteQuietly(tempdir);
        */
        
        // Next round of creating tempdir
        tempdir.mkdirs();
        File dcontdir = new File(tempdir.getAbsolutePath() + "/ont/data");
        File acontdir = new File(tempdir.getAbsolutePath() + "/ont/components");
        File wflowdir = new File(tempdir.getAbsolutePath() + "/ont/workflows");
        File execsdir = new File(tempdir.getAbsolutePath() + "/ont/executions");

        File run_exportdir = new File(tempdir.getAbsolutePath() + "/export/run");
        File tpl_exportdir = new File(tempdir.getAbsolutePath() + "/export/template");
        dcontdir.mkdirs();
        acontdir.mkdirs();
        wflowdir.mkdirs();
        execsdir.mkdirs();
        run_exportdir.mkdirs();
        tpl_exportdir.mkdirs();
        
        // Merge both concrete and abstract component libraries
        String aclib = props.getProperty("lib.concrete.url");
        String abslib = props.getProperty("lib.abstract.url");
        String aclibdata = IOUtils.toString(new URL(aclib));
        String abslibdata = IOUtils.toString(new URL(abslib));
        abslibdata = abslibdata.replaceFirst("<\\?xml.+?>", "");
        abslibdata = Pattern.compile("<rdf:RDF.+?>", Pattern.DOTALL).matcher(abslibdata).replaceFirst("");
        abslibdata = abslibdata.replaceFirst("<\\/rdf:RDF>", "");
        aclibdata = aclibdata.replaceFirst("<\\/rdf:RDF>", "");
        aclibdata += abslibdata + "</rdf:RDF>\n";
        File aclibfile = new File(acontdir.getAbsolutePath() + "/library.owl");
        FileUtils.write(aclibfile, aclibdata);

        //obtain the .owl files
        File rplanfile = new File(execsdir.getAbsolutePath() + "/" + 
            plan.getName() + ".owl");
        String rplandata = IOUtils.toString(new URL(runid));
        FileUtils.write(rplanfile, rplandata);

        URL otplurl = new URL(plan.getOriginalTemplateID());
        File otplfile = new File(wflowdir.getAbsolutePath() + "/" + 
            otplurl.getRef() + ".owl");
        String otpldata = IOUtils.toString(otplurl);
        FileUtils.write(otplfile, otpldata);
        
        Catalog catalog = new Catalog(config.getDomainId(), exportName, 
            publisher.getDomainsDir(), aclibfile.getAbsolutePath());
        WorkflowExecutionExport exp = new WorkflowExecutionExport(
            rplanfile.getAbsolutePath(), catalog, exportName, tstorequery);
        exp.exportAsOPMW(run_exportdir.getAbsolutePath(), "RDF/XML");
        catalog.exportCatalog(null);
        for(File f : run_exportdir.listFiles()) {
          this.publishFile(tstoreurl, exp.getTransformedExecutionURI(), f.getAbsolutePath());
        }
        
        WorkflowTemplateExport texp = exp.getConcreteTemplateExport();
        if(texp != null) {
          texp.exportAsOPMW(tpl_exportdir.getAbsolutePath(), "RDF/XML");
          for(File f : tpl_exportdir.listFiles()) {
            this.publishFile(tstoreurl, 
                texp.getTransformedTemplateIndividual().getURI(),
                f.getAbsolutePath());
          }
        }
        
        FileUtils.deleteQuietly(tempdir);
        
        retmap.put("url", exp.getTransformedExecutionURI());

      } catch (Exception e) {
        e.printStackTrace();
        retmap.put("error", e.getMessage());
      }
    }
    return json.toJson(retmap);
  }


  private void uploadDirectory(ServerDetails server, File tempdir) {
    if(server.getHost() != null) {
      Machine m = new Machine(server.getHost());
      m.setHostName(server.getHost());
      m.setUserId(server.getHostUserId());
      m.setUserKey(server.getPrivateKey());
      HashMap<String, String> filemap = new HashMap<String, String>();
      String srvdir = server.getDirectory();
      for(File f : FileUtils.listFiles(tempdir, null, true)) {
        String fpath = f.getAbsolutePath();
        String srvpath = fpath.replace(tempdir.getParent(), srvdir);
        filemap.put(fpath, srvpath);
      }
      GridkitCloud.uploadFiles(m, filemap);
    }
    else {
      try {
        FileUtils.copyDirectoryToDirectory(tempdir, new File(server.getDirectory()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Upload triples to a rdf store
   * @param tstoreurl: triple store url. e.g., http://ontosoft.isi.edu:3030/provenance/data
   * @param graphurl: graph url.
   * @param filepath
   */
  private void publishFile(String tstoreurl, String graphurl, String filepath) {
    try {
      CloseableHttpClient httpClient = HttpClients.createDefault();
      HttpPut putRequest = new HttpPut(tstoreurl + "?graph=" + graphurl);

      int timeoutSeconds = 5;
      int CONNECTION_TIMEOUT_MS = timeoutSeconds * 1000;
      RequestConfig requestConfig = RequestConfig.custom()
          .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
          .setConnectTimeout(CONNECTION_TIMEOUT_MS)
          .setSocketTimeout(CONNECTION_TIMEOUT_MS)
          .build();
      putRequest.setConfig(requestConfig);

      File file = new File(filepath);
      String rdfxml = FileUtils.readFileToString(file);
      if (rdfxml != null) {
        StringEntity input = new StringEntity(rdfxml);
        input.setContentType("application/rdf+xml");

        putRequest.setEntity(input);
        httpClient.execute(putRequest);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean graphExists(String tstoreurl, String graphurl) {
    try {
      CloseableHttpClient httpClient = HttpClients.createDefault();            
      HttpGet getRequest = new HttpGet(tstoreurl + "?graph=" + graphurl);

      int timeoutSeconds = 5;
      int CONNECTION_TIMEOUT_MS = timeoutSeconds * 1000;
      RequestConfig requestConfig = RequestConfig.custom()
          .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
          .setConnectTimeout(CONNECTION_TIMEOUT_MS)
          .setSocketTimeout(CONNECTION_TIMEOUT_MS)
          .build();
      getRequest.setConfig(requestConfig);

      HttpResponse response = httpClient.execute(getRequest);
      if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        return true;
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
}
