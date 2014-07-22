package edu.isi.wings.execution.engine.api.impl.pegasus;

import java.util.Properties;

import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.logger.api.ExecutionMonitorAPI;

public class PegasusExecutionEngine implements PlanExecutionEngine {
	
	public PegasusExecutionEngine(Properties props) {
		
	}

	@Override
	public void execute(RuntimePlan exe) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStepEnd(RuntimePlan exe) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void abort(RuntimePlan exe) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMaxParallelSteps() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxParallelSteps(int num) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStepExecutionEngine(StepExecutionEngine engine) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public StepExecutionEngine getStepExecutionEngine() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExecutionLogger(ExecutionLoggerAPI monitor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ExecutionLoggerAPI getExecutionLogger() {
		// TODO Auto-generated method stub
		return null;
	}

  @Override
  public void setExecutionMonitor(ExecutionMonitorAPI monitor) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ExecutionMonitorAPI getExecutionMonitor() {
    // TODO Auto-generated method stub
    return null;
  }

}
