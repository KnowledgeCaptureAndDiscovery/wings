package edu.isi.wings.portal.classes.config;

import java.io.File;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class MainConfig {

  // Comma separated list of spellbook client hosts
  private String clients;
  private String dotFile = "/usr/bin/dot";
  private String serverUrl;

  public MainConfig() {
  }

  public MainConfig(PropertyListConfiguration serverConfig) {
    this.clients = serverConfig.getString(Config.MAIN_CLIENTS_KEY);
    this.dotFile = serverConfig.getString(Config.MAIN_GRAPHVIZ_KEY);
    this.serverUrl = serverConfig.getString(Config.MAIN_SERVER_URL_KEY);
    File localDotFile = new File(this.dotFile);
    File localDotAlternative1 = new File("/usr/local/bin/dot");
    File localDotAlternative2 = new File("/usr/bin/dot");
    if (!localDotFile.exists() && localDotAlternative1.exists()) {
      this.dotFile = localDotAlternative1.getAbsolutePath();
    } else if (!localDotFile.exists() && localDotAlternative2.exists()) {
      this.dotFile = localDotAlternative2.getAbsolutePath();
    }
  }

  public String getClients() {
    return clients;
  }

  public void setClients(String clients) {
    this.clients = clients;
  }

  public String getDotFile() {
    return dotFile;
  }

  public void setDotFile(String dotFile) {
    this.dotFile = dotFile;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }
}
