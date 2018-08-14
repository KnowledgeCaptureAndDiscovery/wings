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

import java.util.ArrayList;

import edu.isi.wings.ontapi.rules.KBRuleFunctor;
import edu.isi.wings.ontapi.rules.KBRuleObject;

import org.apache.jena.graph.Node;
import org.apache.jena.reasoner.rulesys.Functor;

public class KBRuleFunctorJena implements KBRuleFunctor {
	transient Functor functor;
	String name;
	ArrayList<KBRuleObject> args;
	
	public KBRuleFunctorJena(Functor functor) {
		this.functor = functor;
		this.name = functor.getName();
		this.args = new ArrayList<KBRuleObject>();
		for (Node arg : functor.getArgs()) {
			args.add(new KBRuleObjectJena(arg));
		}
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public ArrayList<KBRuleObject> getArguments() {
		return this.args;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void addArgument(KBRuleObject item) {
		this.args.add(item);
	}

	@Override
	public void setArguments(ArrayList<KBRuleObject> args) {
		this.args = args;
	}

	@Override
	public Object getInternalFunctor() {
		return this.functor;
	}

}
