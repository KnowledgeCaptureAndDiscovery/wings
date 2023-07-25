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

package edu.isi.wings.catalog.resource.api;

import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.requirements.ComponentRequirement;
import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.catalog.resource.classes.Software;
import edu.isi.wings.catalog.resource.classes.SoftwareEnvironment;
import edu.isi.wings.catalog.resource.classes.SoftwareVersion;
import java.util.ArrayList;

public interface ResourceAPI extends TransactionsAPI {
  // Query functions
  ArrayList<String> getMachineIds();

  ArrayList<String> getSoftwareIds();

  ArrayList<String> getSoftwareVersionIds(String softwareid);

  ArrayList<SoftwareVersion> getAllSoftwareVersions();

  ArrayList<SoftwareEnvironment> getAllSoftwareEnvironment();

  Machine getMachine(String machineid);

  Software getSoftware(String softwareid);

  SoftwareVersion getSoftwareVersion(String versionid);

  ArrayList<String> getMatchingMachineIds(ComponentRequirement req);

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

  void setMachineWhitelist(ArrayList<String> whitelist);

  // Save/ Delete

  boolean save();

  boolean delete();

  void copyFrom(ResourceAPI rc, ComponentCreationAPI dc);
}
