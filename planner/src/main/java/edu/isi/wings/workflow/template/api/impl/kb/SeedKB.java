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

package edu.isi.wings.workflow.template.api.impl.kb;

import java.util.ArrayList;
import java.util.Properties;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.wings.workflow.template.api.ConstraintEngine;
import edu.isi.wings.workflow.template.api.Seed;
import edu.isi.wings.workflow.template.classes.Metadata;
import edu.isi.wings.workflow.template.classes.Rules;
import edu.isi.wings.workflow.template.classes.variables.Variable;

public class SeedKB extends TemplateKB implements Seed {
	private static final long serialVersionUID = 1L;

	transient protected KBAPI r_kb;
	transient protected KBAPI t_kb;

	transient protected ConstraintEngine r_constraintEngine;
	transient protected ConstraintEngine t_constraintEngine;

	String templateURL;

	protected Metadata seedMeta;
	protected Rules seedRules;

	// Loading an existing seed
	public SeedKB(Properties props, String seedid) {
		super(seedid);
		this.props = props;
		super.initVariables(props);
		super.initializeKB(props, true);

		this.templateURL = this.findTemplateUrl();
		try {
			this.kb.importFrom(ontologyFactory.getKB(this.templateURL, OntSpec.PLAIN));
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// Read in the template
		super.readTemplate();

		// Initialize the local seed kbs
		initializeLocalKBs();
		initializeEngines();

//		this.seedMeta = readMetadata(r_kb, this.getUrl());
//		this.metadata = readMetadata(kb, templateURL);
//
//		this.seedRules = readRules(r_kb, this.getUrl());
//		this.rules = readRules(kb, templateURL);
	}

	// Creation of a blank seed
	public SeedKB(Properties props, String seedid, String templateid) {
		super(props, templateid);
		this.templateURL = this.getURL();
		this.setID(seedid);
		
		// Plain kb initialize (no need to read anything here)
		this.r_kb = ontologyFactory.getKB(OntSpec.PLAIN);
		this.t_kb = ontologyFactory.getKB(OntSpec.PLAIN);
		this.t_kb.importFrom(this.kb);
		
		initializeEngines();

		this.seedMeta = new Metadata();
		this.seedRules = new Rules();
	}

	
	@Override
	public void reloadSeedFromEngine() {
		this.kb.importFrom(r_kb);
		super.readTemplate();
	}
	
	private void initializeLocalKBs() {
		try {
			r_kb = ontologyFactory.getKB(this.getURL(), OntSpec.PLAIN);
			r_kb.useRawModel();
			t_kb = ontologyFactory.getKB(this.getURL(), OntSpec.PLAIN);
			t_kb.useBaseModel();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeEngines() {
		this.r_constraintEngine = new ConstraintEngineKB(r_kb, this.wflowns);
		// this.r_constraintEngine.addBlacklistedNamespace(uriPrefix + "/" +
		// domain + "/seeds/");
		// this.r_constraintEngine.addBlacklistedNamespace(uriPrefix + "/" +
		// domain + "/");
		this.t_constraintEngine = new ConstraintEngineKB(t_kb, this.wflowns);
	}

	public ConstraintEngine getSeedConstraintEngine() {
		return this.r_constraintEngine;
	}

	public ConstraintEngine getTemplateConstraintEngine() {
		return this.t_constraintEngine;
	}
	
	private String findTemplateUrl() {
		for (String importurl : kb.getImports(this.getURL())) {
			if (!importurl.equals(this.onturl))
				return importurl;
		}
		return null;
	}

	public String getInternalRepresentation() {
		// return r_kb.toN3(this.url);
		return r_kb.toAbbrevRdf(false, this.getURL());
	}

	public String serialize() {
		// Create a plain new KB
		KBAPI tkb = ontologyFactory.getKB(OntSpec.PLAIN);

		// Add template import
		tkb.createImport("", this.templateURL);

		ArrayList<String> varids = new ArrayList<String>();
		for (Variable v : getVariables())
			varids.add(v.getID());
		for (KBTriple t : this.r_constraintEngine.getConstraints(varids)) {
			tkb.addTriple(t);
		}
		// writeMetadataDescription(tkb, seedMeta);
		// writeRules(tkb, seedRules);

		// Return RDF representation
		// return tapi.toN3(this.url);
		return tkb.toAbbrevRdf(false, this.getURL());
	}

	public String deriveTemplateRepresentation() {
		KBAPI tkb = kb;
		kb = t_kb;
		String rdf = super.serialize();
		kb = tkb;
		return rdf;
	}

	public Metadata getSeedMetadata() {
		return this.seedMeta;
	}

	public Rules getSeedRules() {
		return this.seedRules;
	}
}
