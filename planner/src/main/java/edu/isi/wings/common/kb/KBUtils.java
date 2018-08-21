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

package edu.isi.wings.common.kb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntFactory;

public class KBUtils {
	public static String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	public static String OWL = "http://www.w3.org/2002/07/owl#";
	public static String XSD = "http://www.w3.org/2001/XMLSchema#";

	// Map urls/url-prefixes to files/directories (remove any existing mappings
	// first)
	public static void createLocationMappings(Properties props, OntFactory fac) {
		for (String prop : props.stringPropertyNames()) {
			if (prop.endsWith(".url")) {
				String mapProp = prop.replaceAll("\\.url$", ".map");
				String map = props.getProperty(mapProp);
				String url = props.getProperty(prop);
				if (map != null) {
					// if this is a prefix mapping
					if (mapProp.endsWith(".dir.map"))
						fac.addAltPrefix(url, map);
					else
						fac.addAltEntry(url, map);
				}
			}
		}
	}

	public static String sanitizeID(String name) {
		name = name.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
		if (name.matches("([0-9]|\\.|\\-).*"))
			name = "a" + name;
		return name;
	}
	
	public static String getLocalName(String id) {
	  if(id == null)
	    return null;
	  return id.replaceAll("^.*#", "");
	}

	public static void removeAllTriplesWith(KBAPI kb, String id, boolean prop) {
		KBObject obj = kb.getResource(id);
		ArrayList<KBTriple> triples = kb.genericTripleQuery(obj, null, null);
		if (prop) {
			obj = kb.getProperty(id);
			triples.addAll(kb.genericTripleQuery(null, obj, null));
		}
		triples.addAll(kb.genericTripleQuery(null, null, obj));
		for (KBTriple triple : triples)
			kb.removeTriple(triple);
	}

	public static void renameAllTriplesWith(KBAPI kb, String oldid, String newid, boolean prop) {
		KBObject oldobj = kb.getResource(oldid);
		KBObject newobj = kb.getResource(newid);
		ArrayList<KBTriple> triples = kb.genericTripleQuery(oldobj, null, null);
		for (KBTriple t : triples) {
			kb.removeTriple(t);
			t.setSubject(newobj);
			kb.addTriple(t);
		}
		if (prop) {
			oldobj = kb.getProperty(oldid);
			newobj = kb.getProperty(newid);
			triples = kb.genericTripleQuery(null, oldobj, null);
			for (KBTriple t : triples) {
				kb.removeTriple(t);
				t.setPredicate(newobj);
				kb.addTriple(t);
			}
		}
		triples = kb.genericTripleQuery(null, null, oldobj);
		for (KBTriple t : triples) {
			kb.removeTriple(t);
			t.setObject(newobj);
			kb.addTriple(t);
		}
	}
	
	public static void renameTripleNamespace(KBAPI kb, String oldns, String newns) {
		ArrayList<KBTriple> triples = kb.getAllTriples();
		for (KBTriple t : triples) {
			kb.removeTriple(t);
			t.setSubject(renameKBObjectNamespace(kb, t.getSubject(), oldns, newns));
			t.setPredicate(renameKBObjectNamespace(kb, t.getPredicate(), oldns, newns));
			t.setObject(renameKBObjectNamespace(kb, t.getObject(), oldns, newns));
			kb.addTriple(t);
		}
	}
	
  public static void renameTripleNamespaces(KBAPI kb, HashMap<String, String> map) {
    ArrayList<KBTriple> triples = kb.getAllTriples();
    for (KBTriple t : triples) {
      kb.removeTriple(t);
      for(String oldns: map.keySet()) {
        String newns = map.get(oldns);
        t.setSubject(renameKBObjectNamespace(kb, t.getSubject(), oldns, newns));
        t.setPredicate(renameKBObjectNamespace(kb, t.getPredicate(), oldns, newns));
        t.setObject(renameKBObjectNamespace(kb, t.getObject(), oldns, newns));
      }
      kb.addTriple(t);
    }
  }	
	
	public static void removeTriplesWithPrefix(KBAPI kb, String prefix) {
	  ArrayList<KBTriple> triples = kb.getAllTriples();
	  for (KBTriple t : triples) {
	    boolean remove = false;
	    if(t.getSubject().getID() != null 
	        && t.getSubject().getID().startsWith(prefix))
	      remove = true;
      if(t.getPredicate().getID() != null 
          && t.getPredicate().getID().startsWith(prefix))
	      remove = true;
      if(t.getObject().getID() != null 
          && t.getObject().getID().startsWith(prefix)) {
        // Also remove all related objects ?
        removeAllTriplesWith(kb, t.getObject().getID(), false);
        remove = true;
      }
	    if(remove)
	      kb.removeTriple(t);
	  }
	}
	
	public static void renameTriplesWithPrefix(KBAPI kb, 
	    String oldPrefix, String newPrefix) {
	  ArrayList<KBTriple> triples = kb.getAllTriples();
	  for (KBTriple t : triples) {
      kb.removeTriple(t);
      t.setSubject(renameKBObjectPrefix(kb, t.getSubject(), oldPrefix, newPrefix));
      t.setPredicate(renameKBObjectPrefix(kb, t.getPredicate(), oldPrefix, newPrefix));
      t.setObject(renameKBObjectPrefix(kb, t.getObject(), oldPrefix, newPrefix));
      kb.addTriple(t);
	  }
	}
	
  public static void renameTriplesWithPrefixes(KBAPI kb, HashMap<String, String> map) {
    ArrayList<KBTriple> triples = kb.getAllTriples();
    for (KBTriple t : triples) {
      kb.removeTriple(t);
      for(String oldPrefix: map.keySet()) {
        String newPrefix = map.get(oldPrefix);      
        t.setSubject(renameKBObjectPrefix(kb, t.getSubject(), oldPrefix, newPrefix));
        t.setPredicate(renameKBObjectPrefix(kb, t.getPredicate(), oldPrefix, newPrefix));
        t.setObject(renameKBObjectPrefix(kb, t.getObject(), oldPrefix, newPrefix));
      }
      kb.addTriple(t);
    }
  }
	 
	private static KBObject renameKBObjectNamespace(KBAPI kb, KBObject item, 
	    String oldns, String newns) {
		if(item.isLiteral())
			return item;
		if(item.isAnonymous())
			return item;
		if(item.getNamespace() == null)
			return item;
		if(item.getNamespace().equals(oldns)) {
			return kb.getResource(newns + item.getName());
		}
		return item;
	}
	
	private static KBObject renameKBObjectPrefix(KBAPI kb, KBObject item, 
	    String oldprefix, String newprefix) {
	  if(item.isLiteral())
	    return item;
	  if(item.isAnonymous())
	    return item;
	  if(item.getNamespace() == null)
	    return item;
	  if(item.getID().startsWith(oldprefix)) {
	    return kb.getResource(item.getID().replace(oldprefix, newprefix));
	  }
	  return item;
	}
}
