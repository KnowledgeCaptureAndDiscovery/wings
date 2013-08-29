package edu.isi.wings.execution.engine;

import java.util.Properties;

import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;

public class ExecutionFactory {

	public static PlanExecutionEngine createPlanExecutionEngine(String impl, Properties props) 
			throws Exception {
		Class<?> classz = Class.forName(impl);
		return (PlanExecutionEngine) classz.getDeclaredConstructor(Properties.class).newInstance(props);
	}
	
	public static StepExecutionEngine createStepExecutionEngine(String impl, Properties props) 
			throws Exception {
		Class<?> classz = Class.forName(impl);
		return (StepExecutionEngine) classz.getDeclaredConstructor(Properties.class).newInstance(props);
	}
}
