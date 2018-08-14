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

import edu.isi.wings.ontapi.rules.KBRuleObject;
import edu.isi.wings.ontapi.rules.KBRuleTriple;

import org.apache.jena.reasoner.TriplePattern;

public class KBRuleTripleJena implements KBRuleTriple {
	transient TriplePattern triple;
	KBRuleObject subject;
	KBRuleObject predicate;
	KBRuleObject object;
	
	public KBRuleTripleJena(TriplePattern t) {
		this.triple = t;
		this.subject = new KBRuleObjectJena(t.getSubject());
		this.predicate = new KBRuleObjectJena(t.getPredicate());
		this.object = new KBRuleObjectJena(t.getObject());
	}
	
	@Override
	public KBRuleObject getSubject() {
		return this.subject;
	}

	@Override
	public KBRuleObject getPredicate() {
		return this.predicate;
	}

	@Override
	public KBRuleObject getObject() {
		return this.object;
	}

	@Override
	public void setSubject(KBRuleObject item) {
		this.subject = item;
	}

	@Override
	public void setPredicate(KBRuleObject item) {
		this.predicate = item;
	}

	@Override
	public void setObject(KBRuleObject item) {
		this.object = item;
	}

	@Override
	public Object getInternalTriple() {
		return this.triple;
	}

}
