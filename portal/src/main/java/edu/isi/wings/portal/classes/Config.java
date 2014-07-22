package edu.isi.wings.portal.classes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

import edu.isi.wings.execution.engine.ExecutionFactory;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.api.impl.local.LocalExecutionEngine;
import edu.isi.wings.execution.logger.LoggerFactory;
import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.logger.api.ExecutionMonitorAPI;
import edu.isi.wings.portal.classes.domains.Domain;
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
	private String userId;
	private String sessionId;
	private String contextRootPath;
	private String scriptPath;

	private String communityRelativeDir = "common";
	private String communityUrl;
	private String communityDir;
	
	// This following are user/domain specific properties
	private String userUrl;
	private String userDir;
	private Domain domain;

	public Config(HttpServletRequest request) {
		// Initialize portal config
		this.initializePortalConfig(request);

		// Initialize user config
		this.initializeUserConfig(request);
	}
	
	public boolean checkDomain(HttpServletResponse response) {
		String redirectUrl = this.contextRootPath+"/domain";
		if(this.domain == null && !this.scriptPath.equals(redirectUrl)) {
			response.setContentType("text/html");
			response.setHeader("Refresh", "5; URL="+this.contextRootPath+"/domain");
			try {
				response.getWriter().println("No Domain selected. Please select a domain first !!<br/>"
						+ "Redirecting in 5 seconds to <a href='"+redirectUrl+"'>"+redirectUrl+"</a>");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	private void initializeUserConfig(HttpServletRequest request) {
		this.userId = request.getRemoteUser();
		if(this.userId == null)
			return;
		
		this.sessionId = request.getSession().getId();

		this.userUrl = serverUrl + contextRootPath + exportServletPath + "/" + usersRelativeDir
				+ "/" + userId;
		this.userDir = storageDirectory + File.separator + usersRelativeDir + File.separator + userId;

		// Create userDir (if it doesn't exist)
		File uf = new File(this.userDir);
		if (!uf.exists() && !uf.mkdirs())
			System.err.println("Cannot create user directory : " + uf.getAbsolutePath());
		
		// Get user's selected domain
		DomainController dc = new DomainController(1, this);
		this.domain = dc.getUserDomain();
	}

	private void initializePortalConfig(HttpServletRequest request) {
		this.contextRootPath = request.getContextPath();
		this.scriptPath = this.contextRootPath + request.getServletPath();

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
		
    this.communityUrl = serverUrl + contextRootPath + exportServletPath + "/" 
        + communityRelativeDir;
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
		if (domain != null) {
			Properties props = domain.getProperties();
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

      if(this.getResourceOntologyUrl() == null)
        this.setResourceOntologyUrl(ontdirurl + "/resource.owl");
      props.setProperty("ont.resource.url", this.getResourceOntologyUrl());
      props.setProperty("lib.resource.url", this.getCommunityUrl()+"/resource/library.owl");
      
			ExeEngine pengine = engines.get(domain.getPlanEngine());
			ExeEngine sengine = engines.get(domain.getStepEngine());
			props.putAll(pengine.getProperties());
			props.putAll(sengine.getProperties());
			
			return props;
		}
		return null;
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
			ExecutionLoggerAPI logger = LoggerFactory.createLogger(this.getProperties());
			ExecutionMonitorAPI monitor = LoggerFactory.createMonitor(this.getProperties());
			pee.setStepExecutionEngine(see);
			pee.setExecutionLogger(logger);
			pee.setExecutionMonitor(monitor);
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
		return LoggerFactory.createMonitor(this.getProperties());
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
		this.userId = userId;
	}

	public String getUserDir() {
		return userDir;
	}

	public void setUserDir(String userDir) {
		this.userDir = userDir;
	}

	public void setUserUrl(String userUrl) {
		this.userUrl = userUrl;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getCommunityUrl() {
    return communityUrl;
  }

  public void setCommunityUrl(String communityUrl) {
    this.communityUrl = communityUrl;
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

	public String getUserUrl() {
		return userUrl;
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
