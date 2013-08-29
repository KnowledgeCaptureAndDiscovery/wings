package edu.isi.wings.workflow.plan;

import java.util.Properties;

import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.api.impl.pplan.PPlan;
import edu.isi.wings.workflow.plan.api.impl.pplan.PPlanStep;

public class PlanFactory {

	public static ExecutionPlan createExecutionPlan(String impl, String id, Properties props) 
			throws Exception {
		Class<?> classz = Class.forName(impl);
		return (ExecutionPlan) classz.getDeclaredConstructor(String.class, Properties.class).newInstance(id, props);
	}
	
	public static ExecutionStep createExecutionStep(String impl, String id, Properties props) 
			throws Exception {
		Class<?> classz = Class.forName(impl);
		return (ExecutionStep) classz.getDeclaredConstructor(String.class, Properties.class).newInstance(id, props);
	}
	
	public static ExecutionPlan loadExecutionPlan(String id, Properties props) {
		return new PPlan(id, props, true);
	}
	
	public static ExecutionPlan createExecutionPlan(String id, Properties props) {
		return new PPlan(id, props);
	}
	
	public static ExecutionStep createExecutionStep(String id, Properties props) {
		return new PPlanStep(id, props);
	}
}
