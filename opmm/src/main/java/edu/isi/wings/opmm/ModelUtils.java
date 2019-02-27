package edu.isi.wings.opmm;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

/**
 *
 * @author dgarijo
 */
public class ModelUtils {
    
    /** 
     * Method that loads a file or URL into an OntModel. Throws an exception if file/URL could not be opened.
     * The method will attempt to read the file in RDF/XML and turtle.
     * @param path path to the file/URL from which to load
     * @return and OntModel object after reading it from pathToFile
     */
    public static OntModel loadModel(String path) throws IllegalArgumentException{
        OntModel m = ModelFactory.createOntologyModel(); 
        if(path.startsWith("http://")){
            m.read(path);
            return m;
        }
        InputStream in = FileManager.get().open(path);
        if (in == null) {
            throw new IllegalArgumentException("File: " + path + " not found");
        }
        try{
            m.read(in, null);
        }catch(Exception e){
            System.out.println("Trying uploading "+path+"  as a turtle file");
            in = FileManager.get().open(path);
            m.read(in, null, "TURTLE");
        }
        System.out.println("File "+path+" loaded successfully");
        return m;
    }
    
    /**
     * Function to export the stored model as an RDF file, using ttl syntax
     * @param outFile name and path of the outFile must be created.
     * @param model model to serialize as rdf
     * @param mode serialization to export the model in
     */
    public static void exportRDFFile(String outFile, OntModel model, String mode){
        OutputStream out;
        try {
            out = new FileOutputStream(outFile);
            model.write(out,mode);
            //model.write(out,"RDF/XML");
            out.close();
        } catch (Exception ex) {
            System.out.println("Error while writing the model to file "+ex.getMessage() + " oufile "+outFile);
        }
    }
    
    /**
     * Query a local repository, specified in the second argument
     * @param queryIn sparql query to be performed
     * @param repository repository on which the query will be performed
     * @return 
     */
    public static ResultSet queryLocalRepository(String queryIn, OntModel repository){
        Query query = QueryFactory.create(queryIn);
        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, repository);
        ResultSet rs =  qe.execSelect();
        return rs;
    }
    /**
     * Query an online repository
     * @param queryIn query to specify
     * @param endpointURI URI of the repository
     * @return 
     */
    public static QuerySolution queryOnlineRepository(String queryIn, String endpointURI){
        Query query = QueryFactory.create(queryIn);
        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.sparqlService(endpointURI, query);
        ResultSet rs =  qe.execSelect();
        QuerySolution solution = null;
        if(rs.hasNext())
          solution = rs.next();
        qe.close();
        return solution;
    }
    
    /**
     * Function that initializes a model. If the model exists, it empties it.
     * If it doesn't exist (null) it returns a new instance of a model
     * @param m model to initialize
     * @return clean new model
     */
    public static OntModel initializeModel (OntModel m){
        if(m!=null){
            m.removeAll();
         }else{
            m = ModelFactory.createOntologyModel();
         }
        return m;
    }
    
    
    /**
     * Given a local file path, this method returns an individual of the class provided
     * in the model provided and annotates it with label and location.
     * @param path path of the target file
     * @param model model to add the annotations to
     * @param classURI class of the new individual created
     * @param individualURI URI of the individual
     * @return 
     */
    public static Resource getIndividualFromFile (String path, OntModel model, String classURI, String individualURI){
        Resource i;
        if(individualURI!=null){
            i = model.createClass(classURI).createIndividual(individualURI);
            i.addProperty(model.createProperty(Constants.RDFS_LABEL), i.getLocalName());
        }else{
            i=model.createClass(classURI).createIndividual();//annon id
        }
        i.addProperty(model.createProperty(Constants.OPMW_DATA_PROP_HAS_LOCATION), path);
        return i;
    }
    
    /**
     * Given a label, this method returns a resource with that label.
     * Assumption: only a single resource will have that label
     * @param label unique label you are asking for
     * @param model model where you want to perform the query
     * @return 
     */
    public static Resource getIndividualWithLabel(String label, OntModel model){
        String q = QueriesWorkflowTemplateExport.queryGetIndividualWithLabel(label);
        ResultSet rs = queryLocalRepository(q, model);
        if(rs.hasNext()){
            return rs.next().getResource("?i");
        }
        return null;
    }
}
