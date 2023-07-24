package edu.isi.wings.portal.classes.config;

import java.io.File;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class PortalConfig {

  public String portalConfigurationFile;
  public MainConfig mainConfig;
  public OntologyConfig ontologyConfig;
  public StorageConfig storageConfig;
  public PlannerConfig plannerConfig;
  public ExecutionConfig executionConfig;

  public PublisherConfig publisher;
  public boolean sandboxed;

  public void initializePortalConfig(HttpServletRequest request) {
    PropertyListConfiguration serverConfig = getPortalConfiguration(request);
    getOntologyConfiguration(serverConfig);
    getMainConfiguration(serverConfig, request);
    getStorageConfiguration(serverConfig);
    getPlannerConfiguration(serverConfig);
    getEngineNodeConfiguration(serverConfig);
    getPublisherConfiguration(serverConfig);
  }

  public PropertyListConfiguration getPortalConfiguration(
    HttpServletRequest request
  ) {
    ServletContext app = request.getSession().getServletContext();
    this.portalConfigurationFile = obtainConfigPath(app, request);
    if (this.portalConfigurationFile == null) createDefaultConfigurationFile(
      request
    );
    checkIfFileExists(this.portalConfigurationFile);
    return loadConfigurationOnProps();
  }

  private String obtainConfigPath(
    ServletContext app,
    HttpServletRequest request
  ) {
    return app.getInitParameter("config.file");
  }

  private PropertyListConfiguration loadConfigurationOnProps() {
    PropertyListConfiguration props = new PropertyListConfiguration();
    try {
      props.load(this.portalConfigurationFile);
    } catch (Exception e) {
      throw new RuntimeException(
        "Could not load config file: " + this.portalConfigurationFile
      );
    }
    return props;
  }

  private void getPublisherConfiguration(
    PropertyListConfiguration serverConfig
  ) {
    this.publisher = new PublisherConfig(serverConfig);
  }

  private void getOntologyConfiguration(
    PropertyListConfiguration serverConfig
  ) {
    this.ontologyConfig = new OntologyConfig(serverConfig);
  }

  private void getMainConfiguration(
    PropertyListConfiguration serverConfig,
    HttpServletRequest request
  ) {
    this.mainConfig = new MainConfig(serverConfig, request);
  }

  private void getStorageConfiguration(PropertyListConfiguration serverConfig) {
    this.storageConfig = new StorageConfig(serverConfig);
  }

  private void getPlannerConfiguration(PropertyListConfiguration serverConfig) {
    this.plannerConfig = new PlannerConfig(serverConfig);
  }

  private void createDefaultConfigurationFile(HttpServletRequest request) {
    String configFileName = "portal.properties";
    String configDirectory = createDefaultConfigurationDirectory();
    this.portalConfigurationFile =
      configDirectory + File.separator + configFileName;
    File cfile = new File(this.portalConfigurationFile);
    File configDir = cfile.getParentFile();
    StorageConfig.createStorageDirectory(configDir.getAbsolutePath());
    if (request != null) createDefaultPortalConfig(request);
  }

  private String createDefaultConfigurationDirectory() {
    String defaultDirectory = ".wings";
    String home = System.getProperty("user.home");
    if (home != null && !home.equals("")) {
      return home + File.separator + defaultDirectory;
    }
    return "/etc/wings/portal.properties";
  }

  private void createDefaultPortalConfig(HttpServletRequest request) {
    String server =
      request.getScheme() +
      "://" +
      request.getServerName() +
      ":" +
      request.getServerPort();

    PropertyListConfiguration config = new PropertyListConfiguration();
    StorageConfig storageConfig = new StorageConfig(config);
    OntologyConfig ontologyDefaultConfig = new OntologyConfig();
    MainConfig mainDefaultConfig = new MainConfig(server, request);
    ExecutionConfig executionConfig = new ExecutionConfig();

    config.addProperty(MainConfig.MAIN_SERVER_KEY, server);
    config.addProperty(MainConfig.MAIN_GRAPHVIZ_KEY, mainDefaultConfig.dotFile);
    config.addProperty(
      StorageConfig.STORAGE_LOCAL,
      storageConfig.storageDirectory
    );
    config.addProperty(StorageConfig.STORAGE_TDB, storageConfig.tdbDirectory);
    config.addProperty(
      OntologyConfig.ONTOLOGY_COMPONENT_KEY,
      ontologyDefaultConfig.componentOntologyUrl
    );
    config.addProperty(
      OntologyConfig.ONTOLOGY_DATA_KEY,
      ontologyDefaultConfig.dataOntologyUrl
    );
    config.addProperty(
      OntologyConfig.ONTOLOGY_EXECUTION_KEY,
      ontologyDefaultConfig.executionOntologyUrl
    );
    config.addProperty(
      OntologyConfig.ONTOLOGY_RESOURCE_KEY,
      ontologyDefaultConfig.resourceOntologyUrl
    );
    config.addProperty(
      OntologyConfig.ONTOLOGY_WORKFLOW_KEY,
      ontologyDefaultConfig.workflowOntologyUrl
    );
    executionConfig.addDefaultEngineConfig(config);

    try {
      config.save(this.portalConfigurationFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void getEngineNodeConfiguration(
    PropertyListConfiguration serverConfig
  ) {
    this.executionConfig = new ExecutionConfig(serverConfig);
  }

  public String getPortalConfigurationFile() {
    return portalConfigurationFile;
  }

  public PublisherConfig getPublisher() {
    return publisher;
  }

  public PlannerConfig getPlannerConfig() {
    return plannerConfig;
  }

  public boolean isSandboxed() {
    return sandboxed;
  }

  public OntologyConfig getOntologyConfig() {
    return ontologyConfig;
  }

  private static void checkIfFileExists(String filePath) {
    File cfile = new File(filePath);
    if (!cfile.exists()) {
      throw new RuntimeException("Could not find file: " + filePath);
    }
  }
}
