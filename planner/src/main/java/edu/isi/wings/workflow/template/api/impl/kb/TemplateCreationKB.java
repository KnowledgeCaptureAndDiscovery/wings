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
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.SparqlQuerySolution;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;
import edu.isi.wings.workflow.template.classes.ConstraintProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

public class TemplateCreationKB
  extends TransactionsJena
  implements TemplateCreationAPI {

  String ontVersion = "3.0";

  String wflowns;
  String liburl;
  String onturl;
  String wdirurl;
  String dcdomns;
  String dclibns;
  String pcdomns;

  KBAPI kb;
  KBAPI ontkb;
  KBAPI writerkb;
  KBAPI unionkb;
  Properties props;

  public TemplateCreationKB(Properties props) {
    this.props = props;
    this.onturl = props.getProperty("ont.workflow.url");
    this.liburl = props.getProperty("lib.domain.workflow.url");
    this.wdirurl = props.getProperty("domain.workflows.dir.url");
    this.dcdomns = props.getProperty("ont.domain.data.url") + "#";
    this.dclibns = props.getProperty("lib.domain.data.url") + "#";
    this.pcdomns = props.getProperty("ont.domain.component.ns");

    String hash = "#";
    this.wflowns = this.onturl + hash;

    String tdbRepository = (String) props.get("tdb.repository.dir");
    if (tdbRepository == null) {
      this.ontologyFactory = new OntFactory(OntFactory.JENA);
    } else {
      this.ontologyFactory = new OntFactory(OntFactory.JENA, tdbRepository);
    }
    KBUtils.createLocationMappings(props, this.ontologyFactory);

    this.initializeAPI(false);
    this.addMissingVocabulary();
  }

  private void initializeAPI(boolean create_if_empty) {
    try {
      this.kb =
        this.ontologyFactory.getKB(liburl, OntSpec.PELLET, create_if_empty);
      this.ontkb =
        this.ontologyFactory.getKB(
            onturl,
            OntSpec.PLAIN,
            create_if_empty,
            true
          );
      this.kb.importFrom(this.ontkb);

      this.writerkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);

      this.unionkb =
        this.ontologyFactory.getKB("urn:x-arq:UnionGraph", OntSpec.PLAIN);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void addMissingVocabulary() {
    try {
      // Check if the ontology is an older version
      this.start_read();
      KBObject ontobj = this.ontkb.getIndividual(onturl);
      KBObject version =
        this.ontkb.getPropertyValue(
            ontobj,
            this.ontkb.getProperty(KBUtils.OWL + "versionInfo")
          );

      if (version == null || !this.ontVersion.equals(version.getValue())) {
        this.end();
        this.start_write();
        // Older ontologies don't have some concepts & properties. Add them in here
        // FIXME: Just reload from web instead of doing this

        HashMap<String, KBObject> terms = this.getKBTerms();

        String[] dprops = new String[] {
          "hasRoleID",
          "autoFill",
          "breakPoint",
          "isInactive",
          "tellmeData",
        };
        String[] oprops = new String[] { "derivedFrom", "hasMetadata" };
        String[] concepts = new String[] { "ReduceDimensionality", "Shift" };

        for (String pname : dprops) if (
          !terms.containsKey(this.wflowns + pname)
        ) this.ontkb.createDatatypeProperty(this.wflowns + pname);

        for (String pname : oprops) if (
          !terms.containsKey(this.wflowns + pname)
        ) this.ontkb.createObjectProperty(this.wflowns + pname);

        for (String cname : concepts) if (
          !terms.containsKey(this.wflowns + cname)
        ) this.ontkb.createClass(this.wflowns + cname);

        this.ontkb.setPropertyValue(
            ontobj,
            this.ontkb.getProperty(KBUtils.OWL + "versionInfo"),
            this.ontkb.createLiteral(this.ontVersion)
          );

        this.save();
      }
      this.end();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private HashMap<String, KBObject> getKBTerms() {
    HashMap<String, KBObject> terms = new HashMap<String, KBObject>();
    for (KBObject obj : ontkb.getAllClasses()) {
      if (obj != null) terms.put(obj.getName(), obj);
    }
    for (KBObject obj : ontkb.getAllObjectProperties()) {
      if (obj != null) terms.put(obj.getName(), obj);
    }
    for (KBObject obj : ontkb.getAllDatatypeProperties()) {
      if (obj != null) terms.put(obj.getName(), obj);
    }
    return terms;
  }

  @Override
  public ArrayList<String> getTemplateList() {
    ArrayList<String> list = new ArrayList<String>();
    if (this.kb == null) return list;

    this.start_read();
    KBObject tconcept = this.kb.getConcept(this.wflowns + "WorkflowTemplate");
    ArrayList<KBObject> tobjs = this.kb.getInstancesOfClass(tconcept, true);
    for (KBObject tobj : tobjs) {
      list.add(tobj.getID());
    }
    this.end();

    return list;
  }

  @Override
  public Template getTemplate(String tplid) {
    return new TemplateKB(this.props, tplid);
  }

  @Override
  public Template createTemplate(String tplid) {
    try {
      return new TemplateKB(this.props, tplid, true);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public HashMap<String, ArrayList<String>> getTemplatesContainingComponents(
    String[] cids
  ) {
    HashMap<String, ArrayList<String>> componentTemplates =
      new HashMap<String, ArrayList<String>>();
    ArrayList<String> templateIds = this.getTemplateList();

    String cidstr = "";
    for (String cid : cids) cidstr += "<" + cid + ">\n";

    String query =
      "PREFIX wflow: <" +
      this.wflowns +
      ">\n" +
      "SELECT DISTINCT ?tpl ?compid WHERE {\n" +
      "?cv wflow:hasComponentBinding ?compid .\n" +
      "VALUES ?compid {\n" +
      cidstr +
      "\n} .\n" +
      "?node wflow:hasComponent ?cv .\n" +
      "?tpl wflow:hasNode ?node\n" +
      "}";

    try {
      this.read(() -> {
          ArrayList<ArrayList<SparqlQuerySolution>> result =
            unionkb.sparqlQuery(query);

          for (ArrayList<SparqlQuerySolution> row : result) {
            String tplid = null, compid = null;
            for (SparqlQuerySolution item : row) {
              if (item.getVariable().equals("tpl")) {
                tplid = item.getObject().getID();
              } else if (item.getVariable().equals("compid")) {
                compid = item.getObject().getID();
              }
            }
            if (
              tplid != null && compid != null && templateIds.contains(tplid)
            ) {
              if (!componentTemplates.containsKey(compid)) {
                componentTemplates.put(compid, new ArrayList<String>());
              }
              componentTemplates.get(compid).add(tplid);
            }
          }
        });
      return componentTemplates;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public boolean incrementTemplateVersion(String tplid) {
    return false;
  }

  @Override
  public boolean save() {
    return this.writerkb.save();
  }

  @Override
  public boolean registerTemplate(Template tpl) {
    try {
      this.start_write();
      // Add to List if not already there
      KBObject tplobj = this.kb.getIndividual(tpl.getID());
      if (tplobj == null) {
        KBObject tconcept =
          this.kb.getConcept(this.wflowns + "WorkflowTemplate");
        this.writerkb.createObjectOfClass(tpl.getID(), tconcept);
      }
      return this.save() && this.end();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean registerTemplateAs(Template tpl, String newid) {
    try {
      this.start_write();
      // Add to List if not already there
      KBObject tplobj = this.kb.getIndividual(newid);
      if (tplobj == null) {
        KBObject tconcept =
          this.kb.getConcept(this.wflowns + "WorkflowTemplate");
        this.writerkb.createObjectOfClass(newid, tconcept);
      }
      return this.save() && this.end();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean deregisterTemplate(Template tpl) {
    try {
      this.start_write();
      KBObject tplobj = this.kb.getIndividual(tpl.getID());
      if (tplobj != null) {
        //Remove template from kb
        KBObject tconcept =
          this.kb.getConcept(this.wflowns + "WorkflowTemplate");
        KBObject typeprop = this.kb.getProperty(KBUtils.RDF + "type");
        this.writerkb.removeTriple(tplobj, typeprop, tconcept);
      }
      return this.save() && this.end();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public ArrayList<ConstraintProperty> getAllConstraintProperties() {
    // Return some hard-coded constraint properties
    // FIXME: These should be read from the KB
    ConstraintProperty samedata = new ConstraintProperty(
      this.wflowns + "hasSameDataAs",
      ConstraintProperty.OBJECT
    );
    samedata.addDomain(this.wflowns + "DataVariable");
    samedata.setRange(this.wflowns + "DataVariable");

    ConstraintProperty diffdata = new ConstraintProperty(
      this.wflowns + "hasDifferentDataFrom",
      ConstraintProperty.OBJECT
    );
    diffdata.addDomain(this.wflowns + "DataVariable");
    diffdata.setRange(this.wflowns + "DataVariable");

    ConstraintProperty paramvalue = new ConstraintProperty(
      this.wflowns + "hasParameterValue",
      ConstraintProperty.DATATYPE
    );
    paramvalue.addDomain(this.wflowns + "ParameterVariable");

    ConstraintProperty databinding = new ConstraintProperty(
      this.wflowns + "hasDataBinding",
      ConstraintProperty.OBJECT
    );
    databinding.addDomain(this.wflowns + "DataVariable");

    ConstraintProperty type = new ConstraintProperty(
      KBUtils.RDF + "type",
      ConstraintProperty.OBJECT
    );
    type.addDomain(this.wflowns + "DataVariable");
    type.addDomain(KBUtils.OWL + "Class");

    ConstraintProperty[] list = new ConstraintProperty[] {
      samedata,
      diffdata,
      paramvalue,
      databinding,
      type,
    };
    return new ArrayList<ConstraintProperty>(Arrays.asList(list));
  }

  @Override
  public void copyFrom(TemplateCreationAPI tc) {
    try {
      TemplateCreationKB tckb = (TemplateCreationKB) tc;

      //System.out.println("Copying template list");

      // Edit local workflow list
      this.start_write();

      // FIRST: Copy the workflow list
      // -----------------------------
      tckb.start_read();
      this.writerkb.copyFrom(tckb.writerkb);

      // Rename ontology namespaces to local ones
      KBUtils.renameTripleNamespace(this.writerkb, tckb.wflowns, this.wflowns);
      KBUtils.renameAllTriplesWith(
        this.writerkb,
        tckb.onturl,
        this.onturl,
        false
      );
      KBUtils.renameAllTriplesWith(
        this.writerkb,
        tckb.liburl,
        this.liburl,
        false
      );

      // Rename template ids to local ones
      ArrayList<String> tplids = tckb.getTemplateList();
      for (String tplid : tplids) {
        String ntplid = tplid.replace(tckb.wdirurl, this.wdirurl);
        KBUtils.renameAllTriplesWith(this.writerkb, tplid, ntplid, false);
      }

      // Workflow/Template namespace rename maps
      HashMap<String, String> nsmap = new HashMap<String, String>();
      nsmap.put(tckb.wflowns, this.wflowns);
      nsmap.put(tckb.dcdomns, this.dcdomns);
      nsmap.put(tckb.dclibns, this.dclibns);
      nsmap.put(tckb.pcdomns, this.pcdomns);

      this.save();
      tckb.end();
      this.end();

      // SECOND: Copy Each workflow Graph
      // --------------------------------

      // Copy workflows into local space and rename urls to local
      for (String tplid : tplids) {
        // Load and save the template in the latest format
        TemplateKB tpl = (TemplateKB) tckb.getTemplate(tplid);

        String tplurl = tplid.replaceAll("#.*$", "");
        String ntplurl = tplurl.replace(tckb.wdirurl, this.wdirurl);
        //System.out.println("Copying template " + ntplurl);
        try {
          this.start_write();

          KBAPI ntplkb = this.ontologyFactory.getKB(ntplurl, OntSpec.PLAIN);
          ntplkb.copyFrom(tpl.getKBCopy(true));

          //System.out.println("Copied Template KB");

          HashMap<String, String> tnsmap = new HashMap<String, String>(nsmap);
          tnsmap.put(tplurl + "#", ntplurl + "#");
          KBUtils.renameTripleNamespaces(ntplkb, tnsmap);
          KBUtils.renameAllTriplesWith(ntplkb, tplurl, ntplurl, false);
          KBUtils.renameAllTriplesWith(ntplkb, tckb.onturl, this.onturl, false);
          KBUtils.renameAllTriplesWith(ntplkb, tckb.liburl, this.liburl, false);
          //System.out.println("Renamed Triples in KB");

          this.save(ntplkb);
          //System.out.println("Saved KB");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      //System.out.println("Done");
      this.initializeAPI(true);
    } finally {
      this.end();
    }
  }

  @Override
  public boolean delete() {
    for (String tplid : this.getTemplateList()) {
      try {
        KBAPI tplkb =
          this.ontologyFactory.getKB(
              new URIEntity(tplid).getURL(),
              OntSpec.PLAIN
            );
        this.start_write();
        tplkb.delete();
        tplkb.save();
        this.end();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return this.start_write() && this.kb.delete() && this.save() && this.end();
  }
}
