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

package edu.isi.wings.execution.engine.api.impl.pegasus.dax;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import edu.isi.wings.common.SerializableObjectCloner;
import edu.isi.wings.common.kb.PropertiesHelper;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.WingsSet;
import edu.isi.wings.workflow.template.classes.variables.Variable;

public class DAX {
	String name;
	int index;
	int totalnum;
	int outputformat;

	HashMap<String, String> inputMaps = new HashMap<String, String>();
	HashMap<String, String> intermediateMaps = new HashMap<String, String>();
	HashMap<String, String> outputMaps = new HashMap<String, String>();

	public static int XML = 1;
	public static int SHELL = 2;

	// List of nodes
	HashMap<String, JobNode> nodeList = new HashMap<String, JobNode>();

	// List of actual jobs
	HashMap<String, Job> jobList = new HashMap<String, Job>();

	String instanceId;

	public void fillInParentJobs() {
		for (String nodeid : nodeList.keySet()) {
			JobNode node = nodeList.get(nodeid);

			// Check each parent node and get the corresponding parent jobs
			for (int k = 0; k < node.parentNodeIds.size(); k++) {
				JobNode parentnode = (JobNode) nodeList.get(node.parentNodeIds.get(k));

				for (int j = 0; j < node.jobIds.size(); j++) {
					String jobid = (String) node.jobIds.get(j);
					Job job = (Job) jobList.get(jobid);
					ArrayList<String> jobInputFiles = new ArrayList<String>();
					for (Binding binding : job.inputFiles) {
						ArrayList<Binding> cbs = new ArrayList<Binding>();
						cbs.add(binding);
						while (!cbs.isEmpty()) {
							Binding b = cbs.remove(0);
							if (b.isSet()) {
								for (WingsSet s : b) {
									cbs.add((Binding) s);
								}
							} else {
								jobInputFiles.add(b.getID());
							}
						}
					}

					// Find out which parent job provides an input to the
					// current job
					// - Make that the parent
					// - FIXME: We will not be able to handle control links this
					// way
					for (int i = 0; i < parentnode.jobIds.size(); i++) {
						String pjobid = (String) parentnode.jobIds.get(i);
						Job pjob = (Job) jobList.get(pjobid);
						// System.out.println(jobid+","+jobInputFiles+":"+pjobid+","+pjob.getOutputFiles());
						for (Binding binding : pjob.outputFiles) {
							ArrayList<Binding> cbs = new ArrayList<Binding>();
							cbs.add(binding);
							while (!cbs.isEmpty()) {
								Binding b = cbs.remove(0);
								if (b.isSet()) {
									for (WingsSet s : b) {
										cbs.add((Binding) s);
									}
								} else {
									if (jobInputFiles.contains(b.getID())) {
										if (!job.parentIds.contains(pjobid)) {
											job.parentIds.add(pjobid);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	String getDAXHead() {
		StringBuilder str = new StringBuilder();
		str.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		str.append("<!-- generated: " + new Date());
		str.append(" by Wings -->\n");
		str.append("<adag xsi:schemaLocation=\"http://pegasus.isi.edu/schema/DAX  http://pegasus.isi.edu/schema/dax-2.1.xsd\" \n"
				+ "xmlns=\"http://pegasus.isi.edu/schema/DAX\" \n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
				+ "version=\"1.10\" count=\""
				+ totalnum
				+ "\" index=\""
				+ index
				+ "\" name=\""
				+ name + "\">\n");
		return str.toString();
	}

	String getDAXPart2() {
		String str = "";
		String jobstr = "";

		str += "<!-- part 2: definition of all jobs -->\n";
		for (Object element : jobList.keySet()) {
			Job job = jobList.get(element);
			jobstr = "";
			jobstr += "   <job id=\"" + job.id + "\" namespace=\"" + job.namespace + "\" name=\""
					+ job.name + "\" version=\"" + job.version + "\">\n";
			jobstr += "      <argument>" + job.argumentStr + "</argument>\n";
			for (int j = 0; j < job.profileList.size(); j++) {
				jobstr += "      " + job.profileList.get(j) + "\n";
			}

			for (Binding binding : job.inputFiles) {
				ArrayList<Binding> cbs = new ArrayList<Binding>();
				cbs.add(binding);
				while (!cbs.isEmpty()) {
					Binding b = cbs.remove(0);
					if (b.isSet()) {
						for (WingsSet s : b) {
							cbs.add((Binding) s);
						}
					} else {
						jobstr += "      <uses file=\"" + b.getName()
								+ "\" link=\"input\" register=\"true\"/>\n";
					}
				}
			}

			for (Binding binding : job.outputFiles) {
				ArrayList<Binding> cbs = new ArrayList<Binding>();
				cbs.add(binding);
				while (!cbs.isEmpty()) {
					Binding b = cbs.remove(0);
					if (b.isSet()) {
						for (WingsSet s : b) {
							cbs.add((Binding) s);
						}
					} else {
						jobstr += "      <uses file=\"" + b.getName()
								+ "\" link=\"output\" register=\"true\"/>\n";
					}
				}
			}

			jobstr += "   </job>\n";
			str += jobstr;
		}
		return str;
	}

	String getDAXPart3() {
		StringBuilder str = new StringBuilder();
		str.append("<!-- part 3: list of control-flow dependencies (empty for single jobs) -->\n");
		for (Object element : jobList.keySet()) {
			Job job = (Job) jobList.get(element);
			if (job.parentIds.size() > 0) {
				StringBuilder jobstr = new StringBuilder();
				for (int j = 0; j < job.parentIds.size(); j++) {
					jobstr.append("   <child ref=\"" + job.id + "\">\n");
					jobstr.append("      <parent ref=\"" + job.parentIds.get(j) + "\"/>\n");
					jobstr.append("   </child>\n");
				}
				str.append(jobstr);
			}
		}
		return str.toString();
	}

	public void addJob(Job j) {
		jobList.put(j.id, j);
	}

	public void addNode(JobNode n) {
		nodeList.put(n.id, n);
	}

	String getDAXTail() {
		return "</adag>\n";
	}

	public String getString() {
		if (outputformat == DAX.SHELL) {
			return getShell();
		} else if (outputformat == DAX.XML) {
			return getDAX();
		}
		return "";
	}

	public String getDAX() {
		StringBuilder str = new StringBuilder();
		str.append(getDAXHead());
		str.append("\n");
		str.append(getDAXPart2());
		str.append("\n");
		str.append(getDAXPart3());
		str.append("\n");
		str.append(getDAXTail());
		return str.toString();
	}

	private int getJobLevel(Job job, int level) {
		int maxlvl = 0;
		if (job.parentIds.size() == 0) {
			maxlvl = level;
		} else {
			for (String pjobid : job.parentIds) {
				int lvl = getJobLevel(jobList.get(pjobid), level + 1);
				if (lvl > maxlvl) {
					maxlvl = lvl;
				}
			}
		}
		return maxlvl;
	}

	private String getShellCheck() {
		return "\nif [ $? -ne 0 ]; then echo '**Failure'; exit 1; fi";
	}

	public String getShell() {
		StringBuilder sb = new StringBuilder();
		String cdir = PropertiesHelper.getPCPropertyForCurrentDomain("components.dir");
		String ddir = PropertiesHelper.getDCPropertyForCurrentDomain("data.dir");

		sb.append("#!/bin/bash\n");
		sb.append("\n#Component Dir: " + cdir);
		sb.append("\n#Data Dir: " + ddir);

		TreeMap<Integer, ArrayList<Job>> jobLevels = new TreeMap<Integer, ArrayList<Job>>();

		for (Job job : jobList.values()) {
			Integer lvl = new Integer(getJobLevel(job, 0));
			ArrayList<Job> lvljobs;
			if (jobLevels.containsKey(lvl)) {
				lvljobs = jobLevels.get(lvl);
			} else {
				lvljobs = new ArrayList<Job>();
				jobLevels.put(lvl, lvljobs);
			}
			lvljobs.add(job);
		}
		sb.append("\n\necho '**Total Jobs: " + jobList.values().size() + "'");

		int jobNum = 0;
		for (Integer lvl : jobLevels.keySet()) {
			sb.append("\n\n#Level " + lvl + " jobs:\n#---------------");
			for (Job job : jobLevels.get(lvl)) {
				jobNum++;
				sb.append("\n\n#Job Id: " + job.id);
				String argstr = job.argumentStr;
				argstr = argstr.replaceAll("<filename file=\"", "\"");
				argstr = argstr.replaceAll("\\/>", "");
				argstr = argstr.replaceAll(" type=\"other\"", "");

				sb.append("\n\n#Copy input files over");
				for (Binding binding : job.inputFiles) {
					ArrayList<Binding> cbs = new ArrayList<Binding>();
					cbs.add(binding);
					while (!cbs.isEmpty()) {
						Binding b = cbs.remove(0);
						if (b.isSet()) {
							for (WingsSet s : b) {
								cbs.add((Binding) s);
							}
						} else {
							sb.append("\ncp \"" + ddir + "/" + b.getName() + "\" .");
							sb.append(getShellCheck());
						}
					}
				}

				sb.append("\n\n#Execute the component");
				sb.append("\necho \"**Job " + jobNum + ": " + job.name + "\" " + argstr);
				sb.append("\n\"" + cdir + "/" + job.name + "/run\" " + argstr);
				sb.append(getShellCheck());

				sb.append("\n\n#Copy output files to data dir (needed ?)");
				for (Binding binding : job.outputFiles) {
					ArrayList<Binding> cbs = new ArrayList<Binding>();
					cbs.add(binding);
					while (!cbs.isEmpty()) {
						Binding b = cbs.remove(0);
						if (b.isSet()) {
							for (WingsSet s : b) {
								cbs.add((Binding) s);
							}
						} else {
							sb.append("\ncp \"" + b.getName() + "\" \"" + ddir + "/\"");
							sb.append(getShellCheck());
						}
					}
				}
			}
		}
		sb.append("\n\necho '**Success'\n");

		return sb.toString();
	}

	HashMap<String, String> idmap;
	int idcount;

	private String getDotId(String id) {
		if (idmap.containsKey(id)) {
			return idmap.get(id);
		}
		String nid = "n" + idcount;
		idmap.put(id, nid);
		idcount++;
		return nid;
	}

	private String createDotNode(String id, String label, String shape, String color) {
		if (idmap.containsKey(id)) {
			return "";
		}
		return "\t" + getDotId(id) + "[label=\"" + label + "\" shape=\"" + shape
				+ "\" height=\"0.25\" style=\"filled\" fillcolor=\"" + color + "\"];\n";
	}

	private String createDotEdge(String id1, String id2) {
		return "\t" + getDotId(id1) + "->" + getDotId(id2) + ";\n";
	}

	public String getDot() {
		idmap = new HashMap<String, String>();
		idcount = 0;

		StringBuilder sb = new StringBuilder();

		sb.append("digraph " + getDotId(this.instanceId) + "{\n");

		TreeMap<Integer, ArrayList<Job>> jobLevels = new TreeMap<Integer, ArrayList<Job>>();

		for (Job job : jobList.values()) {
			Integer lvl = new Integer(getJobLevel(job, 0));
			ArrayList<Job> lvljobs;
			if (jobLevels.containsKey(lvl)) {
				lvljobs = jobLevels.get(lvl);
			} else {
				lvljobs = new ArrayList<Job>();
				jobLevels.put(lvl, lvljobs);
			}
			lvljobs.add(job);
		}

		for (Integer lvl : jobLevels.keySet()) {
			for (Job job : jobLevels.get(lvl)) {
				sb.append(createDotNode(job.id, job.name, "ellipse", "orange"));
				for (Binding binding : job.inputFiles) {
					ArrayList<Binding> cbs = new ArrayList<Binding>();
					cbs.add(binding);
					while (!cbs.isEmpty()) {
						Binding b = cbs.remove(0);
						if (b.isSet()) {
							for (WingsSet s : b) {
								cbs.add((Binding) s);
							}
						} else {
							sb.append(createDotNode(b.getName(),
									b.getName().length() < 32 ? b.getName() : "", "box", "cyan"));
							sb.append(createDotEdge(b.getName(), job.id));
						}
					}
				}

				for (Binding binding : job.outputFiles) {
					ArrayList<Binding> cbs = new ArrayList<Binding>();
					cbs.add(binding);
					while (!cbs.isEmpty()) {
						Binding b = cbs.remove(0);
						if (b.isSet()) {
							for (WingsSet s : b) {
								cbs.add((Binding) s);
							}
						} else {
							sb.append(createDotNode(b.getName(), "", "square", "cyan"));
							sb.append(createDotEdge(job.id, b.getName()));
						}
					}
				}
			}
		}
		sb.append("}\n");

		return sb.toString();
	}

	public void write(String filePath) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(filePath));
			out.println(getString());
			out.close();
			// System.out.println(getDot());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public void importDAX(DAX dax) {
		// import the job list
		for (Iterator<String> i = dax.jobList.keySet().iterator(); i.hasNext();) {
			String jobId = (String) i.next();
			Job daxJob = (Job) dax.jobList.get(jobId);
			if (jobList.containsKey(jobId)) {
				Job job = (Job) jobList.get(jobId);
				job.inputFiles.addAll(daxJob.inputFiles);
				job.outputFiles.addAll(daxJob.outputFiles);
			} else {
				jobList.put(jobId, daxJob);
			}
		}
	}

	public void setInstanceId(String id) {
		this.instanceId = id;
	}

	public String getInstanceId() {
		return this.instanceId;
	}

	private void fillMaps(HashMap<String, String> map, Variable[] vars) {
		for (Variable v : vars) {
			String vname = v.getName();
			ArrayList<Binding> vbs = new ArrayList<Binding>();
			Binding b = (Binding) SerializableObjectCloner.clone(v.getBinding());
			vbs.add(b);
			while (!vbs.isEmpty()) {
				Binding vb = vbs.remove(0);
				if (vb.isSet()) {
					for (WingsSet s : vb) {
						vbs.add((Binding) s);
					}
				} else {
					if (vb.isURIBinding()) {
						vb.increaseMinDimensionTo(1);
					}
				}
			}
			map.put(vname, b.toString());
		}
	}

	public void fillInputMaps(Variable[] vars) {
		fillMaps(inputMaps, vars);
	}

	public void fillOutputMaps(Variable[] vars) {
		fillMaps(outputMaps, vars);
	}

	public void fillIntermediateMaps(Variable[] vars) {
		fillMaps(intermediateMaps, vars);
	}

	public HashMap<String, String> getInputMaps() {
		return inputMaps;
	}

	public HashMap<String, String> getIntermediateMaps() {
		return intermediateMaps;
	}

	public HashMap<String, String> getOutputMaps() {
		return outputMaps;
	}

	public String getVariableBindingMapString() {
		StringBuilder str = new StringBuilder();
		str.append("input:");
		str.append(inputMaps.toString());
		str.append("\n");
		str.append("intermediate:");
		str.append(intermediateMaps.toString());
		str.append("\n");
		str.append("output:");
		str.append(outputMaps.toString());
		str.append("\n");
		return str.toString();
	}

	public void writeMapping(String filePath) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(filePath));
			out.println(this.getVariableBindingMapString());
			out.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	// Right now we don't parse the DAX, maybe we'll do that in the future
	// i.e. DAX Creation is NOT through a file as yet, but only through the API
	public DAX(String name, String type) {
		this.name = name;
		if (type.equals("xml")) {
			outputformat = DAX.XML;
		} else if (type.equals("shell")) {
			outputformat = DAX.SHELL;
		} else if (type.equals("oodt")) {
			outputformat = DAX.XML;
		} else {
			System.err.println("Invalid dax output format: " + type
					+ ". Allowed values are 'xml','shell'");
		}
	}

	public String toString() {
		return getID();
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void setTotalDaxes(int total) {
		this.totalnum = total;
	}

	public String getName() {
		return this.name;
	}

	public String getID() {
		return name + "_" + index;
	}

	public int getIndex() {
		return index;
	}

	public int getOutputFormat() {
		return this.outputformat;
	}

	public String getFile() {
		if (outputformat == DAX.SHELL) {
			return getID() + ".sh";
		} else if (outputformat == DAX.XML) {
			return getID() + ".dax";
		}
		return getID();
	}

}
