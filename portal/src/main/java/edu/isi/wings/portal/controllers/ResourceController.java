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

import java.util.Properties;

import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.catalog.resource.classes.Software;
import edu.isi.wings.catalog.resource.classes.SoftwareVersion;
import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.portal.classes.JsonHandler;

import com.google.gson.Gson;

public class ResourceController {
  public String rns;
  public String libns;

  public ResourceAPI api;
  public boolean isSandboxed;
  public Config config;
  public Properties props;
  public Gson json;

  public ResourceController(Config config) {
    this.config = config;
    this.isSandboxed = config.isSandboxed();
    json = JsonHandler.createGson();
    this.props = config.getProperties();

    api = ResourceFactory.getAPI(props);

    this.rns = (String) props.get("ont.resource.url") + "#";
    this.libns = (String) props.get("lib.resource.url") + "#";
  }

  // Query
  public String getAllSoftwareVersions() {
    try {
      return json.toJson(this.api.getAllSoftwareVersions());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      api.end();
    }
  }
  
  public String getAllSoftwareEnvironment() {
    try {
      return json.toJson(this.api.getAllSoftwareEnvironment());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      api.end();
    }
  }
  
  public String getMachineJSON(String resid) {
    try {
      return json.toJson(this.api.getMachine(resid));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      api.end();
    }
  }

  public String getSoftwareJSON(String resid) {
    try {
      return json.toJson(this.api.getSoftware(resid));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      api.end();
    }
  }

  public String getSoftwareVersionJSON(String resid) {
    try {
      return json.toJson(this.api.getSoftwareVersion(resid));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      api.end();
    }
  }
  
  public String checkMachine(String resid) {
    try {
      Machine machine = this.api.getMachine(resid);
      return json.toJson(machine.getMachineDetails());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      api.end();
    }
  }


  // Add
  public boolean addMachine(String resid) {
    try {
      return this.api.addMachine(resid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public boolean addSoftware(String resid) {
    try {
      return this.api.addSoftware(resid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public boolean addSoftwareVersion(String resid, String softwareid) {
    try {
      return this.api.addSoftwareVersion(resid, softwareid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  // Save updates
  public boolean saveMachineJSON(String resid, String resvals_json) {
    try {
      Machine machine = json.fromJson(resvals_json, Machine.class);
      return this.api.saveMachine(machine) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public boolean saveSoftwareJSON(String resid, String resvals_json) {
    try {
      Software software = json.fromJson(resvals_json, Software.class);
      return this.api.saveSoftware(software) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public boolean saveSoftwareVersionJSON(String resid, String resvals_json) {
    try {
      SoftwareVersion version = json.fromJson(resvals_json, SoftwareVersion.class);
      return this.api.saveSoftwareVersion(version) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  // Remove
  public boolean removeMachine(String resid) {
    try {
      return this.api.removeMachine(resid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public boolean removeSoftware(String resid) {
    try {
      return this.api.removeSoftware(resid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public boolean removeSoftwareVersion(String resid) {
    try {
      return this.api.removeSoftwareVersion(resid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }
}
