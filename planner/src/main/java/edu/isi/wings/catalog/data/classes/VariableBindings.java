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

package edu.isi.wings.catalog.data.classes;

import java.util.ArrayList;
import java.util.HashSet;

import edu.isi.wings.ontapi.KBObject;

public class VariableBindings {

	/**
	 * dataVariable from the workflow namespace
	 */
	public KBObject dataVariable;

	/**
	 * dataObjects from the dc namespace
	 */
	public HashSet<KBObject> dataObjects = new HashSet<KBObject>();

	public VariableBindings() {
	}

	/**
	 * 
	 * @param dataVariable
	 *            data variable from the workflow namespace
	 * @param dataObject
	 *            data object from the dc namespace
	 */
	public VariableBindings(KBObject dataVariable, KBObject dataObject) {
		this.dataVariable = dataVariable;
		this.dataObjects.add(dataObject);
	}

	/**
	 * returns an array list of kbojects <dataVariable dataObjects>
	 * 
	 * @return an array list of kbojects
	 */
	public ArrayList<KBObject> toArrayList() {
		ArrayList<KBObject> result = new ArrayList<KBObject>(2);
		result.add(this.getDataVariable());
		result.addAll(this.getDataObjects());
		return result;
	}

	/**
	 * Getter for property 'dataVariable'.
	 * 
	 * @return Value for property 'dataVariable'.
	 */
	public KBObject getDataVariable() {
		return dataVariable;
	}

	/**
	 * Setter for property 'dataVariable'.
	 * 
	 * @param dataVariable
	 *            Value to set for property 'dataVariable'.
	 */
	public void setDataVariable(KBObject dataVariable) {
		this.dataVariable = dataVariable;
	}

	/**
	 * Getter for property 'dataObjects'.
	 * 
	 * @return Value for property 'dataObjects'.
	 */
	public HashSet<KBObject> getDataObjects() {
		return dataObjects;
	}

	/**
	 * Setter for property 'dataObjects'.
	 * 
	 * @param dataObject
	 *            Value to set for property 'dataObjects'.
	 */
	public void setDataObjects(HashSet<KBObject> dataObjects) {
		this.dataObjects = dataObjects;
	}

	/**
	 * Added for property 'dataObjects'
	 * 
	 * @param dataObject
	 *            Value to add for property 'dataObjects'
	 */
	public void addDataObject(KBObject dataObject) {
		this.dataObjects.add(dataObject);
	}

	public String toString() {
		String str = "";
		str += "( " + dataVariable.shortForm() + " : [ ";
		for (KBObject dataObject : dataObjects)
			str += dataObject.shortForm() + " ";
		str += "] )";
		return str;
	}
}
