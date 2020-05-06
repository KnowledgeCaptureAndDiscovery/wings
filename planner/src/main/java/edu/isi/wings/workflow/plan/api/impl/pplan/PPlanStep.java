/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	public PPlanStep() {
	  super();
	}
	
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

  public HashMap<String, ArrayList<Object>> getArgumentNameValueMap() {
    return argumentNameValueMap;
  }

  public void setArgumentNameValueMap(HashMap<String, ArrayList<Object>> argumentNameValueMap) {
    this.argumentNameValueMap = argumentNameValueMap;
  }
}
