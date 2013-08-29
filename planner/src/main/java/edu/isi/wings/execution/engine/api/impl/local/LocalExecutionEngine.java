package edu.isi.wings.execution.engine.api.impl.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;

public class LocalExecutionEngine implements PlanExecutionEngine, StepExecutionEngine {
	
	private final ExecutorService executor;
	
	Properties props;
	protected int maxParallel = 4;
	
	StepExecutionEngine stepEngine;
	PlanExecutionEngine planEngine;
	
	ExecutionLoggerAPI monitor;
	
	public LocalExecutionEngine(Properties props) {
		this.props = props;
		if(props.containsKey("parallel"))
			this.maxParallel = Integer.parseInt(props.getProperty("parallel"));
		this.stepEngine = this;
		this.planEngine = this;
		executor = Executors.newFixedThreadPool(maxParallel);
	}


	@Override
	public void setExecutionLogger(ExecutionLoggerAPI monitor) {
		this.monitor = monitor;
		if(this.stepEngine != this)
			this.stepEngine.setExecutionLogger(monitor);
	}

	@Override
	public ExecutionLoggerAPI getExecutionLogger() {
		return this.monitor;
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
		exe.onStart(this.monitor);
		this.onStepEnd(exe);
	}
	
	@Override
	public void onStepEnd(RuntimePlan exe) {
		ArrayList<RuntimeStep> steps = exe.getQueue().getStepsReadyToExecute();
		if(steps.size() == 0) {
			// Nothing to execute. Check if finished
			if(exe.getQueue().getRunningSteps().size() == 0) {
				RuntimeInfo.Status status = RuntimeInfo.Status.FAILURE;
				if(exe.getQueue().getFinishedSteps().size() == exe.getQueue().getAllSteps().size())
					status = RuntimeInfo.Status.SUCCESS;
				exe.onEnd(this.monitor, status, "Finished");
				this.shutdown();
			}
		}
		else {
			// Run the runnable steps
			for(RuntimeStep stepexe : steps) {
				this.stepEngine.execute(stepexe, exe);
			}
		}
	}
	
	@Override
	public void execute(RuntimeStep exe, RuntimePlan planexe) {
		executor.submit(new StepExecutionThread(exe, planexe, planEngine, monitor));
	}

    class StepExecutionThread implements Runnable {
    	RuntimeStep exe;
    	RuntimePlan planexe;
    	PlanExecutionEngine planEngine;
    	ExecutionLoggerAPI logger;
    	
    	public StepExecutionThread(RuntimeStep exe, 
    			RuntimePlan planexe, PlanExecutionEngine planEngine,
    			ExecutionLoggerAPI monitor) {
    		this.exe = exe;
    		this.exe.setRuntimePlan(planexe);
    		this.planexe = planexe;
    		this.planEngine = planEngine;
    		this.logger = monitor;
    	}
    	
        @Override
        public void run() {
    		exe.onStart(this.logger);
    		try {
    			ArrayList<String> args = new ArrayList<String>();
    			args.add(exe.getStep().getCodeBinding().getLocation());

    			PrintWriter fout = null;
    			for(String argname : exe.getStep().getInvocationArguments().keySet()) {
    				ArrayList<String> values = exe.getStep().getInvocationArguments().get(argname);
    				if(argname.equals(">")) {
    					String outfile = values.get(0);
    					File f = new File(outfile);
    					f.getParentFile().mkdirs();
    					fout = new PrintWriter(f);
    				}
    				else {
    					args.add(argname);
    					for(String value: values)
    						args.add(value);
    				}
    			}
    			exe.onUpdate(this.logger, StringUtils.join(args, " "));
    			
    			ProcessBuilder pb = new ProcessBuilder(args);
    			Process p = pb.start();
    			exe.setProcess(p);
    			
    			p.waitFor();
    			
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

    			// Read error stream
        		line = "";
    			b = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    			while ((line = b.readLine()) != null) {
    				exe.onUpdate(this.logger, line);
    			}
    			b.close();
    			
    			if(fout != null)
    				fout.close();
    			
    			if(p.exitValue() == 0) 
    				exe.onEnd(this.logger, RuntimeInfo.Status.SUCCESS, "");
    			else
    				exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, "");
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
			this.executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// Do nothing
		}
		this.executor.shutdownNow();
	}
}
