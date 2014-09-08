package edu.isi.wings.catalog.provenance.api;

import java.util.ArrayList;

import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;

public interface ProvenanceAPI {
  public Provenance getProvenance(String objectId);

  public ArrayList<ProvActivity> getAllUserActivities(String userId);
  
  public boolean setProvenance(Provenance prov);
  
  public boolean addProvenance(Provenance prov);

  public boolean removeProvenance(Provenance prov);

  public boolean removeAllProvenance(String objectId);
  
  public boolean removeAllDomainProvenance(String domainURL);
  
  public boolean renameAllDomainProvenance(String oldDomainURL, String newDomainURL);
  
  public boolean removeUser(String userId);
  
  public boolean save();
  
  public boolean delete();
  
  public void end();
}
