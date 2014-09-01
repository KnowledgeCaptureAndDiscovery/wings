package edu.isi.wings.portal.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.StorageHandler;
import edu.isi.wings.portal.classes.domains.Domain;
import edu.isi.wings.portal.classes.domains.DomainInfo;
import edu.isi.wings.portal.classes.html.CSSLoader;
import edu.isi.wings.portal.classes.html.JSLoader;

public class DomainController {
	private int guid;
	private String uploadScript;
	private Config config;
	private Gson json;
	
	private Domain domain;
	private HashMap<String, DomainInfo> user_domains;
	private String defaultDomainName = "blank";
	
	String userdir;
	String userConfigFile;

	public DomainController(int guid, Config config) {
		this.guid = guid;
		this.config = config;
		this.json = JsonHandler.createGson();
		this.uploadScript = config.getUserDomainUrl() + "/upload";
		this.user_domains = new HashMap<String, DomainInfo>();
		this.userConfigFile = config.getUserDir() + "/user.properties";
		
		this.initializeDomainList(config.getDomainId());
	}

	public void show(PrintWriter out) {
		// Get Hierarchy
		try {
			String list = this.getDomainsListJSON();
			//System.out.println(list);
			
			out.println("<html>");
			out.println("<head>");
			out.println("<title>Manage Domains</title>");
			JSLoader.loadConfigurationJS(out, config);
			CSSLoader.loadDomainViewer(out, config.getContextRootPath());
			JSLoader.loadDomainViewer(out, config.getContextRootPath());
			out.println("</head>");
	
			out.println("<script>");
			out.println("var domainViewer_" + guid + ";");
			out.println("Ext.onReady(function() {"
					+ "domainViewer_" + guid + " = new DomainViewer("
						+ "'" + guid + "', " 
						+ list + ", " 
						+ "'" + config.getScriptPath() + "', "
						+ "'" + this.uploadScript + "'" 
						+ ");\n"
						+ "domainViewer_" + guid + ".initialize();\n"
					+ "});");
			out.println("</script>");
			out.println("</html>");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getDomainsListJSON() {
		return "{ list: " + json.toJson(user_domains.values()) 
				 + ", selected: "+ (domain != null ? json.toJson(domain.getDomainName()) : "null") 
				 + ", engines: " + json.toJson(config.getEnginesList()) + "}";
	}

  public String getSimpleDomainsListJSON() {
    return json.toJson(user_domains.keySet());
  }
	
	public String getDomainJSON (String domain) {
		DomainInfo dominfo = this.user_domains.get(domain);
		Domain dom = new Domain(dominfo);
		return json.toJson(dom);
	}
	
	public Domain getUserDomain() {
		return this.domain;
	}
	
	public String importDomain(String domain, String location) {
		File f = null;
		
		// Check if the location is a url
		try {
			// Fetch domain zip file from url
			URL url = new URL(location);
			File furl = new File(url.getPath());
			String zipname = furl.getName();
			InputStream input = url.openStream();
			f = File.createTempFile("domain-", "-temp");
			if(f.delete() && f.mkdirs()) {
				f = new File(f.getAbsolutePath() + File.separator + zipname);
				FileUtils.copyInputStreamToFile(input, f);
			}
		} catch (MalformedURLException e) {
			// Do nothing
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		// If not a url, assume normal file location
		if(f == null)
			f = new File(location);

		
		// If the location is a zip file instead of a directory, 
		// then unzip into a temporary directory
		File ftemp = null;
		if(f.exists() && !f.isDirectory() && f.getName().endsWith(".zip")) {
			try {
				ftemp = File.createTempFile("domain-", "-temp");
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			if(ftemp.delete() && ftemp.mkdirs()) {
				ftemp = new File(ftemp.getAbsolutePath() + File.separator + domain);
			}
			domain = domain.replaceFirst(".zip", "");
			StorageHandler.unzipFile(f, domain, ftemp.getAbsolutePath());
			f = new File(ftemp.getAbsolutePath() + File.separator + domain);
		}
		
		// Finally check that the domain directory exists
		if(!f.exists() || !f.isDirectory())
			return null;
		
		// Import domain directory
		Domain dom = null;
		File oldf = new File(f.getAbsolutePath() + File.separator + "wings.properties");
		File newf = new File(f.getAbsolutePath() + File.separator + "domain.properties");
		if(oldf.exists())
			dom = Domain.importLegacyDomain(domain, this.config, f.getAbsolutePath());
		else if(newf.exists())
			dom = Domain.importDomain(domain, this.config, f.getAbsolutePath());
		else
			return null;
		
		DomainInfo dominfo = new DomainInfo(dom);
		this.user_domains.put(dom.getDomainName(), dominfo);
		this.saveUserConfig(this.userConfigFile);
		
		// Delete temporary directory
		if(ftemp != null)
			try {
				FileUtils.deleteDirectory(ftemp);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		return json.toJson(dominfo);
	}
	
	public String createDomain(String domName) {
		Domain dom = 
				Domain.createDefaultDomain(domName, this.config.getUserDir(), this.config.getExportUserUrl());
		DomainInfo dominfo = new DomainInfo(dom);
		this.user_domains.put(dom.getDomainName(), dominfo);
		this.saveUserConfig(this.userConfigFile);
		return json.toJson(dominfo);
	}
	
	public boolean selectDomain(String domain) {
		DomainInfo dominfo = this.user_domains.get(domain);
		this.domain = new Domain(dominfo);
		if(this.saveUserConfig(this.userConfigFile))
			return true;
		return false;
	}
	
	public boolean deleteDomain(String domain) {
		DomainInfo dominfo = this.user_domains.get(domain);
		if(dominfo != null) {
			this.user_domains.remove(domain);
			Domain dom = new Domain(dominfo);
			if(!Domain.deleteDomain(dom, config, true))
				return false;
		}
		if(this.saveUserConfig(this.userConfigFile))
			return true;
		return false;
	}
	
	public boolean renameDomain(String domain, String newname) {
		DomainInfo dominfo = this.user_domains.get(domain);
		if(dominfo != null) {
			this.user_domains.remove(domain);
			Domain dom = new Domain(dominfo);
			Domain newdom = Domain.renameDomain(dom, newname, config);
			if(newdom == null)
				return false;
			DomainInfo newdominfo = new DomainInfo(newdom);
			this.user_domains.put(newname, newdominfo);
			if(this.domain.getDomainName().equals(domain))
				this.domain = newdom;
		}
		if(this.saveUserConfig(this.userConfigFile))
			return true;
		return false;
	}
	
	public boolean streamDomain(String domName, HttpServletResponse response, ServletContext context) {
		DomainInfo dominfo = this.user_domains.get(domName);
		if(dominfo == null)
			return false;
		
		Domain dom = new Domain(dominfo);
		File f = Domain.exportDomain(dom, this.config);
		StorageHandler.streamFile(f.getAbsolutePath() + File.separator + dom.getDomainName(), 
				response, context);
		try {
			FileUtils.deleteDirectory(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/*
	 * Private functions
	 */
	
	@SuppressWarnings("unchecked")
	private void initializeDomainList(String domname) {
		PropertyListConfiguration config = this.getUserConfiguration();
		List<SubnodeConfiguration> domnodes = config.configurationsAt("user.domains.domain");
		if(domname == null)
		  domname= config.getString("user.domain");
		for (SubnodeConfiguration domnode : domnodes) {
			String domurl = domnode.getString("url");
			Boolean isLegacy = domnode.getBoolean("legacy", false);
			String dname = domnode.getString("name");
			Domain domain = new Domain(dname, domnode.getString("dir"), domurl, isLegacy);
			if(dname.equals(domname)) 
				this.domain = domain;
			DomainInfo dominfo = new DomainInfo(domain);
			this.user_domains.put(dominfo.getName(), dominfo);
		}
	}
	
	private PropertyListConfiguration getUserConfiguration() {
		this.userdir = this.config.getUserDir();
		this.userConfigFile = userdir + "/user.properties";
		// Create userConfigFile if it doesn't exist
		File cfile = new File(userConfigFile);
		if(!cfile.exists()) {
			if(!cfile.getParentFile().exists() && !cfile.getParentFile().mkdirs()) {
				System.err.println("Cannot create config file directory : "+cfile.getParent());
				return null;
			}
			createDefaultUserConfig(userConfigFile);
		}
		// Load properties from configFile
		PropertyListConfiguration config = new PropertyListConfiguration();
		try {
			config.load(userConfigFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return config;
	}
	
	private void createDefaultUserConfig(String configFile) {
		this.domain = Domain.createDefaultDomain(this.defaultDomainName, config.getUserDir(), config.getExportUserUrl());
		DomainInfo dominfo = new DomainInfo(this.domain);
		this.user_domains.put(this.domain.getDomainName(), dominfo);
		this.saveUserConfig(configFile);
	}
	
	private boolean saveUserConfig(String file) {
		PropertyListConfiguration config = new PropertyListConfiguration();
		config.addProperty("user.domain", this.domain.getDomainName());
		for (String domname : this.user_domains.keySet()) {
			DomainInfo dom = this.user_domains.get(domname);
			config.addProperty("user.domains.domain(-1).name", dom.getName());
			config.addProperty("user.domains.domain.dir", dom.getDirectory());
			if(dom.isLegacy())
				config.addProperty("user.domains.domain.legacy", dom.isLegacy());
			else
				config.addProperty("user.domains.domain.url", dom.getUrl());
		}
		try {
			config.save(file);
			return true;
		} catch (ConfigurationException e) {
			e.printStackTrace();
			return false;
		}
	}
}
