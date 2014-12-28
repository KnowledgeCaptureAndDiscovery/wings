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

package edu.isi.wings.catalog.component.classes.requirements;

import java.util.ArrayList;

public class ComponentRequirement {
  private float storageGB;
  private float memoryGB;
  private boolean need64bit;
  private ArrayList<String> softwareIds;

  public ComponentRequirement() {
    softwareIds = new ArrayList<String>();
  }

  public float getStorageGB() {
    return storageGB;
  }

  public void setStorageGB(float storageGB) {
    this.storageGB = storageGB;
  }

  public float getMemoryGB() {
    return memoryGB;
  }

  public void setMemoryGB(float memoryGB) {
    this.memoryGB = memoryGB;
  }

  public boolean isNeed64bit() {
    return need64bit;
  }

  public void setNeed64bit(boolean need64bit) {
    this.need64bit = need64bit;
  }

  public ArrayList<String> getSoftwareIds() {
    return softwareIds;
  }

  public void addSoftwareId(String softwareId) {
    this.softwareIds.add(softwareId);
  }

  public String toString() {
    String str = "Softwares: "+this.softwareIds.toString()+"\n";
    str += "MemoryGB: "+this.memoryGB+"\n";
    str += "StorageGB: "+this.storageGB+"\n";
    str += "Needs64bit: "+this.need64bit;
    return str;
  }
}
