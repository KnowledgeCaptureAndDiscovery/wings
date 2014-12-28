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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

//OODT imports
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.filemgr.datatransfer.DataTransfer;
import org.apache.oodt.cas.filemgr.datatransfer.RemoteDataTransferFactory;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.structs.ProductType;
import org.apache.oodt.cas.filemgr.structs.Reference;
import org.apache.oodt.cas.filemgr.structs.exceptions.CatalogException;
import org.apache.oodt.cas.filemgr.structs.exceptions.RepositoryManagerException;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;

/**
 * @author Varun Ratnakar
 * @version $Revsion$
 * 
 *          <p>
 *          A Wings Task (http://www.wings-workflows.org)
 *          </p>
 */
public class WingsTask implements WorkflowTaskInstance {

  /**
   * 
   */
  public WingsTask() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance#run(java.util
   * .Map, org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration)
   */
  public void run(Metadata metadata, WorkflowTaskConfiguration config) {
    Properties props = config.getProperties();
    // Component Info
    String compid = props.getProperty("COMPONENT_ID");
    String tname = props.getProperty("TASKNAME");
    String jobid = props.getProperty("JOBID");
    String argstring = props.getProperty("ARGUMENT");
    ArrayList<String> inputs = fetchFromProps(props, "INPUT");
    ArrayList<String> outputs = fetchFromProps(props, "OUTPUT");

    // Following paths should be Shared across the cluster
    //String script = props.getProperty("SCRIPT_PATH");
    String origjobdir = props.getProperty("JOB_DIR");
    String jobdir = origjobdir;
    //String datadir = props.getProperty("DATA_DIR");

    // File Manager Access
    String fmurl = props.getProperty("FM_URL");
    String fmprefix = props.getProperty("FM_PREFIX");

    // Logging specific info
    String logfile = props.getProperty("LOGFILE");
    String wlogfile = props.getProperty("W_LOGFILE");
    String tplid = wlogfile.replace(".log", "");
    
    PrintStream wlogout = null;
    PrintStream logout = null;

    XmlRpcFileManagerClient fmclient = null;
    try {
      fmclient = new XmlRpcFileManagerClient(new URL(
          fmurl));
      DataTransfer dt = new RemoteDataTransferFactory().createDataTransfer();
      dt.setFileManagerUrl(new URL(fmurl));
      
      // Check if outputs already exist in the file manager
      boolean outputs_already_present = true;
      for (String op : outputs) {
        String prodid = fmprefix + op;
        Product prod = null;
        try {
          prod = fmclient.getProductById(prodid);
        }
        catch (Exception e) {}
        if(prod == null) {
          outputs_already_present = false;
        }
      }
      // If outputs already present, no need to execute
      if(outputs_already_present)
        return;
      

      File tmpdir = File.createTempFile("oodt-run-", "");
      if (tmpdir.delete() && tmpdir.mkdirs())
        jobdir = tmpdir.getAbsolutePath() + File.separator;

      argstring = argstring.replace(origjobdir, jobdir);

      wlogout = new PrintStream(new FileOutputStream(jobdir + wlogfile, true));
      logout = new PrintStream(jobdir + logfile);

      wlogout.println(jobid + " (" + tname + "): RUNNING");
      wlogout.close();
      this.uploadProduct(wlogfile, wlogfile, "GenericFile", 
          new File(jobdir + wlogfile), new Metadata(), fmclient);

      wlogout = new PrintStream(new FileOutputStream(jobdir + wlogfile, true));
      logout.println("[INFO]: Component Initializing");
      logout.println(tname + " " + argstring);

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

      // Fetch component from file manager
      File compdir = new File(jobdir + File.separator + "comp");
      compdir.mkdir();
      Product cprod = fmclient.getProductById(compid);
      cprod.setProductReferences(fmclient.getProductReferences(cprod));
      dt.retrieveProduct(cprod, compdir);
      String scriptPath = null;
      for(File czip : compdir.listFiles()) {
        if(czip.getName().endsWith(".zip")) {
          this.unZipIt(czip.getAbsolutePath(), compdir.getAbsolutePath());
          File tmpf = new File(compdir.getAbsolutePath() + File.separator + "run");
          if(!tmpf.exists())
            tmpf = new File(compdir.getAbsolutePath() + File.separator + "run.bat");
          scriptPath = tmpf.getAbsolutePath();
        }
        else
          scriptPath = czip.getAbsolutePath();
      }
      File scriptf = new File(scriptPath);
      scriptf.setExecutable(true);

      // Create command execution
      ArrayList<String> command = new ArrayList<String>();
      command.add(scriptf.getAbsolutePath());
      for (String s : argstring.split(" ")) {
        command.add(s);
      }

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
        throw new Exception(
            "[ERROR] Component failed with a non-zero exit code");

      // Ingest output files to file manager
      for (String op : outputs) {
        File f = new File(jobdir + op);
        
        File metf = new File(jobdir + op + ".met");
        HashMap<String, String> cmeta = new HashMap<String, String>();
        if(metf.exists()) {
          for(Object ln : FileUtils.readLines(metf)) {
            String metline = (String) ln;
            String[] kv = metline.split("\\s*=\\s*");
            if(kv.length == 2)
              cmeta.put(kv[0], kv[1]);
          }
        }
        if (!f.exists())
          throw new Exception("[ERROR] Missing Output " + op);
        if (f.exists()) {
          logout.println("[INFO] Putting Output into File Manager: " + op);

          // Get Output Metadata & Product Type
          String typeid = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
          Metadata meta = metadata.getSubMetadata(op);
          String prodtypeid = meta.getMetadata(typeid);
          meta.removeMetadata(typeid);
          
          // Override metadata with custom metadata (if any)
          for(String key : meta.getAllKeys()) {
            String[] nsname = key.split("#");
            if(nsname.length == 2) {
              if(cmeta.containsKey(nsname[1])) {
                meta.removeMetadata(key);
                meta.addMetadata(key, cmeta.get(nsname[1]));
              }
            }
          }

          // Upload output to file manager
          String prodid =  fmprefix + op;
          this.uploadProduct(prodid, op, prodtypeid, f, meta, fmclient);
        }
        
        if (metf.exists()) {
          String metname = op + ".met";
          String prodid = fmprefix + metname;
          this.uploadProduct(prodid, metname, "GenericFile", 
              metf, new Metadata(), fmclient);
        }
      }
      logout.println("SUCCESS: Component finished successfully !");
      logout.close();
      wlogout.println(jobid + " (" + tname + "): SUCCESS");
      wlogout.close();
    } catch (Exception e) {
      if (logout != null) {
        logout.println(e.getMessage());
        logout.println("FAILURE: Component Failed");
        logout.close();
        wlogout.println(jobid + " (" + tname + "): FAILURE");
        wlogout.close();
      }
    }
    try {
      if(fmclient != null) {
        this.uploadProduct(wlogfile, wlogfile, "GenericFile", 
            new File(jobdir + wlogfile), new Metadata(), fmclient);
        String logid = tplid + "-" + logfile;
        this.uploadProduct(logid, logid, "GenericFile", 
            new File(jobdir + logfile), new Metadata(), fmclient);        
      }
    } catch (CatalogException e) {
      e.printStackTrace();
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
  }

  private void uploadProduct(String prodid, String prodname, String prodtypeid, 
      File f, Metadata meta, XmlRpcFileManagerClient fmclient) 
          throws CatalogException, RepositoryManagerException {
    ArrayList<Reference> refs = new ArrayList<Reference>();
    long filesize = f.length();
    refs.add(new Reference(f.toURI().toString(), "", filesize));

    Product prod = null;
    try { prod = fmclient.getProductById(prodid); }
    catch (Exception e) {}
    if(prod != null)
      fmclient.removeProduct(prod);

    ProductType type = null;
    try {
      type = fmclient.getProductTypeById(prodtypeid);
    }
    catch (Exception e) {
      String desc = "";
      String ver = "org.apache.oodt.cas.filemgr.versioning.BasicVersioner";
      String repo = "file:///tmp";
      type = new ProductType(prodtypeid, prodtypeid, desc, repo, ver);
      fmclient.addProductType(type);
    }
    
    prod = new Product(prodname, type, 
        Product.STRUCTURE_FLAT, Product.STATUS_TRANSFER, refs);
    prod.setProductId(prodid);

    // Ingest new product
    try {
      fmclient.ingestProduct(prod, meta, false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void unZipIt(String zipFile, String outputFolder) {
    byte[] buffer = new byte[1024];
    try{
      File folder = new File(outputFolder);
      if(!folder.exists()){
        folder.mkdir();
      }
      ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
      ZipEntry ze = zis.getNextEntry();

      while(ze!=null) {
        String fileName = ze.getName();
        File newFile = new File(outputFolder + File.separator + fileName);
        new File(newFile.getParent()).mkdirs();
        FileOutputStream fos = new FileOutputStream(newFile);             
        int len;
        while ((len = zis.read(buffer)) > 0) {
          fos.write(buffer, 0, len);
        }
        fos.close();   
        ze = zis.getNextEntry();
      }
      zis.closeEntry();
      zis.close();
    } catch(IOException ex){
      ex.printStackTrace(); 
    }
  }    

  private ArrayList<String> fetchFromProps(Properties props, String argtype) {
    ArrayList<String> args = new ArrayList<String>();
    int i = 1;
    while (props.containsKey(argtype + i)) {
      args.add(props.getProperty(argtype + i));
      i++;
    }
    return args;
  }
}
