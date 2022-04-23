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

package edu.isi.wings.portal.controllers;

import java.io.File;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.util.Scanner;
import java.io.IOException;
import java.util.*;

import org.apache.jena.util.FileUtils;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentRole;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.data.classes.DataItem;
import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;
import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;
import edu.isi.wings.workflow.template.classes.Link;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.variables.ComponentVariable;
import edu.isi.wings.workflow.template.classes.variables.Variable;
import edu.isi.wings.workflow.template.classes.variables.VariableType;

import com.google.gson.Gson;

//import edu.isi.wings.classes.kb.PropertiesHelper;

public class TemplateController {
    public DataCreationAPI dc;
    public ComponentCreationAPI cc;
    public TemplateCreationAPI tc;
    public ProvenanceAPI prov;

    public Config config;
    public Gson json;
    public Properties props;

    public TemplateController(Config config) {
        this.config = config;
        this.json = JsonHandler.createTemplateGson();
        this.props = config.getProperties();

        tc = TemplateFactory.getCreationAPI(props);
        cc = ComponentFactory.getCreationAPI(props);
        dc = DataFactory.getCreationAPI(props);
        prov = ProvenanceFactory.getAPI(props);
    }
    
    public void end() {
      if(dc != null) {
        dc.end();
      }
      if(cc != null) {
        cc.end();
      }
      if(tc != null) {
        tc.end();
      }
      if(prov != null) {
        prov.end();
      }
    }

    public String getViewerJSON(String tplid) {
        Template tpl = this.tc.getTemplate(tplid);
        HashMap<String, Object> extra = new HashMap<String, Object>();
        extra.put("inputs", this.getTemplateInputs(tpl, true));

        return JsonHandler.getTemplateJSON(json, tpl, extra);
    }

    public String getTemplatesListJSON() {
        return json.toJson(tc.getTemplateList());
    }

    public String getInputsJSON(String tplid) {
        Template tpl = this.tc.getTemplate(tplid);
        return json.toJson(this.getTemplateInputs(tpl, false));
    }

    public String getEditorJSON(String tplid) {
        Template tpl = this.tc.getTemplate(tplid);
        return JsonHandler.getTemplateJSON(json, tpl, null);
    }

    public String getBeamerParaphrasesJSON() {
        try {
            String beamerDir =
                    config.getDomain().getDomainDirectory() + File.separator + "beamer";
            return FileUtils.readWholeFileAsUTF8(beamerDir + File.separator + "paraphrases.json");
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return "{}";
    }

    public String getBeamerMappingsJSON() {
        try {
            String beamerDir =
                    config.getDomain().getDomainDirectory() + File.separator + "beamer";
            return FileUtils.readWholeFileAsUTF8(beamerDir + File.separator + "mappings.json");
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return "{}";
    }

    public synchronized String saveTemplateJSON(String tplid, String templatejson, String consjson) {
        String provlog = "Updating template";
        Provenance p = new Provenance(tplid);
        p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));

        Template tpl = JsonHandler.getTemplateFromJSON(this.json, templatejson, consjson);

        if (!tpl.getMetadata().getContributors().contains(this.config.getUserId()))
            tpl.getMetadata().addContributor(this.config.getUserId());

        if (tpl != null) {
            boolean ok = false;
            if (tplid.equals(tpl.getID())) {
                ok =
                        tpl.save() &&
                                this.tc.registerTemplate(tpl);
            } else {
                ok =
                        tpl.saveAs(tplid);
                this.tc.registerTemplateAs(tpl, tplid);
            }

            if (ok &&
                    prov.addProvenance(p))
                return "OK";
        }
        return "";
    }

    public synchronized String deleteTemplate(String tplid) {
        Template tpl = this.tc.getTemplate(tplid);
        if (tpl != null) {
            if (
                    tpl.delete() &&
                            this.tc.deregisterTemplate(tpl) &&
                            prov.removeAllProvenance(tplid)) {
                return "OK";
            }
        }
        return "";
    }

    public synchronized String newTemplate(String tplid) {
        Template tpl = this.tc.createTemplate(tplid);

        String provlog = "Creating new template";
        Provenance p = new Provenance(tplid);
        p.addActivity(new ProvActivity(ProvActivity.CREATE, provlog));

        if (tpl != null) {
            if (
                    tpl.saveAs(tpl.getID()) &&
                            this.tc.registerTemplate(tpl) &&
                            prov.addProvenance(p)) {
                return "OK";
            }
        }
        return "";
    }

    public String layoutTemplate(String tjson, String dotexe)
            throws IOException {
        Template tpl = JsonHandler.getTemplateFromJSON(this.json, tjson, "[]");
        tpl.autoLayout();
        return JsonHandler.getTemplateJSON(this.json, tpl, null);
    }

    private ArrayList<HashMap<String, Object>> getTemplateInputs(Template tpl, boolean dataoptions) {
        HashMap<String, Integer> varDims = new HashMap<String, Integer>();
        HashMap<String, Role> iroles = tpl.getInputRoles();
        for (String varid : iroles.keySet()) {
            Role r = iroles.get(varid);
            varDims.put(varid, r.getDimensionality());
        }

        ArrayList<HashMap<String, Object>> returnList = new ArrayList<HashMap<String, Object>>();

        // Some caches
        HashMap<String, Boolean> varsDone = new HashMap<String, Boolean>();
        HashMap<String, Component> compCache = new HashMap<String, Component>();

        // Go through the input links
        for (Link l : tpl.getInputLinks()) {
            Variable var = l.getVariable();

            if (var.isAutoFill())
                continue;
            if (varsDone.containsKey(var.getID()))
                continue;
            varsDone.put(var.getID(), true);

            ComponentVariable cvar = l.getDestinationNode().getComponentVariable();
            Binding cbinding = cvar.getBinding();
            String cid = cbinding.getID();

            Binding varbinding = var.getBinding();

            // Fetch component details from PC
            Component comp = compCache.containsKey(cid) ? compCache.get(cid) : this.cc
                    .getComponent(cid, true);

            // Can't find this component in the catalog ? Skip
            if (comp == null) continue;

            // Get relevant role details
            String rolename = l.getDestinationPort().getRole().getRoleId();
            String roletypeid = null;
            int roledim = 0;
            for (ComponentRole crole : comp.getInputs()) {
                if (crole.getType() == null)
                    continue;
                if (crole.getRoleName().equals(rolename)) {
                    roletypeid = crole.getType();
                    roledim = crole.getDimensionality();
                }
            }

            // Variable details
            HashMap<String, Object> vardata = new HashMap<String, Object>();
            vardata.put("id", var.getID());
            vardata.put("name", var.getName());
            if (var.getVariableType() == VariableType.DATA) {
                vardata.put("type", "data");
                vardata.put("dtype", roletypeid);
                if (roletypeid != null && dataoptions) {
                    ArrayList<DataItem> dataitems = this.dc.getDataForDatatype(roletypeid, false);
                    ArrayList<String> items = new ArrayList<String>();
                    for (DataItem ditem : dataitems) {
                        items.add(ditem.getID());
                    }
                    vardata.put("options", items);
                }
                int dim = varDims.containsKey(var.getID()) ? varDims.get(var.getID()) : roledim;
                vardata.put("dim", dim);

                if (varbinding != null) {
                    vardata.put("binding", varbinding.getID());
                }
            } else if (var.getVariableType() == VariableType.PARAM) {
                vardata.put("type", "param");
                vardata.put("dtype", roletypeid);
                vardata.put("binding", (varbinding != null ? varbinding.getValue() : ""));
            }
            returnList.add(vardata);
        }
        Collections.sort(returnList, new PositionComparator());
        return returnList;
    }

    public ArrayList<Object> getConstraintProperties() {
        ArrayList<Object> allprops = new ArrayList<Object>();
        allprops.addAll(dc.getAllMetadataProperties());
        allprops.addAll(tc.getAllConstraintProperties());
        return allprops;
    }

}


class PositionComparator implements Comparator<HashMap<String, Object>> {
    @Override
    public int compare(HashMap<String, Object> o1, HashMap<String, Object> o2) {
        return ((String) (o1.get("name"))).compareTo((String) (o2.get("name")));
    }
}
