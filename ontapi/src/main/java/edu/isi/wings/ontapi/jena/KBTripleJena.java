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

package edu.isi.wings.ontapi.jena;

import java.util.ArrayList;

import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;

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
		return "{" + "subject=" + subject + ", predicate=" + predicate + ", object=" + object.getValueAsString() + '}';
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
