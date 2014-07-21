package edu.isi.wings.workflow.plan.classes;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import edu.isi.wings.common.URIEntity;

public class ExecutionFile extends URIEntity {
	private static final long serialVersionUID = 1L;

	String location;
	String bindingId;
	
	transient String metaExtension = ".met";
	Properties metadata;
	long size;
	
	public ExecutionFile(String id) {
		super(id);
		metadata = new Properties();
		size = -1;
	}
	 
  public void loadMetadataFromLocation() {
    File f = new File(location);
    File metaf = new File(location + metaExtension);
    if(f.exists() && f.isFile()) {
      size = f.length();
      if(metaf.exists() && metaf.isFile()) {
        try {
          metadata.load(new FileInputStream(metaf));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getBinding() {
		return bindingId;
	}
	public void setBinding(String bindingId) {
		this.bindingId = bindingId;
	}
  public Properties getMetadata() {
    return metadata;
  }
  public void setMetadata(Properties metadata) {
    this.metadata = metadata;
  }
  public long getSize() {
    return size;
  }
  public void setSize(long size) {
    this.size = size;
  }
}
