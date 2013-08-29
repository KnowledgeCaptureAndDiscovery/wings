package edu.isi.wings.ontapi.jena.rules;

import com.hp.hpl.jena.reasoner.TriplePattern;

import edu.isi.wings.ontapi.rules.KBRuleObject;
import edu.isi.wings.ontapi.rules.KBRuleTriple;

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
