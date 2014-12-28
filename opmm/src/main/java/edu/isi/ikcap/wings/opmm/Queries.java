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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.isi.ikcap.wings.opmm;

/**
 *
 * @author Daniel Garijo
 */
public class Queries {

    /*Template queries*/

    /**
     * query for retieving the name of a template
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
        String query = "SELECT ?n ?c ?typeComp ?isConcrete WHERE{"
                + "?n a <"+Constants.WINGS_NODE+">."
                + "?n <"+Constants.WINGS_PROP_HAS_COMPONENT+"> ?c."
                + "?c a ?typeComp."
                + "OPTIONAL{?c <"+Constants.WINGS_DATA_PROP_IS_CONCRETE+"> ?isConcrete.}}";
        return query;
    }

    /**
     * Query to retrieve artifacts (Data Variables in template)
     * @return
     */
    public static String queryDataV(){
        String query = "SELECT ?d ?t ?hasDim WHERE{"
                + "?d a <"+Constants.WINGS_DATA_VARIABLE+">."
                + "OPTIONAL{?role <"+Constants.WINGS_PROP_MAPS_TO_VARIABLE+"> ?d."
                + "?role <"+Constants.WINGS_DATA_PROP_HAS_DIMENSIONALITY+"> ?hasDim.}."
                + "OPTIONAL{?d a ?t ."
                + "FILTER(<"+Constants.WINGS_DATA_VARIABLE+"> != ?t)}}";
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
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?role."
                + "}."
                + "}";
        return query;
    }

    /**
     * query for identifying the used relationships for parameters (slightly different)
     * @return
     */
    public static String queryInputLinksP(){
        String query = "SELECT ?var ?dest ?role WHERE{"
                + "?iLink a <"+Constants.WINGS_INPUTLINK+">."
                + "?iLink <"+Constants.WINGS_PROP_HAS_DESTINATION_NODE+">?dest."
                + "?iLink <"+Constants.WINGS_PROP_HAS_VARIABLE+">?var."
                + "?var a <"+Constants.WINGS_PARAMETER_VARIABLE+">."
                + "OPTIONAL{"
                + "?iLink <"+Constants.WINGS_PROP_HAS_DESTINATION_PORT+"> ?port."
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?role."
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
                + "?port <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?role."
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
                + "?portO <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?origRole."
                + "}."
                + "OPTIONAL{"
                + "?ioLink <"+Constants.WINGS_PROP_HAS_DESTINATION_PORT+"> ?portD."
                + "?portD <"+Constants.WINGS_PROP_SATISFIES_ROLE+"> ?destRole."
                + "}."
                + "}";
        return query;
    }

    /*INSTANCE QUERIES*/
    /**
     * query to retrieve the name of the template plus metadata from the results
     * @return
     */
    public static String queryNameWfTemplateAndMetadata(){
        String query = "SELECT ?fileURI ?user ?wTempl ?name ?status ?startT ?endT ?execDiagram ?templDiagram ?tool ?engine ?license WHERE{"
                + "?fileURI <"+Constants.WINGS_PROP_HAS_USER+"> ?user."
                + "?fileURI <"+Constants.WINGS_PROP_USES_TEMPLATE+"> ?templ."
                + "?templ <"+Constants.WINGS_PROP_HAS_ID+"> ?name."
                + "?templ <"+Constants.WINGS_PROP_HAS_URL+"> ?wTempl."
                + "OPTIONAL{?templ <"+Constants.WINGS_DATA_PROP_HAS_TEMPLATE_DIAGRAM+"> ?templDiagram}."
                + "?fileURI <"+Constants.WINGS_DATA_PROP_HAS_STATUS+"> ?status."
                + "?fileURI <"+Constants.WINGS_DATA_PROP_HAS_START_TIME+"> ?startT."
                + "?fileURI <"+Constants.WINGS_DATA_PROP_HAS_END_TIME+"> ?endT."
                + "OPTIONAL {?fileURI <"+Constants.WINGS_DATA_PROP_HAS_CREATION_TOOL+"> ?tool}."
                //+ "OPTIONAL {?fileURI <"+Constants.WINGS_DATA_PROP_HAS_EXECUTION_ENGINE+"> ?engine}."
                + "OPTIONAL {?fileURI <"+Constants.WINGS_DATA_PROP_HAS_LICENSE+"> ?license}."
                + "OPTIONAL {?fileURI <"+Constants.WINGS_DATA_PROP_HAS_EXECUTION_DIAGRAM+"> ?execDiagram}."
                + "}";
        return query;
    }
    
    /**
     * Function to query the tools used and their version.
     * @return 
     */
    static String queryUsedTools() {
        String query = "SELECT ?toolID ?version ?url WHERE{"
                + "?fileURI <"+Constants.WINGS_PROP_HAS_EXECUTION_ENGINE+"> ?eng."
                + "?eng <"+Constants.WINGS_PROP_USES_TOOL+"> ?tool."
                + "?tool <"+Constants.WINGS_PROP_HAS_ID+"> ?toolID."
                + "?tool <"+Constants.WINGS_PROP_HAS_URL+"> ?url."
                + "?tool <"+Constants.WINGS_DATA_PROP_HAS_VERSION+"> ?version."
                + "}";
        return query;
    }

    /**
     * Query to retrieve the nodes (processes) of the results.
     * @return
     */
//    public static String queryNodesResults(){
//        String query = "SELECT ?nodeId ?absComponent ?comp ?compLoc WHERE{"
//                + "?wf <"+Constants.WINGS_PROP_HAS_NODE+"> ?node."
//                + "?node <"+Constants.WINGS_PROP_HAS_ID+"> ?nodeId."
//                + "?node <"+Constants.WINGS_PROP_HAS_COMPONENT_TYPE+"> ?absComponent."
//                + "?node <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?cbind."
//                + "?cbind <"+Constants.WINGS_PROP_HAS_COMPONENT+"> ?comp ."
//                + "OPTIONAL {?cbind <"+Constants.WINGS_PROP_HAS_LOCATION+"> ?compLoc.}}";
//        return query;
//    }
    
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
     * query to retrieve the 'used' relationships
     * @return
     */
//    public static String queryInLinksResults(){
//        String query = "SELECT ?id ?var ?dbind ?cbind ?pbind ?loc ?size ?varT ?role WHERE{"
//                + "?wf <"+Constants.WINGS_PROP_HAS_NODE+"> ?node."
//                + "?node <"+Constants.WINGS_PROP_HAS_ID+"> ?id."
//                + "?node <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?cbind.}";//cbind
////                + "?cbind <"+Constants.WINGS_PROP_HAS_COMPONENT+"> ?comp ."
////                + "?cbind <"+Constants.WINGS_PROP_HAS_INPUT+"> ?inp ."
////                + "?inp <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?var ."
////                + "?inp <"+Constants.WINGS_PROP_HAS_ARGUMENT_ID+"> ?role."                
////                + "OPTIONAL{?inp <"+Constants.WINGS_PROP_HAS_PARAMETER_BINDING+"> ?pbind .}."
////                + "OPTIONAL{?inp <"+Constants.WINGS_PROP_HAS_DATA_BINDING+"> ?dbind ."
////                + "?dbind <"+Constants.WINGS_PROP_HAS_LOCATION+"> ?loc."
////                + "?dbind <"+Constants.WINGS_DATA_PROP_HAS_SIZE+"> ?size."
////                + "?dbind a ?varT.}}";
//        return query;
//    }
    
    /**TEST QUERIES**/
    //query to iterate through an RDF list
//    public static String queryTestspecific(){
//        String query = "SELECT ?member WHERE{"
//                + "?a <"+Constants.WINGS_PROP_HAS_INPUT+"> ?input."
//                + "?input <"+Constants.WINGS_PROP_HAS_ARGUMENT_ID+"> \"InputSensorData\"."
//                + "?input <"+Constants.WINGS_PROP_HAS_VARIABLE+"> <http://www.isi.edu/Water/AquaFlow_NTM.owl#DailyData>."
//                + "?input <"+Constants.WINGS_PROP_HAS_DATA_BINDING+"> ?dbind. "
//                + "?dbind <http://jena.hpl.hp.com/ARQ/list#member> ?member ."
//                + "}";
//        return query;
//    }
//    
//    public static String queryInLinkTest(){
//        String query = "SELECT ?comp ?inp ?dbind ?pbind WHERE{"                         
//                + "?cbind <"+Constants.WINGS_PROP_HAS_COMPONENT+"> ?comp ."
//                + "?cbind <"+Constants.WINGS_PROP_HAS_INPUT+"> ?inp ."
////                + "?inp <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?var ."
////                + "?inp <"+Constants.WINGS_PROP_HAS_ARGUMENT_ID+"> ?role."                
//                + "OPTIONAL{?inp <"+Constants.WINGS_PROP_HAS_PARAMETER_BINDING+"> ?pbind .}."
//                + "OPTIONAL{?inp <"+Constants.WINGS_PROP_HAS_DATA_BINDING+"> ?dbind .}}";
////                + "?dbind <"+Constants.WINGS_PROP_HAS_LOCATION+"> ?loc."
////                + "?dbind <"+Constants.WINGS_DATA_PROP_HAS_SIZE+"> ?size."
////                + "?dbind a ?varT.}}";
//        return query;
//    }
//    
//    public static String getComponentBindingLists(){
//        String query = "SELECT ?id ?first ?rest WHERE{"
//                + "?wf <"+Constants.WINGS_PROP_HAS_NODE+"> ?node."
//                + "?node <"+Constants.WINGS_PROP_HAS_ID+"> ?id."
//                + "?node <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?cbind."
//                + "?cbind <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?first."
//                + "?cbind <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> ?rest}";//cbind
//
//        return query;
//    }
    
//    public static String getCompLocInputOutput(String uri){
//        String query = "SELECT ?comp ?loc WHERE{"
//                + uri+ "<"+Constants.WINGS_PROP_HAS_COMPONENT+"> ?comp ."
//                + uri+ "<"+Constants.WINGS_PROP_HAS_LOCATION+"> ?loc ."                
//                + "}";
//
//        return query;
//    }
    
//    public String getComponentBinding(){
//        return query;
//    }
/**
     * END TEST QUERIES
     */
    
 /**
  * Query to retrieve the 'wasGeneratedBy' relationships
  * @return
  */
// public static String queryOutLinksResults(){
//        String query = "SELECT ?id ?var ?bind ?loc ?size ?varT ?role WHERE{"
//                + "?wf <"+Constants.WINGS_PROP_HAS_NODE+"> ?node."
//                + "?node <"+Constants.WINGS_PROP_HAS_ID+"> ?id."
//                + "?node <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?cbind."
//                + "?cbind <"+Constants.WINGS_PROP_HAS_COMPONENT+"> ?comp ."
//                + "?cbind <"+Constants.WINGS_PROP_HAS_OUTPUT+"> ?out ."
//                + "?out <"+Constants.WINGS_PROP_HAS_VARIABLE+"> ?var ."
//                + "?out <"+Constants.WINGS_PROP_HAS_ARGUMENT_ID+"> ?role."
//                + "?out <"+Constants.WINGS_PROP_HAS_DATA_BINDING+"> ?bind ."
//                + "?bind <"+Constants.WINGS_PROP_HAS_LOCATION+"> ?loc."
//                + "?bind <"+Constants.WINGS_DATA_PROP_HAS_SIZE+"> ?size."
//                + "?bind a ?varT.}";
//        return query;
//    }

 /**
  * Query to retrieve the related properties from an artifact.
  * Since Allegro does not work well with regexp, it retrieves all and
  * the filtering has to be done manually
  * @param artifactID ID of the artifact we are aiming to retrieve the properties.
  * @return
  */
    public static String queryDCDOMProperties(String artifactID){
        String queryDCDOMprops = "SELECT ?prop ?value WHERE{"
                + "<"+artifactID+"> ?prop ?value.}";
        return queryDCDOMprops;
    }
    
    public static String queryIsTheRunAlreadyPublished(String runURL){
        String queryRun= "prefix xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?run WHERE {"
                + "?run <http://www.opmw.org/ontology/hasOriginalLogFile> \""+runURL+"\"^^xsd:anyURI}";
        return queryRun;
    }

    
   
}
