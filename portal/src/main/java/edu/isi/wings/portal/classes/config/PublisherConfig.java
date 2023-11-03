package edu.isi.wings.portal.classes.config;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class PublisherConfig {
  FileStore fileStore;
  TripleStoreConfig tripleStore;

  public PublisherConfig(PropertyListConfiguration serverConfig) {
    this.tripleStore = new TripleStoreConfig(serverConfig);
    this.fileStore = new FileStore(serverConfig);
  }

  public FileStore getFileStore() {
    return fileStore;
  }

  public TripleStoreConfig getTripleStore() {
    return tripleStore;
  }

}
