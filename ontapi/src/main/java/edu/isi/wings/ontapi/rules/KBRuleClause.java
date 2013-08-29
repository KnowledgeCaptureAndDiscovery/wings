package edu.isi.wings.ontapi.rules;

public interface KBRuleClause {

	boolean isTriple();
	
	boolean isFunctor();
	
	KBRuleFunctor getFunctor();
	
	KBRuleTriple getTriple();
	
	Object getInternalClause();
	
}
