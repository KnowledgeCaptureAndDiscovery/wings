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

package edu.isi.wings.portal.classes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.lang.ArrayUtils;
import edu.isi.wings.execution.engine.ExecutionFactory;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.api.impl.distributed.DistributedExecutionEngine;
import edu.isi.wings.execution.engine.api.impl.local.LocalExecutionEngine;
import edu.isi.wings.execution.tools.ExecutionToolsFactory;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import edu.isi.wings.portal.classes.domains.Domain;
import edu.isi.wings.portal.classes.domains.Permission;
import edu.isi.wings.portal.classes.users.User;
import edu.isi.wings.portal.classes.users.UsersDB;
import edu.isi.wings.portal.controllers.DomainController;

public class Config {
	// The Portal configuration properties file. Order of checking:
	// 1. Check "config.file" servlet context parameter
	// 2. Check ${user.home}/.wings/portal.properties if ${user.home} is present
	// 3. Check /etc/wings/portal.properties
	private String configFile;

	// The following are loaded from the config file
	private String storageDirectory;
	private String tdbDirectory;
	private String dotFile;
	private String serverUrl;
	private String workflowOntologyUrl;
	private String dataOntologyUrl;
	private String componentOntologyUrl;
	private String executionOntologyUrl;
	private String resourceOntologyUrl;
	
	private String ontdirurl = "http://www.wings-workflows.org/ontology";

	private HashMap<String, ExeEngine> engines;

	// Some hardcoded values (TODO: override from config file)
	private String usersRelativeDir = "users";
	private String exportServletPath = "/export";
	private boolean isSandboxed = false;

	// The following are set from the "request" variable
	private String viewerId;
	private String userId;
	private String sessionId;
	private String contextRootPath;
	private String scriptPath;
	private String[] scriptArguments;

	private String communityRelativeDir = "common";
	private String communityPath;
	private String communityDir;
	private String exportCommunityUrl;
	
	// This following are user/domain specific properties
	private String userPath;
	private String userDir;
	private ArrayList<String> usersList;
	private boolean isAdminViewer;
	
  private Domain domain;
	private String domainId;
	private ArrayList<String> domainsList;
  private String userDomainUrl;
  private String exportUserUrl;

  private UsersDB userapi;
  
	public String getUserDomainUrl() {
    return userDomainUrl;
  }

  public void setUserDomainUrl(String userDomainUrl) {
    this.userDomainUrl = userDomainUrl;
  }

  public Config(HttpServletRequest request) {
    // Initialize UserDatabase
    this.initializeUserDatabase();
    
		// Initialize portal config
		this.initializePortalConfig(request);

		// Initialize user config
		this.initializeUserConfig(request);
	}
	
  public void getPermissions() {
    // Check domain, user & viewerid
    // Return Permissions (canRead=true/false, canWrite=true/false, canExecute=true/false)
  }
  
  public boolean checkUser(HttpServletResponse response) {
    /* Check that such a user exists */
    try {
      if(!this.userapi.hasUser(this.userId)) { 
        if(response != null) {
          // If userId is not present in server
          response.setContentType("text/html");
          response.getWriter().println("No such user: "+this.userId+" !");
        }
        return false;
      }
      // Get viewer roles
      if(this.viewerId == null)
        return false;
      User viewer = this.userapi.getUser(this.viewerId);
      this.isAdminViewer = viewer.isAdmin();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  public void showError(HttpServletRequest request, 
      HttpServletResponse response, String message) {
    try {
      response.setContentType("text/html");
      request.setAttribute("message", message);
      request.setAttribute("nohome", true);
      request.getRequestDispatcher("index.jsp").include(request, response);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean checkDomain(HttpServletRequest request, 
      HttpServletResponse response) {
    return this.checkDomain(request, response, true);
  }
  
	public boolean checkDomain(HttpServletRequest request, 
	    HttpServletResponse response, boolean show_error) {
    if(!this.checkUser(response))
      return false;

    try {
      // For a non-owner viewer, if there is no domain available, 
      // then return a message
      if(this.domain == null && !this.viewerId.equals(this.userId)) {
        if(show_error)
          this.showError(request, response, "No Domains shared by "+userId+" !");
        return false;
      }
      
      // Check that a domain is provided in the URL, or a default domain exists      
      String redirectUrl = this.getUserPath()+"/domains";
      if(this.domain == null && !this.scriptPath.equals(redirectUrl)) {
        response.setContentType("text/html");
        response.setHeader("Refresh", "5; URL="+redirectUrl);
        response.getWriter().println("No such domain !<br/>"
            + "See list of domains at <a href='"+redirectUrl+"'>"+redirectUrl+"</a>. "
            + "Redirecting in 5 seconds");
        return false;
      }
      else if(this.domain != null 
          && !this.scriptPath.equals(redirectUrl)
          && !this.viewerId.equals(this.userId)) {
        // Check domain permissions
        // TODO: Check read, write & execute permission based on input
        //       For now: all or none permissions
        Permission perm = this.domain.getPermissionForUser(this.viewerId);
        if(!perm.canRead()) {
          if(show_error)
            this.showError(request, response, "No Permission !");
          return false;
        }
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
		return true;
	}

	private void initializeUserConfig(HttpServletRequest request) {
	  // Set userid, domainid, viewerId
	  this.userId = request.getParameter("userid");
	  this.domainId = request.getParameter("domainid");
    this.viewerId = request.getRemoteUser();
    
    // Set default script values
    this.scriptPath = this.contextRootPath + request.getServletPath();
    this.scriptArguments = new String[]{};
    
	  String path = request.getPathInfo();
    if (path == null) path = "/";
	  this.scriptArguments = path.split("/");
	  if(this.scriptArguments.length > 0)
	    this.scriptArguments = (String[]) ArrayUtils.remove(this.scriptArguments, 0);

	  if(this.domainId != null) {
	    this.userDomainUrl = this.contextRootPath + "/" + this.getUsersRelativeDir() 
	        + "/" + this.getUserId() + "/" + this.getDomainId();
	    this.scriptPath = this.userDomainUrl + request.getServletPath();
	  }
	  else if (this.userId != null) {
	      this.scriptPath = this.contextRootPath + "/" + this.getUsersRelativeDir()
	          + "/" + this.getUserId() + request.getServletPath();
	  }
		
		this.sessionId = request.getSession().getId();
    
		if(this.viewerId == null)
		  return;
		
    // If no userId specified, then set the viewer as the user
    if(this.userId == null)
      this.userId = this.viewerId;

    if(!this.checkUser(null))
      return;
    
		this.exportUserUrl = serverUrl + contextRootPath + exportServletPath + "/" + usersRelativeDir
				+ "/" + userId;
    this.userPath = contextRootPath + "/" + usersRelativeDir + "/" + userId;
		this.userDir = storageDirectory + File.separator + usersRelativeDir + File.separator + userId;
    
		// Create userDir (if it doesn't exist)
		File uf = new File(this.userDir);
		if (!uf.exists() && !uf.mkdirs())
			System.err.println("Cannot create user directory : " + uf.getAbsolutePath());
		
		// Get domain and user list
		DomainController dc = new DomainController(1, this);
		this.domainsList = dc.getReadableDomainsList();
		this.usersList = this.userapi.getUsersList();
		
    // Get user's selected domain
    this.domain = dc.getUserDomain();

    // If the domain isn't a part of the readable domain list, 
    // then choose the first one
    if(this.domain == null ||
        !domainsList.contains(this.domain.getDomainName())) {
      if(domainsList.size() > 0)
          this.domain = dc.getDomain(domainsList.get(0));
      else
        this.domain = null;
    }

		if(this.domain != null) {
		  this.userDomainUrl = this.contextRootPath + "/" + this.getUsersRelativeDir() 
        + "/" + this.getUserId() + "/" + this.domain.getDomainName();
		  this.domainId = this.domain.getDomainName();
		}
	}

	public String getViewerId() {
    return viewerId;
  }

  public void setViewerId(String viewerId) {
    this.viewerId = viewerId;
  }

  public String[] getScriptArguments() {
    return scriptArguments;
  }

  public void setScriptArguments(String[] scriptArguments) {
    this.scriptArguments = scriptArguments;
  }

  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  public ArrayList<String> getDomainsList() {
    return domainsList;
  }
  
  public ArrayList<String> getUsersList() {
    return usersList;
  }

  private void initializePortalConfig(HttpServletRequest request) {
		this.contextRootPath = request.getContextPath();
		
		PropertyListConfiguration serverConfig = getPortalConfiguration(request);
		this.storageDirectory = serverConfig.getString("storage.local");
		this.tdbDirectory = serverConfig.getString("storage.tdb");
		this.serverUrl = serverConfig.getString("server");
		this.dotFile = serverConfig.getString("graphviz");
		this.dataOntologyUrl = serverConfig.getString("ontology.data");
		this.componentOntologyUrl = serverConfig.getString("ontology.component");
		this.workflowOntologyUrl = serverConfig.getString("ontology.workflow");
		this.executionOntologyUrl = serverConfig.getString("ontology.execution");
		this.resourceOntologyUrl = serverConfig.getString("ontology.resource");
		
    this.exportCommunityUrl = serverUrl + contextRootPath + exportServletPath + "/" 
        + communityRelativeDir;
    this.communityPath = contextRootPath + "/" + communityRelativeDir;
    this.communityDir = storageDirectory + File.separator 
        + communityRelativeDir;
    // Create communityDir (if it doesn't exist)
    File uf = new File(this.communityDir);
    if (!uf.exists() && !uf.mkdirs())
      System.err.println("Cannot create community directory : " + uf.getAbsolutePath());
    
    this.engines = new HashMap<String, ExeEngine>();
		@SuppressWarnings("unchecked")
		List<SubnodeConfiguration> enginenodes = serverConfig.configurationsAt("execution.engine");
		for (SubnodeConfiguration enode : enginenodes) {
			ExeEngine engine = this.getExeEngine(enode); 
			this.engines.put(engine.getName(), engine);
		}
		// Add in the distributed engine if it doesn't already exist
		if(!this.engines.containsKey("Distributed")) {
		  ExeEngine distengine = new ExeEngine("Distributed", 
          DistributedExecutionEngine.class.getCanonicalName(),ExeEngine.Type.BOTH);
		  this.engines.put(distengine.getName(), distengine);
	    this.addEngineConfig(serverConfig, distengine);
	    try {
	      serverConfig.save(this.configFile);
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
		}
	}
  
  private void initializeUserDatabase() {
    this.userapi = new UsersDB();
  }

	@SuppressWarnings("rawtypes")
	private ExeEngine getExeEngine(SubnodeConfiguration node) {
		String name = node.getString("name");
		String impl = node.getString("implementation");
		ExeEngine.Type type = ExeEngine.Type.valueOf(node.getString("type"));
		ExeEngine engine = new ExeEngine(name, impl, type);
		for (Iterator it = node.getKeys("properties"); it.hasNext();) {
			String key = (String) it.next();
			String value = node.getString(key);
			engine.addProperty(key.replace("properties.", ""), value);
		}
		return engine;
	}
	
	private PropertyListConfiguration getPortalConfiguration(HttpServletRequest request) {
		ServletContext app = request.getSession().getServletContext();
		this.configFile = app.getInitParameter("config.file");
		if (this.configFile == null) {
			String home = System.getProperty("user.home");
			if (home != null && !home.equals(""))
				this.configFile = home + File.separator + ".wings" 
						+ File.separator + "portal.properties";
			else
				this.configFile = "/etc/wings/portal.properties";
		}
		// Create configFile if it doesn't exist (portal.properties)
		File cfile = new File(this.configFile);
		if (!cfile.exists()) {
			if (!cfile.getParentFile().mkdirs()) {
				System.err.println("Cannot create config file directory : " + cfile.getParent());
				return null;
			}
			createDefaultPortalConfig(request);
		}

		// Load properties from configFile
		PropertyListConfiguration props = new PropertyListConfiguration();
		try {
			props.load(this.configFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return props;
	}

	private void createDefaultPortalConfig(HttpServletRequest request) {
		String server = request.getScheme() + "://" + request.getServerName() + ":"
				+ request.getServerPort();
		String storageDir = null;
		String home = System.getProperty("user.home");
		if (home != null && !home.equals(""))
			storageDir = home + File.separator + ".wings" + File.separator + "storage";
		else
			storageDir = System.getProperty("java.io.tmpdir") + 
				File.separator + "wings" + File.separator + "storage";
		if (!new File(storageDir).mkdirs())
			System.err.println("Cannot create storage directory: " + storageDir);

		PropertyListConfiguration config = new PropertyListConfiguration();
		config.addProperty("storage.local", storageDir);
		config.addProperty("storage.tdb", storageDir + File.separator + "TDB");
		config.addProperty("server", server);
		
		File loc1 = new File("/usr/bin/dot");
		File loc2 = new File("/usr/local/bin/dot");
		config.addProperty("graphviz", loc2.exists() ? loc2.getAbsolutePath() : loc1.getAbsolutePath());
		config.addProperty("ontology.data", ontdirurl + "/data.owl");
		config.addProperty("ontology.component", ontdirurl + "/component.owl");
		config.addProperty("ontology.workflow", ontdirurl + "/workflow.owl");
		config.addProperty("ontology.execution", ontdirurl + "/execution.owl");
		config.addProperty("ontology.resource", ontdirurl + "/resource.owl");

		this.addEngineConfig(config, new ExeEngine("Local", 
				LocalExecutionEngine.class.getCanonicalName(),ExeEngine.Type.BOTH));
    this.addEngineConfig(config, new ExeEngine("Distributed", 
        DistributedExecutionEngine.class.getCanonicalName(),ExeEngine.Type.BOTH));
		
		/*this.addEngineConfig(config, new ExeEngine("OODT",
				OODTExecutionEngine.class.getCanonicalName(), ExeEngine.Type.PLAN));

		this.addEngineConfig(config, new ExeEngine("Pegasus", 
				PegasusExecutionEngine.class.getCanonicalName(), ExeEngine.Type.PLAN));*/
		
		try {
			config.save(this.configFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addEngineConfig(PropertyListConfiguration config, ExeEngine engine) {
		config.addProperty("execution.engine(-1).name", engine.getName());
		config.addProperty("execution.engine.implementation", engine.getImplementation());
		config.addProperty("execution.engine.type", engine.getType());
		for (Entry<Object, Object> entry : engine.getProperties().entrySet())
			config.addProperty("execution.engine.properties." + entry.getKey(), entry.getValue());
	}
	
	public Properties getProperties() {
		return this.getProperties(this.domain);
	}
	
	// Return Properties that are currently used by catalogs & planners
	public Properties getProperties(Domain domain) {
	  Properties props = new Properties();
		if (domain != null) {
			props = domain.getProperties();
			if(domain.isLegacy())
				return props;
			
			props.setProperty("ont.dir.url", this.ontdirurl);
			if(!domain.getUseSharedTripleStore())
				props.setProperty("ont.dir.map", 
						"file:"+ domain.getDomainDirectory() + File.separator + "ontology");
			
			props.setProperty("ont.data.url", this.getDataOntologyUrl());
			props.setProperty("ont.component.url", this.getComponentOntologyUrl());
			props.setProperty("ont.workflow.url", this.getWorkflowOntologyUrl());
			props.setProperty("ont.execution.url", this.getExecutionOntologyUrl());
			if (domain.getUseSharedTripleStore())
				props.setProperty("tdb.repository.dir", this.getTripleStoreDir());
      
			ExeEngine pengine = engines.get(domain.getPlanEngine());
			ExeEngine sengine = engines.get(domain.getStepEngine());
			props.putAll(pengine.getProperties());
			props.putAll(sengine.getProperties());
		}
		else {
      props.setProperty("tdb.repository.dir", this.getTripleStoreDir());
		}
		
    if(this.getResourceOntologyUrl() == null)
      this.setResourceOntologyUrl(ontdirurl + "/resource.owl");
    props.setProperty("ont.resource.url", this.getResourceOntologyUrl());
    props.setProperty("lib.resource.url", 
        this.getExportCommunityUrl()+"/resource/library.owl");
    props.setProperty("lib.provenance.url", 
        this.getExportCommunityUrl()+"/provenance/library.owl");
    
    props.setProperty("viewer.id", this.viewerId);
    props.setProperty("user.id", this.userId);
    
    return props;
	}

	public PlanExecutionEngine getDomainExecutionEngine() {
		ExeEngine pengine = engines.get(domain.getPlanEngine());
		ExeEngine sengine = engines.get(domain.getStepEngine());
		try {
			pengine.getProperties().putAll(this.getProperties());
			sengine.getProperties().putAll(this.getProperties());
			// TODO: Check if the selected engines are compatible
			// and can be used as plan and step engines respectively
			PlanExecutionEngine pee = ExecutionFactory.createPlanExecutionEngine(
					pengine.getImplementation(), pengine.getProperties());
			StepExecutionEngine see = ExecutionFactory.createStepExecutionEngine(
					sengine.getImplementation(), sengine.getProperties());
			ExecutionLoggerAPI logger = ExecutionToolsFactory.createLogger(this.getProperties());
			ExecutionMonitorAPI monitor = ExecutionToolsFactory.createMonitor(this.getProperties());
			ExecutionResourceAPI resource = ExecutionToolsFactory.getResourceAPI(this.getProperties());
			resource.setLocalStorageFolder(this.getStorageDirectory());
			pee.setStepExecutionEngine(see);
			pee.setExecutionLogger(logger);
			pee.setExecutionMonitor(monitor);
			pee.setExecutionResource(resource);
			return pee;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public StepExecutionEngine getDomainStepEngine() {
		return null;
	}
	
	public ExecutionMonitorAPI getDomainExecutionMonitor() {
		return ExecutionToolsFactory.createMonitor(this.getProperties());
	}

	// Getters and Setters
	public String getWorkflowOntologyUrl() {
		return this.workflowOntologyUrl;
	}

	public String getComponentOntologyUrl() {
		return this.componentOntologyUrl;
	}

	public String getDataOntologyUrl() {
		return this.dataOntologyUrl;
	}

	public void setWorkflowOntologyUrl(String workflowOntologyUrl) {
		this.workflowOntologyUrl = workflowOntologyUrl;
	}

	public void setDataOntologyUrl(String dataOntologyUrl) {
		this.dataOntologyUrl = dataOntologyUrl;
	}

	public void setComponentOntologyUrl(String componentOntologyUrl) {
		this.componentOntologyUrl = componentOntologyUrl;
	}

	public String getExecutionOntologyUrl() {
		return executionOntologyUrl;
	}

	public void setExecutionOntologyUrl(String executionOntologyUrl) {
		this.executionOntologyUrl = executionOntologyUrl;
	}

	public String getResourceOntologyUrl() {
    return resourceOntologyUrl;
  }

  public void setResourceOntologyUrl(String resourceOntologyUrl) {
    this.resourceOntologyUrl = resourceOntologyUrl;
  }

  public String getConfigFile() {
		return configFile;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	public String getUsersRelativeDir() {
		return usersRelativeDir;
	}

	public void setUsersRelativeDir(String usersRelativeDir) {
		this.usersRelativeDir = usersRelativeDir;
	}

	public String getExportServletPath() {
		return exportServletPath;
	}

	public void setExportServletPath(String exportServletPath) {
		this.exportServletPath = exportServletPath;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
	  if(userDomainUrl != null)
	    this.userDomainUrl = this.userDomainUrl.replace(this.userId, userId);
	  if(scriptPath != null)
	    this.scriptPath = this.scriptPath.replace(this.userId, userId);
	  if(exportUserUrl != null)
	    this.exportUserUrl = this.exportUserUrl.replace(this.userId, userId);
	  if(userPath != null)
	    this.userPath = this.userPath.replace(this.userId, userId);
	  if(userDir != null)
	    this.userDir = this.userDir.replace(this.userId, userId);
		this.userId = userId;
	}

	public String getUserDir() {
		return userDir;
	}

	public void setUserDir(String userDir) {
		this.userDir = userDir;
	}

	public boolean isAdminViewer() {
    return isAdminViewer;
  }

  public void setAdminViewer(boolean isAdminViewer) {
    this.isAdminViewer = isAdminViewer;
  }

  public void setExportUserUrl(String exportUserUrl) {
		this.exportUserUrl = exportUserUrl;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getCommunityPath() {
    return communityPath;
  }

  public void setCommunityPath(String communityPath) {
    this.communityPath = communityPath;
  }

  public String getExportCommunityUrl() {
    return exportCommunityUrl;
  }

  public void setExportCommunityUrl(String exportCommunityUrl) {
    this.exportCommunityUrl = exportCommunityUrl;
  }
  
  public String getCommunityDir() {
    return communityDir;
  }

  public void setCommunityDir(String communityDir) {
    this.communityDir = communityDir;
  }

  public String getContextRootPath() {
		return contextRootPath;
	}

	public void setContextRootPath(String root) {
		this.contextRootPath = root;
	}

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	public String getScriptPath() {
		return scriptPath;
	}

	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}

	public String getExportUserUrl() {
		return exportUserUrl;
	}

  public String getUserPath() {
    return userPath;
  }
  
  public void setUserPath(String userPath) {
    this.userPath = userPath;
  }
  
	public String getTripleStoreDir() {
		return tdbDirectory;
	}

	public void setTripleStoreDir(String tripleStoreDir) {
		this.tdbDirectory = tripleStoreDir;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isSandboxed() {
		return isSandboxed;
	}

	public void setSandboxed(boolean isSandboxed) {
		this.isSandboxed = isSandboxed;
	}

	public String getDotFile() {
		return dotFile;
	}

	public void setDotFile(String dotFile) {
		this.dotFile = dotFile;
	}

	public String getStorageDirectory() {
		return storageDirectory;
	}

	public void setStorageDirectory(String storageDirectory) {
		this.storageDirectory = storageDirectory;
	}
  
  public Set<String> getEnginesList() {
    return this.engines.keySet();
  }
}

class ExeEngine {
	public static enum Type { PLAN, STEP, BOTH };
	Type type;
	String name;
	String implementation;
	Properties props;
	public ExeEngine(String name, String implementation, Type type) {
		this.type = type;
		this.name = name;
		this.implementation = implementation;
		props = new Properties();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public String getImplementation() {
		return implementation;
	}
	public void setImplementation(String implementation) {
		this.implementation = implementation;
	}
	public Properties getProperties() {
		return props;
	}
	public void setProperties(Properties props) {
		this.props = props;
	}
	public void addProperty(String key, String value) {
		this.props.setProperty(key, value);
	}
}
