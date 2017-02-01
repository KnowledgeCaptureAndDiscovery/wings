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

package edu.isi.wings.planner.api;

import java.util.ArrayList;

import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.data.api.DataReasoningAPI;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.template.api.Seed;
import edu.isi.wings.workflow.template.api.Template;

public interface WorkflowGenerationAPI {

	public void useDataService(DataReasoningAPI dc);

	public void useComponentService(ComponentReasoningAPI pc);

	public Seed loadSeed(String seedName);

	public Template loadTemplate(String templateName);

	public Template getInferredTemplate(Template template);

	public ArrayList<Template> specializeTemplates(Template template);

	public ArrayList<VariableBindingsList> selectInputDataObjects(Template specializedTemplate);
	
	public Template bindTemplate(Template specializedTemplate, VariableBindingsList bindings);

	public void setDataMetricsForInputDataObjects(ArrayList<Template> boundTemplates);

	public ArrayList<Template> configureTemplates(Template boundTemplate);
	
	public Template getExpandedTemplate(Template configuredTemplate);

	public ArrayList<String> getExplanations();
	
	public ExecutionPlan getExecutionPlan(Template template);
	
}
