package edu.isi.wings.ontapi.jena.rules;

import com.hp.hpl.jena.graph.Node;

import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.jena.KBObjectJena;
import edu.isi.wings.ontapi.rules.KBRuleObject;

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
