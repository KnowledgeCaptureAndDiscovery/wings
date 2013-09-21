package edu.isi.wings.workflow.plan.api;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.wings.workflow.plan.classes.ExecutionCode;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;

public interface ExecutionStep {	

	public void setID(String id);
	
	public String getID();
	
	public String getNamespace();
	
	public String getName();
	
	public String getURL();
	
	// Precondition/Parent Steps
	public void addParentStep(ExecutionStep step);
	
	public ArrayList<ExecutionStep> getParentSteps();
	
	// Information to run the Step itself
	public void setCodeBinding(ExecutionCode executable);
	
	public ExecutionCode getCodeBinding();
	
	public HashMap<String, ArrayList<Object>> getInvocationArguments();
	
	public String getInvocationArgumentString();
	
	public void setInvocationArguments(HashMap<String, ArrayList<Object>> argumentMap);
	
	// Input/Output information for staging inptus and ingesting outputs (if used)
	public ArrayList<ExecutionFile> getInputFiles();
	
	public void addInputFile(ExecutionFile file);
	
	public ArrayList<ExecutionFile> getOutputFiles();
	
	public void addOutputFile(ExecutionFile file);
}
