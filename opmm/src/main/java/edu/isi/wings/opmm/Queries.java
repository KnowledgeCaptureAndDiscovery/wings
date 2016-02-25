/*
 * Copyright 2012-2013 Ontology Engineering Group, Universidad Politécnica de Madrid, Spain
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

package edu.isi.wings.opmm;

/**
 *
 * @author Daniel Garijo
 */
public class Queries {

    /*Template queries*/
    
    /**
     * Query for retieving the taxonomy url
     * @return
     */
    public static String queryGetTaxonomyURL(){
        String query = "SELECT distinct ?taxonomyURL WHERE{"
                + "?anyNode <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?taxonomyURL}";
        return query;
    }

    /**
     * Query for retieving the name of a template
     * @return
     */
    public static String queryNameWfTemplate(){
        String query = "SELECT ?name ?ver WHERE{?name a <"+Constants.WINGS_WF_TEMPLATE+">."
                + "OPTIONAL{?name <"+Constants.WINGS_DATA_PROP_HAS_VERSION+"> ?ver}}";
        return query;
    }

    /**
     * Query for retrieving the metadata of the template
     * @return
     */
    public static String queryMetadata(){
        String query = "SELECT ?doc ?contrib ?time ?diagram ?license WHERE{"
                + "?m a <"+Constants.WINGS_METADATA+">."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_HAS_CONTRIBUTOR+"> ?contrib}."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_HAS_DOCUMENTATION+"> ?doc}."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_HAS_TEMPLATE_DIAGRAM+"> ?diagram}."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_HAS_LICENSE+"> ?license}."
                + "OPTIONAL{?m <"+Constants.WINGS_DATA_PROP_LAST_UPDATED_TIME+"> ?time.}}";
        return query;
    }

    /**
     * Query to retrieve the components (to be able to link them to processes)
     * @return
     */
    public static String queryNodes(){
        String query = "SELECT ?n ?c ?typeComp ?isConcrete ?rule WHERE{"
                + "?n a <"+Constants.WINGS_NODE+">."
                + "?n <"+Constants.WINGS_PROP_HAS_COMPONENT+"> ?c."
                + "?c <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?cb."
                + "?cb a ?typeComp."
                //filter rdfs:resource and component, which don't add any new knowledge
                + "FILTER(<http://www.wings-workflows.org/ontology/component.owl#Component> !=?typeComp)."
                + "FILTER(<http://www.w3.org/2000/01/rdf-schema#Resource> !=?typeComp)."
                + "OPTIONAL{?c <"+Constants.WINGS_DATA_PROP_IS_CONCRETE+"> ?isConcrete.}"
                + "OPTIONAL{?cb <"+Constants.WINGS_PROP_HAS_RULE+"> ?rule }}";
        return query;
    }

    /**
     * Query to retrieve artifacts (Data Variables in template)
     * @return
     *
    public static String queryDataV(){
        String query = "SELECT ?d ?t ?hasDim WHERE{"
                + "?d a <"+Constants.WINGS_DATA_VARIABLE+">."
                + "OPTIONAL{?role <"+Constants.WINGS_PROP_MAPS_TO_VARIABLE+"> ?d."
                + "?role <"+Constants.WINGS_DATA_PROP_HAS_DIMENSIONALITY+"> ?hasDim.}."
                + "OPTIONAL{?d a ?t ."
                + "FILTER(<"+Constants.WINGS_DATA_VARIABLE+"> != ?t)}}";
        return query;
    }*/
    
    /**
     * Query to retrieve artifacts and their types. (Data Variables in template)
     * Updated for the newer version of Wings
     * @return
     */
    public static String queryDataV2(){
        String query = "SELECT distinct ?d ?hasDim ?t WHERE{"
                + "?d a <"+Constants.WINGS_DATA_VARIABLE+">."
                + "?link <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?d." 
                + "?link ?prop ?port." 
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?r."
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_DIMENSIONALITY+"> ?hasDim." 
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?roleID."
                + "?argument <"+Constants.WINGS_DATA_PROP_HAS_ARGUMENT_ID+"> ?roleID."
                + "?argument a ?t."
                //remove types that are common to all variables.
                + "FILTER(<http://www.wings-workflows.org/ontology/component.owl#DataArgument> !=?t)." 
                + "FILTER(<http://www.wings-workflows.org/ontology/component.owl#ParameterArgument> !=?t)."
                + "FILTER(<http://www.wings-workflows.org/ontology/component.owl#ComponentArgument> !=?t)."
                + "FILTER(<http://www.wings-workflows.org/ontology/data.owl#DataObject> !=?t)."
                + "FILTER(<http://www.w3.org/2000/01/rdf-schema#Resource> !=?t)"
                +"}";
        return query;
    }

    /**
     * Query to retieve the parameters, also artifacts
     * @return
     */
    public static String querySelectParameter(){
        String query = "SELECT ?p ?parValue WHERE{"
                + "?p a <"+Constants.WINGS_PARAMETER_VARIABLE+">."
                + "OPTIONAL{?p <"+Constants.WINGS_DATA_PROP_HAS_PARAMETER_VALUE+"> ?parValue}}";
        return query;
    }

    /**
     * Query for retrieving the information of the used relationships
     * @return
     */
    public static String queryInputLinks(){
        String query = "SELECT ?var ?dest ?role WHERE{"
                + "?iLink a <"+Constants.WINGS_INPUTLINK+">."
                + "?iLink <"+Constants.WINGS_PROP_HAS_DESTINATION_NODE+"> ?dest."
                + "?iLink <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?var."
                + "?var a <"+Constants.WINGS_DATA_VARIABLE+">."
                + "OPTIONAL{"
                + "?iLink <"+Constants.WINGS_PROP_HAS_DESTINATION_PORT+"> ?port."
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?r."
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?role."
                + "}."
                + "}";
        return query;
    }

    /**
     * query for identifying the used relationships for parameters (slightly different)
     * @return
     */
    public static String queryInputLinksP(){
        String query = "SELECT ?var ?dest ?role ?t WHERE{"
                + "?iLink a <"+Constants.WINGS_INPUTLINK+">."
                + "?iLink <"+Constants.WINGS_PROP_HAS_DESTINATION_NODE+">?dest."
                + "?iLink <"+Constants.WINGS_PROP_HAS_VARIABLE+">?var."
                + "?var a <"+Constants.WINGS_PARAMETER_VARIABLE+">."
                + "OPTIONAL{"
                + "?iLink <"+Constants.WINGS_PROP_HAS_DESTINATION_PORT+"> ?port."
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?r."
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?role."
                
//                + "?argument <"+Constants.WINGS_DATA_PROP_HAS_ARGUMENT_ID+"> ?role."
//                + "?argument a ?t."
                
                + "}."
                + "}";
        return query;
    }

    /**
     * query to determine the WasGeneratedBy relationships
     * @return
     */
    public static String queryOutputLinks(){
        String query = "SELECT ?var ?orig ?role WHERE{"
                + "?oLink a <"+Constants.WINGS_OUTPUTLINK+">."
                + "?oLink <"+Constants.WINGS_PROP_HAS_ORIGIN_NODE+">?orig."
                + "?oLink <"+Constants.WINGS_PROP_HAS_VARIABLE+">?var."
                + "OPTIONAL{"
                + "?oLink <"+Constants.WINGS_PROP_HAS_ORIGIN_PORT+"> ?port."
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?r."
                + "?r <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?role."
                + "}."
                + "}";
        return query;
    }

    /**
     * Query to determine used+ was generated by relationships
     * @return
     */
    public static String queryInOutLinks(){
        String query = "SELECT ?var ?orig ?origRole ?dest ?destRole WHERE{"
                + "?ioLink a <"+Constants.WINGS_INOUTLINK+">."
                + "?ioLink <"+Constants.WINGS_PROP_HAS_ORIGIN_NODE+">?orig."
                + "?ioLink <"+Constants.WINGS_PROP_HAS_DESTINATION_NODE+">?dest."
                + "?ioLink <"+Constants.WINGS_PROP_HAS_VARIABLE+">?var."
                + "OPTIONAL{"
                + "?ioLink <"+Constants.WINGS_PROP_HAS_ORIGIN_PORT+"> ?portO."
                + "?portO <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?oRole."
                + "?oRole <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?origRole."
                + "}."
                + "OPTIONAL{"
                + "?ioLink <"+Constants.WINGS_PROP_HAS_DESTINATION_PORT+"> ?portD."
                + "?portD <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?dRole."
                + "?dRole <"+Constants.WINGS_DATA_PROP_HAS_ROLE_ID+"> ?destRole."
                + "}."
                + "}";
        return query;
    }

    /*INSTANCE QUERIES*/
    /**
     * New execution methods
     */
    static String queryIntermediateTemplates() {
        String query = "SELECT ?template ?expandedTemplate ?wfInstance WHERE{"
                + "?execution a <"+Constants.WINGS_EXECUTION+">."
                + "?execution <"+Constants.WINGS_PROP_HAS_TEMPLATE+"> ?template."
                + "?execution <"+Constants.WINGS_PROP_HAS_PLAN+"> ?wfInstance."
                + "?execution <"+Constants.WINGS_PROP_HAS_EXPANDED_TEMPLATE+"> ?expandedTemplate.}";
        return query;
    }
    /**
     * Query to retrieve the name of the template plus metadata from the results
     * @return query
     */
    public static String queryExecutionMetadata(){
        String query = "SELECT ?exec ?status ?startT ?endT ?user ?tool ?license WHERE{"
                + "?exec a <"+Constants.WINGS_EXECUTION+">."
                + "OPTIONAL {?exec <"+Constants.WINGS_DATA_PROP_HAS_STATUS+"> ?status}."
                + "OPTIONAL {?exec <"+Constants.WINGS_DATA_PROP_HAS_START_TIME+"> ?startT}."
                + "OPTIONAL {?exec <"+Constants.WINGS_DATA_PROP_HAS_END_TIME+"> ?endT}."
                + "OPTIONAL {?exec <"+Constants.WINGS_PROP_HAS_USER+"> ?user}."
                + "OPTIONAL {?exec <"+Constants.WINGS_DATA_PROP_HAS_CREATION_TOOL+"> ?tool}."
                //+ "OPTIONAL {?fileURI <"+Constants.WINGS_DATA_PROP_HAS_EXECUTION_ENGINE+"> ?engine}."
                + "OPTIONAL {?exec <"+Constants.WINGS_DATA_PROP_HAS_LICENSE+"> ?license}."
//                + "OPTIONAL {?fileURI <"+Constants.WINGS_DATA_PROP_HAS_EXECUTION_DIAGRAM+"> ?execDiagram}."
                + "}";
        return query;
    }
    
    /**
     * Query to retrieve the metadata of each step
     * @return 
     */
    public static String queryStepsAndMetadata() {
        String query = "SELECT ?step ?startT ?endT ?status ?code ?derivedFrom WHERE{"
                + "?step a <"+Constants.WINGS_EXECUTION_STEP+">. "
                + "OPTIONAL{?step <"+Constants.WINGS_DATA_PROP_HAS_START_TIME+"> ?startT}."//this can be optional
                + "OPTIONAL{?step <"+Constants.WINGS_DATA_PROP_HAS_END_TIME+"> ?endT}."//this can be optional
                + "?step <"+Constants.WINGS_DATA_PROP_HAS_STATUS+"> ?status."
                + "?step <"+Constants.WF_INVOC_DATA_PROP_HAS_CODE_BINDING+"> ?code."
                + "OPTIONAL{?step <"+Constants.WINGS_PROP_DERIVED_FROM+"> ?derivedFrom}."
                + "}";
        return query;
    }
    
    /**
     * Query to retrieve steps and their inputs of an execution
     * @return 
     */
    public static String queryStepInputs() {
        String query = "SELECT ?step ?input ?iBinding WHERE{"
                + "?step a <"+Constants.WINGS_EXECUTION_STEP+">. "
                + "?step <"+Constants.P_PLAN_PROP_HAS_INPUT+"> ?input."
                + "?input <"+Constants.WF_INVOC_DATA_PROP_HAS_DATA_BINDING+"> ?iBinding."
                + "}";
        return query;
    }
    
    /**
     * Query to retrieve steps and their outputs of an execution
     * @return 
     */
    public static String queryStepOutputs() {
        String query = "SELECT ?step ?output ?oBinding WHERE{"
                + "?step a <"+Constants.WINGS_EXECUTION_STEP+">. "
                + "?step <"+Constants.P_PLAN_PROP_HAS_OUTPUT+"> ?output."
                + "?output <"+Constants.WF_INVOC_DATA_PROP_HAS_DATA_BINDING+"> ?oBinding."
                + "}";
        return query;
    }
    
    /**
     * Query to select the data variables and their metadata
     * @return 
     */
    static String queryDataVariablesMetadata() {
        String query = "SELECT ?variable ?prop ?obj WHERE{"
                + "?variable a <"+Constants.WINGS_DATA_VARIABLE+">."
                + "?variable ?prop ?obj."
                //we remove some undesired types
                + "FILTER(<http://www.wings-workflows.org/ontology/workflow.owl#Variable> !=?obj)."
                + "FILTER(<http://www.w3.org/2000/01/rdf-schema#Resource> !=?obj)."
                + "FILTER(<http://www.wings-workflows.org/ontology/workflow.owl#DataVariable> !=?obj)"
                + "}";
        return query;
    }
    /**
     * Query to select step, their input parameters and their values
     * @return 
     */
    public static String querySelectStepParameterValues(){
        String query = "SELECT ?step ?param ?value ?derivedFrom WHERE{"
                + "?link <"+Constants.WINGS_PROP_HAS_DESTINATION_NODE+"> ?step."
                + "?link <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?param."
                + "?param a <"+Constants.WINGS_PARAMETER_VARIABLE+">."
                + "?param <"+Constants.WINGS_DATA_PROP_HAS_PARAMETER_VALUE+"> ?value."
                + "OPTIONAL{?param <"+Constants.WINGS_PROP_DERIVED_FROM+"> ?derivedFrom}."
                + "}";
        return query;
    }
    
    /**
     * Query to retrieve the nodes (processes) of the results.
     * We ask just node id, type and component binding
     * @return
     */
    public static String queryNodesResults(){
        String query = "SELECT ?nodeId ?absComponent ?cbind WHERE{"
                + "?wf <"+Constants.WINGS_PROP_HAS_NODE+"> ?node."
                + "?node <"+Constants.WINGS_PROP_HAS_ID+"> ?nodeId."
                + "?node <"+Constants.WINGS_PROP_HAS_COMPONENT_TYPE+"> ?absComponent."
                + "?node <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?cbind."
                + "}";
        return query;
    }
    
    /**
     * Query that will help determine whether an execution has already been published or not.
     * @param runURL
     * @return 
     */
    public static String queryIsTheRunAlreadyPublished(String runURL){
        String queryRun= "prefix xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?run WHERE {"
                + "?run <http://www.opmw.org/ontology/hasOriginalLogFile> \""+runURL+"\"^^xsd:anyURI}";
        return queryRun;
    }

    

    
   
}
