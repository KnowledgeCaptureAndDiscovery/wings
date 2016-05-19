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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ResultSetFormatter;

/**
 * Class designed to run the tests against a repository of templates and runs.
 * If possible, it would be optimal to run the validator against a template and
 * its runs.
 * @author Daniel Garijo
 */
public class Validator {
    
    /**
     * Runs a set of tests against a Jena model where the OPMW statements have 
     * been loaded.  For the moment we do it locally.
     * @param m OntModel
     * @return 
     */
    public static String validateRepo(OntModel m){
        String result = "##########REPORT##########\n";        
        int n=0;
        //execution artifacts
        result+="#TEST"+(++n)+": ALL EXECUTION ARTIFACTS SHOULD BELONG TO AN ACCOUNT.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_ARTIFACTS_WITHOUT_ACCOUNT, m, "countArt")))+"\n";
        result+="#TEST"+(++n)+": ALL EXECUTION ARTIFACTS SHOULD HAVE A LOCATION (VARIABLES) OR VALUE (PARAMETERS)..\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_ARTIFACTS_WITHOUT_LOCATION_OR_VALUE, m, "countArt")))+"\n";
        result+="#TEST"+(++n)+": ALL EXECUTION ARTIFACTS SHOULD BELONG TO A TEMPLATE VARIABLE OR PARAMETER THAT BELONGS TO A TEMPLATE.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_ARTIFACTS_WITHOUT_BINDING_TO_TEMPLATE_ARTIFACT, m, "countArt")))+"\n";
        result+="#TEST"+(++n)+": ALL EXECUTION ARTIFACTS SHOULD BE USED OR GENERATED BY A PROCESS.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_ARTIFACTS_WITHOUT_BINDING_TO_PROCESS, m, "countArt")))+"\n";
        //execution processes
        result+="#TEST"+(++n)+": ALL PROCESSES SHOULD BELONG TO AN ACCOUNT.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_PROCESSES_WITHOUT_ACCOUNT, m, "countProc")))+"\n";
        result+="#TEST"+(++n)+": ALL PROCESSES SHOULD USE OR GENERATE SOME ARTIFACT.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_PROCESSES_NOT_BOUND_TO_ARTIFACT, m, "countProc")))+"\n";
        result+="#TEST"+(++n)+": ALL PROCESSES SHOULD HAVE AN EXECUTION CODE ASSOCIATED TO THEM.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_PROCESSES_WITHOUT_CODE, m, "countProc")))+"\n";
        result+="#TEST"+(++n)+": ALL PROCESSES SHOULD CORRESPOND TO A PROCESS THAT BELONGS TO A TEMPLATE.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_PROCESSES_WITHOUT_CORRECT_TEMPLATE_BINDING, m, "countProc")))+"\n";
        //execution accounts
        result+="#TEST"+(++n)+": ALL EXECUTIONS MUST BELONG TO A TEMPLATE.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_EXECUTIONS_WITHOUT_TEMPLATE, m, "countAcc")))+"\n";
        result+="#TEST"+(++n)+": ALL EXECUTIONS MUST HAVE AN END TIME, A START TIME AND A STATUS.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_EXECUTIONS_WITHOUT_TIME_OR_STATUS, m, "countAcc")))+"\n";
        result+="#TEST"+(++n)+": (OPTIONAL TEST) AN ACCOUNT MAY HAVE A POINTER TO THE ORIGINAL LOG FILE.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_EXECUTIONS_WITHOUT_LOG_FILE, m, "countAcc")))+"\n";
        //template artifacts
        result+="#TEST"+(++n)+": ALL TEMPLATE ARTIFCATS MUST BELONG TO A TEMPLATE.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_ARTIFACTS_WITHOUT_TEMPLATE, m, "countArt")))+"\n";
        result+="#TEST"+(++n)+": ALL TEMPLATE ARTIFACTS MUST BE CONNECTED TO A TEMPLATE PROCESS (testing in  OPMW).\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_ARTIFACTS_WITHOUT_PROCESS_OPMW, m, "countArt")))+"\n";
        result+="#TEST"+(++n)+": ALL TEMPLATE ARTIFACTS MUST BE CONNECTED TO A TEMPLATE PROCESS (testing in P-PLAN).\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_ARTIFACTS_WITHOUT_PROCESS_P_PLAN, m, "countArt")))+"\n";
        //template processes
        result+="#TEST"+(++n)+": ALL TEMPLATE PROCESSES MUST BELONG TO A TEMPLATE (test in OPMW).\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_PROCESSES_WITHOUT_TEMPLATE_OPMW, m, "countProc")))+"\n";
        result+="#TEST"+(++n)+": ALL TEMPLATE PROCESSES MUST BELONG TO A TEMPLATE (test in PPlan).\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_PROCESSES_WITHOUT_TEMPLATE_PPLAN, m, "countProc")))+"\n";
//        ResultSetFormatter.out(System.out,Utils.queryLocalRepository(Queries.SELECT_TEMPL_PROCESSES_WITHOUT_TEMPLATE_PPLAN, m));
        result+="#TEST"+(++n)+": ARE THERE ANY UNDECLARED WORKFLOW TEMPLATE PROCESSES?.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_UNDECLARED_PROCESSES, m, "countProc")))+"\n";
        result+="#TEST"+(++n)+": ALL TEMPLATE PROCESSES MUST USE OR GENERATE A TEMPLATE ARTIFACT (test in OPMW).\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_OPMW, m, "countProc")))+"\n";
        result+="#TEST"+(++n)+": ALL TEMPLATE PROCESSES MUST USE OR GENERATE A TEMPLATE ARTIFACT (test in P-PLAN).\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_PROCESS_WITHOUT_BINDING_TO_ARTIFACT_PPLAN, m, "countProc")))+"\n";
        
        //templates
        result+="#TEST"+(++n)+": (OPTIONAL TEST) TEMPLATES SHOULD HAVE A VERSION NUMBER.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_WITHOUT_VERSION_NUMBER, m, "countT")))+"\n";
        result+="#TEST"+(++n)+": (OPTIONAL TEST) TEMPLATES SHOULD HAVE A POINTER TO THE NATIVE SYSTEM TEMPLATE.\n";
        result+="\t"+isTestFailed(Integer.parseInt(Utils.getCountOf(Queries.COUNT_TEMPL_WITHOUT_NATIVE_SYS_TEMPL, m, "countT")))+"\n";
        
        return result;
    }
    
    private static String isTestFailed(int problems){
        if(problems<=0){
            return "--->OK!";
        }else return "--->FAILED!! Number of problems found: "+ problems;
    }
}