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

package edu.isi.wings.ontapi.tests;

import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
//import org.mindswap.pellet.jena.PelletReasonerFactory;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.tdb.TDBFactory;

@SuppressWarnings("unused")
public class KBTests {
	static String onturl = "http://www.wings-workflows.org/ontology/workflow.owl";
	static String url = "http://localhost:8080/wings/export/users/1/DMDomain/workflows/ModelAndClassify.owl";
	
	public static void main(String[] args) {
//		OntFactory fac = new OntFactory(OntFactory.JENA, "/Users/varun/git/fuseki/DB");
//		KBAPI plainkb = null;
//		try {
//			KBAPI kb = fac.getKB(onturl, OntSpec.PELLET);			
//			plainkb = fac.getKB(url, OntSpec.PELLET);
//			plainkb.importFrom(kb);
//			
//			plainkb.beginwrite();
//			KBObject cls = plainkb.getConcept(onturl+"#WorkflowTemplate");
//			KBObject tpl = plainkb.createObjectOfClass(url+"#ModelAndClassify", cls);
//			KBObject prop = plainkb.getProperty(onturl + "#hasVersion");
//			plainkb.setPropertyValue(tpl, prop, plainkb.createLiteral(2));
//			plainkb.save();
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		finally {
//			if(plainkb != null)
//				plainkb.end();
//		}
		
		Dataset tdbstore = TDBFactory.createDataset("/Users/varun/git/fuseki/DB");
		try {
//			tdbstore.begin(ReadWrite.WRITE);
//			Model impmodel = tdbstore.getNamedModel(onturl);
//			if(impmodel.isEmpty()) {
//				impmodel.read(onturl);
//			}
//			OntModel ontmodel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, impmodel);
//			tdbstore.commit();
			
//			tdbstore.begin(ReadWrite.WRITE);
//			OntModel model =  ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, tdbstore.getNamedModel(url));
//			model.addSubModel(ontmodel);
//			Property prop = model.getProperty(onturl + "#hasVersion");
//			Individual tpl = model.createIndividual(url + "#ModelAndClassify", model.getResource(onturl + "#WorkflowTemplate"));
//			tpl.setPropertyValue(prop, model.createTypedLiteral(2));
//			tdbstore.commit();
			
//			tdbstore.begin(ReadWrite.READ);
//			OntModel model =  ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, tdbstore.getNamedModel(url));
//			model.getOntClass(onturl + "#WorkflowTemplate");
//			model.addSubModel(ontmodel);
//			model.write(System.out, "N3");
//			tdbstore.end();

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			tdbstore.end();
		}
	}
}
