package edu.isi.wings.ontapi.jena;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
//import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.shared.WrappedIOException;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.vocabulary.RDFS;

import edu.isi.wings.ontapi.*;
import edu.isi.wings.ontapi.rules.KBRule;
import edu.isi.wings.ontapi.rules.KBRuleList;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class KBAPIJena implements KBAPI {
	// The ontology ontmodel
	OntModel ontmodel;

	OntModelSpec modelSpec;
	OntSpec spec;
	Reasoner reasoner;

	// Data source
	String url;
	String base;
	InputStream inputstream;
	
	static Dataset tdbstore; // TDB store
	boolean usetdb;
	boolean cache_url;
	boolean write_file_if_absent;

	public KBAPIJena(OntSpec spec) {
		this.spec = spec;
		try {
			initialize(spec);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public KBAPIJena(String url, OntSpec spec) throws Exception {
		this.url = url;
		this.spec = spec;
		initialize(spec);
	}
	
	public KBAPIJena(String url, OntSpec spec, boolean write_file_if_absent,
			boolean cache_url ) throws Exception {
		this.url = url;
		this.spec = spec;
		this.cache_url = cache_url;
		this.write_file_if_absent = write_file_if_absent;
		initialize(spec);
	}

	public KBAPIJena(String url, String storedir, OntSpec spec) throws Exception{
		this(url, storedir, spec, false);
	}

	public KBAPIJena(InputStream data, String base, OntSpec spec) {
		this.inputstream = data;
		this.base = base;
		this.spec = spec;
		try {
			initialize(spec);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public KBAPIJena(String url, String storedir, OntSpec spec, boolean load_url_into_store) throws Exception {
		this.url = url;
		this.spec = spec;
		this.cache_url = load_url_into_store;
		this.setTdbStore(storedir);
		initialize(spec);
	}

	public Dataset getTdbStore() {
		return tdbstore;
	}

	public void setTdbStore(String storedir) {
		if(tdbstore == null)
			tdbstore = TDBFactory.createDataset(storedir);
		this.usetdb = true;
	}
	
	private void initialize(OntSpec spec) throws Exception {
		modelSpec = getOntSpec(spec);
		if (modelSpec == null)
			return;

		OntDocumentManager.getInstance().setProcessImports(false);
		if (!this.usetdb || tdbstore == null || this.url == null) {
			// If there is no triple tdbstore
			ontmodel = ModelFactory.createOntologyModel(modelSpec);
			readModel();
		} else {
			// If there is a triple tdbstore
			Model tmodel = tdbstore.getNamedModel(this.url);
			if (this.cache_url && tmodel.isEmpty()) {
				tmodel.read(this.url);
				TDB.sync(tmodel);
			}
			if (tmodel != null) {
				ontmodel = ModelFactory.createOntologyModel(modelSpec, tmodel);
			}
		}
	}

	private void readModel() throws Exception {
		if (ontmodel != null) {
			if (this.url != null) {
				try {
					ontmodel.read(this.url);
				}
				catch (WrappedIOException e) {
					if (this.cache_url) {
					    String furi = LocationMapper.get().altMapping(this.url);
					    FileUtils.copyURLToFile(new URL(this.url), new File(new URI(furi)));
					    ontmodel.read(this.url);
					}
					else if(this.write_file_if_absent) {
						this.save();
						ontmodel.read(this.url);
					}
					else {
						System.err.println(this.url + "  not found");
						//e.printStackTrace();
					}
				}
			} else if (this.inputstream != null) {
				ontmodel.read(this.inputstream, this.base);
			}
		}
	}

	private OntModelSpec getOntSpec(OntSpec spec) {
		OntModelSpec modelspec = null;
		if (spec == OntSpec.PLAIN) {
			modelspec = OntModelSpec.OWL_MEM;
		} else if (spec == OntSpec.MINI) {
			modelspec = OntModelSpec.OWL_MEM_MINI_RULE_INF;
		} else if (spec == OntSpec.MICRO) {
			modelspec = OntModelSpec.OWL_MEM_MICRO_RULE_INF;
		} else if (spec == OntSpec.DL) {
			modelspec = OntModelSpec.OWL_DL_MEM_RULE_INF;
		} else if (spec == OntSpec.FULL) {
			modelspec = OntModelSpec.OWL_MEM_RULE_INF;
		} else if (spec == OntSpec.TRANS) {
			modelspec = OntModelSpec.OWL_MEM_TRANS_INF;
		} else if (spec == OntSpec.PELLET) {
			cleanPelletSpec(PelletReasonerFactory.THE_SPEC);
			modelspec = PelletReasonerFactory.THE_SPEC;
		}
		return modelspec;
	}

	public void useRawModel() {
		OntModel tmodel = ModelFactory.createOntologyModel(getOntSpec(spec));
		tmodel.add(ontmodel.getRawModel());
		ontmodel = tmodel;
	}

	public void useBaseModel() {
		OntModel tmodel = ModelFactory.createOntologyModel(getOntSpec(spec));
		tmodel.add(ontmodel.getBaseModel());
		ontmodel = tmodel;
	}

	private void cleanPelletSpec(OntModelSpec spec) {
		ArrayList<String> models = new ArrayList<String>();
		for (Iterator<String> miter = spec.getImportModelMaker().listModels(); miter.hasNext();)
			models.add(miter.next());
		for (String m : models)
			spec.getImportModelMaker().removeModel(m);

		ArrayList<String> graphs = new ArrayList<String>();
		for (Iterator<String> giter = spec.getImportModelMaker().getGraphMaker().listGraphs(); giter
				.hasNext();)
			graphs.add(giter.next());
		for (String g : graphs)
			spec.getImportModelMaker().getGraphMaker().removeGraph(g);
	}

	public void applyRule(KBRule rule) {
		if(rule == null) return;
		ArrayList<Rule> rules = new ArrayList<Rule>();
		rules.add((Rule) rule.getInternalRuleObject());
		this.applyRulesHelper(rules);
	}
	
	public void applyRules(KBRuleList rulelist) {
		if(rulelist == null) return;
		ArrayList<Rule> rules = new ArrayList<Rule>();
		for(KBRule rule : rulelist.getRules()) {
			rules.add((Rule) rule.getInternalRuleObject());
		}
		this.applyRulesHelper(rules);
	}

	private void applyRulesHelper(List<Rule> rules) {
		OntModelSpec rulesModelSpec = new OntModelSpec(getOntSpec(OntSpec.PLAIN));
		GenericRuleReasoner reasoner = new GenericRuleReasoner(rules,
				rulesModelSpec.getReasonerFactory());
		reasoner.setOWLTranslation(true);
		reasoner.setTransitiveClosureCaching(true);
		// reasoner.setDerivationLogging(true);
		rulesModelSpec.setReasoner(reasoner);

		// Create a temporary inference ontmodel and create new entailments
		OntModel newmodel = ModelFactory.createOntologyModel(rulesModelSpec);
		newmodel.add(ontmodel);

		for (Iterator<Statement> itst = newmodel.getDeductionsModel().listStatements(); itst
				.hasNext();) {
			Statement st = itst.next();
			this.ontmodel.add(st);
		}
	}

	/**
	 * rulePrefixes is a prefix:namespace map allowing the use of these prefixes
	 * in rules
	 */
	public void setRulePrefixes(HashMap<String, String> rulePrefixes) {
		for (String prefix : rulePrefixes.keySet()) {
			PrintUtil.registerPrefix(prefix, rulePrefixes.get(prefix));
		}
	}

	// Simple resource, etc queries
	public KBObject getResource(String id) {
		KBObject res = null;
		Resource r = ontmodel.getResource(id);
		if (r != null) {
			res = new KBObjectJena(r);
		}
		return res;
	}

	public KBObject getConcept(String id) {
		KBObject cls = null;
		OntClass cl = ontmodel.getOntClass(id);
		if (cl != null) {
			cls = new KBObjectJena(cl);
		}
		return cls;
	}

	public KBObject getIndividual(String id) {
		KBObject indobj = null;
		Individual ind = ontmodel.getIndividual(id);
		if (ind != null) {
			indobj = new KBObjectJena(ind);
		}
		return indobj;
	}

	public KBObject getProperty(String id) {
		KBObject propobj = null;
		Property prop = ontmodel.getProperty(id);
		if (prop != null && prop.isProperty()) {
			propobj = new KBObjectJena(prop);
		}
		return propobj;
	}

	public boolean containsResource(String id) {
		return ontmodel.containsResource(ontmodel.getResource(id));
	}

	// Membership queries
	public KBObject getClassOfInstance(KBObject obj) {
		OntClass cl = null;
		if (!checkNulls(obj))
			return null;
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		Resource node = ind.getRDFType(true);
		if (node.canAs(OntClass.class)) {
			cl = (OntClass) node.as(OntClass.class);
		}
		KBObject cls = new KBObjectJena(cl);
		return cls;
	}

	public ArrayList<KBObject> getAllClassesOfInstance(KBObject obj, boolean direct) {
		if (!checkNulls(obj))
			return null;
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		if (ind == null) {
			ind = ontmodel.getIndividual(obj.getID());
		}
		if (ind == null) {
			return list;
		}
		for (Iterator<Resource> i = ind.listRDFTypes(direct); i.hasNext();) {
			Resource node = (Resource) i.next();
			if (node.canAs(OntClass.class)) {
				OntClass cl = (OntClass) node.as(OntClass.class);
				list.add(new KBObjectJena(cl));
			}
		}
		return list;
	}

	public ArrayList<KBObject> getInstancesOfClass(KBObject cls, boolean direct) {
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		if (!checkNulls(cls, direct))
			return list;
		OntClass cl = (OntClass) cls.getInternalNode();
		for (Iterator<? extends OntResource> it = cl.listInstances(direct); it.hasNext();) {
			list.add(new KBObjectJena((RDFNode) it.next()));
		}
		return list;
	}

	// Property queries
	public ArrayList<KBObject> getPropertyValues(KBObject obj, KBObject prop) {
		ArrayList<KBObject> v = new ArrayList<KBObject>();
		if (!checkNulls(obj, prop))
			return v;
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		for (NodeIterator it = ind.listPropertyValues((Property) prop.getInternalNode()); it
				.hasNext();) {
			RDFNode node = (RDFNode) it.next();
			if (node != null) {
				KBObjectJena vobj = new KBObjectJena(node);
				v.add(vobj);
			}
		}
		return v;
	}

	public KBObject getPropertyValue(KBObject obj, KBObject prop) {
		if (!checkNulls(obj, prop))
			return null;
		ArrayList<KBObject> list = getPropertyValues(obj, prop);
		if (list.size() > 0) {
			return (KBObject) list.get(0);
		}
		return null;
	}

	public ArrayList<KBObject> getDatatypePropertyValues(KBObject obj, KBObject prop) {
		ArrayList<KBObject> v = new ArrayList<KBObject>();
		if (!checkNulls(obj, prop))
			return v;
		DatatypeProperty p = ontmodel.getDatatypeProperty(prop.getID());
		if (p == null) {
			return v;
		}
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		for (NodeIterator it = ind.listPropertyValues(p); it.hasNext();) {
			RDFNode node = (RDFNode) it.next();
			if (node != null) {
				v.add(new KBObjectJena(node));
			}
		}
		return v;
	}

	public KBObject getDatatypePropertyValue(KBObject obj, KBObject prop) {
		if (!checkNulls(obj, prop))
			return null;
		ArrayList<KBObject> list = getDatatypePropertyValues(obj, prop);
		if (list.size() == 0) {
			return new KBObjectJena(null, true);
			// return null;
		}
		return list.get(0);
	}

	public ArrayList<KBObject> getSubPropertiesOf(KBObject prop, boolean direct) {
		ArrayList<KBObject> subProps = new ArrayList<KBObject>();
		if (!checkNulls(prop))
			return subProps;
		OntProperty p = ontmodel.getOntProperty(prop.getID());
		if (!checkNulls(p)) {
			return subProps;
		}
		for (Iterator<? extends OntProperty> it = p.listSubProperties(direct); it.hasNext();) {
			Resource subprop = (Resource) it.next();
			if (!subprop.getURI().equals(prop.getID())) {
				subProps.add(new KBObjectJena(subprop));
			}
		}
		return subProps;
	}

  public ArrayList<KBObject> getSuperPropertiesOf(KBObject prop, boolean direct) {
    ArrayList<KBObject> superProps = new ArrayList<KBObject>();
    if (!checkNulls(prop))
      return superProps;
    OntProperty p = ontmodel.getOntProperty(prop.getID());
    if (!checkNulls(p)) {
      return superProps;
    }
    for (Iterator<? extends OntProperty> it = p.listSuperProperties(direct); it
        .hasNext();) {
      Resource superprop = (Resource) it.next();
      if (!superprop.getURI().equals(prop.getID())) {
        superProps.add(new KBObjectJena(superprop));
      }
    }
    return superProps;
  }
	 
	public void setPropertyValue(KBObject obj, KBObject prop, KBObject value) {
		if (!checkNulls(obj, prop, value))
			return;
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		Property p = (Property) prop.getInternalNode();
		if (value.isLiteral() && value.getInternalNode() == null) {
			ind.setPropertyValue(p, ontmodel.createTypedLiteral(value.getValue()));
		} else {
			ind.setPropertyValue(p, (RDFNode) value.getInternalNode());
		}
	}

	public void addPropertyValue(KBObject obj, KBObject prop, KBObject value) {
		if (!checkNulls(obj, prop, value))
			return;
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		Property p = (Property) prop.getInternalNode();
		if (value.isLiteral() && value.getInternalNode() == null) {
			ind.addProperty(p, ontmodel.createTypedLiteral(value.getValue()));
		} else {
			ind.addProperty(p, (RDFNode) value.getInternalNode());
		}
	}

	public KBObject createLiteral(Object literal) {
		if (!checkNulls(literal))
			return null;
		return new KBObjectJena(ontmodel.createTypedLiteral(literal));
	}

	public KBObject createXSDLiteral(String literal, String xsdtype) {
		if (!checkNulls(literal))
			return null;
		if (xsdtype == null) {
			return new KBObjectJena(ontmodel.createLiteral(literal));
		} else {
			try {
				return new KBObjectJena(ontmodel.createTypedLiteral(literal, xsdtype));
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public ArrayList<KBTriple> genericTripleQuery(KBObject subj, KBObject pred, KBObject obj) {
		ArrayList<KBTriple> list = new ArrayList<KBTriple>();
		// Check that predicate (if provided) is a property
		if (pred != null && !((Resource) pred.getInternalNode()).canAs(Property.class))
			return list;
		Individual s = subj != null ? getIndividual((Resource) subj.getInternalNode()) : null;
		Property p = pred != null ? (Property) pred.getInternalNode() : null;
		RDFNode o = null;
		Model posit = null;
		if (obj != null && obj.isLiteral()) {
			posit = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			o = posit.createTypedLiteral(obj.getValue());
		} else if (obj != null) {
			o = getIndividual((Resource) obj.getInternalNode());
		}

		StmtIterator sts = null;
		if (posit == null) {
			sts = ontmodel.listStatements(s, p, o);
		} else {
			sts = ontmodel.listStatements(s, p, o, posit);
		}
		for (; sts.hasNext();) {
			Statement st = (Statement) sts.next();
			KBObject newSubject = new KBObjectJena(st.getSubject());
			KBObject newPredicate = new KBObjectJena(st.getPredicate());
			KBObject newObject = new KBObjectJena(st.getObject());
			list.add(new KBTripleJena(newSubject, newPredicate, newObject));
		}
		return list;
	}

	public ArrayList<ArrayList<SparqlQuerySolution>> sparqlQuery(String queryString) {
		ArrayList<ArrayList<SparqlQuerySolution>> list = new ArrayList<ArrayList<SparqlQuerySolution>>();
		Query query = QueryFactory.create(queryString);
		ArrayList<String> vars = new ArrayList<String>(query.getResultVars());
		QueryExecution qexec = QueryExecutionFactory.create(query, ontmodel);
		try {
			ResultSet results = qexec.execSelect();
			for (; results.hasNext();) {
				QuerySolution soln = results.nextSolution();
				ArrayList<SparqlQuerySolution> inner = new ArrayList<SparqlQuerySolution>();
				for (String variableName : vars) {
					RDFNode x = soln.get(variableName);
					// System.out.println(soln.toString());
					KBObject item = null;
					if (x != null) {
						if (x.isLiteral()) {
							Literal l = soln.getLiteral(variableName);
							item = new KBObjectJena(l.getValue(), true);
						} else {
							Resource r = soln.getResource(variableName);
							item = new KBObjectJena(r);
						}
					}
					SparqlQuerySolution sqs = new SparqlQuerySolution(variableName, item);
					inner.add(sqs);
				}
				list.add(inner);
			}

		} finally {
			qexec.close();
		}
		return list;
	}

	public ArrayList<KBObject> getSubClasses(KBObject cls, boolean direct_only) {

		ArrayList<KBObject> list = new ArrayList<KBObject>();
		OntClass cl = (OntClass) cls.getInternalNode();
		for (Iterator<OntClass> it = cl.listSubClasses(direct_only); it.hasNext();) {
			list.add(new KBObjectJena((Resource) it.next()));
		}

		return list;
	}

	public ArrayList<KBObject> getSuperClasses(KBObject cls, boolean direct_only) {
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		OntClass cl = (OntClass) cls.getInternalNode();
		for (Iterator<OntClass> it = cl.listSuperClasses(direct_only); it.hasNext();) {
			list.add(new KBObjectJena((Resource) it.next()));
		}
		return list;
	}

	public ArrayList<KBObject> getListItems(KBObject list) {
		RDFNode listNode = (RDFNode) list.getInternalNode();

		ArrayList<KBObject> items = new ArrayList<KBObject>();
		if (listNode != null && listNode.canAs(RDFList.class)) {
			RDFList rdfitems = (RDFList) listNode.as(RDFList.class);
			if (rdfitems != null && rdfitems.size() > 0) {
				for (Iterator<RDFNode> it = rdfitems.iterator(); it.hasNext();) {
					items.add(new KBObjectJena(it.next()));
				}
			}
		}
		return items;
	}

	public boolean isA(KBObject obj, KBObject cls) {
		if (!checkNulls(obj, cls))
			return false;
		boolean val = false;
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		if (ind == null) {
			return false;
		}
		for (Iterator<Resource> i = ind.listRDFTypes(false); i.hasNext();) {
			if (i.next().toString().equals(cls.getID())) {
				val = true;
				break;
			}
		}
		return val;
	}

	public boolean hasSubClass(KBObject cls1, KBObject cls2) {
		if (!checkNulls(cls1, cls2))
			return false;
		boolean val = false;
		OntClass cl1 = (OntClass) cls1.getInternalNode();
		OntClass cl2 = (OntClass) cls2.getInternalNode();
		if (cl1 != null && cl2 != null) {
			if (cls1.getID().equals(cls2.getID())) {
				val = true;
			} else if (cl1.hasSubClass(cl2)) {
				val = true;
			}
		}
		return val;
	}

	public boolean hasSuperClass(KBObject cls1, KBObject cls2) {
		if (!checkNulls(cls1, cls2))
			return false;
		boolean val = false;
		OntClass cl1 = (OntClass) cls1.getInternalNode();
		OntClass cl2 = (OntClass) cls2.getInternalNode();
		if (cl1 != null && cl2 != null) {
			if (cls1.getID().equals(cls2.getID())) {
				val = true;
			} else if (cl1.hasSuperClass(cl2)) {
				val = true;
			}
		}
		return val;
	}

	public KBObject createIndividual(String id) {
		Individual ind = ontmodel.createIndividual(ontmodel.getResource(id));
		if (ind == null)
			return null;
		return new KBObjectJena(ind);
	}
	
	public KBObject createObjectOfClass(String id, KBObject cls) {
		if (!checkNulls(cls))
			return null;
		Individual ind = ontmodel.createIndividual(id, (Resource) cls.getInternalNode());
		if (ind == null)
			return null;
		return new KBObjectJena(ind);
	}

	public void deleteObject(KBObject obj, boolean subj_props_remove, boolean obj_props_remove) {
		if (!checkNulls(obj, obj.getInternalNode()))
			return;
		// First only delete the object and its properties
		if (subj_props_remove)
			ontmodel.removeAll((Resource) obj.getInternalNode(), null, null);
		// Then delete all properties that have this object as the value
		if (obj_props_remove)
			ontmodel.removeAll(null, null, (RDFNode) obj.getInternalNode());
	}

	public void deleteObjectOnly(KBObject obj) {
		if (!checkNulls(obj))
			return;
		// Get all owlObjectProperties of this object, and delete them

	}

	public KBObject createList(ArrayList<KBObject> items) {
		RDFNode[] nodes = new RDFNode[items.size()];
		int i = 0;
		for (KBObject item : items) {
			if (item == null)
				continue;
			nodes[i] = (RDFNode) item.getInternalNode();
			i++;
		}
		RDFList list = ontmodel.createList(nodes);
		return new KBObjectJena(list);
	}

	public KBObject createParsetypeLiteral(String xml) {
		if (!checkNulls(xml))
			return null;
		Literal lit = ontmodel.createLiteral(xml, true);
		return new KBObjectJena(lit);
	}

	public void importFrom(KBAPI kb) {
		if (!checkNulls(kb))
			return;
		KBAPIJena japi = (KBAPIJena) kb;
		ontmodel.addSubModel(japi.ontmodel);
	}

	public void copyFrom(KBAPI kb) {
		if (!checkNulls(kb))
			return;
		KBAPIJena japi = (KBAPIJena) kb;
		ontmodel.add(japi.ontmodel.listStatements());
		//ontmodel.addSubModel(japi.ontmodel, true);
	}

	public void writeRDF(PrintStream ostr) {
		RDFWriter rdfWriter = ontmodel.getWriter("RDF/XML-ABBREV");
		rdfWriter.setProperty("showXmlDeclaration", "true");
		rdfWriter.setProperty("tab", "6");
		rdfWriter.setProperty("xmlbase", this.url);
		rdfWriter.write(ontmodel.getBaseModel(), ostr, this.url);
	}

	public String toRdf(boolean showheader, String base) {
		return toRdf(showheader, this.url);
	}

	public String toRdf(boolean showheader) {
		StringWriter out = new StringWriter();
		RDFWriter rdfWriter = ontmodel.getWriter("RDF/XML");
		rdfWriter.setProperty("showXmlDeclaration", showheader);
		rdfWriter.setProperty("tab", "6");
		rdfWriter.setProperty("xmlbase", this.url);
		rdfWriter.write(ontmodel.getBaseModel(), out, this.url);
		return out.toString();
	}

	public String toAbbrevRdf(boolean showheader) {
		return toAbbrevRdf(showheader, this.url);
	}

	public String toAbbrevRdf(boolean showheader, String base) {
		StringWriter out = new StringWriter();
		RDFWriter rdfWriter = ontmodel.getWriter("RDF/XML-ABBREV");
		rdfWriter.setProperty("showXmlDeclaration", showheader);
		rdfWriter.setProperty("tab", "6");
		rdfWriter.setProperty("xmlbase", base);
		rdfWriter.write(ontmodel.getBaseModel(), out, this.url);
		return out.toString();
	}

	Individual getIndividual(Resource node) {
		if (node != null && node.canAs(Individual.class)) {
			return (Individual) node.as(Individual.class);
		}
		return null;
	}

	public void addTriples(ArrayList<KBTriple> triples) {
		if (!checkNulls(triples)) {
			return;
		}
		for (KBTriple triple : triples) {
			addTriple(triple);
		}
	}

	public KBTriple addTriple(KBTriple triple) {
		Statement ontst = this.getOntStatementFromTriple(this.ontmodel, triple);
		if (ontst != null) {
			ontmodel.add(ontst);
			return triple;
		}
		return null;
	}
	
	/*
	 * This is to return a list of all classes that can have this property
	 * -- internally the domains are stored as owl:UnionOf [ dom1 dom2 .. ]
	 */
	public ArrayList<KBObject> getPropertyDomainsDisjunctive(KBObject prop) {
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		OntProperty p = ontmodel.getOntProperty(prop.getID());
		if(p == null) 
			return list;
		
		Resource curdom = p.getDomain();
		if(curdom != null) {
			if(curdom.canAs(UnionClass.class)) {
				// If this is a union class domain, return elements of the list
				UnionClass dom = (UnionClass) curdom.as(UnionClass.class);
				list = this.getListItems(new KBObjectJena(dom.getOperands()));
			}
			else {
				// Else just return the simple resource domain
				list.add(new KBObjectJena(curdom));
			}
		} 
		return list;
	}
	
	public ArrayList<KBObject> getPropertyDomains(KBObject prop) {
		ArrayList<KBObject> listOfDomains = new ArrayList<KBObject>();
		OntProperty p = ontmodel.getOntProperty(prop.getID());
		if (p != null) {
			for (Iterator<? extends OntResource> i = p.listDomain(); i.hasNext();) {
				OntResource domain = i.next();
				if (domain != null)
					listOfDomains.add(new KBObjectJena(domain));
			}
		}
		return listOfDomains;
	}

	public KBObject getPropertyDomain(KBObject prop) {
		if (!checkNulls(prop))
			return null;
		OntProperty p = ontmodel.getOntProperty(prop.getID());
		if (p != null) {
			OntResource domain = p.getDomain();
			if (domain != null)
				return new KBObjectJena(domain);
		}
		return null;
	}

	public ArrayList<KBObject> getPropertyRanges(KBObject prop) {
		ArrayList<KBObject> listOfRanges = new ArrayList<KBObject>();
		OntProperty p = ontmodel.getOntProperty(prop.getID());
		if (p != null) {
			for (Iterator<? extends OntResource> i = p.listRange(); i.hasNext();) {
				OntResource domain = i.next();
				if (domain != null)
					listOfRanges.add(new KBObjectJena(domain));
			}
		}
		return listOfRanges;
	}

	public KBObject getPropertyRange(KBObject prop) {
		OntProperty p = ontmodel.getOntProperty(prop.getID());
		if (p != null) {
			OntResource range = p.getRange();
			if (range != null)
				return new KBObjectJena(range);
		}
		return null;
	}

	public void addClassForInstance(KBObject obj, KBObject cls) {
		Individual ind = ontmodel.getIndividual(obj.getID());
		OntClass clsobj = (OntClass) cls.getInternalNode();
		if (!checkNulls(ind, clsobj)) {
			return;
		}
		ind.addRDFType(clsobj);
	}

	public KBObject createClass(String id) {
		OntClass clsobj = ontmodel.createClass(id);
		return new KBObjectJena(clsobj);
	}

	public KBObject createClass(String id, String parentid) {
		OntClass clsobj = ontmodel.createClass(id);
		Resource pobj = ontmodel.getResource(parentid);
		if (pobj != null)
			clsobj.addSuperClass(pobj);
		return new KBObjectJena(clsobj);
	}
	
	public boolean setSuperClass(String id, String parentid) {
		OntClass clsobj = ontmodel.getOntClass(id);
		Resource pobj = ontmodel.getResource(parentid);
		if (pobj != null) {
			clsobj.setSuperClass(pobj);
			return true;
		}
		return false;
	}

	public KBObject createObjectProperty(String id) {
		OntProperty propobj = ontmodel.createObjectProperty(id);
		return new KBObjectJena(propobj);
	}

	public KBObject createObjectProperty(String id, String parentid) {
		OntProperty propobj = ontmodel.createObjectProperty(id);
		Property parentobj = ontmodel.getProperty(parentid);
		if (parentobj != null)
			propobj.addSuperProperty(parentobj);
		return new KBObjectJena(propobj);
	}

	public KBObject createDatatypeProperty(String id) {
		OntProperty propobj = ontmodel.createDatatypeProperty(id);
		return new KBObjectJena(propobj);
	}

	public KBObject createDatatypeProperty(String id, String parentid) {
		OntProperty propobj = ontmodel.createDatatypeProperty(id);
		Property parentobj = ontmodel.getProperty(parentid);
		if (parentobj != null)
			propobj.addSuperProperty(parentobj);
		return new KBObjectJena(propobj);
	}

	public boolean setPropertyDomain(String propid, String domainid) {
		OntProperty pobj = ontmodel.getOntProperty(propid);
		Resource domobj = ontmodel.getResource(domainid);
		if (pobj != null && domobj != null)
			pobj.setDomain(domobj);
		return true;
	}
	
	public boolean addPropertyDomain(String propid, String domainid) {
		OntProperty pobj = ontmodel.getOntProperty(propid);
		Resource domobj = ontmodel.getResource(domainid);
		if (pobj != null && domobj != null)
			pobj.addDomain(domobj);
		return true;
	}
	
	public boolean addPropertyDomainDisjunctive(String propid, String domainid) {
		OntProperty pobj = ontmodel.getOntProperty(propid);
		Resource domobj = ontmodel.getResource(domainid);
		if(pobj == null  || domobj == null)
			return false;
		
		Resource curdom = pobj.getDomain();
		if(curdom == null) {
			// If there is no domain yet, create a simple resource domain
			pobj.addDomain(domobj);
			return true;
		}
		else {
			// If there is a domain already. Check if it is a unionclass already
			// If not, create a new union class and add current resource to it
			UnionClass dom;
			if(curdom.canAs(UnionClass.class)) {
				// add resource to the current union class domain
				dom = (UnionClass) curdom.as(UnionClass.class);
				dom.getOperands().add(domobj);
			}
			else {
				// remove the simple resource domain
				pobj.removeDomain(curdom);
				// create a union class and add the simple domain to it
				dom = ontmodel.createUnionClass(null, ontmodel.createList());
				dom.setOperands(dom.getOperands().with(curdom));
				// add the passed in domain
				dom.getOperands().add(domobj);
				// set union class as the domain
				pobj.addDomain(dom);
			}
			return true;
		}
	}
	
	public boolean removePropertyDomain(String propid, String domainid) {
		OntProperty pobj = ontmodel.getOntProperty(propid);
		Resource domobj = ontmodel.getResource(domainid);
		if (pobj != null && domobj != null)
			pobj.removeDomain(domobj);
		return true;
	}

	public boolean removePropertyDomainDisjunctive(String propid, String domainid) {
		OntProperty pobj = ontmodel.getOntProperty(propid);
		Resource domobj = ontmodel.getResource(domainid);
		if(pobj == null  || domobj == null)
			return false;
		Resource curdom = pobj.getDomain();
		if(curdom != null && curdom.canAs(UnionClass.class)) {
			UnionClass union = (UnionClass) curdom.as(UnionClass.class);
			// Remove domain from union class operand list
			union.setOperands(union.getOperands().remove(domobj));
			// If there is only 1 object left in operand list, convert list to single object
			if(union.getOperands().size() == 1) {
				// Get the last object left in list
				Resource opdom = (Resource) union.getOperands().getHead();
				// Remove all elements from list
				union.getOperands().removeList();
				// Remove all statements about union class 
				ontmodel.removeAll(union, null, null);
				// Remove union class as domain
				pobj.removeDomain(union);
				// add operand domain
				pobj.addDomain(opdom);
			}
		}
		else if(curdom != null) {
			pobj.removeDomain(domobj);
		}
		return true;
	}
	
	public boolean setPropertyRange(String propid, String rangeid) {
		OntProperty pobj = ontmodel.getOntProperty(propid);
		Resource rangeobj = ontmodel.getResource(rangeid);
		if (pobj != null && rangeobj != null)
			pobj.setRange(rangeobj);
		return true;
	}

	public ArrayList<KBObject> getAllClasses() {
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		for (Iterator<OntClass> i = ontmodel.listClasses(); i.hasNext();) {
			list.add(new KBObjectJena((RDFNode) i.next()));
		}
		return list;
	}

	public ArrayList<KBObject> getAllDatatypeProperties() {
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		for (Iterator<DatatypeProperty> i = ontmodel.listDatatypeProperties(); i.hasNext();) {
			list.add(new KBObjectJena((RDFNode) i.next()));
		}
		return list;
	}

	public ArrayList<KBObject> getAllObjectProperties() {
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		for (Iterator<ObjectProperty> i = ontmodel.listObjectProperties(); i.hasNext();) {
			list.add(new KBObjectJena((RDFNode) i.next()));
		}
		return list;
	}

	public ArrayList<KBObject> getAllProperties() {
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		for (Iterator<OntProperty> i = ontmodel.listAllOntProperties(); i.hasNext();) {
			list.add(new KBObjectJena((RDFNode) i.next()));
		}
		return list;
	}

	public ArrayList<KBObject> getPropertiesOfClass(KBObject cls, boolean direct) {
		ArrayList<KBObject> list = new ArrayList<KBObject>();
		OntClass cl = (OntClass) cls.getInternalNode();
		if (cl != null) {
			for (Iterator<OntProperty> i = cl.listDeclaredProperties(direct); i.hasNext();) {
				list.add(new KBObjectJena((RDFNode) i.next()));
			}
		}
		return list;
	}

	public boolean isObjectProperty(KBObject prop) {
		OntProperty p = ontmodel.getOntProperty(prop.getID());
		if (p != null)
			return p.isObjectProperty();
		return false;
	}

	public boolean isDatatypeProperty(KBObject prop) {
		OntProperty p = ontmodel.getOntProperty(prop.getID());
		if (p != null)
			return p.isDatatypeProperty();
		return false;
	}

	public void removeTriple(KBTriple triple) {
		if (!checkNulls(triple))
			return;
		Statement ontst = this.getOntStatementFromTriple(this.ontmodel, triple);
		if (ontst != null) {
			this.ontmodel.remove(ontst);
		}
		for (Graph subg : this.ontmodel.getSubGraphs()) {
			subg.delete(ontst.asTriple());
		}
	}
	
	public void removeAllTriples() {
		this.ontmodel.removeAll();
	}

	private Statement getOntStatementFromTriple(Model model, KBTriple triple) {
		if (!checkNulls(model, triple))
			return null;
		KBObject subj = triple.getSubject();
		KBObject pred = triple.getPredicate();
		KBObject obj = triple.getObject();
		if (subj != null && pred != null && obj != null) {
			Property predobj = model.getProperty(pred.getID());
			Resource subobj = (Resource) subj.getInternalNode();
			RDFNode obobj = (RDFNode) obj.getInternalNode();
			if(subobj == null) {
				if(subj.isAnonymous()) {
					subobj = model.createResource(new AnonId(subj.getID()));
				}
				else {
					subobj = model.getResource(subj.getID());
				}
			}
			if(obobj == null) {
				if (obj.isLiteral() && obj.getValue() != null) {
					if(obj.getDataType() != null)
						obobj = model.createTypedLiteral(obj.getValue(), obj.getDataType());
					else {
						obobj = model.createLiteral(obj.getValue().toString());
					}
				} 
				else {
					obobj = model.getResource(obj.getID());
				}
			}
			return model.createStatement(subobj, predobj, obobj);

		}
		return null;
	}

	public KBTriple addTriple(KBObject subj, KBObject pred, KBObject obj) {
		return this.addTriple(new KBTripleJena(subj, pred, obj));
	}

	public void removeTriple(KBObject subj, KBObject pred, KBObject obj) {
		this.removeTriple(new KBTripleJena(subj, pred, obj));
	}

	public void createImport(String ontid, String importurl) {
		Ontology ont = ontmodel.getOntology(ontid);
		Resource imp = ontmodel.getResource(importurl);
		if (ont == null) {
			ont = ontmodel.createOntology(ontid);
		}
		if (imp == null) {
			imp = ontmodel.createResource(importurl);
		}
		if (ont != null && imp != null) {
			ont.addImport(imp);
		}
	}

	public ArrayList<String> getImports(String ontid) {
		ArrayList<String> imports = new ArrayList<String>();
		Ontology ont = ontmodel.getOntology(ontid);
		if (ont != null) {
			for (Iterator<OntResource> i = ont.listImports(); i.hasNext();) {
				imports.add(i.next().toString());
			}
		}
		return imports;
	}

	public void removeImport(String ontid, String importurl) {
		Ontology ont = ontmodel.getOntology(ontid);
		Resource imp = ontmodel.getResource(importurl);
		if (ont != null && imp != null && ont.imports(imp)) {
			ont.removeImport(imp);
			if (getImports(ontid).size() == 0) {
				this.deleteObject(new KBObjectJena(ont), true, true);
			}
		}
	}

	public void clearImportCache(String url) {
		createNewCacheModel(url, false);
	}

	private OntModel createNewCacheModel(String url, boolean closeOld) {
		FileManager fm = ontmodel.getDocumentManager().getFileManager();
		Model oldmodel = fm.getFromCache(url);
		if (oldmodel != null) {
			fm.removeCacheModel(url);
			OntModel impmodel = ModelFactory.createOntologyModel(this.modelSpec);
			try {
				impmodel.read(url);
				fm.addCacheModel(url, impmodel);
				if (closeOld) {
					oldmodel.removeAll();
				}
			} catch (Exception e) {
				// File probably has been deleted.. ignore
			}
			return impmodel;
		}
		return null;
	}

	public void setLocal(String uriPrefix, String localDirectory) {
		ontmodel.getDocumentManager().addAltEntry(uriPrefix, localDirectory);
	}

  public String getLabel(KBObject obj) {
    if (!checkNulls(obj))
      return null;
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    if (ind == null) {
      return null;
    }

    RDFNode n = ind.getPropertyValue(RDFS.label);
    if (n != null && n.isLiteral()) {
      return (String) n.asNode().getLiteralValue();
    }
    return null;
  }

  public void setLabel(KBObject obj, String label) {
    if (!checkNulls(obj, label))
      return;
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    if (ind == null) {
      return;
    }
    ind.setPropertyValue(RDFS.label, ontmodel.createTypedLiteral(label));
  }

	public String getComment(KBObject obj) {
		if (!checkNulls(obj))
			return null;
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		if (ind == null) {
			return null;
		}

		RDFNode n = ind.getPropertyValue(RDFS.comment);
		if (n != null && n.isLiteral()) {
			return (String) n.asNode().getLiteralValue();
		}
		return null;
	}

	public void setComment(KBObject obj, String comment) {
		if (!checkNulls(obj, comment))
			return;
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		if (ind == null) {
			return;
		}
		ind.setPropertyValue(RDFS.comment, ontmodel.createTypedLiteral(comment));
	}

	public ArrayList<String> getAllComments(KBObject obj) {
		if (!checkNulls(obj))
			return null;
		Individual ind = getIndividual((Resource) obj.getInternalNode());
		if (ind == null) {
			return null;
		}

		ArrayList<String> comments = new ArrayList<String>();
		for (NodeIterator it = ind.listPropertyValues(RDFS.comment); it.hasNext();) {
			RDFNode n = (RDFNode) it.next();
			if (n != null && n.isLiteral()) {
				comments.add((String) n.asNode().getLiteralValue());
			}
		}
		return comments;
	}

	public boolean hasSubProperty(KBObject prop1, KBObject prop2) {
		if (!checkNulls(prop1, prop2))
			return false;
		boolean val = false;
		OntProperty ontProperty1 = (OntProperty) prop1.getInternalNode();
		OntProperty ontProperty2 = (OntProperty) prop2.getInternalNode();
		if (ontProperty1 != null && ontProperty2 != null) {
			if (prop1.getID().equals(prop2.getID())) {
				val = true;
			} else if (ontProperty2.hasSuperProperty(ontProperty1, false)) {
				val = true;
			}
		}
		return val;
	}

	public void writeN3(PrintStream ostr) {
		RDFWriter n3Writer = ontmodel.getWriter("N3-PP");
		n3Writer.write(ontmodel.getBaseModel(), ostr, this.url);
	}

	public String toN3() {
		return toN3(this.url);
	}

	public String toN3(String base) {
		StringWriter out = new StringWriter();
		RDFWriter rdfWriter = ontmodel.getWriter("N3-PP");
		rdfWriter.write(ontmodel.getBaseModel(), out, base);
		return out.toString();
	}

	public ArrayList<KBTriple> getAllTriples() {
		ArrayList<KBTriple> list = new ArrayList<KBTriple>();
		for (Iterator<Statement> sts = this.ontmodel.listStatements(); sts.hasNext();) {
			Statement st = (Statement) sts.next();
			KBObject newSubject = new KBObjectJena(st.getSubject());
			KBObject newPredicate = new KBObjectJena(st.getPredicate());
			KBObject newObject = new KBObjectJena(st.getObject());
			list.add(new KBTripleJena(newSubject, newPredicate, newObject));
		}
		return list;
	}
	
	@Override
	public void end() {
		if(ontmodel != null) {
			ontmodel.getBaseModel().close();
			ontmodel.close();
		}
	}

	@Override
	public boolean save() {
		if (this.url == null)
			return false;

		if (this.usetdb && tdbstore != null) {
			TDB.sync(ontmodel.getBaseModel());
			// System.out.println("Commit to tdb store");
			// tdbstore.commit();
			return true;
		} else {
			// Store in file
			String fileuri = LocationMapper.get().altMapping(this.url);
			try {
				File f = new File(new URL(fileuri).getFile());
				try {
				    	if(!f.getParentFile().exists())
				    	    f.getParentFile().mkdirs();
					FileWriter fout = new FileWriter(f);
					BufferedWriter out = new BufferedWriter(fout);
					out.write(this.toAbbrevRdf(true));
					out.close();
					fout.close();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	@Override
	public boolean saveAs(String url) {
		if (this.usetdb && tdbstore != null) {
			tdbstore.removeNamedModel(url);
			tdbstore.addNamedModel(url, ontmodel.getBaseModel());
			TDB.sync(ontmodel.getBaseModel());
			TDB.sync(tdbstore);
			return true;
		}
		else {
			this.url = url;
			return this.save();
		}
	}

	@Override
	public boolean delete() {
		if (this.url == null)
			return false;

		if (this.usetdb && tdbstore != null) {
			tdbstore.removeNamedModel(this.url);
			TDB.sync(tdbstore);
			return true;
		} else {
			// Delete the file
			String fileuri = LocationMapper.get().altMapping(this.url);
			try {
				File f = new File(new URL(fileuri).getFile());
				f.setWritable(true);
				f.delete();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean checkNulls(Object... vars) {
		for (Object var : vars) {
			if (var == null) {
				StackTraceElement[] stack = Thread.currentThread().getStackTrace();
				StackTraceElement method = stack[2];
				StackTraceElement caller = stack[3];
				String warnstr = method.getMethodName() + ": " + Arrays.toString(vars);
				warnstr += ", called from " + caller.getFileName() + ":" + caller.getLineNumber();
				Logger.getLogger(this.getClass()).warn(warnstr);
				return false;
			}
		}
		return true;
	}
}
