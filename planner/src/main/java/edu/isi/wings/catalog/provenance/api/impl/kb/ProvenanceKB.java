/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.isi.wings.catalog.provenance.api.impl.kb;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.common.kb.KBUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public class ProvenanceKB extends TransactionsJena implements ProvenanceAPI {

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

  public ProvenanceKB(Properties props) {
    this.liburl = props.getProperty("lib.provenance.url");
    this.libns = this.liburl + "#";

    this.tdbRepository = props.getProperty("tdb.repository.dir");
    if (tdbRepository == null) {
      this.ontologyFactory = new OntFactory(OntFactory.JENA);
    } else {
      this.ontologyFactory =
        new OntFactory(OntFactory.JENA, this.tdbRepository);
    }
    this.viewerid = props.getProperty("viewer.id");

    KBUtils.createLocationMappings(props, this.ontologyFactory);

    this.initializeAPI();
  }

  protected void initializeAPI() {
    try {
      this.kb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
      this.kb.importFrom(
          this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, false, true)
        );
      this.libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN, true);
      this.initializeMaps();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initializeMaps() {
    this.propmap = new HashMap<String, KBObject>();
    this.cmap = new HashMap<String, KBObject>();

    this.start_read();

    String[] props = new String[] {
      "wasGeneratedBy",
      "wasAttributedTo",
      "startedAtTime",
      "endedAtTime",
    };
    String[] concepts = new String[] { "Agent", "Entity", "Activity" };
    for (String propid : props) this.propmap.put(
        propid,
        this.kb.getProperty(this.ontns + propid)
      );
    for (String conceptid : concepts) this.cmap.put(
        conceptid,
        this.kb.getConcept(this.ontns + conceptid)
      );

    this.end();
  }

  @Override
  public Provenance getProvenance(String objectId) {
    try {
      this.start_read();
      Provenance prov = new Provenance(objectId);
      KBObject objres = this.kb.getIndividual(objectId);
      if (objres == null) return prov;

      ArrayList<KBObject> actobjs =
        this.kb.getPropertyValues(objres, propmap.get("wasGeneratedBy"));
      for (KBObject actobj : actobjs) {
        ProvActivity activity = this.getActivity(actobj);
        activity.setObjectId(objectId);
        prov.addActivity(activity);
      }
      return prov;
    } finally {
      this.end();
    }
  }

  @Override
  public boolean setProvenance(Provenance prov) {
    this.start_write();
    KBObject objres = this.libkb.getIndividual(prov.getObjectId());
    if (objres != null) this.removeAllProvenance(prov.getObjectId());

    objres =
      this.libkb.createObjectOfClass(prov.getObjectId(), cmap.get("Entity"));
    if (objres == null) return false;

    for (ProvActivity activity : prov.getActivities()) {
      activity.setObjectId(prov.getObjectId());
      KBObject actobj = this.addActivity(activity);
      if (actobj != null) this.libkb.addPropertyValue(
          objres,
          propmap.get("wasGeneratedBy"),
          actobj
        );
    }
    return this.save() && this.end();
  }

  @Override
  public boolean addProvenance(Provenance prov) {
    this.start_write();
    KBObject objres = this.libkb.getIndividual(prov.getObjectId());
    if (objres == null) objres =
      this.libkb.createObjectOfClass(prov.getObjectId(), cmap.get("Entity"));

    if (objres == null) return false;

    for (ProvActivity activity : prov.getActivities()) {
      activity.setObjectId(prov.getObjectId());
      KBObject actobj = this.addActivity(activity);
      if (actobj != null) this.libkb.addPropertyValue(
          objres,
          propmap.get("wasGeneratedBy"),
          actobj
        );
    }
    return this.save() && this.end();
  }

  @Override
  public boolean removeProvenance(Provenance prov) {
    this.start_write();
    KBObject objres = this.libkb.getIndividual(prov.getObjectId());
    for (ProvActivity act : prov.getActivities()) {
      KBObject actobj = this.libkb.getIndividual(act.getId());
      if (!this.removeActivity(actobj)) return false;
      this.libkb.removeTriple(objres, propmap.get("wasGeneratedBy"), actobj);
    }
    return this.save() && this.end();
  }

  @Override
  public boolean removeAllProvenance(String objectId) {
    this.start_write();
    KBObject objres = this.libkb.getIndividual(objectId);
    ArrayList<KBObject> actobjs =
      this.kb.getPropertyValues(objres, propmap.get("wasGeneratedBy"));
    for (KBObject actobj : actobjs) {
      if (!this.removeActivity(actobj)) return false;
    }
    KBUtils.removeAllTriplesWith(this.libkb, objectId, false);
    return this.save() && this.end();
  }

  @Override
  public boolean removeAllDomainProvenance(String domainURL) {
    this.start_read();
    ArrayList<KBObject> objects =
      this.kb.getInstancesOfClass(cmap.get("Entity"), true);
    this.end();
    for (KBObject objres : objects) {
      if (objres.getID().startsWith(domainURL)) {
        this.removeAllProvenance(objres.getID());
      }
    }
    return true;
  }

  @Override
  public boolean removeUser(String userId) {
    this.start_write();
    KBUtils.removeAllTriplesWith(this.libkb, this.libns + userId, false);
    return this.save() && this.end();
  }

  @Override
  public boolean renameAllDomainProvenance(
    String oldDomainURL,
    String newDomainURL
  ) {
    this.start_write();
    KBUtils.renameTriplesWithPrefix(this.libkb, oldDomainURL, newDomainURL);
    return this.save() && this.end();
  }

  @Override
  public ArrayList<ProvActivity> getAllUserActivities(String userId) {
    ArrayList<ProvActivity> acts = new ArrayList<ProvActivity>();
    this.start_read();
    KBObject agentobj = this.kb.getIndividual(this.libns + userId);
    if (agentobj != null) {
      for (KBTriple t : this.kb.genericTripleQuery(
          null,
          propmap.get("wasAttributedTo"),
          agentobj
        )) {
        KBObject actobj = t.getSubject();
        ProvActivity act = this.getActivity(actobj);
        for (KBTriple t2 : this.kb.genericTripleQuery(
            null,
            propmap.get("wasGeneratedBy"),
            actobj
          )) {
          act.setObjectId(t2.getSubject().getID());
        }
        acts.add(act);
      }
    }
    this.end();
    return acts;
  }

  @Override
  public boolean save() {
    return this.save(this.libkb);
  }

  @Override
  public boolean delete() {
    return (
      this.start_write() && this.libkb.delete() && this.save() && this.end()
    );
  }

  private KBObject addActivity(ProvActivity activity) {
    activity.setId(this.libns + UuidGen.generateAUuid("Act"));
    activity.setUserId(this.libns + this.viewerid);

    KBObject actobj =
      this.libkb.createObjectOfClass(activity.getId(), cmap.get("Activity"));
    if (actobj == null) return null;

    KBObject userobj = this.libkb.getIndividual(activity.getUserId());
    if (userobj == null) userobj =
      this.libkb.createObjectOfClass(activity.getUserId(), cmap.get("Agent"));
    if (userobj == null) return null;
    this.libkb.setPropertyValue(
        actobj,
        propmap.get("wasAttributedTo"),
        userobj
      );

    KBObject timeobj = this.libkb.createLiteral(new Date());
    this.libkb.setPropertyValue(actobj, propmap.get("startedAtTime"), timeobj);
    this.libkb.setPropertyValue(actobj, propmap.get("endedAtTime"), timeobj);

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
    KBObject timeobj =
      this.kb.getPropertyValue(actobj, propmap.get("startedAtTime"));
    if (timeobj != null) activity.setTime(
      ((Date) timeobj.getValue()).getTime()
    );
    KBObject userobj =
      this.kb.getPropertyValue(actobj, propmap.get("wasAttributedTo"));
    if (userobj != null) activity.setUserId(userobj.getID());
    return activity;
  }
}
