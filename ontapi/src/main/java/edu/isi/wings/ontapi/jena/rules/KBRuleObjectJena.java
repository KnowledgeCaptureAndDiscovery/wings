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

package edu.isi.wings.ontapi.jena.rules;

import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.jena.KBObjectJena;
import edu.isi.wings.ontapi.rules.KBRuleObject;

import org.apache.jena.graph.Node;

public class KBRuleObjectJena implements KBRuleObject {
	transient Node node;
	KBObject obj;
	boolean isVariable = false;
	String variableName;
	
	public KBRuleObjectJena(Node node) {
		this.node = node;
		if(this.node.isVariable()) {
			this.isVariable = true;
			this.variableName = this.node.getName();
		}
		else if(this.node.isLiteral()) {
			obj = new KBObjectJena(this.node.getLiteralValue(), true);
			obj.setDataType(this.node.getLiteralDatatypeURI());
		}
		else if(this.node.isURI()) {
			obj = new KBObjectJena(this.node.getURI());
		}
	}

	@Override
	public Object getInternalNode() {
		return this.node;
	}

	@Override
	public boolean isVariable() {
		return this.isVariable;
	}

	@Override
	public KBObject getKBObject() {
		return this.obj;
	}

	@Override
	public String getVariableName() {
		return this.variableName;
	}

}
