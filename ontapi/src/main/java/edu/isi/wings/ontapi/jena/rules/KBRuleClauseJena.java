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

import edu.isi.wings.ontapi.rules.KBRuleClause;
import edu.isi.wings.ontapi.rules.KBRuleFunctor;
import edu.isi.wings.ontapi.rules.KBRuleTriple;

import org.apache.jena.reasoner.TriplePattern;
import org.apache.jena.reasoner.rulesys.ClauseEntry;
import org.apache.jena.reasoner.rulesys.Functor;

public class KBRuleClauseJena implements KBRuleClause {
	transient ClauseEntry ce;
	boolean isTriple = false;
	boolean isFunctor = false;
	KBRuleFunctor functor;
	KBRuleTriple triple;
	
	public KBRuleClauseJena(ClauseEntry ce) {
		this.ce = ce;
		this.initialize();
	}
	
	private void initialize() {
		if (ce.getClass().equals(TriplePattern.class)) {
			TriplePattern t = (TriplePattern) ce;
			this.isTriple = true;
			this.triple = new KBRuleTripleJena(t);
		}
		if (ce.getClass().equals(Functor.class)) {
			Functor f = (Functor) ce;
			this.isFunctor = true;
			this.functor = new KBRuleFunctorJena(f);
		}
	}
	
	@Override
	public boolean isTriple() {
		return this.isTriple;
	}

	@Override
	public boolean isFunctor() {
		return this.isFunctor;
	}

	@Override
	public KBRuleFunctor getFunctor() {
		return this.functor;
	}

	@Override
	public KBRuleTriple getTriple() {
		return this.triple;
	}

	@Override
	public Object getInternalClause() {
		return this.ce;
	}

}
