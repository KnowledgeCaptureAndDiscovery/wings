package edu.isi.wings.portal.classes.domains;

public class DomainLibrary extends UrlMapPrefix {
	String name;
	String storageDirectory;
	
	public DomainLibrary(String url, String mapping) {
		super(url,  mapping);
	}
	
	public DomainLibrary(String url, String mapping, String name, String storagedir) {
		super(url,  mapping);
		this.name = name;
		this.storageDirectory = storagedir;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStorageDirectory() {
		return storageDirectory;
	}

	public void setStorageDirectory(String storageDirectory) {
		this.storageDirectory = storageDirectory;
	}

}
