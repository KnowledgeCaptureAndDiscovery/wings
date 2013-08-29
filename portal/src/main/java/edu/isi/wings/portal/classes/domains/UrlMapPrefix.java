package edu.isi.wings.portal.classes.domains;

public class UrlMapPrefix {
	String url;
	String mapping;

	public UrlMapPrefix(String url, String mapping) {
		this.setUrl(url);
		this.setMapping(mapping);
	}

	public String getUrl() {
		return url.toString();
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMapping() {
		return mapping.toString();
	}

	public void setMapping(String mapping) {
		this.mapping = mapping;
	}
}
