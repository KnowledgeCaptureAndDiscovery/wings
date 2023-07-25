package edu.isi.wings.portal.classes.config;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class OntologyConfig {

  public static final String ONTOLOGY_COMPONENT_KEY = "ontology.component";
  public static final String ONTOLOGY_DATA_KEY = "ontology.data";
  public static final String ONTOLOGY_EXECUTION_KEY = "ontology.execution";
  public static final String ONTOLOGY_RESOURCE_KEY = "ontology.resource";
  public static final String ONTOLOGY_WORKFLOW_KEY = "ontology.workflow";
  public static String ONT_DIR_URL = "http://www.wings-workflows.org/ontology";

  public String componentOntologyUrl = ONT_DIR_URL + "/component.owl";
  public String dataOntologyUrl = ONT_DIR_URL + "/data.owl";
  public String executionOntologyUrl = ONT_DIR_URL + "/execution.owl";
  public String resourceOntologyUrl = ONT_DIR_URL + "/resource.owl";
  public String workflowOntologyUrl = ONT_DIR_URL + "/workflow.owl";

  public OntologyConfig(PropertyListConfiguration serverConfig) {
    if (serverConfig.getString(ONTOLOGY_DATA_KEY) != null) {
      this.dataOntologyUrl = serverConfig.getString(ONTOLOGY_DATA_KEY);
    }
    if (serverConfig.getString(ONTOLOGY_COMPONENT_KEY) != null) {
      this.componentOntologyUrl =
        serverConfig.getString(ONTOLOGY_COMPONENT_KEY);
    }
    if (serverConfig.getString(ONTOLOGY_WORKFLOW_KEY) != null) {
      this.workflowOntologyUrl = serverConfig.getString(ONTOLOGY_WORKFLOW_KEY);
    }
    if (serverConfig.getString(ONTOLOGY_EXECUTION_KEY) != null) {
      this.executionOntologyUrl =
        serverConfig.getString(ONTOLOGY_EXECUTION_KEY);
    }
    if (serverConfig.getString(ONTOLOGY_RESOURCE_KEY) != null) {
      this.resourceOntologyUrl = serverConfig.getString(ONTOLOGY_RESOURCE_KEY);
    }
  }

  public String getComponentOntologyUrl() {
    return componentOntologyUrl;
  }

  public String getDataOntologyUrl() {
    return dataOntologyUrl;
  }

  public String getExecutionOntologyUrl() {
    return executionOntologyUrl;
  }

  public String getResourceOntologyUrl() {
    return resourceOntologyUrl;
  }

  public String getWorkflowOntologyUrl() {
    return workflowOntologyUrl;
  }
}
