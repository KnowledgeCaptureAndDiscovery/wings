package edu.isi.wings.opmm;

/**
 * Class made to hold all queries contained in the workflow execution export.
 * @author dgarijo
 */
public class QueriesWorkflowExecutionExport {
    /**
     * Given a runID , this query will return existing executions with that ID.
     * @param runID id of the execution to find
     * @return query to retrieve templates with a certain label
     */
    public static String getOPMWExecutionsWithRunID(String runID){
        return "select distinct ?exec from <urn:x-arq:UnionGraph> where {"
                + "?exec a <"+Constants.OPMW_WORKFLOW_EXECUTION_ACCOUNT+">."
                + "?exec <"+Constants.OPMW_DATA_PROP_HAS_RUN_ID+"> \""+runID+"\"."
                + "}";
    }
    
    //Below are the queries designed to retrieve metadata from a WINGS execution 
    
    /**
     * Given an execution run id, this query retrieves start date, end date and 
     * status of an execution.
     * @param executionURI the run ID for the execution
     * @return query with start, end and status
     */
    public static String getWINGSExecutionMetadata(String executionURI){
        return "select distinct ?start ?end ?status where {"
                + "<"+executionURI+"> <"+Constants.WINGS_DATA_PROP_HAS_STATUS+"> ?status;\n"
                    + "<"+Constants.WINGS_DATA_PROP_HAS_START_TIME+"> ?start.\n"
                + "OPTIONAL {"
                    + "<"+executionURI+"> <"+Constants.WINGS_DATA_PROP_HAS_END_TIME+"> ?end.}\n"
                + "}";
    }
    
    /**
     * Query that retrieves all steps, their start date, end date, 
     * status of an execution, and code binding
     * Assumption: 1 execution per file.
     * @return query with start, end and status
     */
    public static String getWINGSExecutionStepsAndMetadata(){
        return "select distinct ?step ?start ?end ?status ?code ?invLine where {"
                + "?step a <"+Constants.WINGS_EXECUTION_STEP+">."
                + "?step <"+Constants.WINGS_DATA_PROP_HAS_STATUS+"> ?status;\n"
                    + "<"+Constants.WINGS_DATA_PROP_HAS_START_TIME+"> ?start;\n"
                    + "<"+Constants.P_PLAN_PROP_HAS_INVOCATION_LINE+"> ?invLine;\n"
                    + "<"+Constants.WF_INVOC_DATA_PROP_HAS_CODE_BINDING+"> ?code.\n"
                + "OPTIONAL {"
                    + "?step <"+Constants.WINGS_DATA_PROP_HAS_END_TIME+"> ?end.}\n"
                + "}";
    }
    
    /**
     * Query that will retrieve the expanded templates from an execution
     * @return 
     */
    public static String getWINGSExpandedTemplate(){
        return "select distinct ?expTemplate  where {"
                + "?execution <"+Constants.WINGS_PROP_HAS_EXPANDED_TEMPLATE+"> ?expTemplate."
                + "}";
    }
    
    /**
     * Given an execution step, this query retrieves all inputs, outputs  
     * and bindings
     * @param execStepURI the run ID for the execution
     * @return query type of variable, id and binding
     */
    public static String getWINGSExecutionStepI_O(String execStepURI){
        return "select distinct ?varType ?variable ?binding  where {"
                + "<"+execStepURI+"> ?varType ?variable."
                + "?variable <"+Constants.WF_INVOC_DATA_PROP_HAS_DATA_BINDING+"> ?binding."
                + "}";
    }
    
    /**
     * Query that will return, from an expanded template, all the parameters
     * of a step (and their values). For example, given wTopHat2:
     * maxHItsPerRead, value (with their complete URIs)
     * This query is needed to know which parameters were used by which processes
     * (info included in expanded template only)
     * @param expandedTemplateStep step from which you want to retrieve parameters
     * @return query to retrieve parameters and
     */
    public static String getWINGSParametersForStep(String expandedTemplateStep){
        return "select distinct ?param ?paramValue where {"
                + "?param a <"+Constants.WINGS_PARAMETER_VARIABLE+">."
                + "?param <"+Constants.WINGS_PROP_HAS_PARAMETER_VALUE+"> ?paramValue."
                + "?inLink <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?param."
                + "?inLink <"+Constants.WINGS_PROP_HAS_DESTINATION_NODE+"> <"+expandedTemplateStep+">."
                + "}";
    }
    
}
