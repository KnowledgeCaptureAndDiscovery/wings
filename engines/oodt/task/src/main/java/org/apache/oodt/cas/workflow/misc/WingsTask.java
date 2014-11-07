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

package org.apache.oodt.cas.workflow.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
//OODT imports
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.filemgr.datatransfer.DataTransfer;
import org.apache.oodt.cas.filemgr.datatransfer.RemoteDataTransferFactory;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;

/**
 * @author Varun Ratnakar
 * @version $Revsion$
 * 
 * <p>A Wings Task (http://www.wings-workflows.org)</p>
 */
public class WingsTask implements WorkflowTaskInstance {

	/**
	 * 
	 */
	public WingsTask() {
	}

	/* (non-Javadoc)
	 * @see org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance#run(java.util.Map, 
	 * org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration)
	 */
	public void run(Metadata metadata, WorkflowTaskConfiguration config) {
		Properties props = config.getProperties();
		// Component Info
		String tname = props.getProperty("TASKNAME");
		String jobid = props.getProperty("JOBID");
		String argstring = props.getProperty("ARGUMENT");
		ArrayList<String> inputs = fetchFromProps(props, "INPUT");
		ArrayList<String> outputs = fetchFromProps(props, "OUTPUT");
		
		// Following paths should be Shared across the cluster
		String script = props.getProperty("SCRIPT_PATH");
		String jobdir = props.getProperty("JOB_DIR");
		String datadir = props.getProperty("DATA_DIR");
		
		// File Manager Access
		String fmurl = props.getProperty("FM_URL");
		String fmprefix = props.getProperty("FM_PREFIX");
		
		// Logging specific info
		String logfile = props.getProperty("LOGFILE");
		String wlogfile = props.getProperty("W_LOGFILE");
		
		PrintStream wlogout = null;
		PrintStream logout = null;
		
		try {
			XmlRpcFileManagerClient fmclient = new XmlRpcFileManagerClient(new URL(fmurl));
			DataTransfer dt = new RemoteDataTransferFactory().createDataTransfer();
			dt.setFileManagerUrl(new URL(fmurl));
			
			wlogout = new PrintStream(new FileOutputStream(jobdir+wlogfile, true));
			logout = new PrintStream(jobdir+logfile);
			
			wlogout.println(jobid+" ("+tname+"): RUNNING");
			wlogout.flush();
			
			logout.println("[INFO]: Component Initializing");
			logout.println(tname+" "+argstring);

			// Fetch input files from file manager if not already present in directory
			for (String ip : inputs) {
				File f = new File(jobdir + ip);
				if (!f.exists()) {
					logout.println("[INFO] Fetching Input from File Manager: " + ip);
					Product prod = fmclient.getProductById(fmprefix + ip);
					prod.setProductReferences(fmclient.getProductReferences(prod));
					dt.retrieveProduct(prod, new File(jobdir));
				}
			}
			logout.flush();

		    ArrayList<String> command = new ArrayList<String>();
		    command.add(script);
		    for(String s : argstring.split(" "))
		    	command.add(s);
		    
		    ProcessBuilder builder = new ProcessBuilder(command);
			builder.directory(new File(jobdir));
			builder.redirectErrorStream(true);
			
		    final Process process = builder.start();
		    
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				logout.println(line);
			}
			process.waitFor();
			int exitStatus = process.exitValue();
			if (exitStatus != 0) 
				throw new Exception("[ERROR] Component failed with a non-zero exit code");

			for (String op : outputs) {
				File f = new File(jobdir + op);
				if(!f.exists())
					throw new Exception("[ERROR] Missing Output "+op);
				
				// Copy output to wings data location
				FileUtils.copyFileToDirectory(f, new File(datadir));

				// TODO: Ingest output files to file manager
			}
		    logout.println("SUCCESS: Component finished successfully !");
		    logout.close();
		    wlogout.println(jobid+" ("+tname+"): SUCCESS");
		    wlogout.close();
		}
		catch (Exception e) {
			if(logout != null) {
				logout.println(e.getMessage());
				logout.println("FAILURE: Component Failed");
				logout.close();
				wlogout.println(jobid+" ("+tname+"): FAILURE");
				wlogout.close();
			}
		}
	}
	
	private ArrayList<String> fetchFromProps(Properties props, String argtype) {
		ArrayList<String> args = new ArrayList<String>();
		int i=1;
		while(props.containsKey(argtype+i)) {
			args.add(props.getProperty(argtype+i));
			i++;
		}
		return args;
	}
}
