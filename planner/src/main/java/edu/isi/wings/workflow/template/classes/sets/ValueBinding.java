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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ValueBinding extends Binding {
	private static final long serialVersionUID = 1L;

	private String datatype;
	
	public ValueBinding() {
	}

	public ValueBinding(Object value) {
		this.setValue(value);
	}

	public ValueBinding(Object value, String datatype) {
		this.setValue(value);
		this.setDatatype(datatype);
	}
	
	public ValueBinding(ValueBinding b) {
		super(b);
	}

	public ValueBinding(Object[] values) {
		for (Object val : values) {
			this.add(new ValueBinding(val));
		}
	}

	public ValueBinding(Object[] values, String datatype) {
	  for (Object val : values) {
	    this.add(new ValueBinding(val, datatype));
	  }
	}
	 
	public String getDatatype() {
		return datatype;
	}

	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}

	public int hashCode() {
	  if (isSet())
	    return super.hashCode();
	  else if (value != null)
	    return value.hashCode();
	  return 0;
	}
	 
  public String getValueAsString() {
    if(this.value instanceof Date) {
      if(this.datatype == null || this.datatype.endsWith("dateTime")) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        return format.format(this.value);
      }
      else if (this.datatype.endsWith("date")){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(this.value);
      }
    }
    return this.value.toString();
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
