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

package edu.isi.wings.execution.tools.api;

import java.util.ArrayList;

import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.execution.engine.classes.RuntimePlan;

public interface ExecutionMonitorAPI extends TransactionsAPI {
	// The RuntimePlan here is expected to not contain detail about all steps here
	ArrayList<RuntimePlan> getRunList(String pattern, String status, int start, int limit, boolean fasterQuery);

	ArrayList<RuntimePlan> getRunListSimple(String pattern, String status, int start, int limit);


	int getNumberOfRuns(String pattern, String status);

	RuntimePlan getRunDetails(String runid);
	
	boolean runExists(String runid);
	
	boolean deleteRun(String runid);
	
	RuntimePlan rePlan(RuntimePlan planexe);
	
	// Save/Delete
	boolean save();
	
	boolean delete();
}
