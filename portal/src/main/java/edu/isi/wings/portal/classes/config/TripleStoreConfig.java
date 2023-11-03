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
  private static final String PUBLISHER_EXPORT_URL = "publisher.triple-store.export-url";
  private static final String PUBLISHER_EXPORT_NAME = "publisher.triple-store.export-name";

  String publishUrl;
  String queryUrl;
  String exportName;
  String exportUrl;

  String domainsDir;

  public TripleStoreConfig(PropertyListConfiguration serverConfig) {
    this.publishUrl = serverConfig.getString(PUBLISHER_ENDPOINT_POST_KEY);
    this.queryUrl = serverConfig.getString(PUBLISHER_ENDPOINT_QUERY_KEY);
    this.exportName = serverConfig.getString(PUBLISHER_EXPORT_NAME);
    this.exportUrl = serverConfig.getString(PUBLISHER_EXPORT_URL);
    this.domainsDir = serverConfig.getString(PUBLISHER_SPARQL_DOMAINS_DIRECTORY_KEY);
  }

  public String getPublishUrl() {
    return publishUrl;
  }

  public String getQueryUrl() {
    return queryUrl;
  }

  public String getExportPath() {
    return exportName;
  }

  public String getDomainsDir() {
    return domainsDir;
  }

  public String getExportUrl() {
    return exportUrl;
  }

}
