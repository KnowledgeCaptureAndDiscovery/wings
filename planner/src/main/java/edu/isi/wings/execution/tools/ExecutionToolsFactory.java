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

package edu.isi.wings.execution.tools;

import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import edu.isi.wings.execution.tools.api.impl.kb.ExecutionResourceKB;
import edu.isi.wings.execution.tools.api.impl.kb.RunKB;
import java.util.Properties;

public class ExecutionToolsFactory {

  public static ExecutionLoggerAPI createLogger(String impl, Properties props)
    throws Exception {
    Class<?> classz = Class.forName(impl);
    return (ExecutionLoggerAPI) classz
      .getDeclaredConstructor(Properties.class)
      .newInstance(props);
  }

  public static ExecutionMonitorAPI createMonitor(
    String impl,
    Properties props
  ) throws Exception {
    Class<?> classz = Class.forName(impl);
    return (ExecutionMonitorAPI) classz
      .getDeclaredConstructor(Properties.class)
      .newInstance(props);
  }

  public static ExecutionLoggerAPI createLogger(Properties props) {
    return new RunKB(props);
  }

  public static ExecutionMonitorAPI createMonitor(Properties props) {
    return new RunKB(props);
  }

  public static ExecutionResourceAPI getResourceAPI(Properties props) {
    return new ExecutionResourceKB(props);
  }
}
