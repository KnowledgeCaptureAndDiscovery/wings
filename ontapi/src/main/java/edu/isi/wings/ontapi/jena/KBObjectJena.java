/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.isi.wings.ontapi.jena;

import edu.isi.wings.ontapi.KBObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

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
	  KBAPIJena.readLock.lock();
	  try {
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
	  }
	  finally {
	    KBAPIJena.readLock.unlock();
	  }
		return id;
	}

	public String shortForm() {
		return shortForm(false);
	}

	public Object getValue() {
		return this.value;
	}
	
  public String getValueAsString() {
    if(!isLiteral)
      return this.id;
    
    if(this.value instanceof Date) {
      if(this.type == null || this.type.endsWith("#dateTime")) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        return format.format(this.value);
      }
      else if (this.type.endsWith("#date")){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(this.value);
      }
    }
    return (this.value != null ? this.value.toString() : null);
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
	  KBAPIJena.readLock.lock();
	  try {
	    if (res == null)
	      return;
	    this.node = (RDFNode) res;
	    if (node.isLiteral()) {
	      isLiteral = true;
	      this.value = node.asNode().getLiteralValue();
	      this.type = node.asNode().getLiteralDatatypeURI();
	      
	      // Special handling for XSDDateTime
	      if(this.value != null && this.value instanceof XSDDateTime) {
	        this.value = ((XSDDateTime)this.value).asCalendar().getTime();
	      }
	    } else {
	      this.id = ((Resource) node).getURI();
	    }
	  }
	  finally {
	    KBAPIJena.readLock.unlock();
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
	  KBAPIJena.readLock.lock();
	  try {
	    if (node.canAs(RDFList.class)) {
	      RDFList rdfitems = (RDFList) (node.as(RDFList.class));
	      if (rdfitems != null && rdfitems.size() > 0) {
	        return true;
	      }
	    }
	    return false;
	  }
	  finally {
	    KBAPIJena.readLock.unlock();
	  }
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
