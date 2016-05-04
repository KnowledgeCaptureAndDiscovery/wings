package edu.isi.wings.portal.classes.config;

/**
 * Created by varun on 13/07/2015.
 */
public class ServerDetails {
    String url;
    String directory;
    String host;
    String privateKey;
    String hostUserId;
    long maxUploadSize = 0; // Defaults to No limit

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getHostUserId() {
      return hostUserId;
    }

    public void setHostUserId(String hostUserId) {
      this.hostUserId = hostUserId;
    }

    public long getMaxUploadSize() {
      return maxUploadSize;
    }

    public void setMaxUploadSize(long maxUploadSize) {
      this.maxUploadSize = maxUploadSize;
    }
}
