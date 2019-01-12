/*
 * License to be added
 */
package edu.isi.wings.opmm;

/**
 * Class that contains those queries used in the catalog for doing versioning,
 * creation, etc.
 * @author Daniel Garijo
 */
public class QueriesCatalog {
    
    //checks if an entity exists, even if there are no i/o
    public static String getEntityFromLabel(String entityLabel){
    	String query= "SELECT ?e WHERE{"
                + "?e <"+Constants.RDFS_LABEL+"> \""+entityLabel+"\"."
                + "}";
    	return query;
    }
    /**
     * Query for retrieving inputs/outputs of an entity (in the catalog)
     * @param entityLabel
     * @return 
     */
//    public static String getEntityIO(String entityLabel){
//    	String query= "SELECT ?i ?o WHERE{"
//                + "?n <"+Constants.RDFS_LABEL+"> \""+entityLabel+"\"."
//                + "{"
//                + "?n <"+Constants.PREFIX_COMPONENT+"hasInput> ?i."		
//    		+ "}UNION {"
//                + "?n <"+Constants.PREFIX_COMPONENT+"hasOutput> ?o."		
//                + "}"
//                + "}";
//    	return query;
//    }
    
    /**
     * Method that gets the individuals of a class
     * @param classURI
     * @return 
     */
//    public static String getCanonicalIndividuals(String classURI){
//        String query = "SELECT ?cI WHERE{"
//                + "?cI a <"+classURI+">.}";
//        return query;
//    }
    
    /**
     * Get all instance assertions except rdf:type
     * @param instance
     * @return 
     */
    public static String getInstanceAssertions(String instance){
        String query = "SELECT ?p ?o WHERE{"
                + "<"+instance+"> ?p ?o."
                + "FILTER (str(?p) != \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\")"
                + "}";
        return query;
    }
    
    public static String getClassAssertions(String classURI){
        String query = "SELECT distinct ?p ?o WHERE{"
                + "<"+classURI+"> ?p ?o.}";
        return query;
    }
    
    /**
     * Query to retrieve components with a given MD5. 
     * @param md5
     * @return query for retrieving a component with given md5
     */
    public static String getComponentsWithMD5(String md5){
        String query = "SELECT distinct ?c WHERE{"
                + "?c <http://www.opmw.org/ontology/hasMD5> \""+md5+"\".}";
        return query;
    }
    
    /**
     * Query to retrieve the most recent version number of a component
     * @param labelOfComponent
     * @return query for retrieving the latest version number of a component
     */
    public static String getMostRecentVersionNumber(String labelOfComponent){
        String query = "SELECT ?ci ?number WHERE { "
                + "?ci <"+Constants.RDFS_LABEL+"> \""+labelOfComponent+"\"."
                + "?ci <"+Constants.OPMW_DATA_PROP_HAS_MD5+"> ?md5." //we add this so we get only instances
                + "?ci <http://www.w3.org/2002/07/owl#versionInfo> ?number"
                + "}"
                + "ORDER BY DESC(?number) LIMIT 1";
        return query;
    }
   
}
