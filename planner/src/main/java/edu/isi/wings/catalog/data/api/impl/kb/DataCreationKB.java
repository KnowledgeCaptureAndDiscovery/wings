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

package edu.isi.wings.catalog.data.api.impl.kb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.data.classes.DataItem;
import edu.isi.wings.catalog.data.classes.DataTree;
import edu.isi.wings.catalog.data.classes.DataTreeNode;
import edu.isi.wings.catalog.data.classes.MetadataProperty;
import edu.isi.wings.catalog.data.classes.MetadataValue;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.SparqlQuerySolution;

public class DataCreationKB extends DataKB implements DataCreationAPI {
	String topclass;
	String topmetric;
	
	KBAPI tmpkb;
	
	DataCreationAPI externalCatalog;

	public DataCreationKB(Properties props) {
		super(props, true, true);
		// FIXME: Hack to get unionOf domains
		try {
		  this.tmpkb = this.ontologyFactory.getKB(onturl, OntSpec.MICRO);
		}
		catch (Exception e) {
		  e.printStackTrace();
		}
		
		this.topclass = this.dcns + "DataObject";
		this.topmetric = this.dcns + "Metrics";

		String extern = props.getProperty("extern_data_catalog");
		if(extern != null) {
			try {
				Class<?> classz = Class.forName(extern);
				DataCreationAPI externalDC = 
						(DataCreationAPI) classz.getDeclaredConstructor(Properties.class).newInstance(props);
				this.setExternalCatalog(externalDC);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Legacy porting: Fix all properties that have multiple domains
		// -- convert to disjunctive domains FIXME
		this.convertPropertyDomainsToDisjunctiveDomains();
	}
	
	@Override
	public boolean save() {
	  return this.save(libkb) && this.save(ontkb);
	}

	@Override
	public DataTree getDataHierarchy() {
		return this.createHierarchy(this.topclass, false);
	}
	
	@Override
	public DataTree getDatatypeHierarchy() {
		return this.createHierarchy(this.topclass, true);
	}

	@Override
	public DataTree getMetricsHierarchy() {
		return this.createHierarchy(this.topmetric, false);
	}

	@Override
	public ArrayList<String> getAllDatatypeIds() {
		ArrayList<String> list = new ArrayList<String>();
    String query =   
        "SELECT ?type\n" + 
        "WHERE {\n" + 
        "?type a <"+KBUtils.OWL+"Class> .\n" + 
        "FILTER ( STRSTARTS(STR(?type), \"" + this.dcdomns + "\"))\n" + 
        "}";
    
    this.start_read();
    ArrayList<ArrayList<SparqlQuerySolution>> result = this.kb.sparqlQuery(query);
    for(ArrayList<SparqlQuerySolution> row : result) {
      HashMap<String, KBObject> vals = new HashMap<String, KBObject>();
      for(SparqlQuerySolution col : row)
        vals.put(col.getVariable(), col.getObject());
      if(vals.get("type") == null)
        continue;
      String typeid = vals.get("type").getID();
      list.add(typeid);
    }
    this.end();
    return list;		
	}
	
	@Override
	public HashMap<String, ArrayList<String>> getAllDatatypeDatasets()	{
	  HashMap<String, ArrayList<String>> typedata = new HashMap<String, ArrayList<String>>();
    String query =   
        "SELECT ?s ?type\n" + 
        "WHERE {\n" + 
        "?s a ?type .\n" + 
        "FILTER ( STRSTARTS(STR(?type), \"" + this.dcdomns + "\"))\n" + 
        "}";
    
    this.start_read();
    ArrayList<ArrayList<SparqlQuerySolution>> result = this.libkb.sparqlQuery(query);
    for(ArrayList<SparqlQuerySolution> row : result) {
      HashMap<String, KBObject> vals = new HashMap<String, KBObject>();
      for(SparqlQuerySolution col : row)
        vals.put(col.getVariable(), col.getObject());
      if(vals.get("type") == null)
        continue;
      String typeid = vals.get("type").getID();
      String instid = vals.get("s").getID();
      if(!typedata.containsKey(typeid))
        typedata.put(typeid, new ArrayList<String>());
      typedata.get(typeid).add(instid);
    }
    this.end();
	  return typedata;
	}

	@Override
	public ArrayList<MetadataProperty> getAllMetadataProperties() {
	  this.start_read();
		KBObject mprop = this.kb.getProperty(this.dcns + "hasMetrics");
		KBObject dmprop = this.kb.getProperty(this.dcns + "hasDataMetrics");
		ArrayList<KBObject> properties = this.kb.getSubPropertiesOf(mprop, false);
		properties.addAll(this.kb.getSubPropertiesOf(dmprop, false));
		ArrayList<MetadataProperty> mprops = createMetadataProperties(properties);
    this.end();
    return mprops;	}

	@Override
	public ArrayList<MetadataProperty> getMetadataProperties(String dtypeid, boolean direct) {
	  this.start_read();
		KBObject datatype = this.tmpkb.getConcept(dtypeid);
		ArrayList<KBObject> properties = this.tmpkb.getPropertiesOfClass(datatype, direct);
		ArrayList<MetadataProperty> mprops = createMetadataProperties(properties);
		this.end();
		return mprops;
	}

	@Override
	public DataItem getDatatypeForData(String dataid) {
	  try {
  	  this.start_read();
  		KBObject data = this.kb.getIndividual(dataid);
  		if(data == null)
  		  return null;
  		KBObject cls = this.kb.getClassOfInstance(data);
  		if (cls != null)
  			return new DataItem(cls.getID(), DataItem.DATATYPE);
  		return null;
	  }
	  finally {
	    this.end();
	  }
	}

	@Override
	public ArrayList<DataItem> getDataForDatatype(String dtypeid, boolean direct) {
	  try {
  	  this.start_read();
  		KBObject datatype = this.kb.getConcept(dtypeid);
  		ArrayList<KBObject> datas = this.kb.getInstancesOfClass(datatype, direct);
  		ArrayList<DataItem> list = new ArrayList<DataItem>();
  		for (KBObject data : datas) {
  			list.add(new DataItem(data.getID(), DataItem.DATA));
  		}
  		if(!direct && datatype != null) {
  		  for(KBObject cls : this.getSubClasses(datatype))
  		    list.addAll(this.getDataForDatatype(cls.getID(), direct));
  		}
  		return list;
	  }
	  finally {
	    this.end();
	  }
	}

	@Override
	public String getTypeNameFormat(String dtypeid) {
	  try {
  	  this.start_read();
  		KBObject datatype = this.kb.getConcept(dtypeid);
  		Pattern pat = Pattern.compile("^NameFormat=(.+)$");
  		for (String comment : this.kb.getAllComments(datatype)) {
  			Matcher m = pat.matcher(comment);
  			if (m.find()) {
  				return m.group(1);
  			}
  		}
  		return null;
	  }
	  finally {
	    this.end();
	  }
	}

	@Override
	public ArrayList<MetadataValue> getMetadataValues(String dataid, ArrayList<String> propids) {
	  this.start_read();
		KBObject data = this.kb.getIndividual(dataid);
		ArrayList<MetadataValue> values = new ArrayList<MetadataValue>();
		for (String propid : propids) {
			KBObject prop = this.kb.getProperty(propid);
			ArrayList<KBObject> vals = this.kb.getPropertyValues(data, prop);
			for (KBObject val : vals) {
				if (val.isLiteral())
					values.add(new MetadataValue(propid, val.getValue(), MetadataValue.DATATYPE));
				else
					values.add(new MetadataValue(propid, val.getID(), MetadataValue.OBJECT));
			}
		}
		this.end();
		return values;
	}

	@Override
	public MetadataProperty getMetadataProperty(String propid) {
	  try {
  	  this.start_read();
  		if (!this.kb.containsResource(propid))
  			return null;
  		KBObject property = this.kb.getProperty(propid);
  		MetadataProperty mprop = this.createMetadataProperty(property);
  		return mprop;
	  }
	  finally {
	    this.end();
	  }
	}

	@Override
	public boolean addDatatype(String dtypeid, String parentid) {
	  this.start_write();
		KBObject dtype = this.ontkb.createClass(dtypeid, parentid);
		if(this.externalCatalog != null)
			this.externalCatalog.addDatatype(dtypeid, parentid);
		return this.save() && this.end() && (dtype != null);
	}

	@Override
	public boolean removeDatatype(String dtypeid) {
	  this.start_read();
		KBObject cls = this.kb.getConcept(dtypeid);
    // Get all subclasses
    ArrayList<KBObject> subclses = this.getSubClasses(cls);		
		// Get all files
		ArrayList<KBObject> files = this.kb.getInstancesOfClass(cls, true);
		// Get metadata properties
		ArrayList<KBObject> props = this.kb.getPropertiesOfClass(cls, false);
		this.end();
		
		this.start_write();
		// Suspend further internal transactions (i.e. Batch write)
		this.start_batch_operation(); 
		
    // Remove properties
    for (KBObject prop : props) {
      MetadataProperty mprop = this.getMetadataProperty(prop.getID());
      this.start_write();
      if(mprop.getDomains().contains(dtypeid)) {
        if(mprop.getDomains().size() > 1)
          this.removeMetadataPropertyDomain(prop.getID(), dtypeid);
        else
          this.removeMetadataProperty(prop.getID());
      }
    }

		// Remove files
    for (KBObject file : files) {
      this.removeData(file.getID());
    }		
    // Remove subclasses
		for (KBObject subcls : subclses) {
			if (!subcls.isNothing())
				this.removeDatatype(subcls.getID());
		}
		// Finally remove the class itself
		KBUtils.removeAllTriplesWith(this.ontkb, dtypeid, false);

		if(this.externalCatalog != null)
			this.externalCatalog.removeDatatype(dtypeid);
		
		// Resume transactions
		this.stop_batch_operation(); 		
		
		return this.save() && this.end();
	}

	@Override
	public boolean renameDatatype(String newtypeid, String oldtypeid) {
	  this.start_write();
		KBUtils.renameAllTriplesWith(this.ontkb, oldtypeid, newtypeid, false);
		if(this.externalCatalog != null)
			this.externalCatalog.renameDatatype(newtypeid, oldtypeid);
		return this.save() && this.end();
	}

  @Override
  public boolean renameDatatypeInLibrary(String newtypeid, String oldtypeid) {
    this.start_write();
    KBUtils.renameAllTriplesWith(this.libkb, oldtypeid, newtypeid, false);
    if(this.externalCatalog != null)
      this.externalCatalog.renameDatatypeInLibrary(newtypeid, oldtypeid);
    return this.save() && this.end();
  }
  
  @Override
  public boolean moveDatatypeParentInLibrary(String dtypeid, String fromtypeid, String totypeid) {
    this.start_write();
    KBObject cls = this.kb.getConcept(dtypeid);
    KBObject fromcls = this.kb.getConcept(fromtypeid);
    KBObject tocls = this.kb.getConcept(totypeid);
    ArrayList<KBObject> oldprops = this.kb.getPropertiesOfClass(fromcls, false);
    ArrayList<KBObject> newprops = this.kb.getPropertiesOfClass(tocls, false);
    ArrayList<KBObject> removedProps = new ArrayList<KBObject>(); 
    for(KBObject oldprop : oldprops) {
      if(!newprops.contains(oldprop)) {
        removedProps.add(oldprop);
      }
    }
    for(KBObject ind : this.kb.getInstancesOfClass(cls, false)) {
      for(KBObject prop : removedProps) {
        for(KBTriple triple : this.kb.genericTripleQuery(ind, prop, null)) 
          this.libkb.removeTriple(triple);
      }
    }
    if(this.externalCatalog != null)
      this.externalCatalog.moveDatatypeParentInLibrary(dtypeid, fromtypeid, totypeid);  
    
    return this.save() && this.end();
  }
  
	@Override
	public boolean moveDatatypeParent(String dtypeid, String fromtypeid, String totypeid) {
	  this.start_write();
		if(!this.ontkb.setSuperClass(dtypeid, totypeid))
			return false;

		if(this.externalCatalog != null)
			this.externalCatalog.moveDatatypeParent(dtypeid, fromtypeid, totypeid);
		return this.save() && this.end();
	}

	@Override
	public boolean moveDataParent(String dataid, String fromtypeid, String totypeid) {
	  KBObject obj = this.kb.getIndividual(dataid);
	  KBObject fromcls = this.kb.getConcept(fromtypeid);
	  KBObject tocls = this.kb.getConcept(totypeid);
	  ArrayList<KBObject> oldprops = this.kb.getPropertiesOfClass(fromcls, false);
	  ArrayList<KBObject> newprops = this.kb.getPropertiesOfClass(tocls, false);
	  ArrayList<KBObject> removedProps = new ArrayList<KBObject>(); 
	  for(KBObject oldprop : oldprops) {
	    if(!newprops.contains(oldprop)) {
	      removedProps.add(oldprop);
	    }
	  }
	  for(KBObject prop : removedProps) {
	    for(KBTriple triple : this.kb.genericTripleQuery(obj, prop, null)) 
	      this.libkb.removeTriple(triple);
	  }

	  KBObject typeProp = this.kb.getProperty(KBUtils.RDF+"type");
	  this.libkb.removeTriple(obj, typeProp, fromcls);
	  this.libkb.addTriple(obj, typeProp, tocls);
	  
	  if(this.externalCatalog != null)
	    this.externalCatalog.moveDataParent(dataid, fromtypeid, totypeid);
	  return true;
	}
	 
	@Override
	public boolean addData(String dataid, String dtypeid) {
	  this.start_write();
		KBObject dtypeobj = this.kb.getConcept(dtypeid);
		this.libkb.createObjectOfClass(dataid, dtypeobj);
		if(this.externalCatalog != null)
			this.externalCatalog.addData(dataid, dtypeid);
		return this.save() && this.end(); 
	}

	@Override
	public boolean renameData(String newdataid, String olddataid) {
    this.start_write();	  
		KBUtils.renameAllTriplesWith(this.libkb, olddataid, newdataid, false);
		if(this.externalCatalog != null)
			this.externalCatalog.renameData(newdataid, olddataid);
		return this.save() && this.end();
	}

	@Override
	public boolean removeData(String dataid) {
		// Remove data if it is in the catalog's data directory
		String loc = this.getDataLocation(dataid);
		if(loc != null) {
			File f = new File(loc);
			if(f.getParentFile().getAbsolutePath().equals(this.datadir))
				f.delete();
		}
		this.start_write();		
		KBUtils.removeAllTriplesWith(this.libkb, dataid, false);
		if(this.externalCatalog != null)
			this.externalCatalog.removeData(dataid);
		return this.save() && this.end();
	}

	@Override
	public boolean setDataLocation(String dataid, String locuri) {
		// What happens to existing file ?
	  this.start_write();  
		KBObject locprop = this.kb.getProperty(this.dcns + "hasLocation");
		KBObject dobj = this.libkb.getIndividual(dataid);
		KBObject locobj = this.libkb.createLiteral(locuri);
		this.libkb.setPropertyValue(dobj, locprop, locobj);
		if(this.externalCatalog != null)
			this.externalCatalog.setDataLocation(dataid, locuri);
		return this.save() && this.end();
	}

	@Override
	public boolean setTypeNameFormat(String dtypeid, String format) {
	  this.start_write();
		KBObject dtypeobj = this.ontkb.getConcept(dtypeid);
		this.ontkb.setComment(dtypeobj, "NameFormat=" + format);
		if(this.externalCatalog != null)
			this.externalCatalog.setTypeNameFormat(dtypeid, format);
		return this.save() && this.end();
	}

	@Override
	public boolean addDatatypePropertyValue(String dataid, String propid, Object val) {
	  this.start_write();
		KBObject dataobj = this.libkb.getIndividual(dataid);
		KBObject pobj = this.kb.getProperty(propid);
		KBObject valobj = this.libkb.createLiteral(val);
		this.libkb.setPropertyValue(dataobj, pobj, valobj);
		if(this.externalCatalog != null)
			this.externalCatalog.addDatatypePropertyValue(dataid, propid, val);
		return this.save() && this.end();
	}

	@Override
	public boolean addDatatypePropertyValue(String dataid, String propid, String val, String xsdtype) {
	  this.start_write();
		KBObject dataobj = this.libkb.getIndividual(dataid);
		KBObject pobj = this.kb.getProperty(propid);
		KBObject valobj = this.kb.createXSDLiteral(val, xsdtype);
		this.libkb.setPropertyValue(dataobj, pobj, valobj);
		if(this.externalCatalog != null)
			this.externalCatalog.addDatatypePropertyValue(dataid, propid, val, xsdtype);
		return this.save() && this.end();
	}

	@Override
	public boolean addObjectPropertyValue(String dataid, String propid, String valid) {
	  this.start_write();
		KBObject dataobj = this.libkb.getIndividual(dataid);
		KBObject pobj = this.kb.getProperty(propid);
		KBObject valobj = this.kb.getResource(valid);
		this.libkb.setPropertyValue(dataobj, pobj, valobj);
		if(this.externalCatalog != null)
			this.externalCatalog.addObjectPropertyValue(dataid, propid, valid);
		return this.save() && this.end();
	}

	@Override
	public boolean removePropertyValue(String dataid, String propid, Object val) {
	  this.start_write();
		KBObject dataobj = this.libkb.getIndividual(dataid);
		KBObject pobj = this.kb.getProperty(propid);
		KBObject valobj = this.libkb.createLiteral(val);
		this.libkb.removeTriple(dataobj, pobj, valobj);
		if(this.externalCatalog != null)
			this.externalCatalog.removePropertyValue(dataid, propid, val);
		return this.save() && this.end();
	}

	@Override
	public boolean removeAllPropertyValues(String dataid, ArrayList<String> propids) {
	  this.start_write();
		KBObject dataobj = this.libkb.getIndividual(dataid);
		for (String propid : propids) {
			KBObject pobj = this.kb.getProperty(propid);
			ArrayList<KBObject> vals = this.kb.getPropertyValues(dataobj, pobj);
			for (KBObject val : vals) {
				this.libkb.removeTriple(dataobj, pobj, val);
			}
		}
		if(this.externalCatalog != null)
			this.externalCatalog.removeAllPropertyValues(dataid, propids);
		return this.save() && this.end();
	}

	@Override
	public boolean addMetadataProperty(String propid, String domain, String range) {
	  this.start_write();
		if (range.contains(KBUtils.XSD)) {
			this.ontkb.createDatatypeProperty(propid, this.dcns + "hasDataMetrics");
		} else {
			this.ontkb.createObjectProperty(propid, this.dcns + "hasMetrics");
		}		
		if(this.ontkb.getConcept(domain) == null)
      this.ontkb.createClass(domain);
		
		this.ontkb.addPropertyDomainDisjunctive(propid, domain);
		this.ontkb.setPropertyRange(propid, range);
		if(this.externalCatalog != null)
			this.externalCatalog.addMetadataProperty(propid, domain, range);
		return this.save() && this.end();
	}

	@Override
	public boolean addMetadataPropertyDomain(String propid, String domain) {
	  this.start_write();
	  if(this.ontkb.getConcept(domain) == null)
	    this.ontkb.createClass(domain);
		this.ontkb.addPropertyDomainDisjunctive(propid, domain);
		if(this.externalCatalog != null)
			this.externalCatalog.addMetadataPropertyDomain(propid, domain);
		return this.save() && this.end();
	}
	
	@Override
	public boolean removeMetadataPropertyDomain(String propid, String domain) {
	  this.start_write();
		this.ontkb.removePropertyDomainDisjunctive(propid, domain);
		if(this.externalCatalog != null)
			this.externalCatalog.removeMetadataPropertyDomain(propid, domain);
		return this.save() && this.end();
	}
	
	@Override
	public boolean removeMetadataProperty(String propid) {
		// Remove all domains manually
		// - Due to bug in removing triples with union classes
	  this.start_write();
		MetadataProperty prop = this.getMetadataProperty(propid);
		for(String domid : prop.getDomains())
			this.ontkb.removePropertyDomainDisjunctive(propid, domid);
		
		// Remove all triples (this skips removing domain union classes)
		KBUtils.removeAllTriplesWith(this.ontkb, propid, true);
		if(this.externalCatalog != null)
			this.externalCatalog.removeMetadataProperty(propid);
		return this.save() && this.end();
	}

  @Override
  public boolean removeMetadataPropertyInLibrary(String propid) {
    this.start_write();
    KBUtils.removeAllTriplesWith(this.libkb, propid, true);
    if(this.externalCatalog != null)
      this.externalCatalog.removeMetadataPropertyInLibrary(propid);
    return this.save() && this.end();
  }
  
	@Override
	public boolean renameMetadataProperty(String oldid, String newid) {
		// First remove all domains and then readd them later
		// - Due to bug in renaming triples
	  this.start_write();
		MetadataProperty prop = this.getMetadataProperty(oldid);
		for(String domid : prop.getDomains())
			this.ontkb.removePropertyDomainDisjunctive(oldid, domid);
		
		// Rename all triples (this skips renaming domain union classes)
		KBUtils.renameAllTriplesWith(this.ontkb, oldid, newid, true);
		
		for(String domid : prop.getDomains())
			this.ontkb.addPropertyDomainDisjunctive(newid, domid);
		if(this.externalCatalog != null)
			this.externalCatalog.renameMetadataProperty(oldid, newid);
		return this.save() && this.end();
	}
	
  @Override
  public boolean renamePropertyInLibrary(String oldid, String newid) {
    this.start_write();
    KBUtils.renameAllTriplesWith(this.libkb, oldid, newid, true);
    if(this.externalCatalog != null)
      this.externalCatalog.renamePropertyInLibrary(oldid, newid);
    return this.save() && this.end();
  }
	
	@Override
	public void copyFrom(DataCreationAPI dc) {
		DataCreationKB dckb = (DataCreationKB)dc;
		
		this.start_write();
		this.libkb.copyFrom(dckb.libkb);
		
		KBUtils.renameTripleNamespace(this.libkb, dckb.dcns, this.dcns);
		KBUtils.renameTripleNamespace(this.libkb, dckb.dcdomns, this.dcdomns);
		KBUtils.renameTripleNamespace(this.libkb, dckb.dclibns, this.dclibns);
		KBUtils.renameAllTriplesWith(this.libkb, dckb.onturl, this.onturl, false);
		KBUtils.renameAllTriplesWith(this.libkb, dckb.liburl, this.liburl, false);
		
    // Change any specified locations of data
    KBObject locProp = this.libkb.getProperty(this.dcns+"hasLocation");
    ArrayList<KBTriple> triples = 
        this.libkb.genericTripleQuery(null, locProp, null);
    for(KBTriple t : triples) {
      if(t.getObject() == null || t.getObject().getValue() == null)
        continue;
      KBObject data = t.getSubject();
      String loc = (String) t.getObject().getValue();
      File f = new File(loc);
      loc = this.datadir + File.separator + f.getName();
      this.libkb.setPropertyValue(data, locProp, this.libkb.createLiteral(loc));
    }

		this.ontkb.copyFrom(dckb.ontkb);
		
		KBUtils.renameTripleNamespace(this.ontkb, dckb.dcns, this.dcns);
		KBUtils.renameTripleNamespace(this.ontkb, dckb.dcdomns, this.dcdomns);
		KBUtils.renameAllTriplesWith(this.ontkb, dckb.dcurl, this.dcurl, false);
		KBUtils.renameAllTriplesWith(this.ontkb, dckb.onturl, this.onturl, false);
		
		this.save();
		this.end();
		
		this.start_read();
		this.initializeAPI(true, true, true);
		this.end();
	}
	
	@Override
	public boolean delete() {
	    return 
	        this.start_write() &&
	        this.libkb.delete() &&
	        this.ontkb.delete() && 
	        this.save() && 
	        this.end();
	}
	
	@Override
	public DataCreationAPI getExternalCatalog() {
		return this.externalCatalog;
	}

	@Override
	public void setExternalCatalog(DataCreationAPI dc) {
		this.externalCatalog = dc;
		this.externalCatalog.copyFrom(this);
	}
	
	/*
	 * Private Helper functions below
	 */
	
	private ArrayList<KBObject> getSubClasses(KBObject cls) {
	  ArrayList<KBObject> subclses = new ArrayList<KBObject>();
	  for(KBTriple t : 
	      this.kb.genericTripleQuery(null, this.kb.getProperty(KBUtils.RDFS+"subClassOf"), cls)) {
	    KBObject subcls = this.kb.getConcept(t.getSubject().getID());
	    if(subcls == null) {
	      subcls = this.ontkb.createClass(t.getSubject().getID());
	    }
	    subclses.add(subcls);
	  }
	  return subclses;
	}

	private DataTree createHierarchy(String classid, boolean types_only) {
		DataItem rootitem = new DataItem(classid, DataItem.DATATYPE);
		DataTreeNode rootnode = new DataTreeNode(rootitem);
		ArrayList<DataTreeNode> queue = new ArrayList<DataTreeNode>();
		queue.add(rootnode);
		
		this.start_read();
		while (!queue.isEmpty()) {
			DataTreeNode node = queue.remove(0);
			DataItem item = node.getItem();
			if (item.getType() == DataItem.DATATYPE) {
				KBObject cls = this.kb.getConcept(item.getID());
				if (cls == null)
					continue;
				if (!types_only) {
					ArrayList<KBObject> instances = this.kb.getInstancesOfClass(cls, true);
					for (KBObject inst : instances) {
						DataItem institem = new DataItem(inst.getID(), DataItem.DATA);
						DataTreeNode childnode = new DataTreeNode(institem);
						node.addChild(childnode);
					}
				}
				ArrayList<KBObject> subclasses = this.getSubClasses(cls);
				for (KBObject subcls : subclasses) {
					if (!subcls.getNamespace().equals(this.dcdomns)
							&& !subcls.getNamespace().equals(this.dcdomns))
						continue;
					DataItem institem = new DataItem(subcls.getID(), DataItem.DATATYPE);
					DataTreeNode childnode = new DataTreeNode(institem);
					node.addChild(childnode);
					queue.add(childnode);
				}
			}
		}
		this.end();
		DataTree tree = new DataTree(rootnode);

		return tree;
	}
	
	/*
	 * Legacy function to convert properties with multiple domains
	 * to have a single domain which is a unionClass of all the domains
	 * -- we consider multiple domains as disjunctive whereas owl considers
	 *    them conjunctive, so we have to do this translation for our earlier domains
	 */
	private void convertPropertyDomainsToDisjunctiveDomains() {
		if(this.kb == null) 
			return;
		
		this.start_read();
		KBObject mprop = this.kb.getProperty(this.dcns + "hasMetrics");
		KBObject dmprop = this.kb.getProperty(this.dcns + "hasDataMetrics");
		ArrayList<KBObject> properties = this.kb.getSubPropertiesOf(mprop, false);
		properties.addAll(this.kb.getSubPropertiesOf(dmprop, false));
		
		for(KBObject prop : properties) {
			ArrayList<KBObject> doms = this.ontkb.getPropertyDomains(prop);
			if(doms.size() > 1) {
			  this.end();
			  
			  this.start_write();
				for(KBObject dom : doms)
					this.ontkb.removePropertyDomain(prop.getID(), dom.getID());
				for(KBObject dom : doms)
					this.ontkb.addPropertyDomainDisjunctive(prop.getID(), dom.getID());
		    this.ontkb.save();
		    this.end();			
		    
		    this.start_read();
			}
		}
		
		this.end();
	}
	
	private ArrayList<MetadataProperty> createMetadataProperties(ArrayList<KBObject> properties) {
		ArrayList<MetadataProperty> list = new ArrayList<MetadataProperty>();
		for (KBObject property : properties) {
			// Ignore properties not declared in this domain
			if (!property.getNamespace().equals(this.dcdomns))
				continue;
			MetadataProperty prop = this.createMetadataProperty(property);
			if (prop != null)
				list.add(prop);
		}
		return list;
	}

	private MetadataProperty createMetadataProperty(KBObject property) {
		if (property == null)
			return null;
		int proptype = this.kb.isDatatypeProperty(property) ? MetadataProperty.DATATYPE
				: MetadataProperty.OBJECT;
		MetadataProperty prop = new MetadataProperty(property.getID(), proptype);
		
		// Query for domain and range from the non-inference ontkb model (otherwise we get inferenced domains as well)
		ArrayList<KBObject> domains = this.ontkb.getPropertyDomainsDisjunctive(property);
		KBObject range = this.ontkb.getPropertyRange(property);
		for(KBObject domain : domains)
			prop.addDomain(domain.getID());
		if (range != null) {
			prop.setRange(range.getID());
		}
		return prop;
	}

}
