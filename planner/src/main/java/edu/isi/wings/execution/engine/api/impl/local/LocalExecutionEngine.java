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

package edu.isi.wings.execution.engine.api.impl.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import edu.isi.wings.catalog.resource.classes.EnvironmentValue;
import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.engine.classes.RuntimeInfo.Status;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;

public class LocalExecutionEngine implements PlanExecutionEngine, StepExecutionEngine {
	
	protected final ExecutorService executor;
	
	protected Properties props;
	protected int maxParallel = 4;
	
	protected StepExecutionEngine stepEngine;
	protected PlanExecutionEngine planEngine;
	
	protected ExecutionLoggerAPI logger;
	protected ExecutionMonitorAPI monitor;
	protected ExecutionResourceAPI resource;
	
	public LocalExecutionEngine(Properties props) {
		this.props = props;
		if(props.containsKey("parallel"))
			this.maxParallel = Integer.parseInt(props.getProperty("parallel"));
		this.stepEngine = this;
		this.planEngine = this;
		executor = Executors.newFixedThreadPool(maxParallel);
	}


	@Override
	public void setExecutionLogger(ExecutionLoggerAPI logger) {
		this.logger = logger;
		if(this.stepEngine != this)
			this.stepEngine.setExecutionLogger(logger);
	}

	@Override
	public ExecutionLoggerAPI getExecutionLogger() {
		return this.logger;
	}
	
  @Override
  public void setExecutionMonitor(ExecutionMonitorAPI monitor) {
    this.monitor = monitor;
    if (this.stepEngine != this)
      this.stepEngine.setExecutionMonitor(monitor);
  }

  @Override
  public ExecutionMonitorAPI getExecutionMonitor() {
    return this.monitor;
  }
  
  @Override
  public void setExecutionResource(ExecutionResourceAPI resource) {
    this.resource = resource;
    if(this.stepEngine != this)
      this.stepEngine.setExecutionResource(resource);
  }
  
  @Override
  public ExecutionResourceAPI getExecutionResource() {
    return this.resource;
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
	public void setPlanExecutionEngine(PlanExecutionEngine engine) {
		this.planEngine = engine;
	}


	@Override
	public PlanExecutionEngine getPlanExecutionEngine() {
		return this.planEngine;
	}
	
	@Override
	public void execute(RuntimePlan exe) {
	  exe.getRuntimeInfo().setStatus(Status.QUEUED);
		exe.onStart(this.logger);
		this.onStepEnd(exe);
	}
	
	@Override
	public void onStepEnd(RuntimePlan exe) {
	  // If aborted, shut it down
	  if(exe.getRuntimeInfo().getStatus() == Status.FAILURE) {
	    exe.onEnd(this.logger, Status.FAILURE, "Finished");
      this.shutdown();
      return;
	  }
	  
		ArrayList<RuntimeStep> steps = exe.getQueue().getNextStepsToExecute();
		if(steps.size() == 0 ) {
			// Nothing to execute. Check if finished
			if(exe.getQueue().getRunningSteps().size() == 0 &&
			    exe.getQueue().getQueuedSteps().size() == 0) {
			  String endlog = "Finished";
				RuntimeInfo.Status status = RuntimeInfo.Status.FAILURE;
				if(exe.getQueue().getFinishedSteps().size() 
				    == exe.getQueue().getAllSteps().size()) {
					if(exe.getPlan().isIncomplete()) {
					  // If the plan is incomplete, then replan and continue
            System.out.println("Replanning, and re-executing");
					  exe = this.monitor.rePlan(exe);
					  if(exe.getRuntimeInfo().getStatus() != 
					      RuntimeInfo.Status.FAILURE) {
					    this.onStepEnd(exe);
					    return;
					  }
					}
					else {
					  status = RuntimeInfo.Status.SUCCESS;
					}
				}
				exe.onEnd(this.logger, status, endlog);
				this.shutdown();
			}
		}
		else {
			// Run the runnable steps
			for(RuntimeStep stepexe : steps) {
			  stepexe.getRuntimeInfo().setStatus(Status.QUEUED);
			  //System.out.println("Queued "+stepexe.getName());
			}

			for(RuntimeStep stepexe : steps)
				this.stepEngine.execute(stepexe, exe);

		}
	}
	
	@Override
	public void execute(RuntimeStep exe, RuntimePlan planexe) {
	  Machine machine = this.selectStepMachine(exe);
		Future<?> job = 
		    executor.submit(new StepExecutionThread(exe, planexe, planEngine, logger, machine));
		exe.setProcess(job);
	}
	
  private Machine selectStepMachine(RuntimeStep exe) {
    for(String machineId : exe.getStep().getMachineIds())
      return this.resource.getMachine(machineId);
    return null;
  }

  class StepExecutionThread implements Runnable {
    	RuntimeStep exe;
    	RuntimePlan planexe;
    	PlanExecutionEngine planEngine;
    	ExecutionLoggerAPI logger;
    	Process process;
    	Machine machine;
    	
    	public StepExecutionThread(RuntimeStep exe, 
    			RuntimePlan planexe, PlanExecutionEngine planEngine,
    			ExecutionLoggerAPI logger, Machine machine) {
    		this.exe = exe;
    		this.exe.setRuntimePlan(planexe);
    		this.planexe = planexe;
    		this.planEngine = planEngine;
    		this.logger = logger;
    		this.machine = machine;
    	}
    	
      @Override
      public void run() {
    		try {
          // Mark job as started
          this.exe.onStart(this.logger);
          if(exe.getStep().isSkipped()) {
            exe.onUpdate(this.logger, "This job has been SKIPPED");
            exe.onEnd(this.logger, RuntimeInfo.Status.SUCCESS, "");
            return;
          }
          
    			ArrayList<String> args = new ArrayList<String>();
    			args.add(exe.getStep().getCodeBinding().getLocation());

    			PrintWriter fout = null;
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

          // Check if the outputs already exist, if so don't run
          boolean allExist = true;
          for (ExecutionFile file : exe.getStep().getOutputFiles()) {
            //file.removeMetadataFile();
            File f = new File(file.getLocation());
            if(!f.exists())
              allExist = false;
          }
          if(allExist) {
            exe.onEnd(this.logger, RuntimeInfo.Status.SUCCESS, 
                "Outputs already exist. Not running job");
          }
          else {
            // Create a temporary directory
            File tempdir = File.createTempFile(planexe.getName()+"-", "-"+exe.getName());
            if(!tempdir.delete() || !tempdir.mkdirs())
               throw new Exception("Cannot create temp directory");
            
            HashMap<String, String> environment = new HashMap<String, String>();
            if (machine != null) {
              for(EnvironmentValue eval : machine.getEnvironmentValues()) {
                environment.put(eval.getVariable(), eval.getValue());
              }
            }
            
      			ProcessBuilder pb = new ProcessBuilder(args);
      			pb.environment().putAll(environment);
      			pb.directory(tempdir);
      			pb.redirectErrorStream(true);
      			this.process = pb.start();
      			
      			//System.out.println("Running "+exe.getName());
      			// Read output stream
      			StreamGobbler outputGobbler = new 
                StreamGobbler(this.process.getInputStream(), exe, fout, this.logger);
      			outputGobbler.start();
  
      			// Wait for the process to exit
            this.process.waitFor();
            //System.out.println("Finished "+exe.getName());
            
      			// Delete temp directory
      			FileUtils.deleteDirectory(tempdir);
            
      			if(this.process.exitValue() == 0) 
      				exe.onEnd(this.logger, RuntimeInfo.Status.SUCCESS, "");
      			else
      				exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, "");
          }
    		}
    		catch (InterruptedException e) {
    		  if(this.process != null)
    		    this.process.destroy();

    		  exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, 
    		      "!! Stopping !! .. " + exe.getName() + " interrupted");
    		}
    		catch (Exception e) {
    			exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, e.getMessage());
    			e.printStackTrace();
    		}
    		finally {
    		  this.planEngine.onStepEnd(planexe);
    		}
      }
  }

	@Override
	public void abort(RuntimeStep exe) {
		exe.abort();
	}

	@Override
	public void abort(RuntimePlan exe) {
		exe.abort();
		this.shutdown();
	}
	
	@Override
	public int getMaxParallelSteps() {
		return this.maxParallel;
	}

	@Override
	public void setMaxParallelSteps(int num) {
		this.maxParallel = num;
	}

	private void shutdown() {		
		try {
			this.executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// Do nothing
		}
		this.executor.shutdownNow();
	}
}

class StreamGobbler extends Thread {
  InputStream is;
  RuntimeStep exe;
  PrintWriter fout;
  ExecutionLoggerAPI logger;
  int maxLinesLog = 500;

  public StreamGobbler (InputStream is, RuntimeStep exe, PrintWriter fout, 
      ExecutionLoggerAPI logger) {
    this.is = is;
    this.exe = exe;
    this.fout = fout;
    this.logger = logger;
  }
  
  public void run() {
    try {
      String line = "";
      int lineNum = 0;
      BufferedReader b = new BufferedReader(
          new InputStreamReader(this.is));
      while ((line = b.readLine()) != null) {
        if(lineNum < maxLinesLog) {
          if(fout != null)
            fout.println(line);
          else
            exe.onUpdate(this.logger, line);
        }
        else if(lineNum == maxLinesLog) {
          exe.onUpdate(this.logger, ".. Log is too long. Rest is truncated");
        }
        lineNum++;
      }
      b.close();
      if(fout != null)
        fout.close();
    }
    catch (Exception e) {
      exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, e.getMessage());
      e.printStackTrace();
    }
  }
}

