package edu.isi.wings.portal.classes.config;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class OntologyConfig {

  private String ontologyDefaultUrl = "http://www.wings-workflows.org/ontology";
  private String resourceOntologyUrl = ontologyDefaultUrl + "/resource.owl";
  private String workflowOntologyUrl = ontologyDefaultUrl + "/workflow.owl";
  private String dataOntologyUrl = ontologyDefaultUrl + "/data.owl";
  private String componentOntologyUrl = ontologyDefaultUrl + "/component.owl";
  private String executionOntologyUrl = ontologyDefaultUrl + "/execution.owl";

  public OntologyConfig(PropertyListConfiguration serverConfig) {
    dataOntologyUrl = serverConfig.getString("ontology.data");
    componentOntologyUrl = serverConfig.getString("ontology.component");
    workflowOntologyUrl = serverConfig.getString("ontology.workflow");
    executionOntologyUrl = serverConfig.getString("ontology.execution");
    resourceOntologyUrl = serverConfig.getString("ontology.resource");
  }

  public OntologyConfig() {
  }

  public String getWorkflowOntologyUrl() {
    return workflowOntologyUrl;
  }

  public String getDataOntologyUrl() {
    return dataOntologyUrl;
  }

  public String getComponentOntologyUrl() {
    return componentOntologyUrl;
  }

  public String getExecutionOntologyUrl() {
    return executionOntologyUrl;
  }

  public String getResourceOntologyUrl() {
    return resourceOntologyUrl;
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

  public void setExecutionOntologyUrl(String executionOntologyUrl) {
    this.executionOntologyUrl = executionOntologyUrl;
  }

  public void setResourceOntologyUrl(String resourceOntologyUrl) {
    this.resourceOntologyUrl = resourceOntologyUrl;
  }

  public String getOntologyDefaultUrl() {
    return ontologyDefaultUrl;
  }

  public void setOntologyDefaultUrl(String ontologyDefaultUrl) {
    this.ontologyDefaultUrl = ontologyDefaultUrl;
  }

}
