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

package edu.isi.wings.execution.tools.api.impl.kb;

import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.catalog.resource.classes.Machine;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import java.util.Properties;

public class ExecutionResourceKB implements ExecutionResourceAPI {

  ResourceAPI api;
  String storageFolder;

  public ExecutionResourceKB(Properties props) {
    this.api = ResourceFactory.getAPI(props);
  }

  @Override
  public Machine getMachine(String machineId) {
    try {
      this.api.start_read();
      return this.api.getMachine(machineId);
    } finally {
      this.api.end();
    }
  }

  @Override
  public void setLocalStorageFolder(String path) {
    this.storageFolder = path;
  }

  @Override
  public String getLocalStorageFolder() {
    return this.storageFolder;
  }
}
