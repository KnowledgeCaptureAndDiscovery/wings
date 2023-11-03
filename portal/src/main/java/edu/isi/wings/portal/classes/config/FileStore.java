package edu.isi.wings.portal.classes.config;

import edu.isi.wings.portal.classes.config.FileStoreHandler.FileSystem;
import edu.isi.wings.portal.classes.config.FileStoreHandler.Http;
import edu.isi.wings.portal.classes.config.FileStoreHandler.SecureShell;

import java.io.File;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

/**
 * Created by varun on 13/07/2015.
 */
public class FileStore {
  private static final String PUBLISHER_FILE_STORE_TYPE = "publisher.file-store.type";

  public static enum Type {
    SSH,
    HTTP,
    FILE_SYSTEM,
  }

  FileStore.Type type;
  FileSystem fileSystem;
  SecureShell ssh;
  Http http;

  public FileStore.Type getType() {
    return type;
  }

  public FileSystem getFileSystem() {
    return fileSystem;
  }

  public SecureShell getSsh() {
    return ssh;
  }

  public Http getHttp() {
    return http;
  }

  public FileStore(PropertyListConfiguration serverConfig) {
    this.type = FileStore.Type.valueOf(serverConfig.getString(PUBLISHER_FILE_STORE_TYPE));
    switch (this.type) {
      case FILE_SYSTEM:
        this.fileSystem = new FileSystem(serverConfig);
        break;
      case SSH:
        this.ssh = new SecureShell(serverConfig);
        break;
      case HTTP:
        this.http = new Http(serverConfig);
        break;
    }
  }

  public String publishFile(File file) throws Exception {
    switch (this.type) {
      case FILE_SYSTEM:
        return this.fileSystem.publishFile(file);
      case SSH:
        return this.ssh.publishFile(file);
      case HTTP:
        return this.http.publishFile(file);
      default:
        throw new Exception("Unknown file store type: " + this.type);
    }
  }
}
