package edu.isi.wings.execution.tools.api;

import edu.isi.wings.catalog.resource.classes.Machine;

public interface ExecutionResourceAPI {

  public void setLocalStorageFolder(String path);
  
  public String getLocalStorageFolder();
  
  public Machine getMachine(String machineId);

}
