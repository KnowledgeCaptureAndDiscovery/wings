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
public class Constants {
    
    /*****
    ONTOLOGY PREFIXES
    *****/
    public static String PREFIX_EXPORT_RESOURCE =  "http://www.opmw.org/export/resource/";
    public static final String PREFIX_EXTENSION =  "http://www.opmw.org/extension/";
    public static final String PREFIX_OPMW =  "http://www.opmw.org/ontology/";
    public static final String PREFIX_OPMO =  "http://openprovenance.org/model/opmo#";
    public static final String PREFIX_OPMV =  "http://purl.org/net/opmv/ns#";
    public static final String PREFIX_FOAF = "http://xmlns.com/foaf/0.1/";
    public static final String PREFIX_GEO = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    public static final String PREFIX_SIOC =  "http://rdfs.org/sioc/ns#";
    public static final String PREFIX_DCTERMS = "http://purl.org/dc/terms/";
    public static final String PREFIX_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String PREFIX_PROV = "http://www.w3.org/ns/prov#";
    public static final String PREFIX_WFLOW = "http://www.wings-workflows.org/ontology/workflow.owl#";//"http://www.isi.edu/2007/08/workflow.owl#";
    public static final String PREFIX_WEXEC = "http://www.wings-workflows.org/ontology/execution.owl#";
    public static final String PREFIX_WFINVOC = "http://purl.org/net/wf-invocation#";
    public static final String PREFIX_P_PLAN = "http://purl.org/net/p-plan#";

    /*****
    OPM AND OPMW CONCEPTS
    *****/
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

    /*****
    OPM relationships 
    *****/
    public static final String OPM_PROP_HAS_AGENT = PREFIX_OPMO+"hasAgent";
    public static final String OPM_PROP_ACCOUNT = PREFIX_OPMO+"account";
    public static final String OPM_PROP_USED = PREFIX_OPMV+"used";
    public static final String OPM_PROP_WGB = PREFIX_OPMV+"wasGeneratedBy";
    public static final String OPM_PROP_WCB = PREFIX_OPMV+"wasControlledBy";
   

    /*****
    OPMV classes
    *****/
    public static final String OPM_PROCESS = PREFIX_OPMV+CONCEPT_PROCESS;
    public static final String OPM_ARTIFACT = PREFIX_OPMV+CONCEPT_ARTIFACT;
    public static final String OPM_AGENT = PREFIX_OPMV+CONCEPT_AGENT;
    public static final String OPM_ACCOUNT = PREFIX_OPMO+CONCEPT_ACCOUNT;
    
    /*****
    OPMW classes
    *****/
    public static final String OPMW_WORKFLOW_EXECUTION_PROCESS = PREFIX_OPMW+CONCEPT_WORKFLOW_EXECUTION_PROCESS;
    public static final String OPMW_WORKFLOW_EXECUTION_ARTIFACT = PREFIX_OPMW+CONCEPT_WORKFLOW_EXECUTION_ARTIFACT;
    public static final String OPMW_WORKFLOW_TEMPLATE_PROCESS = PREFIX_OPMW+CONCEPT_WORKFLOW_TEMPLATE_PROCESS;
    public static final String OPMW_DATA_VARIABLE = PREFIX_OPMW+CONCEPT_DATA_VARIABLE;
    public static final String OPMW_PARAMETER_VARIABLE = PREFIX_OPMW+CONCEPT_PARAMETER_VARIABLE;
    public static final String OPMW_WORKFLOW_TEMPLATE = PREFIX_OPMW+CONCEPT_WORKFLOW_TEMPLATE;
    public static final String OPMW_WORKFLOW_TEMPLATE_ARTIFACT = PREFIX_OPMW+CONCEPT_WORKFLOW_TEMPLATE_ARTIFACT;
    public static final String OPMW_WORKFLOW_EXECUTION_ACCOUNT = PREFIX_OPMW+CONCEPT_WORKFLOW_EXECUTION_ACCOUNT;
    
    /*****
    OPMW relationships
    *****/
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

    /*****
    OPMW data properties
    *****/
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


    /*****
    Wings template constants. Prefixes
    *****/
    
//    public static final String AC = "http://www.isi.edu/ac/ontology.owl#";

    /*****
    Wings classes
    *****/
    public static final String WINGS_WF_TEMPLATE = PREFIX_WFLOW + "WorkflowTemplate";
    public static final String WINGS_NODE = PREFIX_WFLOW + "Node";
    public static final String WINGS_DATA_VARIABLE = PREFIX_WFLOW + "DataVariable";
    public static final String WINGS_PARAMETER_VARIABLE = PREFIX_WFLOW + "ParameterVariable";
    public static final String WINGS_INPUTLINK = PREFIX_WFLOW + "InputLink";
    public static final String WINGS_OUTPUTLINK = PREFIX_WFLOW + "OutputLink";
    public static final String WINGS_INOUTLINK = PREFIX_WFLOW + "InOutLink";
    public static final String WINGS_ROLE = PREFIX_WFLOW + "Role";
    public static final String WINGS_METADATA = PREFIX_WFLOW + "Metadata";
    public static final String WINGS_EXECUTION = PREFIX_WEXEC + "Execution";
    public static final String WINGS_EXECUTION_STEP= PREFIX_WEXEC + "ExecutionStep";
    /*****
    Wings properties
    *****/
    public static final String WINGS_PROP_HAS_DESTINATION_NODE = PREFIX_WFLOW + "hasDestinationNode";
    public static final String WINGS_PROP_HAS_ORIGIN_NODE = PREFIX_WFLOW + "hasOriginNode";
    public static final String WINGS_PROP_HAS_VARIABLE = PREFIX_WFLOW + "hasVariable";
    public static final String WINGS_PROP_HAS_DESTINATION_PORT = PREFIX_WFLOW + "hasDestinationPort";
    public static final String WINGS_PROP_HAS_ORIGIN_PORT = PREFIX_WFLOW + "hasOriginPort";
    public static final String WINGS_PROP_SATISFIES_ROLE = PREFIX_WFLOW + "satisfiesRole";
    public static final String WINGS_PROP_HAS_COMPONENT = PREFIX_WFLOW + "hasComponent";
    public static final String WINGS_PROP_MAPS_TO_VARIABLE = PREFIX_WFLOW + "mapsToVariable";
    public static final String WINGS_PROP_HAS_TEMPLATE = PREFIX_WEXEC + "hasTemplate";
    public static final String WINGS_PROP_HAS_EXPANDED_TEMPLATE = PREFIX_WEXEC + "hasExpandedTemplate";
    public static final String WINGS_PROP_HAS_PLAN = PREFIX_WEXEC + "hasPlan";
    public static final String WINGS_PROP_DERIVED_FROM = PREFIX_WFLOW + "derivedFrom";
//    public static final String WINGS_PROP_HAS_ARGUMENT_ID = AC + "hasArgumentID";
    
    //execution properties
    public static final String WINGS_PROP_HAS_NODE= PREFIX_WFLOW + "hasNode";
    public static final String WINGS_PROP_HAS_COMPONENT_TYPE= PREFIX_WFLOW + "hasComponentType";
    public static final String WINGS_PROP_HAS_ID = PREFIX_WFLOW + "hasID";
//    public static final String WINGS_PROP_HAS_TEMPLATE_DIAGRAM = WFLOW + "hasTemplateDiagram";
//    public static final String WINGS_PROP_HAS_EXECUTION_DIAGRAM = WFLOW + "hasExecutionDiagram"; 
    public static final String WINGS_PROP_HAS_COMPONENT_BINDING = PREFIX_WFLOW + "hasComponentBinding";
    public static final String WINGS_PROP_HAS_INPUT = PREFIX_WFLOW + "hasInput";
    public static final String WINGS_PROP_HAS_OUTPUT = PREFIX_WFLOW + "hasOutput";
    public static final String WINGS_PROP_HAS_DATA_BINDING = PREFIX_WFLOW + "hasDataBinding";
    public static final String WINGS_PROP_HAS_PARAMETER_BINDING = PREFIX_WFLOW + "hasParameterBinding";
    public static final String WINGS_PROP_HAS_LOCATION = PREFIX_WFLOW + "hasLocation";
    public static final String WINGS_PROP_USES_TEMPLATE = PREFIX_WFLOW + "usesTemplate";
    public static final String WINGS_PROP_HAS_URL = PREFIX_WFLOW + "hasURL";
    public static final String WINGS_PROP_HAS_EXECUTION_ENGINE = PREFIX_WFLOW + "hasExecutionEngine";
    public static final String WINGS_PROP_USES_TOOL = PREFIX_WFLOW + "usesTool";
    public static final String WINGS_PROP_HAS_USER = PREFIX_WEXEC + "hasUser";
    
    static String WINGS_PROP_HAS_RULE = "http://www.wings-workflows.org/ontology/component.owl#hasRule";
    /*****
    Wings data properties
    *****/
    public static final String WINGS_DATA_PROP_IS_CONCRETE = PREFIX_WFLOW + "isConcrete";
    public static final String WINGS_DATA_PROP_HAS_DIMENSIONALITY = PREFIX_WFLOW + "hasDimensionality";
    public static final String WINGS_DATA_PROP_CREATED_FROM = PREFIX_WFLOW + "createdFrom";
    public static final String WINGS_DATA_PROP_HAS_CONTRIBUTOR = PREFIX_WFLOW + "hasContributor";
    public static final String WINGS_DATA_PROP_HAS_PARAMETER_VALUE = PREFIX_WFLOW + "hasParameterValue";
    public static final String WINGS_DATA_PROP_HAS_DOCUMENTATION = PREFIX_WFLOW + "hasDocumentation";
    public static final String WINGS_DATA_PROP_HAS_VERSION = PREFIX_WFLOW + "hasVersion";
    public static final String WINGS_DATA_PROP_LAST_UPDATED_TIME = PREFIX_WFLOW + "lastUpdateTime";
    public static final String WINGS_DATA_PROP_HAS_TEMPLATE_DIAGRAM = PREFIX_WFLOW + "hasTemplateDiagram";
    public static final String WINGS_DATA_PROP_HAS_ROLE_ID = PREFIX_WFLOW + "hasRoleID";
    //execution data properties
    public static final String WINGS_DATA_PROP_HAS_SIZE = PREFIX_WFLOW + "hasSize";
    public static final String WINGS_DATA_PROP_HAS_EXECUTION_DIAGRAM = PREFIX_WEXEC + "hasExecutionDiagram";
    public static final String WINGS_DATA_PROP_HAS_STATUS = PREFIX_WEXEC + "hasExecutionStatus";
    public static final String WINGS_DATA_PROP_HAS_START_TIME = PREFIX_WEXEC + "hasStartTime";
    public static final String WINGS_DATA_PROP_HAS_END_TIME = PREFIX_WEXEC + "hasEndTime";    
    public static final String WINGS_DATA_PROP_HAS_LICENSE = PREFIX_WEXEC + "hasLicense";
    public static final String WINGS_DATA_PROP_HAS_CREATION_TOOL = PREFIX_WEXEC + "creationTool";  
    
    //component (to get variabletypes)
    public static final String WINGS_DATA_PROP_HAS_ARGUMENT_ID = "http://www.wings-workflows.org/ontology/component.owl#hasArgumentID";
    
    /**
     * WF-INVOC
     */
    public static final String WF_INVOC_DATA_PROP_HAS_CODE_BINDING = PREFIX_WFINVOC + "hasCodeBinding";
    public static final String WF_INVOC_DATA_PROP_HAS_DATA_BINDING = PREFIX_WFINVOC + "hasDataBinding";
        
    /*****
    PROV-O classes
    *****/
    public static final String PROV_ENTITY = PREFIX_PROV+ "Entity";
    public static final String PROV_ACTIVITY = PREFIX_PROV+ "Activity";
    public static final String PROV_AGENT = PREFIX_PROV+ "Agent";
    public static final String PROV_PLAN = PREFIX_PROV+ "Plan";
    public static final String PROV_BUNDLE = PREFIX_PROV+ "Bundle";
    
    /*****
    PROV-O properties
    *****/
    public static final String PROV_WAS_ATTRIBUTED_TO = PREFIX_PROV+ "wasAttributedTo";
    public static final String PROV_WAS_INFLUENCED_BY = PREFIX_PROV+ "wasInfluencedBy";
    public static final String PROV_USED = PREFIX_PROV+ "used";
    public static final String PROV_WGB = PREFIX_PROV+ "wasGeneratedBy";
    public static final String PROV_WAS_ASSOCIATED_WITH = PREFIX_PROV+ "wasAssociatedWith";
    public static final String PROV_AT_LOCATION = PREFIX_PROV+ "atLocation";
    public static final String PROV_VALUE = PREFIX_PROV+ "value";
    public static final String PROV_HAD_PRIMARY_SOURCE = PREFIX_PROV+ "hadPrimarySource";
    public static final String PROV_STARTED_AT_TIME = PREFIX_PROV+ "startedAtTime";
    public static final String PROV_ENDED_AT_TIME = PREFIX_PROV+ "endedAtTime";
    public static final String PROV_WAS_DERIVED_FROM = PREFIX_PROV+ "wasDerivedFrom";
                
    /*****
    other useful properties
    *****/
    public static final String RDFS_LABEL = PREFIX_RDFS+"label";
    public static final String PROP_HAS_CONTRIBUTOR = PREFIX_DCTERMS +"contributor";    
    public static final String RDFS_COMMENT = PREFIX_RDFS+"comment";
    
    /*****
    P-Plan classes
    *****/
    public static final String P_PLAN_PLAN = PREFIX_P_PLAN+ "Plan";
    public static final String P_PLAN_STEP = PREFIX_P_PLAN+ "Step";
    public static final String P_PLAN_Variable = PREFIX_P_PLAN+ "Variable";
    
    /*****
    P-Plan properties
    *****/
    public static final String P_PLAN_PROP_HAS_INPUT = PREFIX_P_PLAN+ "hasInputVar";
    public static final String P_PLAN_PROP_HAS_OUTPUT = PREFIX_P_PLAN+ "hasOutputVar";
    public static final String P_PLAN_PROP_IS_INTPUT_VAR_OF = PREFIX_P_PLAN+ "isInputVarOf";
    public static final String P_PLAN_PROP_IS_OUTPUT_VAR_OF = PREFIX_P_PLAN+ "isOutputVarOf";
    public static final String P_PLAN_PROP_CORRESPONDS_TO_STEP = PREFIX_P_PLAN+ "correspondsToStep";
    public static final String P_PLAN_PROP_CORRESPONDS_TO_VAR = PREFIX_P_PLAN+ "correspondsToVariable";
    public static final String P_PLAN_PROP_IS_STEP_OF_PLAN =  PREFIX_P_PLAN+ "isStepOfPlan";
    
    
    
}
