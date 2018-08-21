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

package edu.isi.wings.catalog.provenance.api;

import java.util.ArrayList;

import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;

public interface ProvenanceAPI extends TransactionsAPI {
  public Provenance getProvenance(String objectId);

  public ArrayList<ProvActivity> getAllUserActivities(String userId);
  
  public boolean setProvenance(Provenance prov);
  
  public boolean addProvenance(Provenance prov);

  public boolean removeProvenance(Provenance prov);

  public boolean removeAllProvenance(String objectId);
  
  public boolean removeAllDomainProvenance(String domainURL);
  
  public boolean renameAllDomainProvenance(String oldDomainURL, String newDomainURL);
  
  public boolean removeUser(String userId);
  
  // Save/Delete
  public boolean save();
  
  public boolean delete();
}
