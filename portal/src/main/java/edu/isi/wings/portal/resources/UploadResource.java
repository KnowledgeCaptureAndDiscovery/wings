package edu.isi.wings.portal.resources;

import com.google.gson.Gson;
import edu.isi.wings.portal.classes.StorageHandler;
import edu.isi.wings.portal.classes.domains.Domain;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.xerces.util.URI;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("{user}/{domain}/upload")
public class UploadResource extends WingsResource {

  private int BUF_SIZE = 2 * 1024;
  private Gson gson = new Gson();

  @PostConstruct
  public void init() {
    super.init();
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public String uploadFile(
    @FormDataParam("file") InputStream uploadedInputStream,
    @FormDataParam("file") FormDataContentDisposition fileDetail,
    @FormDataParam("id") String id,
    @FormDataParam("name") String name,
    @FormDataParam("type") String type,
    @FormDataParam("chunk") int chunk,
    @FormDataParam("chunks") int chunks
  ) {
    if (!this.hasPermissions()) return errorMessage("No permission");

    if (name == null) name = fileDetail.getFileName();
    // Sanitize name to get an RDF compatible name
    name = name.replaceAll("[^\\w\\.\\-_]+", "_");

    Domain dom = this.config.getDomain();
    boolean isComponent = false;
    String storageDir = dom.getDomainDirectory() + "/";
    if ("data".equals(type)) storageDir +=
    dom.getDataLibrary().getStorageDirectory(); else if (
      "component".equals(type)
    ) {
      storageDir += dom.getConcreteComponentLibrary().getStorageDirectory();
      isComponent = true;
    } else {
      storageDir = System.getProperty("java.io.tmpdir");
    }
    File storageDirFile = new File(storageDir);
    if (!storageDirFile.exists()) storageDirFile.mkdirs();

    try {
      File uploadFile = new File(
        storageDirFile.getPath() + "/" + name + ".part"
      );
      saveUploadFile(uploadedInputStream, uploadFile, chunk);

      if (chunks == 0 || chunk == chunks - 1) {
        // Done upload
        File partUpload = new File(
          storageDir + File.separator + name + ".part"
        );
        File finalUpload = new File(storageDir + File.separator + name);
        partUpload.renameTo(finalUpload);

        String mime = new Tika().detect(finalUpload);
        // Read & rewrite text files ( dos2unix )
        if (
          mime.equals("application/x-sh") || mime.startsWith("text/")
        ) FileUtils.writeLines(finalUpload, FileUtils.readLines(finalUpload));

        // Check if this is a zip file and unzip if needed
        String location = finalUpload.getAbsolutePath();
        if (isComponent && mime.equals("application/zip")) {
          String dirname = new URI(id).getFragment();
          location = StorageHandler.unzipFile(finalUpload, dirname, storageDir);
          finalUpload.delete();
        }
        return this.okMessage(location);
      }
    } catch (Exception e) {
      return this.errorMessage(e.getMessage());
    }
    return null;
  }

  private void saveUploadFile(InputStream input, File dst, int chunk)
    throws IOException {
    OutputStream out = null;
    try {
      if (chunk > 0) {
        out =
          new BufferedOutputStream(new FileOutputStream(dst, true), BUF_SIZE);
      } else {
        out = new BufferedOutputStream(new FileOutputStream(dst), BUF_SIZE);
      }
      byte[] buffer = new byte[BUF_SIZE];
      int len = 0;
      while ((len = input.read(buffer)) > 0) {
        out.write(buffer, 0, len);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (null != input) {
        try {
          input.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (null != out) {
        try {
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private String errorMessage(String msg) {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("message", msg);
    map.put("success", false);
    return gson.toJson(map);
  }

  private String okMessage(String path) {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("success", true);
    map.put("location", path);
    return gson.toJson(map);
  }
}
