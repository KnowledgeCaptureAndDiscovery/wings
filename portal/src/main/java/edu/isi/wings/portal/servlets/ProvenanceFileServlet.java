package edu.isi.wings.portal.servlets;

import edu.isi.wings.portal.classes.config.Config;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.config.ConfigLoader;

public class ProvenanceFileServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    ConfigLoader configLoader = new ConfigLoader(request, null, null);
    // TODO: replace the following with
    // configLoader.portalConfig.getProvenanceDirectory();
    String provenanceDirectoryPath = "/tmp" + File.separator + "wings" + File.separator + "provenance";
    File provenanceDirectory = new File(provenanceDirectoryPath);

    String requestFile = request.getPathInfo();
    String filePath = provenanceDirectoryPath + File.separator + requestFile;
    System.out.println("FileServlet: " + filePath);

    File file = new File(filePath);
    if (!file.exists()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
      return;
    }
    // check if filepath is in allowed directory
    if (!isInsideDirectory(file, provenanceDirectory)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    // Set content type and headers for the response
    response.setContentType("application/octet-stream");
    response.setHeader(
        "Content-Disposition",
        "attachment; filename=\"" + file.getName() + "\"");

    // Read the file and write its contents to the response output stream
    try (
        FileInputStream fis = new FileInputStream(filePath);
        OutputStream out = response.getOutputStream()) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      // Handle exception
      e.printStackTrace();
    }
  }

  private static boolean isInsideDirectory(File file, File directory) {
    File parent = file.getParentFile();
    if (parent == null) {
      return false;
    } else if (parent.equals(directory)) {
      return true;
    } else {
      return isInsideDirectory(parent, directory);
    }
  }
}
