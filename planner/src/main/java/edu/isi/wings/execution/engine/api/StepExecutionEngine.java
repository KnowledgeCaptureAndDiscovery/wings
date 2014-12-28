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

package edu.isi.wings.execution.engine.api;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;

public interface StepExecutionEngine {
	
	public void execute(RuntimeStep exe, RuntimePlan plan);
	
	public void abort(RuntimeStep exe);
	
	public void setPlanExecutionEngine(PlanExecutionEngine engine);
	
	public PlanExecutionEngine getPlanExecutionEngine();
	
	public void setExecutionLogger(ExecutionLoggerAPI logger);

	public void setExecutionMonitor(ExecutionMonitorAPI monitor);
	
	public void setExecutionResource(ExecutionResourceAPI resource);
	 
	public ExecutionLoggerAPI getExecutionLogger();
	
	public ExecutionMonitorAPI getExecutionMonitor();
	
	public ExecutionResourceAPI getExecutionResource();
}
