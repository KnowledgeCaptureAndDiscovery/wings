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

import edu.isi.kcap.ontapi.transactions.TransactionsAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentTree;

public interface ComponentCreationAPI extends TransactionsAPI {
	// Query
	ComponentTree getComponentHierarchy(boolean details);

	Component getComponent(String cid, boolean details);

	String getComponentLocation(String cid);
	
	String getDefaultComponentLocation(String cid);

	String getComponentHolderId(String cid);
	
	// Update
	boolean addComponent(Component comp, String pholderid);
	
	boolean addComponentHolder(String holderid, String pholderid);

	boolean updateComponent(Component newcomp);
	
	boolean renameComponent(String oldid, String newid);

	boolean removeComponent(String cid, boolean remove_holder, boolean unlink);
	
	boolean removeComponentHolder(String holderid);
	
	boolean setComponentLocation(String cid, String location);
	
	boolean setComponentVersion(String cid, int version);
	
	boolean incrementComponentVersion(String id);
	
	// Saving
	boolean save();
	
	boolean delete();
	
	// Copy from another API (Advisable to give the same implementation of the API here)
	void copyFrom(ComponentCreationAPI dc);
	
	 // Get/Set external data catalog to copy from
	ComponentCreationAPI getExternalCatalog();
  
  void setExternalCatalog(ComponentCreationAPI dc);
}
