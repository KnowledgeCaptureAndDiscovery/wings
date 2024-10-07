package edu.isi.wings.portal.classes.util;

import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentInvocation;
import edu.isi.wings.catalog.component.classes.ComponentPacket;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.portal.classes.config.ConfigLoader;
import edu.isi.wings.workflow.plan.PlanFactory;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.classes.ExecutionCode;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.variables.ComponentVariable;
import edu.isi.wings.workflow.template.classes.variables.Variable;
import edu.isi.wings.workflow.template.classes.variables.VariableType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.apache.http.cookie.Cookie;

public class ComponentExecutingThread implements Runnable {

  String cid;
  ConfigLoader config;
  HashMap<String, Binding> role_bindings;
  PlanningAPIBindings api_bindings;
  String callbackUrl;
  Cookie[] callbackCookies;

  public ComponentExecutingThread(
    String cid,
    ConfigLoader config,
    HashMap<String, Binding> role_bindings,
    PlanningAPIBindings api_bindings,
    String callbackUrl,
    Cookie[] callbackCookies
  ) {
    this.cid = cid;
    this.config = config;
    this.role_bindings = role_bindings;
    this.api_bindings = api_bindings;
    this.callbackUrl = callbackUrl;
    this.callbackCookies = callbackCookies;
  }

  private void runExecutionPlan(RuntimePlan rplan) {
    PlanExecutionEngine engine = config.getDomainExecutionEngine();
    // "execute" below is an asynchronous call
    engine.execute(rplan);
    // Save the engine for an abort if needed
    //this.context.setAttribute("plan_" + rplan.getID(), rplan);
    //this.context.setAttribute("engine_" + rplan.getID(), engine);
  }

  private ExecutionPlan getExecutionPlan() {
    Properties props = config.getProperties();
    ExecutionPlan plan = PlanFactory.createExecutionPlan(this.cid, props);

    ExecutionStep step = PlanFactory.createExecutionStep(this.cid, props);
    //step.setMachineIds(n.getMachineIds());

    Component c = this.api_bindings.ccc.getComponent(this.cid, false);
    ComponentVariable cv = new ComponentVariable(c.getID());
    cv.setBinding(new Binding(c.getID()));

    LinkedHashMap<Role, Variable> roleMap = new LinkedHashMap<Role, Variable>();
    for (String roleid : this.role_bindings.keySet()) {
      Role r = new Role(roleid + "_role");
      r.setRoleId(roleid);
      Binding b = this.role_bindings.get(roleid);
      Variable v = new Variable(
        roleid,
        (b.isValueBinding() ? VariableType.PARAM : VariableType.DATA)
      );
      v.setBinding(b);
      roleMap.put(r, v);
    }
    ComponentPacket mapsComponentDetails = new ComponentPacket(
      cv,
      roleMap,
      new ArrayList<KBTriple>()
    );
    ComponentInvocation invocation =
      this.api_bindings.cc.getComponentInvocation(mapsComponentDetails);

    if (invocation == null) {
      System.err.println("Cannot create invocation for " + cv.getBinding());
      return null;
    }

    ExecutionCode code = new ExecutionCode(invocation.getComponentId());
    code.setLocation(invocation.getComponentLocation());
    code.setCodeDirectory(invocation.getComponentDirectory());
    step.setCodeBinding(code);

    HashMap<String, ArrayList<Object>> argMaps = new HashMap<
      String,
      ArrayList<Object>
    >();
    for (ComponentInvocation.Argument arg : invocation.getArguments()) {
      ArrayList<Object> cur = argMaps.get(arg.getName());
      if (cur == null) cur = new ArrayList<Object>();
      if (arg.getValue() instanceof Binding) {
        Binding b = (Binding) arg.getValue();
        String varid = arg.getVariableid();
        ExecutionFile file = new ExecutionFile(varid);

        String location = b.getLocation();
        file.setLocation(location);
        file.setBinding(b.getName());
        if (arg.isInput()) step.addInputFile(file); else step.addOutputFile(
          file
        );
        cur.add(file);
      } else {
        cur.add(arg.getValue().toString());
      }
      argMaps.put(arg.getName(), cur);
    }
    step.setInvocationArguments(argMaps);
    plan.addExecutionStep(step);
    return plan;
  }

  @Override
  public void run() {
    try {
      ExecutionPlan plan = this.getExecutionPlan();
      if (plan != null) {
        RuntimePlan rplan = new RuntimePlan(plan);
        // TODO: Add Callback
        rplan.setCallbackUrl(this.callbackUrl);
        rplan.setCallbackCookies(this.callbackCookies);
        this.runExecutionPlan(rplan);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
