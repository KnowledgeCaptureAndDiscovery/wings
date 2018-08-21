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

import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.transactions.TransactionsAPI;

public interface ConstraintEngine extends TransactionsAPI {
	public void setConstraints(ArrayList<KBTriple> constraints);

	public void addConstraints(ArrayList<KBTriple> constraints);

	public void removeConstraint(KBTriple constraint);

	public void removeObjectAndConstraints(KBObject obj);

	public KBTriple createNewConstraint(String subjID, String predID, String objID);

	public KBTriple createNewDataConstraint(String subjID, String predID, String obj, String type);

	public KBObject getResource(String ID);

	public boolean containsConstraint(KBTriple constraint);

	public ArrayList<KBTriple> getConstraints();

	public ArrayList<KBTriple> getConstraints(String id);

	public ArrayList<KBTriple> getConstraints(ArrayList<String> ids);

	public void addBlacklistedId(String id);

	public void removeBlacklistedId(String id);

	public void addBlacklistedNamespace(String ns);

	public void removeBlacklistedNamespace(String ns);

	public void addWhitelistedNamespace(String ns);

	public void removeWhitelistedNamespace(String ns);

	public void replaceSubjectInConstraints(KBObject subj, KBObject newSubj);

	public void replaceObjectInConstraints(KBObject obj, KBObject newObj);

}
