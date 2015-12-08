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

package edu.isi.wings.workflow.template.classes.sets;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;

import edu.isi.wings.catalog.data.classes.metrics.Metrics;

/*
 * A Binding can contain just one URI or a set of URIs
 * 
 */
public class Binding extends WingsSet implements Serializable {
	private static final long serialVersionUID = 1L;

	protected URI id;
	protected Object value;
	protected Metrics metrics = new Metrics();

	protected HashMap<String, Object> data;

	public Binding() {
	}

	public Binding(String id) {
		setID(id);
		this.obj = this.id;
		this.data = new HashMap<String, Object>();
	}

	public Binding(Binding b) {
		super(b);
	}

	public Binding(String[] ids) {
		for (String id : ids) {
			this.add(new Binding(id));
		}
	}

	public String getID() {
		if (id != null)
			return id.toString();
		return null;
	}

	public void setID(String id) {
		try {
			this.id = new URI(id);
			this.obj = this.id;
		} catch (Exception e) {
			System.err.println(id + " Not a URI. Only URIs allowed for Binding IDs");
		}
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public void setValue(Object[] values) {
		for (Object val : values) {
			Binding b = new Binding();
			b.setValue(val);
			this.add(b);
		}
	}

	public String getName() {
		if (id != null)
			return id.getFragment();
		return null;
	}

	public String getNamespace() {
		if (id != null)
			return id.getScheme() + ":" + id.getSchemeSpecificPart() + "#";
		return null;
	}

	public String toString() {
		if (isSet())
			return super.toString();
		return getValue() != null ? getValue().toString() : getName();
		// return (getValue() != null ? getValue().toString() : getName()) +
		// (getData() != null ? "(" + getData().toString() + ")" : "");
	}

	public boolean isValueBinding() {
		return (getValue() != null);
	}

	public boolean isURIBinding() {
		return (getID() != null);
	}

	public void setMetrics(Metrics metrics) {
		this.metrics = metrics;
	}

	public Metrics getMetrics() {
		return this.metrics;
	}

	public void setData(Object data) {
		this.data.put("data", data);
		super.obj = "" + this.id + this.data.toString();
	}

	public Object getData() {
		return this.data.get("data");
	}
	
  public void setData(String key, Object data) {
    this.data.put(key, data);
    super.obj = "" + this.id + this.data.toString();
  }

  public Object getData(String key) {
    return this.data.get(key);
  }

	/*
	 * public Binding clone() { Binding b = (Binding)super.clone(); if(isSet())
	 * { for(int i=0; i<this.size(); i++) { b.add(i,
	 * ((Binding)this.get(i)).clone()); } } return b; }
	 */
	  
}
