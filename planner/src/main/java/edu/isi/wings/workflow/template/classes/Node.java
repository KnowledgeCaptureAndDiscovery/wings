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

package edu.isi.wings.workflow.template.classes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.workflow.template.classes.sets.ComponentSetCreationRule;
import edu.isi.wings.workflow.template.classes.sets.PortSetCreationRule;
import edu.isi.wings.workflow.template.classes.variables.*;

public class Node extends URIEntity {
	private static final long serialVersionUID = 1L;
	private ComponentVariable componentVariable;

	private String comment;

	private HashMap<String, Port> inputPorts;
	private HashMap<String, Port> outputPorts;

	private PortSetCreationRule prule;
	private ComponentSetCreationRule crule;
	
	private ArrayList<String> machineIds;
	private boolean inactive;
	private boolean skip;
	
	private String derivedFrom;

	public Node(String id) {
		super(id);
		inputPorts = new HashMap<String, Port>();
		outputPorts = new HashMap<String, Port>();
		machineIds = new ArrayList<String>();
		inactive = false;
		skip = false;
	}

	public void setComponentVariable(ComponentVariable componentVariable) {
		this.componentVariable = componentVariable;
	}

	public ComponentVariable getComponentVariable() {
		return componentVariable;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String str) {
		this.comment = str;
	}

	public void addComponentSetRule(ComponentSetCreationRule crule) {
		this.crule = crule;
	}

	public ComponentSetCreationRule getComponentSetRule() {
		return this.crule;
	}

	public void addPortSetRule(PortSetCreationRule prule) {
		this.prule = prule;
	}

	public PortSetCreationRule getPortSetRule() {
		return this.prule;
	}

	public void setInputPorts(HashMap<String, Port> inputPorts) {
		this.inputPorts = inputPorts;
	}

	public void setOutputPorts(HashMap<String, Port> outputPorts) {
		this.outputPorts = outputPorts;
	}

	public void addInputPort(Port inputPort) {
		this.inputPorts.put(inputPort.getID(), inputPort);
	}

	public void addOutputPort(Port outputPort) {
		this.outputPorts.put(outputPort.getID(), outputPort);
	}

	public void deleteInputPort(Port port) {
		this.inputPorts.remove(port.getID());
	}

	public void deleteOutputPort(Port port) {
		this.outputPorts.remove(port.getID());
	}

	public Collection<Port> getInputPorts() {
		return this.inputPorts.values();
	}

	public Collection<Port> getOutputPorts() {
		return this.outputPorts.values();
	}

	public Port findInputPort(String id) {
		return inputPorts.get(id);
	}

	public Port findOutputPort(String id) {
		return outputPorts.get(id);
	}

	public ArrayList<String> getMachineIds() {
    return machineIds;
  }

  public void setMachineIds(ArrayList<String> machineIds) {
    this.machineIds = machineIds;
  }

  public String toString() {
		return this.getID();
	}
  
  public boolean isInactive() {
    return inactive;
  }

  public void setInactive(boolean inactive) {
    this.inactive = inactive;
  }

  public boolean isSkip() {
    return skip;
  }

  public void setSkip(boolean skip) {
    this.skip = skip;
  }

  public String getDerivedFrom() {
    return derivedFrom;
  }

  public void setDerivedFrom(String derivedFrom) {
    this.derivedFrom = derivedFrom;
  }
}
