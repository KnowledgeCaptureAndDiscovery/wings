package edu.isi.wings.execution.engine.api.impl.distributed;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

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

public class DistributedExecutionEngine extends LocalExecutionEngine implements
    PlanExecutionEngine, StepExecutionEngine {
  
  public DistributedExecutionEngine(Properties props) {
    super(props);
  }
  
  @Override
  public void execute(RuntimeStep exe, RuntimePlan planexe) {
    executor.submit(new DistributedStepExecutionThread(exe, planexe, planEngine, 
        logger, resource));
    exe.onStart(this.logger);
  }

  class DistributedStepExecutionThread implements Runnable {
      RuntimeStep exe;
      RuntimePlan planexe;
      PlanExecutionEngine planEngine;
      ExecutionLoggerAPI logger;
      ExecutionMonitorAPI monitor;
      ExecutionResourceAPI resource;
      
      public DistributedStepExecutionThread(RuntimeStep exe, 
          RuntimePlan planexe, PlanExecutionEngine planEngine,
          ExecutionLoggerAPI logger, ExecutionResourceAPI resource) {
        this.exe = exe;
        this.exe.setRuntimePlan(planexe);
        this.planexe = planexe;
        this.planEngine = planEngine;
        this.logger = logger;
        this.resource = resource;
      }
      
      @Override
      public void run() {
        try {
          // Get machine ids first
          ArrayList<String> machineIds = exe.getStep().getMachineIds();
          Machine machine = null;
          for(String machineId : machineIds) {
            machine = this.resource.getMachine(machineId);
            if(machine.isHealthy()) 
              break; // Choose the first healthy machine
          }
          
          // If no healthy machine found. Log an error, and exit
          if(machine == null) {
            exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, 
                "No healthy machine to run "+exe.getStep().getName());
            return;
          }
          
          // If getCodeBinding returns $WINGS_STORAGE, then no change required 
          ArrayList<String> args = new ArrayList<String>();
          args.add(exe.getStep().getCodeBinding().getLocation());

          PrintWriter fout = null;
          // If getInvocationArguments returns $WINGS_STORAGE, then no change
          for(String argname : exe.getStep().getInvocationArguments().keySet()) {
            ArrayList<Object> values = exe.getStep().getInvocationArguments().get(argname);
            if(argname.equals(">")) {
              ExecutionFile outfile = (ExecutionFile) values.get(0);
              File f = new File(outfile.getLocation());
              f.getParentFile().mkdirs();
              fout = new PrintWriter(f);
            }
            else {
              args.add(argname);
              for(Object value: values) {
                if(value instanceof String)
                  args.add((String)value);
                else if(value instanceof ExecutionFile)
                  args.add(((ExecutionFile)value).getLocation());
              }
            }
          }
          exe.onUpdate(this.logger, StringUtils.join(args, " "));
          
          // TODO: Logon to machine. The following should happen in the remote machine
          
          // TODO: Check that all ExecutionFile, and CodeBinding locations exist
          // If not, then upload from local to remote
          // If yes, then check that remote is up-to-date
          
          // Create a temporary directory
          File tempdir = File.createTempFile(planexe.getName()+"-", "-"+exe.getName());
          if(!tempdir.delete() || !tempdir.mkdirs())
             throw new Exception("Cannot create temp directory");
          
          ProcessBuilder pb = new ProcessBuilder(args);
          pb.directory(tempdir);
          pb.redirectErrorStream(true);
          
          // TODO: Set environment variables ($WINGS_STORAGE, and other machine variables)
          
          Process p = pb.start();
          exe.setProcess(p);
          
          // Read output stream
          String line = "";
          BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
          while ((line = b.readLine()) != null) {
            if(fout != null)
              fout.println(line);
            else
              exe.onUpdate(this.logger, line);
          }
          b.close();
          
          if(fout != null)
            fout.close();

          p.waitFor();
          
          // Delete temp directory
          FileUtils.deleteDirectory(tempdir);
          
          if(p.exitValue() == 0) 
            exe.onEnd(this.logger, RuntimeInfo.Status.SUCCESS, "");
          else
            exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, "");
          
          // Logout of machine
        } 
        catch (Exception e) {
          exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, e.getMessage());
          //e.printStackTrace();
        }
        finally {
          this.planEngine.onStepEnd(planexe);
        }
      }
  }
}
