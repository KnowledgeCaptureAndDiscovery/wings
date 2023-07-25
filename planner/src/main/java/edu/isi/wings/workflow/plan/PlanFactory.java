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

package edu.isi.wings.workflow.plan;

import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.ExecutionStep;
import edu.isi.wings.workflow.plan.api.impl.pplan.PPlan;
import edu.isi.wings.workflow.plan.api.impl.pplan.PPlanStep;
import java.util.Properties;

public class PlanFactory {

  public static ExecutionPlan createExecutionPlan(
    String impl,
    String id,
    Properties props
  ) throws Exception {
    Class<?> classz = Class.forName(impl);
    return (ExecutionPlan) classz
      .getDeclaredConstructor(String.class, Properties.class)
      .newInstance(id, props);
  }

  public static ExecutionStep createExecutionStep(
    String impl,
    String id,
    Properties props
  ) throws Exception {
    Class<?> classz = Class.forName(impl);
    return (ExecutionStep) classz
      .getDeclaredConstructor(String.class, Properties.class)
      .newInstance(id, props);
  }

  public static ExecutionPlan loadExecutionPlan(String id, Properties props) {
    return new PPlan(id, props, true);
  }

  public static ExecutionPlan createExecutionPlan(String id, Properties props) {
    return new PPlan(id, props);
  }

  public static ExecutionStep createExecutionStep(String id, Properties props) {
    return new PPlanStep(id, props);
  }
}
