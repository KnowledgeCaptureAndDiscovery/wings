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

/**
 * Queries that contain the tests to be performed against templates and runs.
 * @author Daniel Garijo
 */
public class Queries {
    
    //commonly used predicates for artifacts and processes. The queries are equivalent, 
    //but I prefer to declare them this way for clarity.
    private static final String ARTIFACT_SELECTION = "select distinct ?artifact where{";
    private static final String ARTIFACT_COUNT = "select (count(distinct ?artifact) as ?countArt) where {";
    
    private static final String PROCESS_SELECTION = "select distinct ?process where{";
    private static final String PROCESS_COUNT = "select (count(distinct ?process) as ?countProc) where {";
    
    private static final String ACCOUNT_SELECTION = "select distinct ?acc where{";
    private static final String ACCOUNT_COUNT = "select (count(distinct ?acc) as ?countAcc) where {";
    
    private static final String TEMPLATE_SELECTION = "select distinct ?t where{";
    private static final String TEMPLATE_COUNT = "select (count(distinct ?t) as ?countT) where {";
    
    //stats
    //number of artifacts
    public static final String NUMBER_OF_EXEC_ARTIFACTS = ARTIFACT_COUNT+ "?artifact a <http://www.opmw.org/ontology/WorkflowExecutionArtifact>.}";
    public static final String NUMBER_OF_EXEC_PARAMETERS = ARTIFACT_COUNT+ "?artifact a <http://www.opmw.org/ontology/WorkflowExecutionArtifact>."
            + "?artifact <http://www.opmw.org/ontology/hasValue> ?value.}";
    public static final String NUMBER_OF_EXEC_VARIABLES = ARTIFACT_COUNT+ "?artifact a <http://www.opmw.org/ontology/WorkflowExecutionArtifact>."
            + "?artifact <http://www.opmw.org/ontology/hasLocation> ?location.}";
    //number of processes
    public static final String NUMBER_OF_EXEC_PROC = PROCESS_COUNT+ "?process a <http://www.opmw.org/ontology/WorkflowExecutionProcess>.}";
    //number of executions (accounts)
    public static final String NUMBER_OF_EXEC_ACCCOUNTS = ACCOUNT_COUNT +"?acc a <http://www.opmw.org/ontology/WorkflowExecutionAccount>}";
    //Number of template artifacts
    public static final String NUMBER_OF_TEMPLATE_ARTIFACTS = ARTIFACT_COUNT +"?artifact a <http://www.opmw.org/ontology/WorkflowTemplateArtifact>}";
    //number of parameters in template
    public static final String NUMBER_OF_TEMPLATE_PARAMETERS = ARTIFACT_COUNT +"?artifact a <http://www.opmw.org/ontology/ParameterVariable>}";
    //number of data variables in template
    public static final String NUMBER_OF_TEMPLATE_VARIABLES = ARTIFACT_COUNT +"?artifact a <http://www.opmw.org/ontology/DataVariable>}";
    //number of template processes
    public static final String NUMBER_OF_TEMPLATE_PROCESSES = PROCESS_COUNT +"?process a <http://www.opmw.org/ontology/WorkflowTemplateProcess>}";
    //number of templates
    public static final String NUMBER_OF_TEMPLATES = TEMPLATE_COUNT +"?t a <http://www.opmw.org/ontology/WorkflowTemplate>}";
    
    /**** ARTIFACT RULES ****/
    
    //REQ 1: ALL ARTIFACTS SHOULD BELONG TO AN ACCOUNT
    private static final String ARTIFACTS_WITHOUT_ACCOUNT = 
              "?artifact a <http://www.opmw.org/ontology/WorkflowExecutionArtifact>."
            + "FILTER NOT EXISTS { "
            + "?artifact  <http://openprovenance.org/model/opmo#account> ?account"
            + "}"
            + "}";
    public static final String SELECT_ARTIFACTS_WITHOUT_ACCOUNT = ARTIFACT_SELECTION + ARTIFACTS_WITHOUT_ACCOUNT;
    public static final String COUNT_ARTIFACTS_WITHOUT_ACCOUNT = ARTIFACT_COUNT + ARTIFACTS_WITHOUT_ACCOUNT;
    
    //REQ2: ALL EXECUTION ARTIFACTS SHOULD HAVE A LOCATION (VARIABLES) OR VALUE (PARAMETERS).
    private static final String ARTIFACTS_WITHOUT_LOCATION_OR_VALUE = 
            "?artifact a <http://www.opmw.org/ontology/WorkflowExecutionArtifact>."
            + "FILTER NOT EXISTS { "
            + "{?artifact  <http://www.opmw.org/ontology/hasValue> ?account.}"
            + "UNION"
            + "{?artifact  <http://www.opmw.org/ontology/hasLocation> ?loc}"
            + "}"
            + "}";
    public static final String SELECT_ARTIFACTS_WITHOUT_LOCATION_OR_VALUE = ARTIFACT_SELECTION + ARTIFACTS_WITHOUT_LOCATION_OR_VALUE;
    public static final String COUNT_ARTIFACTS_WITHOUT_LOCATION_OR_VALUE = ARTIFACT_COUNT+ ARTIFACTS_WITHOUT_LOCATION_OR_VALUE;
    
    //REQ3: ALL EXECUTION ARTIFACTS SHOULD BELONG TO A TEMPLATE VARIABLE OR PARAMETER THAT BELONGS TO A TEMPLATE
    private static final String ARTIFACTS_WITHOUT_BINDING_TO_TEMPLATE_ARTIFACT = 
            "?artifact a <http://www.opmw.org/ontology/WorkflowExecutionArtifact>."
            + "FILTER NOT EXISTS{"
            + "?artifact <http://www.opmw.org/ontology/correspondsToTemplateArtifact> ?tempArtifact."
            + "{?tempArtifact <http://www.opmw.org/ontology/isParameterOfTemplate> ?template}"
            + "UNION"
            + "{?tempArtifact <http://www.opmw.org/ontology/isVariableOfTemplate> ?template}"
            + "}"
            + "}";
    
    public static final String SELECT_ARTIFACTS_WITHOUT_BINDING_TO_TEMPLATE_ARTIFACT = ARTIFACT_SELECTION + ARTIFACTS_WITHOUT_BINDING_TO_TEMPLATE_ARTIFACT;
    public static final String COUNT_ARTIFACTS_WITHOUT_BINDING_TO_TEMPLATE_ARTIFACT = ARTIFACT_COUNT + ARTIFACTS_WITHOUT_BINDING_TO_TEMPLATE_ARTIFACT;
    
    //REQ4: ALL EXECUTION ARTIFACTS SHOULD BE USED OR GENERATED BY A PROCESS
    private static final String ARTIFACTS_WITHOUT_BINDING_TO_PROCESS = 
            "?artifact a <http://www.opmw.org/ontology/WorkflowExecutionArtifact>."
            + "FILTER NOT EXISTS {"
            + "{?process <http://purl.org/net/opmv/ns#used> ?artifact }"
            + "UNION"
            + "{?artifact <http://purl.org/net/opmv/ns#wasGeneratedBy> ?process}"
            + "}"
            + "}";
    public static final String SELECT_ARTIFACTS_WITHOUT_BINDING_TO_PROCESS = ARTIFACT_SELECTION + ARTIFACTS_WITHOUT_BINDING_TO_PROCESS;
    public static final String COUNT_ARTIFACTS_WITHOUT_BINDING_TO_PROCESS = ARTIFACT_COUNT + ARTIFACTS_WITHOUT_BINDING_TO_PROCESS;
    

    /**** PROCESS RULES ****/
    
    //REQ1: ALL PROCESSES SHOULD BELONG TO AN ACCOUNT
    private static final String PROCESSES_WITHOUT_ACCOUNT=
            "?process a <http://www.opmw.org/ontology/WorkflowExecutionProcess>."
            + "FILTER NOT EXISTS { ?process  <http://openprovenance.org/model/opmo#account> ?account}"
            + "}";
    public static final String SELECT_PROCESSES_WITHOUT_ACCOUNT= PROCESS_SELECTION+ PROCESSES_WITHOUT_ACCOUNT;
    public static final String COUNT_PROCESSES_WITHOUT_ACCOUNT= PROCESS_COUNT+ PROCESSES_WITHOUT_ACCOUNT;
    
    //REQ2: ALL PROCESSES SHOULD USE OR GENERATE SOME ARTIFACT
    private static final String PROCESSES_NOT_BOUND_TO_ARTIFACT = 
            "?process a <http://www.opmw.org/ontology/WorkflowExecutionProcess>."
            + "FILTER NOT EXISTS {"
            + "{?process <http://purl.org/net/opmv/ns#used> ?artifact }"
            + "UNION"
            + "{?artifact <http://purl.org/net/opmv/ns#wasGeneratedBy> ?process}"
            + "}"
            + "}";
    public static final String SELECT_PROCESSES_NOT_BOUND_TO_ARTIFACT = PROCESS_SELECTION + PROCESSES_NOT_BOUND_TO_ARTIFACT;
    public static final String COUNT_PROCESSES_NOT_BOUND_TO_ARTIFACT = PROCESS_COUNT + PROCESSES_NOT_BOUND_TO_ARTIFACT;
    
    //REQ3: ALL PROCESSES SHOULD HAVE AN EXECUTION CODE ASSOCIATED TO THEM. 
    private static final String PROCESSES_WITHOUT_CODE = 
            "?process a <http://www.opmw.org/ontology/WorkflowExecutionProcess>."
            + "FILTER NOT EXISTS {"
            + "?process <http://www.opmw.org/ontology/hasExecutableComponent> ?comp."
            + "?comp <http://www.opmw.org/ontology/hasLocation> ?loc"
            + "}"
            + "}";
    public static final String SELECT_PROCESSES_WITHOUT_CODE = PROCESS_SELECTION + PROCESSES_WITHOUT_CODE;
    public static final String COUNT_PROCESSES_WITHOUT_CODE = PROCESS_COUNT + PROCESSES_WITHOUT_CODE;
    
    //REQ4: ALL PROCESSES SHOULD CORRESPOND TO A PROCESS THAT BELONGS TO A TEMPLATE
    private static final String PROCESSES_WITHOUT_TEMPLATE_BINDING = 
            "?process a <http://www.opmw.org/ontology/WorkflowExecutionProcess>."
            + "FILTER NOT EXISTS {"
            + "?process <http://www.opmw.org/ontology/correspondsToTemplateProcess> ?templProc."
            + "?templProc <http://www.opmw.org/ontology/isStepOfTemplate> ?templ"
            + "}"
            + "}";
    public static final String SELECT_PROCESSES_WITHOUT_TEMPLATE_BINDING = PROCESS_SELECTION + PROCESSES_WITHOUT_TEMPLATE_BINDING;
    public static final String COUNT_PROCESSES_WITHOUT_CORRECT_TEMPLATE_BINDING = PROCESS_COUNT + PROCESSES_WITHOUT_TEMPLATE_BINDING;
    
        
    /**** EXECUTION RULES ****/
    //REQ1: ALL EXECUTIONS MUST BELONG TO A TEMPLATE
    private static final String EXECUTIONS_WITHOUT_TEMPLATE =""
            + "?acc a <http://www.opmw.org/ontology/WorkflowExecutionAccount>."
            + "FILTER NOT EXISTS{"
            + "?acc <http://www.opmw.org/ontology/correspondsToTemplate> ?templ."
            + "?templ a <http://www.opmw.org/ontology/WorkflowTemplate>"
            + "}"
            + "}";
    public static final String SELECT_EXECUTIONS_WITHOUT_TEMPLATE =ACCOUNT_SELECTION+EXECUTIONS_WITHOUT_TEMPLATE;
    public static final String COUNT_EXECUTIONS_WITHOUT_TEMPLATE =ACCOUNT_COUNT+EXECUTIONS_WITHOUT_TEMPLATE;
    
    //REQ2: ALL EXECUTIONS MUST HAVE AN END TIME, A START TIME AND A STATUS
    private static final String EXECUTIONS_WITHOUT_TIME_OR_STATUS=""
            + "?acc a <http://www.opmw.org/ontology/WorkflowExecutionAccount>."
            + "FILTER NOT EXISTS{"
            + "?acc <http://www.opmw.org/ontology/overallStartTime> ?t."
            + "?acc <http://www.opmw.org/ontology/overallEndTime> ?t2."
            + "?acc <http://www.opmw.org/ontology/hasStatus> ?s."
            + "}"
            + "}";
    public static final String SELECT_EXECUTIONS_WITHOUT_TIME_OR_STATUS=ACCOUNT_SELECTION+EXECUTIONS_WITHOUT_TIME_OR_STATUS;
    public static final String COUNT_EXECUTIONS_WITHOUT_TIME_OR_STATUS=ACCOUNT_COUNT+EXECUTIONS_WITHOUT_TIME_OR_STATUS;
    
    //OPT: AN ACCOUNT MAY HAVE A POINTER TO THE ORIGINAL LOG FILE
    private static final String EXECUTIONS_WITHOUT_LOG_FILE=""
            + "?acc a <http://www.opmw.org/ontology/WorkflowExecutionAccount>."
            + "FILTER NOT EXISTS{"
            + "?acc <http://www.opmw.org/ontology/hasOriginalLogFile> ?log."
            + "}"
            + "}";
    public static final String SELECT_EXECUTIONS_WITHOUT_LOG_FILE=ACCOUNT_SELECTION+EXECUTIONS_WITHOUT_LOG_FILE;
    public static final String COUNT_EXECUTIONS_WITHOUT_LOG_FILE=ACCOUNT_COUNT+EXECUTIONS_WITHOUT_LOG_FILE;
    
    /**** TEMPLATE ARTIFACT RULES ****/
    //REQ1: ALL TEMPLATE ARTIFCATS MUST BELONG TO A TEMPLATE
    private static final String TEMPL_ARTIFACTS_WITHOUT_TEMPLATE=""
            + "?artifact a <http://www.opmw.org/ontology/WorkflowTemplateArtifact>."
            + "FILTER NOT EXISTS{"
            + "{?artifact <http://www.opmw.org/ontology/isVariableOfTemplate> ?temp.}"
            + "UNION"
            + "{?artifact <http://www.opmw.org/ontology/isParameterOfTemplate> ?temp.}"
            + "}"
            + "}";
    public static final String SELECT_TEMPL_ARTIFACTS_WITHOUT_TEMPLATE=ARTIFACT_SELECTION+TEMPL_ARTIFACTS_WITHOUT_TEMPLATE;
    public static final String COUNT_TEMPL_ARTIFACTS_WITHOUT_TEMPLATE=ARTIFACT_COUNT+TEMPL_ARTIFACTS_WITHOUT_TEMPLATE;
   
    //REQ2: ALL TEMPLATE ARTIFACTS MUST BE CONNECTED TO A TEMPLATE PROCESS
    private static final String TEMPL_ARTIFACTS_WITHOUT_PROCESS_OPMW=""
            + "?artifact a <http://www.opmw.org/ontology/WorkflowTemplateArtifact>."
            + "FILTER NOT EXISTS{"
            + "{?artifact <http://www.opmw.org/ontology/isGeneratedBy> ?process.}"
            + "UNION"
            + "{?process <http://www.opmw.org/ontology/uses> ?artifact.}"
            + "}"
            + "}";
    public static final String SELECT_TEMPL_ARTIFACTS_WITHOUT_PROCESS_OPMW=ARTIFACT_SELECTION+TEMPL_ARTIFACTS_WITHOUT_PROCESS_OPMW;
    public static final String COUNT_TEMPL_ARTIFACTS_WITHOUT_PROCESS_OPMW=ARTIFACT_COUNT+TEMPL_ARTIFACTS_WITHOUT_PROCESS_OPMW;
    
    //same in p-plan
    private static final String TEMPL_ARTIFACTS_WITHOUT_PROCESS_P_PLAN=""
            + "?artifact a <http://purl.org/net/p-plan#Variable>."
            + "FILTER NOT EXISTS{"
            + "{?artifact <http://purl.org/net/p-plan#isOutputVarOf> ?process.}"
            + "UNION"
            + "{?process1 <http://purl.org/net/p-plan#hasInputVar> ?artifact.}"
            + "}"
            + "}";
    public static final String SELECT_TEMPL_ARTIFACTS_WITHOUT_PROCESS_P_PLAN=ARTIFACT_SELECTION+TEMPL_ARTIFACTS_WITHOUT_PROCESS_P_PLAN;
    public static final String COUNT_TEMPL_ARTIFACTS_WITHOUT_PROCESS_P_PLAN=ARTIFACT_COUNT+TEMPL_ARTIFACTS_WITHOUT_PROCESS_P_PLAN;
    
    
    /**** TEMPLATE PROCESS RULES ****/
    //REQ1: ALL TEMPLATE PROCESSES MUST BELONG TO A TEMPLATE
    private static final String TEMPL_PROCESSES_WITHOUT_TEMPLATE_OPMW=""
            + "?process a <http://www.opmw.org/ontology/WorkflowTemplateProcess>."
            + "FILTER NOT EXISTS{"
            + "?process <http://www.opmw.org/ontology/isStepOfTemplate> ?temp."
            + "}"
            + "}";
    public static final String SELECT_TEMPL_PROCESSES_WITHOUT_TEMPLATE_OPMW=PROCESS_SELECTION+TEMPL_PROCESSES_WITHOUT_TEMPLATE_OPMW;
    public static final String COUNT_TEMPL_PROCESSES_WITHOUT_TEMPLATE_OPMW=PROCESS_COUNT+TEMPL_PROCESSES_WITHOUT_TEMPLATE_OPMW;
   
    private static final String TEMPL_PROCESSES_WITHOUT_TEMPLATE_PPLAN=""
            + "?process a <http://purl.org/net/p-plan#Step>."
            + "FILTER NOT EXISTS{"
            + "?process <http://purl.org/net/p-plan#isStepOfPlan> ?temp."
            + "}"
            + "}";
    public static final String SELECT_TEMPL_PROCESSES_WITHOUT_TEMPLATE_PPLAN=PROCESS_SELECTION+TEMPL_PROCESSES_WITHOUT_TEMPLATE_PPLAN;
    public static final String COUNT_TEMPL_PROCESSES_WITHOUT_TEMPLATE_PPLAN=PROCESS_COUNT+TEMPL_PROCESSES_WITHOUT_TEMPLATE_PPLAN;
   
    //REQ2: ARE THERE ANY UNDECLARED WORKFLOW TEMPLATE PROCESSES?
    private static final String UNDECLARED_PROCESSES=""
            + "{?process <http://www.opmw.org/ontology/uses> ?a.}"
            + "UNION"
            + "{?a <http://www.opmw.org/ontology/isGeneratedBy> ?process}"
            + "FILTER NOT EXISTS{"
            + "?process a <http://www.opmw.org/ontology/WorkflowTemplateProcess>."
            + "}"
            + "}";
    public static final String SELECT_UNDECLARED_PROCESSES=PROCESS_SELECTION+UNDECLARED_PROCESSES;
    public static final String COUNT_UNDECLARED_PROCESSES=PROCESS_COUNT+UNDECLARED_PROCESSES;
    
    //REQ3: ALL TEMPLATE PROCESSES MUST USE OR GENERATE A TEMPLATE ARTIFACT
    private static final String TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_OPMW=""
            + "?process a <http://www.opmw.org/ontology/WorkflowTemplateProcess>"            
            + "FILTER NOT EXISTS{"
            + "{?process <http://www.opmw.org/ontology/uses> ?a}"
            + "UNION"
            + "{?a1 <http://www.opmw.org/ontology/isGeneratedBy> ?process}"
            + "}"
            + "}";
    public static final String SELECT_TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_OPMW=PROCESS_SELECTION+TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_OPMW;
    public static final String COUNT_TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_OPMW=PROCESS_COUNT+TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_OPMW;
    
    private static final String TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_PPLAN=""
            + "?process a <http://purl.org/net/p-plan#Step>"            
            + "FILTER NOT EXISTS{"
            + "{?process <http://purl.org/net/p-plan#hasInputVar> ?a}"
            + "UNION"
            + "{?a1 <http://purl.org/net/p-plan#isOutputVarOf> ?process}"
            + "}"
            + "}";
    public static final String SELECT_TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_PPLAN=PROCESS_SELECTION+TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_PPLAN;
    public static final String COUNT_TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_PPLAN=PROCESS_COUNT+TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_PPLAN;
    
    /**** TEMPLATE RULES ****/
    //OPT: TEMPLATES SHOULD HAVE A VERSION NUMBER
    private static final String TEMPL_WITHOUT_VERSION_NUMBER=""
            + "?t a <http://www.opmw.org/ontology/WorkflowTemplate>"            
            + "FILTER NOT EXISTS{"
            + "?t <http://www.opmw.org/ontology/versionNumber> ?number"
            + "}"
            + "}";
    public static final String SELECT_TEMPL_WITHOUT_VERSION_NUMBER=TEMPLATE_SELECTION+TEMPL_WITHOUT_VERSION_NUMBER;
    public static final String COUNT_TEMPL_WITHOUT_VERSION_NUMBER=TEMPLATE_COUNT+TEMPL_WITHOUT_VERSION_NUMBER;
    
    //OPT: TEMPLATES SHOULD HAVE A POINTER TO THE NATIVE SYSTEM TEMPLATE
    private static final String TEMPL_WITHOUT_NATIVE_SYS_TEMPL=""
            + "?t a <http://www.opmw.org/ontology/WorkflowTemplate>"            
            + "FILTER NOT EXISTS{"
            + "?t <http://www.opmw.org/ontology/hasNativeSystemTemplate> ?temp2"
            + "}"
            + "}";
    public static final String SELECT_TEMPL_WITHOUT_NATIVE_SYS_TEMPL=TEMPLATE_SELECTION+TEMPL_WITHOUT_NATIVE_SYS_TEMPL;
    public static final String COUNT_TEMPL_WITHOUT_NATIVE_SYS_TEMPL=TEMPLATE_COUNT+TEMPL_WITHOUT_NATIVE_SYS_TEMPL;
    
    
    //other queries and tests
    public static String retrieveAllWEAccounts (){
        String query = "select ?acc where{"
                + "?acc a <http://www.opmw.org/ontology/WorkflowExecutionAccount>."
                + "}";
        return query;                
    }
}
