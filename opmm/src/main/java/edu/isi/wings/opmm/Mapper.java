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

import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;

/**
 *
 * @author Daniel Garijo
 */
public class Mapper {
    private OntModel WINGSModelTemplate;
    private OntModel WINGSExecutionResults;
    private OntModel OPMWModel;
    private OntModel PROVModel;
    private String taxonomyURL;
    public Mapper(){

    }

    /**
     * Query a local repository, specified in the second argument
     * @param queryIn sparql query to be performed
     * @param repository repository on which the query will be performed
     * @return 
     */
    private ResultSet queryLocalRepository(String queryIn, OntModel repository){
        Query query = QueryFactory.create(queryIn);
        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, repository);
        ResultSet rs =  qe.execSelect();
        //qe.close();
        return rs;
    }
    
    /**
     * Query the local OPMW repository
     * @param queryIn input query
     * @return 
     */
    private ResultSet queryLocalOPMRepository(String queryIn) {
        return queryLocalRepository(queryIn, OPMWModel);
    }
    /**
     * Query the local Wings repository
     * @param queryIn input query
     * @return 
     */
    private ResultSet queryLocalWINGSTemplateModelRepository(String queryIn) {
        return queryLocalRepository(queryIn, WINGSModelTemplate);
    }
    /**
     * Query the local results repository
     * @param queryIn input query
     * @return 
     */
    private ResultSet queryLocalWINGSResultsRepository(String queryIn) {
        return queryLocalRepository(queryIn, WINGSExecutionResults);
    }
    /**
     * Method for accessing the URl of the domain ontology.
     * @param queryIn input query
     * @return 
     */
    private String getTaxonomyURL(OntModel m) throws Exception{
        if(taxonomyURL!=null)return taxonomyURL;
        else{
            ResultSet rs = this.queryLocalRepository(Queries.queryGetTaxonomyURL(), m);
            if(rs.hasNext()){
                taxonomyURL = rs.next().getResource("?taxonomyURL").getNameSpace();
            }else{
                throw new Exception("Taxonomy is not available");
            }
        }
        return taxonomyURL;
    }

    /**
     * Loads the files to the Local repository, to prepare conversion to OPM
     * @param template. Workflow template
     * @param modeFile. syntax of the files to load: "RDF/XML", "N-TRIPLE", "TURTLE" (or "TTL") and "N3"
     * @throws java.lang.Exception
     */
    public void loadTemplateFileToLocalRepository(String template, String modeFile) throws Exception{
        WINGSModelTemplate = ModelFactory.createOntologyModel();//ModelFactory.createDefaultModel();
        InputStream in = FileManager.get().open(template);
        if (in == null) {
            throw new IllegalArgumentException("File: " + template + " not found");
        }
        // read the RDF/XML file
        WINGSModelTemplate.read(in, null, modeFile);
        System.out.println("File "+template+" loaded into the model template");
//        getACDCfromModel(true);
        //load the taxonomy as well
        loadTaxonomy(WINGSModelTemplate);
    }
    
    /**
     * Method to load the domain specific taxonomy. Used to determine the node types.
     * @param m model where to load the taxonomy
     * @throws Exception 
     */
    private void loadTaxonomy(OntModel m)throws Exception{
         System.out.println("Attempting to load the domain specific domain ...");
        //since this is NOT included in the template per se, we need to download it
        System.out.println("Importing taxonomy at: "+ getTaxonomyURL(m));
        m.read(getTaxonomyURL(m));
        System.out.println("Done");
    }

    /**
     * Method to load an execution file to a local model.
     * @param executionResults owl file with the execution
     * @param mode type of serialization. E.g., "RDF/XML"
     */
    public void loadResultFileToLocalRepository(String executionResults, String mode){
        //InputStream in2 = FileManager.get().open(executionResults);
        InputStream in2 = FileManager.get().open(executionResults.replaceAll("#.*$", ""));
        if (in2 == null){
            throw new IllegalArgumentException("File: " + executionResults + " not found");
        }
        
        WINGSExecutionResults.read(in2, null, mode);
        System.out.println("File "+executionResults+" loaded into the execution results");
    }

    /**
     * Method to transform a Wings template to OPMW, PROV and P-Plan
     * @param template template file
     * @param mode rdf serialization of the file
     * @param outFile output file name
     * @return Template URI assigned to identify the template
     */
    //public String transformWINGSElaboratedTemplateToOPMW(String template,String mode, String outFile){
    public String transformWINGSElaboratedTemplateToOPMW(String template,String mode, String outFile, String templateName){
        //clean previous transformations
        if(WINGSModelTemplate!=null){
            WINGSModelTemplate.removeAll();
        }
        if(OPMWModel!=null){
            OPMWModel.removeAll();            
        }
        OPMWModel = ModelFactory.createOntologyModel(); //inicialization of the model        
        try{
            //load the template file to WINGSModel (already loads the taxonomy as well
            this.loadTemplateFileToLocalRepository(template, mode);            
        }catch(Exception e){
            System.err.println("Error "+e.getMessage());
            return "";
        }
        //retrieval of the name of the workflowTemplate
        String queryNameWfTemplate = Queries.queryNameWfTemplate();
        //String templateName = null, templateName_ = null;
        String templateName_ = null;
        //System.out.println(queryNameWfTemplate);
        ResultSet r = queryLocalWINGSTemplateModelRepository(queryNameWfTemplate);
        if(r.hasNext()){//there should be just one local name per template
            QuerySolution qs = r.next();
            Resource res = qs.getResource("?name");
            Literal v = qs.getLiteral("?ver");
            if (templateName==null){
                templateName = res.getLocalName();
                if(templateName == null){
                    System.out.println("Error: No Template specified.");
                    return "";
                }
            }
            templateName_=templateName+"_";
            //add the template as a provenance graph
            this.addIndividual(OPMWModel,templateName, Constants.OPMW_WORKFLOW_TEMPLATE, templateName);
            
            OntClass cParam = OPMWModel.createClass(Constants.P_PLAN_PLAN);
            cParam.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+encode(templateName));
            
            if(v!=null){
                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,""+ v.getInt(),
                        Constants.OPMW_DATA_PROP_VERSION_NUMBER, XSDDatatype.XSDint);
            }
            //add the uri of the original log file (native system template)
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName, 
                    res.getURI(),Constants.OPMW_DATA_PROP_HAS_NATIVE_SYSTEM_TEMPLATE, XSDDatatype.XSDanyURI);
            
            //Prov-o interoperability : workflow template           
            OntClass plan = OPMWModel.createClass(Constants.PROV_PLAN);
            plan.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+encode(templateName));
            
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                res.getURI(),Constants.PROV_HAD_PRIMARY_SOURCE, XSDDatatype.XSDanyURI);
        }        
        
        //additional metadata from the template.
        String queryMetadata = Queries.queryMetadata();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryMetadata);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Literal doc = qs.getLiteral("?doc");
            Literal contrib = qs.getLiteral("?contrib");
            Literal time = qs.getLiteral("?time");
            Literal license = qs.getLiteral("?license");
            Resource diagram = qs.getResource("?diagram");
            //ask for diagram here: hasTemplateDiagram xsd:anyURI (png)
            if(doc!=null){
                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,doc.getString(),
                        Constants.OPMW_DATA_PROP_HAS_DOCUMENTATION);
            }
            if(contrib!=null){
                this.addIndividual(OPMWModel,contrib.getString(), Constants.OPM_AGENT,"Agent "+contrib.getString());
                this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,Constants.CONCEPT_AGENT+"/"+contrib.getString(),
                        Constants.PROP_HAS_CONTRIBUTOR);
                
                //prov-o interoperability
                String agEncoded = encode(Constants.CONCEPT_AGENT+"/"+contrib.getString());
                OntClass d = OPMWModel.createClass(Constants.PROV_AGENT);
                d.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+agEncoded);
            }
            if(license!=null){
                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,license.getString(),
                        Constants.DATA_PROP_RIGHTS, XSDDatatype.XSDanyURI);
            }
            if(time!=null){
                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,time.getString(),
                        Constants.DATA_PROP_MODIFIED, XSDDatatype.XSDdateTime);
            }
            if(diagram!=null){
                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,diagram.getURI(),
                        Constants.OPMW_DATA_PROP_HAS_TEMPLATE_DIAGRAM, XSDDatatype.XSDanyURI);
            }
        }
        
        // retrieval of the Components (nodes, with their components and if they are concrete or not)
        String queryNodes = Queries.queryNodes();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryNodes);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource res = qs.getResource("?n");
            Resource comp = qs.getResource("?c");
            Resource typeComp = qs.getResource("?typeComp");
            Literal rule = qs.getLiteral("?rule");
            //Literal isConcrete = qs.getLiteral("?isConcrete");
            System.out.println(res+" Node has component "+comp+" of type: "+ typeComp);//+ " which is concrete: "+isConcrete.getBoolean()
            //add each of the nodes as a UniqueTemplateProcess
            this.addIndividual(OPMWModel,templateName_+res.getLocalName(),Constants.OPMW_WORKFLOW_TEMPLATE_PROCESS, "Workflow template process "+res.getLocalName());
            //p-plan interop
            OntClass cStep = OPMWModel.createClass(Constants.P_PLAN_STEP);
            cStep.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+encode(templateName_+res.getLocalName()));
            
            if(typeComp.isURIResource()){ //only adds the type if the type is a uRI (not a blank node)
                String tempURI = encode(Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+res.getLocalName());
                OntClass cAux = OPMWModel.createClass(typeComp.getURI());//repeated tuples will not be duplicated
                cAux.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+tempURI);
            }else{
                System.out.println("ANON RESOURCE "+typeComp.getURI()+" ignored");
            }
            if(rule!=null){
                //rules are strings
                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+res.getLocalName(),
                    rule.getString(),                    
                        Constants.WINGS_PROP_HAS_RULE);
            }
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+res.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,                    
                        Constants.OPMW_PROP_IS_STEP_OF_TEMPLATE);            
            //p-plan interop
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+res.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,                    
                        Constants.P_PLAN_PROP_IS_STEP_OF_PLAN);
        }
        //retrieval of the dataVariables
        String queryDataV = Queries.queryDataV2();
        r = queryLocalWINGSTemplateModelRepository(queryDataV);
//        ResultSetFormatter.out(r);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource variable = qs.getResource("?d");
            Resource type = qs.getResource("?t");
            Literal dim = qs.getLiteral("?hasDim");            
            this.addIndividual(OPMWModel,templateName_+variable.getLocalName(), Constants.OPMW_DATA_VARIABLE, "Data variable "+variable.getLocalName());
            //p-plan interop
            OntClass cVar = OPMWModel.createClass(Constants.P_PLAN_Variable);
            cVar.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+Constants.CONCEPT_DATA_VARIABLE+"/"+encode(templateName_+variable.getLocalName()));
           
            //we add the individual as a workflowTemplateArtifact as well            
            String aux = encode(Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+variable.getLocalName());
            OntClass cAux = OPMWModel.createClass(Constants.OPMW_WORKFLOW_TEMPLATE_ARTIFACT);//repeated tuples will not be duplicated
            cAux.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+aux);
                   
            if(dim!=null){//sometimes is null, but it shouldn't
                this.addDataProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+variable.getLocalName(),
                        ""+dim.getInt(), Constants.OPMW_DATA_PROP_HAS_DIMENSIONALITY, XSDDatatype.XSDint);
                //System.out.println(res+" has dim: "+dim.getInt());
            }
            //types of data variables
            if(type!=null){
                //sometimes there are some blank nodes asserted as types in the ellaboration.
                //This will remove the blank nodes.
                if(type.isURIResource()){
                    System.out.println(variable+" of type "+ type);
                    //add the individual as an instance of another class, not as a new individual
                    String nameEncoded = encode(Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+variable.getLocalName());
                    OntClass c = OPMWModel.createClass(type.getURI());
                    c.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+nameEncoded);
                }else{
                    System.out.println("ANON RESOURCE "+type.getURI()+" ignored");
                }
            }else{
                System.out.println(variable);
            }
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+variable.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                        Constants.OPMW_PROP_IS_VARIABLE_OF_TEMPLATE);
        }
        //retrieval of the parameterVariables
        String queryParameterV = Queries.querySelectParameter();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryParameterV);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource res = qs.getResource("?p");
//            Literal parValue = qs.getLiteral("?parValue");
            System.out.println(res);
            this.addIndividual(OPMWModel,templateName_+res.getLocalName(), Constants.OPMW_PARAMETER_VARIABLE, "Parameter variable "+res.getLocalName());
            //p-plan interop
            OntClass cVar = OPMWModel.createClass(Constants.P_PLAN_Variable);
            cVar.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+Constants.CONCEPT_PARAMETER_VARIABLE+"/"+encode(templateName_+res.getLocalName()));
           
            //add the parameter value as an artifact too
            String aux = encode(Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+res.getLocalName());
            OntClass cAux = OPMWModel.createClass(Constants.OPMW_WORKFLOW_TEMPLATE_ARTIFACT);//repeated tuples will not be duplicated
            cAux.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+aux);
            
            this.addProperty(OPMWModel,Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+res.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,                    
                        Constants.OPMW_PROP_IS_PARAMETER_OF_TEMPLATE);
        }

        //InputLinks == Used
        String queryInputLinks = Queries.queryInputLinks();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryInputLinks);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource resVar = qs.getResource("?var");
            Resource resNode = qs.getResource("?dest");
            String role = qs.getLiteral("?role").getString();            
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.OPMW_PROP_USES);
            //p-plan interop
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.P_PLAN_PROP_HAS_INPUT);
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                        Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                            Constants.P_PLAN_PROP_IS_INTPUT_VAR_OF);
            if(role!=null){
                System.out.println("Node "+resNode.getLocalName() +" Uses "+ resVar.getLocalName()+ " Role: "+role);
                //add the roles as subproperty of used. This triple should be on the ontology.
                this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.PREFIX_EXTENSION+"usesAs_"+role);
                //link the property as a subproperty of Used
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_USES, Constants.PREFIX_EXTENSION+"usesAs_"+role);
                //description of the new property
                OntProperty propUsed = OPMWModel.getOntProperty(Constants.PREFIX_EXTENSION+"usesAs_"+role);
                propUsed.addLabel("Property that indicates that a resource has been used as a "+role, "EN");
            }
        }
        String queryInputLinksP = Queries.queryInputLinksP();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryInputLinksP);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource resVar = qs.getResource("?var");
            Resource resNode = qs.getResource("?dest");
            String role = qs.getLiteral("?role").getString(); 
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.OPMW_PROP_USES);
            //p-plan interop
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.P_PLAN_PROP_HAS_INPUT);
            this.addProperty(OPMWModel,Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                        Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                            Constants.P_PLAN_PROP_IS_INTPUT_VAR_OF);
            if(role!=null){
                System.out.println("Node "+resNode.getLocalName() +" Uses "+ resVar.getLocalName()+ " Role: "+role);
                //add the roles as subproperty of used. This triple should be on the ontology.
                this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.PREFIX_EXTENSION+"usesAs_"+role);
                //link the property as a subproperty of Used
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_USES, Constants.PREFIX_EXTENSION+"usesAs_"+role);
                OntProperty propUsed = OPMWModel.getOntProperty(Constants.PREFIX_EXTENSION+"usesAs_"+role);
                propUsed.addLabel("Property that indicates that a resource has been used as a "+role, "EN");
//                System.out.println(resVar.getLocalName() +" type "+ qs.getResource("?t").getURI());
            }
        }

        //OutputLInks == WasGeneratedBy
        String queryOutputLinks = Queries.queryOutputLinks();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryOutputLinks);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource resVar = qs.getResource("?var");
            Resource resNode = qs.getResource("?orig");
            String role = qs.getLiteral("?role").getString();             
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.OPMW_PROP_IGB);
            //p-plan interop
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                        Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                            Constants.P_PLAN_PROP_IS_OUTPUT_VAR_OF);
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.P_PLAN_PROP_HAS_OUTPUT);            
            if(role!=null){
                System.out.println("Artifact "+ resVar.getLocalName()+" Is generated by node "+resNode.getLocalName()+" Role "+role);
                //add the roles as subproperty of used. This triple should be on the ontology.
                this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                            Constants.PREFIX_EXTENSION+"isGeneratedByAs_"+role);
                //link the property as a subproperty of WGB
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_IGB, Constants.PREFIX_EXTENSION+"isGeneratedByAs_"+role);
                OntProperty propGenerated = OPMWModel.getOntProperty(Constants.PREFIX_EXTENSION+"isGeneratedByAs_"+role);
                propGenerated.addLabel("Property that indicates that a resource has been generated as a "+role, "EN");
            }
        }
        //InOutLink == Used and WasGeneratedBy
        String queryInOutLinks = Queries.queryInOutLinks();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryInOutLinks);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource resVar = qs.getResource("?var");
            Resource resNode = qs.getResource("?orig");
            String roleOrig = qs.getLiteral("?origRole").getString();
            Resource resNodeD = qs.getResource("?dest");
            String roleDest = qs.getLiteral("?destRole").getString();
            if(roleOrig!=null && roleDest!=null){
                System.out.println("Artifact "+ resVar.getLocalName()+" is generated by node "+resNode.getLocalName()
                        +" with role "+roleOrig+" and uses node "+resNodeD.getLocalName()
                        +" with role "+ roleDest);
            }
            //they are all data variables
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.OPMW_PROP_IGB);
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNodeD.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.OPMW_PROP_USES);
            //p-plan interop
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                        Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                            Constants.P_PLAN_PROP_IS_OUTPUT_VAR_OF);
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.P_PLAN_PROP_HAS_OUTPUT);
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNodeD.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.P_PLAN_PROP_HAS_INPUT);
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                        Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNodeD.getLocalName(),
                            Constants.P_PLAN_PROP_IS_INTPUT_VAR_OF);            
            if(roleOrig!=null){                
                this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                            Constants.PREFIX_EXTENSION+"isGeneratedByAs_"+roleOrig);
                //link the property as a subproperty of WGB
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_IGB, Constants.PREFIX_EXTENSION+"isGeneratedByAs_"+roleOrig);
                OntProperty propGenerated = OPMWModel.getOntProperty(Constants.PREFIX_EXTENSION+"isGeneratedByAs_"+roleOrig);
                propGenerated.addLabel("Property that indicates that a resource has been generated as a "+roleOrig, "EN");
            }
            if(roleDest!=null){
                //System.out.println("created role "+ Constants.PREFIX_ONTOLOGY_PROFILE+"used_"+roleDest.getLocalName());
                this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNodeD.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.PREFIX_EXTENSION+"usesAs_"+roleDest);
                //link the property as a subproperty of Used
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_USES, Constants.PREFIX_EXTENSION+"usesAs_"+roleDest);
                OntProperty propUsed = OPMWModel.getOntProperty(Constants.PREFIX_EXTENSION+"usesAs_"+roleDest);
                propUsed.addLabel("Property that indicates that a resource has been used as a "+roleDest, "EN");
            }
        }
        /******************
         * FILE EXPORT. 
         ******************/        
        exportRDFFile(outFile, OPMWModel);
        return Constants.PREFIX_EXPORT_RESOURCE+""+Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+encode(templateName);
    }

/**
 * Method to transform a Wings execution to OPMW, PROV and P-Plan.
 * Note that this method will load the Workflow Instance and Workflow Expanded Template
 * from the data in the execution file. It is assumed that these URLs are accessible.
 * @param resultFile the Wings Execution File for this execution.
 * @param libraryFile the library file containing all the execution metadata.
 * @param modeFile the serialization of the data (e.g., RDF/XML)
 * @param outFilenameOPMW output name for the OPMW serialization
 * @param outFilenamePROV output name for the PROV serialization
 * @return 
 */
    //public String transformWINGSResultsToOPMW(String resultFile, String libraryFile, String modeFile, String outFilenameOPMW, String outFilenamePROV){
    public String transformWINGSResultsToOPMW(String resultFile, String libraryFile, String modeFile, 
        String outFilenameOPMW, String outFilenamePROV, String suffix){
        //clean previous transformations        
        if(WINGSExecutionResults!=null){
            WINGSExecutionResults.removeAll();//where we store the RDF to query
        }
        WINGSExecutionResults = ModelFactory.createOntologyModel();//ModelFactory.createDefaultModel();
        if(OPMWModel!=null){
            OPMWModel.removeAll(); //where we store the new RDF we create
        }
        OPMWModel = ModelFactory.createOntologyModel(); //inicialization of the model
        if(PROVModel!=null){
            PROVModel.removeAll();
        }
        PROVModel=ModelFactory.createOntologyModel();
        //load the execution library file
        this.loadResultFileToLocalRepository(libraryFile, modeFile);
        //load the execution file
        this.loadResultFileToLocalRepository(resultFile, modeFile);        
        //now, extract the expanded template and the workflow instance. Load them as well
        String queryIntermediateTemplates = Queries.queryIntermediateTemplates();
        //the template is only needed to connect the execution account to itself.
        ResultSet r = queryLocalWINGSResultsRepository(queryIntermediateTemplates);
        String templateName = "", templateURI, expandedTemplateURI;
        if(r.hasNext()){
            QuerySolution qs = r.next();
            Resource template = qs.getResource("?template");
            templateURI = template.getURI();
            templateName = template.getLocalName();
            String wfInstance = qs.getResource("?wfInstance").getURI();
            expandedTemplateURI = qs.getResource("?expandedTemplate").getURI();
//            this.loadResultFileToLocalRepository(template.getURI(), modeFile);
//            System.out.println("Loaded the original template successfully ...");
            this.loadResultFileToLocalRepository(expandedTemplateURI, modeFile);
            System.out.println("Loaded the expanded template successfully ...");
            this.loadResultFileToLocalRepository(wfInstance, modeFile);
            System.out.println("Loaded the workflow instance successfully ...");
        }else{
            System.err.println("The template, expanded template or workflow instance are not available. ");
            return "";
        }
        String date = ""+new Date().getTime();//necessary to add unique nodeId identifiers
        if(suffix == null){
          suffix = date;
        }
        //add the account of the current execution
        //this.addIndividual(OPMWModel,"Account"+date, Constants.OPMW_WORKFLOW_EXECUTION_ACCOUNT,"Execution account created on "+date);
        this.addIndividual(OPMWModel,"Account-"+suffix, Constants.OPMW_WORKFLOW_EXECUTION_ACCOUNT,"Execution account created on "+date);
        //we also assert that it is an account
        //String accname = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date);
        String accname = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account-"+suffix);
        OntClass cAux = OPMWModel.createClass(Constants.OPM_ACCOUNT);
        cAux.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+accname);
        
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/
        OntClass d = PROVModel.createClass(Constants.PROV_BUNDLE);
        d.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+accname);
        
        //relation between the account and the template
        this.addProperty(OPMWModel,accname,
                Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                    Constants.OPMW_PROP_CORRESPONDS_TO_TEMPLATE);
        //p-plan interop
        this.addProperty(PROVModel,accname,
                Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                    Constants.PROV_WAS_DERIVED_FROM);
        
        //account metadata: start time, end time, user, license and status.                
        String queryMetadata = Queries.queryExecutionMetadata();
        String executionFile = null, user = null,
                status = null, startT = null, endT = null, license = null, tool = null;
        
        //we need the template name to reference the nodes in the wf exec.
        /********************************************************/
        /************** EXECUTION ACCOUNT METADATA **************/
        /********************************************************/
        r = queryLocalWINGSResultsRepository(queryMetadata);
//        Resource execDiagram = null; //the newer version doesn't have this info
//        Resource templDiagram = null;
        if(r.hasNext()){
            QuerySolution qs = r.next();
            executionFile = qs.getResource("?exec").getNameSpace();
            status = qs.getLiteral("?status").getString();
            startT = qs.getLiteral("?startT").getString();
            Literal e = qs.getLiteral("?endT");
//            execDiagram = qs.getResource("?execDiagram");
//            templDiagram = qs.getResource("?templDiagram");
            Literal t = qs.getLiteral("?tool");
            Literal u = qs.getLiteral("?user");
            Literal l = qs.getLiteral("?license");
            if(e!=null){
                endT = e.getString();
            }else{
                endT="Not available";
            }
            if(t!=null){
                tool = t.getString();
            }else{
                tool = "http://wings-workflows.org/";//default
            }
            if(u!=null){
                user = u.getString();
            }else{
                //can be extracted from the execution file
                try{
                    user = executionFile;
                    user = user.substring(user.indexOf("users/"), user.length());
                    user = user.split("/",3)[1];
                }catch(Exception ex){
                    user = "unknown";
                }
            }
            if(l!=null){
                license = l.getString();
            }else{
                license = "http://creativecommons.org/licenses/by-sa/3.0/";//default
            }
            //engine = qs.getLiteral("?engine").getString();
            System.out.println("Wings results file:"+executionFile+"\n"
                   // + "User: "+user+", \n"
                    + "Workflow Template: "+templateName+"\n"
                    + "status: "+status+"\n"
                    + "startTime: "+startT+"\n"
                    + "endTime: "+endT);
        }
        
        //metadata about the execution: Agent
        if(user!=null){
            this.addIndividual(OPMWModel,user, Constants.OPM_AGENT, "Agent "+user);//user HAS to have a URI
            this.addProperty(OPMWModel,Constants.CONCEPT_AGENT+"/"+user,
                accname,
                    Constants.OPM_PROP_ACCOUNT);
            
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/
           String agEncoded = encode(Constants.CONCEPT_AGENT+"/"+user);
           OntClass ag = PROVModel.createClass(Constants.PROV_AGENT);
           ag.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+agEncoded);
        }
        
        this.addDataProperty(OPMWModel,accname,
                executionFile,Constants.OPMW_DATA_PROP_HAS_ORIGINAL_LOG_FILE,
                        XSDDatatype.XSDanyURI);
        
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/ 
        //hasOriginalLogFile subprop of hadPrimary Source
        this.addDataProperty(PROVModel,accname,
                executionFile,Constants.PROV_HAD_PRIMARY_SOURCE,
                        XSDDatatype.XSDanyURI);
        
        //status
        this.addDataProperty(OPMWModel,accname,
                status, Constants.OPMW_DATA_PROP_HAS_STATUS);
        //startTime
        this.addDataProperty(OPMWModel,accname,
                startT,Constants.OPMW_DATA_PROP_OVERALL_START_TIME,
                    XSDDatatype.XSDdateTime);
        //endTime
        this.addDataProperty(OPMWModel,accname,
                endT,Constants.OPMW_DATA_PROP_OVERALL_END_TIME,
                    XSDDatatype.XSDdateTime);
        if(license!=null){
            this.addDataProperty(OPMWModel,accname,
                license,Constants.DATA_PROP_RIGHTS,
                    XSDDatatype.XSDanyURI);
        }
        if(tool!=null){
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                tool,Constants.OPMW_DATA_PROP_CREATED_IN_WORKFLOW_SYSTEM,
                    XSDDatatype.XSDanyURI);
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            //the template is a prov:Plan
            OntClass plan = PROVModel.createClass(Constants.PROV_PLAN);
            plan.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+encode(templateName));
            //createdIn wf system subprop of wasAttributedTo
            this.addDataProperty(PROVModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                tool,Constants.PROV_WAS_ATTRIBUTED_TO,
                    XSDDatatype.XSDanyURI);
            //the run wasInfluencedBy the template
            this.addProperty(PROVModel,accname,
                Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                    Constants.PROV_WAS_INFLUENCED_BY);
        }
        
        /********************************************************/
        /********************* NODE LINKING**********************/
        /********************************************************/
        //query for detecting steps, their inputs and their outputs
        String queryStepsAndIO = Queries.queryStepsAndMetadata();
        r = queryLocalWINGSResultsRepository(queryStepsAndIO);
        String stepName, sStartT = null, sEndT = null, sStatus, sCode, derivedFrom = null;        
        while (r.hasNext()){
            QuerySolution qs = r.next();
            
            //start time and end time could be optional.
            stepName = qs.getResource("?step").getLocalName();
            Literal stLiteral = qs.getLiteral("?startT");
            if (stLiteral!=null){
                sStartT = stLiteral.getString();
            }
            Literal seLiteral = qs.getLiteral("?endT");
            if(seLiteral!=null){
                sEndT = seLiteral.getString();
            }
            sStatus = qs.getLiteral("?status").getString();
            sCode = qs.getLiteral("?code").getString();
            try{
                derivedFrom = qs.getResource("?derivedFrom").getLocalName();
            }catch(Exception e){
                //if we don't have the derivedFrom relationship, we assume that
                //the node name on the template is the same as in the exp template
                derivedFrom = stepName;
            }
//            System.out.println(step +"\n\t "+ sStartT+"\n\t "+sEndT+"\n\t "+sStatus+"\n\t "+sCode);
            //add each step with its metadata to the model Start and end time are reused from prov.
            this.addIndividual(OPMWModel,stepName+date, Constants.OPMW_WORKFLOW_EXECUTION_PROCESS, "Execution process "+stepName);
            //add type opmv:Process as well
            String auxP = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date);
            OntClass cP = OPMWModel.createClass(Constants.OPM_PROCESS);
            cP.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxP);
            
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date,
                Constants.CONCEPT_AGENT+"/"+user,
                    Constants.OPM_PROP_WCB);
            
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date,
                accname,
                    Constants.OPM_PROP_ACCOUNT);
            
            /*************************
             * PROV-O INTEROPERABILITY
             *************************/
            OntClass d1 = PROVModel.createClass(Constants.PROV_ACTIVITY);
            d1.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxP);
            
            this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date,
                Constants.CONCEPT_AGENT+"/"+user,
                    Constants.PROV_WAS_ASSOCIATED_WITH);

            //metadata
            if(sStartT!=null){
                this.addDataProperty(PROVModel, 
                        Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date, 
                        sStartT, 
                        Constants.PROV_STARTED_AT_TIME,
                        XSDDatatype.XSDdateTime);
            }
            if(sEndT!=null){
                this.addDataProperty(PROVModel, 
                        Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date, 
                        sEndT, 
                        Constants.PROV_ENDED_AT_TIME,
                        XSDDatatype.XSDdateTime);
            }
            this.addDataProperty(OPMWModel, 
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date, 
                    sStatus, 
                    Constants.OPMW_DATA_PROP_HAS_STATUS);
            
            //add the code binding as an executable component            
            Resource blankNode = OPMWModel.createResource();
            blankNode.addProperty(OPMWModel.createOntProperty(Constants.OPMW_DATA_PROP_HAS_LOCATION),
                    sCode).
                    addProperty(OPMWModel.createOntProperty(Constants.RDFS_LABEL), 
                            "Executable Component associated to "+stepName);
            String procURI = Constants.PREFIX_EXPORT_RESOURCE+ encode(Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date);
            OPMWModel.getResource(procURI).
                    addProperty(OPMWModel.createOntProperty(Constants.OPMW_PROP_HAS_EXECUTABLE_COMPONENT), 
                            blankNode);
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            Resource bnodeProv = PROVModel.createResource();
            bnodeProv.addProperty(PROVModel.createOntProperty(Constants.PROV_AT_LOCATION),
                    sCode).
                    addProperty(PROVModel.createOntProperty(Constants.RDFS_LABEL), 
                            "Executable Component associated to "+stepName);
            PROVModel.getResource(procURI).
                    addProperty(PROVModel.createOntProperty(Constants.OPM_PROP_USED), 
                            bnodeProv);
            
            //link node  to the process templates
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date,
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName+"_"+derivedFrom,
                        Constants.OPMW_PROP_CORRESPONDS_TO_TEMPLATE_PROCESS);
            //p-plan interop
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+stepName+date,
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName+"_"+derivedFrom,
                        Constants.P_PLAN_PROP_CORRESPONDS_TO_STEP);
        }
        //annotation of inputs
        String getInputs = Queries.queryStepInputs();
        r = queryLocalWINGSResultsRepository(getInputs);
        String step, input, inputBinding;
        while(r.hasNext()){
            QuerySolution qs = r.next();
            step = qs.getResource("?step").getLocalName();
            input = qs.getResource("?input").getLocalName();
            inputBinding = qs.getLiteral("?iBinding").getString();
            System.out.println("Step: "+step+" used input "+input+" with data binding: "+inputBinding);            
            //no need to add the variable individual now because the types are going to be added later
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+step+date,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+input+date,
                        Constants.OPM_PROP_USED);
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+input+date,
                    inputBinding,
                        Constants.OPMW_DATA_PROP_HAS_LOCATION, XSDDatatype.XSDanyURI);
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+step+date,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+input+date,
                        Constants.PROV_USED);
            //hasLocation subrpop of atLocation
            this.addDataProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+input+date,
                    inputBinding,
                        Constants.PROV_AT_LOCATION, XSDDatatype.XSDanyURI);
            
        }
        //parameters are separated (in expanded template). 
        String getParams = Queries.querySelectStepParameterValues();
        r = queryLocalWINGSResultsRepository(getParams);
        String paramName, paramvalue, derived = null;
        while(r.hasNext()){
            QuerySolution qs = r.next();
            step = qs.getResource("?step").getLocalName();
            paramName = qs.getResource("?param").getLocalName();
            paramvalue = qs.getLiteral("?value").getString();
            Resource res = qs.getResource("?derivedFrom");
            if(res!=null){
                derived = res.getLocalName();
            }
            System.out.println("step "+step +"used param: "+paramName+" with value: "+paramvalue);
            this.addIndividual(OPMWModel, paramName+date,
                    Constants.OPMW_WORKFLOW_EXECUTION_ARTIFACT, "Parameter with value: "+paramvalue);
            String auxParam = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date);
            OntClass cParam = OPMWModel.createClass(Constants.OPM_ARTIFACT);
            cParam.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxParam);
            this.addDataProperty(OPMWModel, 
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date, 
                    paramvalue, 
                    Constants.OPMW_DATA_PROP_HAS_VALUE);
            this.addProperty(OPMWModel, 
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+step+date, 
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date, 
                    Constants.OPM_PROP_USED);
            //link to template
            if(res!=null){
                this.addProperty(OPMWModel,
                        Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date,
                        Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName+"_"+derived,
                        Constants.OPMW_PROP_CORRESPONDS_TO_TEMPLATE_ARTIFACT);
                this.addProperty(OPMWModel,
                        Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date,
                        Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName+"_"+derived,
                        Constants.P_PLAN_PROP_CORRESPONDS_TO_VAR);
            }
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date,
                accname,
                    Constants.OPM_PROP_ACCOUNT);
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            String auxP = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date);
            OntClass cP = PROVModel.createClass(Constants.PROV_ENTITY);
            cP.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxP);
            this.addDataProperty(PROVModel, 
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date, 
                    paramvalue,
                    Constants.PROV_VALUE);            
            this.addProperty(PROVModel, 
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+step+date, 
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+paramName+date, 
                    Constants.PROV_USED);            
        }
        //annotation of outputs
        String getOutputs = Queries.queryStepOutputs();
        r = queryLocalWINGSResultsRepository(getOutputs);
        String output, outputBinding;
        while(r.hasNext()){
            QuerySolution qs = r.next();
            step = qs.getResource("?step").getLocalName();
            output = qs.getResource("?output").getLocalName();
            outputBinding = qs.getLiteral("?oBinding").getString();
            System.out.println("Step: "+step+" has output "+output+" with data binding: "+outputBinding);
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+output+date,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+step+date,
                        Constants.OPM_PROP_WGB);
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+output+date,
                    outputBinding,
                        Constants.OPMW_DATA_PROP_HAS_LOCATION, XSDDatatype.XSDanyURI);
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+output+date,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+step+date,
                        Constants.PROV_WGB);
            //hasLocation subrpop of atLocation
            this.addDataProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+output+date,
                    outputBinding,
                        Constants.PROV_AT_LOCATION, XSDDatatype.XSDanyURI);
        }
        //annotation of variable metadata
        String getVarMetadata = Queries.queryDataVariablesMetadata();
        r = queryLocalWINGSResultsRepository(getVarMetadata);
        String var, prop, obj, objName = null;
        while(r.hasNext()){
            QuerySolution qs = r.next();
            var = qs.getResource("?variable").getLocalName();
            prop = qs.getResource("?prop").getURI();
            try{
                //types
                Resource rObj = qs.getResource("?obj");
                obj = rObj.getURI();
                objName = rObj.getLocalName();
            }catch(Exception e){
                //basic metadata
                obj = qs.getLiteral("?obj").getString();
            }
//            System.out.println("Var "+var+" <"+prop+ "> "+ obj);
            this.addIndividual(OPMWModel, var+date,
                    Constants.OPMW_WORKFLOW_EXECUTION_ARTIFACT, 
                    "Workflow execution artifact: "+var+date);
            //redundancy: add it as a opm:Artifact as well
            String auxP = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+var+date);
            OntClass cP = OPMWModel.createClass(Constants.OPM_ARTIFACT);
            cP.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxP);
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+var+date,
                accname,
                    Constants.OPM_PROP_ACCOUNT);
            //link to template
            if(prop.contains("derivedFrom")){
                //this relationship ensures that we are doing the linking correctly.
                //if it doesn't exist we avoid linking to the template.
                this.addProperty(OPMWModel,
                        Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+var+date,
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName+"_"+objName,
                        Constants.OPMW_PROP_CORRESPONDS_TO_TEMPLATE_ARTIFACT);
                //p-plan interop
                this.addProperty(OPMWModel,
                        Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+var+date,
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName+"_"+objName,
                        Constants.P_PLAN_PROP_CORRESPONDS_TO_VAR);
            }else
            //metadata
            if(prop.contains("type")){
                //the objects are resources in this case
                //String auxP = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+var+date);
                cP = OPMWModel.createClass(obj);
                cP.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxP);
            }
            else if(prop.contains("hasSize")){
                this.addDataProperty(OPMWModel,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+var+date,
                    obj,
                    Constants.OPMW_DATA_PROP_HAS_SIZE);
            }else if(prop.contains("hasDataBinding")||prop.contains("isVariableOfPlan")){
                //do nothing! we have already dealt with data binding before
                //regarding the p-plan, i don't add it to avoid confusion
            }else{
                //custom wings property: preserve it.
                this.addDataProperty(OPMWModel,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+var+date,
                    obj,
                    prop);
            }
            
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            
            cP = PROVModel.createClass(Constants.PROV_ENTITY);
            cP.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxP);
        }

        /***********************************************************************************
         * FILE EXPORT 
         ***********************************************************************************/        
        exportRDFFile(outFilenameOPMW, OPMWModel);
        exportRDFFile(outFilenamePROV, PROVModel);
        return (Constants.PREFIX_EXPORT_RESOURCE+accname);
    }
    
    public String getRunUrl(String suffix) {
        String accname = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account-"+suffix);
        return (Constants.PREFIX_EXPORT_RESOURCE+accname);
    }
    
    public String getTemplateUrl(String templateName) {
        return Constants.PREFIX_EXPORT_RESOURCE+""+Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+
            encode(templateName);
    }
    
    public void setPublishExportPrefix(String prefix) {
      Constants.PREFIX_EXPORT_RESOURCE = prefix;
    }

    /**
     * Function to export the stored model as an RDF file, using ttl syntax
     * @param outFile name and path of the outFile must be created.
     */
    private void exportRDFFile(String outFile, OntModel model){
        OutputStream out;
        try {
            out = new FileOutputStream(outFile);
//            model.write(out,"TURTLE");
            model.write(out,"RDF/XML");
            out.close();
        } catch (Exception ex) {
            System.out.println("Error while writing the model to file "+ex.getMessage());
        }
    }
    /**
     * FUNCTIONS TO ADD RELATIONSHIPS TO THE MODEL
     */

    /**
     * Funtion to insert an individual as an instance of a class. If the class does not exist, it is created.
     * @param individualId Instance id. If exists it won't be created.
     * @param classURL URL of the class from which we want to create the instance
     */
    private void addIndividual(OntModel m,String individualId, String classURL, String label){
        String nameOfIndividualEnc = encode(getClassName(classURL)+"/"+individualId);
        OntClass c = m.createClass(classURL);
        c.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+nameOfIndividualEnc);
        if(label!=null){
            this.addDataProperty(m,nameOfIndividualEnc,label,Constants.RDFS_LABEL);
        }
    }

    /**
     * Funtion to add a property between two individuals. If the property does not exist, it is created.
     * @param orig Domain of the property (Id, not complete URI)
     * @param dest Range of the property (Id, not complete URI)
     * @param property URI of the property
     */
    private void addProperty(OntModel m, String orig, String dest, String property){
        OntProperty propSelec = m.createOntProperty(property);
        Resource source = m.getResource(Constants.PREFIX_EXPORT_RESOURCE+ encode(orig) );
        Individual instance = (Individual) source.as( Individual.class );
        if(dest.contains("http://")){//it is a URI
            instance.addProperty(propSelec,dest);            
        }else{//it is a local resource
            instance.addProperty(propSelec, m.getResource(Constants.PREFIX_EXPORT_RESOURCE+encode(dest)));
        }
        //System.out.println("Creada propiedad "+ propiedad+" que relaciona los individuos "+ origen + " y "+ destino);
    }

    /**
     * Function to add dataProperties. Similar to addProperty
     * @param origen Domain of the property (Id, not complete URI)
     * @param literal literal to be asserted
     * @param dataProperty URI of the data property to assign.
     */
    private void addDataProperty(OntModel m, String origen, String literal, String dataProperty){
        OntProperty propSelec;
        //lat y long son de otra ontologia, tienen otro prefijo distinto
        propSelec = m.createDatatypeProperty(dataProperty);
        //propSelec = (modeloOntologia.getResource(dataProperty)).as(OntProperty.class);
        Resource orig = m.getResource(Constants.PREFIX_EXPORT_RESOURCE+ encode(origen) );
        m.add(orig, propSelec, literal); 
    }

    /**
     * Function to add dataProperties. Similar to addProperty
     * @param m Model of the propery to be added
     * @param origen Domain of the property
     * @param dato literal to be asserted
     * @param dataProperty URI of the dataproperty to assert
     * @param tipo type of the literal (String, int, double, etc.).
     */
    private void addDataProperty(OntModel m, String origen, String dato, String dataProperty,RDFDatatype tipo) {
        OntProperty propSelec;
        //lat y long son de otra ontologia, tienen otro prefijo distinto
        propSelec = m.createDatatypeProperty(dataProperty);
        Resource orig = m.getResource(Constants.PREFIX_EXPORT_RESOURCE+ encode(origen));
        m.add(orig, propSelec, dato,tipo);
    }

    /**
     * Function to add a property as a subproperty of the other.
     * @param uriProp
     * @param uriSubProp
     */
    private void createSubProperty(OntModel m, String uriProp, String uriSubProp){
        if(uriProp.equals(uriSubProp))return;
        OntProperty propUsed = m.getOntProperty(uriProp);
        OntProperty propRole = m.getOntProperty(uriSubProp);
        propUsed.addSubProperty(propRole);
    }

    /**
     * Encoding of the name to avoid any trouble with spacial characters and spaces
     * @param name
     */
    private String encode(String name){
        name = name.replace("http://","");
        String prenom = name.substring(0, name.indexOf("/")+1);
        //remove tabs and new lines
        String nom = name.replace(prenom, "");
        if(name.length()>255){
            try {
                nom = MD5.MD5(name);
            } catch (Exception ex) {
                System.err.println("Error when encoding in MD5: "+ex.getMessage() );
            }
        }        

        nom = nom.replace("\\n", "");
        nom = nom.replace("\n", "");
        nom = nom.replace("\b", "");
        //quitamos "/" de las posibles urls
        nom = nom.replace("/","_");
        nom = nom.replace("=","_");
        nom = nom.trim();
        //espacios no porque ya se urlencodean
        //nom = nom.replace(" ","_");
        //a to uppercase
        nom = nom.toUpperCase();
        try {
            //urlencodeamos para evitar problemas de espacios y acentos
            nom = new URI(null,nom,null).toASCIIString();//URLEncoder.encode(nom, "UTF-8");
        }
        catch (Exception ex) {
            try {
                System.err.println("Problem encoding the URI:" + nom + " " + ex.getMessage() +". We encode it in MD5");
                nom = MD5.MD5(name);
                System.err.println("MD5 encoding: "+nom);
            } catch (Exception ex1) {
                System.err.println("Could not encode in MD5:" + name + " " + ex1.getMessage());
            }
        }
        return prenom+nom;
    }

    /**
     * Method to retrieve the name of a URI class objet
     * @param classAndVoc URI from which retrieve the name
     * @return 
     */
    private String getClassName(String classAndVoc){
        if(classAndVoc.contains(Constants.PREFIX_DCTERMS))return classAndVoc.replace(Constants.PREFIX_DCTERMS,"");
        else if(classAndVoc.contains(Constants.PREFIX_FOAF))return classAndVoc.replace(Constants.PREFIX_FOAF,"");
        else if(classAndVoc.contains(Constants.PREFIX_OPMO))return classAndVoc.replace(Constants.PREFIX_OPMO,"");
        else if(classAndVoc.contains(Constants.PREFIX_OPMV))return classAndVoc.replace(Constants.PREFIX_OPMV,"");
        else if(classAndVoc.contains(Constants.PREFIX_RDFS))return classAndVoc.replace(Constants.PREFIX_RDFS,"");
        else if(classAndVoc.contains(Constants.PREFIX_OPMW))return classAndVoc.replace(Constants.PREFIX_OPMW,"");
//        else if(classAndVoc.contains(ACDOM))return classAndVoc.replace(ACDOM,"");
//        else if(classAndVoc.contains(DCDOM))return classAndVoc.replace(DCDOM,"");
        else return null;
    }
    
    /**
     * Function to determine whether a run has already been published or not.
     * If the run has been published, it should not be republished again.
     * @param endpointURL URL of the repository where we store the runs
     * @param runURL URL of the physical file of containing the run.
     * @return True if the run has been published. False in other case.
     */
    public boolean isRunPublished(String endpointURL, String runURL){
        String query = Queries.queryIsTheRunAlreadyPublished(runURL);        
        QueryExecution qe = QueryExecutionFactory.sparqlService(endpointURL, query);
        ResultSet rs = qe.execSelect();
        return rs.hasNext();        
    }

    
}
