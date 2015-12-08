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

// Simple interface to any KBObject
// - Wraps around internal implementation of KBObjects
// *PLEASE* Check KBObjectJena for implementation

public interface KBObject {
	public String getID(); // Concatenation of Namespace and Name, ex:

	// http://www.example.org/owl#Person

	// RDF/OWL based KB's
	public String getNamespace(); // Namespace of object ex:

	// http://www.example.org/owl#

	public String getName(); // LocalName of the object (After the #), ex:

	// Person

	// Literals : KBObject that contains Data values such as numbers or strings
	public Object getValue(); // Example: 4, "hello",..
	public String getValueAsString();
	
	public String getDataType(); // Example:
	// http://www.w3.org/2001/XMLSchema#integer

	public void setDataType(String type);
	
	public boolean isLiteral(); // Check if this is a literal or not

	public boolean isList();

	// For use by respective KBAPI implementations to manipulate internal
	// structures
	// For example: if using Jena, the internal resource would be a Jena
	// "Resource", etc.
	Object getInternalNode();

	void setInternalNode(Object res);

	public boolean isAnonymous();
	
	// Non-critical functions
	public boolean isThing();

	public boolean isNothing(); 

	public boolean isClassificationProperty(); // The RDF "type" property

	public String shortForm();

	public String shortForm(boolean showLiteralTypes);

}
