package edu.isi.wings.workflow.plan.api;

import java.util.ArrayList;

public interface ExecutionPlan {
	
	// ID functions
	public void setID(String id);
	
	public String getID();
	
	public String getNamespace();
	
	public String getName();
	
	public String getURL();
	
	// Step functions
	public void addExecutionStep(ExecutionStep step);
	
	public ArrayList<ExecutionStep> getAllExecutionSteps();
	
	// Save
	public boolean save();
}
