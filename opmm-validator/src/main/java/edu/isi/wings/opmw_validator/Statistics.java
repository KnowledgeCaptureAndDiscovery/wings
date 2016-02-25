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

/**
 * Class to get some OPMW/P-PLAN stats on the entities loaded in an endpoint.
 * @author dgarijo
 */
public class Statistics {
    public static String getStats (OntModel m){
        String s = "######STATISTICS ON THE LOADED FILES######\n";
        s+="NUMBER OF EXECUTIONS: "+ Utils.getCountOf(Queries.NUMBER_OF_EXEC_ACCCOUNTS, m, "countAcc")+"\n";
        s+="NUMBER OF EXECUTION ARTIFACTS: "+ Utils.getCountOf(Queries.NUMBER_OF_EXEC_ARTIFACTS, m, "countArt")+"\n";
        s+="\tNUMBER OF EXEC FILES: "+ Utils.getCountOf(Queries.NUMBER_OF_EXEC_VARIABLES, m, "countArt")+"\n";
        s+="\tNUMBER OF EXEC PARAMETERS: "+ Utils.getCountOf(Queries.NUMBER_OF_EXEC_PARAMETERS, m, "countArt")+"\n";
        s+="NUMBER OF EXECUTION PROCESSES: "+ Utils.getCountOf(Queries.NUMBER_OF_EXEC_PROC, m, "countProc")+"\n";
        s+="NUMBER OF TEMPLATES: "+ Utils.getCountOf(Queries.NUMBER_OF_TEMPLATES, m, "countT")+"\n";
        s+="NUMBER OF TEMPLATE ARTIFACTS: "+ Utils.getCountOf(Queries.NUMBER_OF_TEMPLATE_ARTIFACTS, m, "countArt")+"\n";
        s+="\tNUMBER OF TEMPLATE VARIABLES: "+ Utils.getCountOf(Queries.NUMBER_OF_TEMPLATE_VARIABLES, m, "countArt")+"\n";
        s+="\tNUMBER OF TEMPLATE PARAMENTERS: "+ Utils.getCountOf(Queries.NUMBER_OF_TEMPLATE_PARAMETERS, m, "countArt")+"\n";
        s+="NUMBER OF TEMPLATE PROCESSES: "+ Utils.getCountOf(Queries.NUMBER_OF_TEMPLATE_PROCESSES, m, "countProc")+"\n";
        s+="#######################################";
        return s;
    }
}
