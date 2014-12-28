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

package edu.isi.wings.catalog.component.classes;

import java.util.ArrayList;

public class ComponentInvocation {
	String componentId;
	String componentLocation;
	String componentDirectory;
	ArrayList<Argument> arguments;

	public ComponentInvocation() {
		this.arguments = new ArrayList<Argument>();
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getComponentLocation() {
		return componentLocation;
	}

	public void setComponentLocation(String componentLocation) {
		this.componentLocation = componentLocation;
	}

  public String getComponentDirectory() {
    return componentDirectory;
  }

  public void setComponentDirectory(String componentDirectory) {
    this.componentDirectory = componentDirectory;
  }

	public ArrayList<Argument> getArguments() {
		return arguments;
	}

	public void setArguments(ArrayList<Argument> arguments) {
		this.arguments = arguments;
	}

	public void addArgument(Argument arg) {
		this.arguments.add(arg);
	}

	public void addArgument(String name, Object value, String varid, boolean isInput) {
		this.arguments.add(new Argument(name, value, varid, isInput));
	}
	
	public class Argument {
		String name;
		String variableid;
		Object value;
		boolean isInput;

		public Argument(String name, Object value, String varid, boolean isInput) {
			this.name = name;
			this.value = value;
			this.variableid = varid;
			this.isInput = isInput;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public boolean isInput() {
			return isInput;
		}

		public void setInput(boolean isInput) {
			this.isInput = isInput;
		}

		public String getVariableid() {
		    return variableid;
		}

		public void setVariableid(String variableid) {
		    this.variableid = variableid;
		}
	}
}
