package edu.isi.wings.portal.classes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;

public class StorageHandler {
	
	public static void streamFile(String location, HttpServletResponse response, ServletContext context) {
		File f = new File(location);
		if(f.canRead()) {
			if(f.isDirectory())
				streamDirectory(f, response, context);
			else
				streamFile(f, response, context);
		}
		else {
			try {
				PrintWriter out = response.getWriter();
				out.println("File not on server\nLocation: "+location);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String unzipFile(File f, String todirname, String toDirectory) {
		File todir = new File(toDirectory);
		if (!todir.exists())
			todir.mkdirs();

		try {
			// Check if the zip file contains only one directory
			ZipFile zfile = new ZipFile(f);
			String topDir = null;
			boolean isOneDir = true;
			for (Enumeration<? extends ZipEntry> e = zfile.entries(); e.hasMoreElements();) {
				ZipEntry ze = e.nextElement();
				String name = ze.getName().replaceAll("/.+$", "");
				name = name.replaceAll("/$", "");
				// OSX Zips carry an extra __MACOSX directory. Ignore it
				if(name.equals("__MACOSX")) 
				  continue;
				
				if(topDir == null)
					topDir = name;
				else if(!topDir.equals(name)) {
					isOneDir = false;
					break;
				}
			}
			zfile.close();
			
			// Delete existing directory (if any)
			FileUtils.deleteDirectory(new File(toDirectory + File.separator + todirname));

			// Unzip file(s) into toDirectory/todirname
			ZipInputStream zis = new ZipInputStream(new FileInputStream(f));
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				String fileName = ze.getName();
        // OSX Zips carry an extra __MACOSX directory. Ignore it
				if(fileName.startsWith("__MACOSX")) {
				  ze = zis.getNextEntry();
				  continue;
				}
				
        // Get relative file path translated to 'todirname'
				if(isOneDir)
					fileName = fileName.replaceFirst(topDir, todirname);
				else
					fileName = todirname + File.separator + fileName;
				
				// Create directories
				File newFile = new File(toDirectory + File.separator + fileName);
				if(ze.isDirectory())
					newFile.mkdirs();
				else
					newFile.getParentFile().mkdirs();
				
				try {
					// Copy file
					FileOutputStream fos = new FileOutputStream(newFile);
					IOUtils.copy(zis, fos);
					fos.close();
					
					String mime = new Tika().detect(newFile);
					if(mime.equals("application/x-sh") || mime.startsWith("text/"))
					  FileUtils.writeLines(newFile, FileUtils.readLines(newFile));

					// Set all files as executable for now
					newFile.setExecutable(true);
				}
				catch (FileNotFoundException fe) {
					// Silently ignore
					//fe.printStackTrace();
				}
				ze = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
			
			return toDirectory + File.separator + todirname;
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*public static void copyDirectory(File src, File dest) throws IOException {
		if(!dest.exists())
			dest.mkdirs();
		EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
		Copy.TreeCopier tc = new Copy.TreeCopier(src.toPath(), dest.toPath());
		Files.walkFileTree(src.toPath(), opts, Integer.MAX_VALUE, tc);
	}*/
    
	private static void streamFile(File f, HttpServletResponse response, 
			ServletContext context) {
		try {
			String mimeType = context.getMimeType(f.getAbsolutePath());
			response.setContentType(mimeType);
			response.setHeader("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
			FileInputStream fin = new FileInputStream(f);
			IOUtils.copyLarge(fin, response.getOutputStream());
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void streamDirectory(File directory, HttpServletResponse response, 
	    ServletContext context) {
	  try {
	    response.setContentType("application/zip");
	    response.setHeader("Content-Disposition",
	        "attachment; filename=\"" + directory.getName() + ".zip\"");

	    // Start the ZipStream reader. Whatever is read is streamed to response
	    PipedInputStream pis = new PipedInputStream(2048);
	    ZipStreamer pipestreamer = new ZipStreamer(pis, response);
	    pipestreamer.start();

	    // Start Zipping folder and piping to the ZipStream reader
	    PipedOutputStream pos = new PipedOutputStream(pis);
	    ZipOutputStream zos = new ZipOutputStream(pos);
	    zipAndStream(directory, zos, directory.getName() + "/");
	    zos.flush();
	    zos.close();

	  } catch (Exception e) {
	    e.printStackTrace();
	  }
	}
    
	private static void zipAndStream(File dir, ZipOutputStream zos, String prefix) 
	    throws Exception {
	  byte bytes[] = new byte[2048];
	  for (File file : dir.listFiles()) {
	    if(file.isDirectory())
	      zipAndStream(file, zos, prefix + file.getName() + "/" );
	    else {
	      FileInputStream fis = new FileInputStream(file.getAbsolutePath());
	      BufferedInputStream bis = new BufferedInputStream(fis);
	      zos.putNextEntry(new ZipEntry(prefix + file.getName()));
	      int bytesRead;
	      while ((bytesRead = bis.read(bytes)) != -1) {
	        zos.write(bytes, 0, bytesRead);
	      }
	      zos.closeEntry();
	      bis.close();
	      fis.close();
	    }
	  }
	}
}

class ZipStreamer extends Thread {
	public PipedInputStream pis;
	public HttpServletResponse response;
	
	public ZipStreamer(PipedInputStream pis, HttpServletResponse response) {
		super();
		this.pis = pis;
		this.response = response;
	}
	
	public void run() {
		try {
			IOUtils.copyLarge(pis, response.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}


/*class Copy { 
    static void copyFile(Path source, Path target) {
        CopyOption[] options = new CopyOption[] { COPY_ATTRIBUTES, REPLACE_EXISTING };
        try {
            Files.copy(source, target, options);
        } catch (IOException x) {
            System.err.format("Unable to copy: %s: %s%n", source, x);
        }
    }
    static class TreeCopier implements FileVisitor<Path> {
        private final Path source;
        private final Path target;
 
        TreeCopier(Path source, Path target) {
            this.source = source;
            this.target = target;
        }
 
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            CopyOption[] options =  new CopyOption[] { COPY_ATTRIBUTES };
            Path newdir = target.resolve(source.relativize(dir));
            try {
                Files.copy(dir, newdir, options);
            } catch (FileAlreadyExistsException x) {
                // ignore
            } catch (IOException x) {
                System.err.format("Unable to create: %s: %s%n", newdir, x);
                return SKIP_SUBTREE;
            }
            return CONTINUE;
        }
 
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            copyFile(file, target.resolve(source.relativize(file)));
            return CONTINUE;
        }
 
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            // fix up modification time of directory when done
            if (exc == null) {
                Path newdir = target.resolve(source.relativize(dir));
                try {
                    FileTime time = Files.getLastModifiedTime(dir);
                    Files.setLastModifiedTime(newdir, time);
                } catch (IOException x) {
                    System.err.format("Unable to copy all attributes to: %s: %s%n", newdir, x);
                }
            }
            return CONTINUE;
        }
 
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            if (exc instanceof FileSystemLoopException) {
                System.err.println("cycle detected: " + file);
            } else {
                System.err.format("Unable to copy: %s: %s%n", file, exc);
            }
            return CONTINUE;
        }
    }
}*/