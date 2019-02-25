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

package edu.isi.wings.catalog.data.api;

import java.util.ArrayList;

import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.metrics.Metrics;

/**
 * The interface used by the Workflow system to assist in Planning and
 * Generating workflows
 */
public interface DataReasoningAPI extends TransactionsAPI {
	// API to help in Workflow Planning and Generation

	ArrayList<VariableBindingsList> findDataSources(ArrayList<KBTriple> dods, String templateNS);

	Metrics findDataMetricsForDataObject(String dataObjectId);
	
	Metrics fetchDataMetricsForDataObject(String dataObjectId);

	String getDataLocation(String dataid);
	
	String getDefaultDataLocation(String dataid);

	String createDataIDFromKey(String hashkey, String prefix);

	String createDataIDFromMetrics(String id, String type, Metrics metrics);

	boolean checkDatatypeSubsumption(String dtypeid_subsumer, String dtypeid_subsumee);
	
}
