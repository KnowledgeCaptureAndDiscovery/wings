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

package edu.isi.wings.workflow.template.api;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.workflow.template.classes.ConstraintProperty;

public interface TemplateCreationAPI extends TransactionsAPI {
	// Query
	ArrayList<String> getTemplateList();
	
	Template getTemplate(String tplid);
	
	ArrayList<ConstraintProperty> getAllConstraintProperties();
	
	public HashMap<String, ArrayList<String>> getTemplatesContainingComponents(String[] cids);

	// Creation
	
	Template createTemplate(String tplid);
	
	boolean registerTemplate(Template tpl);
	
	boolean registerTemplateAs(Template tpl, String newid);
	
	boolean deregisterTemplate(Template tpl);
	
	// Versioning
	
	boolean incrementTemplateVersion(String tplid);

	// Transactions
	boolean delete();
	
	boolean save();
	
	// Copy from another API (Advisable to give the same implementation of the API here)
	void copyFrom(TemplateCreationAPI tc);
	
}
