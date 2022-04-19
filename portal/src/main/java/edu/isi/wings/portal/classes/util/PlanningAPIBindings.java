package edu.isi.wings.portal.classes.util;

import java.util.Properties;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.data.api.DataReasoningAPI;
import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.planner.api.WorkflowGenerationAPI;
import edu.isi.wings.planner.api.impl.kb.WorkflowGenerationKB;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;

public class PlanningAPIBindings {
  Properties props;
  
  public TemplateCreationAPI tc;
  public ComponentReasoningAPI cc;
  public ComponentCreationAPI ccc;
  public DataReasoningAPI dc;
  public DataCreationAPI dcc;
  public ResourceAPI rc;
  public WorkflowGenerationAPI wg;
  
  public PlanningAPIBindings(Properties props) {
    this.tc = TemplateFactory.getCreationAPI(props);
    this.cc = ComponentFactory.getReasoningAPI(props);
    this.ccc = ComponentFactory.getCreationAPI(props, true);
    this.dc = DataFactory.getReasoningAPI(props);
    this.dcc = DataFactory.getCreationAPI(props);
    this.rc = ResourceFactory.getAPI(props);
    this.wg = new WorkflowGenerationKB(props, dc, dcc, cc, ccc, rc, UuidGen.generateAUuid(""));
  }

}
