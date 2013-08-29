package edu.isi.wings.execution.engine.api.impl.pegasus.dax;

import java.util.ArrayList;

public class JobNode {
	public String id;

	public ArrayList<String> parentNodeIds;

	public ArrayList<String> jobIds;

	public JobNode(String id) {
		this.id = id;
		parentNodeIds = new ArrayList<String>();
		jobIds = new ArrayList<String>();
	}

	/**
	 * Getter for property 'id'.
	 * 
	 * @return Value for property 'id'.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Setter for property 'id'.
	 * 
	 * @param id
	 *            Value to set for property 'id'.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Getter for property 'parentNodeIds'.
	 * 
	 * @return Value for property 'parentNodeIds'.
	 */
	public ArrayList<String> getParentNodeIds() {
		return parentNodeIds;
	}

	/**
	 * Setter for property 'parentNodeIds'.
	 * 
	 * @param parentNodeIds
	 *            Value to set for property 'parentNodeIds'.
	 */
	public void setParentNodeIds(ArrayList<String> parentNodeIds) {
		this.parentNodeIds = parentNodeIds;
	}

	/**
	 * Getter for property 'jobIds'.
	 * 
	 * @return Value for property 'jobIds'.
	 */
	public ArrayList<String> getJobIds() {
		return jobIds;
	}

	/**
	 * Setter for property 'jobIds'.
	 * 
	 * @param jobIds
	 *            Value to set for property 'jobIds'.
	 */
	public void setJobIds(ArrayList<String> jobIds) {
		this.jobIds = jobIds;
	}
}
