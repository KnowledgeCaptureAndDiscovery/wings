package edu.isi.wings.execution.engine.api;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;

public interface StepExecutionEngine {
	
	public void execute(RuntimeStep exe, RuntimePlan plan);
	
	public void abort(RuntimeStep exe);
	
	public void setPlanExecutionEngine(PlanExecutionEngine engine);
	
	public PlanExecutionEngine getPlanExecutionEngine();
	
	public void setExecutionLogger(ExecutionLoggerAPI logger);

	public void setExecutionMonitor(ExecutionMonitorAPI monitor);
	
	public void setExecutionResource(ExecutionResourceAPI resource);
	 
	public ExecutionLoggerAPI getExecutionLogger();
	
	public ExecutionMonitorAPI getExecutionMonitor();
	
	public ExecutionResourceAPI getExecutionResource();
}
