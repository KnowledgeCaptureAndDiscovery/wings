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

package edu.isi.wings.portal.controllers;

import com.google.gson.Gson;
import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentHolder;
import edu.isi.wings.catalog.component.classes.ComponentRole;
import edu.isi.wings.catalog.component.classes.ComponentTreeNode;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.StorageHandler;
import edu.isi.wings.portal.classes.config.ConfigLoader;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ComponentController {

  public String pcdomns;
  public String dcdomns;
  public String liburl;

  public ComponentCreationAPI cc;
  public DataCreationAPI dc;
  public TemplateCreationAPI tc;
  public ProvenanceAPI prov;

  public boolean isSandboxed;
  public boolean loadExternal;

  public ConfigLoader config;
  public Properties props;
  public Gson json;

  public ComponentController(ConfigLoader config, boolean loadExternal) {
    this.config = config;
    this.isSandboxed = config.isSandboxed();
    json = JsonHandler.createComponentJson();
    this.props = config.getProperties();

    cc = ComponentFactory.getCreationAPI(props);
    dc = DataFactory.getCreationAPI(props);
    tc = TemplateFactory.getCreationAPI(props);
    prov = ProvenanceFactory.getAPI(props);

    this.loadExternal = loadExternal;
    if (this.loadExternal) cc = cc.getExternalCatalog();

    this.pcdomns = (String) props.get("ont.domain.component.ns");
    this.dcdomns = (String) props.get("ont.domain.data.url") + "#";
    this.liburl = (String) props.get("lib.concrete.url");
  }

  public String getComponentJSON(String cid) {
    return json.toJson(cc.getComponent(cid, true));
  }

  public String getComponentHierarchyJSON() {
    return json.toJson(this.cc.getComponentHierarchy(false).getRoot());
  }

  public String getDatatypeIdsJSON() {
    return json.toJson(this.dc.getAllDatatypeIds());
  }

  public Response streamComponent(String cid, ServletContext context) {
    String location = cc.getComponentLocation(cid);
    return StorageHandler.streamFile(location, context);
  }

  /*
   * Writing Methods
   */
  public synchronized boolean saveComponentJSON(String cid, String comp_json) {
    if (this.cc == null) return false;
    try {
      String provlog = "Updating component";
      Component comp = json.fromJson(comp_json, Component.class);
      Provenance p = new Provenance(comp.getID());
      p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));

      return cc.updateComponent(comp) && prov.addProvenance(p);
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;
  }

  public synchronized boolean incrementComponentVersion(String cid) {
    if (this.cc == null) return false;
    try {
      RunController.invalidateCachedAPIs();
      return cc.incrementComponentVersion(cid);
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;
  }

  public synchronized boolean incrementWorkflowVersionContainingComponent(
    String cid
  ) {
    if (this.tc == null || this.cc == null) return false;
    try {
      /*
      // TODO: Uncomment this, when :
       * - incrementTemplateVersion is implemented
       * - this call is made asynchronous (otherwise it could take time, and will make editing components tedious)
      ArrayList<String> types = this.cc.getParentComponentTypes(cid);
      types.add(0, cid);
      String[] cids = new String[types.size()];
      HashMap<String, ArrayList<String>> compTemplates = tc.getTemplatesContainingComponents(types.toArray(cids));
      HashSet<String> tplids = new HashSet<String>();
      for(ArrayList<String> ctplids : compTemplates.values()) {
        tplids.addAll(ctplids);
      }
      for(String tplid: tplids) {
        tc.incrementTemplateVersion(tplid);
      }*/
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return true;
  }

  public synchronized HashMap<String, ArrayList<String>> getWorkflowsContainingComponent(
    String cid
  ) {
    if (this.tc == null) return null;
    try {
      ArrayList<String> types = this.cc.getParentComponentTypes(cid);
      types.add(0, cid);
      String[] cids = new String[types.size()];
      return tc.getTemplatesContainingComponents(types.toArray(cids));
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return null;
  }

  public synchronized boolean addComponent(
    String cid,
    String pid,
    String ptype,
    boolean isConcrete
  ) {
    try {
      int type = (isConcrete ? Component.CONCRETE : Component.ABSTRACT);
      Component comp = this.cc.getComponent(pid, true);
      String provlog = "New component";
      if (comp == null) {
        // No parent component (probably because of it being a category
        // or top node)
        comp = new Component(cid, type);
      } else {
        provlog += " from " + comp.getName();
        comp.setID(cid);
        comp.setType(type);
      }
      Provenance p = new Provenance(cid);
      p.addActivity(new ProvActivity(ProvActivity.CREATE, provlog));

      return cc.addComponent(comp, ptype) && prov.addProvenance(p);
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;
  }

  public synchronized boolean setComponentLocation(
    String cid,
    String location
  ) {
    try {
      String provlog = "Setting location";
      Provenance p = new Provenance(cid);
      p.addActivity(new ProvActivity(ProvActivity.UPLOAD, provlog));

      return cc.setComponentLocation(cid, location) && prov.addProvenance(p);
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;
  }

  public synchronized boolean addCategory(String ctype, String ptype) {
    try {
      return cc.addComponentHolder(ctype, ptype, false);
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;
  }

  public synchronized boolean delComponent(String cid) {
    try {
      return (
        cc.removeComponent(cid, true, true, true) &&
        prov.removeAllProvenance(cid)
      );
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;
  }

  public synchronized boolean duplicateComponent(
    String cid,
    String pid,
    String ptype,
    String new_cid
  ) {
    //create a temporal component using the source component
    try {
      Component temp_component = cc.getComponent(cid, true);

      this.addComponent(new_cid, pid, ptype, temp_component.isConcrete());

      //edit the id field
      temp_component.setID(new_cid);

      //modify the rules
      if (temp_component.getRules() != null) {
        String cname = KBUtils.getLocalName(cid);
        String new_cname = KBUtils.getLocalName(new_cid);
        ArrayList<String> new_rules = new ArrayList<String>();
        for (String rule : temp_component.getRules()) {
          rule = rule.replaceAll(cname + "Class", new_cname + "Class");
          new_rules.add(rule);
        }
        temp_component.setRules(new_rules);
      }

      //copy the location file
      if (temp_component.isConcrete()) {
        String old_location = temp_component.getLocation();
        String new_location = cc.getDefaultComponentLocation(new_cid);
        if (old_location != null) {
          File old_file = new File(old_location);
          File new_file = new File(new_location);
          if (old_file.exists() && !new_file.exists()) {
            try {
              FileUtils.copyDirectory(old_file, new_file);
              temp_component.setLocation(new_location);
              File runFile = new File(new_location + "/run");
              runFile.setExecutable(true);
            } catch (IOException e) {
              return false;
            }
          }
          temp_component.setLocation(new_location);
        }
      }

      //generate new json component
      String new_component_json = json.toJson(temp_component);
      //add the new component and save it
      return this.saveComponentJSON(new_cid, new_component_json);
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;
  }

  public synchronized boolean renameComponent(
    String cid,
    String pid,
    String ptype,
    String new_cid
  ) {
    try {
      return (
        this.duplicateComponent(cid, pid, ptype, new_cid) &&
        cc.moveChildComponentsTo(cid, new_cid) &&
        cc.removeComponent(cid, true, true, false)
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public synchronized boolean delCategory(String ctype) {
    try {
      return cc.removeComponentHolder(ctype, false);
    } catch (Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;
  }

  public void end() {
    if (cc != null) {
      cc.end();
    }
    if (prov != null) {
      prov.end();
    }
  }

  /*
   * Component browser functions
   */
  public String listComponentDirectory(String cid, String path) {
    ArrayList<FileAttrib> files = new ArrayList<FileAttrib>();
    String loc = cc.getComponentLocation(cid);
    if (loc != null) {
      if (path != null) loc = loc + "/" + path;
      File floc = new File(loc);
      if (floc.isDirectory()) {
        for (File f : floc.listFiles()) {
          if (!f.equals(floc) && !f.getName().equals(".DS_Store")) files.add(
            new FileAttrib(
              f.getName(),
              (path != null ? path + "/" : "") + f.getName(),
              f.isFile()
            )
          );
        }
      }
    }
    return json.toJson(files);
  }

  public Response streamComponentFile(
    String cid,
    String path,
    ServletContext context
  ) {
    String loc = cc.getComponentLocation(cid);
    if (loc != null) {
      if (path != null) {
        loc = loc + "/" + path;
        File f = new File(loc);
        if (f.isFile() && f.canRead()) return StorageHandler.streamFile(
          f.getAbsolutePath(),
          context
        );
      }
    }
    return null;
  }

  public boolean deleteComponentItem(String cid, String path) {
    String loc = cc.getComponentLocation(cid);
    if (loc != null && path != null) {
      loc = loc + "/" + path;
      File f = new File(loc);
      return FileUtils.deleteQuietly(f);
    }
    return false;
  }

  public boolean saveComponentFile(String cid, String path, String data) {
    try {
      String loc = cc.getComponentLocation(cid);
      if (loc != null && path != null) {
        loc = loc + "/" + path;
        File f = new File(loc);
        if (f.isFile() && f.canWrite()) {
          FileUtils.writeStringToFile(f, data);
          return true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean renameComponentItem(String cid, String path, String newname) {
    try {
      String loc = cc.getComponentLocation(cid);

      if (loc != null && path != null) {
        loc = loc + "/" + path;
        File f = new File(loc);
        File newf = new File(f.getParent() + "/" + newname);
        if (!newf.exists()) {
          if (f.isDirectory()) FileUtils.moveDirectory(f, newf); else if (
            f.isFile()
          ) FileUtils.moveFile(f, newf);
          return true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean addComponentFile(String cid, String path) {
    try {
      String loc = cc.getComponentLocation(cid);
      if (loc == null) {
        loc = cc.getDefaultComponentLocation(cid);
        cc.setComponentLocation(cid, loc);
        cc.save();
      }

      if (loc != null && path != null) {
        loc = loc + "/" + path;
        File f = new File(loc);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        if (
          f.getParentFile().isDirectory() && !f.exists()
        ) return f.createNewFile();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean addComponentDirectory(String cid, String path) {
    String loc = cc.getComponentLocation(cid);
    if (loc == null) {
      loc = cc.getDefaultComponentLocation(cid);
      cc.setComponentLocation(cid, loc);
    }

    if (loc != null && path != null) {
      loc = loc + "/" + path;
      File f = new File(loc);
      if (!f.exists()) return f.mkdirs();
    }
    return false;
  }

  public boolean initializeComponentFiles(String cid, String lang) {
    try {
      Component c = cc.getComponent(cid, true);
      String loc = c.getLocation();
      if (loc == null) {
        loc = cc.getDefaultComponentLocation(cid);
        c.setLocation(loc);
        cc.setComponentLocation(cid, loc);
      }

      // Copy io.sh from resources
      ClassLoader classloader = Thread.currentThread().getContextClassLoader();
      FileUtils.copyInputStreamToFile(
        classloader.getResourceAsStream("io.sh"),
        new File(loc + "/io.sh")
      );

      int numi = 0, nump = 0, numo = c.getOutputs().size();
      for (ComponentRole r : c.getInputs()) {
        if (r.isParam()) nump++; else numi++;
      }

      String suffix = "";
      for (int i = 1; i <= numi; i++) suffix += " $INPUTS" + i;
      for (int i = 1; i <= nump; i++) suffix += " $PARAMS" + i;
      for (int i = 1; i <= numo; i++) suffix += " $OUTPUTS" + i;

      String runscript = "";
      String filename = null;
      for (String line : IOUtils.readLines(
        classloader.getResourceAsStream("run")
      )) {
        if (line.matches(".*io\\.sh.*")) {
          // Number of inputs and outputs
          line =
            ". $BASEDIR/io.sh " + numi + " " + nump + " " + numo + " \"$@\"";
        } else if (line.matches(".*generic_code.*")) {
          // Code invocation
          if (lang.equals("R")) {
            filename = c.getName() + ".R";
            line = "Rscript --no-save --no-restore $BASEDIR/" + filename;
          } else if (lang.equals("PHP")) {
            filename = c.getName() + ".php";
            line = "php $BASEDIR/" + filename;
          } else if (lang.equals("Python")) {
            filename = c.getName() + ".py";
            line = "python $BASEDIR/" + filename;
          } else if (lang.equals("Perl")) {
            filename = c.getName() + ".pl";
            line = "perl $BASEDIR/" + filename;
          } else if (lang.equals("Java")) {
            line =
              "# Relies on existence of " +
              c.getName() +
              ".class file in this directory\n";
            line += "java -classpath $BASEDIR " + c.getName();
          }
          // Add inputs, outputs as suffixes
          line += suffix;
        }
        runscript += line + "\n";
      }
      File runFile = new File(loc + "/run");
      FileUtils.writeStringToFile(runFile, runscript);
      runFile.setExecutable(true);

      if (filename != null) new File(loc + "/" + filename).createNewFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  // Increment component version of all components in the library
  // - This is sometimes necessary if the dependent software changes (like the docker image)
  public boolean incrementComponentVersions() {
    for (ComponentTreeNode cnode : this.cc.getComponentHierarchy(false)
      .flatten()) {
      ComponentHolder holder = cnode.getCls();
      if (
        holder != null && holder.getComponent() != null
      ) cc.incrementComponentVersion(holder.getComponent().getID());
    }
    return false;
  }
}

class FileAttrib {

  String text;
  String path;
  boolean leaf;

  public FileAttrib(String text, String path, boolean leaf) {
    this.text = text;
    this.path = path;
    this.leaf = leaf;
  }
}
