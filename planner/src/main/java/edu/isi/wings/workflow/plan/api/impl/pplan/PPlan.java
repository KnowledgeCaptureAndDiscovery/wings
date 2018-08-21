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

package edu.isi.wings.workflow.plan.api.impl.pplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionCode;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;

public class PPlan extends URIEntity 
implements ExecutionPlan, TransactionsAPI {
  private static final long serialVersionUID = 1L;

  transient Properties props;
  OntFactory ontologyFactory;
  boolean incomplete;
  
  TransactionsJena transaction;
  
  ArrayList<ExecutionStep> steps;

  public PPlan(String id, Properties props) {
    this(id, props, false);
  }

  public PPlan(String id, Properties props, boolean load) {
    super(id);
    this.props = props;
    steps = new ArrayList<ExecutionStep>();

    if (load) {
      try {
        this.loadFromKB();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public String serialize() {
    return this.toRDF();
  }

  @Override
  public void addExecutionStep(ExecutionStep step) {
    this.steps.add(step);
  }

  @Override
  public ArrayList<ExecutionStep> getAllExecutionSteps() {
    return this.steps;
  }

  private String toRDF() {
    try {
      return this.getKBModel().toAbbrevRdf(false);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private void loadFromKB() throws Exception {
    String wfinst = "http://purl.org/net/wf-invocation";
    String pplan = "http://purl.org/net/p-plan";
    String wfont = this.props.getProperty("ont.workflow.url");

    String tdbRepository = this.props.getProperty("tdb.repository.dir");
    if (tdbRepository == null) {
      this.ontologyFactory = new OntFactory(OntFactory.JENA);
    } else {
      this.ontologyFactory = new OntFactory(OntFactory.JENA, tdbRepository);
    }
    transaction = new TransactionsJena(this.ontologyFactory);
    
    KBUtils.createLocationMappings(this.props, this.ontologyFactory);

    KBAPI kb = this.ontologyFactory.getKB(this.getURL(), OntSpec.PLAIN);
    kb.importFrom(this.ontologyFactory.getKB(wfinst, OntSpec.PLAIN, true));
    kb.importFrom(this.ontologyFactory.getKB(pplan, OntSpec.PLAIN, true));
    kb.importFrom(this.ontologyFactory.getKB(wfont, OntSpec.PLAIN, true, true));

    this.start_read();
    
    KBObject stepcls = kb.getConcept(wfinst + "#Step");
    KBObject varcls = kb.getConcept(wfinst + "#Variable");
    KBObject plancls = kb.getConcept(pplan + "#Plan");

    KBObject isstepofplanprop = kb.getProperty(pplan + "#isStepOfPlan");
    KBObject isvarofplanprop = kb.getProperty(pplan + "#isVariableOfPlan");
    KBObject dbindingprop = kb.getProperty(wfinst + "#hasDataBinding");
    KBObject invarprop = kb.getProperty(pplan + "#hasInputVar");
    KBObject invlineprop = kb.getProperty(pplan + "#hasInvocationLine");
    KBObject outvarprop = kb.getProperty(pplan + "#hasOutputVar");
    KBObject cbindingprop = kb.getProperty(wfinst + "#hasCodeBinding");
    KBObject canrunonprop = kb.getProperty(wfont + "#canRunOn");
    KBObject cdataprop = kb.getProperty(wfont + "#hasCustomData");

    for (KBObject pobj : kb.getInstancesOfClass(plancls, true)) {
      this.setID(pobj.getID());
    }
    HashMap<String, ExecutionFile> varmaps = new HashMap<String, ExecutionFile>();
    for (KBObject vobj : kb.getInstancesOfClass(varcls, true)) {
      KBObject pobj = kb.getPropertyValue(vobj, isvarofplanprop);
      if (pobj.getID().equals(this.getID())) {
        ExecutionFile file = new ExecutionFile(vobj.getID());
        KBObject bobj = kb.getPropertyValue(vobj, dbindingprop);
        file.setLocation((String) bobj.getValue());
        varmaps.put(vobj.getID(), file);
      }
    }
    for (KBObject sobj : kb.getInstancesOfClass(stepcls, true)) {
      KBObject pobj = kb.getPropertyValue(sobj, isstepofplanprop);
      if (pobj.getID().equals(this.getID())) {
        ExecutionStep step = new PPlanStep(sobj.getID(), this.props);
        for (KBObject invar : kb.getPropertyValues(sobj, invarprop))
          step.addInputFile(varmaps.get(invar.getID()));
        for (KBObject outvar : kb.getPropertyValues(sobj, outvarprop))
          step.addOutputFile(varmaps.get(outvar.getID()));
        
        ArrayList<String> machineIds = new ArrayList<String>();
        for (KBObject mvar : kb.getPropertyValues(sobj, canrunonprop))
          machineIds.add(mvar.getID());
        step.setMachineIds(machineIds);

        ExecutionCode code = new ExecutionCode(sobj.getID());
        KBObject cobj = kb.getPropertyValue(sobj, cbindingprop);
        if (cobj != null)
          code.setLocation((String) cobj.getValue());
        step.setCodeBinding(code);

        KBObject cdata = kb.getPropertyValue(sobj, cdataprop);
        if (cdata != null) {
          HashMap<String, String> keyvals = new HashMap<String, String>();
          String customdata = ((String) cdata.getValue());
          String[] lines = customdata.split("\\n");
          for(String line : lines) {
            String[] kval = line.split("=");
            keyvals.put(kval[0], kval[1]);
          }
          if(keyvals.containsKey("CodeDirectory"))
            code.setCodeDirectory(keyvals.get("CodeDirectory"));
        }

        KBObject invline = kb.getPropertyValue(sobj, invlineprop);
        @SuppressWarnings("unused")
        String invocationline = (String) invline.getValue();
        // step.setInvocationArguments(invocationline);

        this.addExecutionStep(step);
      }
    }
    
    this.end();
  }

  private KBAPI getKBModel() throws Exception {
    String wfinst = "http://purl.org/net/wf-invocation";
    String pplan = "http://purl.org/net/p-plan";
    String wfont = this.props.getProperty("ont.workflow.url");

    String tdbRepository = this.props.getProperty("tdb.repository.dir");
    if (tdbRepository == null) {
      this.ontologyFactory = new OntFactory(OntFactory.JENA);
    } else {
      this.ontologyFactory = new OntFactory(OntFactory.JENA, tdbRepository);
    }
    this.transaction = new TransactionsJena(this.ontologyFactory);
    
    KBUtils.createLocationMappings(this.props, this.ontologyFactory);

    KBAPI kb = this.ontologyFactory.getKB(OntSpec.PLAIN);
    KBAPI wfkb = this.ontologyFactory.getKB(wfinst, OntSpec.PLAIN, false, true);
    KBAPI pkb = this.ontologyFactory.getKB(pplan, OntSpec.PLAIN, false, true);
    
    this.start_read();

    KBObject stepcls = wfkb.getConcept(wfinst + "#Step");
    KBObject varcls = wfkb.getConcept(wfinst + "#Variable");
    KBObject plancls = pkb.getConcept(pplan + "#Plan");

    KBObject isstepofplanprop = pkb.getProperty(pplan + "#isStepOfPlan");
    KBObject isvarofplanprop = pkb.getProperty(pplan + "#isVariableOfPlan");
    KBObject dbindingprop = wfkb.getProperty(wfinst + "#hasDataBinding");
    KBObject invarprop = pkb.getProperty(pplan + "#hasInputVar");
    KBObject invlineprop = pkb.getProperty(pplan + "#hasInvocationLine");
    KBObject outvarprop = pkb.getProperty(pplan + "#hasOutputVar");
    KBObject cbindingprop = wfkb.getProperty(wfinst + "#hasCodeBinding");
    // KBObject outvarprop = pkb.getProperty(pplan+"#isOutputVarOf");
    
    this.end();

    KBObject canrunonprop = kb.getProperty(wfont + "#canRunOn");
    KBObject cdataprop = kb.getProperty(wfont + "#hasCustomData");

    KBObject planobj = kb.createObjectOfClass(this.getID(), plancls);

    HashMap<String, KBObject> fileObjects = new HashMap<String, KBObject>();
    for (ExecutionStep step : steps) {
      KBObject stepobj = kb.createObjectOfClass(step.getID(), stepcls);
      kb.setPropertyValue(stepobj, isstepofplanprop, planobj);
      if (step.getCodeBinding().getLocation() != null)
        kb.setPropertyValue(stepobj, cbindingprop,
            kb.createLiteral(step.getCodeBinding().getLocation()));
      if (step.getCodeBinding().getCodeDirectory() != null)
        kb.setPropertyValue(stepobj, cdataprop,
            kb.createLiteral("CodeDirectory="+step.getCodeBinding().getCodeDirectory()));
      String invocationLine = step.getInvocationArgumentString();
      kb.setPropertyValue(stepobj, invlineprop,
          kb.createLiteral(invocationLine));

      for (ExecutionFile f : step.getInputFiles()) {
        KBObject varobj = fileObjects.get(f.getID());
        if (varobj == null) {
          varobj = kb.createObjectOfClass(f.getID(), varcls);
          kb.setPropertyValue(varobj, isvarofplanprop, planobj);
          kb.setPropertyValue(varobj, dbindingprop,
              kb.createLiteral(f.getLocation()));
          fileObjects.put(f.getID(), varobj);
        }
        kb.addPropertyValue(stepobj, invarprop, varobj);
      }
      for (ExecutionFile f : step.getOutputFiles()) {
        KBObject varobj = fileObjects.get(f.getID());
        if (varobj == null) {
          varobj = kb.createObjectOfClass(f.getID(), varcls);
          kb.setPropertyValue(varobj, isvarofplanprop, planobj);
          kb.setPropertyValue(varobj, dbindingprop,
              kb.createLiteral(f.getLocation()));
          fileObjects.put(f.getID(), varobj);
        }
        kb.addPropertyValue(stepobj, outvarprop, varobj);
      }
      
      for(String mid : step.getMachineIds()) {
        kb.addTriple(stepobj, canrunonprop, kb.getResource(mid));
      }
    }
    
    return kb;
  }

  @Override
  public boolean save() {
    try {
      // Write to temporary KB
      KBAPI tkb = this.getKBModel();
      
      // Write to triple store;
      this.start_write();
      ontologyFactory.useTripleStore(tkb);      
      return tkb.saveAs(this.getURL());
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
  public boolean saveAs(String newid) {
    try {
      // Save to temporary KB
      KBAPI tkb = this.getKBModel();
      
      // Rename namespaces
      String curns = this.getNamespace();
      this.setID(newid); // Set new ID
      String newns = this.getNamespace();
      KBUtils.renameTripleNamespace(tkb, curns, newns);
      
      // Write to triple store;
      this.start_write();
      ontologyFactory.useTripleStore(tkb);      
      return tkb.saveAs(this.getURL());
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      this.end();
    }
    return false;
  }

  @Override
  public boolean isIncomplete() {
    return this.incomplete;
  }

  @Override
  public void setIsIncomplete(boolean incomplete) {
    this.incomplete = incomplete;
  }

  // TransactionsAPI functions
  @Override
  public boolean start_read() {
    return transaction.start_read();
  }

  @Override
  public boolean start_write() {
    return transaction.start_write();
  }

  @Override
  public boolean saveAll() {
    return transaction.saveAll();
  }
  
  @Override
  public boolean save(KBAPI kb) {
    return transaction.save(kb);
  }

  @Override
  public boolean end() {
    return transaction.end();
  }

  @Override
  public boolean start_batch_operation() {
    return transaction.start_batch_operation();
  }

  @Override
  public void stop_batch_operation() {
    transaction.stop_batch_operation();
  }
}
