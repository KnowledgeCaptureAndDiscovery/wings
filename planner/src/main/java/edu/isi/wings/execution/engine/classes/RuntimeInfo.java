package edu.isi.wings.execution.engine.classes;

import java.util.Date;

public class RuntimeInfo {
	public static enum Status { QUEUED, RUNNING, SUCCESS, FAILURE };
	
	Status status = Status.QUEUED;
	String log = "";
	Date startTime;
	Date endTime;
	
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public String getLog() {
		return log;
	}
	public void setLog(String log) {
		this.log = log;
	}
	public void addLog(String log) {
		this.log += log + "\n";
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
	public String toString() {
		return "\n"+status+"\n"+startTime+" -> "+endTime+"\n" + log;
	}
}
