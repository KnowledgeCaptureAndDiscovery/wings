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
public class Constants {
    //constants for prefixes
//    public static final String PREFIX_EXPORT_RESOURCE =  "http://wings.isi.edu/opmexport/resource/";
//    public static final String PREFIX_ONTOLOGY_PROFILE =  "http://wings.isi.edu/ontology/opmv/";
    public static final String PREFIX_EXPORT_RESOURCE =  "http://www.opmw.org/export/resource/";
    public static final String PREFIX_OPMW =  "http://www.opmw.org/ontology/";
    public static final String PREFIX_OPMO =  "http://openprovenance.org/model/opmo#";
    public static final String PREFIX_OPMV =  "http://purl.org/net/opmv/ns#";
    public static final String PREFIX_FOAF = "http://xmlns.com/foaf/0.1/";
    public static final String PREFIX_GEO = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    public static final String PREFIX_SIOC =  "http://rdfs.org/sioc/ns#";
    public static final String PREFIX_DCTERMS = "http://purl.org/dc/terms/";
    public static final String PREFIX_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String PREFIX_PROV = "http://www.w3.org/ns/prov#";
//    public static final String PREFIX_WINGS_AC = "http://www.isi.edu/ac/WINGS/library.owl#";
//    public static final String PREFIX_WINGS_DC = "http://www.isi.edu/dc/WINGS/ontology.owl#";

    //OPM concepts, classes and relationships. Necessary to build the rdf

    //concepts: necessary to assert the relationships easily
    public static final String CONCEPT_ARTIFACT = "Artifact";
    public static final String CONCEPT_ACCOUNT = "Account";    
    public static final String CONCEPT_PROCESS = "Process";
    public static final String CONCEPT_AGENT = "Agent";
    public static final String CONCEPT_DATA_VARIABLE = "DataVariable";
    public static final String CONCEPT_PARAMETER_VARIABLE = "ParameterVariable";
    public static final String CONCEPT_WORKFLOW_EXECUTION_PROCESS = "WorkflowExecutionProcess";
    public static final String CONCEPT_WORKFLOW_TEMPLATE_PROCESS = "WorkflowTemplateProcess";
    public static final String CONCEPT_WORKFLOW_EXECUTION_ARTIFACT = "WorkflowExecutionArtifact";
    public static final String CONCEPT_WORKFLOW_TEMPLATE_ARTIFACT = "WorkflowTemplateArtifact";
    public static final String CONCEPT_WORKFLOW_TEMPLATE = "WorkflowTemplate";
    public static final String CONCEPT_WORKFLOW_EXECUTION_ACCOUNT = "WorkflowExecutionAccount";

    //relationships
    public static final String OPM_PROP_HAS_AGENT = PREFIX_OPMO+"hasAgent";
//    public static final String OPM_PROP_HAS_ARTIFACT = PREFIX_OPMO+"hasArtifact";
//    public static final String OPM_PROP_HAS_PROCESS = PREFIX_OPMO+"hasProcess";
    public static final String OPM_PROP_ACCOUNT = PREFIX_OPMO+"account";
    //the above three relationships could be avoided with the use of named graphs in the future.
    public static final String OPM_PROP_USED = PREFIX_OPMV+"used";
    public static final String OPM_PROP_WGB = PREFIX_OPMV+"wasGeneratedBy";
    public static final String OPM_PROP_WCB = PREFIX_OPMV+"wasControlledBy";
    
    //opmw
    public static final String OPMW_PROP_USES = PREFIX_OPMW+"uses";
    public static final String OPMW_PROP_IGB = PREFIX_OPMW+"isGeneratedBy";
    
    public static final String OPMW_PROP_CORRESPONDS_TO_TEMPLATE_PROCESS = PREFIX_OPMW+"correspondsToTemplateProcess";
    public static final String OPMW_PROP_CORRESPONDS_TO_TEMPLATE_ARTIFACT = PREFIX_OPMW+"correspondsToTemplateArtifact";
    public static final String OPMW_PROP_CORRESPONDS_TO_TEMPLATE = PREFIX_OPMW+"correspondsToTemplate";
    public static final String OPMW_PROP_HAS_EXECUTABLE_COMPONENT = PREFIX_OPMW+"hasExecutableComponent";
    //public static final String OPMW_PROP_BELONGS_TO_TEMPLATE = PREFIX_OPMW+"belongsToTemplate";
    public static final String OPMW_PROP_IS_STEP_OF_TEMPLATE = PREFIX_OPMW+"isStepOfTemplate";
    public static final String OPMW_PROP_IS_VARIABLE_OF_TEMPLATE = PREFIX_OPMW+"isVariableOfTemplate";
    public static final String OPMW_PROP_IS_PARAMETER_OF_TEMPLATE = PREFIX_OPMW+"isParameterOfTemplate";
    public static final String OPMW_PROP_EXECUTED_IN_WORKFLOW_SYSTEM = PREFIX_OPMW+"executedInWorkflowSystem";
    public static final String PROP_HAS_CONTRIBUTOR = "http://purl.org/dc/terms/contributor";

    //classes
    public static final String OPM_PROCESS = PREFIX_OPMV+CONCEPT_PROCESS;
    public static final String OPM_ARTIFACT = PREFIX_OPMV+CONCEPT_ARTIFACT;
    public static final String OPM_AGENT = PREFIX_OPMV+CONCEPT_AGENT;
    public static final String OPM_ACCOUNT = PREFIX_OPMO+CONCEPT_ACCOUNT;
    
    //classes opmw
    public static final String OPMW_WORKFLOW_EXECUTION_PROCESS = PREFIX_OPMW+CONCEPT_WORKFLOW_EXECUTION_PROCESS;
    public static final String OPMW_WORKFLOW_EXECUTION_ARTIFACT = PREFIX_OPMW+CONCEPT_WORKFLOW_EXECUTION_ARTIFACT;
    public static final String OPMW_WORKFLOW_TEMPLATE_PROCESS = PREFIX_OPMW+CONCEPT_WORKFLOW_TEMPLATE_PROCESS;
    public static final String OPMW_DATA_VARIABLE = PREFIX_OPMW+CONCEPT_DATA_VARIABLE;
    public static final String OPMW_PARAMETER_VARIABLE = PREFIX_OPMW+CONCEPT_PARAMETER_VARIABLE;
    public static final String OPMW_WORKFLOW_TEMPLATE = PREFIX_OPMW+CONCEPT_WORKFLOW_TEMPLATE;
    public static final String OPMW_WORKFLOW_TEMPLATE_ARTIFACT = PREFIX_OPMW+CONCEPT_WORKFLOW_TEMPLATE_ARTIFACT;
    public static final String OPMW_WORKFLOW_EXECUTION_ACCOUNT = PREFIX_OPMW+CONCEPT_WORKFLOW_EXECUTION_ACCOUNT;

    //data properties
    public static final String OPMW_DATA_PROP_HAS_SIZE = PREFIX_OPMW+"hasSize";
    public static final String OPMW_DATA_PROP_HAS_LOCATION = PREFIX_OPMW+"hasLocation";
    public static final String OPMW_DATA_PROP_IS_CONCRETE = PREFIX_OPMW+"isConcrete";
    public static final String OPMW_DATA_PROP_VERSION_NUMBER = PREFIX_OPMW+"versionNumber";
    public static final String OPMW_DATA_PROP_HAS_DIMENSIONALITY = PREFIX_OPMW+"hasDimensionality";
    public static final String OPMW_DATA_PROP_HAS_DOCUMENTATION = PREFIX_OPMW+"hasDocumentation";    
    public static final String OPMW_DATA_PROP_HAS_NATIVE_SYSTEM_TEMPLATE = PREFIX_OPMW+"hasNativeSystemTemplate";
    public static final String OPMW_DATA_PROP_HAS_ORIGINAL_LOG_FILE = PREFIX_OPMW+"hasOriginalLogFile";
    public static final String OPMW_DATA_PROP_HAS_STATUS = PREFIX_OPMW+"hasStatus";
    public static final String OPMW_DATA_PROP_OVERALL_START_TIME = PREFIX_OPMW+"overallStartTime";
    public static final String OPMW_DATA_PROP_OVERALL_END_TIME = PREFIX_OPMW+"overallEndTime";
    public static final String OPMW_DATA_PROP_HAS_TEMPLATE_DIAGRAM = PREFIX_OPMW+"hasTemplateDiagram";
    public static final String OPMW_DATA_PROP_HAS_EXECUTION_DIAGRAM = PREFIX_OPMW+"hasExecutionDiagram";
    public static final String OPMW_DATA_PROP_HAS_FILE_NAME = PREFIX_OPMW+"hasFileName";     
    public static final String OPMW_DATA_PROP_CREATED_IN_WORKFLOW_SYSTEM = PREFIX_OPMW+"createdInWorkflowSystem";
    public static final String DATA_PROP_HAS_HOME_PAGE = "http://xmlns.com/foaf/0.1/homepage";
    public static final String OPMW_DATA_PROP_HAS_VALUE = PREFIX_OPMW + "hasValue";
    public static final String DATA_PROP_RIGHTS = "http://purl.org/dc/elements/1.1/rights";
    public static final String DATA_PROP_MODIFIED = "http://purl.org/dc/terms/modified";


    //WINGS Templates Constants. Necessary to query the model
    //prefixes
    public static final String WFLOW = "http://www.isi.edu/2007/08/workflow.owl#";
    public static final String AC = "http://www.isi.edu/ac/ontology.owl#";

    //classes
    public static final String WINGS_WF_TEMPLATE = WFLOW + "WorkflowTemplate";
    public static final String WINGS_NODE = WFLOW + "Node";
    public static final String WINGS_DATA_VARIABLE = WFLOW + "DataVariable";
    public static final String WINGS_PARAMETER_VARIABLE = WFLOW + "ParameterVariable";
    public static final String WINGS_INPUTLINK = WFLOW + "InputLink";
    public static final String WINGS_OUTPUTLINK = WFLOW + "OutputLink";
    public static final String WINGS_INOUTLINK = WFLOW + "InOutLink";
    public static final String WINGS_ROLE = WFLOW + "Role";
    public static final String WINGS_METADATA = WFLOW + "Metadata";

    //properties
    public static final String WINGS_PROP_HAS_DESTINATION_NODE = WFLOW + "hasDestinationNode";
    public static final String WINGS_PROP_HAS_ORIGIN_NODE = WFLOW + "hasOriginNode";
    public static final String WINGS_PROP_HAS_VARIABLE = WFLOW + "hasVariable";
    public static final String WINGS_PROP_HAS_DESTINATION_PORT = WFLOW + "hasDestinationPort";
    public static final String WINGS_PROP_HAS_ORIGIN_PORT = WFLOW + "hasOriginPort";
    public static final String WINGS_PROP_SATISFIES_ROLE = WFLOW + "satisfiesRole";
    public static final String WINGS_PROP_HAS_COMPONENT = WFLOW + "hasComponent";
    //public static final String WINGS_PROP_HAS_COMPONENT_URL = WFLOW + "hasComponentURL";
    public static final String WINGS_PROP_MAPS_TO_VARIABLE = WFLOW + "mapsToVariable";
    public static final String WINGS_PROP_HAS_ARGUMENT_ID = AC + "hasArgumentID";
    
    //execution properties
    public static final String WINGS_PROP_HAS_NODE= WFLOW + "hasNode";
    public static final String WINGS_PROP_HAS_COMPONENT_TYPE= WFLOW + "hasComponentType";
    public static final String WINGS_PROP_HAS_ID = WFLOW + "hasID";
//    public static final String WINGS_PROP_HAS_TEMPLATE_DIAGRAM = WFLOW + "hasTemplateDiagram";
//    public static final String WINGS_PROP_HAS_EXECUTION_DIAGRAM = WFLOW + "hasExecutionDiagram"; 
    public static final String WINGS_PROP_HAS_COMPONENT_BINDING = WFLOW + "hasComponentBinding";
    public static final String WINGS_PROP_HAS_INPUT = WFLOW + "hasInput";
    public static final String WINGS_PROP_HAS_OUTPUT = WFLOW + "hasOutput";
    public static final String WINGS_PROP_HAS_DATA_BINDING = WFLOW + "hasDataBinding";
    public static final String WINGS_PROP_HAS_PARAMETER_BINDING = WFLOW + "hasParameterBinding";
    public static final String WINGS_PROP_HAS_LOCATION = WFLOW + "hasLocation";
    public static final String WINGS_PROP_HAS_USER = WFLOW + "hasUser";
    public static final String WINGS_PROP_USES_TEMPLATE = WFLOW + "usesTemplate";
    public static final String WINGS_PROP_HAS_URL = WFLOW + "hasURL";
    public static final String WINGS_PROP_HAS_EXECUTION_ENGINE = WFLOW + "hasExecutionEngine";
    public static final String WINGS_PROP_USES_TOOL = WFLOW + "usesTool";
    
    

    //data prop
    public static final String WINGS_DATA_PROP_IS_CONCRETE = WFLOW + "isConcrete";
    public static final String WINGS_DATA_PROP_HAS_DIMENSIONALITY = WFLOW + "hasDimensionality";
    public static final String WINGS_DATA_PROP_CREATED_FROM = WFLOW + "createdFrom";
    public static final String WINGS_DATA_PROP_HAS_CONTRIBUTOR = WFLOW + "hasContributor";
    public static final String WINGS_DATA_PROP_HAS_PARAMETER_VALUE = WFLOW + "hasParameterValue";
    public static final String WINGS_DATA_PROP_HAS_DOCUMENTATION = WFLOW + "hasDocumentation";
    public static final String WINGS_DATA_PROP_HAS_VERSION = WFLOW + "hasVersion";
    public static final String WINGS_DATA_PROP_LAST_UPDATED_TIME = WFLOW + "lastUpdateTime";
    public static final String WINGS_DATA_PROP_HAS_TEMPLATE_DIAGRAM = WFLOW + "hasTemplateDiagram";
    //execution data properties
    public static final String WINGS_DATA_PROP_HAS_SIZE = WFLOW + "hasSize";
    public static final String WINGS_DATA_PROP_HAS_EXECUTION_DIAGRAM = WFLOW + "hasExecutionDiagram";
    public static final String WINGS_DATA_PROP_HAS_STATUS = WFLOW + "hasStatus";
    public static final String WINGS_DATA_PROP_HAS_START_TIME = WFLOW + "hasStartTime";
    public static final String WINGS_DATA_PROP_HAS_END_TIME = WFLOW + "hasEndTime";    
    public static final String WINGS_DATA_PROP_HAS_LICENSE = WFLOW + "hasLicense";
    public static final String WINGS_DATA_PROP_HAS_CREATION_TOOL = WFLOW + "creationTool";    
    
    //other
    public static final String RDFS_LABEL = PREFIX_RDFS+"label";
    
    //PROV-O statements
    //concepts
    public static final String PROV_ENTITY = PREFIX_PROV+ "Entity";
    public static final String PROV_ACTIVITY = PREFIX_PROV+ "Activity";
    public static final String PROV_AGENT = PREFIX_PROV+ "Agent";
    public static final String PROV_PLAN = PREFIX_PROV+ "Plan";
    public static final String PROV_BUNDLE = PREFIX_PROV+ "Bundle";
    
    //properties
    public static final String PROV_WAS_ATTRIBUTED_TO = PREFIX_PROV+ "wasAttributedTo";
    public static final String PROV_WAS_INFLUENCED_BY = PREFIX_PROV+ "wasInfluencedBy";
    public static final String PROV_USED = PREFIX_PROV+ "used";
    public static final String PROV_WGB = PREFIX_PROV+ "wasGeneratedBy";
    public static final String PROV_WAS_ASSOCIATED_WITH = PREFIX_PROV+ "wasAssociatedWith";
    public static final String PROV_AT_LOCATION = PREFIX_PROV+ "atLocation";
    public static final String PROV_VALUE = PREFIX_PROV+ "value";
    public static final String PROV_HAD_PRIMARY_SOURCE = PREFIX_PROV+ "hadPrimarySource";
            
}
