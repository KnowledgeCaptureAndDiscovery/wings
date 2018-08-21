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

package edu.isi.wings.catalog.component.api;

import java.util.ArrayList;

import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.catalog.component.classes.ComponentInvocation;
import edu.isi.wings.catalog.component.classes.ComponentPacket;

/**
 * The interface for communicating with the Component Catalog during Workflow
 * Planning and generation
 */
public interface ComponentReasoningAPI extends TransactionsAPI {
	// Generation API
	ArrayList<ComponentPacket> specializeAndFindDataDetails(ComponentPacket details);

	ComponentPacket findDataDetails(ComponentPacket details);

	ArrayList<ComponentPacket> findOutputDataPredictedDescriptions(ComponentPacket details);

	ComponentInvocation getComponentInvocation(ComponentPacket details);

}
