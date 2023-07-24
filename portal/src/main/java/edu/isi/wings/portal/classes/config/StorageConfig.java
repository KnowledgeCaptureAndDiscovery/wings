package edu.isi.wings.portal.classes.config;

import java.io.File;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class StorageConfig {

  public static final String STORAGE_DELETE_RUN_OUTPUTS =
    "storage.delete-run-outputs";
  public static final String STORAGE_LOGS = "storage.logs";
  public static final String STORAGE_LOCAL = "storage.local";
  public static final String STORAGE_TDB = "storage.tdb";
  public static final String COMMUNITY_RELATIVE_DIR = "common";
  private static final String TDB_DIRECTORY = "TDB";
  private static final String LOGS_DIRECTORY = "logs";
  public String storageDirectory;
  public String tdbDirectory;
  public String logsDirectory;
  public String communityDirectory;
  public boolean deleteRunOutputs = false;

  public StorageConfig(PropertyListConfiguration serverConfig) {
    if (serverConfig.containsKey(STORAGE_LOCAL)) {
      this.storageDirectory = serverConfig.getString(STORAGE_LOCAL);
    } else {
      this.storageDirectory = createDefaultStorageDirectory();
    }
    this.logsDirectory =
      this.storageDirectory + File.separator + LOGS_DIRECTORY;
    this.logsDirectory = this.storageDirectory + File.separator + TDB_DIRECTORY;
    this.communityDirectory =
      this.storageDirectory + File.separator + COMMUNITY_RELATIVE_DIR;

    if (serverConfig.containsKey(STORAGE_TDB)) {
      this.tdbDirectory = serverConfig.getString(STORAGE_TDB);
    }

    if (serverConfig.containsKey(STORAGE_LOGS)) {
      this.logsDirectory = serverConfig.getString(STORAGE_LOGS);
    }
    if (serverConfig.containsKey(STORAGE_DELETE_RUN_OUTPUTS)) {
      this.deleteRunOutputs =
        serverConfig.getBoolean(STORAGE_DELETE_RUN_OUTPUTS);
    }
    createStorageDirectory(this.communityDirectory);
    createStorageDirectory(this.logsDirectory);
  }

  private String createDefaultStorageDirectory() {
    String parentDirectory = null;
    String home = System.getProperty("user.home");
    if (home != null && !home.equals("")) {
      parentDirectory =
        home + File.separator + ".wings" + File.separator + "storage";
    } else {
      parentDirectory =
        System.getProperty("java.io.tmpdir") +
        File.separator +
        "wings" +
        File.separator +
        "storage";
    }
    createStorageDirectory(parentDirectory);
    return parentDirectory;
  }

  public static void createStorageDirectory(String directory) {
    File uf = new File(directory);
    if (!uf.exists() && !uf.mkdirs()) throw new RuntimeException(
      "Cannot create storage directory: " + directory
    );
  }

  public static String getStorageDeleteRunOutputs() {
    return STORAGE_DELETE_RUN_OUTPUTS;
  }

  public static String getStorageLogs() {
    return STORAGE_LOGS;
  }

  public static String getStorageLocal() {
    return STORAGE_LOCAL;
  }

  public static String getStorageTdb() {
    return STORAGE_TDB;
  }

  public static String getCommunityRelativeDir() {
    return COMMUNITY_RELATIVE_DIR;
  }

  public String getStorageDirectory() {
    return storageDirectory;
  }

  public String getTdbDirectory() {
    return tdbDirectory;
  }

  public String getLogsDirectory() {
    return logsDirectory;
  }

  public String getCommunityDirectory() {
    return communityDirectory;
  }

  public boolean isDeleteRunOutputs() {
    return deleteRunOutputs;
  }
}
