package edu.isi.wings.portal.classes.config;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

/**
 * Created by varun on 13/07/2015.
 */
public class Publisher {

  private static final String PUBLISHER_SPARQL_DOMAINS_DIRECTORY_KEY =
    "publisher.triple-store.domains-directory";
  private static final String PUBLISHER_ENDPOINT_QUERY_KEY =
    "publisher.triple-store.query";
  private static final String PUBLISHER_ENDPOINT_POST_KEY =
    "publisher.triple-store.publish";
  private static final String PUBLISHER_NAME_KEY = "publisher.name";
  private static final String PUBLISHER_URL_KEY = "publisher.url";
  String url;
  String tstorePublishUrl;
  String tstoreQueryUrl;
  ServerDetails uploadServer;

  String domainsDir;
  String exportName;

  public Publisher(PropertyListConfiguration serverConfig) {
    this.url = serverConfig.getString(PUBLISHER_URL_KEY);
    this.exportName = serverConfig.getString(PUBLISHER_NAME_KEY);
    this.tstorePublishUrl = serverConfig.getString(PUBLISHER_ENDPOINT_POST_KEY);
    this.tstoreQueryUrl = serverConfig.getString(PUBLISHER_ENDPOINT_QUERY_KEY);
    this.domainsDir =
      serverConfig.getString(PUBLISHER_SPARQL_DOMAINS_DIRECTORY_KEY);
    ServerDetails upserver = new ServerDetails(serverConfig);
    this.setUploadServer(upserver);
  }

  public String getExportName() {
    return exportName;
  }

  public String getUrl() {
    return url;
  }

  public String getTstorePublishUrl() {
    return tstorePublishUrl;
  }

  public String getTstoreQueryUrl() {
    return tstoreQueryUrl;
  }

  public ServerDetails getUploadServer() {
    return uploadServer;
  }

  public String getDomainsDir() {
    return domainsDir;
  }

  public void setUploadServer(ServerDetails uploadServer) {
    this.uploadServer = uploadServer;
  }
}
