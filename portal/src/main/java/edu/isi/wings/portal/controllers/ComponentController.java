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

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.Component;
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
}
