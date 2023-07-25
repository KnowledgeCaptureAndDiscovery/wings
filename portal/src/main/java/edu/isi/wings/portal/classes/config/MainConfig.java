package edu.isi.wings.portal.classes.config;

import edu.isi.kcap.wings.opmm_deprecated.Main;
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

  public String dotFile = "/usr/bin/dot";
  public String serverUrl;
  public boolean hasMetaWorkflows = false;
  public String clients = null;
  public String contextRootPath;
  public String exportCommunityUrl;
  public String communityPath;


  public MainConfig(
    PropertyListConfiguration serverConfig,
    HttpServletRequest request
  ) {
    this.contextRootPath = request.getContextPath();
    if (serverConfig.containsKey(MAIN_GRAPHVIZ_KEY)) {
      this.dotFile = serverConfig.getString(MAIN_GRAPHVIZ_KEY);
    }
    if (serverConfig.containsKey(MAIN_SERVER_KEY)) {
      this.serverUrl = serverConfig.getString(MAIN_SERVER_KEY);
    } else {
      this.serverUrl =
        request.getScheme() +
        "://" +
        request.getServerName() +
        ":" +
        request.getServerPort();
    }
    if (serverConfig.containsKey(MAIN_CLIENTS_KEY)) {
      this.clients = serverConfig.getString(MAIN_CLIENTS_KEY);
    }

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
