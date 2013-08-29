package edu.isi.wings.ontapi.jena.rules;

import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;

import edu.isi.wings.ontapi.rules.KBRuleClause;
import edu.isi.wings.ontapi.rules.KBRuleFunctor;
import edu.isi.wings.ontapi.rules.KBRuleTriple;

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
