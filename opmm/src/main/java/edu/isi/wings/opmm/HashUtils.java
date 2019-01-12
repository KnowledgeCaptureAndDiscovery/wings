package edu.isi.wings.opmm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

/**
 * Class that defines a set of methods for encoding components, files, templates, etc.
 * These are necessary to assess whether two given components are the same or not.
 * @author Daniel Garijo
 */
public class HashUtils {
    
    
    /**
     * Function to create an md5 signature for components. It uses: inputs names, output names and component name.
     * For simplicity, changes in hardware deps, software deps and isConcrete do not constitute a new component version.
     * Changes in code do not constitute a new component version. Changes in the input types do not constitute a new version.
     * 
     * No full URIs are taken into account when creating hash, as the types
     * may be different in WINGS from local catalog.
     * @param wingsCanonicalInstance the canonical instance defining the inputs/outputs of the component.
     * @return 
     */
    public static String createMD5ForWINGSComponent(Individual wingsCanonicalInstance){
        ArrayList<String> componentMetadata = new ArrayList<>();
        componentMetadata.add(wingsCanonicalInstance.getLocalName());
        StmtIterator propertiesIt = wingsCanonicalInstance.listProperties();
        while(propertiesIt.hasNext()){
            Statement s = propertiesIt.next();
            String prop = s.getPredicate().getLocalName();
            if(prop.equals("hasInput")||prop.equals("hasOutput"))
            componentMetadata.add(prop+"_"+s.getObject().asResource().getLocalName());
        }
        //sort to make sure the MD5 is consistent across components
        return serializeArrayListAsMD5(componentMetadata);
    }

    /**
     * Function for creating an MD5 out of the input text
     * @param text
     * @return MD5 or the component, or null if a problem was found
     */
    public static String MD5(String text) {
        try{
            MessageDigest md=MessageDigest.getInstance("MD5");
            md.update(text.getBytes());
            byte b[]=md.digest();
            StringBuilder sb=new StringBuilder();
            for(byte b1:b){
                    sb.append(Integer.toHexString(b1 & 0xff));
            }
            return sb.toString();
        }catch (NoSuchAlgorithmException e){
            System.err.println("Error while calculating MD5: "+e.getMessage());
            return null;
        }

    }
    
    //to do when creating locations.
    //public static String createMD5ForFile(String fileName, String bytes)
    
    //templates
    /**
     * Method that creates a unique hash for a template
     * @param templateInstance WINGS template instance (hashes have to be created from them)
     * @param wingsTemplateModel Model with the template loaded
     * @param wingsTaxonomy WINGS component catalog. Necessary to access the component I/O.
     * @return 
     */
    public static String createMD5ForTemplate(Individual templateInstance, OntModel wingsTemplateModel, OntModel wingsTaxonomy){
        ArrayList<String> connectionsAndComponents = new ArrayList();
        connectionsAndComponents.add(templateInstance.getLocalName());
        //to do? if 2 templates are the same, should they be published separately? (Expanded templates are different all the time)
        StmtIterator propertiesIt = templateInstance.listProperties(wingsTemplateModel.getProperty(Constants.WINGS_PROP_HAS_LINK));
        while(propertiesIt.hasNext()){
            Statement s = propertiesIt.next();
            connectionsAndComponents.add(s.getObject().asResource().getURI());
        }
        //for retrieving components, a query is easier than asking nodes/hasComponent/hasComponentBinding
        String query = QueriesHashUtils.getComponentsURIsOfTemplate();
        //assumption: there will be at least one component of a type
        ResultSet rs = ModelUtils.queryLocalRepository(query, wingsTemplateModel);
        while (rs.hasNext()){
            QuerySolution qs = rs.next();
            String wingsComponentURI = qs.getResource("?compURI").getURI();
            Individual wingsComp = wingsTaxonomy.getIndividual(wingsComponentURI);
            connectionsAndComponents.add(HashUtils.createMD5ForWINGSComponent(wingsComp));
        }
        return serializeArrayListAsMD5(connectionsAndComponents);
    }
    
    /**
     * Method that, given an arraylist with strings, it serializes them in a single string
     * and returns a MD5 identifier from everything
     * @param arr input arraylist to serialize
     * @return MD5 encoding of the contents arraylist, concatenated
     */
    private static String serializeArrayListAsMD5(ArrayList<String> arr){
        String md5ToEncode = "";
        Collections.sort(arr);
        for(String x:arr){
            md5ToEncode+=x+"\n";
        }
        //System.out.println("This information goes into the MD5: "+md5ToEncode);
        return MD5(md5ToEncode);
    }
    
    //tests -> to add in test component.
    //2 versions of the same component should have 2 hashes
    //2 versions of the same file should have 2 different hashes.
    //2 versions of the same template should have 2 hashes.
    public static void main(String[] arges){
        //note local path below.
        OntModel m = ModelUtils.loadModel("C:\\Users\\dgarijo\\Dropbox (OEG-UPM)\\NetBeansProjects\\WingsToOPMWMapper\\NEW_TEST\\concreteWorkflow\\MpileupVariantCallerV1.owl");
        OntModel taxonomy = ModelUtils.loadModel("http://www.wings-workflows.org/wings-omics-portal/export/users/ravali/genomics/components/library.owl");
        Individual templateInstance = m.getIndividual("http://www.wings-workflows.org/wings-omics-portal/export/users/ravali/genomics/workflows/MpileupVariantCaller.owl#MpileupVariantCaller");
        System.out.println(HashUtils.createMD5ForTemplate(templateInstance,m, taxonomy));
    }
}
