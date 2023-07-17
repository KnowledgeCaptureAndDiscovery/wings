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

package edu.isi.wings.workflow.plan.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.workflow.plan.api.impl.pplan.PPlan;
import java.util.ArrayList;

@JsonDeserialize(as = PPlan.class)
public interface ExecutionPlan extends TransactionsAPI {
  // ID functions
  public void setID(String id);

  public String getID();

  public String getNamespace();

  public String getName();

  public String getURL();

  // Step functions
  public void addExecutionStep(ExecutionStep step);

  public ArrayList<ExecutionStep> getAllExecutionSteps();

  // Interleaving planning / execution
  public boolean isIncomplete();

  public void setIsIncomplete(boolean incomplete);

  // Save
  public boolean save();

  public boolean saveAs(String newid);

  public String serialize();
}
