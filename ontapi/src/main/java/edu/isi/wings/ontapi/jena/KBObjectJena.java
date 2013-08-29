package edu.isi.wings.ontapi.jena;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.isi.wings.ontapi.KBObject;

public class KBObjectJena implements KBObject {
	String id;

	Object value;
	String type;

	transient RDFNode node;

	boolean isLiteral;

	public KBObjectJena(String id) {
		this.id = id;
	}

	public KBObjectJena(Object value, boolean dummy) {
		this.value = value;
		this.isLiteral = true;
	}

	public KBObjectJena(RDFNode node) {
		this.node = node;
		setInternalNode(node);
	}

	public String getID() {
		return id;
	}

	public String getNamespace() {
		if (node != null && node.isResource()) {
			return ((Resource) node).getNameSpace();
		}
		return null;
	}

	public String getName() {
		if (node != null && node.isResource()) {
			return ((Resource) node).getLocalName();
		}
		return null;
	}

	public String shortForm(boolean showLiteralTypes) {
		if (node != null && node.isResource()) {
			Resource resource = (Resource) node;
			Model model = resource.getModel();
			try {
				String name = model.shortForm(resource.getURI());
				if (name.startsWith(":"))
					name = name.substring(1);
				return name;
			} catch (Exception e) {
				return resource.toString();
			}
		} else if (isLiteral()) {
			String str = this.value.toString();
			if (node != null && showLiteralTypes)
				str = node.toString();
			return str;
		}
		return id;
	}

	public String shortForm() {
		return shortForm(false);
	}

	public Object getValue() {
		return this.value;
	}

	public String getDataType() {
		return this.type;
	}
	
	public void setDataType(String type) {
		this.type = type;
	}

	public Object getInternalNode() {
		return node;
	}

	public void setInternalNode(Object res) {
		if (res == null)
			return;
		this.node = (RDFNode) res;
		if (node.isLiteral()) {
			isLiteral = true;
			this.value = node.asNode().getLiteralValue();
			this.type = node.asNode().getLiteralDatatypeURI();
		} else {
			this.id = ((Resource) node).getURI();
		}
	}

	public boolean isLiteral() {
		return this.isLiteral;
	}
	
	public boolean isAnonymous() {
		return (this.node != null && this.node.isAnon()); 
	}

	public boolean isNothing() {
		return id.equals(OWL.Nothing.getURI());
	}

	public boolean isThing() {
		return id.equals(OWL.Thing.getURI());
	}

	public boolean isClassificationProperty() {
		return id.equals(RDF.type.getURI());
	}

	public String toString() {
		String str = "";
		if (isLiteral && value != null) {
			str += value.toString();
		} else {
			str = id;
		}
		return str;
	}

	public boolean isList() {
		if (node.canAs(RDFList.class)) {
			RDFList rdfitems = (RDFList) (node.as(RDFList.class));
			if (rdfitems != null && rdfitems.size() > 0) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof KBObjectJena) {
			KBObjectJena objJena = (KBObjectJena) obj;
			if (isLiteral) {
				if (objJena.isLiteral) {
					if (type.equals(objJena.type)
							&& value.toString().equals(objJena.value.toString())) {
						return true;
					}
				}
			} else {
				if (!objJena.isLiteral) {
					if (id.equals(objJena.id)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int factor = 37;
		// Begin with a prime number (following all recommendations)
		int output = 17;
		if (isLiteral) {
			if (type != null)
				output += factor * type.hashCode();
			if (value != null)
				output += factor * value.hashCode();
		} else {
			if (id != null)
				output += factor * id.hashCode();
		}
		return output;
	}
}
