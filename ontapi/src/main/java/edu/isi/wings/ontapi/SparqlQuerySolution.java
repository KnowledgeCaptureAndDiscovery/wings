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

package edu.isi.wings.ontapi;

public class SparqlQuerySolution {

	/**
	 * the variable
	 */
	public String variable;

	/**
	 * the kbobject
	 */
	public KBObject object;

	/**
	 * a new sparql query solution
	 * 
	 * @param variable
	 *            a variable
	 * @param object
	 *            a kbobject
	 */
	public SparqlQuerySolution(String variable, KBObject object) {
		this.variable = variable;
		this.object = object;
	}

	/**
	 * Getter for property 'variable'.
	 * 
	 * @return Value for property 'variable'.
	 */
	public String getVariable() {
		return variable;
	}

	/**
	 * Setter for property 'variable'.
	 * 
	 * @param variable
	 *            Value to set for property 'variable'.
	 */
	public void setVariable(String variable) {
		this.variable = variable;
	}

	/**
	 * Getter for property 'object'.
	 * 
	 * @return Value for property 'object'.
	 */
	public KBObject getObject() {
		return object;
	}

	/**
	 * Setter for property 'object'.
	 * 
	 * @param object
	 *            Value to set for property 'object'.
	 */
	public void setObject(KBObject object) {
		this.object = object;
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		string.append("( ");
		string.append(variable);
		string.append(" = ");
		string.append(object == null ? null : (object.isLiteral() ? object.getValue() : object
				.getID()));
		string.append(" )");
		return string.toString();
	}
}
