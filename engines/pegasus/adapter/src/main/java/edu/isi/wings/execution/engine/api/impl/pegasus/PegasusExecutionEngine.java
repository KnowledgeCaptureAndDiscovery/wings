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

package edu.isi.wings.execution.engine.api.impl.pegasus;

import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.classes.ExecutionQueue;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PegasusExecutionEngine implements PlanExecutionEngine, StepExecutionEngine {
    String baseDir;
    Properties props;
    Thread monitoringThread;
    PlanExecutionEngine planEngine;
    StepExecutionEngine stepEngine;
    PegasusWorkflowAdapter adapter;

    private Logger log = null;

    protected ExecutionLoggerAPI logger;
    protected ExecutionMonitorAPI monitor;
    protected ExecutionResourceAPI resource;

    public PegasusExecutionEngine(Properties props) {
        this.props = props;
        this.stepEngine = this;
        this.planEngine = this;
        this.log = Logger.getLogger(this.getClass());
    }

    @Override
    public void execute(RuntimePlan exe) {
        String pegasusHome = props.getProperty("pegasus.home") + File.separator;
        String siteCatalog = props.getProperty("pegasus.site-catalog");
        String site = props.getProperty("pegasus.site", "local");
        String storageDir = props.getProperty("pegasus.storage-dir", FileUtils.getTempDirectoryPath()) + File.separator;

        try {
            // Create base directory
            File baseDir = new File(storageDir + exe.getName());
            FileUtils.forceMkdir(baseDir);

            this.baseDir = baseDir.getAbsolutePath() + File.separator;
            this.adapter = new PegasusWorkflowAdapter(this.props);

            String submitDir = this.baseDir + "submit" + File.separator;

            // Construct Pegasus Workflow, and submit it for execution
            this.adapter.runWorkflow(exe, siteCatalog, site, this.baseDir);

            // Start Monitoring thread
            this.monitoringThread = new Thread(new ExecutionMonitoringThread(this, exe, logger, monitor,
                    submitDir, pegasusHome));
            this.monitoringThread.start();

        } catch (Exception e) {
            exe.onStart(this.logger);
            exe.onEnd(this.logger, RuntimeInfo.Status.FAILURE, e.getMessage());
            log.error(e.getMessage(), e);
        }

    }

    @Override
    public void onStepEnd(RuntimePlan exe) {
        // Do nothing
    }

    /**
     * * Stops a Pegasus workflow by executing pegasus-remove
     *
     * @param exe
     */
    @Override
    public void abort(RuntimePlan exe) {
        String storageDir = props.getProperty("pegasus.storage-dir", FileUtils.getTempDirectoryPath()) + File.separator;

        // Abort plan
        if (this.monitoringThread != null && this.monitoringThread.isAlive()) {
            this.monitoringThread.interrupt();
        }

        this.adapter.stopWorkflow(exe, storageDir + exe.getName() + File.separator);
    }

    @Override
    public void execute(RuntimeStep exe, RuntimePlan plan) {
        // Execute Step: Do nothing
    }

    @Override
    public void abort(RuntimeStep exe) {
        // Abort Step: Do nothing
    }

    @Override
    public void setPlanExecutionEngine(PlanExecutionEngine engine) {
        this.planEngine = engine;
    }

    @Override
    public PlanExecutionEngine getPlanExecutionEngine() {
        return this.planEngine;
    }

    @Override
    public int getMaxParallelSteps() {
        return -1;
    }

    @Override
    public void setMaxParallelSteps(int num) {
        return;
    }

    @Override
    public void setStepExecutionEngine(StepExecutionEngine engine) {
        this.stepEngine = engine;
    }

    @Override
    public StepExecutionEngine getStepExecutionEngine() {
        return this.stepEngine;
    }

    @Override
    public void setExecutionLogger(ExecutionLoggerAPI logger) {
        this.logger = logger;
    }

    @Override
    public ExecutionLoggerAPI getExecutionLogger() {
        return this.logger;
    }

    @Override
    public void setExecutionMonitor(ExecutionMonitorAPI monitor) {
        this.monitor = monitor;
        if (this.stepEngine != this) {
            this.stepEngine.setExecutionMonitor(monitor);
        }
    }

    @Override
    public ExecutionMonitorAPI getExecutionMonitor() {
        return this.monitor;
    }

    @Override
    public void setExecutionResource(ExecutionResourceAPI resource) {
        this.resource = resource;
        if (this.stepEngine != this) {
            this.stepEngine.setExecutionResource(resource);
        }
    }

    @Override
    public ExecutionResourceAPI getExecutionResource() {
        return this.resource;
    }

    /**
     * Returns an iterator which can be used to traverse a file one line at a time.
     *
     * @param fileName
     * @param skip     Skip first <i>skip</i> lines
     * @return
     * @throws IOException
     */
    public Iterator<String> lineIterator(String fileName, int skip) throws IOException {
        final BufferedReader r = new BufferedReader(new FileReader(new File(fileName)));

        for (int i = 0; i < skip; ++i) {
            String s = r.readLine();
            if (s == null) {
                break;
            }
        }

        Iterator<String> it = new Iterator<String>() {
            private String line = null;
            private boolean read = true;

            @Override
            public boolean hasNext() {
                try {
                    if (read) {
                        line = r.readLine();
                        read = false;
                    }

                    if (line == null) {
                        close();
                    }

                    return line != null;

                } catch (IOException e) {
                    close();
                    return false;
                }
            }

            @Override
            public String next() {
                if (hasNext()) {
                    read = true;
                    return line;
                } else {
                    return null;
                }
            }

            private void close() {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

        return it;
    }


    class ExecutionMonitoringThread implements Runnable {
        int sleepTime = 5000;
        int jobstateLogMark = 0;
        String submitDir = null;
        RuntimePlan plan = null;
        String pegasusHome = null;
        private Logger log = null;
        Set<String> failedJobs = null;
        ExecutionLoggerAPI logger = null;
        ExecutionMonitorAPI monitor = null;
        PlanExecutionEngine planEngine = null;
        Map<String, RuntimeStep> jobMap = null;
        Map<String, String> jobFileLoc = null;
        RuntimeInfo.Status workflowStatus = null;

        public ExecutionMonitoringThread(PlanExecutionEngine planEngine, RuntimePlan plan, ExecutionLoggerAPI logger,
                                         ExecutionMonitorAPI monitor, String submitDir, String pegasusHome) {
            this.plan = plan;
            this.logger = logger;
            this.monitor = monitor;
            this.submitDir = submitDir;
            this.planEngine = planEngine;
            this.pegasusHome = pegasusHome;
            this.failedJobs = new HashSet<String>();
            this.log = Logger.getLogger(this.getClass());
            this.jobMap = new HashMap<String, RuntimeStep>();
            this.jobFileLoc = new HashMap<String, String>();
        }

        private int getSleepTime() {
            return sleepTime;
        }

        /**
         * Write out the entire jobstate log file to the plan log.
         */
        private void writeJobstateLog() {
            try {
                Iterator<String> it = lineIterator(submitDir + "jobstate.log", 0);

                plan.getRuntimeInfo().addLog("Jobstate Log File ***************");
                while (it.hasNext()) {
                    plan.getRuntimeInfo().addLog(it.next());
                }
                plan.getRuntimeInfo().addLog("*********************************");
            } catch (IOException e) {
                plan.getRuntimeInfo().addLog("Exception while reading jobstate.log " + e.getMessage());
            }
        }

        /**
         * Get most recent job retry count. i.e. If <job-name>.out.002 then return 002.
         */
        private String getRetryCount(String jobLoc, String jobName) {
            int retryCount = 0;

            while (true) {
                if (new File(jobLoc + "/" + jobName + ".out" + String.format(".%03d", retryCount)).exists()) {
                    retryCount++;
                } else {
                    if (retryCount > 0) {
                        retryCount--;
                    }
                    break;
                }
            }

            return String.format(".%03d", retryCount);
        }

        private String showStdOutErr(String jobLoc, String jobName) throws IOException, Exception {
            String ext = getRetryCount(jobLoc, jobName);

            File kickstartFile = new File(jobLoc + "/" + jobName + ".out" + ext);
            log.debug("Reading " + kickstartFile.getCanonicalPath() + " Exists? " + kickstartFile.exists());

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(kickstartFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("statcall");
            StringBuffer out = new StringBuffer();
            StringBuffer err = new StringBuffer();
            StringBuffer sb = new StringBuffer();

            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node n = nodeList.item(temp);

                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;

                    if (e.getAttribute("id").equals("stdout") && e.getElementsByTagName("data").getLength() > 0) {
                        out.append(e.getElementsByTagName("data").item(0).getTextContent());
                    } else if (e.getAttribute("id").equals("stderr") && e.getElementsByTagName("data").getLength() > 0) {
                        err.append(e.getElementsByTagName("data").item(0).getTextContent());
                    }
                }
            }

            // Standard Output
            if (out.length() > 0) {
                sb.append(String.format("%n---Standard Output---%n"));
                sb.append(out.toString().trim());
                sb.append(String.format("%n---Standard Output---%n"));
            }

            // Standard Error
            if (err.length() > 0) {
                sb.append(String.format("%n---Standard Error---%n"));
                sb.append(err.toString().trim());
                sb.append(String.format("%n---Standard Error---%n"));
            }

            return sb.toString();
        }

        /**
         * Traverse the jobstate log and update the status of the workflow, and all the jobs.
         * For jobs that Wings is aware off, update the job's step objects status attribute.
         * For jobs that Wings is NOT aware off, like transfer, or cleanup jobs, update the overall workflow status,
         * if any of them have failed.
         */
        private void updateJobStatus() {
            try {
                String line;
                String[] lineParts;
                Iterator<String> it = lineIterator(submitDir + "jobstate.log", jobstateLogMark);

                log.debug("Skipping " + jobstateLogMark + " line(s) of jobstate.log");

                while (it.hasNext()) {
                    line = it.next();
                    lineParts = line.split("\\s+");

                    String jobName = lineParts[1];
                    String jobState = lineParts[2];
                    String jobLoc = this.jobFileLoc.get(jobName);
                    RuntimeStep step = jobMap.get(jobName);

                    jobstateLogMark += 1;
                    log.debug("Processing Jobstate Log line " + jobstateLogMark);

                    if (jobName.equals("INTERNAL")) {
                        // 1457574965 INTERNAL *** DAGMAN_STARTED/FINISHED 1 ***
                        // 1457574967 INTERNAL *** MONITORD_STARTED/FINISHED 0 ***
                        log.debug("jobName is INTERNAL, skipping...");
                        continue;
                    } else {
                        if (step == null) {
                            log.debug("Job: " + jobName + " not known to Wings");
                            if (jobState.equals("JOB_FAILURE") || jobState.equals("POST_SCRIPT_FAILURE")) {
                                failedJobs.add(jobName);
                                log.debug("Job: " + jobName + " failed (" + jobState + ")");
                            }
                        } else {
                            boolean isFailed = false;
                            RuntimeInfo.Status newStatus = null;
                            RuntimeInfo.Status oldStatus = step.getRuntimeInfo().getStatus();

                            if (jobState.equals("SUBMIT")) {
                                newStatus = RuntimeInfo.Status.QUEUED;
                                step.setRuntimePlan(plan);
                                step.onStart(logger);

                            } else if (jobState.equals("EXECUTE")) {
                                newStatus = RuntimeInfo.Status.RUNNING;

                            } else if (jobState.equals("JOB_SUCCESS") || jobState.equals("POST_SCRIPT_SUCCESS")) {
                                newStatus = RuntimeInfo.Status.SUCCESS;
                                if (oldStatus != newStatus) {
                                    step.onEnd(logger, newStatus, "Success");
                                }
                                if (jobState.equals("POST_SCRIPT_SUCCESS")) {
                                    step.onEnd(logger, newStatus, showStdOutErr(jobLoc, jobName));
                                    step.onEnd(logger, newStatus, "Success");
                                }
                            } else if (jobState.equals("JOB_FAILURE") || jobState.equals("POST_SCRIPT_FAILURE")) {
                                isFailed = true;
                                newStatus = RuntimeInfo.Status.FAILURE;
                                if (oldStatus != newStatus) {
                                    step.onEnd(logger, newStatus, "Failure");
                                }
                                if (jobState.equals("POST_SCRIPT_FAILURE")) {
                                    step.onEnd(logger, newStatus, showStdOutErr(jobLoc, jobName));
                                    step.onEnd(logger, newStatus, "Failure");
                                }
                            } else {
                                newStatus = oldStatus;
                            }

                            if (isFailed) {
                                failedJobs.add(jobName);
                            } else {
                                failedJobs.remove(jobName);
                            }

                            // Job status only contains jobs that Wings is aware of.
                            // Jobs created by Pegasus to manage data transfer, and cleanup are ignored.
                            step.getRuntimeInfo().setStatus(newStatus);
                            log.debug("Job Name: " + jobName + " Old Status: " + oldStatus + " New Status: " + newStatus);
                        }
                    }
                }

                workflowStatus = failedJobs.size() > 0 ? RuntimeInfo.Status.FAILURE : RuntimeInfo.Status.SUCCESS;
                log.debug("Overall Workflow Status: " + workflowStatus);
            } catch (IOException e) {
                plan.getRuntimeInfo().addLog("Exception while reading jobstate.log " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                plan.getRuntimeInfo().addLog("Exception " + e.getMessage());
            }
        }

        private String getJobName(RuntimeStep step) {
            ExecutionStep eStep = step.getStep();
            String componentName = FilenameUtils.getBaseName(eStep.getCodeBinding().getCodeDirectory());
            return componentName + "_" + step.getName();
        }

        /**
         * After Pegasus has planned a workflow, it would have added new jobs for file transfer, cleanup, etc.
         * Wings isn't aware of these jobs yet. This methd will create dummy RuntimeSteps to represent these new jobs.
         */
        private void registerPegasusJobsWithPlan() throws IOException {
            RuntimeStep step = null;
            ExecutionQueue queue = plan.getQueue();

            Iterator<java.io.File> it = FileUtils.iterateFiles(new java.io.File(submitDir), new String[]{"sub"}, true);

            log.debug("Iterating over " + submitDir + " to get all .sub files");

            while (it.hasNext()) {
                final File file = it.next();
                final String sub = FilenameUtils.removeExtension(file.getName());

                if (sub.endsWith(".condor")) {
                    log.debug("Ignoring " + sub + ".sub file");
                    continue;
                }

                if (!jobMap.containsKey(sub)) {
                    log.debug("Adding new runtime step for " + sub);
                    // ID is #sub as Wings expects it to be a URI and does uri.getFragment to get job name.
                    step = new RuntimeStep("#" + sub);

                    step.setRuntimeInfo(new RuntimeInfo());
                    step.getRuntimeInfo().setStatus(null);
                    queue.addStep(step);
                    jobMap.put(sub, step);
                }

                jobFileLoc.put(sub, file.getParentFile().getCanonicalPath());
                log.debug("Job " + sub + " Sub File " + file.getParentFile().getCanonicalPath());
            }
        }

        @Override
        public void run() {
            plan.onStart(logger);

            // Workflow is successful by default
            workflowStatus = RuntimeInfo.Status.SUCCESS;

            try {
                File done = new File(submitDir + "monitord.done");

                // Initialize status of all jobs to UNREADY, i.e. NULL
                for (RuntimeStep step : plan.getQueue().getAllSteps()) {
                    step.getRuntimeInfo().setStatus(null);
                    String jobName = getJobName(step);
                    jobMap.put(jobName, step);
                }

                registerPegasusJobsWithPlan();

                while (true) {
                    updateJobStatus();

                    // Workflow finished? i.e. Presence of monitord.done file
                    if (!done.exists()) {
                        Thread.sleep(getSleepTime());
                    } else {
                        writeJobstateLog();

                        if (workflowStatus == RuntimeInfo.Status.SUCCESS) {
                            plan.onEnd(logger, workflowStatus, "Successful");
                        } else {
                            plan.onEnd(logger, workflowStatus, "Failure");
                        }

                        break;
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                writeJobstateLog();
                plan.onEnd(logger, RuntimeInfo.Status.FAILURE, e.getMessage());
            }
        }
    }
}
