package edu.isi.wings.portal.classes.config;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class TripleStoreConfig {
  /**
   * Handle the following configuration keys:
   * graph-name = "exportTest"
   * publish = http://ontosoft.isi.edu:3030/provenance/data;
   * query = http://ontosoft.isi.edu:3030/provenance/sparql;
   * domains-directory = /opt/wings/storage/default;
   *
   *
   */
  private static final String PUBLISHER_SPARQL_DOMAINS_DIRECTORY_KEY = "publisher.triple-store.domains-directory";
  private static final String PUBLISHER_ENDPOINT_QUERY_KEY = "publisher.triple-store.query";
  private static final String PUBLISHER_ENDPOINT_POST_KEY = "publisher.triple-store.publish";
  private static final String PUBLISHER_GRAPH_NAME = "publisher.triple-store.graph-name";

  String publishUrl;
  String queryUrl;
  String graphName;
  String domainsDir;

  public TripleStoreConfig(PropertyListConfiguration serverConfig) {
    this.publishUrl = serverConfig.getString(PUBLISHER_ENDPOINT_POST_KEY);
    this.queryUrl = serverConfig.getString(PUBLISHER_ENDPOINT_QUERY_KEY);
    this.graphName = serverConfig.getString(PUBLISHER_GRAPH_NAME);
    this.domainsDir = serverConfig.getString(PUBLISHER_SPARQL_DOMAINS_DIRECTORY_KEY);
  }

  public String getPublishUrl() {
    return publishUrl;
  }

  public String getQueryUrl() {
    return queryUrl;
  }

  public String getGraphName() {
    return graphName;
  }

  public String getDomainsDir() {
    return domainsDir;
  }
}
