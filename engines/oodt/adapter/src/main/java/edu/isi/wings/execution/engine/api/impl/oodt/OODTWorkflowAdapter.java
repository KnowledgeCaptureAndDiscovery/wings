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

import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.wmservices.client.WmServicesClient;
import org.apache.oodt.cas.workflow.structs.Graph;
import org.apache.oodt.cas.workflow.structs.ParentChildWorkflow;
import org.apache.oodt.cas.workflow.structs.Workflow;
import org.apache.oodt.cas.workflow.structs.WorkflowTask;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.system.XmlRpcWorkflowManagerClient;

public class OODTWorkflowAdapter {

  String codedir;
  String datadir;
  String jobdir;
  String wmurl;
  String fmurl;
  String wmsurl;
  String fmprefix;
  String wlogfile;

  XmlRpcWorkflowManagerClient wmclient;

  public OODTWorkflowAdapter(
    String wmurl,
    String wmsurl,
    String fmurl,
    String fmprefix,
    String codedir,
    String datadir,
    String jobdir,
    String wlogfile
  ) {
    this.wmurl = wmurl;
    this.wmsurl = wmsurl;
    this.fmurl = fmurl;
    this.fmprefix = fmprefix;

    try {
      this.wmclient = new XmlRpcWorkflowManagerClient(new URL(wmurl));
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    this.codedir = codedir;
    this.datadir = datadir;
    this.jobdir = jobdir;
    this.wlogfile = wlogfile;
  }

  public Workflow runWorkflow(RuntimePlan planexe, Metadata meta)
    throws Exception {
    // Get List of all Jobs
    HashMap<String, WorkflowTask> tasksById =
      new HashMap<String, WorkflowTask>();
    HashMap<String, String> opProducers = new HashMap<String, String>();
    HashMap<String, String> taskJobs = new HashMap<String, String>();
    for (RuntimeStep stepexe : planexe.getQueue().getAllSteps()) {
      ExecutionStep step = stepexe.getStep();

      ArrayList<String> inputs = new ArrayList<String>();
      ArrayList<String> outputs = new ArrayList<String>();
      String argstring = "";
      for (String argname : step.getInvocationArguments().keySet()) {
        argstring += argname + " ";
        for (Object arg : step.getInvocationArguments().get(argname)) {
          if (arg instanceof String) argstring += arg; else if (
            arg instanceof ExecutionFile
          ) argstring += this.jobdir + ((ExecutionFile) arg).getBinding();
          argstring += " ";
        }
      }
      for (ExecutionFile input : step.getInputFiles()) inputs.add(
        input.getBinding()
      );
      for (ExecutionFile output : step.getOutputFiles()) outputs.add(
        output.getBinding()
      );
      WorkflowTask task =
        this.getTask(planexe, stepexe, inputs, outputs, argstring);

      tasksById.put(stepexe.getName(), task);
      taskJobs.put(stepexe.getName(), step.getCodeBinding().getID());
      for (ExecutionFile output : step.getOutputFiles()) opProducers.put(
        output.getName(),
        stepexe.getName()
      );
    }

    // Get Parent Child Relationship between jobs
    HashMap<String, ArrayList<String>> parents =
      new HashMap<String, ArrayList<String>>();
    for (RuntimeStep stepexe : planexe.getQueue().getAllSteps()) {
      ExecutionStep step = stepexe.getStep();
      ArrayList<String> chparents = parents.get(stepexe.getName());
      if (chparents == null) chparents = new ArrayList<String>();
      for (ExecutionFile input : step.getInputFiles()) {
        if (opProducers.containsKey(input.getName())) chparents.add(
          opProducers.get(input.getName())
        );
      }
      parents.put(stepexe.getName(), chparents);
    }

    // Arrange Jobs into Job Levels
    TreeMap<Integer, ArrayList<String>> jobLevels =
      new TreeMap<Integer, ArrayList<String>>();
    for (String jobid : tasksById.keySet()) {
      Integer lvl = new Integer(getJobLevel(jobid, 0, parents));
      ArrayList<String> lvljobs;
      if (jobLevels.containsKey(lvl)) {
        lvljobs = jobLevels.get(lvl);
      } else {
        lvljobs = new ArrayList<String>();
        jobLevels.put(lvl, lvljobs);
      }
      lvljobs.add(jobid);
    }

    // Create workflow graph
    Graph graph = new Graph();
    graph.setExecutionType("sequential");
    graph.setModelId(planexe.getName());
    graph.setModelName(planexe.getName());

    ParentChildWorkflow pcw = new ParentChildWorkflow(graph);
    pcw.setId(planexe.getName());

    // Add Jobs to Workflow
    int jobnum = 1;
    for (Integer lvl : jobLevels.keySet()) {
      Graph subGraph = null;

      /*if(jobLevels.get(lvl).size() > 1) {
        subGraph = new Graph();
        subGraph.setParent(graph);
        subGraph.setExecutionType("parallel");
        subGraph.setModelId(planexe.getName()+"_level_"+lvl);
        subGraph.setModelName(planexe.getName()+"_level_"+lvl);
      }
      else {*/
      subGraph = graph;
      //}

      for (String jobid : jobLevels.get(lvl)) {
        WorkflowTask task = tasksById.get(jobid);
        String compid = taskJobs.get(jobid);
        // Set job number specific configs
        String taskid = "Job" + jobnum;
        task
          .getTaskConfig()
          .addConfigProperty("LOGFILE", task.getTaskName() + ".log");
        task.getTaskConfig().addConfigProperty("JOBID", taskid);
        task.getTaskConfig().addConfigProperty("COMPONENT_ID", compid);

        Graph taskGraph = new Graph();
        taskGraph.setExecutionType("task");
        taskGraph.setModelIdRef(task.getTaskId());
        taskGraph.setTask(task);

        taskGraph.setParent(subGraph);
        subGraph.getChildren().add(taskGraph);

        pcw.getTasks().add(task);
        jobnum++;
      }

      if (graph != subGraph) graph.getChildren().add(subGraph);
    }

    PrintStream wlog = new PrintStream(this.jobdir + this.wlogfile);
    wlog.println("Total Jobs: " + (jobnum - 1));
    wlog.close();

    try {
      WmServicesClient wmclient = new WmServicesClient(this.wmsurl);
      wmclient.addPackagedWorkflow(pcw.getId(), pcw);
      this.wmclient.sendEvent(pcw.getId(), meta);
      return pcw;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public boolean stopWorkflow(RuntimePlan exe) {
    try {
      return this.wmclient.stopWorkflowInstance(exe.getName());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private int getJobLevel(
    String jobid,
    int level,
    HashMap<String, ArrayList<String>> parents
  ) {
    int maxlvl = 0;
    ArrayList<String> parentIds = parents.get(jobid);
    if (parentIds == null || parentIds.size() == 0) {
      maxlvl = level;
    } else {
      for (String pjobid : parentIds) {
        int lvl = getJobLevel(pjobid, level + 1, parents);
        if (lvl > maxlvl) {
          maxlvl = lvl;
        }
      }
    }
    return maxlvl;
  }

  private WorkflowTask getTask(
    RuntimePlan planexe,
    RuntimeStep exe,
    ArrayList<String> ips,
    ArrayList<String> ops,
    String arg
  ) throws Exception {
    //checkAndCreateTask(this.wmclient, exe.getName(), exe.getName());
    WorkflowTask task = new WorkflowTask();
    //task.setConditions(Collections.emptyList());
    task.setRequiredMetFields(Collections.emptyList());
    task.setTaskId(planexe.getName() + "-" + exe.getName());
    task.setTaskInstanceClassName(
      "org.apache.oodt.cas.workflow.misc.WingsTask"
    );
    task.setTaskName(exe.getName());
    task.setTaskConfig(this.getTaskConfiguration(exe, ips, ops, arg));
    return task;
  }

  private WorkflowTaskConfiguration getTaskConfiguration(
    RuntimeStep exe,
    ArrayList<String> ips,
    ArrayList<String> ops,
    String arg
  ) {
    WorkflowTaskConfiguration config = new WorkflowTaskConfiguration();
    for (int i = 1; i <= ips.size(); i++) {
      config.addConfigProperty("INPUT" + i, ips.get(i - 1));
    }
    for (int i = 1; i <= ops.size(); i++) {
      config.addConfigProperty("OUTPUT" + i, ops.get(i - 1));
    }
    config.addConfigProperty("ARGUMENT", arg);
    config.addConfigProperty(
      "SCRIPT_PATH",
      exe.getStep().getCodeBinding().getLocation()
    );
    config.addConfigProperty("JOB_DIR", this.jobdir);
    config.addConfigProperty("DATA_DIR", this.datadir);
    config.addConfigProperty("FM_URL", this.fmurl);
    config.addConfigProperty("FM_PREFIX", this.fmprefix);
    config.addConfigProperty("W_LOGFILE", this.wlogfile);
    config.addConfigProperty("TASKNAME", exe.getName());
    return config;
  }
}
