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

package edu.isi.wings.execution.engine.api.impl.distributed;

import edu.isi.wings.catalog.resource.classes.EnvironmentValue;
import edu.isi.wings.catalog.resource.classes.GridkitCloud;
import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.api.impl.local.LocalExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class DistributedExecutionEngine
  extends LocalExecutionEngine
  implements PlanExecutionEngine, StepExecutionEngine {

  public DistributedExecutionEngine(Properties props) {
    super(props);
  }

  @Override
  public void execute(RuntimeStep exe, RuntimePlan planexe) {
    Machine machine = this.selectStepMachine(exe);
    // If no healthy machine found. Log an error, and exit
    if (machine == null) {
      exe.onStart(this.logger);
      exe.onEnd(
        this.logger,
        RuntimeInfo.Status.FAILURE,
        "No healthy machine to run " + exe.getStep().getName()
      );
      return;
    }
    // If machine is localhost, just call execute for the LocalExecutionEngine
    if (machine.getName().equals("Localhost")) {
      super.execute(exe, planexe);
    } else {
      // Submit job on machine
      Future<?> job = executor.submit(
        new DistributedStepExecutionThread(
          exe,
          planexe,
          planEngine,
          logger,
          resource,
          machine
        )
      );
      exe.setProcess(job);
      exe.onStart(this.logger);
    }
  }

  private Machine selectStepMachine(RuntimeStep exe) {
    // Get machine ids first
    ArrayList<String> machineIds = exe.getStep().getMachineIds();
    ArrayList<Machine> healthyMachines = new ArrayList<Machine>();
    for (String machineId : machineIds) {
      Machine machine = this.resource.getMachine(machineId);
      if (machine.isHealthy()) healthyMachines.add(machine);
    }
    if (healthyMachines.size() == 0) return null;

    // Pick a random healthy machine for now
    // TODO: This should be based on machine load, connectivity, etc
    Random rand = new Random();
    int index = rand.nextInt(healthyMachines.size());
    return healthyMachines.get(index);
  }

  class DistributedStepExecutionThread implements Runnable {

    RuntimeStep exe;
    RuntimePlan planexe;
    PlanExecutionEngine planEngine;
    ExecutionLoggerAPI logger;
    ExecutionMonitorAPI monitor;
    ExecutionResourceAPI resource;
    Machine machine;
    Future<ProcessStatus> job;

    String localfolder = "";
    String remotefolder = "";
    ArrayList<String[]> uploadFiles;

    public DistributedStepExecutionThread(
      RuntimeStep exe,
      RuntimePlan planexe,
      PlanExecutionEngine planEngine,
      ExecutionLoggerAPI logger,
      ExecutionResourceAPI resource,
      Machine machine
    ) {
      this.exe = exe;
      this.exe.setRuntimePlan(planexe);
      this.planexe = planexe;
      this.planEngine = planEngine;
      this.logger = logger;
      this.resource = resource;
      this.machine = machine;
      this.uploadFiles = new ArrayList<String[]>();
    }

    private void addToUploadList(File f)
      throws FileNotFoundException, IOException {
      String oldf = f.getAbsolutePath();
      String newf = oldf.replace(localfolder, remotefolder);
      String md5 = DigestUtils.md5Hex(new FileInputStream(f));
      this.uploadFiles.add(new String[] { oldf, newf, md5 });
    }

    @Override
    public void run() {
      try {
        // Mark job as started
        this.exe.onStart(this.logger);
        if (exe.getStep().isSkipped()) {
          exe.onUpdate(this.logger, "This job has been SKIPPED");
          exe.onEnd(this.logger, RuntimeInfo.Status.SUCCESS, "");
          return;
        }

        this.localfolder = this.resource.getLocalStorageFolder();
        this.remotefolder = machine.getStorageFolder();

        // Add all items in code directory to the list of files to upload
        File codeDir = new File(
          exe.getStep().getCodeBinding().getCodeDirectory()
        );
        for (File f : FileUtils.listFiles(codeDir, null, true)) {
          this.addToUploadList(f);
        }
        // Add all input files to the list of files to upload
        for (ExecutionFile exf : exe.getStep().getInputFiles()) {
          File f = new File(exf.getLocation());
          this.addToUploadList(f);
        }

        // Create command arguments for the machine
        ArrayList<String> args = new ArrayList<String>();
        String codebin = exe
          .getStep()
          .getCodeBinding()
          .getLocation()
          .replace(localfolder, remotefolder);
        args.add(codebin);

        String outfilepath = null;
        for (String argname : exe.getStep().getInvocationArguments().keySet()) {
          ArrayList<Object> values = exe
            .getStep()
            .getInvocationArguments()
            .get(argname);
          if (argname.equals(">")) {
            ExecutionFile outfile = (ExecutionFile) values.get(0);
            outfilepath =
              outfile.getLocation().replace(localfolder, remotefolder);
          } else {
            args.add(argname);
            for (Object value : values) {
              if (value instanceof String) args.add((String) value); else if (
                value instanceof ExecutionFile
              ) {
                args.add(
                  ((ExecutionFile) value).getLocation()
                    .replace(localfolder, remotefolder)
                );
              }
            }
          }
        }

        // Logon to machine, and check the list of upload files
        // to see which ones really need to be uploaded
        this.uploadFiles =
          GridkitCloud
            .getNode(machine)
            .exec(new MachineUploadLister(this.uploadFiles));

        // Upload the required files (if any)
        if (this.uploadFiles.size() > 0) {
          exe.onUpdate(this.logger, "Uploading files to " + machine.getName());
          HashMap<String, String> uploadMap = new HashMap<String, String>();
          for (String[] fobj : this.uploadFiles) {
            uploadMap.put(fobj[0], fobj[1]);
          }
          GridkitCloud.uploadFiles(machine, uploadMap);
        }

        exe.onUpdate(this.logger, "Running on " + machine.getName());
        exe.onUpdate(this.logger, StringUtils.join(args, " "));

        // Run code on the machine
        HashMap<String, String> environment = new HashMap<String, String>();
        for (EnvironmentValue eval : machine.getEnvironmentValues()) {
          environment.put(eval.getVariable(), eval.getValue());
        }
        MachineCodeRunner mcr = new MachineCodeRunner(
          planexe.getName(),
          exe.getName(),
          codebin,
          args,
          outfilepath,
          environment
        );
        this.job = GridkitCloud.getNode(machine).submit(mcr);

        ProcessStatus status = this.job.get();
        exe.onUpdate(this.logger, status.getLog());

        if (status.exitValue() == 0) {
          // Fetch  outputs from the machine
          // Add all output files to the list of files to download
          HashMap<String, String> downloadMap = new HashMap<String, String>();
          for (ExecutionFile exf : exe.getStep().getOutputFiles()) {
            String fp = (new File(exf.getLocation())).getAbsolutePath();
            String newfp = fp.replace(localfolder, remotefolder);
            downloadMap.put(fp, newfp);
            downloadMap.put(fp + ".met", newfp + ".met"); // Also download .met files
          }
          exe.onUpdate(
            this.logger,
            "Downloading output files from " + machine.getName()
          );
          GridkitCloud.downloadFiles(machine, downloadMap);
          exe.onEnd(this.logger, RuntimeInfo.Status.SUCCESS, "");
        } else {
          exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, "");
        }
      } catch (InterruptedException e) {
        if (this.job != null) this.job.cancel(true);

        exe.onEnd(
          this.logger,
          RuntimeInfo.Status.FAILURE,
          "!! Stopping !! .. " + exe.getName() + " interrupted"
        );
      } catch (Exception e) {
        exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, e.getMessage());
        e.printStackTrace();
      } finally {
        //GridkitCloud.resetNode(machine);
        this.planEngine.onStepEnd(planexe);
      }
    }
  }
}

class MachineUploadLister
  implements Callable<ArrayList<String[]>>, Serializable {

  private static final long serialVersionUID = 5960512182954001309L;

  ArrayList<String[]> totalList;
  ArrayList<String[]> uploadList;

  public MachineUploadLister(ArrayList<String[]> list) {
    this.totalList = list;
    this.uploadList = new ArrayList<String[]>();
  }

  @Override
  public ArrayList<String[]> call() throws Exception {
    for (String[] fobj : totalList) {
      String newf = fobj[1];
      String oldmd5 = fobj[2];
      File f = new File(newf);
      if (!f.exists()) {
        if (!f.getParentFile().exists()) if (
          !f.getParentFile().mkdirs()
        ) throw new Exception("Could not create directories in remote node");
        uploadList.add(fobj);
      } else {
        String newmd5 = DigestUtils.md5Hex(new FileInputStream(f));
        if (!newmd5.equals(oldmd5)) uploadList.add(fobj);
      }
    }
    return uploadList;
  }
}

class MachineCodeRunner implements Callable<ProcessStatus>, Serializable {

  private static final long serialVersionUID = 5960512182954001309L;

  String planName;
  String exeName;
  String codeBinary;
  Process process;
  ArrayList<String> args;
  String outfilepath;
  HashMap<String, String> environment;

  public MachineCodeRunner(
    String planName,
    String exeName,
    String codeBinary,
    ArrayList<String> args,
    String outfilepath,
    HashMap<String, String> environment
  ) {
    this.planName = planName;
    this.exeName = exeName;
    this.codeBinary = codeBinary;
    this.args = args;
    this.outfilepath = outfilepath;
    this.environment = environment;
  }

  @Override
  public ProcessStatus call() throws Exception {
    File tempdir = File.createTempFile(planName + "-", "-" + exeName);
    if (!tempdir.delete() || !tempdir.mkdirs()) throw new Exception(
      "Cannot create temp directory"
    );

    ProcessStatus status = new ProcessStatus();
    try {
      File codef = new File(this.codeBinary);
      codef.setExecutable(true);

      PrintWriter fout = null;
      if (outfilepath != null) {
        File f = new File(outfilepath);
        f.getParentFile().mkdirs();
        fout = new PrintWriter(f);
      }

      ProcessBuilder pb = new ProcessBuilder(args);
      pb.directory(tempdir);
      pb.redirectErrorStream(true);

      // Set environment variables
      for (String var : this.environment.keySet()) pb
        .environment()
        .put(var, this.environment.get(var));

      this.process = pb.start();

      // Read output stream
      StreamGobbler outputGobbler = new StreamGobbler(
        this.process.getInputStream(),
        fout
      );
      outputGobbler.start();

      // Wait for the process to exit
      this.process.waitFor();

      status.setExitValue(this.process.exitValue());
      status.setLog(outputGobbler.getLog());
    } catch (InterruptedException e) {
      if (this.process != null) {
        //System.out.println("Stopping remote process");
        this.process.destroy();
      }
      status.setLog("!! Stopping Remotely !! .. " + exeName);
      status.setExitValue(-1);
    } catch (Exception e) {
      status.setLog(e.getMessage());
      status.setExitValue(-1);
    }

    // Delete temp directory
    FileUtils.deleteDirectory(tempdir);
    return status;
  }
}

class StreamGobbler extends Thread {

  InputStream is;
  String log;
  PrintWriter fout;

  public StreamGobbler(InputStream is, PrintWriter fout) {
    this.is = is;
    this.fout = fout;
    this.log = "";
  }

  public void run() {
    try {
      String line = "";
      BufferedReader b = new BufferedReader(new InputStreamReader(this.is));
      while ((line = b.readLine()) != null) {
        if (fout != null) fout.println(line); else this.log += line + "\n";
      }
      b.close();
      if (fout != null) fout.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getLog() {
    return this.log;
  }
}

class ProcessStatus implements Serializable {

  private static final long serialVersionUID = -3638512231716838756L;
  String log;
  int exitValue;

  public String getLog() {
    return log;
  }

  public void setLog(String log) {
    this.log = log;
  }

  public int exitValue() {
    return exitValue;
  }

  public void setExitValue(int exitValue) {
    this.exitValue = exitValue;
  }
}
