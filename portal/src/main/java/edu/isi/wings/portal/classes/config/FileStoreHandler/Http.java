package edu.isi.wings.portal.classes.config.FileStoreHandler;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class Http {
  private static final String PUBLISHER_FILE_STORE_HTTP_URL = "publisher.file-store.http-url";
  private static final String PUBLISHER_FILE_STORE_HTTP_USERNAME = "publisher.file-store.username";
  private static final String PUBLISHER_FILE_STORE_HTTP_PASSWORD = "publisher.file-store.password";
  private static final String PUBLISHER_FILE_STORE_HTTP_MAX_UPLOAD_SIZE = "publisher.file-store.max-upload-size";

  public Http(PropertyListConfiguration serverConfig) {
  }

  public String publishFile(File file) {
    throw new UnsupportedOperationException("Not supported yet.");
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
        if (units.equals("k"))
          return size * kb;
        if (units.equals("m"))
          return size * mb;
        if (units.equals("g"))
          return size * gb;
        if (units.equals("t"))
          return size * tb;
      }
      return size;
    }
    return 0;
  }
  // private String uploadFile(FileStore server, File datafile) {
  // String upUrl = server.getUrl();
  // String username = server.getUsername();
  // String password = server.getPassword();

  // if (username == null || password == null) {
  // return ("missing username or password " +
  // upUrl +
  // " " +
  // username +
  // " " +
  // password);
  // }
  // if (datafile.exists()) {
  // AsyncHttpClient client = Dsl.asyncHttpClient();
  // InputStream inputStream;

  // try {
  // inputStream = new BufferedInputStream(new FileInputStream(datafile));
  // try {
  // org.asynchttpclient.Response response = client
  // .preparePost(upUrl)
  // .setRealm(
  // basicAuthRealm(username, password).setUsePreemptiveAuth(true))
  // .addBodyPart(
  // new InputStreamPart(
  // datafile.getName(),
  // inputStream,
  // datafile.getName(),
  // -1,
  // "application/octet-stream",
  // UTF_8))
  // .execute()
  // .get();
  // return response.getResponseBody();
  // } catch (InterruptedException e) {
  // return null;
  // } catch (ExecutionException e) {
  // return null;
  // }
  // } catch (FileNotFoundException e) {
  // return null;
  // }
  // }
  // return null;
  // }

  // public Response streamData(String dataid, ServletContext context) {
  // String location = dc.getDataLocation(dataid);

  // if (location != null) {
  // // Check if this is a file url
  // File f = null;
  // try {
  // URL url = new URL(location);
  // f = new File(url.getPath());
  // } catch (MalformedURLException e) {
  // // Do nothing
  // }
  // // Else assume it's a file path
  // if (f == null)
  // f = new File(location);

  // return StorageHandler.streamFile(f.getAbsolutePath(), context);
  // }
  // return null;
  // }
}
