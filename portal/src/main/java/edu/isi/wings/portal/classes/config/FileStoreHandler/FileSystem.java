package edu.isi.wings.portal.classes.config.FileStoreHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.io.FileUtils;

public class FileSystem {
  private static final String PUBLISHER_FILE_STORE_FILE_SYSTEM_DIRECTORY = "publisher.file-store.file-system.directory";
  private String directory;

  public FileSystem(PropertyListConfiguration serverConfig) {
    this.directory = serverConfig.getString(PUBLISHER_FILE_STORE_FILE_SYSTEM_DIRECTORY);
  }

  public String publishFile(File file) throws IOException {
    String path = this.directory;
    File directory = new File(path);
    if (!directory.exists()) {
      directory.mkdirs();
    }
    File newFile = new File(path + "/" + file.getName());
    FileUtils.copyFile(file, newFile);
    // return relative path
    Path path1 = Paths.get(directory.getAbsolutePath());
    Path path2 = Paths.get(newFile.getAbsolutePath());
    return path1.relativize(path2).toString();
  }

}
