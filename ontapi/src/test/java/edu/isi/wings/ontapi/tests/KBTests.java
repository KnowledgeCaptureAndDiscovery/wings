package edu.isi.wings.ontapi.tests;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;

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
			tdbstore.begin(ReadWrite.WRITE);
			Model impmodel = tdbstore.getNamedModel(onturl);
			if(impmodel.isEmpty()) {
				impmodel.read(onturl);
			}
			OntModel ontmodel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, impmodel);
			tdbstore.commit();
			
//			tdbstore.begin(ReadWrite.WRITE);
//			OntModel model =  ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, tdbstore.getNamedModel(url));
//			model.addSubModel(ontmodel);
//			Property prop = model.getProperty(onturl + "#hasVersion");
//			Individual tpl = model.createIndividual(url + "#ModelAndClassify", model.getResource(onturl + "#WorkflowTemplate"));
//			tpl.setPropertyValue(prop, model.createTypedLiteral(2));
//			tdbstore.commit();
			
			tdbstore.begin(ReadWrite.READ);
			OntModel model =  ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, tdbstore.getNamedModel(url));
			model.getOntClass(onturl + "#WorkflowTemplate");
			model.addSubModel(ontmodel);
			model.write(System.out, "N3");
			tdbstore.end();

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			tdbstore.end();
		}
	}
}
