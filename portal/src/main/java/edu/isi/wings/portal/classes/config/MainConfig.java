package edu.isi.wings.portal.classes.config;

import java.io.File;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class MainConfig {

  public static final String MAIN_LIGHT_REASONER_KEY = "light-reasoner";
  public static final String MAIN_CLIENTS_KEY = "clients";
  public static final String MAIN_GRAPHVIZ_KEY = "graphviz";
  public static final String MAIN_METAWORKFLOWS_KEY = "metaworkflows";
  public static final String MAIN_SERVER_KEY = "server";
  public static final String USERS_RELATIVE_DIR = "users";
  public static final String EXPORT_SERVLET_PATH = "/export";

  public String dotFile;
  public String serverUrl;
  public boolean hasMetaWorkflows;
  public String clients;
  public String contextRootPath;
  public String exportCommunityUrl;

  public String communityPath;

  public MainConfig(
    PropertyListConfiguration serverConfig,
    HttpServletRequest request
  ) {
    this.serverUrl = serverConfig.getString(MAIN_SERVER_KEY);
    this.dotFile = serverConfig.getString(MAIN_GRAPHVIZ_KEY);
    this.clients = serverConfig.getString(MAIN_CLIENTS_KEY);
    this.contextRootPath = request.getContextPath();
    if (
      serverConfig.containsKey(MAIN_METAWORKFLOWS_KEY)
    ) this.hasMetaWorkflows = serverConfig.getBoolean(MAIN_METAWORKFLOWS_KEY);
    this.exportCommunityUrl =
      this.serverUrl +
      contextRootPath +
      EXPORT_SERVLET_PATH +
      "/" +
      StorageConfig.COMMUNITY_RELATIVE_DIR;
    this.communityPath =
      contextRootPath +
      "/" +
      USERS_RELATIVE_DIR +
      "/" +
      StorageConfig.COMMUNITY_RELATIVE_DIR;
  }

  public MainConfig(String defaultServer, HttpServletRequest request) {
    this.serverUrl = defaultServer;
    File loc1 = new File("/usr/bin/dot");
    File loc2 = new File("/usr/local/bin/dot");
    dotFile = loc2.exists() ? loc2.getAbsolutePath() : loc1.getAbsolutePath();
    this.contextRootPath = request.getContextPath();
    this.exportCommunityUrl =
      this.serverUrl +
      contextRootPath +
      EXPORT_SERVLET_PATH +
      "/" +
      StorageConfig.COMMUNITY_RELATIVE_DIR;
    this.communityPath =
      contextRootPath +
      "/" +
      USERS_RELATIVE_DIR +
      "/" +
      StorageConfig.COMMUNITY_RELATIVE_DIR;
  }

  public static String getMainLightReasonerKey() {
    return MAIN_LIGHT_REASONER_KEY;
  }

  public static String getMainClientsKey() {
    return MAIN_CLIENTS_KEY;
  }

  public static String getMainGraphvizKey() {
    return MAIN_GRAPHVIZ_KEY;
  }

  public static String getMainMetaworkflowsKey() {
    return MAIN_METAWORKFLOWS_KEY;
  }

  public static String getMainServerKey() {
    return MAIN_SERVER_KEY;
  }

  public String getDotFile() {
    return dotFile;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public boolean hasMetaWorkflows() {
    return hasMetaWorkflows;
  }

  public String getClients() {
    return clients;
  }

  public String getExportCommunityUrl() {
    return exportCommunityUrl;
  }

  public static String getUsersRelativeDir() {
    return USERS_RELATIVE_DIR;
  }

  public static String getExportServletPath() {
    return EXPORT_SERVLET_PATH;
  }

  public boolean isHasMetaWorkflows() {
    return hasMetaWorkflows;
  }

  public String getContextRootPath() {
    return contextRootPath;
  }

  public String getCommunityPath() {
    return communityPath;
  }
}
