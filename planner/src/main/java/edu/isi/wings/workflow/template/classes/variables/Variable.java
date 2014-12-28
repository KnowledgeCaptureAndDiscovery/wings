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

package edu.isi.wings.workflow.template.classes.variables;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.workflow.template.classes.sets.Binding;

public class Variable extends URIEntity {
	private static final long serialVersionUID = 1L;

	private Binding binding;
	private String comment;
	private short type;
	private boolean autofill;
	private boolean breakpoint;

	public Variable(String id, short type) {
		super(id);
		this.type = type;
	}

	public void setVariableType(short type) {
		this.type = type;
	}

	public short getVariableType() {
		return this.type;
	}

	public boolean isDataVariable() {
		return type == VariableType.DATA;
	}

	public boolean isParameterVariable() {
		return type == VariableType.PARAM;
	}

	public boolean isComponentVariable() {
		return type == VariableType.COMPONENT;
	}

	public Binding getBinding() {
		return this.binding;
	}

	public void setBinding(Binding binding) {
		this.binding = binding;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String str) {
		this.comment = str;
	}

	public boolean isAutoFill() {
    return autofill;
  }

  public void setAutoFill(boolean autoFill) {
    this.autofill = autoFill;
  }

  public boolean isBreakpoint() {
    return breakpoint;
  }

  public void setBreakpoint(boolean breakpoint) {
    this.breakpoint = breakpoint;
  }

  public String toString() {
		return getID() + (binding != null ? " (" + binding.toString() + ")" : "");
	}
}
