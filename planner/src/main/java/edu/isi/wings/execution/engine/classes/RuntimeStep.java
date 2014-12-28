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
import java.util.Date;
import java.util.concurrent.Future;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.workflow.plan.api.ExecutionStep;

public class RuntimeStep extends URIEntity {
	private static final long serialVersionUID = 1L;
	
	ExecutionStep step;
	RuntimeInfo runtimeInfo;
	RuntimePlan runtimePlan;
	int logCount = 0;
	int logBatchSize = 20;
	
	ArrayList<RuntimeStep> parents;
	Future<?> process;
	
	public RuntimeStep(String id) {
	    super(id);
	}
	
	public RuntimeStep(ExecutionStep step) {
		super(step.getID());
		this.step = step;
		this.runtimeInfo = new RuntimeInfo();
		this.parents = new ArrayList<RuntimeStep>();
	}

	public ExecutionStep getStep() {
		return step;
	}

	public void setStep(ExecutionStep step) {
		this.step = step;
	}
	
	public void addParent(RuntimeStep step) {
		this.parents.add(step);
	}

	public ArrayList<RuntimeStep> getParents() {
		return this.parents;
	}
	
	public Future<?> getProcess() {
		return process;
	}

	public void setProcess(Future<?> process) {
		this.process = process;
	}
	
	public RuntimeInfo getRuntimeInfo() {
		return this.runtimeInfo;
	}
	
	public void setRuntimeInfo(RuntimeInfo info) {
		this.runtimeInfo = info;
	}
	
	public RuntimePlan getRuntimePlan() {
		return runtimePlan;
	}

	public void setRuntimePlan(RuntimePlan runtimePlan) {
		this.runtimePlan = runtimePlan;
	}

	public void onStart(ExecutionLoggerAPI logger) {
		this.runtimeInfo.setStatus(RuntimeInfo.Status.RUNNING);
		this.runtimeInfo.setStartTime(new Date());
		logger.updateRuntimeInfo(this);
	}
	
	public void onEnd(ExecutionLoggerAPI logger, RuntimeInfo.Status status, String log) {
		this.runtimeInfo.setStatus(status);
		this.runtimeInfo.addLog(log);
		this.runtimeInfo.setEndTime(new Date());
		logger.updateRuntimeInfo(this);
	}
	
	public void onUpdate(ExecutionLoggerAPI logger, String log) {
		this.runtimeInfo.addLog(log);
    // NOTE: Updating KB in batches of [logBatchSize]
		this.logCount++;
		if(this.logCount > this.logBatchSize) {
		  logger.updateRuntimeInfo(this);
		  this.logCount = 0;
		}
	}
	
	public void abort() {
		this.getProcess().cancel(true);
	}
}
