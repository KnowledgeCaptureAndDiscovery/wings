package edu.isi.wings.workflow.plan.api.impl.pplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionCode;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;

public class PPlan extends URIEntity implements ExecutionPlan {
  private static final long serialVersionUID = 1L;

  transient Properties props;

  boolean incomplete;
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

    OntFactory fac;
    String tdbRepository = this.props.getProperty("tdb.repository.dir");
    if (tdbRepository == null) {
      fac = new OntFactory(OntFactory.JENA);
    } else {
      fac = new OntFactory(OntFactory.JENA, tdbRepository);
    }
    KBUtils.createLocationMappings(this.props, fac);

    KBAPI kb = fac.getKB(this.getURL(), OntSpec.MICRO);
    kb.importFrom(fac.getKB(wfinst, OntSpec.PLAIN, true));
    kb.importFrom(fac.getKB(pplan, OntSpec.PLAIN, true));
    kb.importFrom(fac.getKB(wfont, OntSpec.PLAIN, true, true));

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

        KBObject invline = kb.getPropertyValue(sobj, invlineprop);
        @SuppressWarnings("unused")
        String invocationline = (String) invline.getValue();
        // step.setInvocationArguments(invocationline);

        this.addExecutionStep(step);
      }
    }
  }

  private KBAPI getKBModel() throws Exception {
    String wfinst = "http://purl.org/net/wf-invocation";
    String pplan = "http://purl.org/net/p-plan";
    String wfont = this.props.getProperty("ont.workflow.url");

    OntFactory fac;
    String tdbRepository = this.props.getProperty("tdb.repository.dir");
    if (tdbRepository == null) {
      fac = new OntFactory(OntFactory.JENA);
    } else {
      fac = new OntFactory(OntFactory.JENA, tdbRepository);
    }
    KBUtils.createLocationMappings(this.props, fac);

    KBAPI kb = fac.getKB(OntSpec.PLAIN);
    KBAPI wfkb = fac.getKB(wfinst, OntSpec.PLAIN, false, true);
    KBAPI pkb = fac.getKB(pplan, OntSpec.PLAIN, false, true);

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
    KBObject canrunonprop = kb.getProperty(wfont + "#canRunOn");

    KBObject planobj = kb.createObjectOfClass(this.getID(), plancls);

    HashMap<String, KBObject> fileObjects = new HashMap<String, KBObject>();
    for (ExecutionStep step : steps) {
      KBObject stepobj = kb.createObjectOfClass(step.getID(), stepcls);
      kb.setPropertyValue(stepobj, isstepofplanprop, planobj);
      if (step.getCodeBinding().getLocation() != null)
        kb.setPropertyValue(stepobj, cbindingprop,
            fac.getDataObject(step.getCodeBinding().getLocation()));
      String invocationLine = step.getInvocationArgumentString();
      kb.setPropertyValue(stepobj, invlineprop,
          fac.getDataObject(invocationLine));

      for (ExecutionFile f : step.getInputFiles()) {
        KBObject varobj = fileObjects.get(f.getID());
        if (varobj == null) {
          varobj = kb.createObjectOfClass(f.getID(), varcls);
          kb.setPropertyValue(varobj, isvarofplanprop, planobj);
          kb.setPropertyValue(varobj, dbindingprop,
              fac.getDataObject(f.getLocation()));
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
              fac.getDataObject(f.getLocation()));
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
      KBAPI kb = this.getKBModel();
      return kb.saveAs(this.getURL());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
  public boolean saveAs(String newid) {
    try {
      KBAPI kb = this.getKBModel();
      String curns = this.getNamespace();
      this.setID(newid);
      String newns = this.getNamespace();
      KBUtils.renameTripleNamespace(kb, curns, newns);
      return kb.saveAs(this.getURL());
    } catch (Exception e) {
      e.printStackTrace();
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
}
