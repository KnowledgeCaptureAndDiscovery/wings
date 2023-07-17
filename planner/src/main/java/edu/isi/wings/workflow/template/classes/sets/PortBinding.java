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

package edu.isi.wings.workflow.template.classes.sets;

import edu.isi.wings.workflow.template.classes.Port;
import java.util.HashMap;

public class PortBinding extends HashMap<Port, Binding> {

  private static final long serialVersionUID = 1L;

  public PortBinding() {}

  public PortBinding(PortBinding b) {
    super(b);
  }

  public Binding getById(String portid) {
    for (Port p : this.keySet()) {
      if (p.getID().equals(portid)) {
        return this.get(p);
      }
    }
    return null;
  }
}
