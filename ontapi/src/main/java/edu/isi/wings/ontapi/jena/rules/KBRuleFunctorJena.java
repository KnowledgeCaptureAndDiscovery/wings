package edu.isi.wings.ontapi.jena.rules;

import java.util.ArrayList;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.Functor;

import edu.isi.wings.ontapi.rules.KBRuleFunctor;
import edu.isi.wings.ontapi.rules.KBRuleObject;

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
