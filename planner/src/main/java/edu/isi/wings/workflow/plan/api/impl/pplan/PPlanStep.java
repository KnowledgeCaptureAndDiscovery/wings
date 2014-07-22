package edu.isi.wings.workflow.plan.api.impl.pplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionCode;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;

public class PPlanStep extends URIEntity implements ExecutionStep {
	private static final long serialVersionUID = 1L;
	
	transient Properties props;
	ArrayList<ExecutionStep> parentSteps;
	ArrayList<ExecutionFile> inputFiles;
	ArrayList<ExecutionFile> outputFiles;
	ArrayList<String> machineIds;
	
	HashMap<String, ArrayList<Object>> argumentNameValueMap;
	String invocationLine;
	String customData;
	ExecutionCode codeBinding;

	public PPlanStep(String id, Properties props) {
		super(id);
		this.props = props;
		
		inputFiles = new ArrayList<ExecutionFile>();
		outputFiles = new ArrayList<ExecutionFile>();
		parentSteps = new ArrayList<ExecutionStep>();
		machineIds = new ArrayList<String>();
		argumentNameValueMap = new HashMap<String, ArrayList<Object>>();
	}

	public void addInvocationLine(String s) {
		this.invocationLine = s;
	}

	public void addCustomData(String data) {
		this.customData = data;
	}

	public void setCodeBinding(ExecutionCode code) {
		this.codeBinding = code;
	}

	public String getCustomData() {
		return this.customData;
	}

	public String getInvocationLine() {
		return this.invocationLine;
	}

	@Override
	public ExecutionCode getCodeBinding() {
		return this.codeBinding;
	}

	@Override
	public void addParentStep(ExecutionStep step) {
		this.parentSteps.add(step);
	}

	@Override
	public ArrayList<ExecutionStep> getParentSteps() {
		return this.parentSteps;
	}

	@Override
	public HashMap<String, ArrayList<Object>> getInvocationArguments() {
		return this.argumentNameValueMap;
	}
	
	@Override
	public String getInvocationArgumentString() {
		String str = "";
		for(String argname : this.argumentNameValueMap.keySet()) {
			str += argname+" ";
			for(Object val : this.argumentNameValueMap.get(argname)) {
				if(val instanceof String)
					str += val;
				else if(val instanceof ExecutionFile) {
					ExecutionFile f = (ExecutionFile)val;
					if(f.getLocation() != null)
						str += f.getLocation();
					else
						str += f.getBinding();
				}
				str += " ";
			}
		}
		return str;
	}

	@Override
	public void setInvocationArguments(HashMap<String, ArrayList<Object>> argumentMap) {
		this.argumentNameValueMap = argumentMap;
		
	}

	@Override
	public ArrayList<ExecutionFile> getInputFiles() {
		return this.inputFiles;
	}

	@Override
	public void addInputFile(ExecutionFile file) {
		this.inputFiles.add(file);
	}

	@Override
	public ArrayList<ExecutionFile> getOutputFiles() {
		return this.outputFiles;
	}

	@Override
	public void addOutputFile(ExecutionFile file) {
		this.outputFiles.add(file);
	}

  @Override
  public ArrayList<String> getMachineIds() {
    return this.machineIds;
  }

  @Override
  public void setMachineIds(ArrayList<String> machineIds) {
    this.machineIds = machineIds;
  }
}
