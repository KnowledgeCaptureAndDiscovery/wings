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

import java.util.Date;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.execution.engine.classes.RuntimeInfo.Status;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;

public class RuntimePlan extends URIEntity {
	private static final long serialVersionUID = 1L;

	ExecutionQueue queue;
	ExecutionPlan plan;
	RuntimeInfo runtimeInfo;
	
	String originalTemplateId;
	String expandedTemplateId;
	String seededTemplateId;
	
	public RuntimePlan(String id) {
    super(id);
	}
	
	public RuntimePlan(ExecutionPlan plan) {
		super(plan.getID());
		this.setID(UuidGen.generateURIUuid((URIEntity)plan));
		this.plan = plan;
		this.queue = new ExecutionQueue(plan);
		this.runtimeInfo = new RuntimeInfo();
	}

	public ExecutionQueue getQueue() {
		return queue;
	}

	public void setQueue(ExecutionQueue queue) {
		this.queue = queue;
	}

	public ExecutionPlan getPlan() {
		return plan;
	}

	public void setPlan(ExecutionPlan plan) {
		this.plan = plan;
	}
	
	public RuntimeInfo getRuntimeInfo() {
		return this.runtimeInfo;
	}
	
	public void setRuntimeInfo(RuntimeInfo info) {
		this.runtimeInfo = info;
	}
	
	public void onStart(ExecutionLoggerAPI logger) {
		logger.startLogging(this);
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
	
	public void waitFor() throws InterruptedException {
		while(true) {
			if(this.runtimeInfo.getStatus() != RuntimeInfo.Status.RUNNING)
				break;
			for(RuntimeStep step : queue.getRunningSteps()) {
				if(step.getProcess() != null && !step.getProcess().isDone())
		      Thread.sleep(1000);
			}
			Thread.sleep(500);
		}
	}
	
	public void abort() {
	  this.runtimeInfo.setStatus(Status.FAILURE);
		for(RuntimeStep exe : this.getQueue().getRunningSteps()) {
			exe.abort();
		}
	}
	
	public String getExpandedTemplateID() {
		return this.expandedTemplateId;
	}

	public String getOriginalTemplateID() {
		return this.originalTemplateId;
	}

	public void setExpandedTemplateID(String tid) {
		this.expandedTemplateId = tid;
	}

	public void setOriginalTemplateID(String tid) {
		this.originalTemplateId = tid;
	}

  public String getSeededTemplateID() {
    return seededTemplateId;
  }

  public void setSeededTemplateId(String seededTemplateId) {
    this.seededTemplateId = seededTemplateId;
  }
}
