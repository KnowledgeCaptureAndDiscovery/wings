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

package edu.isi.ikcap.wings.opmm;

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
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
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
    private String ACDOM = null;
    private String DCDOM = null;
    private int count = 0;//count for avoiding the same process identiefier in n-dimensional lists
    //this 2 variables cannot be on the constants because they change on every domain
    public Mapper(){

    }

    private ResultSet queryLocalRepository(String queryIn, OntModel repository){
        Query query = QueryFactory.create(queryIn);
        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, repository);
        ResultSet rs =  qe.execSelect();
        //qe.close();
        return rs;
    }

    public ResultSet queryLocalOPMRepository(String queryIn) {
        return queryLocalRepository(queryIn, OPMWModel);
    }
    public ResultSet queryLocalWINGSTemplateModelRepository(String queryIn) {
        return queryLocalRepository(queryIn, WINGSModelTemplate);
    }
    public ResultSet queryLocalWINGSResultsRepository(String queryIn) {
        return queryLocalRepository(queryIn, WINGSExecutionResults);
    }

    /**
     * Loads the files to the Local repository, to prepare conversion to OPM
     * @param template. Workflow template
     * @param executionResults. Results of the execution of with the workflow template
     * @param modeFile1. syntax of the files to load: "RDF/XML", "N-TRIPLE", "TURTLE" (or "TTL") and "N3"
     * @param modeFile2. syntax of the files to load: "RDF/XML", "N-TRIPLE", "TURTLE" (or "TTL") and "N3"
     */
    public void loadTemplateFileToLocalRepository(String template, String modeFile){
        WINGSModelTemplate = ModelFactory.createOntologyModel();//ModelFactory.createDefaultModel();
        InputStream in = FileManager.get().open(template);
        if (in == null) {
            throw new IllegalArgumentException("File: " + template + " not found");
        }
        // read the RDF/XML file
        WINGSModelTemplate.read(in, null, modeFile);
        System.out.println("File "+template+" loaded into the model template");
        getACDCfromModel(true);
    }

    public void loadResultFileToLocalRepository(String executionResults, String mode){
        InputStream in2 = FileManager.get().open(executionResults);
        if (in2 == null){
            throw new IllegalArgumentException("File: " + executionResults + " not found");
        }
        WINGSExecutionResults = ModelFactory.createOntologyModel();//ModelFactory.createDefaultModel();
        WINGSExecutionResults.read(in2, null, mode);
        System.out.println("File "+executionResults+" loaded into the execution results");
        getACDCfromModel(false);
    }

    /**
     * Constants ac catalog and dc catalog depend on the domain. They have to be loaded from the file
     */
    public void getACDCfromModel(boolean template){
        if(template){
            //query the template
            this.ACDOM = WINGSModelTemplate.getNsPrefixURI("acdom");
            this.DCDOM = WINGSModelTemplate.getNsPrefixURI("dcdom");

        }else{//results
            this.ACDOM = WINGSExecutionResults.getNsPrefixURI("acdom");
            this.DCDOM = WINGSExecutionResults.getNsPrefixURI("dcdom");
        }
        System.out.println("ACDOM: "+ACDOM);
        System.out.println("DCDOM: "+DCDOM);
    }

    //THE PROV MODEL IS NOT NECESSARY HERE BECAUSE THE BUNDLE CONTAINS ONLY THE EXECUTION (PROV)
    public void transformWINGSElaboratedTemplateToOPM(String template,String mode, String outFile){
        //clean previous transformations
        if(WINGSModelTemplate!=null){
            WINGSModelTemplate.removeAll();
        }
        if(OPMWModel!=null){
            OPMWModel.removeAll();            
        }
        OPMWModel = ModelFactory.createOntologyModel(); //inicialization of the model        
        //load the template file to WINGSModel
        this.loadTemplateFileToLocalRepository(template, mode);        
        //query the model and transform
        //retrieval of the name of the workflowTemplate
        String queryNameWfTemplate = Queries.queryNameWfTemplate();
        String templateName = null, templateName_ = null;
        //System.out.println(queryNameWfTemplate);
        ResultSet r = queryLocalWINGSTemplateModelRepository(queryNameWfTemplate);
        if(r.hasNext()){//there should be just one local name per template
            QuerySolution qs = r.next();
            Resource res = qs.getResource("?name");
            Literal v = qs.getLiteral("?ver");
            templateName = res.getLocalName();
            if (templateName==null){
                System.out.println("Error: No Template specified.");
                return;
            }
            templateName_=templateName+"_";
            //add the template as a provenance graph
            this.addIndividual(OPMWModel,templateName, Constants.OPMW_WORKFLOW_TEMPLATE, templateName);
            if(v!=null){
                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,""+ v.getInt(),
                        Constants.OPMW_DATA_PROP_VERSION_NUMBER, XSDDatatype.XSDint);
            }
            //Prov-o interoperability : workflow template           
            OntClass plan = OPMWModel.createClass(Constants.PROV_PLAN);
            plan.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+encode(templateName));
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
            //Literal isConcrete = qs.getLiteral("?isConcrete");
            System.out.println(res+" Template has component "+comp+" of type: "+ typeComp);//+ " which is concrete: "+isConcrete.getBoolean()
            //add each of the nodes as a UniqueTemplateProcess
            this.addIndividual(OPMWModel,templateName_+res.getLocalName(),Constants.OPMW_WORKFLOW_TEMPLATE_PROCESS, "Workflow template process "+res.getLocalName());
                     
            if(typeComp.isURIResource()){ //only adds the type if the type is a uRI (not a blank node)
                String tempURI = encode(Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+res.getLocalName());
                OntClass cAux = OPMWModel.createClass(typeComp.getURI());//repeated tuples will not be duplicated
                cAux.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+tempURI);
            }else{
                System.out.println("ANON RESOURCE "+typeComp.getURI()+" ignored");
            }
            
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+res.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,                    
                        Constants.OPMW_PROP_IS_STEP_OF_TEMPLATE);            
        }
        //retrieval of the dataVariables
        String queryDataV = Queries.queryDataV();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryDataV);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource res = qs.getResource("?d");
            Resource opt = qs.getResource("?t");
            Literal dim = qs.getLiteral("?hasDim");            
            this.addIndividual(OPMWModel,templateName_+res.getLocalName(), Constants.OPMW_DATA_VARIABLE, "Data variable "+res.getLocalName());
            //we add the individual as a workflowTemplateArtifact as well            
            String aux = encode(Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+res.getLocalName());
            OntClass cAux = OPMWModel.createClass(Constants.OPMW_WORKFLOW_TEMPLATE_ARTIFACT);//repeated tuples will not be duplicated
            cAux.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+aux);
                   
            if(dim!=null){//sometimes is null, but it shouldn't
                this.addDataProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+res.getLocalName(),
                        ""+dim.getInt(), Constants.OPMW_DATA_PROP_HAS_DIMENSIONALITY, XSDDatatype.XSDint);
                System.out.println(res+" has dim: "+dim.getInt());
            }
            if(opt!=null){
                //sometimes there are some blank nodes asserted as types in the ellaboration.
                //This will remove the blank nodes.
                if(opt.isURIResource()){
                    System.out.println(res+" of type "+ opt);
                    //add the individual as an instance of another class, not as a new individual
                    String nombreIndividuoEnc = encode(Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+res.getLocalName());
                    OntClass c = OPMWModel.createClass(opt.getURI());
                    c.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+nombreIndividuoEnc);
                }else{
                    System.out.println("ANON RESOURCE "+opt.getURI()+" ignored");
                }
            }else{
                System.out.println(res);
            }
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+res.getLocalName(),
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
            //add the parameter value as an artifact too
            String aux = encode(Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+res.getLocalName());
            OntClass cAux = OPMWModel.createClass(Constants.OPMW_WORKFLOW_TEMPLATE_ARTIFACT);//repeated tuples will not be duplicated
            cAux.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+aux);
            //this has been commented to avoid asserting parameter values in templates. Not necessary
//            if(parValue!=null){
//                this.addDataProperty(OPMWModel,Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+res.getLocalName(),
//                    parValue.getLexicalForm(), Constants.OPMW_DATA_PROP_HAS_DIMENSIONALITY);
//            }
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
            Resource role = qs.getResource("?role");            
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.OPMW_PROP_USES);
            if(role!=null){
                System.out.println("Node "+resNode.getLocalName() +" Uses "+ resVar.getLocalName()+ " Role: "+role.getLocalName());
                //add the roles as subproperty of used. This triple should be on the ontology.
                this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.PREFIX_OPMW+"usesAs_"+role.getLocalName());
                //link the property as a subproperty of Used
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_USES, Constants.PREFIX_OPMW+"usesAs_"+role.getLocalName());
            }
        }
        String queryInputLinksP = Queries.queryInputLinksP();
        r = null;
        r = queryLocalWINGSTemplateModelRepository(queryInputLinksP);
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource resVar = qs.getResource("?var");
            Resource resNode = qs.getResource("?dest");
            Resource role = qs.getResource("?role");
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.OPMW_PROP_USES);
            if(role!=null){
                System.out.println("Node "+resNode.getLocalName() +" Uses "+ resVar.getLocalName()+ " Role: "+role.getLocalName());
                //add the roles as subproperty of used. This triple should be on the ontology.
                this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.PREFIX_OPMW+"usesAs_"+role.getLocalName());
                //link the property as a subproperty of Used
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_USES, Constants.PREFIX_OPMW+"usesAs_"+role.getLocalName());
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
            Resource role = qs.getResource("?role");            
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.OPMW_PROP_IGB);
            if(role!=null){
                System.out.println("Artifact "+ resVar.getLocalName()+" Is generated by node "+resNode.getLocalName()+" Role "+role.getLocalName());
                //add the roles as subproperty of used. This triple should be on the ontology.
                this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                            Constants.PREFIX_OPMW+"isGeneratedByAs_"+role.getLocalName());
                //link the property as a subproperty of WGB
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_IGB, Constants.PREFIX_OPMW+"isGeneratedByAs_"+role.getLocalName());
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
            Resource roleOrig = qs.getResource("?origRole");
            Resource resNodeD = qs.getResource("?dest");
            Resource roleDest = qs.getResource("?destRole");
            if(roleOrig!=null && roleDest!=null){
                System.out.println("Artifact "+ resVar.getLocalName()+" is generated by node "+resNode.getLocalName()
                        +" with role "+roleOrig.getLocalName()+" and uses node "+resNodeD.getLocalName()
                        +" with role "+ roleDest.getLocalName());
            }
            //they are all data variables
            this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                        Constants.OPMW_PROP_IGB);
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNodeD.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.OPMW_PROP_USES);
            if(roleOrig!=null){                
                this.addProperty(OPMWModel,Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                    Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNode.getLocalName(),
                            Constants.PREFIX_OPMW+"isGeneratedByAs_"+roleOrig.getLocalName());
                //link the property as a subproperty of WGB
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_IGB, Constants.PREFIX_OPMW+"isGeneratedByAs_"+roleOrig.getLocalName());
            }
            if(roleDest!=null){
                //System.out.println("created role "+ Constants.PREFIX_ONTOLOGY_PROFILE+"used_"+roleDest.getLocalName());
                this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName_+resNodeD.getLocalName(),
                        Constants.CONCEPT_DATA_VARIABLE+"/"+templateName_+resVar.getLocalName(),
                            Constants.PREFIX_OPMW+"usesAs_"+roleDest.getLocalName());
                //link the property as a subproperty of Used
                this.createSubProperty(OPMWModel,Constants.OPMW_PROP_USES, Constants.PREFIX_OPMW+"usesAs_"+roleDest.getLocalName());
            }
        }
        /***********************************************************************************
         * FILE EXPORT. 
         ***********************************************************************************/        
        exportRDFFile(outFile, OPMWModel);
    }

    
    public String transformWINGSResultsToOPM(String results,String modeFile, String outFilenameOPMW, String outFilenamePROV){
        //this first operations should be on a "initialize" method
        //clean previous transformations        
        if(WINGSExecutionResults!=null){
            WINGSExecutionResults.removeAll();//where we store the RDF to query
        }
        if(OPMWModel!=null){
            OPMWModel.removeAll(); //where we store the new RDF we create
        }
        OPMWModel = ModelFactory.createOntologyModel(); //inicialization of the model
        if(PROVModel!=null){
            PROVModel.removeAll();
        }
        PROVModel=ModelFactory.createOntologyModel();
        //load the files to WINGSModel
        //this.loadTemplateFileToLocalRepository(template, modeFile2);
        this.loadResultFileToLocalRepository(results, modeFile);        
        String queryNameWfTemplate = Queries.queryNameWfTemplateAndMetadata();
        String wingsResultsFile = null, templateName = null, user = null,
                wingsURLname = null, status = null, startT = null, endT = null, license = null, tool = null;
        //we need the template name to reference the nodes in the wf exec.
        /********************************************************/
        /************** EXECUTION ACCOUNT METADATA **************/
        /********************************************************/
        ResultSet r = queryLocalWINGSResultsRepository(queryNameWfTemplate);
        Resource execDiagram = null;
        Resource templDiagram = null;
        if(r.hasNext()){
            QuerySolution qs = r.next();
            wingsResultsFile = qs.getResource("?fileURI").getURI();
            Resource res = qs.getResource("?name");
            templateName = res.getLocalName();
            user = qs.getLiteral("?user").getString();
            wingsURLname = qs.getResource("?wTempl").getURI();
            status = qs.getLiteral("?status").getString();
            startT = qs.getLiteral("?startT").getString();
            endT = qs.getLiteral("?endT").getString();
            execDiagram = qs.getResource("?execDiagram");
            templDiagram = qs.getResource("?templDiagram");
            tool = qs.getLiteral("?tool").getString();
            //engine = qs.getLiteral("?engine").getString();
            license = qs.getLiteral("?license").getString();
            
            System.out.println("Wings results file:"+wingsResultsFile+"\n"
                    + "User: "+user+", \n"
                    + "Workflow Template: "+templateName+"\n"
                    + "Wings Workflow template: "+wingsURLname+"\n"
                    + "status: "+status+"\n"
                    + "startTime: "+startT+"\n"
                    + "endTime: "+endT);
        }
        
        if(templateName == null){
            System.out.println("Workflow Template not existant");
            return null;
        }
        //query for the tools used in the execution (there can be more than one)
        String queryToolsUsed = Queries.queryUsedTools();        
        //we need the template name to reference the nodes in the wf exec.        
        r = queryLocalWINGSResultsRepository(queryToolsUsed);
        ArrayList<ArrayList<String>> tools = new ArrayList<ArrayList<String>>();
        ArrayList<String> currTool;
        while(r.hasNext()){
            QuerySolution qs = r.next();
            String toolID = qs.getLiteral("toolID").getString();
            String toolURL = qs.getResource("url").getURI();
            String toolVersion = qs.getLiteral("version").getString();
            currTool = new ArrayList<String>();
            currTool.add(toolID);
            currTool.add(toolURL);
            currTool.add(toolVersion);
            tools.add(currTool);
            //System.out.println("TOOL USED: "+toolID+ " URL :"+toolURL+ " VERSION: "+toolVersion);
        }
        
        String date = ""+new Date().getTime();//necessary to add unique nodeId identifiers
        
        //add the account of the current execution
        this.addIndividual(OPMWModel,"Account"+date, Constants.OPMW_WORKFLOW_EXECUTION_ACCOUNT,"Execution account created on "+date);
        //we also assert that it is an account
        String accname = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date);
        OntClass cAux = OPMWModel.createClass(Constants.OPM_ACCOUNT);
        cAux.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+accname);
        
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/
        OntClass d = PROVModel.createClass(Constants.PROV_BUNDLE);
        d.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+accname);
        
        //relation between the account and the template
        this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                    Constants.OPMW_PROP_CORRESPONDS_TO_TEMPLATE);
        //metadata about the execution: Agent
        this.addIndividual(OPMWModel,user, Constants.OPM_AGENT, "Agent "+user);//user HAS to have a URI
        this.addProperty(OPMWModel,Constants.CONCEPT_AGENT+"/"+user,
                Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                    Constants.OPM_PROP_ACCOUNT);
        
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/
        String agEncoded = encode(Constants.CONCEPT_AGENT+"/"+user);
        OntClass ag = PROVModel.createClass(Constants.PROV_AGENT);
        ag.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+agEncoded);
        
        //link to the wings template and results file
        this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                wingsURLname,Constants.OPMW_DATA_PROP_HAS_NATIVE_SYSTEM_TEMPLATE,
                        XSDDatatype.XSDanyURI);
        
        //prov-o interoperability: hasNativeSysTempl subprop of hadPrimary Source
        this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                wingsURLname,Constants.PROV_HAD_PRIMARY_SOURCE,
                        XSDDatatype.XSDanyURI);
        
        
        this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                wingsResultsFile,Constants.OPMW_DATA_PROP_HAS_ORIGINAL_LOG_FILE,
                        XSDDatatype.XSDanyURI);
        
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/ 
        //hasOriginalLogFile subprop of hadPrimary Source
        this.addDataProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                wingsResultsFile,Constants.PROV_HAD_PRIMARY_SOURCE,
                        XSDDatatype.XSDanyURI);
        
        //status
        this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                status, Constants.OPMW_DATA_PROP_HAS_STATUS);
        //startTime
        this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                startT,Constants.OPMW_DATA_PROP_OVERALL_START_TIME,
                    XSDDatatype.XSDdateTime);
        //endTime
        this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                endT,Constants.OPMW_DATA_PROP_OVERALL_END_TIME,
                    XSDDatatype.XSDdateTime);
        if(execDiagram!=null){
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                execDiagram.getURI(),Constants.OPMW_DATA_PROP_HAS_EXECUTION_DIAGRAM,
                    XSDDatatype.XSDanyURI);
        }
        if(templDiagram!=null){
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                templDiagram.getURI(),Constants.OPMW_DATA_PROP_HAS_TEMPLATE_DIAGRAM,
                    XSDDatatype.XSDanyURI);
        }
        if(license!=null){
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                license,Constants.DATA_PROP_RIGHTS,
                    XSDDatatype.XSDanyURI);
        }
        for(int i = 0; i<tools.size(); i++){
            currTool = (ArrayList<String>) tools.get(i);
            String id = (String) currTool.get(0);
            String url = (String) currTool.get(1);
            String version = (String) currTool.get(2);
            //add the tool as an agent (or artifact)?
            if(id!=null){
                this.addIndividual(OPMWModel,id, Constants.OPM_AGENT, "Tool "+id);
                
                /*************************
                * PROV-O INTEROPERABILITY
                *************************/
                agEncoded = encode(Constants.CONCEPT_AGENT+"/"+id);
                ag.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+agEncoded);
            
                if(url!=null){
                    this.addDataProperty(OPMWModel,Constants.CONCEPT_AGENT+"/"+id,
                    url,Constants.DATA_PROP_HAS_HOME_PAGE,
                        XSDDatatype.XSDanyURI);
                }
                if(version != null){
                    this.addDataProperty(OPMWModel,Constants.CONCEPT_AGENT+"/"+id,
                    version,Constants.OPMW_DATA_PROP_VERSION_NUMBER);
                }
                //bind the agent to the account
                this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                    Constants.CONCEPT_AGENT+"/"+id, 
                        Constants.OPMW_PROP_EXECUTED_IN_WORKFLOW_SYSTEM);
                /*************************
                * PROV-O INTEROPERABILITY
                *************************/
                this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                    Constants.CONCEPT_AGENT+"/"+id, 
                        Constants.PROV_WAS_ATTRIBUTED_TO);
            }
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
            this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                Constants.CONCEPT_WORKFLOW_TEMPLATE+"/"+templateName,
                    Constants.PROV_WAS_INFLUENCED_BY);
        }
        //nodes and links to the process templates
        /********************************************************/
        /********************* NODE LINKING**********************/
        /********************************************************/
        //an execution process has to be made per componentBinding, not per Node
        String queryNodes = Queries.queryNodesResults();
        r = null;
        r = queryLocalWINGSResultsRepository(queryNodes);
        //iteration through the nodes
        //PROPERTIES TO BE USED        
        Property pFirst = WINGSExecutionResults.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
//        Property pRest = WINGSExecutionResults.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");
        while(r.hasNext()){
            QuerySolution qs = r.next();
            Resource node = qs.getResource("?nodeId");
            Resource absComp = qs.getResource("?absComponent");//used in the template
            Resource compBinding = qs.getResource("?cbind");
            
            //if cbind if a list, parse the list (could be n-dimensional)
            String processName = node.getLocalName()+date;
            if(compBinding.getProperty(pFirst)!=null){
                System.out.println("It's a list!!!. Node: "+node.getLocalName());
                //query list recursively, given the blank node
                treatProcessFromComponentBindingList(node,absComp,compBinding,processName,date,user,templateName);
            }else{//else, parse it normally
                System.out.println("NOT a list!!!. Node: "+node.getLocalName());                
                this.addProcessMetadata(node, absComp, compBinding, processName,date,user,templateName);
            }  
        }
        /***********************************************************************************
         * FILE EXPORT 
         ***********************************************************************************/        
        exportRDFFile(outFilenameOPMW, OPMWModel);
        exportRDFFile(outFilenamePROV, PROVModel);
        return (Constants.PREFIX_EXPORT_RESOURCE+accname);
    }
    
    /**
     * 
     * @param node
     * @param abstractType
     * @param cbind: the resource that has the metadata about the process 
     * @param processName
     * @param date
     * @param user 
     */
    private void addProcessMetadata(Resource node, Resource abstractType, Resource cbind, String processName, String date, String user, String templateName){
        //component, location
        Property pComp = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_COMPONENT);
        Property pLoc = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_LOCATION);
        Property pHasInput = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_INPUT);
        Property pHasOutput = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_OUTPUT);
        Property pHasArgument = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_ARGUMENT_ID);
        Property pHasVariable = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_VARIABLE);
        Property pHasDBinding = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_DATA_BINDING);
        Property pHasPBinding = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_PARAMETER_BINDING);
        Statement sComponent = cbind.getProperty(pComp);
        Statement sLocation = cbind.getProperty(pLoc);
        Resource comp = (Resource) sComponent.getObject();
        Resource compLocation =  (Resource) sLocation.getObject();
        
        if(compLocation!=null){
            System.out.println("Node Instance: "+node.getLocalName()+ " has type: "+abstractType+ " and specific binding: "
                    + comp+ ". Location of the component: "+ compLocation.getURI());
        }
        //add to the model the rdf as process instances
        this.addIndividual(OPMWModel,processName, Constants.OPMW_WORKFLOW_EXECUTION_PROCESS, "Execution process "+node.getLocalName());
        //add type opmv:Process as well
        String auxP = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processName);
        OntClass cP = OPMWModel.createClass(Constants.OPM_PROCESS);//repeated tuples will not be duplicated
        cP.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxP);
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/
        OntClass d = PROVModel.createClass(Constants.PROV_ACTIVITY);
        d.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+auxP);
        
        //link them to the process templates
        this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processName,
                Constants.CONCEPT_WORKFLOW_TEMPLATE_PROCESS+"/"+templateName+"_"+node.getLocalName(),
                    Constants.OPMW_PROP_CORRESPONDS_TO_TEMPLATE_PROCESS);
        //assert the used components (specific components)
        this.addIndividual(OPMWModel,"Component"+comp.getLocalName()+date, comp.getURI(), comp.getLocalName());
        this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processName,
                getClassName(comp.getURI())+"/"+"Component"+comp.getLocalName()+date,
                    Constants.OPMW_PROP_HAS_EXECUTABLE_COMPONENT);
        
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/
        //has specific component is a subprop of used
        this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processName,
                getClassName(comp.getURI())+"/"+"Component"+comp.getLocalName()+date,
                    Constants.PROV_USED);
        
        //assert the process as part of the OPM account of the execution
        this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processName,
                Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                    Constants.OPM_PROP_ACCOUNT);
        //add the agent as the controller of the process
        this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processName,
                Constants.CONCEPT_AGENT+"/"+user,
                    Constants.OPM_PROP_WCB);
        
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/ 
        //wcb subprop of associatedwith
        this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processName,
                Constants.CONCEPT_AGENT+"/"+user,
                    Constants.PROV_WAS_ASSOCIATED_WITH);
        //add the location of the component (URL)
        if(compLocation!=null){
            //the location should be added to the component, not the process                
            this.addDataProperty(OPMWModel,getClassName(comp.getURI())+"/"+"Component"+comp.getLocalName()+date,
                    compLocation.getURI(),Constants.OPMW_DATA_PROP_HAS_LOCATION,
                        XSDDatatype.XSDanyURI);
            //prov-o interoperability: hasLocation subrprop of atLocation
            this.addDataProperty(PROVModel,getClassName(comp.getURI())+"/"+"Component"+comp.getLocalName()+date,
                    compLocation.getURI(),Constants.PROV_AT_LOCATION,
                        XSDDatatype.XSDanyURI);
        }
        //Iterate through the inputs
        StmtIterator listPropertiesIn = cbind.listProperties(pHasInput);
        while (listPropertiesIn.hasNext()){//if there are any, then they have the following structure
            Statement currentInput = listPropertiesIn.next();
            Resource inputBinding = (Resource) currentInput.getObject();
            //hasArgument, hasVariable, hasDataBinding
            Literal argID = (Literal) inputBinding.getProperty(pHasArgument).getObject();
            Resource variable = (Resource) inputBinding.getProperty(pHasVariable).getObject();
            Statement dataBinding = inputBinding.getProperty(pHasDBinding);
            Statement parameterBinding = inputBinding.getProperty(pHasPBinding);
            //data binding can be a list, literal or resource
            if(parameterBinding!=null){
                System.out.println("Data binding literal (parameter)");
                //set of actions of the input parameter.
                this.addArtifactMetadata(parameterBinding.getObject(), true, true, variable, argID.getLexicalForm(),
                        processName, templateName, date);
            }else if ((dataBinding.getObject()).isURIResource()){
                System.out.println("Data binding resource (URI)");
                //set of assertions of the input artifact
                this.addArtifactMetadata(dataBinding.getObject(), false, true, variable, argID.getLexicalForm(),
                        processName, templateName, date);
            }else{
                System.out.println("Data binding is a list!!");
                treatArtifactNamesFromDataBindingList((Resource)dataBinding.getObject(),true, variable, argID.getLexicalForm(),
                        processName, templateName, date);
            }
        }
        //Iterate through the outputs
        StmtIterator listPropertiesOut = cbind.listProperties(pHasOutput);
        while (listPropertiesOut.hasNext()){
            Statement currentOutput = listPropertiesOut.next();
            Resource outputBinding = (Resource) currentOutput.getObject();
            //hasArgument, hasVariable, hasDataBinding
            Literal argID = (Literal) outputBinding.getProperty(pHasArgument).getObject();
            Resource variable = (Resource) outputBinding.getProperty(pHasVariable).getObject();
            RDFNode dataBinding = outputBinding.getProperty(pHasDBinding).getObject();
            //data binding can be a list or resource
            if (dataBinding.isURIResource()){
                System.out.println("Data binding resource (URI)");
                //set of assertions of the output artifact
                this.addArtifactMetadata(dataBinding, false, false, variable, argID.getLexicalForm(),
                        processName, templateName, date);
            }else{
                System.out.println("Data binding is a list!!");
                treatArtifactNamesFromDataBindingList((Resource)dataBinding,false, variable, argID.getLexicalForm(),
                        processName, templateName, date);
            }
        }
        
    }
    
    private void treatProcessFromComponentBindingList(Resource node, Resource abstractType, Resource cbind,String processBaseName, String date, String user, String template){
        Statement isList = cbind.getProperty(WINGSExecutionResults.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first"));
        if(isList == null){            
            //for every component binding, we add a new process
            addProcessMetadata(node, abstractType, cbind,this.getNextId(processBaseName),date,user,template);
            return;// no hay que seguir porque es la URI del artifact            
        }else{//recursively we see if the first is another list
            treatProcessFromComponentBindingList(node,abstractType,(Resource)isList.getObject(),processBaseName,date,user,template);
        }
        //iterate through the rest of the list, if it's not finished.
        Resource rest = (Resource) cbind.getProperty(WINGSExecutionResults.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")).getObject();
        if(rest.getURI() == null){
            treatProcessFromComponentBindingList(node, abstractType,rest, processBaseName, date, user, template);
        }
    }
    
    //small auxiliar function to help provide unique ids for processes
    private String getNextId(String baseName){
        this.count++;
        return (baseName+count);
    }
    
    private void treatArtifactNamesFromDataBindingList(Resource databinding,boolean isInput, Resource variableID,String role, String processId, String templateName, String date ){
        //if it is not a list, print current value     
        Statement isList = databinding.getProperty(WINGSExecutionResults.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first"));
        if(isList == null){
            if(databinding.isURIResource()){//variable
                addArtifactMetadata(databinding, false, isInput, variableID, role, processId, templateName, date);
            }else{//parameter
                addArtifactMetadata(databinding, true, isInput, variableID, role, processId, templateName, date);
            }
            System.out.println(databinding.getLocalName());
            return;// no hay que seguir porque es la URI del artifact            
        }else{//recursively we see if the first is another list
             treatArtifactNamesFromDataBindingList((Resource)isList.getObject(),isInput, variableID, role, processId, templateName, date);                       
        }
        //iterate through the rest of the list, if it's not finished.
        Resource rest = (Resource) databinding.getProperty(WINGSExecutionResults.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")).getObject();
        if(rest.getURI() == null){
            treatArtifactNamesFromDataBindingList(rest, isInput,variableID, role, processId, templateName, date);
        }
        
    }
    
    //the dataBind value is supposed to be the node at the lower level of granularity. No lists allowed.
    private void addArtifactMetadata(RDFNode dataBind, boolean isParameter, boolean isInput, Resource variableID,String role, String processId, String templateName, String date ){
        String artifactID = null;
        Property pHasLocation = WINGSExecutionResults.createProperty(Constants.WINGS_PROP_HAS_LOCATION);
        Property pHasSize = WINGSExecutionResults.createProperty(Constants.WINGS_DATA_PROP_HAS_SIZE);
        Property pHasType = WINGSExecutionResults.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");        
        
        if(isParameter){
            Literal parameterValue = (Literal) dataBind;            
            System.out.println("process: "+ processId +" has inputID " +variableID.getLocalName()
                    +" with parameter value: "+parameterValue.getLexicalForm());
            //parameter variables
            artifactID = variableID.getLocalName()+parameterValue.getLexicalForm();
            //link the artifact to its template
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    Constants.CONCEPT_PARAMETER_VARIABLE+"/"+templateName+"_"+variableID.getLocalName(),
                        Constants.OPMW_PROP_CORRESPONDS_TO_TEMPLATE_ARTIFACT);
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID, parameterValue.getLexicalForm(),
                    Constants.OPMW_DATA_PROP_HAS_VALUE);
            
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            //hasValue subprop of prov:value
            this.addDataProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID, parameterValue.getLexicalForm(),
                    Constants.PROV_VALUE);
        
        }else{//is a data variable
            //get location, size and type from the artifact
            Resource artifact = (Resource) dataBind;
            Resource location = (Resource) artifact.getProperty(pHasLocation).getObject();
            Literal size = (Literal) artifact.getProperty(pHasSize).getObject();
            StmtIterator types = artifact.listProperties(pHasType);     
                    
            System.out.println("Process: "+ processId+" has inputID " +role
                    +" with loc: "+location+"\n, size: "+size.getString());                
            try {
                //they are all data variables
                //create the artifact from its location. Type: artifactInstance AND varT
                artifactID = MD5Util.MD5(location.getURI());
            } catch (Exception ex) {
                System.out.println("Error while trying to make MD5Util key "+ex.getMessage());
                artifactID = location.getLocalName();
            }
//            artifactID =variableID.getLocalName() + artifactID;
            String nombreIndividuoEnc = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID);
            
            //assert all the types to the artifact (it may have more than one)
            while(types.hasNext()){
                Statement stAux = types.next();
                Resource currentType = (Resource)stAux.getObject();
                OntClass c = OPMWModel.createClass(currentType.getURI());
                c.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+nombreIndividuoEnc);
            }
            
            //link the artifact to its template
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    Constants.CONCEPT_DATA_VARIABLE+"/"+templateName+"_"+variableID.getLocalName(),
                        Constants.OPMW_PROP_CORRESPONDS_TO_TEMPLATE_ARTIFACT);
            
            //assert size, location to artifact
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    location.getURI(),
                        Constants.OPMW_DATA_PROP_HAS_LOCATION, XSDDatatype.XSDanyURI);
            
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            //hasLocation subrpop of atLocation
            this.addDataProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    location.getURI(),
                        Constants.PROV_AT_LOCATION, XSDDatatype.XSDanyURI);
            
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    size.getLexicalForm(),
                        Constants.OPMW_DATA_PROP_HAS_SIZE, XSDDatatype.XSDlong);
            
            //DCDOM custom properties
            //use artifactID from WINGS to retrieve dcdom properties. They are all data properties
            addDCDOMProperties(artifactID, artifact.getURI());
            
            //add here the property to link the variables to the same name: hasFileName
            this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    getFileName(location.getURI()),
                        Constants.OPMW_DATA_PROP_HAS_FILE_NAME);            
        }
        //common artifact metadata
        this.addIndividual(OPMWModel,artifactID, Constants.OPMW_WORKFLOW_EXECUTION_ARTIFACT, "Execution artifact with id: "+artifactID);

        //this is done this way to avoid inferences and suppport interoperability
        String aux = encode(Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID);
        OntClass c = OPMWModel.createClass(Constants.OPM_ARTIFACT);//repeated tuples will not be duplicated
        c.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+aux);
        /*************************
         * PROV-O INTEROPERABILITY
         *************************/
        OntClass d = PROVModel.createClass(Constants.PROV_ENTITY);
        d.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+aux);

        //assert artifact to opm account
        this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ACCOUNT+"/"+"Account"+date,
                        Constants.OPM_PROP_ACCOUNT);
        
        if(isInput){
            //assert USED relationship between the node and the artifact
            //Roles should be specialized here.
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processId,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                        Constants.OPM_PROP_USED);
            //roles
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processId,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                            Constants.PREFIX_OPMW+"usedAs_"+role);
            this.createSubProperty(OPMWModel,Constants.OPM_PROP_USED, Constants.PREFIX_OPMW+"usedAs_"+role);
            
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            //used subprop of prov:used
            this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processId,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                        Constants.PROV_USED);
            this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processId,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                            Constants.PREFIX_OPMW+"usedAs_"+role);
            this.createSubProperty(PROVModel,Constants.PROV_USED, Constants.PREFIX_OPMW+"usedAs_"+role);
        }else{
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processId,
                        Constants.OPM_PROP_WGB);
            //add roles for the wgb extended relations. Since the subproperty is added
            //in the template, there is no need to add it here.
            this.addProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processId,
                            Constants.PREFIX_OPMW+"wasGeneratedByAs_"+role);
            this.createSubProperty(OPMWModel,Constants.OPM_PROP_WGB, Constants.PREFIX_OPMW+"wasGeneratedByAs_"+role);            
            
            /*************************
            * PROV-O INTEROPERABILITY
            *************************/ 
            //used wgb of prov:wgb
            this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processId,
                        Constants.PROV_WGB);
            this.addProperty(PROVModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                    Constants.CONCEPT_WORKFLOW_EXECUTION_PROCESS+"/"+processId,
                            Constants.PREFIX_OPMW+"wasGeneratedByAs_"+role);
            this.createSubProperty(PROVModel,Constants.PROV_WGB, Constants.PREFIX_OPMW+"wasGeneratedByAs_"+role);
        }
        
        
    }
    

    /**
     * given a url, it will take the last id of the file
     * @param url
     * @return the last part of the url
     */
    private String getFileName(String url){
        return url.substring(url.lastIndexOf("/")+1, url.length());
    }

    private void addDCDOMProperties(String artifactID, String WINGSArtifactID){
        //use artifactID from WINGS to retrieve dcdom properties. They are all data properties
        String queryDCDOMprops = Queries.queryDCDOMProperties(WINGSArtifactID);
        ResultSet rAux = queryLocalWINGSResultsRepository(queryDCDOMprops);
        while(rAux.hasNext()){
                    QuerySolution qsAux = rAux.next();
                    Resource prop = qsAux.getResource("?prop");
                    try{
                        Literal value = qsAux.getLiteral("?value");
                        //ONLY dcdom properties
                        if(prop.getURI().contains(DCDOM)){
                            if(value.getDatatype() == null){
                                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                                    value.getString(),
                                        prop.getURI());
                            }else{//not string
                                XSDDatatype tipo = (XSDDatatype) value.getDatatype();
                                if(tipo.equals(XSDDatatype.XSDinteger)){
                                    tipo = XSDDatatype.XSDlong;
                                }//we switch to long in case the int numbers are too high.                                
                                this.addDataProperty(OPMWModel,Constants.CONCEPT_WORKFLOW_EXECUTION_ARTIFACT+"/"+artifactID,
                                        value.getString(),
                                            prop.getURI(),
                                                tipo);
                            }
                        }

                    }catch(Exception e){
                        //If it fails, they are not dcdom properties
                    }
                }
    }

    /**
     * Function to export the stored model as an RDF file, using ttl syntax
     * @param outFile name and path of the outFile must be created.
     */
    private void exportRDFFile(String outFile, OntModel model){
        OutputStream out;
        try {
            out = new FileOutputStream(outFile);
            model.write(out,"TURTLE");
        } catch (FileNotFoundException ex) {
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
        String nombreIndividuoEnc = encode(getClassName(classURL)+"/"+individualId);
        OntClass c = m.createClass(classURL);
        c.createIndividual(Constants.PREFIX_EXPORT_RESOURCE+nombreIndividuoEnc);
        if(label!=null){
            this.addDataProperty(m,nombreIndividuoEnc,label,Constants.RDFS_LABEL);
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
                nom = MD5Util.MD5(name);
            } catch (Exception ex) {
                System.err.println("Error when encoding in MD5Util: "+ex.getMessage() );
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
                System.err.println("Problem encoding the URI:" + nom + " " + ex.getMessage() +". We encode it in MD5Util");
                nom = MD5Util.MD5(name);
                System.err.println("MD5Util encoding: "+nom);
            } catch (Exception ex1) {
                System.err.println("Could not encode in MD5Util:" + name + " " + ex1.getMessage());
            }
        }
        return prenom+nom;
    }

    private String getClassName(String classAndVoc){
        if(classAndVoc.contains(Constants.PREFIX_DCTERMS))return classAndVoc.replace(Constants.PREFIX_DCTERMS,"");
        else if(classAndVoc.contains(Constants.PREFIX_FOAF))return classAndVoc.replace(Constants.PREFIX_FOAF,"");
        else if(classAndVoc.contains(Constants.PREFIX_OPMO))return classAndVoc.replace(Constants.PREFIX_OPMO,"");
        else if(classAndVoc.contains(Constants.PREFIX_OPMV))return classAndVoc.replace(Constants.PREFIX_OPMV,"");
        else if(classAndVoc.contains(Constants.PREFIX_RDFS))return classAndVoc.replace(Constants.PREFIX_RDFS,"");
        else if(classAndVoc.contains(Constants.PREFIX_OPMW))return classAndVoc.replace(Constants.PREFIX_OPMW,"");
        else if(classAndVoc.contains(ACDOM))return classAndVoc.replace(ACDOM,"");
        else if(classAndVoc.contains(DCDOM))return classAndVoc.replace(DCDOM,"");
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
        if(rs.hasNext()) return true;
        return false;        
    }

    
}
