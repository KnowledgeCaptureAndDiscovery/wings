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

import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.rules.KBRule;
import edu.isi.wings.ontapi.rules.KBRuleClause;
import edu.isi.wings.ontapi.rules.KBRuleFunctor;
import edu.isi.wings.ontapi.rules.KBRuleObject;
import edu.isi.wings.ontapi.rules.KBRuleTriple;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.TriplePattern;
import org.apache.jena.reasoner.rulesys.ClauseEntry;
import org.apache.jena.reasoner.rulesys.Functor;
import org.apache.jena.reasoner.rulesys.Rule;

public class KBRuleJena implements KBRule {
	transient Rule rule;
	
	String name;
	ArrayList<KBRuleClause> body;
	ArrayList<KBRuleClause> head;

	public KBRuleJena(Rule rule) {
		this.name = rule.getName();
		this.rule = rule;
		this.body = new ArrayList<KBRuleClause>();
		this.head = new ArrayList<KBRuleClause>();
		this.initialize();
	}
	
	public KBRuleJena(String ruleText) {
		this(Rule.parseRule(ruleText));
	}
	
	private void initialize() {
		for (ClauseEntry ce : this.rule.getBody()) {
			body.add(new KBRuleClauseJena(ce));
		}
		for (ClauseEntry ce : rule.getHead()) {
			head.add(new KBRuleClauseJena(ce));
		}
	}

	@Override
	public ArrayList<KBRuleClause> getRuleBody() {
		return this.body;
	}

	@Override
	public ArrayList<KBRuleClause> getRuleHead() {
		return this.head;
	}

	@Override
	public void setRuleBody(ArrayList<KBRuleClause> body) {
		this.body = body;
	}

	@Override
	public void setRuleHead(ArrayList<KBRuleClause> head) {
		this.head = head;
	}

	@Override
	public Object getInternalRuleObject() {
		return this.rule;
	}
	
	@Override
	public String toString() {
	  if(this.rule != null)
	    return this.rule.toString();
	  return null;
	}
	 
	@Override
	public String toShortString() {
	  if(this.rule != null) {
	    String str = this.rule.toString();
	    str = str.replaceAll("http://www.w3.org/2001/XMLSchema#", "xsd:");
	    str = str.replaceAll("'(\\S+?)'\\^\\^xsd:(float|double|integer|int)", "$1");
	    return str;
	  }
	  return null;
	}
	
	@Override
	public void resetInternalRuleObject() {
		ArrayList<ClauseEntry> head = new ArrayList<ClauseEntry>();
		ArrayList<ClauseEntry> body = new ArrayList<ClauseEntry>();
		for(KBRuleClause kbclause : this.head) {
			head.add(this.getClause(kbclause));
		}
		for(KBRuleClause kbclause : this.body) {
			body.add(this.getClause(kbclause));
		}
		this.rule = new Rule(this.name, head, body);
	}
	
	private ClauseEntry getClause(KBRuleClause kbclause) {
		if(kbclause.isFunctor()) {
			KBRuleFunctor kbf = kbclause.getFunctor();
			Node[] args = new Node[kbf.getArguments().size()];
			int i=0;
			for(KBRuleObject kbarg : kbf.getArguments()) {
				args[i] = this.getKBNode(kbarg);
				i++;
			}
			return new Functor(kbf.getName(), args);
		}
		else if(kbclause.isTriple()) {
			KBRuleTriple kbtriple = kbclause.getTriple();
			Node subject = this.getKBNode(kbtriple.getSubject());
			Node predicate = this.getKBNode(kbtriple.getPredicate());
			Node object = this.getKBNode(kbtriple.getObject());
			if(subject != null && predicate != null && object != null)
				return new TriplePattern(subject, predicate, object);
		}
		return null;
	}
	
	private Node getKBNode(KBRuleObject kbobj) {
		if(kbobj.isVariable()) {
			return NodeFactory.createVariable(kbobj.getVariableName());
		}
		else {
			KBObject argobj = kbobj.getKBObject();
			if(argobj.isLiteral()) {
				RDFDatatype type = NodeFactory.getType(argobj.getDataType());
				return NodeFactory.createLiteral(argobj.toString(), type);
			}
			else
				return NodeFactory.createURI(argobj.getID());
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}
