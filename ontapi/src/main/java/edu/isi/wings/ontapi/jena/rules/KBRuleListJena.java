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

import edu.isi.wings.ontapi.rules.KBRule;
import edu.isi.wings.ontapi.rules.KBRuleList;

import org.apache.jena.reasoner.rulesys.Rule;

public class KBRuleListJena implements KBRuleList {
	ArrayList<KBRule> rules;
	
	public KBRuleListJena() {
		this.rules = new ArrayList<KBRule>();
	}
	
	public KBRuleListJena(String rulesText) {
		this();
		for(Rule rule : Rule.parseRules(rulesText)) {
			rules.add(new KBRuleJena(rule));
		}
	}
	
	@Override
	public ArrayList<KBRule> getRules() {
		return this.rules;
	}

	@Override
	public void addRule(KBRule rule) {
		this.rules.add(rule);
	}

	@Override
	public void setRules(ArrayList<KBRule> rules) {
		this.rules = rules;
	}

	@Override
	public void mergeRules(KBRuleList rulelist) {
		if(rulelist == null)
			return;
		for(KBRule rule : rulelist.getRules())
			this.rules.add(rule);
	}

}
