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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Metric implements Serializable {
	private static final long serialVersionUID = 1L;

	private int type;
	private String datatype;
	private Object value;

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
	
  public String getValueAsString() {
    if(this.value instanceof Date) {
      if(this.datatype == null || this.datatype.endsWith("#dateTime")) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        return format.format(this.value);
      }
      else if (this.datatype.endsWith("#date")){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(this.value);
      }
    }
    if(this.value == null)
      return null;
    
    return this.value.toString();
  }	

	public void setValue(Object value) {
		this.value = value;
	}
	
  public void setValueFromString(String value) throws ParseException {
    if(this.datatype != null) {
      if(this.datatype.endsWith("#dateTime")) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        this.value = format.parse(value);
      }
      else if (this.datatype.endsWith("#date")){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        this.value = format.parse(value);
      }
      else if (this.datatype.endsWith("#float")) {
        this.value = Float.parseFloat(value);
      }
      else if (this.datatype.endsWith("#int")) {
        this.value = Integer.parseInt(value);
      }
      else if (this.datatype.endsWith("#boolean")) {
        this.value = Boolean.parseBoolean(value);
      }
      else {
        this.value = value;
      }
    }
    else {
      this.value = value;
    }
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
   * Serialize Date into appropriate values
   */
  private void writeObject(java.io.ObjectOutputStream out)
       throws IOException {
    if (this.value != null &&
        this.value instanceof Date) {
      this.value = this.getValueAsString();
    }
    out.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException,
      ClassNotFoundException, ParseException {
    in.defaultReadObject();
    if (this.value != null
        && this.value instanceof String) {
      this.setValueFromString(this.value.toString());
    }
  }
	
}
