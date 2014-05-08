package edu.isi.wings.execution.engine.classes;

import java.util.ArrayList;
import java.util.Date;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;
import edu.isi.wings.workflow.plan.api.ExecutionStep;

public class RuntimeStep extends URIEntity {
	private static final long serialVersionUID = 1L;
	
	ExecutionStep step;
	RuntimeInfo runtimeInfo;
	RuntimePlan runtimePlan;
	int logCount = 0;
	int logBatchSize = 20;
	
	ArrayList<RuntimeStep> parents;
	Process process;
	
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
	
	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
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
		this.getProcess().destroy();
	}
}
