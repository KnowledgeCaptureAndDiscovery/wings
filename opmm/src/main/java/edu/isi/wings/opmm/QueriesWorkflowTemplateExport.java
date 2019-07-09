package edu.isi.wings.opmm;

/**
 * Class made to hold all queries contained in the workflow template export.
 * @author dgarijo
 */
public class QueriesWorkflowTemplateExport {
    
    /**
     * Given a label with the template name, this query will return 
     * @param labelWingsTemplateName label of the template name to find
     * @return query to retrieve templates with a certain label
     */
    public static String getOPMWTemplatesWithLabel(String labelWingsTemplateName){
        return "select distinct ?t ?v from <urn:x-arq:UnionGraph> where {"
                + "?t a <"+Constants.OPMW_WORKFLOW_TEMPLATE+">."
                + "?t <"+Constants.RDFS_LABEL+"> \""+labelWingsTemplateName+"\"."
                + "OPTIONAL {?t <http://www.w3.org/2002/07/owl#versionInfo> ?v}."
                + "}ORDER BY DESC(?v) LIMIT 1";
    }
    
    /**
     * Given a template hash, this function returns a query that will retrieve any template
     * with such hash.
     * @param templateHash hash to be found
     * @return query for retrieving templates with the hash given as a parameter.
     */
    public static String getOPMWTemplatesWithMD5Hash(String templateHash){
        return "select distinct ?t from <urn:x-arq:UnionGraph> where {"
                + "?t a <"+Constants.OPMW_WORKFLOW_TEMPLATE+">."
                + "?t <" + Constants.OPMW_DATA_PROP_HAS_MD5 + "> \""+templateHash+"\".}";
    }   
    
    //Below are the methods for queries to retrieve against a WINGS template loaded in a local OntModel
    
    /**
     * Query for retrieving all metadata of a WINGS template.
     * The template itself does not need to be specified because there is only one metadata object per template.
     * This query should be performed against an OntModel that has a single template loaded.
     * @return query for retrieving metadata
     */
    public static String queryWINGSTemplateMetadata(){
        String query = "SELECT ?doc ?contrib ?time ?license ?version WHERE{"
                + "?template <"+Constants.WINGS_DATA_PROP_HAS_METADATA+"> ?m."
                + "OPTIONAL{?template <"+Constants.WINGS_DATA_PROP_HAS_VERSION+"> ?version}."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_HAS_CONTRIBUTOR+"> ?contrib}."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_HAS_DOCUMENTATION+"> ?doc}."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_HAS_LICENSE+"> ?license}."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_LAST_UPDATED_TIME+"> ?time.}}";
        return query;
    }

    /**
     * Query for retrieving the elements of an abstract template from the remote repository
     */
    public static String queryRetrieveAbstractTemplateElements(String queryTemplate){
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                "CONSTRUCT  {" +
                "?a ?c ?d" +
                "}" +
                "from <urn:x-arq:UnionGraph>" +
                "WHERE" +
                "{ " +
                "  ?a ?b <" + queryTemplate +  ">." +
                "  ?a ?c ?d" +
                "  filter not exists {?a a <https://www.opmw.org/ontology/WorkflowTemplate>}" +
                "}";
        return query;
    }
    /**
     * Query to retrieve workflow template steps of a WINGS template
     * @return
     */
    public static String queryWINGSTemplateSteps(){
        String query = "SELECT distinct ?n ?c ?cb ?isConcrete ?rule ?derivedFrom WHERE{"
                + "?n a <"+Constants.WINGS_NODE+">."
                + "?n <"+Constants.WINGS_PROP_HAS_COMPONENT+"> ?c."
                + "?c <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?cb."
                + "OPTIONAL{?c <"+Constants.WINGS_DATA_PROP_IS_CONCRETE+"> ?isConcrete.}"
                + "OPTIONAL{?n <"+Constants.WINGS_PROP_DERIVED_FROM+"> ?derivedFrom.}"
                + "OPTIONAL{?c <"+Constants.WINGS_DATA_PROP_HAS_RULE+"> ?rule }}";
        return query;
    }
    
    /**
     * Query to retrieve variables and parameters, along with their dimensionality. 
     * @return
     */
    public static String queryWINGSDataVariables(){
        String query = "SELECT distinct ?d ?type ?hasDim ?roleID ?derivedFrom WHERE{"
                //+ "?d a <"+Constants.WINGS_DATA_VARIABLE+">."
                + "?link <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?d." 
                + "?d a ?type."
                + "?link ?prop ?port." 
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?r."
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_DIMENSIONALITY+"> ?hasDim." 
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?roleID."
                + "OPTIONAL {?d <"+Constants.WINGS_PROP_DERIVED_FROM+"> ?derivedFrom.}."
                +"}";
        return query;
    }
    
    /**
     * Query to retrieve the data types of an argument ID
     * This query should only be issued to a WINGS taxonomy.
     * @param roleId role we are looking for
     * @return 
     */
    public static String queryWINGSTypesOfArgumentID(String roleId){
        String query = "SELECT distinct ?argument WHERE{"
                + "?argument <"+Constants.WINGS_DATA_PROP_HAS_ARGUMENT_ID+"> \""+roleId+"\"."
                +"}";
        return query;
    }
    
    /**
     * Given a node id, this query retrieves the variables/parameters that are used as input
     * @param nodeId
     * @return 
     */
    public static String queryWINGSInputsOfNode(String nodeId){
        String query = "SELECT distinct ?var ?role WHERE{"
                + "?link <"+Constants.WINGS_PROP_HAS_DESTINATION_NODE+"> <"+nodeId+">."
                + "?link <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?var."
                + "OPTIONAL{"
                + "?link <"+Constants.WINGS_PROP_HAS_DESTINATION_PORT+"> ?port."
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?r."
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?role."
                + "}."
                +"}";
        return query;
    }
    
    /**
     * Given a node id, this query retrieves the variables that are produced as output
     * @param nodeId
     * @return 
     */
    public static String queryWINGSOutputsOfNode(String nodeId){
        String query = "SELECT distinct ?var ?role WHERE{"
                + "?link <"+Constants.WINGS_PROP_HAS_ORIGIN_NODE+"> <"+nodeId+">."
                + "?link <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?var."
                + "OPTIONAL{"
                + "?link <"+Constants.WINGS_PROP_HAS_ORIGIN_PORT+"> ?port."
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?r."
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?role."
                + "}."
                +"}";
        return query;
    }
    
    /**
     * String that retrieves all the "derivedFrom" relationships from a WINGS template.
     * @return query to retrieve all derivations
     */
    public static String queryWINGSDerivations(){
        String query = "SELECT distinct ?source ?dest WHERE{"
                + "?source <"+Constants.WINGS_PROP_DERIVED_FROM+"> ?dest."
                +"}";
        return query;
    }
    
    
    /**
     * Query that given a label, returns individuals with it.
     * This query is used because we use ids to link templates and concrete templates.
     * @param label
     * @return 
     */
    public static String queryGetIndividualWithLabel(String label){
        String query = "SELECT distinct ?i WHERE{"
                + "?i <"+Constants.RDFS_LABEL+"> \""+label+"\"."
                +"}";
        return query;
    }
}
