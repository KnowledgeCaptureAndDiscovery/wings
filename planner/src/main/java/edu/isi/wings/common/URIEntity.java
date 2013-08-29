package edu.isi.wings.common;

import java.io.Serializable;
import java.net.URI;

public class URIEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	private URI id;

	public URIEntity(String id) {
		setID(id);
	}

	public String getID() {
		if (id != null)
			return id.toString();
		else
			return null;
	}

	public void setID(String id) {
		try {
			this.id = new URI(id).normalize();
		} catch (Exception e) {
			System.err.println(id + " Not a URI. Only URIs allowed for IDs");
		}
	}
	
	public String getURL() {
		return this.getNamespace().replaceAll("#$", "");
	}

	public String getName() {
		if (id != null)
			return id.getFragment();
		else
			return null;
	}

	public String getNamespace() {
		if (id != null)
			return id.getScheme() + ":" + id.getSchemeSpecificPart() + "#";
		else
			return null;
	}

	public String toString() {
		return getName();
	}

	public int hashCode() {
		return id.hashCode();
	}

}
