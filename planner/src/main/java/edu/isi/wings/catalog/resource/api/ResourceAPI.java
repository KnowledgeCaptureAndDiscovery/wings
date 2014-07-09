package edu.isi.wings.catalog.resource.api;

import java.util.ArrayList;

import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.catalog.resource.classes.Software;
import edu.isi.wings.catalog.resource.classes.SoftwareEnvironment;
import edu.isi.wings.catalog.resource.classes.SoftwareVersion;

public interface ResourceAPI {
  // Query functions
  ArrayList<String> getMachineIds();

  ArrayList<String> getSoftwareIds();

  ArrayList<String> getSoftwareVersionIds(String softwareid);

  ArrayList<SoftwareVersion> getAllSoftwareVersions();
  
  ArrayList<SoftwareEnvironment> getAllSoftwareEnvironment();
  
  Machine getMachine(String machineid);

  Software getSoftware(String softwareid);

  SoftwareVersion getSoftwareVersion(String versionid);

  // Write functions
  boolean addMachine(String machineid);
  
  boolean addSoftware(String softwareid);
  
  boolean addSoftwareVersion(String versionid, String softwareid);

  boolean removeMachine(String machineid);
  
  boolean removeSoftware(String softwareid);
  
  boolean removeSoftwareVersion(String versionid);
  
  boolean saveMachine(Machine machine);
  
  boolean saveSoftware(Software machine);
  
  boolean saveSoftwareVersion(SoftwareVersion machine);
  
  // Sync/Save
  boolean save();

  void end();

  void delete();
}
