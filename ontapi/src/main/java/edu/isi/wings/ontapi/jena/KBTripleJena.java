package edu.isi.wings.ontapi.jena;

import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;

import java.util.ArrayList;

public class KBTripleJena implements KBTriple {

	public KBObject subject;
	public KBObject predicate;
	public KBObject object;

	public KBTripleJena(KBObject subject, KBObject predicate, KBObject object) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public ArrayList<KBObject> toArrayList() {
		ArrayList<KBObject> result = new ArrayList<KBObject>(3);
		result.add(this.getSubject());
		result.add(this.getPredicate());
		result.add(this.getObject());
		return result;
	}

	public boolean sameAs(KBTriple other) {
		String subjectString = this.getSubject().getID();
		String predicateString = this.getPredicate().getID();
		String objectString = this.getObject().getID();
		return (subjectString.equals(other.getSubject().getID())
				&& predicateString.equals(other.getPredicate().getID()) && objectString
					.equals(other.getObject().getID()));
	}

	public String toString() {
		return this.shortForm();
		// return "KBTripleJena{" +
		// "subject=" + subject +
		// ", predicate=" + predicate +
		// ", object=" + object +
		// '}';
	}

	public String fullForm() {
		return "{" + "subject=" + subject + ", predicate=" + predicate + ", object=" + object + '}';
	}

	/** {@inheritDoc} */
	public KBObject getSubject() {
		return subject;
	}

	/** {@inheritDoc} */
	public void setSubject(KBObject subject) {
		this.subject = subject;
	}

	/** {@inheritDoc} */
	public KBObject getPredicate() {
		return predicate;
	}

	/** {@inheritDoc} */
	public void setPredicate(KBObject predicate) {
		this.predicate = predicate;
	}

	/** {@inheritDoc} */
	public KBObject getObject() {
		return object;
	}

	/** {@inheritDoc} */
	public void setObject(KBObject object) {
		this.object = object;
	}

	public String shortForm() {
		StringBuilder result = new StringBuilder();
		String space = " ";
		result.append("(");
		result.append(this.getSubject().shortForm());
		result.append(space);
		result.append(this.getPredicate().shortForm());
		result.append(space);
		result.append(this.getObject().shortForm());
		result.append(")");
		return result.toString();
	}
}
