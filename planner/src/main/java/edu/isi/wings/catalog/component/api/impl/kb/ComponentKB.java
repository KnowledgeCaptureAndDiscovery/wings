package edu.isi.wings.catalog.component.api.impl.kb;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.ontapi.rules.KBRule;
import edu.isi.wings.ontapi.rules.KBRuleClause;
import edu.isi.wings.ontapi.rules.KBRuleList;
import edu.isi.wings.ontapi.rules.KBRuleObject;
import edu.isi.wings.ontapi.rules.KBRuleTriple;

public class ComponentKB {
	protected KBAPI kb;
	protected KBAPI writerkb;
	protected String dcns;
	protected String pcns;
	protected String pcdomns;
	protected String dcdomns;
	protected String wflowns;
	protected String pcurl;
	protected String liburl;
	protected String absurl;
	protected String dconturl;
	protected String resonturl;
	protected String resliburl;
	protected String codedir;
	protected String topclass;

	protected OntFactory ontologyFactory;

	protected HashMap<String, KBObject> objPropMap;
	protected HashMap<String, KBObject> dataPropMap;
	protected HashMap<String, KBObject> conceptMap;

	protected ArrayList<KBTriple> domainKnowledge;
	protected HashMap<String, String> rulePrefixes;

	protected boolean load_concrete;
	
	protected Properties props;

	/**
	 * Constructor
	 * 
	 * @param props
	 *            The properties should contain: lib.concrete.url,
	 *            lib.concrete.rules.path lib.abstract.url,
	 *            lib.abstract.rules.path ont.domain.component.url,
	 *            ont.domain.data.url, ont.component.url, ont.data.url,
	 *            ont.workflow.url tdb.repository.dir (optional)
	 */
	public ComponentKB(Properties props, boolean load_concrete, 
			boolean create_writers, boolean create_if_empty) {
		this.props = props;
		
		String hash = "#";
		this.dcns = props.getProperty("ont.data.url") + hash;
		this.pcns = props.getProperty("ont.component.url") + hash;
		this.dcdomns = props.getProperty("ont.domain.data.url") + hash;
		this.wflowns = props.getProperty("ont.workflow.url") + hash;
		this.pcurl = props.getProperty("ont.component.url");
		this.dconturl = props.getProperty("ont.domain.data.url");
		this.pcdomns = props.getProperty("ont.domain.component.ns");
		this.absurl = props.getProperty("lib.abstract.url");
		this.liburl = props.getProperty("lib.concrete.url");
		this.codedir = props.getProperty("lib.domain.code.storage");
    this.resonturl = props.getProperty("ont.resource.url");
    this.resliburl = props.getProperty("lib.resource.url");
		this.load_concrete = load_concrete;
		this.topclass = this.pcns + "Component";
		
		String tdbRepository = props.getProperty("tdb.repository.dir");
		if (tdbRepository == null) {
			this.ontologyFactory = new OntFactory(OntFactory.JENA);
		} else {
			this.ontologyFactory = new OntFactory(OntFactory.JENA, tdbRepository);
		}
		KBUtils.createLocationMappings(props, this.ontologyFactory);

		this.initializeAPI(create_writers, create_if_empty);
	}
	
	protected void initializeAPI(boolean create_writers, boolean create_if_empty) {
		try {
			this.kb = this.ontologyFactory.getKB(absurl, OntSpec.PELLET, create_if_empty);
		}
		catch(Exception e) {
			// Legacy Porting:  
			// - Absurl is missing. Load liburl and extract abstract components from it
			this.createAbstractFromConcrete();
		}
		finally {
			// Legacy Porting: 
			// - If rule files are present, move rules as property values into components
			this.saveRulesInComponents(props);
		}
		
		try {
			if (load_concrete) {
				this.kb.importFrom(this.ontologyFactory.getKB(liburl, OntSpec.PLAIN, create_if_empty));
			}
			this.kb.importFrom(this.ontologyFactory.getKB(props.getProperty("ont.domain.data.url"),
					OntSpec.PLAIN, create_if_empty));
			this.kb.importFrom(this.ontologyFactory.getKB(props.getProperty("ont.component.url"),
					OntSpec.PLAIN, create_if_empty, true));
//		this.kb.importFrom(this.ontologyFactory.getKB(props.getProperty("ont.data.url"),
//				OntSpec.PLAIN, true, true));
      this.kb.importFrom(this.ontologyFactory.getKB(this.resonturl,
          OntSpec.PLAIN, create_if_empty, true));
      this.kb.importFrom(this.ontologyFactory.getKB(this.resliburl,
          OntSpec.PLAIN, create_if_empty, true));
	
			if (create_writers) {
				if (load_concrete)
					this.writerkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
				else
					this.writerkb = this.ontologyFactory.getKB(absurl, OntSpec.PLAIN);
			}
			this.initializeMaps(this.kb);
			this.initDomainKnowledge();
			this.setRuleMappings(this.kb);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeMaps(KBAPI kb) {
		this.objPropMap = new HashMap<String, KBObject>();
		this.dataPropMap = new HashMap<String, KBObject>();
		this.conceptMap = new HashMap<String, KBObject>();

		for (KBObject prop : kb.getAllObjectProperties()) {
			this.objPropMap.put(prop.getName(), prop);
		}
		for (KBObject con : kb.getAllClasses()) {
			this.conceptMap.put(con.getName(), con);
		}
		for (KBObject odp : kb.getAllDatatypeProperties()) {
			this.dataPropMap.put(odp.getName(), odp);
		}
		
		// Legacy ontologies don't have some properties. Add them in here
		if(!dataPropMap.containsKey("hasLocation"))
			dataPropMap.put("hasLocation", kb.createDatatypeProperty(this.pcns+"hasLocation"));
		if(!dataPropMap.containsKey("hasRule"))
			dataPropMap.put("hasRule", kb.createDatatypeProperty(this.pcns+"hasRule"));
		if(!dataPropMap.containsKey("hasDocumentation"))
			dataPropMap.put("hasDocumentation", kb.createDatatypeProperty(this.pcns+"hasDocumentation"));
	}

	private void initDomainKnowledge() {
		// Create general domain knowledge data for use in rules
		domainKnowledge = new ArrayList<KBTriple>();
		KBObject rdfsSubProp = this.kb.getProperty(KBUtils.RDFS + "subPropertyOf");
		KBObject dcMetricsProp = this.kb.getProperty(this.dcns + "hasMetrics");
		KBObject dcDataMetricsProp = this.kb.getProperty(this.dcns + "hasDataMetrics");
		domainKnowledge.addAll(kb.genericTripleQuery(null, rdfsSubProp, dcMetricsProp));
		domainKnowledge.addAll(kb.genericTripleQuery(null, rdfsSubProp, dcDataMetricsProp));
	}

	public String getComponentLocation(String cid) {
		KBObject locprop = this.kb.getProperty(this.pcns + "hasLocation");
		KBObject cobj = this.kb.getIndividual(cid);
		KBObject locobj = this.kb.getPropertyValue(cobj, locprop);
		if (locobj != null && locobj.getValue() != null)
			return locobj.getValue().toString();
		else {
			String location = this.codedir + File.separator + cobj.getName();
			File f = new File(location);
			if(f.exists())
				return location;
		}
		return null;
	}
	
	protected void setComponentRules(String cid, String text) {
		KBObject compobj = this.writerkb.getIndividual(cid);
		KBObject ruleProp = this.dataPropMap.get("hasRule");
		for(KBRule rule : ontologyFactory.parseRules(text).getRules()) {
			KBObject ruleobj = ontologyFactory.getDataObject(rule.getInternalRuleObject().toString());
			this.writerkb.addPropertyValue(compobj, ruleProp, ruleobj);
		}
	}
	
	protected KBRuleList getComponentRules(String cid) {
		KBRuleList comprules = this.getDirectComponentRules(cid);
		comprules.mergeRules(this.getInheritedComponentRules(cid));
		return comprules;
	}
	
	protected KBRuleList getDirectComponentRules(String cid) {
		String rulestr = "";
		KBObject compobj = this.kb.getIndividual(cid);
		KBObject ruleProp = this.dataPropMap.get("hasRule");
		for(KBObject ruleobj : this.kb.getPropertyValues(compobj, ruleProp))
			rulestr += ruleobj.getValue().toString();
		return ontologyFactory.parseRules(rulestr);
	}
	
	protected KBRuleList getInheritedComponentRules(String cid) {
		String rulestr = "";
		KBObject compobj = this.kb.getIndividual(cid);
		KBObject ruleProp = this.dataPropMap.get("hasRule");
		for(KBObject clsobj : this.kb.getAllClassesOfInstance(compobj, true)) {
			for(KBObject superclsobj : this.kb.getSuperClasses(clsobj, false)) {
				for(KBObject supercompobj : this.kb.getInstancesOfClass(superclsobj, true))
					for(KBObject ruleobj : this.kb.getPropertyValues(supercompobj, ruleProp))
						rulestr += ruleobj.getValue().toString();
			}
		}
		return ontologyFactory.parseRules(rulestr);
	}
	

	/*
	 * Legacy component library porting functions
	 */
	
	private void saveRulesInComponents(Properties props) {
		File absrulesfile = null, librulesfile = null;
		if(props.containsKey("lib.abstract.rules.path"))
			absrulesfile = new File(props.getProperty("lib.abstract.rules.path"));
		if(props.containsKey("lib.concrete.rules.path"))
			librulesfile = new File(props.getProperty("lib.concrete.rules.path"));

		if((absrulesfile != null && absrulesfile.exists()) || 
				(librulesfile != null && librulesfile.exists())) {
			String rulestr = this.readRulesFromFile(absrulesfile);
			rulestr += this.readRulesFromFile(librulesfile);
			if(rulestr.equals("")) 
				return;
			
			try {
				KBAPI abskb = this.ontologyFactory.getKB(absurl, OntSpec.PLAIN);
				KBAPI libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
				
				KBAPI kb = this.ontologyFactory.getKB(absurl, OntSpec.PLAIN);
				kb.importFrom(this.ontologyFactory.getKB(
						this.props.getProperty("ont.component.url"), OntSpec.PLAIN));
				KBObject ruleProp = kb.getProperty(this.pcns+"hasRule");
				
				this.setRuleMappings(kb);
				KBRuleList rulelist = ontologyFactory.parseRules(rulestr);
				for (KBRule rule : rulelist.getRules()) {
					boolean found = false;
					KBObject ruleobj = 
							ontologyFactory.getDataObject(rule.getInternalRuleObject().toString());
					
					for (KBRuleClause clause : rule.getRuleBody()) {
						if (clause.isTriple()) {
							KBRuleTriple triple = clause.getTriple();
							KBRuleObject subj = triple.getSubject();
							KBRuleObject pred = triple.getPredicate();
							KBRuleObject obj = triple.getObject();
							if (subj == null || pred == null || obj == null)
								continue;
							if (subj.isVariable() && !pred.isVariable() && !obj.isVariable()) {
								if (pred.getKBObject().getID().equals(KBUtils.RDF + "type")
										&& (obj.getKBObject().getID().endsWith("Class"))) {
									KBObject cls = abskb.getConcept(obj.getKBObject().getID());
									if(cls != null) {
										for(KBObject compobj : abskb.getInstancesOfClass(cls, true))
											abskb.addPropertyValue(compobj, ruleProp, ruleobj);
										found = true;
										break;
									}
									else {
										cls = libkb.getConcept(obj.getKBObject().getID());
										if(cls != null) {
											for(KBObject compobj : libkb.getInstancesOfClass(cls, true)) 
												libkb.addPropertyValue(compobj, ruleProp, ruleobj);
										}
										found = true;
										break;
									}
								}
							}
						}
					}
					
					if(!found) {
						// Miscellaneous rule. Add to all topmost objects
						for(KBObject subcls : kb.getSubClasses(kb.getConcept(this.topclass), true)) {
							for(KBObject kbcobj : kb.getInstancesOfClass(subcls, true)) {
								KBObject compobj = abskb.getIndividual(kbcobj.getID());
								if(compobj != null)
									abskb.addPropertyValue(compobj, ruleProp, ruleobj);
							}
						}
					}
				}
				abskb.save();
				libkb.save();
				
				this.kb = this.ontologyFactory.getKB(this.absurl, OntSpec.PELLET);
				
				if (absrulesfile.exists())
					absrulesfile.delete();
				if (librulesfile.exists())
					librulesfile.delete();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	// Legacy Porting code
	private void createAbstractFromConcrete() {
		try {
			KBAPI abskb = this.ontologyFactory.getKB(OntSpec.PLAIN);
			KBAPI libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
			KBAPI kb = this.ontologyFactory.getKB(liburl, OntSpec.PELLET);
			kb.importFrom(this.ontologyFactory.getKB(this.props.getProperty("ont.component.url"), OntSpec.PLAIN));
			this.initializeMaps(kb);
			
			HashMap<String, KBObject> cmap = this.conceptMap;
			HashMap<String, KBObject> dpmap = this.dataPropMap;
			HashMap<String, KBObject> opmap = this.objPropMap;
			
			ArrayList<KBObject> compobjs = kb.getInstancesOfClass(cmap.get("Component"), false);
			for(KBObject compobj : compobjs) {
				KBObject dval = kb.getDatatypePropertyValue(compobj, dpmap.get("isConcrete"));
				if(dval != null && dval.getValue() != null && !(Boolean)dval.getValue()) {
					// Not concrete -- move to abstract
					ArrayList<KBTriple> triples = new ArrayList<KBTriple>();
					
					// Get Component Class
					KBObject compcls = libkb.getClassOfInstance(compobj);
 
					// Read and write from plain kb so as not to get any entailments
					triples = libkb.genericTripleQuery(compobj, null, null);
					triples.addAll(libkb.genericTripleQuery(compcls, null, null));
					
					for(KBObject inputobj : libkb.getPropertyValues(compobj, opmap.get("hasInput")))
						triples.addAll(libkb.genericTripleQuery(inputobj, null, null));
					for(KBObject outputobj : libkb.getPropertyValues(compobj, opmap.get("hasOutput")))
						triples.addAll(libkb.genericTripleQuery(outputobj, null, null));
					
					for(KBTriple triple : triples)
						libkb.removeTriple(triple);
					
					abskb.addTriples(triples);
				}
			}
			abskb.createImport(this.absurl, props.getProperty("ont.component.url"));
			abskb.saveAs(this.absurl);
			libkb.save();
			
			this.kb = this.ontologyFactory.getKB(this.absurl, OntSpec.PELLET);
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	
	private String readRulesFromFile(File f) {
		String str = "";
		try {
			if(!f.canRead()) return str;
			Scanner sc = new Scanner(f);
			while (sc.hasNextLine()) {
				String line = ignoreComments(sc.nextLine());
				if (line != null)
					str += line + "\n";
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return str;
	}

	private String ignoreComments(String line) {
		String result_line = null;
		int upto = line.indexOf('#');
		if (upto != 0 && upto > 0) {
			result_line = line.substring(0, upto);
		} else if (upto < 0) {
			result_line = line;
		}
		return result_line;
	}

	/**
	 * Set Rule Prefix-Namespace Mappings Prefixes allowed in Rules: rdf, rdfs,
	 * owl, xsd -- usual dc, dcdom -- data catalog pc, pcdom -- component
	 * catalog ac, acdom -- synonyms for pc, pcdom
	 */
	private void setRuleMappings(KBAPI kb) {
		rulePrefixes = new HashMap<String, String>();
		rulePrefixes.put("rdf", KBUtils.RDF);
		rulePrefixes.put("rdfs", KBUtils.RDFS);
		rulePrefixes.put("owl", KBUtils.OWL);
		rulePrefixes.put("xsd", KBUtils.XSD);
		rulePrefixes.put("", this.wflowns);
		rulePrefixes.put("dcdom", this.dcdomns);
		rulePrefixes.put("dc", this.dcns);
		rulePrefixes.put("pcdom", this.pcdomns);
		rulePrefixes.put("pc", this.pcns);
		rulePrefixes.put("acdom", this.pcdomns); // Legacy
		rulePrefixes.put("ac", this.pcns); // Legacy
		rulePrefixes.put("wflow", this.wflowns);
		kb.setRulePrefixes(rulePrefixes);
	}
}
