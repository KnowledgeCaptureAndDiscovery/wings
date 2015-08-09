package edu.isi.wings.portal.classes.config;

import edu.isi.wings.portal.classes.config.ServerDetails;

/**
 * Created by varun on 13/07/2015.
 */
public class Publisher {
    String url;
    String tstoreUrl;
    ServerDetails uploadServer;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTstoreUrl() {
        return tstoreUrl;
    }

    public void setTstoreUrl(String tstoreUrl) {
        this.tstoreUrl = tstoreUrl;
    }

    public ServerDetails getUploadServer() {
        return uploadServer;
    }

    public void setUploadServer(ServerDetails uploadServer) {
        this.uploadServer = uploadServer;
    }
}
