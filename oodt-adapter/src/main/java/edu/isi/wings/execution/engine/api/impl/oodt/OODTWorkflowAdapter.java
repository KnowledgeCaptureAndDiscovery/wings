package edu.isi.wings.execution.engine.api.impl.oodt;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.Workflow;
import org.apache.oodt.cas.workflow.structs.WorkflowTask;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.system.XmlRpcWorkflowManagerClient;

import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;

public class OODTWorkflowAdapter {
	String codedir;
	String datadir;
	String jobdir;
	String wmurl;
	String fmurl;
	String fmprefix;
	String wlogfile;
	
	XmlRpcWorkflowManagerClient wmclient;
	
	public OODTWorkflowAdapter(String wmurl, String fmurl, String fmprefix,
			String codedir, String datadir,
			String jobdir, String wlogfile) {
		
		this.wmurl = wmurl;
		this.fmurl = fmurl;
		this.fmprefix = fmprefix;
		
		try {
			this.wmclient = new XmlRpcWorkflowManagerClient(new URL(wmurl));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		this.codedir = codedir;
		this.datadir = datadir;
		this.jobdir = jobdir;
		this.wlogfile = wlogfile;
	}
	
	public Workflow runWorkflow(RuntimePlan planexe) 
			throws Exception {
		
		Workflow wflow = new Workflow();
		wflow.setId(planexe.getID());
		
		// Get List of all Jobs
		HashMap<String, WorkflowTask> tasksById = new HashMap<String, WorkflowTask>();
		HashMap<String, String> opProducers = new HashMap<String, String>();
		for(RuntimeStep stepexe : planexe.getQueue().getAllSteps()) {
			ExecutionStep step = stepexe.getStep();
			
			ArrayList<String> inputs = new ArrayList<String>();
			ArrayList<String> outputs = new ArrayList<String>();
			String argstring = "";
			for(String argname : step.getInvocationArguments().keySet()) {
				argstring += argname + " ";
				for(Object arg : step.getInvocationArguments().get(argname)) {
					if(arg instanceof String)
						argstring += arg;
					else if(arg instanceof ExecutionFile)
						argstring += this.jobdir + ((ExecutionFile)arg).getBinding();
					argstring += " ";
				}
			}
			for(ExecutionFile input : step.getInputFiles())
				inputs.add(input.getBinding());
			for(ExecutionFile output : step.getOutputFiles())
				outputs.add(output.getBinding());

			WorkflowTask task = this.getTask(stepexe, inputs, outputs, argstring);
			
			tasksById.put(stepexe.getID(),  task);
			for(ExecutionFile output : step.getOutputFiles())
				opProducers.put(output.getID(), stepexe.getID());
		}
		
		// Get Parent Child Relationship between jobs
		HashMap <String, ArrayList<String>> parents = 
				new HashMap <String, ArrayList<String>>();
		for (RuntimeStep stepexe : planexe.getQueue().getAllSteps()) {
			ExecutionStep step = stepexe.getStep();
			ArrayList<String> chparents = parents.get(stepexe.getID());
			if (chparents == null)
				chparents = new ArrayList<String>();
			for (ExecutionFile input : step.getInputFiles()) {
				if (opProducers.containsKey(input.getID()))
					chparents.add(opProducers.get(input.getID()));
			}
			parents.put(stepexe.getID(), chparents);
		}

		// Arrange Jobs into Job Levels
		TreeMap<Integer, ArrayList<String>> jobLevels = new TreeMap<Integer, ArrayList<String>>();
		for (String jobid : tasksById.keySet()) {
			Integer lvl = new Integer(getJobLevel(jobid, 0, parents));
			ArrayList<String> lvljobs;
			if (jobLevels.containsKey(lvl)) {
				lvljobs = jobLevels.get(lvl);
			} else {
				lvljobs = new ArrayList<String>();
				jobLevels.put(lvl, lvljobs);
			}
			lvljobs.add(jobid);
		}
	    
	    // Add Jobs to Workflow
	    int jobnum = 1;
	    for (Integer lvl : jobLevels.keySet()) {
	      for (String jobid : jobLevels.get(lvl)) {
	    	  WorkflowTask task = tasksById.get(jobid);
	    	  // Set job number specific configs
	    	  String taskid = "Job"+jobnum;
	    	  task.getTaskConfig().addConfigProperty("LOGFILE", task.getTaskName()+".log");
	    	  task.getTaskConfig().addConfigProperty("JOBID", taskid);
	    	  wflow.getTasks().add(task);
	    	  jobnum++;
	      }
	    }

	    PrintStream wlog = new PrintStream(this.jobdir + this.wlogfile);
	    wlog.println("Total Jobs: "+(jobnum-1));
	    wlog.close();

	    this.wmclient.executeWorkflow(wflow, new Metadata());
		return wflow;
	}
	
	public boolean stopWorkflow(RuntimePlan exe) {
		try {
			return this.wmclient.stopWorkflowInstance(exe.getID());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private int getJobLevel(String jobid, int level, 
			HashMap <String, ArrayList<String>> parents) {
		int maxlvl = 0;
		ArrayList<String> parentIds = parents.get(jobid);
		if (parentIds == null || parentIds.size() == 0) {
			maxlvl = level;
		} else {
			for (String pjobid : parentIds) {
				int lvl = getJobLevel(pjobid, level + 1, parents);
				if (lvl > maxlvl) {
					maxlvl = lvl;
				}
			}
		}
		return maxlvl;
	}
	
	private WorkflowTask getTask(RuntimeStep exe, 
			ArrayList<String> ips, ArrayList<String> ops, String arg) 
					throws Exception {
		checkAndCreateTask(this.wmclient, exe.getID(), exe.getName());
	    WorkflowTask task = new WorkflowTask();
	    //task.setConditions(Collections.emptyList());
	    task.setRequiredMetFields(Collections.emptyList());
	    task.setTaskId(exe.getID());
	    task.setTaskInstanceClassName("org.apache.oodt.cas.workflow.misc.WingsTask");
	    task.setTaskName(exe.getName());
	    task.setTaskConfig(this.getTaskConfiguration(exe, ips, ops, arg));
	    return task;
	}
	
	private WorkflowTaskConfiguration getTaskConfiguration(RuntimeStep exe,
			ArrayList<String> ips, ArrayList<String> ops, String arg) {
	    WorkflowTaskConfiguration config = new WorkflowTaskConfiguration();
	    for(int i=1; i<=ips.size(); i++) {
	    	config.addConfigProperty("INPUT"+i, ips.get(i-1));
	    }
	    for(int i=1; i<=ops.size(); i++) {
	    	config.addConfigProperty("OUTPUT"+i, ops.get(i-1));
	    }
	    config.addConfigProperty("ARGUMENT", arg);
	    config.addConfigProperty("SCRIPT_PATH", exe.getStep().getCodeBinding().getLocation());
	    config.addConfigProperty("JOB_DIR", this.jobdir);
	    config.addConfigProperty("DATA_DIR", this.datadir);
	    config.addConfigProperty("FM_URL", this.fmurl);
	    config.addConfigProperty("FM_PREFIX", this.fmprefix);
	    config.addConfigProperty("W_LOGFILE", this.wlogfile);
	    config.addConfigProperty("TASKNAME", exe.getName());
	    return config;
	}
	
	private void checkAndCreateTask(XmlRpcWorkflowManagerClient client, String taskId, String tname) 
			throws Exception {
		WorkflowTask task = null;
		try {
			task = client.getTaskById(taskId);
		}
		catch (Exception e) {
			task = null;
		}
		if(task == null) {
			task = new WorkflowTask();
			task.setTaskId(taskId);
			task.setTaskInstanceClassName("org.apache.oodt.cas.workflow.misc.WingsTask");
		    task.setTaskName(tname);
			client.addTask(task);
		}
	}
}
