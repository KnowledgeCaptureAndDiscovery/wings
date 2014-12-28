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

import edu.isi.wings.workflow.template.api.Template;

public class TemplateBinding extends Binding {
	private static final long serialVersionUID = 1L;
	transient private Template t;

	public TemplateBinding(Template t) {
		this.t = t;
		super.obj = t.getID();
	}

	public TemplateBinding(TemplateBinding b) {
		super(b);
	}

	public TemplateBinding(Template[] values) {
		for (Template val : values) {
			this.add(new TemplateBinding(val));
		}
	}

	public Template getTemplate() {
		return this.t;
	}
}
