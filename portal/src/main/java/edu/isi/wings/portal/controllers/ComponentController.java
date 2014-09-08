package edu.isi.wings.portal.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;
import edu.isi.wings.common.kb.PropertiesHelper;
import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.StorageHandler;
import edu.isi.wings.portal.classes.html.CSSLoader;
import edu.isi.wings.portal.classes.html.JSLoader;

@SuppressWarnings("unused")
public class ComponentController {
	private int guid;
	private String pcdomns;
	private String dcdomns;
	private String liburl;
	
	private String uploadScript;
	private String resourceScript;

	private ComponentCreationAPI cc;
	private DataCreationAPI dc;
	private ProvenanceAPI prov;
	
	private boolean isSandboxed;
	private boolean loadConcrete;
	private Config config;
	private Properties props;
	private Gson json;

	public ComponentController(int guid, Config config, boolean loadConcrete) {
		this.guid = guid;
		this.config = config;
		this.loadConcrete = loadConcrete;
		this.isSandboxed = config.isSandboxed();
		json = JsonHandler.createPrettyGson();
		this.props = config.getProperties();

		cc = ComponentFactory.getCreationAPI(props, this.loadConcrete);
		dc = DataFactory.getCreationAPI(props);
		prov = ProvenanceFactory.getAPI(props);
		
		this.pcdomns = (String) props.get("ont.domain.component.ns");
		this.dcdomns = (String) props.get("ont.domain.data.url") + "#";
		this.liburl = (String) props.get("lib.concrete.url");
		
		this.uploadScript = config.getUserDomainUrl() + "/upload";
		this.resourceScript = config.getCommunityPath() + "/resources";
	}

	public void show(PrintWriter out) {
		// Get Hierarchy
		try {
			String tree = json.toJson(cc.getComponentHierarchy(false).getRoot());
			String types = json.toJson(dc.getAllDatatypeIds());
			out.println("<html>");
			out.println("<head>");
			out.println("<title>Manage Component" + (this.loadConcrete ? "s" : " Types") + "</title>");
			JSLoader.loadConfigurationJS(out, config);
			CSSLoader.loadComponentViewer(out, config.getContextRootPath());
			JSLoader.loadComponentViewer(out, config.getContextRootPath());
			out.println("</head>");
	
			out.println("<script>");
			out.println("var compViewer_" + guid + ";");
			out.println("Ext.onReady(function() {"
					+ "compViewer_" + guid + " = new ComponentViewer('"+ guid + "', { " 
						+ "tree: " + tree + ", " 
						+ "types: " + types 
					+ " }, " 
					+ "'" + config.getScriptPath() + "', "
					+ "'" + this.resourceScript + "', "
					+ "'" + this.uploadScript + "', "
					+ "'" + this.pcdomns + "', "
					+ "'" + this.dcdomns + "', " 
					+ "'" + this.liburl + "', " 
					+ loadConcrete + ", " 
					+ !isSandboxed 
					+ ");"
					+ "compViewer_" + guid + ".initialize();\n"
					+ "});\n"
					);
			out.println("</script>");
	
			out.println("</html>");
		}
		finally {
			cc.end();
			dc.end();
			prov.end();
		}
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
	
	public void streamComponent(String cid, HttpServletResponse response, ServletContext context) {
		try {
			String location = cc.getComponentLocation(cid);
			if(location != null) {
				File f = new File(location);
				if(f.canRead()) {
					StorageHandler.streamFile(f.getAbsolutePath(), response, context);
				}
				else {
					try {
						PrintWriter out = response.getWriter();
						out.println("File not on server\nLocation: "+location);
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
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
