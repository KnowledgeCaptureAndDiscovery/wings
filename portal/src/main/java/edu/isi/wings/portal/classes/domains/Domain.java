package edu.isi.wings.portal.classes.domains;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.io.FileUtils;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.common.kb.PropertiesHelper;
import edu.isi.wings.execution.tools.ExecutionToolsFactory;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;

public class Domain {
	String defaultDomainConfigFilename = "domain.properties";
	String domainConfigFile;
	String domainUrl;
	String domainDirectory;

	String domainName;
	Boolean useSharedTripleStore;
	
	DomainLibrary templateLibrary;
	UrlMapPrefix newTemplateDirectory;
	
	DomainLibrary executionLibrary;
	UrlMapPrefix newExecutionDirectory;
	
	DomainLibrary dataLibrary;
	DomainOntology dataOntology;
	
	DomainLibrary abstractComponentLibrary;
	DomainLibrary concreteComponentLibrary;
	ArrayList<DomainLibrary> concreteComponentLibraries;
	String componentLibraryNamespace;
	
	String planEngine;
	String stepEngine;
	
	boolean isLegacy = false;
	ArrayList<Permission> permissions;
	
	static String fsep = File.separator;
	static String usep = "/";
	
	/*
	 * Static Functions for creating/importing domains
	 */
	public static Domain createDefaultDomain(String domName, String userDir, String userUrl) {
		Domain domain = new Domain(domName);
		domain.setDomainDirectory(userDir + fsep + domain.domainName);
		domain.setDomainUrl(userUrl + usep + domain.domainName);
		domain.templateLibrary = new DomainLibrary("workflows/library.owl", 
				"ontology/workflows/library.owl");
		domain.newTemplateDirectory = new UrlMapPrefix("workflows", 
				"ontology/workflows");
		
		domain.executionLibrary = new DomainLibrary("executions/library.owl", 
				"ontology/executions/library.owl");
		domain.newExecutionDirectory = new UrlMapPrefix("executions", 
				"ontology/executions");
		
		domain.dataLibrary = new DomainLibrary("data/library.owl",
				"ontology/data/library.owl");
		domain.dataLibrary.setStorageDirectory("data");
		domain.dataOntology = new DomainOntology("data/ontology.owl", 
				"ontology/data/ontology.owl");
		domain.abstractComponentLibrary = new DomainLibrary("components/abstract.owl", 
				"ontology/components/abstract.owl");
		domain.concreteComponentLibrary = new DomainLibrary("components/library.owl", 
				"ontology/components/library.owl");
		domain.concreteComponentLibrary.setName("library");
		domain.concreteComponentLibrary.setStorageDirectory("code/library");
		domain.componentLibraryNamespace = "components/library.owl#";
		domain.concreteComponentLibraries.add(domain.concreteComponentLibrary);
		
		domain.useSharedTripleStore = true;
		domain.isLegacy = false;
		
		domain.planEngine = "Local";
		domain.stepEngine = "Local";
		
    domain.permissions = new ArrayList<Permission>();
    
		domain.saveDomain();
		return domain; 
	}

	public static Domain importLegacyDomain(String domName, Config config, String legacyDomDir) {
		// Get Legacy apis
		PropertiesHelper.resetProperties();
		PropertiesHelper.loadWingsProperties(legacyDomDir + fsep + "wings.properties");
		Properties legacyprops = TemplateFactory.createLegacyConfiguration();
		legacyprops.putAll(DataFactory.createLegacyConfiguration());
		legacyprops.putAll(ComponentFactory.createLegacyConfiguration());
		DataCreationAPI ldc = DataFactory.getCreationAPI(legacyprops);
		ComponentCreationAPI lacc = ComponentFactory.getCreationAPI(legacyprops, false);
		ComponentCreationAPI lccc = ComponentFactory.getCreationAPI(legacyprops, true);
		TemplateCreationAPI ltc = TemplateFactory.getCreationAPI(legacyprops);

		// Get new apis
		Domain domain = Domain.createDefaultDomain(domName, config.getUserDir(), config.getExportUserUrl());
		Properties props = config.getProperties(domain);
		DataCreationAPI dc = DataFactory.getCreationAPI(props);
		ComponentCreationAPI acc = ComponentFactory.getCreationAPI(props, false);
		ComponentCreationAPI ccc = ComponentFactory.getCreationAPI(props, true);
		TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);
		
		// Copy from legacy apis to new apis
		dc.copyFrom(ldc);
		acc.copyFrom(lacc);
		ccc.copyFrom(lccc);
		tc.copyFrom(ltc);
		
		// Copy legacy data/code directories to new data/code storage directory
		File srcDataDir = new File(PropertiesHelper.getDataDirectory());
		File destDataDir = new File(domain.getDomainDirectory() + fsep
				+ domain.getDataLibrary().getStorageDirectory());
		File srcCodeDir = new File(PropertiesHelper.getCodeDirectory());
		File destCodeDir = new File(domain.getDomainDirectory() + fsep
				+ domain.getConcreteComponentLibrary().getStorageDirectory());
		try {
			if(srcDataDir.isDirectory())
				FileUtils.copyDirectory(srcDataDir, destDataDir);
			if(srcCodeDir.isDirectory()) {
				FileUtils.copyDirectory(srcCodeDir, destCodeDir);
				// FIXME: Setting executable permissions on all files for now
				for(File f : FileUtils.listFiles(destCodeDir, null, true)) {
					f.setExecutable(true);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return domain;
	}

	public static Domain importDomain(String domName, Config config, String importDomDir) {
		String domurl;
		try {
			domurl = FileUtils.readFileToString(new File(importDomDir + File.separator + "domain.url"));
			domurl = domurl.trim();
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		Domain fromdom = new Domain(domName, importDomDir, domurl, false);
		Properties props = config.getProperties(fromdom);
		DataCreationAPI dc = DataFactory.getCreationAPI(props);
		ComponentCreationAPI acc = ComponentFactory.getCreationAPI(props, false);
		ComponentCreationAPI ccc = ComponentFactory.getCreationAPI(props, true);
		TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);

		Domain todom = Domain.createDefaultDomain(domName, config.getUserDir(), config.getExportUserUrl());
		props = config.getProperties(todom);
		DataCreationAPI todc = DataFactory.getCreationAPI(props);
		ComponentCreationAPI toacc = ComponentFactory.getCreationAPI(props, false);
		ComponentCreationAPI toccc = ComponentFactory.getCreationAPI(props, true);
		TemplateCreationAPI totc = TemplateFactory.getCreationAPI(props);
		
		// Copy from legacy apis to new apis
		todc.copyFrom(dc);
		toacc.copyFrom(acc);
		toccc.copyFrom(ccc);
		totc.copyFrom(tc);
		
		// Copy legacy data/code directories to new data/code storage directory
		File srcDataDir = new File(fromdom.getDomainDirectory() + fsep
				+ fromdom.getDataLibrary().getStorageDirectory());
		File destDataDir = new File(todom.getDomainDirectory() + fsep
				+ todom.getDataLibrary().getStorageDirectory());
		File srcCodeDir = new File(fromdom.getDomainDirectory() + fsep
				+ fromdom.getConcreteComponentLibrary().getStorageDirectory());
		File destCodeDir = new File(todom.getDomainDirectory() + fsep
				+ todom.getConcreteComponentLibrary().getStorageDirectory());
		try {
			if(srcDataDir.isDirectory())
				FileUtils.copyDirectory(srcDataDir, destDataDir);
			if(srcCodeDir.isDirectory()) {
				FileUtils.copyDirectory(srcCodeDir, destCodeDir);
				// FIXME: Setting executable permissions on all files for now
				for(File f : FileUtils.listFiles(destCodeDir, null, true)) {
					f.setExecutable(true);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return todom;
	}
	
	public static File exportDomain(Domain fromdom, Config config) {
		Properties props = config.getProperties(fromdom);
		DataCreationAPI dc = DataFactory.getCreationAPI(props);
		ComponentCreationAPI acc = ComponentFactory.getCreationAPI(props, false);
		ComponentCreationAPI ccc = ComponentFactory.getCreationAPI(props, true);
		TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);
		
		File tempdir;
		try {
			 tempdir = File.createTempFile("domain-", "-temp");
			 if(!tempdir.delete() || !tempdir.mkdirs())
				 return null;
		} catch (IOException e1) {
			return null;
		}
		
		Domain todom = Domain.createDefaultDomain(fromdom.getDomainName(), 
				tempdir.getAbsolutePath(), config.getExportUserUrl());
		todom.setUseSharedTripleStore(false);
		todom.saveDomain();
		props = config.getProperties(todom);
		DataCreationAPI todc = DataFactory.getCreationAPI(props);
		ComponentCreationAPI toacc = ComponentFactory.getCreationAPI(props, false);
		ComponentCreationAPI toccc = ComponentFactory.getCreationAPI(props, true);
		TemplateCreationAPI totc = TemplateFactory.getCreationAPI(props);
		
		// Copy into non-triple-store apis
		todc.copyFrom(dc);
		toacc.copyFrom(acc);
		toccc.copyFrom(ccc);
		totc.copyFrom(tc);
		
		// Copy legacy data/code directories to new data/code storage directory
		File srcDataDir = new File(fromdom.getDomainDirectory() + fsep
				+ fromdom.getDataLibrary().getStorageDirectory());
		File destDataDir = new File(todom.getDomainDirectory() + fsep
				+ todom.getDataLibrary().getStorageDirectory());
		File srcCodeDir = new File(fromdom.getDomainDirectory() + fsep
				+ fromdom.getConcreteComponentLibrary().getStorageDirectory());
		File destCodeDir = new File(todom.getDomainDirectory() + fsep
				+ todom.getConcreteComponentLibrary().getStorageDirectory());
		try {
			if(srcDataDir.isDirectory())
				FileUtils.copyDirectory(srcDataDir, destDataDir);
			if(srcCodeDir.isDirectory()) {
				FileUtils.copyDirectory(srcCodeDir, destCodeDir);
				// FIXME: Setting executable permissions on all files for now
				for(File f : FileUtils.listFiles(destCodeDir, null, true)) {
					f.setExecutable(true);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Copy domain url
		File domUriFile = new File(todom.getDomainDirectory() + fsep + "domain.url");
		try {
			FileUtils.write(domUriFile, todom.getDomainUrl());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return tempdir;
	}
	
	public static boolean deleteDomain(Domain domain, Config config, boolean deleteStorage) {
		// Get new apis
		Properties props = config.getProperties(domain);
		DataCreationAPI dc = DataFactory.getCreationAPI(props);
		ComponentCreationAPI acc = ComponentFactory.getCreationAPI(props, false);
		ComponentCreationAPI ccc = ComponentFactory.getCreationAPI(props, true);
		TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);
		ExecutionMonitorAPI em = ExecutionToolsFactory.createMonitor(props);
		dc.delete();
		acc.delete();
		ccc.delete();
		tc.delete();
		em.delete();
		
		// Remove domain directory
		if(deleteStorage) {
			try {
				FileUtils.deleteDirectory(new File(domain.getDomainDirectory()));
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public static Domain renameDomain(Domain domain, String newname, Config config) {
		Properties props = config.getProperties(domain);
		DataCreationAPI dc = DataFactory.getCreationAPI(props);
		ComponentCreationAPI acc = ComponentFactory.getCreationAPI(props, false);
		ComponentCreationAPI ccc = ComponentFactory.getCreationAPI(props, true);
		TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);
		
		File tempdir;
		try {
			 tempdir = File.createTempFile("domain-", "-temp");
			 if(!tempdir.delete() || !tempdir.mkdirs())
				 return null;
		} catch (IOException e1) {
			return null;
		}
		
		Domain todom = Domain.createDefaultDomain(newname, 
				config.getUserDir(), config.getExportUserUrl());
		todom.saveDomain();
		
		props = config.getProperties(todom);
		DataCreationAPI todc = DataFactory.getCreationAPI(props);
		ComponentCreationAPI toacc = ComponentFactory.getCreationAPI(props, false);
		ComponentCreationAPI toccc = ComponentFactory.getCreationAPI(props, true);
		TemplateCreationAPI totc = TemplateFactory.getCreationAPI(props);
		
		// Copy into non-triple-store apis
		todc.copyFrom(dc);
		toacc.copyFrom(acc);
		toccc.copyFrom(ccc);
		totc.copyFrom(tc);
		
		// Copy legacy data/code directories to new data/code storage directory
		File srcDataDir = new File(domain.getDomainDirectory() + fsep
				+ domain.getDataLibrary().getStorageDirectory());
		File destDataDir = new File(todom.getDomainDirectory() + fsep
				+ todom.getDataLibrary().getStorageDirectory());
		File srcCodeDir = new File(domain.getDomainDirectory() + fsep
				+ domain.getConcreteComponentLibrary().getStorageDirectory());
		File destCodeDir = new File(todom.getDomainDirectory() + fsep
				+ todom.getConcreteComponentLibrary().getStorageDirectory());
		try {
			if(srcDataDir.isDirectory())
				FileUtils.copyDirectory(srcDataDir, destDataDir);
			if(srcCodeDir.isDirectory()) {
				FileUtils.copyDirectory(srcCodeDir, destCodeDir);
				// FIXME: Setting executable permissions on all files for now
				for(File f : FileUtils.listFiles(destCodeDir, null, true)) {
					f.setExecutable(true);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		deleteDomain(domain, config, true);
		
		return todom;
	}
	
	
	/*
	 * Constructors / Initialization
	 */
	private Domain(String domainName) {
		this.domainName = domainName;
		this.concreteComponentLibraries = new ArrayList<DomainLibrary>();
		this.permissions = new ArrayList<Permission>();
	}
	
	public Domain(DomainInfo info) {
		this(info.getName(), info.getDirectory(), info.getUrl(), info.isLegacy());
	}
	
	public Domain(String domainName, String domainDirectory, String domainUrl, boolean legacy) {
		this(domainName);
		this.setDomainDirectory(domainDirectory);
		if(legacy == true) {
			this.isLegacy = true;
			this.planEngine = "Local";
			this.stepEngine = "Local";
		}
		else {
			this.setDomainUrl(domainUrl);
			this.initializeDomain();
		}
	}

	private void initializeDomain() {
		try {
			PropertyListConfiguration config = new PropertyListConfiguration(this.domainConfigFile);

			this.useSharedTripleStore = config.getBoolean("useSharedTripleStore", true);
			this.planEngine = config.getString("executions.engine.plan", "Local");
			this.stepEngine = config.getString("executions.engine.step", "Local");

			this.templateLibrary = new DomainLibrary(config.getString("workflows.library.url"),
					config.getString("workflows.library.map"));
			this.newTemplateDirectory = new UrlMapPrefix(config.getString("workflows.prefix.url"),
					config.getString("workflows.prefix.map"));

			this.executionLibrary = new DomainLibrary(config.getString("executions.library.url"),
					config.getString("executions.library.map"));
			this.newExecutionDirectory = new UrlMapPrefix(config.getString("executions.prefix.url"),
					config.getString("executions.prefix.map"));
			
			this.dataOntology = new DomainOntology(config.getString("data.ontology.url"),
					config.getString("data.ontology.map"));
			this.dataLibrary = new DomainLibrary(config.getString("data.library.url"),
					config.getString("data.library.map"));
			this.dataLibrary.setStorageDirectory(config.getString("data.library.storage"));

			this.componentLibraryNamespace = config.getString("components.namespace");
			this.abstractComponentLibrary = new DomainLibrary(
					config.getString("components.abstract.url"),
					config.getString("components.abstract.map"));

			String concreteLibraryName = config.getString("components.concrete");
			@SuppressWarnings("unchecked")
			List<SubnodeConfiguration> clibs = config.configurationsAt("components.libraries.library");
			for (SubnodeConfiguration clib : clibs) {
				String url = clib.getString("url");
				String map = clib.getString("map");
				String name = clib.getString("name");
				String codedir = clib.getString("storage");
				DomainLibrary concreteLib = new DomainLibrary(url, map, name, codedir);
				this.concreteComponentLibraries.add(concreteLib);
				if(name.equals(concreteLibraryName))
					this.concreteComponentLibrary = concreteLib;
			}
			
			@SuppressWarnings("unchecked")
			List<SubnodeConfiguration> perms = config.configurationsAt("permissions.permission");
      for (SubnodeConfiguration perm : perms) {
        String userid = perm.getString("userid");
        boolean canRead = perm.getBoolean("canRead", false);
        boolean canWrite = perm.getBoolean("canWrite", false);
        boolean canExecute = perm.getBoolean("canExecute", false);
        Permission permission = new Permission(userid, canRead, canWrite, canExecute);
        this.permissions.add(permission);
      }
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Member functions
	 */

	// Translate config into Properties that are currently used by catalogs & planners
	public Properties getProperties() {
		// Handle legacy domains
		if(this.isLegacy) {
			PropertiesHelper.resetProperties();
			PropertiesHelper.loadWingsProperties(this.domainDirectory + "/wings.properties");
			Properties props = TemplateFactory.createLegacyConfiguration();
			props.putAll(ComponentFactory.createLegacyConfiguration());
			props.putAll(DataFactory.createLegacyConfiguration());
			//props.putAll(ExecutionFactory.createLegacyConfiguration());
			return props;
		}
		
		Properties domainProps = new Properties();
		String domurl = this.domainUrl + usep;
		String domdir = new File(this.domainDirectory).getAbsolutePath() + fsep;
		
		domainProps.setProperty("lib.domain.workflow.url", domurl + this.templateLibrary.getUrl());
		domainProps.setProperty("domain.workflows.dir.url",
				domurl + this.newTemplateDirectory.getUrl());
		
		domainProps.setProperty("lib.domain.execution.url", domurl + this.executionLibrary.getUrl());
		domainProps.setProperty("domain.executions.dir.url",
				domurl + this.newExecutionDirectory.getUrl());
		
		domainProps.setProperty("lib.domain.data.url", domurl + this.dataLibrary.getUrl());
		domainProps.setProperty("ont.domain.data.url", domurl + this.dataOntology.getUrl());
		domainProps
				.setProperty("lib.abstract.url", domurl + this.abstractComponentLibrary.getUrl());
		domainProps
				.setProperty("lib.concrete.url", domurl + this.concreteComponentLibrary.getUrl());
		domainProps.setProperty("ont.domain.component.ns", domurl + this.componentLibraryNamespace);

		domainProps.setProperty("lib.domain.data.storage",
				domdir + this.dataLibrary.getStorageDirectory());
		domainProps.setProperty("lib.domain.code.storage",
				domdir + this.concreteComponentLibrary.getStorageDirectory());

		if (!this.getUseSharedTripleStore()) {
			String furl = "file:";
			domainProps.setProperty("lib.domain.workflow.map",
					furl + domdir + this.templateLibrary.getMapping());
			domainProps.setProperty("domain.workflows.dir.map",
					furl + domdir + this.newTemplateDirectory.getMapping());
			
			domainProps.setProperty("lib.domain.execution.map",
					furl + domdir + this.executionLibrary.getMapping());
			domainProps.setProperty("domain.executions.dir.map",
					furl + domdir + this.newExecutionDirectory.getMapping());
			
			domainProps.setProperty("lib.domain.data.map", 
					furl + domdir + this.dataLibrary.getMapping());
			domainProps.setProperty("ont.domain.data.map", 
					furl + domdir + this.dataOntology.getMapping());
			domainProps.setProperty("lib.abstract.map",
					furl + domdir + this.abstractComponentLibrary.getMapping());
			domainProps.setProperty("lib.concrete.map",
					furl + domdir + this.concreteComponentLibrary.getMapping());
		}
		return domainProps;
	}

	private void setUrlMapProp(PropertyListConfiguration config, String prefix, UrlMapPrefix urlmap) {
		config.addProperty(prefix + ".url", urlmap.getUrl());
		config.addProperty(prefix + ".map", urlmap.getMapping());
	}

	public void saveDomain() {
		PropertyListConfiguration config = new PropertyListConfiguration();
		config.addProperty("name", this.domainName);
		config.addProperty("useSharedTripleStore", this.useSharedTripleStore);
		
		config.addProperty("executions.engine.plan", this.planEngine);
		config.addProperty("executions.engine.step", this.stepEngine);

		this.setUrlMapProp(config, "workflows.library", this.templateLibrary);
		this.setUrlMapProp(config, "workflows.prefix", this.newTemplateDirectory);
		
		this.setUrlMapProp(config, "executions.library", this.executionLibrary);
		this.setUrlMapProp(config, "executions.prefix", this.newExecutionDirectory);
		
		this.setUrlMapProp(config, "data.ontology", this.dataOntology);
		this.setUrlMapProp(config, "data.library", this.dataLibrary);
		config.addProperty("data.library.storage", this.dataLibrary.getStorageDirectory());

		config.addProperty("components.namespace", this.componentLibraryNamespace);
		this.setUrlMapProp(config, "components.abstract", this.abstractComponentLibrary);
		config.addProperty("components.concrete", this.concreteComponentLibrary.getName());

		for (DomainLibrary clib : this.concreteComponentLibraries) {
			config.addProperty("components.libraries.library(-1).url", clib.getUrl());
			config.addProperty("components.libraries.library.map", clib.getMapping());
			config.addProperty("components.libraries.library.name", clib.getName());
			config.addProperty("components.libraries.library.storage", clib.getStorageDirectory());
		}

		for (Permission permission : this.permissions) {
		  config.addProperty("permissions.permission(-1).userid", permission.getUserid());
		  config.addProperty("permissions.permission.canRead", permission.canRead());
		  config.addProperty("permissions.permission.canWrite", permission.canWrite());
		  config.addProperty("permissions.permission.canExecute", permission.canExecute());
		}
		
		if (this.domainDirectory != null) {
			File domdir = new File(this.domainDirectory);
			if (!domdir.exists() && !domdir.mkdirs())
				System.err.println("Could not create domain directory: " + this.domainDirectory);
		}
		try {
			config.save(this.domainConfigFile);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void prepareDomainForExport() {
		// TODO: If useSharedTripleStore, then create owl files into map paths
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getDomainConfigFile() {
		return domainConfigFile;
	}

	public void setDomainConfigFile(String domainConfigFile) {
		this.domainConfigFile = domainConfigFile;
	}

	public String getDomainDirectory() {
		return domainDirectory;
	}

	public void setDomainDirectory(String domainDirectory) {
		this.domainDirectory = domainDirectory;
		this.domainConfigFile = domainDirectory + fsep + this.defaultDomainConfigFilename;
	}

	public DomainLibrary getTemplateLibrary() {
		return templateLibrary;
	}

	public UrlMapPrefix getNewTemplateDirectory() {
		return newTemplateDirectory;
	}

	public void setNewTemplateDirectory(UrlMapPrefix newTemplateDirectory) {
		this.newTemplateDirectory = newTemplateDirectory;
	}

	public DomainLibrary getExecutionLibrary() {
		return executionLibrary;
	}

	public void setExecutionLibrary(DomainLibrary executionLibrary) {
		this.executionLibrary = executionLibrary;
	}

	public DomainLibrary getDataLibrary() {
		return dataLibrary;
	}

	public DomainOntology getDataOntology() {
		return dataOntology;
	}

	public DomainLibrary getAbstractComponentLibrary() {
		return abstractComponentLibrary;
	}

	public void setAbstractComponentLibrary(DomainLibrary abstractComponentLibrary) {
		this.abstractComponentLibrary = abstractComponentLibrary;
	}

	public DomainLibrary getConcreteComponentLibrary() {
		return concreteComponentLibrary;
	}

	public void setConcreteComponentLibrary(DomainLibrary concreteComponentLibrary) {
		this.concreteComponentLibrary = concreteComponentLibrary;
	}

	public ArrayList<DomainLibrary> getConcreteComponentLibraries() {
		return concreteComponentLibraries;
	}

	public void setConcreteComponentLibraries(ArrayList<DomainLibrary> concreteComponentLibraries) {
		this.concreteComponentLibraries = concreteComponentLibraries;
	}

	public void addConcreteComponentLibrary(DomainLibrary concreteComponentLibrary) {
		this.concreteComponentLibraries.add(concreteComponentLibrary);
	}

	public String getDefaultDomainConfigFilename() {
		return defaultDomainConfigFilename;
	}

	public void setDefaultDomainConfigFilename(String defaultDomainConfigFilename) {
		this.defaultDomainConfigFilename = defaultDomainConfigFilename;
	}

	public String getDomainUrl() {
		return domainUrl;
	}

	public void setDomainUrl(String domainUrl) {
		this.domainUrl = domainUrl;
	}

	public String getComponentLibraryNamespace() {
		return componentLibraryNamespace;
	}

	public void setComponentLibraryNamespace(String componentLibraryNamespace) {
		this.componentLibraryNamespace = componentLibraryNamespace;
	}

	public void setTemplateLibrary(DomainLibrary templateLibrary) {
		this.templateLibrary = templateLibrary;
	}

	public void setDataLibrary(DomainLibrary dataLibrary) {
		this.dataLibrary = dataLibrary;
	}

	public void setDataOntology(DomainOntology dataOntology) {
		this.dataOntology = dataOntology;
	}

	public Boolean getUseSharedTripleStore() {
		return useSharedTripleStore;
	}

	public void setUseSharedTripleStore(Boolean useSharedTripleStore) {
		this.useSharedTripleStore = useSharedTripleStore;
	}

	public String getPlanEngine() {
		return planEngine;
	}

	public void setPlanEngine(String planEngine) {
		this.planEngine = planEngine;
	}

	public String getStepEngine() {
		return stepEngine;
	}

	public void setStepEngine(String stepEngine) {
		this.stepEngine = stepEngine;
	}

	public boolean isLegacy() {
		return isLegacy;
	}

	public void setLegacy(boolean isLegacy) {
		this.isLegacy = isLegacy;
	}

	public ArrayList<Permission> getPermissions() {
	  return this.permissions;
	}
}
