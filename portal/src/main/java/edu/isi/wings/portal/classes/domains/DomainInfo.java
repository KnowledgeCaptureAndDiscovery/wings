package edu.isi.wings.portal.classes.domains;

import java.util.ArrayList;

public class DomainInfo {
	String name;
	String engine;
	String directory;
	String url;
	boolean isLegacy;
	ArrayList<Permission> permissions;

	public DomainInfo(Domain dom) {
		this.name = dom.getDomainName();
		this.directory = dom.getDomainDirectory();
		this.url = dom.getDomainUrl();
		this.isLegacy = dom.isLegacy();
		this.engine = dom.getPlanEngine();
		this.permissions = dom.getPermissions();
	}
	
	public DomainInfo(String name, String engine, 
	    String directory, String url, boolean isLegacy, 
	    ArrayList<Permission> permissions) {
		super();
		this.name = name;
		this.engine = engine;
		this.directory = directory;
		this.url = url;
		this.isLegacy = isLegacy;
		this.permissions = permissions;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isLegacy() {
		return isLegacy;
	}

	public void setLegacy(boolean isLegacy) {
		this.isLegacy = isLegacy;
	}

  public String getEngine() {
    return engine;
  }

  public void setEngine(String engine) {
    this.engine = engine;
  }
}