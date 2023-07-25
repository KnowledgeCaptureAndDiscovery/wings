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
    try {
      checkIfFileExists(portalConfigurationFile);
    } catch (Exception e) {
      throw new RuntimeException(
        "Could not find config file: " + portalConfigurationFile
      );
    }
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
