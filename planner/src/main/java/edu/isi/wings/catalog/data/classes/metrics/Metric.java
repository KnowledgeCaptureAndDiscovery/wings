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

package edu.isi.wings.catalog.data.classes.metrics;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

public class Metric implements Serializable {
	private static final long serialVersionUID = 1L;

	private int type;
	private String datatype;
	transient private Object value;
	private Object serialized_value;

	public static int URI = 1;
	public static int LITERAL = 2;

	public Metric(int type, Object value) {
		super();
		this.type = type;
		this.value = value;
	}
	
	public Metric(int type, Object value, String datatype) {
		super();
		this.type = type;
		this.value = value;
		this.datatype = datatype;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getDatatype() {
		return datatype;
	}

	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}

	public String toString() {
		return this.value + "(" + this.type + ")";
	}
	
	 /*
   * XSDDateTime isn't serializable. So converting it to Calendar and back on
   * Serialization
   */
	private void writeObject(java.io.ObjectOutputStream out)
	     throws IOException {
	  this.serialized_value = this.value;
    if (this.value != null &&
        this.value.getClass().getSimpleName().equals("XSDDateTime")) {
      this.serialized_value = ((XSDDateTime) this.value).asCalendar();
      ((Calendar) this.serialized_value).setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    out.defaultWriteObject();
	}

  private void readObject(java.io.ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    this.value = this.serialized_value;
    if (this.value != null
        && this.value.getClass().getSimpleName().equals("GregorianCalendar")) {
      // ((Calendar)this.serialized_value).setTimeZone(TimeZone.getTimeZone("UTC"));
      this.value = new XSDDateTime((Calendar) this.value);
      ((XSDDateTime) this.value).narrowType(XSDDatatype.XSDdate);
    }
  }
}
