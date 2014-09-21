package edu.isi.wings.portal.servlets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.xerces.util.URI;

import com.google.gson.Gson;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.StorageHandler;
import edu.isi.wings.portal.classes.domains.Domain;

public class HandleUpload extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private int BUF_SIZE = 2 * 1024;
	private Gson gson = new Gson();

	/**
	 * Handle an HTTP POST request from Plupload.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		Config config = new Config(request);
		if(!config.checkDomain(request, response))
			return;
		
		Domain dom = config.getDomain();
		
		String name = null;
		String id = null;
		String storageDir = dom.getDomainDirectory() + "/";
		int chunk = 0;
		int chunks = 0;
		boolean isComponent = false;
		
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (isMultipart) {
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator iter;
			try {
				iter = upload.getItemIterator(request);
				while (iter.hasNext()) {
					FileItemStream item = iter.next();
					try {
						InputStream input = item.openStream();
						if (item.isFormField()) {
							String fieldName = item.getFieldName();
							String value = Streams.asString(input);
							if ("name".equals(fieldName))
								name = value.replaceAll("[^\\w\\.\\-_]+", "_");
							else if ("id".equals(fieldName))
								id = value;
							else if ("type".equals(fieldName)) {
								if("data".equals(value))
									storageDir += dom.getDataLibrary().getStorageDirectory();
								else if("component".equals(value)) {
									storageDir += dom.getConcreteComponentLibrary().getStorageDirectory();
									isComponent = true;
								}
								else {
									storageDir = System.getProperty("java.io.tmpdir"); 
								}
							}
							else if ("chunk".equals(fieldName))
								chunk = Integer.parseInt(value);
							else if ("chunks".equals(fieldName))
								chunks = Integer.parseInt(value);
						} else if (name != null) {
							File storageDirFile = new File(storageDir);
							if (!storageDirFile.exists())
								storageDirFile.mkdirs();
							File uploadFile = new File(storageDirFile.getPath() + "/" + name + ".part");
							saveUploadFile(input, uploadFile, chunk);
						}
					} catch (Exception e) {
						this.printError(out, e.getMessage());
						e.printStackTrace();
					}
				}
			} catch (FileUploadException e1) {
				this.printError(out, e1.getMessage());
				e1.printStackTrace();
			}
		} else {
			this.printError(out, "Not multipart data");
		}
		
		if(chunks == 0 || chunk == chunks - 1) {
			// Done upload
			File partUpload = new File(storageDir + File.separator + name + ".part");
			File finalUpload = new File(storageDir + File.separator + name);
			partUpload.renameTo(finalUpload);
			
      String mime = new Tika().detect(finalUpload);
      if(mime.equals("application/x-sh") || mime.startsWith("text/"))
        FileUtils.writeLines(finalUpload, FileUtils.readLines(finalUpload));

			// Check if this is a zip file and unzip if needed
			String location = finalUpload.getAbsolutePath();
			if(isComponent && mime.equals("application/zip")) {
				String dirname = new URI(id).getFragment();
				location = StorageHandler.unzipFile(finalUpload, dirname, storageDir);
				finalUpload.delete();
			}
			this.printOk(out, location);
		}
	}

	private void printError(PrintWriter out, String msg) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("message", msg);
		map.put("success", false);
		gson.toJson(map, out);
	}

	private void printOk(PrintWriter out, String path) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("success", true);
		map.put("location", path);
		gson.toJson(map, out);
	}

	private void saveUploadFile(InputStream input, File dst, int chunk) throws IOException {
		OutputStream out = null;
		try {
			if (chunk > 0) {
				out = new BufferedOutputStream(new FileOutputStream(dst, true), BUF_SIZE);
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
}
