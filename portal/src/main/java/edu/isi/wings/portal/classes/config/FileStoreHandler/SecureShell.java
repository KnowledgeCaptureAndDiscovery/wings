package edu.isi.wings.portal.classes.config.FileStoreHandler;

import java.io.File;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class SecureShell {
  private static final String PUBLISHER_FILE_STORE_SSH_HOST = "publisher.file-store.host";
  private static final String PUBLISHER_FILE_STORE_SSH_USERID = "publisher.file-store.userid";
  private static final String PUBLISHER_FILE_STORE_SSH_PRIVATE_KEY = "publisher.file-store.private-key";

  public SecureShell(PropertyListConfiguration serverConfig) {
  }

  public String publishFile(File file) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

}
