package edu.isi.wings.portal.classes.util;

import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.VariableBindingsListSet;
import edu.isi.wings.common.CollectionsHelper;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.portal.classes.config.ConfigLoader;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.ValueBinding;
import edu.isi.wings.workflow.template.classes.variables.ComponentVariable;
import edu.isi.wings.workflow.template.classes.variables.Variable;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import javax.servlet.ServletContext;

class ExecutionThread implements Runnable {

  RuntimePlan rplan;
  ConfigLoader config;
  ServletContext context;

  public ExecutionThread(
    RuntimePlan rplan,
    ConfigLoader config,
    ServletContext context
  ) {
    this.rplan = rplan;
    this.config = config;
    this.context = context;
  }

  @Override
  public void run() {
    PlanExecutionEngine engine = config.getDomainExecutionEngine();

    // Save the engine for an abort if needed
    this.context.setAttribute("plan_" + rplan.getID(), rplan);
    this.context.setAttribute("engine_" + rplan.getID(), engine);

    // Set callback thread to be this thread
    synchronized (this) {
      rplan.setCallbackThread(this);

      // This is an asynchronous call.. So we make it synchronous by waiting until the execution is complete
      engine.execute(rplan);

      // Wait until notified of completion
      try {
        //System.out.println("Waiting for thread: " + this);
        this.wait();
        //System.out.println("Execution finished");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}

public class PlanningAndExecutingThread implements Runnable {

  String ex_prefix;
  String template_id;
  ConfigLoader config;
  TemplateBindings template_bindings;
  PlanningAPIBindings api_bindings;
  ExecutorService executor;
  int max_number_of_executions;
  ServletContext context;
  ArrayList<String> runids;

  public PlanningAndExecutingThread(
    String ex_prefix,
    String template_id,
    ConfigLoader config,
    int max_number_of_executions,
    TemplateBindings template_bindings,
    PlanningAPIBindings api_bindings,
    ExecutorService executor,
    ServletContext context
  ) {
    this.config = config;
    this.template_bindings = template_bindings;
    this.api_bindings = api_bindings;
    this.max_number_of_executions = max_number_of_executions;
    this.executor = executor;
    this.ex_prefix = ex_prefix;
    this.template_id = template_id;
    this.context = context;
    this.runids = new ArrayList<String>();
  }

  private void addTemplateBindings(Template tpl, TemplateBindings tb) {
    // Set data bindings
    for (String key : tb.getDataBindings().keySet()) {
      Binding b = new Binding();
      ArrayList<String> list = tb.getDataBindings().get(key);
      if (list.size() == 0) continue;
      if (list.size() == 1) b.setID(list.get(0)); else {
        for (String bid : list) {
          b.add(new Binding(bid));
        }
      }
      Variable var = tpl.getVariable(key);
      if (var != null) var.setBinding(b);
    }

    // Set parameter bindings
    for (String key : tb.getParameterBindings().keySet()) {
      Binding b = new Binding();
      ArrayList<Object> list = tb.getParameterBindings().get(key);
      String datatype = tb.getParameterTypes().get(key);
      if (datatype == null) continue;
      if (list.size() == 0) continue;
      if (list.size() == 1) {
        b = new ValueBinding(list.get(0), datatype);
      } else {
        for (Object value : list) {
          b.add(new ValueBinding(value, datatype));
        }
      }
      Variable var = tpl.getVariable(key);
      if (var != null) var.setBinding(b);
    }

    // Set component bindings
    for (String key : tb.getComponentBindings().keySet()) {
      String cid = tb.getComponentBindings().get(key);
      Binding b = new Binding(cid);
      ComponentVariable cv = tpl.getComponentVariable(key);
      if (cv != null) cv.setBinding(b);
    }
  }

  private ArrayList<Template> getExpandedTemplates(Template seedtpl) {
    ArrayList<Template> candidates = new ArrayList<Template>();
    if (!config.portalConfig.getPlannerConfig().useSpecialization()) {
      candidates.add(seedtpl);
    } else {
      Template itpl = api_bindings.wg.getInferredTemplate(seedtpl);
      candidates = api_bindings.wg.specializeTemplates(itpl);
    }

    ArrayList<Template> bts = new ArrayList<Template>();

    if (!config.portalConfig.getPlannerConfig().useDataValidation()) bts = candidates; else {
      for (Template t : candidates) {
        // If template has no input data variables, skip
        if (t.getInputDataVariables().length == 0) {
          bts.add(t);
          continue;
        }

        VariableBindingsListSet bindingset =
          api_bindings.wg.selectInputDataObjects(t);
        if (bindingset == null) continue;

        ArrayList<VariableBindingsList> bindings =
          CollectionsHelper.combineVariableDataObjectMappings(bindingset);
        for (VariableBindingsList binding : bindings) {
          Template bt = api_bindings.wg.bindTemplate(t, binding);
          if (bt != null) bts.add(bt);
        }
      }
    }

    api_bindings.wg.setDataMetricsForInputDataObjects(bts);

    ArrayList<Template> cts = new ArrayList<Template>();
    for (Template bt : bts) cts.addAll(api_bindings.wg.configureTemplates(bt));

    ArrayList<Template> ets = new ArrayList<Template>();
    for (Template ct : cts) ets.add(api_bindings.wg.getExpandedTemplate(ct));

    return ets;
  }

  private void runExecutionPlan(RuntimePlan rplan) {
    ExecutionThread exthread = new ExecutionThread(
      rplan,
      this.config,
      this.context
    );
    // This is an asynchronous call
    executor.submit(exthread);
  }

  @Override
  public void run() {
    try {
      String tplid = this.template_bindings.templateId;
      Template seedtpl = api_bindings.tc.getTemplate(tplid);
      this.addTemplateBindings(seedtpl, template_bindings);

      ArrayList<Template> ets = this.getExpandedTemplates(seedtpl);
      if (ets != null) {
        int i = 0;
        for (Template xtpl : ets) {
          i++;
          ExecutionPlan plan = api_bindings.wg.getExecutionPlan(xtpl);

          String seedid = UuidGen.generateURIUuid((URIEntity) seedtpl);
          if (plan != null) {
            // Create a runid
            URIEntity tpluri = new URIEntity(this.template_id);
            tpluri.setID(UuidGen.generateURIUuid(tpluri));
            String runid =
              this.ex_prefix +
              "/" +
              tpluri.getName() +
              ".owl#" +
              tpluri.getName();

            // Save the expanded template, seeded template and plan
            if (xtpl.save() && seedtpl.saveAs(seedid) && plan.save()) {
              RuntimePlan rplan = new RuntimePlan(plan);
              rplan.setID(runid);
              rplan.setExpandedTemplateID(xtpl.getID());
              rplan.setOriginalTemplateID(tplid);
              rplan.setSeededTemplateId(seedid);
              rplan.setCallbackUrl(this.template_bindings.getCallbackUrl());
              rplan.setCallbackCookies(
                this.template_bindings.getCallbackCookies()
              );
              this.runExecutionPlan(rplan);
              this.runids.add(runid);
            }
          }
          if (i >= this.max_number_of_executions) {
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ArrayList<String> getRunids() {
    return runids;
  }

  public void setRunids(ArrayList<String> runids) {
    this.runids = runids;
  }
}
