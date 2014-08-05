package edu.isi.wings.execution.tools.api.impl.kb;

import java.util.Properties;

import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;

public class ExecutionResourceKB implements ExecutionResourceAPI {
  ResourceAPI api;
  
  public ExecutionResourceKB(Properties props) {
    this.api = ResourceFactory.getAPI(props);
  }
  
  @Override
  public Machine getMachine(String machineId) {
    return this.api.getMachine(machineId);
  }

}
