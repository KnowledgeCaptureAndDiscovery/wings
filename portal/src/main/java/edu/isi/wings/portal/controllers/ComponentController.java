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

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentRole;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;
import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.StorageHandler;

import com.google.gson.Gson;

public class ComponentController {
	public String pcdomns;
	public String dcdomns;
	public String liburl;

	public ComponentCreationAPI cc;
	public DataCreationAPI dc;
	public ProvenanceAPI prov;
	
	public boolean isSandboxed;
	public boolean loadConcrete;
	public boolean loadExternal;
	
	public Config config;
	public Properties props;
	public Gson json;

	public ComponentController(Config config, 
	    boolean loadConcrete, boolean loadExternal) {
		this.config = config;
		this.loadConcrete = loadConcrete;
		this.isSandboxed = config.isSandboxed();
		json = JsonHandler.createComponentJson();
		this.props = config.getProperties();

		cc = ComponentFactory.getCreationAPI(props, this.loadConcrete);
		dc = DataFactory.getCreationAPI(props);
		prov = ProvenanceFactory.getAPI(props);
		
		this.loadExternal = loadExternal;
    if(this.loadExternal)
      cc = cc.getExternalCatalog();
		
		this.pcdomns = (String) props.get("ont.domain.component.ns");
		this.dcdomns = (String) props.get("ont.domain.data.url") + "#";
		this.liburl = (String) props.get("lib.concrete.url");
	}

	public String getComponentJSON(String cid) {
		try {
			return json.toJson(cc.getComponent(cid, true));
		}
		finally {
			cc.end();
			dc.end();
			prov.end();
		}
	}
	
	public Response streamComponent(String cid, ServletContext context) {
		try {
			String location = cc.getComponentLocation(cid);
			return StorageHandler.streamFile(location, context);
		}
		finally {
			dc.end();
			prov.end();
		}
	}
	
	/*
	 * Writing Methods
	 */
	public synchronized boolean saveComponentJSON(String cid, String comp_json) {
		if (this.cc == null)
			return false;

		String provlog = "Updating component";
		try {
			Component comp = json.fromJson(comp_json, Component.class);
			Provenance p = new Provenance(comp.getID());
			p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));
			return this.cc.updateComponent(comp) && prov.addProvenance(p)
					&& this.cc.save() && prov.save();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			cc.end();
			dc.end();
			prov.end();
		}
	}

	public synchronized boolean addComponent(String cid, String pid, String ptype) {
		try {
			int type = this.loadConcrete ? Component.CONCRETE : Component.ABSTRACT;
			Component comp = this.cc.getComponent(pid, true);
			String provlog = "New component";
			if (comp == null) {
				// No parent component (probably because of it being a category
				// or top node)
				comp = new Component(cid, type);
			} else {
			  provlog += " from "+comp.getName(); 
				comp.setID(cid);
				comp.setType(type);
			}
			Provenance p = new Provenance(cid);
			p.addActivity(new ProvActivity(ProvActivity.CREATE, provlog));
			return this.cc.addComponent(comp, ptype) && prov.addProvenance(p) 
					&& this.cc.save() && prov.save();
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			cc.end();
			dc.end();
			prov.end();
		}
	}

	public synchronized boolean setComponentLocation(String cid, String location) {
		try {
		  String provlog = "Setting location";
      Provenance p = new Provenance(cid);
      p.addActivity(new ProvActivity(ProvActivity.UPLOAD, provlog));
			return this.cc.setComponentLocation(cid, location) && prov.addProvenance(p) 
					&& this.cc.save() && prov.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			cc.end();
			dc.end();
			prov.end();
		}
	}
	
	public synchronized boolean addCategory(String ctype, String ptype) {
		try {
			return this.cc.addComponentHolder(ctype, ptype)
					&& this.cc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			cc.end();
			dc.end();
			prov.end();
		}
	}

	public synchronized boolean delComponent(String cid) {
		try {
			return this.cc.removeComponent(cid, true, true) 
			    && prov.removeAllProvenance(cid)
					&& this.cc.save() && prov.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			cc.end();
			dc.end();
      prov.end();
		}
	}

	public synchronized boolean delCategory(String ctype) {
		try {
			return  this.cc.removeComponentHolder(ctype)
					&& this.cc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			cc.end();
			dc.end();
			prov.end();
		}
	}
  
	/*
	 * Component browser functions
	 */
  public String listComponentDirectory(String cid, String path) {
    try {
      ArrayList<FileAttrib> files = new ArrayList<FileAttrib>();
      String loc = cc.getComponentLocation(cid);
      if(loc != null) {
        if(path != null)
          loc = loc + "/" + path;
        File floc = new File(loc);
        if(floc.isDirectory()) {
          for(File f : floc.listFiles()) {
            if(!f.equals(floc) && !f.getName().equals(".DS_Store"))
              files.add(new FileAttrib(f.getName(), 
                  (path != null ? path + "/" : "") + f.getName(), 
                  f.isFile()));
          }
        }
      }
      return json.toJson(files);
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    finally {
      cc.end();
      dc.end();
      prov.end();
    }
  }
  
  public Response streamComponentFile(String cid, String path, ServletContext context) {
    try {
      String loc = cc.getComponentLocation(cid);
      if(loc != null) {
        if(path != null) {
          loc = loc + "/" + path;
          File f = new File(loc);
          if(f.isFile() && f.canRead())
            return StorageHandler.streamFile(f.getAbsolutePath(), context);
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      cc.end();
      dc.end();
      prov.end();
    }
    return null;
  }

  public boolean deleteComponentItem(String cid, String path) {
    try {
      String loc = cc.getComponentLocation(cid);
      if(loc != null && path != null) {
          loc = loc + "/" + path;
          File f = new File(loc);
          return FileUtils.deleteQuietly(f);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      cc.end();
      dc.end();
      prov.end();
    }
    return false;
  }

  public boolean saveComponentFile(String cid, String path, String data) {
    try {
      String loc = cc.getComponentLocation(cid);
      if(loc != null && path != null) {
        loc = loc + "/" + path;
        File f = new File(loc);
        if(f.isFile() && f.canWrite()) {
          FileUtils.writeStringToFile(f, data);
          return true;
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      cc.end();
      dc.end();
      prov.end();
    }
    return false;
  }
  
  public boolean renameComponentItem(String cid, String path, String newname) {
    try {
      String loc = cc.getComponentLocation(cid);
      if(loc != null && path != null) {
          loc = loc + "/" + path;
          File f = new File(loc);
          File newf = new File(f.getParent() + newname);
          if(!newf.exists()) {
            if(f.isDirectory())
              FileUtils.moveDirectory(f, newf);
            else if(f.isFile())
              FileUtils.moveFile(f,  newf);
            return true;
          }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      cc.end();
      dc.end();
      prov.end();
    }
    return false;
  }  

  public boolean addComponentFile(String cid, String path) {
    try {
      String loc = cc.getComponentLocation(cid);
      if(loc == null)  {
        loc = cc.getDefaultComponentLocation(cid);
        cc.setComponentLocation(cid, loc);
        cc.save();
      }

      if(loc != null && path != null) {
        loc = loc + "/" + path;
        File f = new File(loc);
        if(!f.getParentFile().exists())
          f.getParentFile().mkdirs();
        if(f.getParentFile().isDirectory() && !f.exists())
          return f.createNewFile();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      cc.end();
      dc.end();
      prov.end();
    }
    return false;
  }

  public boolean addComponentDirectory(String cid, String path) {
    try {
      String loc = cc.getComponentLocation(cid);
      if(loc == null) {
        loc = cc.getDefaultComponentLocation(cid);
        cc.setComponentLocation(cid, loc);
        cc.save();
      }
      
      if(loc != null && path != null) {
        loc = loc + "/" + path;
        File f = new File(loc);
        if(!f.exists())
          return f.mkdirs();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      cc.end();
      dc.end();
      prov.end();
    }
    return false;
  }
  
  public boolean initializeComponentFiles(String cid, String lang) {
    try {
      Component c = cc.getComponent(cid, true);
      String loc = c.getLocation();
      if(loc == null) {
        loc = cc.getDefaultComponentLocation(cid);
        c.setLocation(loc);
        cc.setComponentLocation(cid, loc);
        cc.save();
      }
      
      // Copy io.sh from resources
      ClassLoader classloader = Thread.currentThread().getContextClassLoader();
      FileUtils.copyInputStreamToFile(classloader.getResourceAsStream("io.sh"), 
          new File(loc + "/io.sh"));
      
      int numi = 0, nump = 0, numo = c.getOutputs().size();
      for(ComponentRole r : c.getInputs()) {
        if(r.isParam())
          nump++;
        else
          numi++; 
      }
      
      String suffix = "";
      for(int i=1; i<=numi; i++)
        suffix += " $INPUTS"+i;
      for(int i=1; i<=nump; i++)
        suffix += " $PARAMS"+i;
      for(int i=1; i<=numo; i++)
        suffix += " $OUTPUTS"+i;
      
      String runscript = "";
      String filename = null;
      for(String line : IOUtils.readLines(classloader.getResourceAsStream("run"))) {
        if(line.matches(".*io\\.sh.*")) {
          // Number of inputs and outputs
          line = ". $BASEDIR/io.sh "+numi+" "+nump+" "+numo+" \"$@\"";
        }
        else if(line.matches(".*generic_code.*")) {
          // Code invocation
          if(lang.equals("R")) {
            filename = c.getName() + ".R";
            line = "Rscript --no-save --no-restore $BASEDIR/"+filename;
          }
          else if(lang.equals("PHP")) {
            filename = c.getName() + ".php";
            line = "php $BASEDIR/"+filename;
          }
          else if(lang.equals("Python")) {
            filename = c.getName() + ".py";
            line = "python $BASEDIR/"+filename;
          }
          else if(lang.equals("Perl")) {
            filename = c.getName() + ".pl";
            line = "perl $BASEDIR/"+filename;
          }
          else if(lang.equals("Java")) {
            line = "# Relies on existence of "+c.getName() +".class file in this directory\n";
            line += "java -classpath $BASEDIR "+c.getName() ;
          }
          // Add inputs, outputs as suffixes
          line += suffix;          
        }
        runscript += line + "\n";
      }
      FileUtils.writeStringToFile(new File(loc+"/run"), runscript);
      
      if(filename != null)
        new File(loc+"/"+filename).createNewFile();

    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      cc.end();
      dc.end();
      prov.end();
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
};
