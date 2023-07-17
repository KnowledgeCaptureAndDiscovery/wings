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

package edu.isi.wings.execution.engine.api.impl.pegasus;

import edu.isi.pegasus.planner.dax.*;
import edu.isi.pegasus.planner.dax.File;
import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.ComponentTreeNode;
import edu.isi.wings.catalog.component.classes.requirements.ComponentRequirement;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.log4j.Logger;

public class PegasusWorkflowAdapter {

  ADAG adag = null;

  String codeDir = null;
  String dataDir = null;
  String pegasusHome = null;
  String cleanup = null;
  boolean force = false;

  Properties props = null;

  Set<String> inputs = null;
  Set<String> executables = null;

  private Map<String, ComponentRequirement> req = null;

  private Logger log = null;

  /**
   * @param props Properties with lib.domain.code.storage, lib.domain.data.storage, pegasus.home,
   * and pegasus.force properties.
   */
  public PegasusWorkflowAdapter(Properties props) throws Exception {
    this.props = props;
    this.codeDir =
      props.getProperty("lib.domain.code.storage") + java.io.File.separator;
    this.dataDir =
      props.getProperty("lib.domain.data.storage") + java.io.File.separator;
    this.pegasusHome =
      props.getProperty("pegasus.home") + java.io.File.separator;
    this.cleanup = props.getProperty("pegasus.cleanup", "inplace");
    this.force =
      Boolean.parseBoolean(props.getProperty("pegasus.force", "false"));

    this.inputs = new HashSet<String>();
    this.executables = new HashSet<String>();
    this.log = Logger.getLogger(this.getClass());

    // Is Pegasus directory valid?
    if (new java.io.File(pegasusHome).isDirectory()) {
      if (!new java.io.File(pegasusHome + "/bin/pegasus-plan").canExecute()) {
        log.error(
          "Invalid Pegasus Home: " + pegasusHome + "/bin/pegasus-plan not found"
        );
        throw new Exception(
          "Invalid Pegasus Home: " + pegasusHome + "/bin/pegasus-plan not found"
        );
      }
    } else {
      log.error("Invalid Pegasus Home: " + pegasusHome + " is not a directory");
      throw new Exception(
        "Invalid Pegasus Home: " + pegasusHome + " is not a directory"
      );
    }
  }

  public ADAG runWorkflow(
    RuntimePlan plan,
    String siteCatalog,
    String site,
    String baseDir
  ) throws Exception {
    log.debug("Plan Workflow: " + plan.getName());
    adag = new ADAG(plan.getName());

    // Construct map for components' requirement.
    constructComponentRequirementMap();

    // Add jobs to the workflow
    for (RuntimeStep step : plan.getQueue().getAllSteps()) {
      // Invoking Pegasus with --input-dir, so we don't need to create a Replica Catalog
      registerWithReplicaCatalog(step);

      // Register Wings components and there dependent files.
      registerWithTransformationCatalog(step);

      // Construct Pegasus Job for each Wings Node.
      buildAndRegisterJob(plan, step);
      // Job Dependencies
      // Pegasus planner automatically identifies dependencies based on input/output files used by a job
    }

    // Write DAX file to submit dir
    adag.writeToFile(baseDir + plan.getName() + ".dax");

    Process process = null;
    try {
      // Write Properties file to submit dir
      String props = writePropertiesFile(baseDir, siteCatalog);
      List<String> command = new ArrayList<String>();

      command.add(pegasusHome + "bin/pegasus-plan");
      command.add(props);

      command.add("--dax");
      command.add(baseDir + plan.getName() + ".dax");

      command.add("--dir");
      command.add(baseDir);

      command.add("--relative-submit-dir");
      command.add("submit");

      command.add("--output-dir");
      command.add(dataDir);

      command.add("--sites");
      command.add(site);

      command.add("--verbose");

      command.add("--cleanup");
      command.add(this.cleanup.toLowerCase());

      if (this.force) {
        command.add("--force");
      }

      command.add("--submit");

      // Execute pegasus-plan
      process = new ProcessBuilder(command).redirectErrorStream(true).start();

      writeOutStd(process, plan);

      process.waitFor();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    if (process.exitValue() == 0) {
      return adag;
    } else {
      log.debug(
        "Plan Failure: " + plan.getName() + " Exit Code: " + process.exitValue()
      );
      throw new Exception(
        "pegasus-plan failed with exit code " + process.exitValue()
      );
    }
  }

  /**
   * Write standard out from a process to the plan log.
   *
   * @param p    Process which has been executed
   * @param plan Plan whose logger should be used to write out the standard out.
   * @throws Exception
   */
  private void writeOutStd(Process p, RuntimePlan plan) throws Exception {
    String line;
    BufferedReader r = null;
    try {
      r = new BufferedReader(new InputStreamReader(p.getInputStream()));

      while ((line = r.readLine()) != null) {
        plan.getRuntimeInfo().addLog(line);
      }
    } catch (IOException e) {
      throw new Exception(e.getMessage());
    } finally {
      if (r != null) {
        r.close();
      }
    }
  }

  /**
   * @param submitDir   Directory where the properties file should be written
   * @param siteCatalog Location of site-catalog file
   */
  private String writePropertiesFile(String submitDir, String siteCatalog) {
    StringBuilder props = new StringBuilder();
    props.append("-Dpegasus.catalog.site.file=" + siteCatalog + " ");
    props.append(
      "-Dpegasus.catalog.master.url=" + "sqlite:///" + submitDir + "master.db "
    );
    props.append("-Dpegasus.metrics.app=wings");

    return props.toString();
  }

  /**
   *
   */
  private void constructComponentRequirementMap() {
    this.req = new HashMap<String, ComponentRequirement>();

    ComponentCreationAPI api = ComponentFactory.getCreationAPI(
      this.props,
      true
    );

    for (ComponentTreeNode n : api.getComponentHierarchy(true).flatten()) {
      if (n.getCls().getComponent() == null) {
        continue;
      }
      log.debug("Component ID: " + n.getCls().getComponent().getID());
      this.req.put(
          n.getCls().getComponent().getID(),
          n.getCls().getComponent().getComponentRequirement()
        );
    }
  }

  /**
   * Wings jobs can depend on multiple input files.
   * The method identifies files and registers them with replica catalog
   *
   * @param rStep Step whose input files are being registered.
   * @throws Exception
   */
  private void registerWithReplicaCatalog(RuntimeStep rStep) throws Exception {
    ExecutionStep eStep = rStep.getStep();

    for (ExecutionFile input : eStep.getInputFiles()) {
      String lfn = input.getBinding();
      Path pfn = Paths.get(input.getLocation());

      if (inputs.contains(lfn)) {
        continue;
      }

      log.debug(
        "Replica: " +
        lfn +
        " " +
        pfn.normalize().toUri() +
        " site=local Exists?: " +
        pfn.toFile().exists()
      );

      if (pfn.toFile().exists()) {
        File f = new File(lfn);
        f.addPhysicalFile(pfn.normalize().toUri().toString(), "local");
        adag.addFile(f);
      }

      inputs.add(lfn);
    }

    for (ExecutionFile output : eStep.getOutputFiles()) {
      String lfn = output.getBinding();
      Path pfn = Paths.get(output.getLocation());

      if (inputs.contains(lfn)) {
        continue;
      }

      log.debug(
        "Replica: " +
        lfn +
        " " +
        pfn.normalize().toUri() +
        " site=local Exists?: " +
        pfn.toFile().exists()
      );

      if (pfn.toFile().exists()) {
        File f = new File(lfn);
        f.addPhysicalFile(pfn.normalize().toUri().toString(), "local");
        adag.addFile(f);
      }

      inputs.add(lfn);
    }
  }

  /**
   * Wings components can contain of multiple files.
   * Example: wings-labkey/{run,io.sh,sub-dir-1/file-1,sub-dir-2/{file-1,file-2}}
   * The method identifies all dependencies, registers files with x bit as executables, and rest as data files
   * Transformation: wings-labkey
   * Executables: wings-labkey -> run, io.sh
   * Data files: sub-dir-1/file-1, sub-dir-2/file-1, sub-dir-2/file-2
   *
   * @param rStep Step whose code bindings are being registered.
   * @throws Exception
   */
  private void registerWithTransformationCatalog(RuntimeStep rStep)
    throws Exception {
    ExecutionStep eStep = rStep.getStep();
    String componentName = Paths
      .get(eStep.getCodeBinding().getCodeDirectory())
      .getFileName()
      .toString();

    if (executables.contains(componentName)) {
      return;
    }

    Transformation transformation = new Transformation(componentName);
    String dir =
      eStep.getCodeBinding().getCodeDirectory() + java.io.File.separator;
    Path profiles = Paths.get(dir + "__pegasus-job.properties");

    Executable executable = new Executable(componentName);
    executable.addPhysicalFile(new PFN("file://" + dir + "run"));
    executable.setInstalled(false);
    executable.setArchitecture(Executable.ARCH.X86_64);
    executable.setOS(Executable.OS.LINUX);

    if (profiles.toFile().exists()) {
      InputStream input = null;
      String key = null;
      String value = null;
      String namespace = null;

      try {
        Properties jobProfiles = new Properties();

        input = new FileInputStream(profiles.toFile());
        jobProfiles.load(input);

        Enumeration<?> e = jobProfiles.propertyNames();
        while (e.hasMoreElements()) {
          key = (String) e.nextElement();
          value = jobProfiles.getProperty(key);

          int indexOf = key.indexOf('.');
          if (indexOf == -1) {
            throw new Exception(
              "Invalid Pegasus profile \"" +
              key +
              "\" in __pegasus-job.properties of component " +
              componentName
            );
          }

          namespace = key.substring(0, indexOf);
          key = key.substring(indexOf + 1);
          executable.addProfile(
            Profile.NAMESPACE.valueOf(namespace.toLowerCase()),
            key,
            value
          );
        }
      } catch (IllegalArgumentException e) {
        throw new Exception(
          "Invalid Pegasus profile namespace \"" +
          namespace +
          "\" in __pegasus-job.properties of component " +
          componentName,
          e
        );
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (input != null) {
          try {
            input.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    transformation.uses(executable);

    adag.addExecutable(executable);

    log.debug("Transformation: " + componentName);

    Stack<String> stack = new Stack<String>();
    stack.push(dir);
    log.debug("Main Dir: " + dir);

    /*
     * Iterate over the component directory to register all files with the transformation catalog.
     * Executable Files are registered as such.
     */
    while (!stack.isEmpty()) {
      String currentDir = stack.pop();
      log.debug("Popped Dir: " + currentDir + " " + stack.size());

      for (java.io.File file : new java.io.File(currentDir).listFiles()) {
        if (
          file.getName().equals(".") ||
          file.getName().equals("..") ||
          file.getName().equals("run") ||
          (Files.exists(profiles) && Files.isSameFile(profiles, file.toPath()))
        ) {
          log.debug("Skipped: " + file.getName());
          continue;
        }

        if (file.isDirectory()) {
          stack.push(file.getAbsolutePath() + java.io.File.separator);
          log.debug("Push: " + stack.peek());
        } else {
          String name = file.getAbsolutePath().replaceAll(dir, "");
          if (file.canExecute()) {
            Executable e = new Executable(name);
            e.addPhysicalFile(new PFN("file://" + file.getAbsolutePath()));
            e.setInstalled(false);
            e.setArchitecture(Executable.ARCH.X86_64);
            e.setOS(Executable.OS.LINUX);

            transformation.uses(e);

            if (!executables.contains(name)) {
              adag.addExecutable(e);
            }
          } else {
            log.debug("File: " + name + " " + file.getAbsolutePath());
            File f = new File(name);
            f.addPhysicalFile(new PFN("file://" + file.getAbsolutePath()));
            transformation.uses(f);
            if (!executables.contains(name)) {
              adag.addFile(f);
            }
          }

          executables.add(name);
        }
      }
    }

    adag.addTransformation(transformation);
    executables.add(componentName);
  }

  /**
   * Construct Pegasus Job i.e. Add input/output files, arguments, etc.
   *
   * @param plan  Workflow Plan
   * @param rStep Steps for which Pegasus job is being built.
   * @return edu.isi.pegasus.planner.dax.Job
   * @throws Exception
   */
  private Job buildAndRegisterJob(RuntimePlan plan, RuntimeStep rStep)
    throws Exception {
    ExecutionStep eStep = rStep.getStep();
    String componentID = eStep.getCodeBinding().getID();
    String componentName = Paths
      .get(eStep.getCodeBinding().getCodeDirectory())
      .getFileName()
      .toString();

    Job job = new Job(rStep.getName(), componentName);
    log.debug("Workflow " + plan.getName());
    log.debug("\tJob Name: " + job.getName() + " Job ID: " + job.getId());
    log.debug("\tJob Code Dir:" + eStep.getCodeBinding().getCodeDirectory());

    // Arguments
    for (String name : eStep.getInvocationArguments().keySet()) {
      job.addArgument(name);

      for (Object value : eStep.getInvocationArguments().get(name)) {
        if (value instanceof String) {
          job.addArgument((String) value);
        } else if (value instanceof ExecutionFile) {
          job.addArgument(new File(((ExecutionFile) value).getBinding()));
        }
      }
    }

    // Input Files
    for (ExecutionFile input : eStep.getInputFiles()) {
      job.uses(new File(input.getBinding()), File.LINK.INPUT);
    }

    // Output Files
    for (ExecutionFile output : eStep.getOutputFiles()) {
      job.uses(new File(output.getBinding()), File.LINK.OUTPUT);
    }

    ComponentRequirement tmp = this.req.get(componentID);
    if (tmp.getMemoryGB() > 0) {
      job.addProfile(
        Profile.NAMESPACE.pegasus,
        "memory",
        (Math.round(Math.ceil(tmp.getMemoryGB() * 1024))) + ""
      );
    }

    if (tmp.getStorageGB() > 0) {
      job.addProfile(
        Profile.NAMESPACE.pegasus,
        "diskspace",
        (Math.round(Math.ceil(tmp.getStorageGB() * 1024))) + ""
      );
    }

    adag.addJob(job);
    return job;
  }

  /**
   * Stops a Pegasus workflow by executing pegasus-remove
   *
   * @param plan
   * @param baseDir
   * @return
   */
  public boolean stopWorkflow(RuntimePlan plan, String baseDir) {
    try {
      log.info(
        "Stop Workflow: " +
        pegasusHome +
        "bin/pegasus-remove " +
        baseDir +
        "submit"
      );
      Process process = new ProcessBuilder(
        pegasusHome + "bin/pegasus-remove",
        baseDir + "submit"
      )
        .redirectErrorStream(true)
        .start();
      process.waitFor();
      writeOutStd(process, plan);
      if (process.exitValue() == 0) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return false;
    }
  }
}
