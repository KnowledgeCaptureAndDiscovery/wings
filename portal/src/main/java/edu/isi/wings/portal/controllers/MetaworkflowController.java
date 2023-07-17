package edu.isi.wings.portal.controllers;

import com.google.gson.Gson;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimeInfo.Status;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.config.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class MetaworkflowController {

  public Config config;

  Gson json;
  Properties props;
  TemplateController tc;
  RunController rc;
  public String templateUrl;

  public MetaworkflowController(Config config) {
    this.config = config;
    json = JsonHandler.createGson();
    this.props = config.getProperties();
    this.tc = new TemplateController(config);
    this.rc = new RunController(config);
    this.templateUrl = config.getUserDomainUrl() + "/workflows";
  }

  public String getMetaworkflowsListJSON() {
    ArrayList<String> list = new ArrayList<String>();
    for (String tid : this.tc.tc.getTemplateList()) {
      if (tid.contains("#Meta")) {
        list.add(tid);
      }
    }
    return json.toJson(list);
  }

  public String getWorkflowRunsJSON() {
    ArrayList<String> runlist = new ArrayList<String>();
    ArrayList<HashMap<String, Object>> list =
      this.rc.getRunListSimple(null, null, -1, -1, null);
    for (HashMap<String, Object> map : list) {
      String id = (String) map.get("id");
      String tid = (String) map.get("template_id");
      if (tid.contains("#Meta")) continue;
      RuntimeInfo info = (RuntimeInfo) map.get("runtimeInfo");
      if (info.getStatus() == Status.SUCCESS) {
        runlist.add(id);
      }
    }
    return json.toJson(runlist);
  }
}
