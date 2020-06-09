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

package edu.isi.wings.workflow.plan.api;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import edu.isi.wings.workflow.plan.api.impl.pplan.PPlanStep;
import edu.isi.wings.workflow.plan.classes.ExecutionCode;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;

@JsonDeserialize(as = PPlanStep.class)
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
	
	// Input/Output information for staging inputs and ingesting outputs (if used)
	public ArrayList<ExecutionFile> getInputFiles();
	
	public void addInputFile(ExecutionFile file);
	
	public ArrayList<ExecutionFile> getOutputFiles();
	
	public void addOutputFile(ExecutionFile file);
	
	// Machine information
	public ArrayList<String> getMachineIds();
	
	public void setMachineIds(ArrayList<String> machineIds);
	
}
