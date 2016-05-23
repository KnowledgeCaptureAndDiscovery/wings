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

package edu.isi.wings.execution.engine.classes;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.wings.execution.engine.classes.RuntimeInfo.Status;
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
    HashMap<String, RuntimeStep> stepmap = 
        new HashMap<String, RuntimeStep>();
		for(ExecutionStep step : plan.getAllExecutionSteps()) {
			RuntimeStep exestep = new RuntimeStep(step);
			steps.add(exestep);
			stepmap.put(step.getID(), exestep);
		}
		for(ExecutionStep step : plan.getAllExecutionSteps()) {
      RuntimeStep exestep = stepmap.get(step.getID());
      for (ExecutionStep pstep : step.getParentSteps()) {
        RuntimeStep exepstep = stepmap.get(pstep.getID());
        if (exepstep != null)
          exestep.addParent(exepstep);
      }
		}
	}

	public ArrayList<RuntimeStep> getNextStepsToExecute() {
		ArrayList<RuntimeStep> steps = new ArrayList<RuntimeStep>();
		for(RuntimeStep step : this.steps) {
			if(step.getRuntimeInfo().getStatus() == Status.WAITING) {
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
	
	public ArrayList<RuntimeStep> getQueuedSteps() {
	  ArrayList<RuntimeStep> steps = new ArrayList<RuntimeStep>();
	  for(RuntimeStep step : this.steps) {
	    if(step.getRuntimeInfo().getStatus() == RuntimeInfo.Status.QUEUED)
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
