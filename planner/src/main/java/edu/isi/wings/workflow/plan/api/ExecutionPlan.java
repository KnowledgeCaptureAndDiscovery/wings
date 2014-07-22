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
	
	// Interleaving planning / execution
	public boolean isIncomplete();
	
	public void setIsIncomplete(boolean incomplete);
	
	// Save
	public boolean save();
	
	public boolean saveAs(String newid);
}
