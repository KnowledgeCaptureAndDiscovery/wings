package edu.isi.wings.opmm;

/**
 * This class shows how the mapper should be run, with a complete example from a run and a template.
 * TO DO: accept the input files via console.
 * @author Daniel Garijo
 */
public class Main {
    public static void main(String [] args){
        System.out.println("Transform an execution to OPMW and PROV ");
        Mapper instance = new Mapper();
        String lib = "src\\main\\java\\sample_data\\new\\abs_words\\Library.owl";
        String execution = "src\\main\\java\\sample_data\\new\\abs_words\\Execution.owl";
        String template = "src\\main\\java\\sample_data\\new\\aquaflow_ntm\\Template.owl";
        String mode = "RDF/XML";
        String outFileOPMW = "testResult";
        String outFilePROV = "testResult2";
        String outFile = "testTemplate";
        //template with abstract nodes:
//        System.out.println("Transforming an execution of an abstract template...");
//        instance.transformWINGSResultsToOPMW(execution, lib, mode, outFileOPMW, outFilePROV);
//        
        //template with collections
        System.out.println("Transforming an execution of a template with collections...");
        lib="src\\main\\java\\sample_data\\new\\aquaflow_ntm\\Library.owl";
        execution="src\\main\\java\\sample_data\\new\\aquaflow_ntm\\Execution.owl";
        instance.transformWINGSResultsToOPMW(execution, lib, mode, outFileOPMW, outFilePROV,null);
        instance.transformWINGSElaboratedTemplateToOPMW(template, mode, outFile, null);
    }
}
