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

package edu.isi.wings.ontapi;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.wings.ontapi.rules.KBRule;
import edu.isi.wings.ontapi.rules.KBRuleList;

/**
 * @author varunr
 *         <p/>
 *         KBAPI Interface : Needs to be implemented (see KBAPIJena for an
 *         example) Uses KBObject interface (see KBObjectJena for an example)
 *         <p/>
 *         Also, check OntFactory/OntSpec to see the list of constants used
 */
public interface KBAPI {
	public void useRawModel();

	public void useBaseModel();

	// Query for Ontology elements
	public KBObject getResource(String id);

	public KBObject getConcept(String id);

	public KBObject getIndividual(String id);

	public KBObject getProperty(String id);

	public KBObject getAnnotationProperty(String id);
	 
	public ArrayList<KBObject> getAllClasses();

	public ArrayList<KBObject> getAllDatatypeProperties();

	public ArrayList<KBObject> getAllObjectProperties();

	public ArrayList<KBObject> getAllProperties();

	public boolean containsResource(String id);

	// Classificiation
	public KBObject getClassOfInstance(KBObject obj);

	public ArrayList<KBObject> getAllClassesOfInstance(KBObject obj, boolean direct);

	public ArrayList<KBObject> getInstancesOfClass(KBObject cls, boolean direct);

	public ArrayList<KBObject> getPropertiesOfClass(KBObject cls, boolean direct);

	public void addClassForInstance(KBObject obj, KBObject cls);
	
	public void setClassForInstance(KBObject obj, KBObject cls);

	// Property Queries/Setting
	public KBObject getPropertyValue(KBObject obj, KBObject prop);

	public ArrayList<KBObject> getPropertyValues(KBObject obj, KBObject prop);

	public KBObject getDatatypePropertyValue(KBObject obj, KBObject prop);

	public ArrayList<KBObject> getDatatypePropertyValues(KBObject obj, KBObject prop);

	public void setPropertyValue(KBObject obj, KBObject prop, KBObject value);

	public void addPropertyValue(KBObject obj, KBObject prop, KBObject value);

	public boolean isObjectProperty(KBObject obj);

	public boolean isDatatypeProperty(KBObject obj);

	public KBObject getPropertyDomain(KBObject prop);

	public KBObject getPropertyRange(KBObject prop);

	public boolean isFunctionalProperty(KBObject prop);

	public ArrayList<KBObject> getPropertyDomains(KBObject prop);
	
	public ArrayList<KBObject> getPropertyDomainsDisjunctive(KBObject prop);

	public ArrayList<KBObject> getPropertyRanges(KBObject prop);

	// Shortcut to get Label properties
	public String getLabel(KBObject obj);
	
	public void setLabel(KBObject obj, String label);
	
	// Shortcut to get Comment properties
	public String getComment(KBObject obj);

	public void setComment(KBObject obj, String comment);

	public ArrayList<String> getAllComments(KBObject obj);

	// Generic Triple query (return all possible values of input KBObjects which
	// are null)
	// -- All inputs null : No need to handle
	// -- Returns an ArrayList of KBObject[] Arrays

	public ArrayList<KBTriple> genericTripleQuery(KBObject subj, KBObject pred, KBObject obj);

	public ArrayList<KBTriple> getAllTriples();

	public ArrayList<ArrayList<SparqlQuerySolution>> sparqlQuery(String queryString);

	public void addTriples(ArrayList<KBTriple> statements);

	public KBTriple addTriple(KBTriple triple);

	public KBTriple addTriple(KBObject subj, KBObject pred, KBObject obj);

	public void removeTriple(KBTriple triple);

	public void removeTriple(KBObject subj, KBObject pred, KBObject obj);
	
	public void removeAllTriples();

	// isA, subClasses, subProperties
	public boolean isA(KBObject obj, KBObject cls);

	public boolean hasSubClass(KBObject cls1, KBObject cls2);

	public boolean hasSuperClass(KBObject cls1, KBObject cls2);

	public boolean hasSubProperty(KBObject prop1, KBObject prop2);

	public ArrayList<KBObject> getSubClasses(KBObject cls, boolean direct_only);

	public ArrayList<KBObject> getSuperClasses(KBObject cls, boolean direct_only);

	public ArrayList<KBObject> getSubPropertiesOf(KBObject prop, boolean direct);
	
	public ArrayList<KBObject> getSuperPropertiesOf(KBObject prop, boolean direct);

	// Creation/Deletion
	public KBObject createClass(String id);

	public KBObject createClass(String id, String parentid);
	
	public boolean setSuperClass(String id, String parentid);

	public KBObject createIndividual(String id);
	
	public KBObject createObjectOfClass(String id, KBObject cls);

	public KBObject createObjectProperty(String propid);

	public KBObject createObjectProperty(String propid, String parentpropid);

	public KBObject createDatatypeProperty(String propid);

	public KBObject createDatatypeProperty(String propid, String parentpropid);

	public boolean setPropertyDomain(String propid, String domainid);
	
	public boolean addPropertyDomain(String propid, String domainid);
	
	public boolean addPropertyDomainDisjunctive(String propid, String domainid);
	
	public boolean removePropertyDomain(String propid, String domainid);
	
	public boolean removePropertyDomainDisjunctive(String propid, String domainid);

	public boolean setPropertyRange(String propid, String rangeid);

	public KBObject createLiteral(Object literal);

	public KBObject createXSDLiteral(String literal, String xsdtype);

	public KBObject createParsetypeLiteral(String xml);

	public void deleteObject(KBObject obj, boolean subjprops_remove, boolean objprops_remove);

	// Lists
	public KBObject createList(ArrayList<KBObject> items);

	public ArrayList<KBObject> getListItems(KBObject list);

	// Rules
	public void setRulePrefixes(HashMap<String, String> map);
	
	public void applyRules(KBRuleList rules);
	
	public void applyRule(KBRule rule);

	// Imports
	public void createImport(String ontid, String importurl);

	public void removeImport(String ontid, String importurl);

	public ArrayList<String> getImports(String ontid);

	public void clearImportCache(String importurl);

	public void importFrom(KBAPI kb);

	public void copyFrom(KBAPI kb);

	// API Transactions
	public boolean save();

	public boolean saveAs(String url);
	
	public boolean delete();
	
	public void end();

	// Serialization (TODO: API should be made RDF agnostic)
	public String toRdf(boolean showheader);

	public String toRdf(boolean showheader, String base);

	public String toAbbrevRdf(boolean showheader);

	public String toAbbrevRdf(boolean showheader, String base);

	public void writeRDF(PrintStream ostr);

	public void writeN3(PrintStream ostr);

	public String toN3();

	public String toN3(String base);
	
	public String toJson();
	
	public String toJson(String base);
}
