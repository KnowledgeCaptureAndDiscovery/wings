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

package edu.isi.wings.catalog.component.classes.rules;

import java.util.ArrayList;

public class ComponentRule {
	public static enum Type {
		InversePrecondition, MetadataPropagation, ParameterConfiguration, CollectionConfiguration, Miscellaneous
	};

	String componentId;
	Type type;
	ArrayList<Precondition> preconditions;
	ArrayList<Effect> effects;

	public ComponentRule(String componentId) {
		this.componentId = componentId;
		this.preconditions = new ArrayList<Precondition>();
		this.effects = new ArrayList<Effect>();
	}
	
	public ComponentRule(String componentId, Type type) {
		this(componentId);
		this.type = type;
	}
	
	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	public String getComponentId() {
		return this.componentId;
	}
	
	public void setComponentId(String id) {
		this.componentId = id;
	}
	
}
