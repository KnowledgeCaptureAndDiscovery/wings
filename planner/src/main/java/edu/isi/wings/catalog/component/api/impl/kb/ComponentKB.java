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

package edu.isi.wings.catalog.component.api.impl.kb;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.kcap.ontapi.rules.KBRule;
import edu.isi.kcap.ontapi.rules.KBRuleClause;
import edu.isi.kcap.ontapi.rules.KBRuleList;
import edu.isi.kcap.ontapi.rules.KBRuleObject;
import edu.isi.kcap.ontapi.rules.KBRuleTriple;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentRole;
import edu.isi.wings.catalog.component.classes.requirements.ComponentRequirement;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.common.kb.KBUtils;

public class ComponentKB extends TransactionsJena {
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

	protected ResourceAPI resapi;

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
			boolean create_writers, boolean create_if_empty, boolean plainkb) {
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
		
		this.initializeAPI(create_writers, create_if_empty, plainkb);
	}
	
	protected void initializeAPI(boolean create_writers, boolean create_if_empty, boolean plainkb) {
		try {
			this.kb = this.ontologyFactory.getKB(absurl, 
			    plainkb ? OntSpec.PLAIN : OntSpec.PELLET, create_if_empty);
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
//    this.kb.importFrom(this.ontologyFactory.getKB(this.resliburl,
//        OntSpec.PLAIN, create_if_empty, true));
	
			if (create_writers) {
				if (load_concrete)
					this.writerkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
				else
					this.writerkb = this.ontologyFactory.getKB(absurl, OntSpec.PLAIN);
			}
			
			this.start_write();
			this.initializeMaps(this.kb);
			this.end();
			
			this.start_read();
			this.initDomainKnowledge();
			this.setRuleMappings(this.kb);
			
			this.end();
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
			this.conceptMap.put(prop.getID(), prop);
		}
		for (KBObject con : kb.getAllClasses()) {
			this.conceptMap.put(con.getName(), con);
			this.conceptMap.put(con.getID(), con);
		}
		for (KBObject odp : kb.getAllDatatypeProperties()) {
			this.dataPropMap.put(odp.getName(), odp);
			this.dataPropMap.put(odp.getID(), odp);
		}
		
		// Legacy ontologies don't have some properties. Add them in here
		if(!dataPropMap.containsKey("hasLocation"))
			dataPropMap.put("hasLocation", kb.createDatatypeProperty(this.pcns+"hasLocation"));
    if(!dataPropMap.containsKey("hasVersion"))
      dataPropMap.put("hasVersion", kb.createDatatypeProperty(this.pcns+"hasVersion"));
		if(!dataPropMap.containsKey("hasRule"))
			dataPropMap.put("hasRule", kb.createDatatypeProperty(this.pcns+"hasRule"));
		if(!dataPropMap.containsKey("hasDocumentation"))
			dataPropMap.put("hasDocumentation", kb.createDatatypeProperty(this.pcns+"hasDocumentation"));
    if(!dataPropMap.containsKey("isNoOperation"))
      dataPropMap.put("isNoOperation", kb.createDatatypeProperty(this.pcns+"isNoOperation"));		
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
	  boolean new_transaction = false;
	  if(!this.is_in_transaction()) {
	    this.start_read();
	    new_transaction = true;
	  }
	  try {
  		KBObject locprop = this.kb.getProperty(this.pcns + "hasLocation");
  		KBObject cobj = this.kb.getIndividual(cid);
  		KBObject locobj = this.kb.getPropertyValue(cobj, locprop);
  		if (locobj != null && locobj.getValue() != null)
  			return locobj.getValueAsString();
  		else if (cobj != null) {
  			String location = this.codedir + File.separator + cobj.getName();
  			File f = new File(location);
  			if(f.exists())
  				return location;
  		}
  		return null;
	  }
	  finally {
	    if(new_transaction) {
	      this.end();
	    }
	  }
	}
	
	public String getDefaultComponentLocation(String cid) {
	   boolean new_transaction = false;
    if(!this.is_in_transaction()) {
      this.start_read();
      new_transaction = true;
    }
    try {
  	  KBObject cobj = this.kb.getIndividual(cid);
  	  return this.codedir + File.separator + cobj.getName();
    }
    finally {
      if(new_transaction) {
        this.end();
      }
    }
	}
	
	protected void setComponentRules(String cid, String text) {
		KBObject compobj = this.writerkb.getIndividual(cid);
		KBObject ruleProp = this.dataPropMap.get("hasRule");
		try {
  		for(KBRule rule : ontologyFactory.parseRules(text).getRules()) {
  			KBObject ruleobj = writerkb.createLiteral(rule.toString());
  			this.writerkb.addPropertyValue(compobj, ruleProp, ruleobj);
  		}
		}
		catch (Exception e) {
		  e.printStackTrace();
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
			rulestr += ruleobj.getValueAsString();
		try {
		  return ontologyFactory.parseRules(rulestr);
		}
		catch (Exception e) {
		  return ontologyFactory.createEmptyRuleList();
		}
	}
	
	protected KBRuleList getInheritedComponentRules(String cid) {
	  try {
  		String rulestr = "";
  		KBObject compobj = this.kb.getIndividual(cid);
  		KBObject ruleProp = this.dataPropMap.get("hasRule");
  		for(KBObject clsobj : this.kb.getAllClassesOfInstance(compobj, true)) {
  			for(KBObject superclsobj : this.kb.getSuperClasses(clsobj, false)) {
  				for(KBObject supercompobj : this.kb.getInstancesOfClass(superclsobj, true))
  					for(KBObject ruleobj : this.kb.getPropertyValues(supercompobj, ruleProp))
  						rulestr += ruleobj.getValueAsString();
  			}
  		}
  		return ontologyFactory.parseRules(rulestr);
    }
    catch (Exception e) {
      return ontologyFactory.createEmptyRuleList();
    }
	}
	
	 
  protected ComponentRequirement getComponentRequirements(KBObject compobj, 
      KBAPI tkb) {
    ComponentRequirement requirement = new ComponentRequirement();
    
    KBObject sdprop = this.objPropMap.get("hasSoftwareDependency");
    KBObject hdprop = this.objPropMap.get("hasHardwareDependency");
    KBObject minver = this.objPropMap.get("requiresMinimumVersion");
    //KBObject exver = this.objPropMap.get("requiresExactVersion");
    for(KBObject sdobj : tkb.getPropertyValues(compobj, sdprop)) {
      KBObject verobj = tkb.getPropertyValue(sdobj, minver);
      if(verobj != null) 
        requirement.addSoftwareId(verobj.getID());
    }

    KBObject hdobj = tkb.getPropertyValue(compobj, hdprop);
    if(hdobj != null) {
      KBObject memobj = tkb.getPropertyValue(hdobj,
          this.dataPropMap.get("requiresMemoryGB"));
      KBObject stobj = tkb.getPropertyValue(hdobj,
          this.dataPropMap.get("requiresStorageGB"));
      KBObject s4bitobj = tkb.getPropertyValue(hdobj,
          this.dataPropMap.get("needs64bit"));
      
      if(memobj != null && memobj.getValue() != null)
        requirement.setMemoryGB(Float.parseFloat(memobj.getValueAsString()));
      if(stobj != null && stobj.getValue() != null)
        requirement.setStorageGB(Float.parseFloat(stobj.getValueAsString()));
      if(s4bitobj != null)
        requirement.setNeed64bit((Boolean)s4bitobj.getValue());
    }
    
    return requirement;
  }
  

  protected void setComponentRequirements(KBObject compobj,
      ComponentRequirement requirement, KBAPI tkb, KBAPI writerkb) {
    KBObject sdprop = this.objPropMap.get("hasSoftwareDependency");
    KBObject sdcls = this.conceptMap.get("SoftwareDependency");
    KBObject hdprop = this.objPropMap.get("hasHardwareDependency");
    KBObject hdcls = this.conceptMap.get("HardwareDependency");
    KBObject minver = this.objPropMap.get("requiresMinimumVersion");
    //KBObject exver = this.objPropMap.get("requiresExactVersion");    

    if(requirement.getSoftwareIds() != null) {
      for (String softwareId : requirement.getSoftwareIds()) {
        KBObject sdobj = writerkb.createObjectOfClass(null, sdcls);
        KBObject verobj = tkb.getResource(softwareId);
        if(verobj != null)
          writerkb.setPropertyValue(sdobj, minver, verobj);
        writerkb.addPropertyValue(compobj, sdprop, sdobj);
      }
    }
    
    KBObject hdobj = writerkb.createObjectOfClass(null, hdcls);
    writerkb.setPropertyValue(hdobj, 
        this.dataPropMap.get("requiresMemoryGB"), 
        writerkb.createLiteral(requirement.getMemoryGB()));
    writerkb.setPropertyValue(hdobj, 
        this.dataPropMap.get("requiresStorageGB"), 
        writerkb.createLiteral(requirement.getStorageGB()));
    writerkb.setPropertyValue(hdobj, 
        this.dataPropMap.get("needs64bit"), 
        writerkb.createLiteral(requirement.isNeed64bit()));
    writerkb.setPropertyValue(compobj, hdprop, hdobj);
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
			  this.start_write();
			  
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
							ontologyFactory.getDataObject(rule.toString());
					
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
				this.save(abskb);
				this.save(libkb);
				this.end();
				
				this.kb = this.ontologyFactory.getKB(this.absurl, OntSpec.PELLET);
				
				if (absrulesfile.exists())
					absrulesfile.delete();
				if (librulesfile.exists())
					librulesfile.delete();

			} catch (Exception e) {
			  this.end();
				e.printStackTrace();
			}
		}
	}
	
	// Legacy Porting code
	private void createAbstractFromConcrete() {
		try {
		  this.start_read();
			KBAPI abskb = this.ontologyFactory.getKB(OntSpec.PLAIN);
			KBAPI libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
			KBAPI kb = this.ontologyFactory.getKB(liburl, OntSpec.PELLET);
			kb.importFrom(this.ontologyFactory.getKB(this.props.getProperty("ont.component.url"), OntSpec.PLAIN));
			this.end();
			
			this.start_write();
			this.initializeMaps(kb);
			this.end();
			
			HashMap<String, KBObject> cmap = this.conceptMap;
			HashMap<String, KBObject> dpmap = this.dataPropMap;
			HashMap<String, KBObject> opmap = this.objPropMap;
			
			ArrayList<KBTriple> triples = new ArrayList<KBTriple>();
			
			this.start_read();
			ArrayList<KBObject> compobjs = kb.getInstancesOfClass(cmap.get("Component"), false);
			for(KBObject compobj : compobjs) {
				KBObject dval = kb.getDatatypePropertyValue(compobj, dpmap.get("isConcrete"));
				if(dval != null && dval.getValue() != null && !(Boolean)dval.getValue()) {
					// Get Component Class
					KBObject compcls = libkb.getClassOfInstance(compobj);
 
					// Read and write from plain kb so as not to get any entailments
					triples = libkb.genericTripleQuery(compobj, null, null);
					triples.addAll(libkb.genericTripleQuery(compcls, null, null));
					
					for(KBObject inputobj : libkb.getPropertyValues(compobj, opmap.get("hasInput")))
						triples.addAll(libkb.genericTripleQuery(inputobj, null, null));
					for(KBObject outputobj : libkb.getPropertyValues(compobj, opmap.get("hasOutput")))
						triples.addAll(libkb.genericTripleQuery(outputobj, null, null));
				}
			}
			this.end();
			
			this.start_write();
      for(KBTriple triple : triples)
        libkb.removeTriple(triple);
      libkb.save();

      abskb.addTriples(triples);			
			abskb.createImport(this.absurl, props.getProperty("ont.component.url"));
			abskb.saveAs(this.absurl);
			this.end();
			
			this.kb = this.ontologyFactory.getKB(this.absurl, OntSpec.PELLET);
		}
		catch (Exception e) {
		  this.end();
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
	
  public Component getComponent(String cid, boolean details) {
    this.start_read();
    
    try {
      KBObject compobj = kb.getIndividual(cid);
      if(compobj == null) return null;
      
      KBObject concobj = kb.getDatatypePropertyValue(compobj, this.dataPropMap.get("isConcrete"));
      boolean isConcrete = false;
      if(concobj != null && concobj.getValue() != null)
        isConcrete = ((Boolean) concobj.getValue()).booleanValue();
      int ctype = isConcrete ? Component.CONCRETE : Component.ABSTRACT;
  
      Component comp = new Component(compobj.getID(), ctype);
      if (isConcrete)
        comp.setLocation(this.getComponentLocation(cid));
  
      if (details) {
				comp.setDocumentation(this.getComponentDocumentation(compobj));
				//obtain component type
				KBObject classComponent = this.kb.getClassOfInstance(this.kb.getIndividual(cid));
				if(classComponent != null && classComponent.getID() != null) {
  				ArrayList<KBObject> clss = this.kb.getSuperClasses(classComponent, true);
  				if (clss != null && clss.size() > 0){
  					KBObject cls = clss.get(0);
  					comp.setComponentType(cls.getName().replace("Class", ""));
  				}
				}
        comp.setComponentRequirement(
            this.getComponentRequirements(compobj, this.kb));
        
        ArrayList<KBObject> inobjs = this.getComponentInputs(compobj);
        for (KBObject inobj : inobjs) {
          comp.addInput(this.getRole(inobj));
        }
        ArrayList<KBObject> outobjs = this.getComponentOutputs(compobj);
        for (KBObject outobj : outobjs) {
          comp.addOutput(this.getRole(outobj));
        }
        comp.setRules(this.getDirectComponentRules(cid));
        comp.setInheritedRules(this.getInheritedComponentRules(cid));
        comp.setSource(this.getComponentModelCatalog(compobj));
        comp.setVersion(this.getComponentVersion(compobj));
      }
      return comp;
    }
    finally {
      this.end();      
    }
  }	
  
  protected int getComponentVersion(KBObject compobj) {
    KBObject versionProp = kb.getProperty(this.pcns + "hasVersion");
    KBObject versionVal = kb.getPropertyValue(compobj, versionProp);
    return (Integer) ((versionVal != null && versionVal.getValue() != null) ? versionVal.getValue() : 0);  
  }
  
  protected ArrayList<KBObject> getComponentInputs(KBObject compobj) {
    KBObject inProp = kb.getProperty(this.pcns + "hasInput");
    return kb.getPropertyValues(compobj, inProp);
  }

  protected ArrayList<KBObject> getComponentOutputs(KBObject compobj) {
    KBObject outProp = kb.getProperty(this.pcns + "hasOutput");
    return kb.getPropertyValues(compobj, outProp);
  }

  protected String getComponentDocumentation(KBObject compobj) {
    KBObject docProp = kb.getProperty(this.pcns + "hasDocumentation");
    KBObject doc = kb.getPropertyValue(compobj, docProp);
    if(doc != null && doc.getValue() != null)
        return doc.getValueAsString();
    return null;
  }

	protected String getComponentModelCatalog(KBObject compobj) {
		KBObject docProp = kb.getProperty(this.pcns + "source");
		KBObject doc = kb.getPropertyValue(compobj, docProp);
		if(doc != null && doc.getValue() != null)
			return doc.getValueAsString();
		return null;
	}
  
  protected ComponentRole getRole(KBObject argobj) {
    ComponentRole arg = new ComponentRole(argobj.getID());
    KBObject argidProp = kb.getProperty(this.pcns + "hasArgumentID");
    KBObject dimProp = kb.getProperty(this.pcns + "hasDimensionality");
    KBObject pfxProp = kb.getProperty(this.pcns + "hasArgumentName");
    KBObject valProp = kb.getProperty(this.pcns + "hasValue");

    ArrayList<KBObject> alltypes = kb.getAllClassesOfInstance(argobj, true);

    for (KBObject type : alltypes) {
      if (type.getID().equals(this.pcns + "ParameterArgument"))
        arg.setParam(true);
      else if (type.getID().equals(this.pcns + "DataArgument"))
        arg.setParam(false);
      else if (type.getNamespace().equals(this.dcdomns)
          || type.getNamespace().equals(this.dcns))
        arg.setType(type.getID());
    }
    KBObject role = kb.getPropertyValue(argobj, argidProp);
    KBObject dim = kb.getPropertyValue(argobj, dimProp);
    KBObject pfx = kb.getPropertyValue(argobj, pfxProp);

    if (arg.isParam()) {
      KBObject val = kb.getPropertyValue(argobj, valProp);
      if (val != null) {
        arg.setType(val.getDataType());
        arg.setParamDefaultalue(val.getValue());
      }
    }
    if (role != null && role.getValue() != null)
      arg.setRoleName(role.getValueAsString());
    if (dim != null && dim.getValue() != null)
      arg.setDimensionality((Integer) dim.getValue());
    if (pfx != null && pfx.getValue() != null)
      arg.setPrefix(pfx.getValueAsString());
    
    return arg;
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
		rulePrefixes.put("res", this.resonturl+"#");
		rulePrefixes.put("reslib", this.resliburl+"#");
		kb.setRulePrefixes(rulePrefixes);
	}
}
