package edu.isi.wings.execution.engine.api.impl.oodt;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;

public class OODTExecutionEngine implements PlanExecutionEngine, StepExecutionEngine{
	Properties props;
	protected int maxParallel = 4;
	
	StepExecutionEngine stepEngine;
	PlanExecutionEngine planEngine;
	
	ExecutionLoggerAPI monitor;
	OODTWorkflowAdapter adapter;
	Thread monitoringThread;
	
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
		
		String codedir = props.getProperty("lib.domain.code.storage") + File.separator;
		String datadir = props.getProperty("lib.domain.data.storage") + File.separator;
		try {
			File f = File.createTempFile("oodt-run-", "");
			if (f.delete() && f.mkdirs()) {
				this.jobdir = f.getAbsolutePath() + File.separator;
				this.wlogfile = "workflow.log";
				this.adapter = new OODTWorkflowAdapter(wmurl, wmsurl, 
				    fmurl, libns,
						codedir, datadir,
						jobdir, wlogfile);
				this.adapter.runWorkflow(exe);

				// Start Monitoring thread
				this.monitoringThread = new Thread(
						new ExecutionMonitoringThread(exe, this.monitor, this.jobdir, this.wlogfile));
				this.monitoringThread.start();
			}
		} catch (Exception e) {
			exe.onEnd(this.monitor, RuntimeInfo.Status.FAILURE, e.getMessage());
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
		if(this.monitoringThread != null &&
				this.monitoringThread.isAlive())
			this.monitoringThread.interrupt();
		
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
	public void setExecutionLogger(ExecutionLoggerAPI monitor) {
			this.monitor = monitor;
	}

	@Override
	public ExecutionLoggerAPI getExecutionLogger() {
		return this.monitor;

	}
	
    class ExecutionMonitoringThread implements Runnable {
    	RuntimePlan planexe;
    	String jobdir;
    	String wlogfile;
    	PlanExecutionEngine planEngine;
    	ExecutionLoggerAPI logger;
    	
    	public ExecutionMonitoringThread(RuntimePlan planexe, 
    			ExecutionLoggerAPI monitor, String jobdir, String wlogfile) {
    		this.planexe = planexe;
    		this.logger = monitor;
    		this.jobdir = jobdir;
    		this.wlogfile = wlogfile;
    	}
    	
        @Override
        public void run() {
        	planexe.onStart(this.logger);
    		try {
				HashMap<String, RuntimeInfo.Status> jobstatus = 
						new HashMap<String, RuntimeInfo.Status>();
				
				int osleeptime = 1000;
				int maxsleeptime = 4*osleeptime;
				int sleeptime = osleeptime;
				
				Pattern pattern = Pattern.compile("^(Job\\d+)\\s+\\((.+)\\)\\s*:\\s+(.+)$");
    			while(true) { 
	    			for(String line : FileUtils.readLines(new File(this.jobdir + this.wlogfile))) {
	    				Matcher mat = pattern.matcher(line);
	    				if(mat.find()) {
	    					String jobname = mat.group(2);
	    					RuntimeInfo.Status status = RuntimeInfo.Status.valueOf(mat.group(3));
	    					jobstatus.put(jobname, status);
	    				}
	    			}
	    			
	    			ArrayList<RuntimeStep> steps = new ArrayList<RuntimeStep>();
	    			steps.addAll(planexe.getQueue().getStepsReadyToExecute());
	    			steps.addAll(planexe.getQueue().getRunningSteps());
	    			
	    			boolean shortsleep = false;
	    			for(RuntimeStep stepexe : steps) {
	    				String jobname = stepexe.getStep().getName();
	    				if(jobstatus.containsKey(jobname)) {
    						if(stepexe.getRuntimeInfo().getStatus() == 
    								RuntimeInfo.Status.QUEUED) {
    							stepexe.setRuntimePlan(planexe);
    							stepexe.onStart(logger);
    							shortsleep = true;
    						}
    						
	    					RuntimeInfo.Status status = jobstatus.get(jobname);
	    					if(status == RuntimeInfo.Status.SUCCESS ||
	    							status == RuntimeInfo.Status.FAILURE) {
	    						stepexe.onEnd(logger, status, 
	    								FileUtils.readFileToString(
	    										new File(this.jobdir + jobname + ".log")));
	    						shortsleep = true;
	    					}
	    				}
	    			}
	    			
	    			steps = planexe.getQueue().getStepsReadyToExecute();
	    			if(steps.size() == 0) {
	    				// Nothing to execute. Check if finished
	    				if(planexe.getQueue().getRunningSteps().size() == 0) {
	    					RuntimeInfo.Status status = RuntimeInfo.Status.FAILURE;
	    					if(planexe.getQueue().getFinishedSteps().size() == 
	    							planexe.getQueue().getAllSteps().size())
	    						status = RuntimeInfo.Status.SUCCESS;
	    					planexe.onEnd(this.logger, status, "Finished");
	    					break;
	    				}
	    			}
	    			
	    			sleeptime = shortsleep ? osleeptime : 
	    				(sleeptime >= maxsleeptime ? maxsleeptime : sleeptime*2);
	    			Thread.sleep(sleeptime);
    			}
    		}
    		catch (Exception e) {
    			this.planexe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, e.getMessage());
    		}
        }
    }

}
