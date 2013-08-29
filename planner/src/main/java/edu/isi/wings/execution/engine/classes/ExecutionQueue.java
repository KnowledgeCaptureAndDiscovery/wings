package edu.isi.wings.execution.engine.classes;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;

public class ExecutionQueue {
	ExecutionPlan plan;
	
	ArrayList<RuntimeStep> steps;
	
	public ExecutionQueue() { 
	    	this.steps = new ArrayList<RuntimeStep>();
	}
	
	public ExecutionQueue(ExecutionPlan plan) {
		this.plan = plan;
	    	this.steps = new ArrayList<RuntimeStep>();
		this.initialize();
	}
	
	private void initialize() {
	    	HashMap<ExecutionStep, RuntimeStep> stepmap = 
	    		new HashMap<ExecutionStep, RuntimeStep>(); 
		for(ExecutionStep step : plan.getAllExecutionSteps()) {
			RuntimeStep exestep = new RuntimeStep(step);
			steps.add(exestep);
			stepmap.put(step, exestep);
		}
		for(ExecutionStep step : plan.getAllExecutionSteps()) {
		    RuntimeStep exestep = stepmap.get(step);
		    for(ExecutionStep pstep : step.getParentSteps()) {
			RuntimeStep exepstep = stepmap.get(pstep);
			if(exepstep != null)
			    exestep.addParent(exepstep);
		    }
		}
	}

	public ArrayList<RuntimeStep> getStepsReadyToExecute() {
		ArrayList<RuntimeStep> steps = new ArrayList<RuntimeStep>();
		for(RuntimeStep step : this.steps) {
			if(step.getRuntimeInfo().getStatus() == RuntimeInfo.Status.QUEUED) {
				boolean ok = true;
				for(RuntimeStep parentStep : step.getParents()) {
					if(parentStep != null &&
							parentStep.getRuntimeInfo().getStatus() != RuntimeInfo.Status.SUCCESS)
						ok = false;
				}
				if(ok)
					steps.add(step);
			}
		}
		return steps;
	}
	
	public ArrayList<RuntimeStep> getAllSteps() {
	    	return this.steps; 
	}
	
	public ArrayList<RuntimeStep> getFinishedSteps() {
		ArrayList<RuntimeStep> steps = new ArrayList<RuntimeStep>();
		for(RuntimeStep step : this.steps) {
			if(step.getRuntimeInfo().getStatus() == RuntimeInfo.Status.SUCCESS)
				steps.add(step);
		}
		return steps;
}
	
	public ArrayList<RuntimeStep> getFailedSteps() {
		ArrayList<RuntimeStep> steps = new ArrayList<RuntimeStep>();
		for(RuntimeStep step : this.steps) {
			if(step.getRuntimeInfo().getStatus() == RuntimeInfo.Status.FAILURE)
				steps.add(step);
		}
		return steps;
}
	
	public ArrayList<RuntimeStep> getRunningSteps() {
		ArrayList<RuntimeStep> steps = new ArrayList<RuntimeStep>();
		for(RuntimeStep step : this.steps) {
			if(step.getRuntimeInfo().getStatus() == RuntimeInfo.Status.RUNNING)
				steps.add(step);
		}
		return steps;
	}

	public ExecutionPlan getPlan() {
		return plan;
	}

	public void setPlan(ExecutionPlan plan) {
		this.plan = plan;
	}
	
	public void addStep(RuntimeStep step) {
	    	this.steps.add(step);
	}
	
	public void removeStep(RuntimeStep step) {
	    	this.steps.remove(step);
	}
}
