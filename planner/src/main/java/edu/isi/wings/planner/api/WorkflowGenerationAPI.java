package edu.isi.wings.planner.api;

import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.data.api.DataReasoningAPI;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.template.api.Seed;
import edu.isi.wings.workflow.template.api.Template;

import java.util.ArrayList;

public interface WorkflowGenerationAPI {

	public void useDataService(DataReasoningAPI dc);

	public void useComponentService(ComponentReasoningAPI pc);

	public Seed loadSeed(String seedName);

	public Template loadTemplate(String templateName);

	public Template getInferredTemplate(Template template);

	public ArrayList<Template> specializeTemplates(Template template);

	public ArrayList<Template> selectInputDataObjects(Template specializedTemplate);

	public void setDataMetricsForInputDataObjects(ArrayList<Template> boundTemplates);

	public ArrayList<Template> configureTemplates(Template boundTemplate);
	
	public Template getExpandedTemplate(Template configuredTemplate);

	public ArrayList<String> getExplanations();
	
	public ExecutionPlan getExecutionPlan(Template template);
	
}
