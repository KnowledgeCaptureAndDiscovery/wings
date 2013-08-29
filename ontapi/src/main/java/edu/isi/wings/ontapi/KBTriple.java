package edu.isi.wings.ontapi;

import java.util.ArrayList;

public interface KBTriple {

	public ArrayList<KBObject> toArrayList();

	public KBObject getSubject();

	public void setSubject(KBObject subject);

	public KBObject getPredicate();

	public void setPredicate(KBObject predicate);

	public KBObject getObject();

	public void setObject(KBObject object);

	public String shortForm();

	public String fullForm();

}
