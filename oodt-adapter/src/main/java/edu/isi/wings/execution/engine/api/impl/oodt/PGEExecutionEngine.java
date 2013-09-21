package edu.isi.wings.execution.engine.api.impl.oodt;

import java.util.Properties;

import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;


public class PGEExecutionEngine implements StepExecutionEngine {

	String fmgrUri = "http://localhost:9000";
	String wmgrUri = "http://localhost:9001";
	
	public PGEExecutionEngine(Properties props) {
		
	}

	@Override
	public void execute(RuntimeStep exe, RuntimePlan plan) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void abort(RuntimeStep exe) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPlanExecutionEngine(PlanExecutionEngine engine) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PlanExecutionEngine getPlanExecutionEngine() {
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
}
