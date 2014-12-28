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

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.workflow.template.classes.Port;
import edu.isi.wings.workflow.template.classes.variables.Variable;

public class Link extends URIEntity {
	private static final long serialVersionUID = 1L;

	private Node fromNode;
	private Node toNode;
	private Port fromPort;
	private Port toPort;
	private Variable variable;

	public Link(String id, Node fromNode, Node toNode, Port fromPort, Port toPort) {
		super(id);
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.fromPort = fromPort;
		this.toPort = toPort;
	}

	public Port getDestinationPort() {
		return toPort;
	}

	public Node getDestinationNode() {
		return toNode;
	}

	public Port getOriginPort() {
		return fromPort;
	}

	public Node getOriginNode() {
		return fromNode;
	}

	public Variable getVariable() {
		return this.variable;
	}

	public void setVariable(Variable v) {
		this.variable = v;
	}

	public boolean isInOutLink() {
		if (this.fromNode != null && this.toNode != null) {
			return true;
		}
		return false;
	}

	public boolean isInputLink() {
		// if (linkType == IN) {
		if (this.fromNode == null && this.toNode != null) {
			return true;
		}
		return false;
	}

	public boolean isOutputLink() {
		// if (linkType == OUT) {
		if (this.fromNode != null && this.toNode == null) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return (fromNode == null ? "" : fromNode + " [" + fromPort + "]  ") + " -> "
				+ (toNode == null ? "" : toNode + " [" + toPort + "]") + "\n" + " (" + variable
				+ ")";
	}

	public void setDestinationPort(Port port) {
		this.toPort = port;
	}

	public void setDestinationNode(Node n) {
		this.toNode = n;
	}

	public void setOriginPort(Port port) {
		this.fromPort = port;
	}

	public void setOriginNode(Node n) {
		this.fromNode = n;
	}
}
