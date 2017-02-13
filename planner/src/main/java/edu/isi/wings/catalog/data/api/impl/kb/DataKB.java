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
import java.util.HashMap;
import java.util.Properties;

import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.ontapi.SparqlFactory;

public class DataKB {
	protected KBAPI kb;
	protected KBAPI ontkb;
	protected KBAPI libkb;

	protected String dcns;
	protected String dclibns;
	protected String dcdomns;

	protected String liburl;
	protected String onturl;
	protected String dcurl;
	
	protected String datadir;

	protected HashMap<String, KBObject> objPropMap;
	protected HashMap<String, KBObject> dataPropMap;
	protected HashMap<String, KBObject> conceptMap;

	protected HashMap<String, String> conceptNameFormat;

	protected OntFactory ontologyFactory;
	protected SparqlFactory sparqlFactory;
	protected String tdbRepository;

	public DataKB(Properties props, boolean create_writers, boolean plainkb) {
		this.dcurl = props.getProperty("ont.data.url");
		this.onturl = props.getProperty("ont.domain.data.url");
		this.liburl = props.getProperty("lib.domain.data.url");
		this.datadir = props.getProperty("lib.domain.data.storage");

		String hash = "#";
		this.dcns = dcurl + hash;
		this.dcdomns = onturl + hash;
		this.dclibns = liburl + hash;

		this.sparqlFactory = new SparqlFactory(this.dcns, this.dcdomns, this.liburl);

		this.tdbRepository = props.getProperty("tdb.repository.dir");
		if (tdbRepository == null) {
			this.ontologyFactory = new OntFactory(OntFactory.JENA);
		} else {
			this.ontologyFactory = new OntFactory(OntFactory.JENA, this.tdbRepository);
		}
		KBUtils.createLocationMappings(props, this.ontologyFactory);
		
		this.initializeAPI(create_writers, false, plainkb);
	}
	
	protected void initializeAPI(boolean create_writers, boolean create_if_empty, 
	    boolean plainkb) {
		try {
			this.kb = this.ontologyFactory.getKB(liburl, 
			    plainkb ? OntSpec.PLAIN : OntSpec.PELLET, create_if_empty);
			this.kb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, create_if_empty));
			this.kb.importFrom(this.ontologyFactory.getKB(dcurl, OntSpec.PLAIN, create_if_empty, true));
			if (create_writers) {
				this.ontkb = this.ontologyFactory.getKB(onturl, OntSpec.PLAIN);
				this.libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
			}
			this.initializeMaps();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeMaps() {
		this.objPropMap = new HashMap<String, KBObject>();
		this.dataPropMap = new HashMap<String, KBObject>();
		this.conceptMap = new HashMap<String, KBObject>();
		this.conceptNameFormat = new HashMap<String, String>();

		for (KBObject prop : this.kb.getAllObjectProperties()) {
			this.objPropMap.put(prop.getName(), prop);
		}
		for (KBObject prop : this.kb.getAllDatatypeProperties()) {
			this.dataPropMap.put(prop.getName(), prop);
		}
		for (KBObject con : this.kb.getAllClasses()) {
			this.conceptMap.put(con.getName(), con);
			for (String comment : this.kb.getAllComments(con)) {
				if (comment.startsWith("NameFormat=")) {
					this.conceptNameFormat
							.put(con.getID(), comment.replaceFirst("NameFormat=", ""));
				}
			}
		}
	}
	
	public String getDataLocation(String dataid) {
		KBObject locprop = this.kb.getProperty(this.dcns + "hasLocation");
		KBObject dobj = this.kb.getIndividual(dataid);
		KBObject locobj = this.kb.getPropertyValue(dobj, locprop);
		if (locobj != null && locobj.getValue() != null)
			return locobj.getValueAsString();
		else {
			String location = this.getDefaultDataLocation(dataid);
			File f = new File(location);
			if(f.exists())
				return location;
		}
		return null;
	}
	
	public String getDefaultDataLocation(String dataid) {
		KBObject dobj = this.kb.getResource(dataid);
		return this.datadir + File.separator + dobj.getName();
	}
}
