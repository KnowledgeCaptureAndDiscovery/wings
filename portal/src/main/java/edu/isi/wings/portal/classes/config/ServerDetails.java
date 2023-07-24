package edu.isi.wings.portal.classes.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

/**
 * Created by varun on 13/07/2015.
 */
public class ServerDetails {

  private static final String PUBLISHER_UPLOAD_SERVER_MAX_UPLOAD_SIZE =
    "publisher.upload-server.max-upload-size";
  private static final String PUBLISHER_UPLOAD_SERVER_PRIVATE_KEY =
    "publisher.upload-server.private-key";
  private static final String PUBLISHER_UPLOAD_SERVER_USERID =
    "publisher.upload-server.userid";
  private static final String PUBLISHER_UPLOAD_SERVER_HOST =
    "publisher.upload-server.host";
  private static final String PUBLISHER_UPLOAD_SERVER_DIRECTORY =
    "publisher.upload-server.directory";
  private static final String PUBLISHER_UPLOAD_SERVER_PASSWORD =
    "publisher.upload-server.password";
  private static final String PUBLISHER_UPLOAD_SERVER_USERNAME =
    "publisher.upload-server.username";
  private static final String PUBLISHER_UPLOAD_SERVER_URL =
    "publisher.upload-server.url";
  String url;
  String directory;
  String host;
  String privateKey;
  String hostUserId;
  String username;
  String password;
  long maxUploadSize = 0; // Defaults to No limit

  public ServerDetails(PropertyListConfiguration serverConfig) {
    this.url = serverConfig.getString(PUBLISHER_UPLOAD_SERVER_URL);
    this.username = serverConfig.getString(PUBLISHER_UPLOAD_SERVER_USERNAME);
    this.password = serverConfig.getString(PUBLISHER_UPLOAD_SERVER_PASSWORD);
    this.directory = serverConfig.getString(PUBLISHER_UPLOAD_SERVER_DIRECTORY);
    this.host = serverConfig.getString(PUBLISHER_UPLOAD_SERVER_HOST);
    this.hostUserId = serverConfig.getString(PUBLISHER_UPLOAD_SERVER_USERID);
    this.privateKey =
      serverConfig.getString(PUBLISHER_UPLOAD_SERVER_PRIVATE_KEY);
    String maxUploadSizeString = serverConfig.getString(
      PUBLISHER_UPLOAD_SERVER_MAX_UPLOAD_SIZE
    );
    if (maxUploadSizeString != null) {
      long size = this.getSizeFromString(maxUploadSizeString);
      this.maxUploadSize = size;
    }
  }

  public String getUrl() {
    return url;
  }

  public String getDirectory() {
    return directory;
  }

  public String getHost() {
    return host;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getHostUserId() {
    return hostUserId;
  }

  public long getMaxUploadSize() {
    return maxUploadSize;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  private long getSizeFromString(String sizeString) {
    long kb = 1024;
    long mb = kb * kb;
    long gb = kb * mb;
    long tb = kb * gb;

    Pattern pat = Pattern.compile("(\\d+)\\s*([KkMmGgTt])[Bb]?");
    Matcher mat = pat.matcher(sizeString);
    if (mat.find()) {
      long size = Long.parseLong(mat.group(1));
      if (mat.groupCount() > 1) {
        String units = mat.group(2).toLowerCase();
        if (units.equals("k")) return size * kb;
        if (units.equals("m")) return size * mb;
        if (units.equals("g")) return size * gb;
        if (units.equals("t")) return size * tb;
      }
      return size;
    }
    return 0;
  }
}
