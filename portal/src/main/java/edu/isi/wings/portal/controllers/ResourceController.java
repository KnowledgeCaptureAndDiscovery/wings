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
import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.catalog.resource.classes.Software;
import edu.isi.wings.catalog.resource.classes.SoftwareVersion;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.config.ConfigLoader;
import java.util.Properties;

public class ResourceController {

  public String rns;
  public String libns;

  public ResourceAPI api;
  public boolean isSandboxed;
  public ConfigLoader config;
  public Properties props;
  public Gson json;

  public ResourceController(ConfigLoader config) {
    this.config = config;
    this.isSandboxed = config.isSandboxed();
    json = JsonHandler.createGson();
    this.props = config.getProperties();

    api = ResourceFactory.getAPI(props);

    this.rns = (String) props.get("ont.resource.url") + "#";
    this.libns = (String) props.get("lib.resource.url") + "#";
  }

  public void end() {
    if (api != null) {
      api.end();
    }
  }

  // Query
  public String getAllSoftwareVersions() {
    return json.toJson(this.api.getAllSoftwareVersions());
  }

  public String getAllSoftwareEnvironment() {
    return json.toJson(this.api.getAllSoftwareEnvironment());
  }

  public String getMachineJSON(String resid) {
    return json.toJson(this.api.getMachine(resid));
  }

  public String getSoftwareJSON(String resid) {
    return json.toJson(this.api.getSoftware(resid));
  }

  public String getSoftwareVersionJSON(String resid) {
    return json.toJson(this.api.getSoftwareVersion(resid));
  }

  public String checkMachine(String resid) {
    Machine machine = this.api.getMachine(resid);
    return json.toJson(machine.getMachineDetails());
  }

  // Add
  public boolean addMachine(String resid) {
    return this.api.addMachine(resid);
  }

  public boolean addSoftware(String resid) {
    return this.api.addSoftware(resid);
  }

  public boolean addSoftwareVersion(String resid, String softwareid) {
    return this.api.addSoftwareVersion(resid, softwareid);
  }

  // Save updates
  public boolean saveMachineJSON(String resid, String resvals_json) {
    Machine machine = json.fromJson(resvals_json, Machine.class);
    return this.api.saveMachine(machine);
  }

  public boolean saveSoftwareJSON(String resid, String resvals_json) {
    Software software = json.fromJson(resvals_json, Software.class);
    return this.api.saveSoftware(software);
  }

  public boolean saveSoftwareVersionJSON(String resid, String resvals_json) {
    SoftwareVersion version = json.fromJson(
      resvals_json,
      SoftwareVersion.class
    );
    return this.api.saveSoftwareVersion(version);
  }

  // Remove
  public boolean removeMachine(String resid) {
    return this.api.removeMachine(resid);
  }

  public boolean removeSoftware(String resid) {
    return this.api.removeSoftware(resid);
  }

  public boolean removeSoftwareVersion(String resid) {
    return this.api.removeSoftwareVersion(resid);
  }
}
