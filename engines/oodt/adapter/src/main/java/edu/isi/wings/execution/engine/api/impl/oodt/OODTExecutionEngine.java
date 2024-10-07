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

package edu.isi.wings.execution.engine.api.impl.oodt;

import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.classes.variables.Variable;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.oodt.cas.filemgr.datatransfer.DataTransfer;
import org.apache.oodt.cas.filemgr.datatransfer.RemoteDataTransferFactory;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;

public class OODTExecutionEngine
  implements PlanExecutionEngine, StepExecutionEngine {

  Properties props;
  protected int maxParallel = 4;

  StepExecutionEngine stepEngine;
  PlanExecutionEngine planEngine;

  OODTWorkflowAdapter adapter;
  Thread monitoringThread;

  protected ExecutionLoggerAPI logger;
  protected ExecutionMonitorAPI monitor;
  protected ExecutionResourceAPI resource;

  String jobdir;
  String wlogfile;

  public OODTExecutionEngine(Properties props) {
    this.props = props;
    this.stepEngine = this;
    this.planEngine = this;
  }

  @Override
  public void execute(RuntimeStep exe, RuntimePlan plan) {
    // Execute Step: Do nothing
  }

  @Override
  public void abort(RuntimeStep exe) {
    // Abort Step: Do nothing
  }

  @Override
  public void setPlanExecutionEngine(PlanExecutionEngine engine) {
    this.planEngine = engine;
  }

  @Override
  public PlanExecutionEngine getPlanExecutionEngine() {
    return this.planEngine;
  }

  @Override
  public void execute(RuntimePlan exe) {
    String wmurl = props.getProperty("oodt.wmurl");
    String fmurl = props.getProperty("oodt.fmurl");
    String wmsurl = props.getProperty("oodt.wmsurl");
    String libns = props.getProperty("lib.domain.data.url") + "#";

    String codedir =
      props.getProperty("lib.domain.code.storage") + File.separator;
    String datadir =
      props.getProperty("lib.domain.data.storage") + File.separator;

    // Get Variable metadata (predicted)
    Template tmpl = TemplateFactory.getTemplate(
      props,
      exe.getExpandedTemplateID()
    );
    org.apache.oodt.cas.metadata.Metadata meta =
      new org.apache.oodt.cas.metadata.Metadata();
    for (Variable var : tmpl.getVariables()) {
      org.apache.oodt.cas.metadata.Metadata vmeta =
        new org.apache.oodt.cas.metadata.Metadata();
      for (KBTriple t : tmpl
        .getConstraintEngine()
        .getConstraints(var.getID())) {
        vmeta.addMetadata(t.getPredicate().getID(), t.getObject().toString());
      }
      meta.addMetadata(var.getBinding().getName(), vmeta);
    }

    try {
      File f = File.createTempFile("oodt-run-", "");
      if (f.delete() && f.mkdirs()) {
        this.jobdir = f.getAbsolutePath() + File.separator;
        this.wlogfile = exe.getName() + ".log";
        this.adapter =
          new OODTWorkflowAdapter(
            wmurl,
            wmsurl,
            fmurl,
            libns,
            codedir,
            datadir,
            jobdir,
            wlogfile
          );
        this.adapter.runWorkflow(exe, meta);

        // Start Monitoring thread
        this.monitoringThread =
          new Thread(
            new ExecutionMonitoringThread(
              this,
              exe,
              this.logger,
              this.monitor,
              this.jobdir,
              datadir,
              this.wlogfile,
              fmurl,
              libns
            )
          );
        this.monitoringThread.start();
      }
    } catch (Exception e) {
      exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, e.getMessage());
      //e.printStackTrace();
    }
  }

  @Override
  public void onStepEnd(RuntimePlan exe) {
    // Do nothing
  }

  @Override
  public void abort(RuntimePlan exe) {
    // Abort plan
    if (
      this.monitoringThread != null && this.monitoringThread.isAlive()
    ) this.monitoringThread.interrupt();

    this.adapter.stopWorkflow(exe);
  }

  @Override
  public int getMaxParallelSteps() {
    return 0;
  }

  @Override
  public void setMaxParallelSteps(int num) {
    // Do nothing
  }

  @Override
  public void setStepExecutionEngine(StepExecutionEngine engine) {
    this.stepEngine = engine;
  }

  @Override
  public StepExecutionEngine getStepExecutionEngine() {
    return this.stepEngine;
  }

  @Override
  public void setExecutionLogger(ExecutionLoggerAPI logger) {
    this.logger = logger;
  }

  @Override
  public ExecutionLoggerAPI getExecutionLogger() {
    return this.logger;
  }

  class ExecutionMonitoringThread implements Runnable {

    RuntimePlan planexe;
    String jobdir;
    String datadir;
    String wlogfile;
    String fmurl;
    String libns;
    PlanExecutionEngine planEngine;
    ExecutionLoggerAPI logger;
    ExecutionMonitorAPI monitor;

    public ExecutionMonitoringThread(
      PlanExecutionEngine planEngine,
      RuntimePlan planexe,
      ExecutionLoggerAPI logger,
      ExecutionMonitorAPI monitor,
      String jobdir,
      String datadir,
      String wlogfile,
      String fmurl,
      String libns
    ) {
      this.planEngine = planEngine;
      this.planexe = planexe;
      this.logger = logger;
      this.monitor = monitor;
      this.jobdir = jobdir;
      this.datadir = datadir;
      this.wlogfile = wlogfile;
      this.fmurl = fmurl;
      this.libns = libns;
    }

    @Override
    public void run() {
      planexe.onStart(this.logger);
      try {
        HashMap<String, RuntimeInfo.Status> jobstatus = new HashMap<
          String,
          RuntimeInfo.Status
        >();

        int osleeptime = 1000;
        int maxsleeptime = 4 * osleeptime;
        int sleeptime = osleeptime;

        XmlRpcFileManagerClient fmclient = null;
        DataTransfer dt = null;
        try {
          fmclient = new XmlRpcFileManagerClient(new URL(fmurl));
          dt = new RemoteDataTransferFactory().createDataTransfer();
          dt.setFileManagerUrl(new URL(fmurl));
        } catch (Exception e) {
          e.printStackTrace();
          return;
        }

        Product prod = null;
        int count = 0;
        while (true) {
          try {
            prod = fmclient.getProductById(this.wlogfile);
            prod.setProductReferences(fmclient.getProductReferences(prod));
            break;
          } catch (Exception e) {
            Thread.sleep(2000);
            count++;
            if (count == 20) return;
          }
        }

        Pattern pattern = Pattern.compile(
          "^(Job\\d+)\\s+\\((.+)\\)\\s*:\\s+(.+)$"
        );
        while (true) {
          dt.retrieveProduct(prod, new File(jobdir));

          for (String line : FileUtils.readLines(
            new File(this.jobdir + this.wlogfile)
          )) {
            Matcher mat = pattern.matcher(line);
            if (mat.find()) {
              String jobname = mat.group(2);
              RuntimeInfo.Status status = RuntimeInfo.Status.valueOf(
                mat.group(3)
              );
              jobstatus.put(jobname, status);
            }
          }

          ArrayList<RuntimeStep> steps = new ArrayList<RuntimeStep>();
          steps.addAll(planexe.getQueue().getNextStepsToExecute());
          steps.addAll(planexe.getQueue().getRunningSteps());

          boolean shortsleep = false;
          for (RuntimeStep stepexe : steps) {
            String jobname = stepexe.getStep().getName();
            if (jobstatus.containsKey(jobname)) {
              if (
                stepexe.getRuntimeInfo().getStatus() ==
                RuntimeInfo.Status.QUEUED
              ) {
                stepexe.setRuntimePlan(planexe);
                stepexe.onStart(logger);
                shortsleep = true;
              }

              RuntimeInfo.Status status = jobstatus.get(jobname);
              if (
                status == RuntimeInfo.Status.SUCCESS ||
                status == RuntimeInfo.Status.FAILURE
              ) {
                // Fetch log file
                File f = new File(this.jobdir + jobname + ".log");
                String log = "";
                if (!f.exists()) {
                  try {
                    Product logprod = fmclient.getProductById(
                      planexe.getName() + "-" + f.getName()
                    );
                    logprod.setProductReferences(
                      fmclient.getProductReferences(logprod)
                    );
                    dt.retrieveProduct(logprod, new File(this.jobdir));
                    log = FileUtils.readFileToString(f);
                    fmclient.removeProduct(logprod);
                  } catch (Exception e) {}
                }
                stepexe.onEnd(logger, status, log);
                shortsleep = true;
              }
            }
          }

          // Retrieve output files (and metafiles)
          for (RuntimeStep stepexe : planexe.getQueue().getFinishedSteps()) {
            for (ExecutionFile file : stepexe.getStep().getOutputFiles()) {
              File f = new File(datadir + file.getName());
              if (!f.exists()) {
                try {
                  String outprodid = this.libns + file.getBinding();
                  Product outprod = fmclient.getProductById(outprodid);
                  outprod.setProductReferences(
                    fmclient.getProductReferences(outprod)
                  );
                  dt.retrieveProduct(outprod, new File(datadir));

                  Product metprod = fmclient.getProductById(outprodid + ".met");
                  if (metprod != null) {
                    metprod.setProductReferences(
                      fmclient.getProductReferences(metprod)
                    );
                    dt.retrieveProduct(metprod, new File(datadir));
                  }
                } catch (Exception e) {
                  //e.printStackTrace();
                }
              }
            }
          }

          steps = planexe.getQueue().getNextStepsToExecute();
          if (steps.size() == 0) {
            // Nothing to execute. Check if finished
            if (planexe.getQueue().getRunningSteps().size() == 0) {
              RuntimeInfo.Status status = RuntimeInfo.Status.FAILURE;
              if (
                planexe.getQueue().getFinishedSteps().size() ==
                planexe.getQueue().getAllSteps().size()
              ) {
                if (planexe.getPlan().isIncomplete()) {
                  // If the plan is incomplete, then replan and continue
                  System.out.println("Replanning, and re-executing");
                  planexe = this.monitor.rePlan(planexe);
                  if (
                    planexe.getRuntimeInfo().getStatus() ==
                    RuntimeInfo.Status.FAILURE
                  ) {
                    status = RuntimeInfo.Status.FAILURE;
                    planexe.onEnd(this.logger, status, "Finished");
                    fmclient.removeProduct(prod);
                    break;
                  } else {
                    // Quit monitoring and run the new plan
                    fmclient.removeProduct(prod);
                    this.planEngine.execute(planexe);
                    break;
                  }
                } else {
                  status = RuntimeInfo.Status.SUCCESS;
                  planexe.onEnd(this.logger, status, "Finished");
                  fmclient.removeProduct(prod);
                  break;
                }
              }
            }
          }

          sleeptime =
            shortsleep
              ? osleeptime
              : (sleeptime >= maxsleeptime ? maxsleeptime : sleeptime * 2);
          Thread.sleep(sleeptime);
        }
      } catch (Exception e) {
        this.planexe.onEnd(
            this.logger,
            RuntimeInfo.Status.FAILURE,
            e.getMessage()
          );
      }
    }
  }

  @Override
  public void setExecutionMonitor(ExecutionMonitorAPI monitor) {
    this.monitor = monitor;
    if (this.stepEngine != this) this.stepEngine.setExecutionMonitor(monitor);
  }

  @Override
  public ExecutionMonitorAPI getExecutionMonitor() {
    return this.monitor;
  }

  @Override
  public void setExecutionResource(ExecutionResourceAPI resource) {
    this.resource = resource;
    if (this.stepEngine != this) this.stepEngine.setExecutionResource(resource);
  }

  @Override
  public ExecutionResourceAPI getExecutionResource() {
    return this.resource;
  }
}
