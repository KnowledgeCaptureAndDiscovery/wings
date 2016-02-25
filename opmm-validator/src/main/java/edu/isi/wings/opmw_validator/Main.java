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
 * Main class to validate OPMW traces and templates.
 * Intended use: to compare executions and templates in pairs 
 * (or a set of executions against a template).
 * @author Daniel Garijo
 */
public class Main {
    public static void main (String[] args){
        int i=0;
        String path ="";
        while(i< args.length){
            String s = args[i];
            if(s.equals("-d")){
                path = args[i+1];
                i++;
            }else{
                System.out.println("Usage: -d directory path with the OPMW RDF to validate");
                return;
            }
            i++;
        }
        OntModel m = Utils.loadDirectory("C:\\Users\\dgarijo\\Dropbox\\NetBeansProjects\\OPMWValidator\\testFiles");
//        OntModel m = Utils.loadDirectory(path);
        String s = Statistics.getStats(m);
        s+= Validator.validateRepo(m);
        System.out.println(s);
    }
}
