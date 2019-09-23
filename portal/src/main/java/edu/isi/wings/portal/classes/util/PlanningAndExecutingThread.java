package edu.isi.wings.portal.classes.util;

import java.util.ArrayList;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.VariableBindingsListSet;
import edu.isi.wings.common.CollectionsHelper;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.ValueBinding;
import edu.isi.wings.workflow.template.classes.variables.ComponentVariable;
import edu.isi.wings.workflow.template.classes.variables.Variable;

public class PlanningAndExecutingThread implements Runnable {
  String runid;
  Config config;
  TemplateBindings template_bindings;
  PlanningAPIBindings api_bindings;
  
  public PlanningAndExecutingThread(
      String runid,
      Config config,
      TemplateBindings template_bindings, 
      PlanningAPIBindings api_bindings) {
    this.runid = runid;
    this.config = config;
    this.template_bindings = template_bindings;
    this.api_bindings = api_bindings;
  }
  
  private void addTemplateBindings(Template tpl, TemplateBindings tb) {
    // Set data bindings
    for(String key : tb.getDataBindings().keySet()) {
      Binding b = new Binding();
      ArrayList<String> list = tb.getDataBindings().get(key);
      if(list.size() == 0)
        continue;
      if(list.size() == 1)
        b.setID(list.get(0));
      else {
        for(String bid : list) {
          b.add(new Binding(bid));
        }
      }
      Variable var = tpl.getVariable(key);
      if(var != null)
        var.setBinding(b);
    }
    
    // Set parameter bindings
    for(String key : tb.getParameterBindings().keySet()) {
      Object value = tb.getParameterBindings().get(key);
      String datatype = tb.getParameterTypes().get(key);
      if(datatype == null)
        continue;
      ValueBinding b = new ValueBinding(value, datatype);
      Variable var = tpl.getVariable(key);
      if(var != null) 
        var.setBinding(b);
    }
    
    // Set component bindings
    for(String key : tb.getComponentBindings().keySet()) {
      String cid = tb.getComponentBindings().get(key);
      Binding b = new Binding(cid);
      ComponentVariable cv = tpl.getComponentVariable(key);
      if(cv != null) 
        cv.setBinding(b);
    }
  }
  
  private ArrayList<Template> getExpandedTemplates(Template seedtpl) {
    
    ArrayList<Template> candidates = new ArrayList<Template>();
    if(!config.getPlannerConfig().useSpecialization()) {
      candidates.add(seedtpl);
    }
    else {
      Template itpl = api_bindings.wg.getInferredTemplate(seedtpl);
      candidates = api_bindings.wg.specializeTemplates(itpl); 
    }
    
    ArrayList<Template> bts = new ArrayList<Template>();
    
    if(!config.getPlannerConfig().useDataValidation())
      bts = candidates;
    else {
      for(Template t : candidates) {
        // If template has no input data variables, skip
        if(t.getInputDataVariables().length == 0) {
          bts.add(t);
          continue;
        }
        
        VariableBindingsListSet bindingset = api_bindings.wg.selectInputDataObjects(t);
        if(bindingset == null)
          continue;

        ArrayList<VariableBindingsList> bindings = 
            CollectionsHelper.combineVariableDataObjectMappings(bindingset);
        for(VariableBindingsList binding : bindings) {
          Template bt = api_bindings.wg.bindTemplate(t, binding);
          if(bt != null)
            bts.add(bt);
        }
      }
    }
        
    api_bindings.wg.setDataMetricsForInputDataObjects(bts);

    ArrayList<Template> cts = new ArrayList<Template>();
    for(Template bt : bts)
      cts.addAll(api_bindings.wg.configureTemplates(bt));
    
    ArrayList<Template> ets = new ArrayList<Template>();
    for(Template ct : cts)
      ets.add(api_bindings.wg.getExpandedTemplate(ct));
    
    return ets;
  }
  
  private void runExecutionPlan(RuntimePlan rplan) {
    PlanExecutionEngine engine = config.getDomainExecutionEngine();
    // "execute" below is an asynchronous call
    engine.execute(rplan);

    // Save the engine for an abort if needed
    //this.context.setAttribute("plan_" + rplan.getID(), rplan);
    //this.context.setAttribute("engine_" + rplan.getID(), engine);
  }
  
  @Override
  public void run() {
    String tplid = this.template_bindings.templateId;
    Template seedtpl = api_bindings.tc.getTemplate(tplid);
    this.addTemplateBindings(seedtpl, template_bindings);
    
    ArrayList<Template> ets = this.getExpandedTemplates(seedtpl);
    if(ets != null && ets.size() > 0) {
      Template xtpl = ets.get(0); // Choose first expanded template
      ExecutionPlan plan = api_bindings.wg.getExecutionPlan(xtpl);
  
      String seedid = UuidGen.generateURIUuid((URIEntity) seedtpl);
      if (plan != null) {
        // Save the expanded template, seeded template and plan
        if (xtpl.save() && seedtpl.saveAs(seedid) && plan.save()) {
          RuntimePlan rplan = new RuntimePlan(plan);
          rplan.setID(this.runid);
          rplan.setExpandedTemplateID(xtpl.getID());
          rplan.setOriginalTemplateID(tplid);
          rplan.setSeededTemplateId(seedid);
          this.runExecutionPlan(rplan);
        }
      }
    }
  }
}