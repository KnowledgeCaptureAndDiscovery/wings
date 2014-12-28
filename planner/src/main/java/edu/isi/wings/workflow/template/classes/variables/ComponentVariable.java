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

import edu.isi.wings.catalog.component.classes.requirements.ComponentRequirement;
import edu.isi.wings.workflow.template.api.Template;

public class ComponentVariable extends Variable {
	private static final long serialVersionUID = 1L;

	private boolean isConcrete;
	private Template template;
	private ComponentRequirement requirements;

	public ComponentVariable(String id) {
		super(id, VariableType.COMPONENT);
	}

	public ComponentVariable(Template t) {
		super(t.getID(), VariableType.COMPONENT);
		this.template = t;
	}

	public void setConcrete(boolean isConcrete) {
		this.isConcrete = isConcrete;
	}

	public boolean isConcrete() {
		return this.isConcrete;
		// return (binding != null)
	}

	public Template getTemplate() {
		return this.template;
	}

	public boolean isTemplate() {
		return (this.template != null);
	}

  public ComponentRequirement getRequirements() {
    return requirements;
  }

  public void setRequirements(ComponentRequirement requirements) {
    this.requirements = requirements;
  }
}
