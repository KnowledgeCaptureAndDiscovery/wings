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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.StorageHandler;
import edu.isi.wings.portal.classes.config.ConfigLoader;
import edu.isi.wings.portal.classes.domains.Domain;
import edu.isi.wings.portal.classes.domains.DomainInfo;
import edu.isi.wings.portal.classes.domains.Permission;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.io.FileUtils;

public class DomainController {

  public ConfigLoader config;
  public Gson json;

  private Domain domain;
  private HashMap<String, DomainInfo> user_domains;
  private String defaultDomainName = "blank";

  private String userdir;
  private String userConfigFile;

  public DomainController(ConfigLoader config) {
    this.config = config;
    this.json = JsonHandler.createGson();
    this.user_domains = new HashMap<String, DomainInfo>();
    this.userConfigFile = config.getUserDir() + "/user.properties";

    this.initializeDomainList(config.getDomainId());
  }

  public String getDomainsListJSON() {
    String viewerid = config.getViewerId();
    Collection<DomainInfo> dominfos = new ArrayList<DomainInfo>();
    String selected = "null";
    if (viewerid.equals(config.getUserId())) {
      dominfos = user_domains.values();
      selected =
        (domain != null ? json.toJson(domain.getDomainName()) : "null");
    } else {
      for (DomainInfo dominfo : this.user_domains.values()) {
        Domain dom = new Domain(dominfo);
        Permission perm = dom.getPermissionForUser(viewerid);
        if (perm.canRead()) {
          dominfos.add(dominfo);
        }
      }
      selected = "null";
    }
    return (
      "{ list: " +
      json.toJson(dominfos) +
      ", selected: " +
      selected +
      ", engines: " +
      json.toJson(config.portalConfig.getEnginesList()) +
      "}"
    );
  }

  public ArrayList<String> getDomainsList() {
    ArrayList<String> domains = new ArrayList<String>();
    String viewerid = config.getViewerId();
    if (viewerid.equals(config.getUserId())) {
      for (String domname : user_domains.keySet()) domains.add(domname);
    }
    return domains;
  }

  public ArrayList<String> getReadableDomainsList() {
    String viewerid = config.getViewerId();
    if (viewerid.equals(config.getUserId())) return this.getDomainsList();

    ArrayList<String> domains = new ArrayList<String>();
    for (DomainInfo dominfo : this.user_domains.values()) {
      Domain dom = new Domain(dominfo);
      Permission perm = dom.getPermissionForUser(viewerid);
      if (perm.canRead()) {
        domains.add(dom.getDomainName());
      }
    }
    return domains;
  }

  public Domain getDomain(String domain) {
    DomainInfo dominfo = this.user_domains.get(domain);
    return new Domain(dominfo);
  }

  public DomainInfo getDomainInfo(String domain) {
    return this.user_domains.get(domain);
  }

  public String getDomainJSON(String domain) {
    DomainInfo dominfo = this.user_domains.get(domain);
    if (dominfo != null) {
      Domain dom = new Domain(dominfo);
      return json.toJson(dom);
    }
    return null;
  }

  public Domain getUserDomain() {
    return this.domain;
  }

  public String importDomain(String domain, String location) {
    File f = null;

    // Check if the location is a url
    try {
      // Fetch domain zip file from url
      URL url = new URL(location);
      File furl = new File(url.getPath());
      String zipname = furl.getName();
      InputStream input = url.openStream();
      f = File.createTempFile("domain-", "-temp");
      if (f.delete() && f.mkdirs()) {
        f = new File(f.getAbsolutePath() + File.separator + zipname);
        FileUtils.copyInputStreamToFile(input, f);
      }
    } catch (MalformedURLException e) {
      // Do nothing
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    // If not a url, assume normal file location
    if (f == null) f = new File(location);

    // If the location is a zip file instead of a directory,
    // then unzip into a temporary directory
    File ftemp = null;
    if (f.exists() && !f.isDirectory() && f.getName().endsWith(".zip")) {
      try {
        ftemp = File.createTempFile("domain-", "-temp");
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
      if (ftemp.delete() && ftemp.mkdirs()) {
        ftemp = new File(ftemp.getAbsolutePath() + File.separator + domain);
      }
      domain = domain.replaceFirst(".zip", "");
      StorageHandler.unzipFile(f, domain, ftemp.getAbsolutePath());
      f = new File(ftemp.getAbsolutePath() + File.separator + domain);
    }

    // Finally check that the domain directory exists
    if (!f.exists() || !f.isDirectory()) return null;

    // Import domain directory
    Domain dom = null;
    File oldf = new File(
      f.getAbsolutePath() + File.separator + "wings.properties"
    );
    File newf = new File(
      f.getAbsolutePath() + File.separator + "domain.properties"
    );
    if (oldf.exists()) dom =
      Domain.importLegacyDomain(
        domain,
        this.config,
        f.getAbsolutePath()
      ); else if (newf.exists()) dom =
      Domain.importDomain(
        domain,
        this.config,
        f.getAbsolutePath()
      ); else return null;

    DomainInfo dominfo = new DomainInfo(dom);
    this.user_domains.put(dom.getDomainName(), dominfo);
    this.saveUserConfig(this.userConfigFile);

    // Delete temporary directory
    if (ftemp != null) try {
      FileUtils.deleteDirectory(ftemp);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return json.toJson(dominfo);
  }

  public String createDomain(String domName) {
    Domain dom = Domain.createDefaultDomain(
      domName,
      this.config.getUserDir(),
      this.config.getExportUserUrl()
    );
    DomainInfo dominfo = new DomainInfo(dom);
    this.user_domains.put(dom.getDomainName(), dominfo);
    this.saveUserConfig(this.userConfigFile);
    return json.toJson(dominfo);
  }

  public boolean selectDomain(String domain) {
    DomainInfo dominfo = this.user_domains.get(domain);
    this.domain = new Domain(dominfo);
    if (this.saveUserConfig(this.userConfigFile)) return true;
    return false;
  }

  public boolean setDomainURL(String domain, String url) {
    DomainInfo dominfo = this.user_domains.get(domain);
    dominfo.setUrl(url);
    if (this.saveUserConfig(this.userConfigFile)) return true;
    return false;
  }

  public boolean deleteDomain(String domain) {
    DomainInfo dominfo = this.user_domains.get(domain);
    if (dominfo != null) {
      this.user_domains.remove(domain);
      Domain dom = new Domain(dominfo);
      if (!Domain.deleteDomain(dom, config, true)) return false;

      ProvenanceAPI prov = ProvenanceFactory.getAPI(config.getProperties());
      prov.removeAllDomainProvenance(dom.getDomainUrl());
    }
    if (this.saveUserConfig(this.userConfigFile)) return true;
    return false;
  }

  public boolean renameDomain(String domain, String newname) {
    DomainInfo dominfo = this.user_domains.get(domain);
    if (dominfo != null) {
      this.user_domains.remove(domain);
      Domain dom = new Domain(dominfo);
      Domain newdom = Domain.renameDomain(dom, newname, config);
      if (newdom == null) return false;
      DomainInfo newdominfo = new DomainInfo(newdom);
      this.user_domains.put(newname, newdominfo);
      if (this.domain.getDomainName().equals(domain)) this.domain = newdom;

      ProvenanceAPI prov = ProvenanceFactory.getAPI(config.getProperties());
      prov.renameAllDomainProvenance(dom.getDomainUrl(), newdom.getDomainUrl());
    }
    if (this.saveUserConfig(this.userConfigFile)) return true;
    return false;
  }

  public Response streamDomain(String domName, ServletContext context) {
    DomainInfo dominfo = this.user_domains.get(domName);
    if (dominfo == null) return Response.status(Status.NOT_FOUND).build();

    Domain dom = new Domain(dominfo);
    File f = Domain.exportDomain(dom, this.config);
    if (f == null) return Response.status(Status.INTERNAL_SERVER_ERROR).build();

    return StorageHandler.streamFile(
      f.getAbsolutePath() + File.separator + dom.getDomainName(),
      context
    );
    /*try {
			FileUtils.deleteDirectory(f);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
  }

  public boolean setDomainExecutionEngine(String domain, String engine) {
    DomainInfo dominfo = this.user_domains.get(domain);
    if (dominfo == null) return false;

    Domain dom = new Domain(dominfo);
    if (config.portalConfig.getEnginesList().contains(engine)) {
      dom.setPlanEngine(engine);
      dom.setStepEngine(engine);
    }
    return dom.saveDomain();
  }

  public boolean setDomainPermissions(String domain, String permissions_json) {
    DomainInfo dominfo = this.user_domains.get(domain);
    if (dominfo == null) return false;

    ArrayList<Permission> permissions = new ArrayList<Permission>();
    JsonParser parser = new JsonParser();
    JsonElement perms = parser.parse(permissions_json);
    for (JsonElement el : perms.getAsJsonArray()) {
      permissions.add(json.fromJson(el, Permission.class));
    }

    Domain dom = new Domain(dominfo);
    dom.setPermissions(permissions);
    return dom.saveDomain();
  }

  /*
   * Private functions
   */

  private void initializeDomainList(String domname) {
    PropertyListConfiguration config = this.getUserConfiguration();
    List<HierarchicalConfiguration> domnodes = config.configurationsAt(
      "user.domains.domain"
    );
    if (domname == null) domname = config.getString("user.domain");
    for (HierarchicalConfiguration domnode : domnodes) {
      String domurl = domnode.getString("url");
      Boolean isLegacy = domnode.getBoolean("legacy", false);
      String dname = domnode.getString("name");
      Domain domain = new Domain(
        dname,
        domnode.getString("dir"),
        domurl,
        isLegacy
      );
      if (dname.equals(domname)) this.domain = domain;
      DomainInfo dominfo = new DomainInfo(domain);
      this.user_domains.put(dominfo.getName(), dominfo);
    }
  }

  private PropertyListConfiguration getUserConfiguration() {
    this.userdir = this.config.getUserDir();
    this.userConfigFile = userdir + "/user.properties";
    // Create userConfigFile if it doesn't exist
    File cfile = new File(userConfigFile);
    if (!cfile.exists()) {
      if (!cfile.getParentFile().exists() && !cfile.getParentFile().mkdirs()) {
        System.err.println(
          "Cannot create config file directory : " + cfile.getParent()
        );
        return null;
      }
      createDefaultUserConfig(userConfigFile);
    }
    // Load properties from configFile
    PropertyListConfiguration config = new PropertyListConfiguration();
    try {
      config.load(userConfigFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return config;
  }

  private void createDefaultUserConfig(String configFile) {
    this.domain =
      Domain.createDefaultDomain(
        this.defaultDomainName,
        config.getUserDir(),
        config.getExportUserUrl()
      );
    DomainInfo dominfo = new DomainInfo(this.domain);
    this.user_domains.put(this.domain.getDomainName(), dominfo);
    this.saveUserConfig(configFile);
  }

  private boolean saveUserConfig(String file) {
    PropertyListConfiguration config = new PropertyListConfiguration();
    config.addProperty("user.domain", this.domain.getDomainName());
    for (String domname : this.user_domains.keySet()) {
      DomainInfo dom = this.user_domains.get(domname);
      config.addProperty("user.domains.domain(-1).name", dom.getName());
      config.addProperty("user.domains.domain.dir", dom.getDirectory());
      if (dom.isLegacy()) config.addProperty(
        "user.domains.domain.legacy",
        dom.isLegacy()
      ); else config.addProperty("user.domains.domain.url", dom.getUrl());
    }
    try {
      config.save(file);
      return true;
    } catch (ConfigurationException e) {
      e.printStackTrace();
      return false;
    }
  }
}
