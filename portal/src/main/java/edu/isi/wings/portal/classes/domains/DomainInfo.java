package edu.isi.wings.portal.classes.domains;

public class DomainInfo {
	String name;
	String directory;
	String url;
	boolean isLegacy;

	public DomainInfo(Domain dom) {
		this.name = dom.getDomainName();
		this.directory = dom.getDomainDirectory();
		this.url = dom.getDomainUrl();
		this.isLegacy = dom.isLegacy();
	}
	
	public DomainInfo(String name, String directory, String url, boolean isLegacy) {
		super();
		this.name = name;
		this.directory = directory;
		this.url = url;
		this.isLegacy = isLegacy;
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
}