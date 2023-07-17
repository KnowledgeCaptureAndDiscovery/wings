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

package edu.isi.wings.workflow.template.api.impl.kb;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.workflow.template.api.ConstraintEngine;
import java.util.ArrayList;

public class ConstraintEngineKB
  extends TransactionsJena
  implements ConstraintEngine {

  KBAPI kb;

  ArrayList<String> blacklistns;
  ArrayList<String> whitelistns; // If set, then blacklistns is ignored

  ArrayList<String> blacklistIds;
  ArrayList<String> allowedIds;

  public ConstraintEngineKB(KBAPI kb, String wflowns) {
    // The ConstraintEngine KB is kept separate from template kb
    this.ontologyFactory = new OntFactory(OntFactory.JENA);
    this.kb = this.ontologyFactory.getKB(OntSpec.PLAIN);
    initializeBlacklistNS(wflowns);
    initializeWhitelistNS();
    initializeAllowedIds(wflowns);
    initBannedIds();
    initKB(kb);
  }

  public ConstraintEngineKB(ConstraintEngineKB engine) {
    this.ontologyFactory = new OntFactory(OntFactory.JENA);
    this.kb = ontologyFactory.getKB(OntSpec.PLAIN);

    if (engine.blacklistns != null) blacklistns =
      new ArrayList<String>(engine.blacklistns);
    if (engine.whitelistns != null) whitelistns =
      new ArrayList<String>(engine.whitelistns);
    if (engine.blacklistIds != null) blacklistIds =
      new ArrayList<String>(engine.blacklistIds);
    if (engine.allowedIds != null) allowedIds =
      new ArrayList<String>(engine.allowedIds);

    // Copy over triples from existing engine
    this.start_write();
    this.kb.addTriples(engine.getConstraints());
    this.save(this.kb);
    this.end();
  }

  private void initKB(KBAPI kb) {
    this.start_write();
    ArrayList<KBTriple> triples = kb.getAllTriples();
    ArrayList<KBTriple> newTriples = new ArrayList<KBTriple>();
    for (KBTriple t : triples) {
      if (
        isBanned(t.getSubject()) ||
        isBanned(t.getPredicate()) ||
        isBanned(t.getObject())
      ) {
        continue;
      }
      newTriples.add(t);
    }
    this.kb.addTriples(newTriples);
    this.save(this.kb);
    this.end();
  }

  private void initializeBlacklistNS(String wflowns) {
    blacklistns = new ArrayList<String>();
    // Filter out statements from workflow namespace by default
    // - Except the workflow constraint properties
    blacklistns.add(wflowns);
    // Also filter out standard namespaces
    blacklistns.add(KBUtils.RDF);
    blacklistns.add(KBUtils.RDFS);
    blacklistns.add(KBUtils.OWL);
    blacklistns.add(KBUtils.XSD);
  }

  private void initializeWhitelistNS() {
    whitelistns = new ArrayList<String>();
  }

  private void initializeAllowedIds(String wflowns) {
    allowedIds = new ArrayList<String>();
    allowedIds.add(KBUtils.RDF + "type");
    allowedIds.add(wflowns + "hasDataBinding");
    allowedIds.add(wflowns + "hasParameterValue");
    allowedIds.add(wflowns + "hasSameDataAs");
    allowedIds.add(wflowns + "hasDifferentDataFrom");
  }

  private void initBannedIds() {
    blacklistIds = new ArrayList<String>();
    /*
     * blacklistIds.add(KBUtils.RDFS + "comment");
     * blacklistIds.add(KBUtils.RDFS + "range");
     * blacklistIds.add(KBUtils.RDFS + "domain");
     * blacklistIds.add(KBUtils.OWL + "topDataProperty");
     * blacklistIds.add(KBUtils.OWL + "bottomDataProperty");
     */
  }

  private boolean isBanned(KBObject item) {
    if (item.isLiteral()) return false;
    if (blacklistIds.contains(item.getID())) return true;
    if (allowedIds.contains(item.getID())) return false;

    if (
      (whitelistns != null) && (whitelistns.contains(item.getNamespace()))
    ) return false;
    if (
      (blacklistns != null) && (blacklistns.contains(item.getNamespace()))
    ) return true;

    return false;
  }

  private boolean isRelevant(KBObject item) {
    ArrayList<KBObject> clses = kb.getAllClassesOfInstance(item, true);
    if (clses == null) {
      // System.err.println(item + " does not have any class !!");
      return false;
    }
    for (KBObject cls : clses) {
      if (
        cls.getID() != null &&
        whitelistns != null &&
        whitelistns.contains(cls.getNamespace())
      ) {
        return true;
      } else if (
        cls.getID() != null && !blacklistns.contains(cls.getNamespace())
      ) {
        return true;
      }
    }
    return false;
  }

  private ArrayList<KBTriple> removeUselessConstraints(
    ArrayList<KBTriple> constraints
  ) {
    // What are useless constraints for us ?
    // - Duplicates
    // - Constraints which have the same subject and object
    // (like subClassOf, equivalentClass entailments)
    ArrayList<String> strconstraints = new ArrayList<String>();
    ArrayList<KBTriple> newconstraints = new ArrayList<KBTriple>();
    for (KBTriple triple : constraints) {
      ArrayList<KBObject> kbos = triple.toArrayList();
      String str = kbos.toString();
      if (!strconstraints.contains(str)) {
        strconstraints.add(str);
        KBObject subj = triple.getSubject();
        KBObject obj = triple.getObject();
        if (obj.isLiteral() || !obj.getID().equals(subj.getID())) {
          newconstraints.add(triple);
        }
      }
    }
    return newconstraints;
  }

  // filterType = 0 : Filter both object and subject for relevance
  // filterType = 1 : Filter only subject for relevance
  // filterType = 2 : Filter only object for relevance
  private ArrayList<KBTriple> getTriplesFor(
    KBObject forsubj,
    KBObject forobj,
    int filterType
  ) {
    this.start_read();
    ArrayList<KBTriple> triples =
      this.kb.genericTripleQuery(forsubj, null, forobj);
    this.end();

    ArrayList<KBTriple> relevantTriples = new ArrayList<KBTriple>();
    for (KBTriple triple : triples) {
      // System.out.println(triple);
      KBObject subj = triple.getSubject();
      KBObject pred = triple.getPredicate();
      KBObject obj = triple.getObject();
      if (
        subj != null &&
        pred != null &&
        obj != null &&
        subj.getID() != null &&
        (obj.getID() != null || obj.isLiteral()) &&
        pred.getID() != null
      ) {
        if (isBanned(subj) || isBanned(pred) || isBanned(obj)) {
          continue;
        }

        relevantTriples.add(triple);
      }
    }
    return removeUselessConstraints(relevantTriples);
  }

  private ArrayList<KBTriple> getConstraintsForId(
    String id,
    ArrayList<String> done
  ) {
    this.start_read();
    boolean batched = this.start_batch_operation();

    KBObject item = this.kb.getResource(id);
    if (done.contains(id) || blacklistIds.contains(id)) {
      return new ArrayList<KBTriple>();
    }
    done.add(id);
    if (item == null) {
      return new ArrayList<KBTriple>();
    }

    ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();

    // Get Triples with item as subject and object

    // TODO: Temporarily just choose subject-based constraints
    // to simplify which constraints are chosen !!

    ArrayList<KBTriple> tmp = this.getTriplesFor(item, null, 2);
    // ArrayList<KBTriple> tmp2 = this.getTriplesFor(null, item, 1,
    // blacklistIds);
    constraints.addAll(tmp);
    // constraints.addAll(tmp2);

    // Have to recursively get constraints for all the objects in tmp
    for (KBTriple triple : tmp) {
      KBObject obj = triple.getObject();
      if (obj != null && obj.getID() != null && isRelevant(obj)) {
        constraints.addAll(getConstraintsForId(obj.getID(), done));
      }
    }

    // Have to recursively get constraints for all the subjects in tmp2
    /*
     * for (KBTriple triple : tmp2) { KBObject subj = triple.getSubject();
     * if (subj != null && subj.getID() != null) {
     * constraints.addAll(getConstraintsForId(subj.getID(), done)); } }
     */

    ArrayList<KBTriple> cleanConstraints =
      this.removeUselessConstraints(constraints);

    if (batched) this.stop_batch_operation();
    this.end();

    return cleanConstraints;
  }

  public ArrayList<KBTriple> getConstraints() {
    return this.getTriplesFor(null, null, 0);
  }

  public ArrayList<KBTriple> getConstraints(String id) {
    return getConstraintsForId(id, new ArrayList<String>());
  }

  public ArrayList<KBTriple> getConstraints(ArrayList<String> ids) {
    ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();
    for (String id : ids) {
      constraints.addAll(this.getConstraints(id));
    }
    return this.removeUselessConstraints(constraints);
  }

  public void setConstraints(ArrayList<KBTriple> constraints) {
    this.start_write();
    // Modify the internal kb to add statements
    this.kb.addTriples(constraints);
    this.save(this.kb);
    this.end();
  }

  public void addConstraints(ArrayList<KBTriple> constraints) {
    this.start_write();
    // Modify the internal kb to add constraints
    // this.constraints.addAll(constraints);
    this.kb.addTriples(constraints);
    this.save(this.kb);
    this.end();
  }

  public void removeConstraint(KBTriple constraint) {
    this.start_write();
    this.kb.removeTriple(constraint);
    this.save(this.kb);
    this.end();
  }

  public void removeObjectAndConstraints(KBObject obj) {
    this.start_write();
    this.kb.deleteObject(obj, true, true);
    this.save(this.kb);
    this.end();
  }

  public void addBlacklistedNamespace(String ns) {
    blacklistns.add(ns);
  }

  public void addBlacklistedId(String id) {
    blacklistIds.add(id);
  }

  public void removeBlacklistedId(String id) {
    blacklistIds.remove(id);
  }

  public void addWhitelistedNamespace(String ns) {
    if (whitelistns == null) {
      whitelistns = new ArrayList<String>();
    }
    whitelistns.add(ns);
  }

  public boolean containsConstraint(KBTriple cons) {
    try {
      this.start_read();
      if (
        this.kb.genericTripleQuery(
            cons.getSubject(),
            cons.getPredicate(),
            cons.getObject()
          ) !=
        null
      ) return true;
      return false;
    } finally {
      this.end();
    }
  }

  public void replaceSubjectInConstraints(KBObject subj, KBObject newSubj) {
    this.start_write();
    for (KBTriple t : this.kb.genericTripleQuery(subj, null, null)) {
      this.kb.removeTriple(t);
      t.setSubject(newSubj);
      this.kb.addTriple(t);
    }
    this.save(this.kb);
    this.end();
  }

  public void replaceObjectInConstraints(KBObject obj, KBObject newObj) {
    this.start_write();
    for (KBTriple t : this.kb.genericTripleQuery(null, null, obj)) {
      this.kb.removeTriple(t);
      t.setObject(newObj);
      this.kb.addTriple(t);
    }
    this.save(this.kb);
    this.end();
  }

  public KBTriple createNewConstraint(
    String subjID,
    String predID,
    String objID
  ) {
    this.start_write();
    KBObject subjkb = kb.getResource(subjID);
    KBObject predkb = kb.getProperty(predID);
    KBObject objkb = kb.getResource(objID);
    if (subjkb != null && predkb != null && objkb != null) {
      return this.kb.addTriple(subjkb, predkb, objkb);
    }
    this.save(this.kb);
    this.end();
    return null;
  }

  public KBTriple createNewDataConstraint(
    String subjID,
    String predID,
    String obj,
    String type
  ) {
    this.start_write();
    KBTriple triple = null;
    KBObject subjkb = kb.getResource(subjID);
    KBObject predkb = kb.getProperty(predID);
    if (subjkb != null && predkb != null) {
      try {
        KBObject objkb = kb.createXSDLiteral(obj, type); // null type is ok
        if (objkb != null) {
          triple = this.kb.addTriple(subjkb, predkb, objkb);
          this.save(this.kb);
        }
      } catch (Exception e) {
        System.err.println(obj + " is not of type " + type);
      }
    }
    this.end();
    return triple;
  }

  public KBObject getResource(String ID) {
    try {
      this.start_read();
      return kb.getResource(ID);
    } finally {
      this.end();
    }
  }

  public String toString() {
    // return this.kb.toN3();
    try {
      this.start_read();
      return this.kb.toRdf(false);
    } finally {
      this.end();
    }
  }

  public void removeBlacklistedNamespace(String ns) {
    blacklistns.remove(ns);
  }

  public void removeWhitelistedNamespace(String ns) {
    whitelistns.remove(ns);
  }
}
