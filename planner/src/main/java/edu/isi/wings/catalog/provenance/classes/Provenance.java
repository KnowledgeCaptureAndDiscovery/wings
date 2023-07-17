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

package edu.isi.wings.catalog.provenance.classes;

import java.util.ArrayList;

public class Provenance {

  private String objectId;
  private ArrayList<ProvActivity> activities;

  public Provenance(String objectId) {
    super();
    this.objectId = objectId;
    this.activities = new ArrayList<ProvActivity>();
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public ArrayList<ProvActivity> getActivities() {
    return activities;
  }

  public void setActivities(ArrayList<ProvActivity> activities) {
    this.activities = activities;
  }

  public void addActivity(ProvActivity activity) {
    this.activities.add(activity);
  }
}
