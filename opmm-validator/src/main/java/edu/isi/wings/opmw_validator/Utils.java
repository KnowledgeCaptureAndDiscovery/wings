/*
 * Create by Daniel Garijo
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.isi.wings.opmw_validator;

import java.io.File;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 *
 * @author Daniel Garijo
 */
public class Utils {
    
    //given a directory, loads all the files in a local model.
    public static OntModel loadDirectory(String dirPath){
        OntModel model = ModelFactory.createOntologyModel();
        File dir = new File (dirPath);
        if(dir.isDirectory()){
            for (File currf : dir.listFiles()){
                loadFileInModel(model, currf);
            }
            return model;
        }
        System.err.println("The path "+dirPath+ " is not a directory");
        return null;
        
    }
    
    private static OntModel loadFileInModel(OntModel m, File f){
        try{
            System.out.println("Loading file: "+f.getName());
            m.read(f.getAbsolutePath(), null, "TURTLE");
        }catch(Exception e){
            System.err.println("Could not load the file in turtle. Attempting to read it in turtle...");
            try{
                m.read(f.getAbsolutePath(), null, "RDF/XML");
            }catch(Exception e1){
                System.err.println("Could not load ontology in rdf/xml.");
            }
        }
        return m;
    }
    
    public static ResultSet queryLocalRepository(String queryIn, OntModel repository){
        Query query = QueryFactory.create(queryIn);
        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, repository);
        ResultSet rs =  qe.execSelect();
        //qe.close();
        return rs;
    }
    
    public static String getCountOf (String query, OntModel m, String varToQuery){
        ResultSet r =  Utils.queryLocalRepository(query, m);
        String result = "";
        while (r.hasNext()){
            QuerySolution qs = r.nextSolution();
            result+=qs.getLiteral("?"+varToQuery).getString();
        }
        return result;
    }
            
    
    //given an online repository, perform a test against a template/run.
    //TO DO
}
