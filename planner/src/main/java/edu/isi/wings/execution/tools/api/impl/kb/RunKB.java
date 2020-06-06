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

package edu.isi.wings.execution.tools.api.impl.kb;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.SparqlQuerySolution;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.VariableBindingsListSet;
import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.common.CollectionsHelper;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.execution.engine.classes.ExecutionQueue;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.planner.api.WorkflowGenerationAPI;
import edu.isi.wings.planner.api.impl.kb.WorkflowGenerationKB;
import edu.isi.wings.workflow.plan.PlanFactory;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;

public class RunKB extends TransactionsJena 
implements ExecutionLoggerAPI, ExecutionMonitorAPI {
	KBAPI kb;
	KBAPI libkb;
	KBAPI unionkb;

	Properties props;

	String ns;
	String onturl;
	String liburl;
	String newrunurl;
	String newtplurl;
	String tdbRepository;
	String logsDirectory;

	protected HashMap<String, KBObject> objPropMap;
	protected HashMap<String, KBObject> dataPropMap;
	protected HashMap<String, KBObject> conceptMap;

	public RunKB(Properties props) {
		this.props = props;
		this.onturl = props.getProperty("ont.execution.url");
		this.liburl = props.getProperty("lib.domain.execution.url");
		this.newtplurl = props.getProperty("domain.workflows.dir.url") + "/";
		this.newrunurl = props.getProperty("domain.executions.dir.url") + "/";
		this.tdbRepository = props.getProperty("tdb.repository.dir");
		this.logsDirectory = props.getProperty("logs.dir");

		if (tdbRepository == null) {
			this.ontologyFactory = new OntFactory(OntFactory.JENA);
		} else {
			this.ontologyFactory = new OntFactory(OntFactory.JENA, this.tdbRepository);
		}
		KBUtils.createLocationMappings(props, this.ontologyFactory);
		try {
			this.kb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN, true);
			this.kb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, false, true));
			this.libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
			this.unionkb = 
			    this.ontologyFactory.getKB("urn:x-arq:UnionGraph", OntSpec.PLAIN);
			
			this.initializeMaps();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeMaps() {
		this.objPropMap = new HashMap<String, KBObject>();
		this.dataPropMap = new HashMap<String, KBObject>();
		this.conceptMap = new HashMap<String, KBObject>();

		this.start_read();
		for (KBObject prop : this.kb.getAllObjectProperties()) {
			this.objPropMap.put(prop.getName(), prop);
		}
		for (KBObject prop : this.kb.getAllDatatypeProperties()) {
			this.dataPropMap.put(prop.getName(), prop);
		}
		for (KBObject con : this.kb.getAllClasses()) {
			this.conceptMap.put(con.getName(), con);
		}
		this.end();
		this.start_write();
		if (!dataPropMap.containsKey("hasLog"))
			dataPropMap.put("hasLog", this.kb.createDatatypeProperty(this.onturl + "#hasLog"));
    if(!objPropMap.containsKey("hasSeededTemplate"))
      objPropMap.put("hasSeededTemplate", kb.createObjectProperty(this.onturl+"#hasSeededTemplate"));
    this.end();
	}

	@Override
	public synchronized void startLogging(RuntimePlan exe) {
		try {
		  KBAPI tkb = this.ontologyFactory.getKB(exe.getURL(), OntSpec.PLAIN);
		  
		  this.start_write();
		  this.writeExecutionRun(tkb, exe);
		  KBObject exobj = kb.createObjectOfClass(exe.getID(), conceptMap.get("Execution"));
		  this.updateRuntimeInfo(kb, exobj, exe.getRuntimeInfo());
		  this.save();
		  
		} catch (Exception e) {
		  e.printStackTrace();
		} finally {
		  this.end();
		}
	}

	@Override
	public synchronized void updateRuntimeInfo(RuntimePlan exe) {
	  try {
	    KBAPI tkb = this.ontologyFactory.getKB(exe.getURL(), OntSpec.PLAIN);
	    this.start_write();
	    
	    this.updateExecutionRun(tkb, exe);
	    KBObject exobj = kb.getIndividual(exe.getID());
	    if(exobj != null)
	      this.updateRuntimeInfo(kb, exobj, exe.getRuntimeInfo());
	    
	    this.save(tkb);
	    this.save();
	  } catch (Exception e) {
	    e.printStackTrace();
	  } finally {
	    this.end();
	  }
	}

	@Override
	public synchronized void updateRuntimeInfo(RuntimeStep stepexe) {
    try {
      KBAPI tkb = this.ontologyFactory.getKB(stepexe.getRuntimePlan().getURL(),
          OntSpec.PLAIN);
      this.start_write();
      this.updateExecutionStep(tkb, stepexe);
      tkb.save();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      this.end();
    }
	}

	@Override
  public int getNumberOfRuns(String pattern, String status, Date started_after) {
	    String starttime = null;
	    if(started_after != null) {
	      LocalDateTime time = started_after.toInstant()
	          .atZone(ZoneId.systemDefault()).toLocalDateTime();
	      ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(time);
	      starttime = time.format(DateTimeFormatter.ISO_DATE_TIME) + offset.getId();
	    }
	    
	    String query = 
	        "PREFIX exec: <http://www.wings-workflows.org/ontology/execution.owl#>\n" + 
          "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
	        "SELECT (COUNT(*) as ?count)\n" + 
	        "WHERE { " +
          "?run a exec:Execution .\n" +
          "?run exec:hasExecutionStatus ?status .\n" +
          "?run exec:hasTemplate ?template .\n" +
          "?run exec:hasStartTime ?start .\n" +
          (status != null ? 
              "?run exec:hasExecutionStatus '" + status + "') .\n" : "") +
          (starttime != null ?
              "FILTER(?start > '"+starttime+"'^^xsd:dateTime) .\n" : "") +      
          (pattern != null ? 
              "FILTER REGEX(str(?template), '" + newtplurl + ".*" + pattern + ".*') .\n" : "") +
          "FILTER REGEX(str(?run), '" + newrunurl + "') .\n" +
	        "}";

	    this.start_read();
	    ArrayList<ArrayList<SparqlQuerySolution>> result = unionkb.sparqlQuery(query);
	    int size = (Integer) result.get(0).get(0).getObject().getValue();
	    this.end();
	    return size;
	 }

	public ArrayList<RuntimePlan> getRunListSimple(String pattern, String status, 
	    int start, int limit, Date started_after) {
		ArrayList<RuntimePlan> rplans = new ArrayList<RuntimePlan>();
		
		String starttime = null;
		if(started_after != null) {
		  LocalDateTime time = started_after.toInstant()
		      .atZone(ZoneId.systemDefault()).toLocalDateTime();
		  ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(time);
		  starttime = time.format(DateTimeFormatter.ISO_DATE_TIME) + offset.getId();
		}

		String query =
						"PREFIX exec: <http://www.wings-workflows.org/ontology/execution.owl#>\n" +
		        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
										"SELECT ?run ?status ?template ?start ?end \n" +
										"WHERE {\n" +
										"?run a exec:Execution .\n" +
										"?run exec:hasExecutionStatus ?status .\n" +
										"?run exec:hasTemplate ?template .\n" +
										"?run exec:hasStartTime ?start .\n" +
										(status != null ? 
										    "?run exec:hasExecutionStatus '" + status + "') .\n" : "") +
                    (starttime != null ?
                        "FILTER(?start > '"+starttime+"'^^xsd:dateTime) .\n" : "") +			
                    (pattern != null ? 
                        "FILTER REGEX(str(?template), '" + newtplurl + ".*" + pattern + ".*') .\n" : "") +
										"FILTER REGEX(str(?run), '" + newrunurl + "') .\n";
		query += "}\n";

		if(limit >= 0 && start >=0) {
			query += " LIMIT " + limit + " OFFSET " + start;
		}
		
		this.start_read();
		ArrayList<ArrayList<SparqlQuerySolution>> result = unionkb.sparqlQuery(query);
		for(ArrayList<SparqlQuerySolution> row : result) {
			HashMap<String, KBObject> vals = new HashMap<String, KBObject>();
			for(SparqlQuerySolution col : row)
				vals.put(col.getVariable(), col.getObject());
			if(vals.get("run") == null)
				continue;
			RuntimePlan rplan = new RuntimePlan(vals.get("run").getID());
			rplan.setOriginalTemplateID(vals.get("template").getID());
			RuntimeInfo info = new RuntimeInfo();
			KBObject sttime = vals.get("start");
			if (sttime != null && sttime.getValue() != null)
				info.setStartTime((Date) sttime.getValue());
			KBObject endtime = vals.get("end");
			if (endtime != null && endtime.getValue() != null)
				info.setEndTime((Date) endtime.getValue());
			KBObject statusObj = vals.get("status");
			if (statusObj != null && statusObj.getValue() != null)
				info.setStatus(RuntimeInfo.Status.valueOf((String) statusObj.getValue()));
			rplan.setRuntimeInfo(info);
			rplans.add(rplan);
		}
		this.end();
		return rplans;
	}


	@Override
	public ArrayList<RuntimePlan> getRunList(String pattern, String status, int start, int limit, boolean fasterQuery) {
	  ArrayList<RuntimePlan> rplans = new ArrayList<RuntimePlan>();
	  
	  if(pattern == null) {
	    pattern = "";
	  }
	  
	  String query = 
	      "PREFIX exec: <http://www.wings-workflows.org/ontology/execution.owl#>\n" + 
	      "SELECT ?run ?status ?template ?start ?end \n" + 
	      "(GROUP_CONCAT (CONCAT (STR(?step), '=', STR(?stepstatus)); "
	          + "SEPARATOR=\"|\") AS ?steps)  \n" + 
	      "WHERE {\n" + 
	      "?run a exec:Execution .\n" + 
	      "?run exec:hasExecutionStatus ?status .\n" + 
	      "?run exec:hasTemplate ?template .\n" + 
	      "?run exec:hasStartTime ?start .\n" + 
	      "FILTER REGEX(str(?run), '" + newrunurl + "') .\n" +
	      "FILTER REGEX(str(?template), '" + newtplurl + ".*" + pattern + ".*') .\n";
	  
	  if(status == null || status.equals("SUCCESS")) {
  	  query += 
          // Query for Successful runs
          "{\n" +
          " ?run exec:hasExecutionStatus 'SUCCESS' .\n" +        
          " ?run exec:hasEndTime ?end .\n" +
          "}\n";
	  }
    if(status == null || status.equals("FAILURE")) {
      if(status == null) 
        query += "UNION\n";
      query += 
          // Query for Failures        
          "{\n" +
          " ?run exec:hasExecutionStatus 'FAILURE' .\n" +         
          " ?run exec:hasEndTime ?end .\n" +
          // FIXME: Removing this as we could have failures without any steps
          /*" ?run exec:hasStep ?step .\n" + 
          (fasterQuery ? " ?step exec:hasExecutionStatus 'FAILURE' .\n": "") + 
          " ?step exec:hasExecutionStatus ?stepstatus .\n" +*/
          "}\n";
    }
    if(status == null || status.equals("RUNNING")) {
      if(status == null) 
        query += "UNION\n";
      query += 
          // Query for Failures        
          "{\n" +
          " ?run exec:hasExecutionStatus 'RUNNING' .\n" +         
          " ?run exec:hasStep ?step .\n" +
          (fasterQuery ? " ?step exec:hasExecutionStatus 'RUNNING' .\n": "") +           
          " ?step exec:hasExecutionStatus ?stepstatus .\n" +
          "}\n";
    }
    query += "}\n";
    
    query += "GROUP BY ?run ?status ?template ?start ?end \n" +
	      "ORDER BY DESC(?start)";
    
	  if(limit >= 0 && start >=0) {
	    query += " LIMIT " + limit + " OFFSET " + start;
	  }
	  
	  //System.out.println(query);
	  this.start_read();
	  ArrayList<ArrayList<SparqlQuerySolution>> result = unionkb.sparqlQuery(query);
	  for(ArrayList<SparqlQuerySolution> row : result) {
	    HashMap<String, KBObject> vals = new HashMap<String, KBObject>();
	    for(SparqlQuerySolution col : row)
	      vals.put(col.getVariable(), col.getObject());
	    if(vals.get("run") == null)
	      continue;
	    RuntimePlan rplan = new RuntimePlan(vals.get("run").getID());
	    rplan.setOriginalTemplateID(vals.get("template").getID());
	    RuntimeInfo info = new RuntimeInfo();
	    KBObject sttime = vals.get("start");
	    if (sttime != null && sttime.getValue() != null)
	      info.setStartTime((Date) sttime.getValue());
	    KBObject endtime = vals.get("end");
	    if (endtime != null && endtime.getValue() != null)
	      info.setEndTime((Date) endtime.getValue());
	    KBObject statusObj = vals.get("status");
	    if (statusObj != null && statusObj.getValue() != null)
	      info.setStatus(RuntimeInfo.Status.valueOf((String) statusObj.getValue()));
	    rplan.setRuntimeInfo(info);
	    
	    KBObject steps = vals.get("steps");
	    ExecutionQueue queue = new ExecutionQueue();
	    if(steps != null && steps.getValue() != null) {
	      for(String stepstatus : ((String) steps.getValue()).split("\\|")) {
	        String[] ss = stepstatus.split("=", 2);
	        RuntimeStep step = new RuntimeStep(ss[0]);
	        RuntimeInfo stepinfo = new RuntimeInfo();
	        stepinfo.setStatus(RuntimeInfo.Status.valueOf(ss[1]));
	        step.setRuntimeInfo(stepinfo);
	        queue.addStep(step);
	      }
	    }
	    rplan.setQueue(queue);
	    rplans.add(rplan);
	  }
	  this.end();
	  
	  /*
		ArrayList<RuntimePlan> rplans = new ArrayList<RuntimePlan>();
		for (KBObject exobj : this.kb.getInstancesOfClass(conceptMap.get("Execution"), true)) {
			RuntimePlan rplan = this.getExecutionRun(exobj, false);
			rplans.add(rplan);
		}*/
		return rplans;
	}

	@Override
	public RuntimePlan getRunDetails(String runid) {
		try {
			RuntimePlan rplan = this.getExecutionRun(runid, true);
			return rplan;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean deleteRun(String runid, boolean deleteOutputs) {
		return this.deleteExecutionRun(runid, deleteOutputs);
	}

	@Override
	public boolean runExists(String runid) {
	  try {
	    String runurl = runid.replaceAll("#.*$", "");
	    KBAPI tkb = this.ontologyFactory.getKB(runurl, OntSpec.PLAIN);
	    
	    this.start_read();
	    int size = tkb.getAllTriples().size();
	    this.end();
	    
	    if(tkb != null && size > 0)
	      return true;
	  }
	  catch (Exception e) {
	    e.printStackTrace();
	  }
	  return false;
	}
	

	@Override
	public boolean delete() {
	  boolean ok = true;
		for(RuntimePlan rplan : this.getRunListSimple(null, null, -1, -1, null)) {
			ok = this.deleteRun(rplan.getID(), false);
			if(!ok)
			  return false;
		}
		return
		    this.start_write() && 
		    this.kb.delete() && 
		    this.save() && 
		    this.end();
	}
	
	@Override
	public boolean save() {
	  return this.save(this.kb);
	}

	/*
	 * Private helper functions
	 */
	private KBObject writeExecutionRun(KBAPI tkb, RuntimePlan exe) {
		KBObject exobj = tkb.createObjectOfClass(exe.getID(), conceptMap.get("Execution"));
		KBObject xtobj = tkb.getResource(exe.getExpandedTemplateID());
		KBObject tobj = tkb.getResource(exe.getOriginalTemplateID());
		KBObject sobj = tkb.getResource(exe.getSeededTemplateID());
		KBObject pobj = tkb.getResource(exe.getPlan().getID());
		if(xtobj != null)
		  tkb.setPropertyValue(exobj, objPropMap.get("hasExpandedTemplate"), xtobj);
		if(sobj != null)
		  tkb.setPropertyValue(exobj, objPropMap.get("hasSeededTemplate"), sobj);
		if(tobj != null)
		  tkb.setPropertyValue(exobj, objPropMap.get("hasTemplate"), tobj);
		if(pobj != null)
		  tkb.setPropertyValue(exobj, objPropMap.get("hasPlan"), pobj);
		for (RuntimeStep stepexe : exe.getQueue().getAllSteps()) {
			KBObject stepobj = this.writeExecutionStep(tkb, stepexe);
			tkb.addPropertyValue(exobj, objPropMap.get("hasStep"), stepobj);
		}
		this.updateRuntimeInfo(tkb, exobj, exe.getRuntimeInfo());
		return exobj;
	}

	private RuntimePlan getExecutionRun(String runid, boolean details) {
		// Create new runtime plan
	  KBObject exobj = this.kb.getResource(runid);
	  if(exobj == null)
	    return null;
	  
		RuntimePlan rplan = new RuntimePlan(exobj.getID());
		rplan.setRuntimeInfo(this.getRuntimeInfo(this.kb, exobj));
		RuntimeInfo.Status status = rplan.getRuntimeInfo().getStatus();
		if (details
				|| (status == RuntimeInfo.Status.FAILURE || status == RuntimeInfo.Status.RUNNING)) {
		  
			try {
				KBAPI tkb = this.ontologyFactory.getKB(rplan.getURL(), OntSpec.PLAIN);
				
				this.start_read(); 
				
				exobj = tkb.getIndividual(rplan.getID());
				if(exobj == null) {
				  this.end();
				  return null;
				}
				
        boolean batchok = this.start_batch_operation();
				// Get execution queue (list of steps)
				ExecutionQueue queue = new ExecutionQueue();
				for (KBObject stepobj : tkb.getPropertyValues(exobj, objPropMap.get("hasStep"))) {
					RuntimeStep rstep = new RuntimeStep(stepobj.getID());
					rstep.setRuntimeInfo(this.getRuntimeInfo(tkb, stepobj));
					queue.addStep(rstep);
				}
				rplan.setQueue(queue);

				// Get provenance information
				KBObject xtobj = tkb.getPropertyValue(exobj, objPropMap.get("hasExpandedTemplate"));
        KBObject sobj = tkb.getPropertyValue(exobj, objPropMap.get("hasSeededTemplate"));
				KBObject tobj = tkb.getPropertyValue(exobj, objPropMap.get("hasTemplate"));
				KBObject pobj = tkb.getPropertyValue(exobj, objPropMap.get("hasPlan"));
        
        if(batchok)
          this.stop_batch_operation();
        this.end();
        
				if(xtobj != null)
				  rplan.setExpandedTemplateID(xtobj.getID());
        if(sobj != null)
          rplan.setSeededTemplateId(sobj.getID());
				if(tobj != null)
				  rplan.setOriginalTemplateID(tobj.getID());
				if(pobj != null)
				  rplan.setPlan(PlanFactory.loadExecutionPlan(pobj.getID(), props));
        
				return rplan;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rplan;
	}

	private void deleteGraph(String id) {
	  try {
	    if(id != null) {
	      KBAPI tkb = ontologyFactory.getKB(new URIEntity(id).getURL(), OntSpec.PLAIN);
	      this.start_write();
	      tkb.delete();
	      this.save(tkb);
	      this.end();
	    }
    } catch (Exception e) {
      e.printStackTrace();
    }
	}
	
	@SuppressWarnings("unused")
  private boolean fileIsOutputofAnotherRun(ExecutionFile file) {
    String query = 
        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
        "PREFIX exec: <http://www.wings-workflows.org/ontology/execution.owl#>\n" + 
        "PREFIX wflow: <http://www.wings-workflows.org/ontology/workflow.owl#>\n" + 
        "PREFIX pplan: <http://purl.org/net/p-plan#>\n" + 
        "PREFIX wf: <http://purl.org/net/wf-invocation#>\n" + 
        "SELECT distinct ?run\n" + 
        "WHERE {\n" + 
        "  ?run a exec:Execution .\n" + 
        "  ?run exec:hasPlan ?plan .\n" + 
        "  ?var pplan:isVariableOfPlan ?plan .\n" + 
        "  ?var wf:hasDataBinding \"" + file.getLocation() + "\"^^ xsd:string\n" + 
        "}";
    
    this.start_read();
    ArrayList<ArrayList<SparqlQuerySolution>> result = unionkb.sparqlQuery(query);
    this.end();
    
    if(result.size() > 1)
      return true;
    return false;
	}
	
  private boolean xgraphIsUsedInAnotherRun(String graphid) {
    String query = 
        "PREFIX exec: <http://www.wings-workflows.org/ontology/execution.owl#>\n" +  
        "SELECT ?run\n" + 
        "WHERE {\n" + 
        "  ?run exec:hasExpandedTemplate <" + graphid + "> .\n" +  
        "}";
    
    this.start_read();
    ArrayList<ArrayList<SparqlQuerySolution>> result = unionkb.sparqlQuery(query);
    this.end();
    
    if(result.size() > 1)
      return true;
    return false;
  }	
  
  private boolean sgraphIsUsedInAnotherRun(String graphid) {
    String query = 
        "PREFIX exec: <http://www.wings-workflows.org/ontology/execution.owl#>\n" +  
        "SELECT ?run\n" + 
        "WHERE {\n" + 
        "  ?run exec:hasSeededTemplate <" + graphid + "> .\n" +  
        "}";
    
    this.start_read();
    ArrayList<ArrayList<SparqlQuerySolution>> result = unionkb.sparqlQuery(query);
    this.end();
    
    if(result.size() > 1)
      return true;
    return false;
  } 
  
	private boolean deleteExecutionRun(String runid, boolean deleteOutputs) {
		RuntimePlan rplan = this.getExecutionRun(runid, true);
		
		try {
			KBAPI tkb = this.ontologyFactory.getKB(rplan.getURL(), OntSpec.PLAIN);

			if(rplan.getPlan() != null) {
        for (ExecutionStep step : rplan.getPlan().getAllExecutionSteps()) {
          // Delete output files
          if(deleteOutputs) {
            for (ExecutionFile file : step.getOutputFiles()) {
              file.removeMetadataFile();
              File f = new File(file.getLocation());
              if(f.exists() && !this.fileIsOutputofAnotherRun(file))
                f.delete();
            }
          }
          // Delete log file
          File logfile = this.getLogFile(step.getID());
          if(logfile.exists())
            logfile.delete();
        }
			}
			// Delete expanded template
      if(!this.xgraphIsUsedInAnotherRun(rplan.getExpandedTemplateID()))
        this.deleteGraph(rplan.getExpandedTemplateID());
      
      // Delete seeded template
      if(!this.sgraphIsUsedInAnotherRun(rplan.getSeededTemplateID()))
        this.deleteGraph(rplan.getSeededTemplateID());
      
      // Delete execution plan
      if(rplan.getPlan() != null)
        this.deleteGraph(rplan.getPlan().getID());
      
      // Delete plan log file
      File plan_logfile = this.getLogFile(rplan.getID());
      if(plan_logfile.exists())
        plan_logfile.delete();
      
      // Delete execution provenance
      this.start_write();
      tkb.delete();
      this.save(tkb);
      this.end();
       			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		this.start_write();
		KBUtils.removeAllTriplesWith(this.kb, runid, false);
		this.save();
		this.end();
		
		return true;
	}

	private KBObject writeExecutionStep(KBAPI tkb, RuntimeStep stepexe) {
		KBObject exobj = tkb.createObjectOfClass(stepexe.getID(), conceptMap.get("ExecutionStep"));
		this.updateRuntimeInfo(tkb, exobj, stepexe.getRuntimeInfo());
		return exobj;
	}

	private void updateExecutionRun(KBAPI tkb, RuntimePlan exe) {
		KBObject exobj = tkb.getIndividual(exe.getID());
		if(exobj != null)
		  this.updateRuntimeInfo(tkb, exobj, exe.getRuntimeInfo());
	}

	private void updateExecutionStep(KBAPI tkb, RuntimeStep exe) {
		KBObject exobj = tkb.getIndividual(exe.getID());
		if(exobj == null) {
      exobj = this.writeExecutionStep(tkb, exe);
		  KBObject planexeobj = tkb.getIndividual(exe.getRuntimePlan().getID());
      tkb.addPropertyValue(planexeobj, objPropMap.get("hasStep"), exobj);
		}
		this.updateRuntimeInfo(tkb, exobj, exe.getRuntimeInfo());
	}
	
	private File getLogFile(String objid) {
    String fname = DigestUtils.md5Hex(objid);
    return new File(this.logsDirectory + File.separator + fname + ".log");
	}

	private void updateRuntimeInfo(KBAPI tkb, KBObject exobj, RuntimeInfo rinfo) {
	  if(rinfo.getLog() != null) {
	     /*
      tkb.setPropertyValue(exobj, dataPropMap.get("hasLog"),
          tkb.createLiteral(rinfo.getLog()));
      */
	    // Storing logs in a log file now
	    try {
	      File logfile = this.getLogFile(exobj.getID());
        FileUtils.writeStringToFile(logfile, rinfo.getLog());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } 
	  }
		if(rinfo.getStartTime() != null)
		  tkb.setPropertyValue(exobj, dataPropMap.get("hasStartTime"),
		      tkb.createLiteral(rinfo.getStartTime()));
		if(rinfo.getEndTime() != null)
		  tkb.setPropertyValue(exobj, dataPropMap.get("hasEndTime"),
		      tkb.createLiteral(rinfo.getEndTime()));
		if(rinfo.getStatus() != null)
		  tkb.setPropertyValue(exobj, dataPropMap.get("hasExecutionStatus"),
		      tkb.createLiteral(rinfo.getStatus().toString()));
	}

	private RuntimeInfo getRuntimeInfo(KBAPI tkb, KBObject exobj) {
	  this.start_read();
	  boolean batchok = this.start_batch_operation();
	  
		RuntimeInfo info = new RuntimeInfo();
		KBObject sttime = this.kb.getPropertyValue(exobj, dataPropMap.get("hasStartTime"));
		KBObject endtime = this.kb.getPropertyValue(exobj, dataPropMap.get("hasEndTime"));
		KBObject status = this.kb.getPropertyValue(exobj, dataPropMap.get("hasExecutionStatus"));
		if (sttime != null && sttime.getValue() != null)
			info.setStartTime((Date) sttime.getValue());
		if (endtime != null && endtime.getValue() != null)
			info.setEndTime((Date) endtime.getValue());
		if (status != null && status.getValue() != null)
			info.setStatus(RuntimeInfo.Status.valueOf((String) status.getValue()));
		
		// Read log from log file
    File logfile = this.getLogFile(exobj.getID());
    if(logfile.exists()) {
      try {
        info.setLog(FileUtils.readFileToString(logfile));
      } catch (IOException e) {
      }
    }
    else {
      KBObject log = this.kb.getPropertyValue(exobj, dataPropMap.get("hasLog"));    
      if (log != null && log.getValue() != null)
        info.setLog((String) log.getValue());
    }
    
		if(batchok)
		  this.stop_batch_operation();
		this.end();
		return info;
	}
	
	private RuntimePlan setPlanError(RuntimePlan planexe, String message) {
	  planexe.getRuntimeInfo().addLog(message);
	  planexe.getRuntimeInfo().setStatus(RuntimeInfo.Status.FAILURE);
	  return planexe;
	}

  @Override
  public RuntimePlan rePlan(RuntimePlan planexe) {
    WorkflowGenerationAPI wg = new WorkflowGenerationKB(props,
        DataFactory.getReasoningAPI(props), ComponentFactory.getReasoningAPI(props),
        ResourceFactory.getAPI(props), planexe.getID());
    
    TemplateCreationAPI tc = TemplateFactory.getCreationAPI(props);
    Template seedtpl = tc.getTemplate(planexe.getSeededTemplateID());
    
    try {
      Template itpl = wg.getInferredTemplate(seedtpl);
      ArrayList<Template> candidates = wg.specializeTemplates(itpl);
      if(candidates.size() == 0) 
        return this.setPlanError(planexe, 
            "No Specialized templates after planning");
  
      ArrayList<Template> bts = new ArrayList<Template>();
      for(Template t : candidates) {
        // If template has no input data variables, skip
        if(t.getInputDataVariables().length == 0) {
          bts.add(t);
          continue;
        }
        
        VariableBindingsListSet bindingset = wg.selectInputDataObjects(t);
        if(bindingset != null) {
          ArrayList<VariableBindingsList> bindings = 
              CollectionsHelper.combineVariableDataObjectMappings(bindingset);
          for(VariableBindingsList binding : bindings) {
            Template bt = wg.bindTemplate(t, binding);
            if(bt != null)
              bts.add(bt);
          }
        }
      }
      if(bts.size() == 0) 
        return this.setPlanError(planexe, 
            "No Bound templates after planning");
  
      wg.setDataMetricsForInputDataObjects(bts);
  
      ArrayList<Template> cts = new ArrayList<Template>();
      for(Template bt : bts)
        cts.addAll(wg.configureTemplates(bt));
      if(cts.size() == 0)
        return this.setPlanError(planexe, 
            "No Configured templates after planning");
  
      ArrayList<Template> ets = new ArrayList<Template>();
      for(Template ct : cts)
        ets.add(wg.getExpandedTemplate(ct));
      if(ets.size() == 0)
        return this.setPlanError(planexe, 
            "No Expanded templates after planning");
  
      // TODO: Should show all options to the user. Picking the top one for now
      Template xtpl = ets.get(0);
      xtpl.autoLayout();
      
      String xpid = planexe.getExpandedTemplateID();
  
      // Delete the existing expanded template
      this.deleteGraph(xpid);
      // Save the new expanded template
      if (!xtpl.saveAs(xpid)) {
        return this.setPlanError(planexe, 
            "Could not save new Expanded template");
      }
      xtpl = tc.getTemplate(xpid);
  
      String ppid = planexe.getPlan().getID();
      ExecutionPlan newplan = wg.getExecutionPlan(xtpl);
      if(newplan != null) {
        // Delete the existing plan
        this.deleteGraph(ppid);
        // Save the new plan
        if(!newplan.saveAs(ppid)) {
          return this.setPlanError(planexe, 
              "Could not save new Plan");
        }
        newplan.setID(ppid);
  
        // Get the new runtime plan
        RuntimePlan newexe = new RuntimePlan(newplan);
  
        // Update the current plan executable with the new plan
        planexe.setPlan(newplan);
  
        // Hash steps from current queue
        HashMap<String, RuntimeStep>
        stepMap = new HashMap<String, RuntimeStep>();
        for(RuntimeStep step : planexe.getQueue().getAllSteps())
          stepMap.put(step.getID(), step);
  
        // Add new steps to the current queue
        boolean newsteps = false;
        for(RuntimeStep newstep : newexe.getQueue().getAllSteps()) {
          // Add steps not already in current queue
          if(!stepMap.containsKey(newstep.getID())) {
            newsteps = true;
  
            // Set runtime plan
            newstep.setRuntimePlan(planexe);
  
            // Set parents
            @SuppressWarnings("unchecked")
            ArrayList<RuntimeStep> parents = 
            (ArrayList<RuntimeStep>) newstep.getParents().clone();
            newstep.getParents().clear();
            for(RuntimeStep pstep : parents) {
              if(stepMap.containsKey(pstep.getID()))
                pstep = stepMap.get(pstep.getID());
              newstep.addParent(pstep);
            }
  
            // Add new step to queue
            planexe.getQueue().addStep(newstep);
          }
        }
  
        if(newsteps)
          return planexe;
        else {
          return this.setPlanError(planexe, 
              "No new steps in the new execution plan");
        }
      }
      else {
        return this.setPlanError(planexe, 
            "Could not get a new Execution Plan");
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      return this.setPlanError(planexe, e.getMessage());
    }
  }
}
