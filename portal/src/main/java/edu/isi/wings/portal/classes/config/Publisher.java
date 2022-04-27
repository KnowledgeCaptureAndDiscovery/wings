package edu.isi.wings.portal.classes.config;

/**
 * Created by varun on 13/07/2015.
 */
public class Publisher {
    String url;
    String tstorePublishUrl;
    String tstoreQueryUrl;
    ServerDetails uploadServer;
    String domainsDir;
    String exportName;
    
    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTstorePublishUrl() {
      return tstorePublishUrl;
    }

    public void setTstorePublishUrl(String tstorePublishUrl) {
      this.tstorePublishUrl = tstorePublishUrl;
    }

    public String getTstoreQueryUrl() {
      return tstoreQueryUrl;
    }

    public void setTstoreQueryUrl(String tstoreQueryUrl) {
      this.tstoreQueryUrl = tstoreQueryUrl;
    }

    public ServerDetails getUploadServer() {
        return uploadServer;
    }

    public void setUploadServer(ServerDetails uploadServer) {
        this.uploadServer = uploadServer;
    }

    public String getDomainsDir() {
      return domainsDir;
    }

    public void setDomainsDir(String domainsDir) {
      this.domainsDir = domainsDir;
    }
}
