package edu.isi.wings.catalog.provenance.api.impl.kb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;

public class ProvenanceKB implements ProvenanceAPI {
  private String libns;
  private String liburl;
  private KBAPI kb;
  private KBAPI libkb;
  private String viewerid;
  
  private String onturl = "http://www.w3.org/ns/prov-o";
  private String ontns = "http://www.w3.org/ns/prov#";
  
  private HashMap<String, KBObject> propmap;
  private HashMap<String, KBObject> cmap;
  
  private String tdbRepository;
  private OntFactory ontologyFactory;
  
  public ProvenanceKB(Properties props) {
    this.liburl = props.getProperty("lib.provenance.url");
    this.libns = this.liburl + "#";

    this.tdbRepository = props.getProperty("tdb.repository.dir");
    if (tdbRepository == null) {
      this.ontologyFactory = new OntFactory(OntFactory.JENA);
    } else {
      this.ontologyFactory = new OntFactory(OntFactory.JENA, this.tdbRepository);
    }
    this.viewerid = props.getProperty("viewer.id");
    
    KBUtils.createLocationMappings(props, this.ontologyFactory);
    
    this.initializeAPI();
  }
  
  protected void initializeAPI() {
    try {
      this.kb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
      this.kb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, false, true));
      this.libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN, true);
      this.initializeMaps();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initializeMaps() {
    this.propmap = new HashMap<String, KBObject>();
    this.cmap = new HashMap<String, KBObject>();

    String[] props = new String[] {"wasGeneratedBy", 
        "wasAttributedTo", "startedAtTime", "endedAtTime" };
    String[] concepts = new String[] {"Agent", "Entity", "Activity"};
    for (String propid : props) 
      this.propmap.put(propid, this.kb.getProperty(this.ontns + propid));
    for (String conceptid : concepts) 
      this.cmap.put(conceptid, this.kb.getConcept(this.ontns + conceptid));
  }
  
  @Override
  public Provenance getProvenance(String objectId) {
    Provenance prov = new Provenance(objectId);
    KBObject objres = this.kb.getIndividual(objectId);
    ArrayList<KBObject> actobjs = 
        this.kb.getPropertyValues(objres, propmap.get("wasGeneratedBy"));
    for(KBObject actobj : actobjs) {
      ProvActivity activity = this.getActivity(actobj);
      activity.setObjectId(objectId);
      prov.addActivity(activity);
    }
    return prov;
  }

  @Override
  public boolean setProvenance(Provenance prov) {
    KBObject objres = this.libkb.getIndividual(prov.getObjectId());
    if(objres != null)
      this.removeAllProvenance(prov.getObjectId());
    
    objres = this.libkb.createObjectOfClass(prov.getObjectId(), cmap.get("Entity"));
    if(objres == null)
      return false;
    
    for(ProvActivity activity: prov.getActivities()) {
      activity.setObjectId(prov.getObjectId());
      KBObject actobj = this.addActivity(activity);
      if(actobj != null)
        this.libkb.addPropertyValue(objres, propmap.get("wasGeneratedBy"), actobj);
    }
    return true;
  }

  @Override
  public boolean addProvenance(Provenance prov) {
    KBObject objres = this.libkb.getIndividual(prov.getObjectId());
    if(objres == null)
      objres = this.libkb.createObjectOfClass(prov.getObjectId(), cmap.get("Entity"));
    
    if(objres == null)
      return false;
    
    for(ProvActivity activity: prov.getActivities()) {
      activity.setObjectId(prov.getObjectId());
      KBObject actobj = this.addActivity(activity);
      if(actobj != null)
        this.libkb.addPropertyValue(objres, propmap.get("wasGeneratedBy"), actobj);
    }
    return true;
  }
  
  @Override
  public boolean removeProvenance(Provenance prov) {
    KBObject objres = this.libkb.getIndividual(prov.getObjectId());
    for(ProvActivity act : prov.getActivities()) {
      KBObject actobj = this.libkb.getIndividual(act.getId());
      if(!this.removeActivity(actobj))
        return false;
      this.libkb.removeTriple(objres, propmap.get("wasGeneratedBy"), actobj);
    }
    return false;
  }

  @Override
  public boolean removeAllProvenance(String objectId) {
    KBObject objres = this.libkb.getIndividual(objectId);
    ArrayList<KBObject> actobjs = 
        this.kb.getPropertyValues(objres, propmap.get("wasGeneratedBy"));
    for(KBObject actobj : actobjs) {
      if(!this.removeActivity(actobj))
        return false;
    }
    KBUtils.removeAllTriplesWith(this.libkb, objectId, false);
    return true;
  }
  
  @Override
  public boolean removeAllDomainProvenance(String domainURL) {
    for(KBObject objres : this.kb.getInstancesOfClass(cmap.get("Entity"), true)) {
      if(objres.getID().startsWith(domainURL)) {
        this.removeAllProvenance(objres.getID());
      }
    }
    return true;
  }
  
  @Override
  public boolean removeUser(String userId) {
    KBUtils.removeAllTriplesWith(this.libkb, this.libns + userId, false);
    return true;
  }
  
  @Override
  public boolean renameAllDomainProvenance(String oldDomainURL, String newDomainURL) {
    KBUtils.renameTriplesWithPrefix(this.libkb, oldDomainURL, newDomainURL);
    return true;
  }
  
  @Override
  public ArrayList<ProvActivity> getAllUserActivities(String userId) {
    ArrayList<ProvActivity> acts = new ArrayList<ProvActivity>();
    KBObject agentobj = this.kb.getIndividual(this.libns + userId);
    if(agentobj != null) {
      for(KBTriple t : this.kb.genericTripleQuery(null, propmap.get("wasAttributedTo"), 
          agentobj)) {
        KBObject actobj = t.getSubject();
        ProvActivity act = this.getActivity(actobj);
        for(KBTriple t2 :  this.kb.genericTripleQuery(null, propmap.get("wasGeneratedBy"), 
            actobj)) {
          act.setObjectId(t2.getSubject().getID());
        }
        acts.add(act);
      }
    }
    return acts;
  }
  
  @Override
  public boolean save() {
    return this.libkb.save();
  }

  @Override
  public boolean delete() {
    return this.libkb.delete();
  }
  
  @Override
  public void end() {
    this.libkb.end();
  }
  
  private KBObject addActivity(ProvActivity activity) {
    activity.setId(this.libns + UuidGen.generateAUuid("Act"));
    activity.setUserId(this.libns + this.viewerid);
    
    KBObject actobj = this.libkb.createObjectOfClass(activity.getId(), 
        cmap.get("Activity"));
    if(actobj == null)
      return null;
    
    KBObject userobj = this.libkb.getIndividual(activity.getUserId());
    if(userobj == null)
      userobj = this.libkb.createObjectOfClass(activity.getUserId(), 
          cmap.get("Agent"));
    if(userobj == null)
      return null;
    this.libkb.setPropertyValue(actobj, propmap.get("wasAttributedTo"), 
        userobj);
    
    Calendar cal = Calendar.getInstance();
    XSDDateTime dtime = new XSDDateTime(cal);
    KBObject timeobj = this.libkb.createLiteral(dtime);
    this.libkb.setPropertyValue(actobj, propmap.get("startedAtTime"), 
        timeobj);
    this.libkb.setPropertyValue(actobj, propmap.get("endedAtTime"), 
        timeobj);
    
    this.libkb.setLabel(actobj, activity.getType());
    this.libkb.setComment(actobj, activity.getLog());
    
    return actobj;
  }
  
  private boolean removeActivity(KBObject actobj) {
    KBUtils.removeAllTriplesWith(this.libkb, actobj.getID(), false);
    return true;
  }
  
  private ProvActivity getActivity(KBObject actobj) {
    ProvActivity activity = new ProvActivity(actobj.getID());
    activity.setType(this.kb.getLabel(actobj));
    activity.setLog(this.kb.getComment(actobj));
    KBObject timeobj = this.kb.getPropertyValue(actobj, propmap.get("startedAtTime"));
    if(timeobj != null)
      activity.setTime(((XSDDateTime)timeobj.getValue()).asCalendar().getTimeInMillis());
    KBObject userobj = this.kb.getPropertyValue(actobj, propmap.get("wasAttributedTo"));
    if(userobj != null)
      activity.setUserId(userobj.getID());
    return activity;
  }

}
