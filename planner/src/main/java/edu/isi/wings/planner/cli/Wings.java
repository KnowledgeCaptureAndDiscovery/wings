package edu.isi.wings.planner.cli;

import edu.isi.wings.catalog.component.ComponentFactory;
import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataReasoningAPI;
import edu.isi.wings.catalog.resource.ResourceFactory;
import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.common.kb.PropertiesHelper;
import edu.isi.wings.common.logging.LogEvent;
import edu.isi.wings.execution.engine.ExecutionFactory;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.impl.local.LocalExecutionEngine;
import edu.isi.wings.execution.engine.api.impl.pegasus.dax.DAX;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.planner.api.WorkflowGenerationAPI;
import edu.isi.wings.planner.api.impl.kb.WorkflowGenerationKB;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.plan.api.impl.pplan.PPlan;
import edu.isi.wings.workflow.template.TemplateFactory;
import edu.isi.wings.workflow.template.api.Seed;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.WingsSet;
import edu.isi.wings.workflow.template.classes.variables.Variable;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

/**
 * The Wings command line interface
 */
public class Wings {
	boolean workOnTemplate = false;

	String seedName;
	String templateName;

	String DCDomain, PCDomain, TemplateDomain;

	Logger logger;

	WorkflowGenerationAPI wg;

	ComponentReasoningAPI pc;
	DataReasoningAPI dc;
	ResourceAPI rc;
	
	boolean storeProvenance;

	String requestId;
	String seedId, templateId;

	Seed seed;
	Template template;
	
	Properties props = new Properties();

	public Wings(String requestName, String requestId, String propFile) {
		this.initHelper(requestName, requestId, propFile);
		// Current request is the same as a seed
		this.seedName = requestName;
	}

	public Wings(String requestName, String requestId, String propFile, boolean isTemplate) {
		this.workOnTemplate = isTemplate;
		this.initHelper(requestName, requestId, propFile);

		if (isTemplate)
			this.templateName = requestName;
		else
			this.seedName = requestName;
	}

	private void initHelper(String requestName, String requestId, String propFile) {
		this.requestId = requestId;
		if (this.requestId == null) {
			this.requestId = UuidGen.generateAUuid(requestName);
		}

		PropertiesHelper.loadWingsProperties(propFile);
		logger = PropertiesHelper.getLogger(Wings.class.getName(), this.requestId);

		DCDomain = PropertiesHelper.getDCDomain();
		PCDomain = PropertiesHelper.getPCDomain();
		TemplateDomain = PropertiesHelper.getTemplateDomain();

		storeProvenance = PropertiesHelper.getProvenanceFlag();
	}

	public String getRequestId() {
		return this.requestId;
	}

	public File getLogFile() {
		File f = new File(PropertiesHelper.getProposedLogFileName(requestId));
		return f;
	}

	public ComponentReasoningAPI initializePC() {
		LogEvent event = getEvent(LogEvent.EVENT_WG_INITIALIZE_PC);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.DOMAIN, PCDomain));
		this.props.putAll(ComponentFactory.createLegacyConfiguration());
		pc = ComponentFactory.getReasoningAPI(this.props);
		logger.info(event.createEndLogMsg());
		return pc;
	}

  public ResourceAPI initializeRC() {
    this.props.putAll(ResourceFactory.createLegacyConfiguration());
    rc = ResourceFactory.getAPI(props);
    return rc;
  }

	public void initializeWorkflowGenerator() {
		this.props.putAll(TemplateFactory.createLegacyConfiguration());
		this.props.putAll(DataFactory.createLegacyConfiguration());
		wg = new WorkflowGenerationKB(this.props, dc, pc, rc, requestId);
	}

	public DataReasoningAPI initializeDC() {
		LogEvent event = getEvent(LogEvent.EVENT_WG_INITIALIZE_DC);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.DOMAIN, DCDomain));
		dc = DataFactory.getReasoningAPI(this.props);
		logger.info(event.createEndLogMsg());
		return dc;
	}

	public void setDC(DataReasoningAPI dc) {
		this.dc = dc;
		wg.useDataService(dc);
	}

	public void setPC(ComponentReasoningAPI pc) {
		this.pc = pc;
		wg.useComponentService(pc);
	}

	public void initializeItem() {
		if (this.workOnTemplate)
			initializeTemplate();
		else
			initializeSeed();
	}

	private void initializeSeed() {
		LogEvent event = getEvent(LogEvent.EVENT_WG_LOAD_SEED);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.SEED_NAME, seedName));

		seed = wg.loadSeed(seedName);
		seedId = seed.getID();

		logger.info(event.createLogMsg().addWQ(LogEvent.SEED_ID, seedId));
		logger.info(event.createEndLogMsg());
	}

	private void initializeTemplate() {
		LogEvent event = getEvent(LogEvent.EVENT_WG_LOAD_SEED);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.TEMPLATE, templateName));

		template = wg.loadTemplate(templateName);
		templateId = template.getID();

		logger.info(event.createLogMsg().addWQ(LogEvent.TEMPLATE_ID, templateId));
		logger.info(event.createEndLogMsg());
	}

	private LogEvent getEvent(String evid) {
		return new LogEvent(evid, "Wings", LogEvent.REQUEST_ID, this.requestId);
	}

	public ArrayList<Template> backwardSweep(Seed seed) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_BACKWARD_SWEEP);
		logger.info(event.createStartLogMsg());
		ArrayList<Template> candidateWorkflows = new ArrayList<Template>();

		Template inferredTemplate = wg.getInferredTemplate((Template) seed);
		ArrayList<Template> innerSpecialized = wg.specializeTemplates(inferredTemplate);
		for (Template template : innerSpecialized) {
			template.getRules().addRules(seed.getSeedRules());
			template.setCreatedFrom(seed);
			template.getMetadata().addCreationSource(seed.getName() + "(Specialized)");
			candidateWorkflows.add(template);
		}

		logger.info(event.createLogMsg().addWQ(LogEvent.MSG,
				"Backward Sweep generated " + candidateWorkflows.size() + " from seed."));
		logger.info(event.createEndLogMsg());
		return candidateWorkflows;
	}

	public ArrayList<Template> selectInputData(ArrayList<Template> candidateWorkflows) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_DATA_SELECTION);
		logger.info(event.createStartLogMsg());

		// wg.setCurrentLogEvent(event);
		ArrayList<Template> boundWorkflows = new ArrayList<Template>();
		for (Template candidateWorkflow : candidateWorkflows) {
			ArrayList<Template> innerPartials = wg.selectInputDataObjects(candidateWorkflow);
			for (Template partial : innerPartials) {
				partial.setCreatedFrom(candidateWorkflow);
				partial.getMetadata().addCreationSource(
						candidateWorkflow.getCreatedFrom().getName() + "(Bound)");
				boundWorkflows.add(partial);
			}
		}

		logger.info(event.createLogMsg().addWQ(
				LogEvent.MSG,
				"Select Input Data Objects returned " + boundWorkflows.size() + " templates from "
						+ candidateWorkflows.size() + " templates."));

		logger.info(event.createEndLogMsg());

		return boundWorkflows;
	}

	public void getDataMetricsForInputData(ArrayList<Template> boundWorkflows) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_FETCH_METRICS);
		logger.info(event.createStartLogMsg());

		// wg.setCurrentLogEvent(event);

		wg.setDataMetricsForInputDataObjects(boundWorkflows);
		logger.info(event.createEndLogMsg());
	}

	public void writeDataSelections(ArrayList<Template> boundWorkflows, String file) {
		ArrayList<HashMap<String, String>> dataBindings = new ArrayList<HashMap<String, String>>();
		for (Template boundWorkflow : boundWorkflows) {
			HashMap<String, String> dataBinding = new HashMap<String, String>();
			for (Variable iv : boundWorkflow.getInputVariables()) {
				if (iv.isDataVariable()) {
					dataBinding.put(iv.getName(), iv.getBinding().toString());
				}
			}
			dataBindings.add(dataBinding);
		}
		try {
			PrintStream out = new PrintStream(new FileOutputStream(file));
			out.println(new Gson().toJson(dataBindings));
			out.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		// System.out.println(dataBinding);
	}

	public void writeTemplateRDF(Template t, String file) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(file));
			out.println(t.serialize());
			out.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public ArrayList<Template> forwardSweep(ArrayList<Template> boundWorkflows) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_FORWARD_SWEEP);
		logger.info(event.createStartLogMsg());

		ArrayList<Template> configuredWorkflows = new ArrayList<Template>();
		for (Template boundWorkflow : boundWorkflows) {
			ArrayList<Template> instances = wg.configureTemplates(boundWorkflow);
			if (instances != null) {
				configuredWorkflows.addAll(instances);
			}
		}

		logger.info(event.createEndLogMsg());
		return configuredWorkflows;
	}

	public ArrayList<Template> getExpandedTemplates(ArrayList<Template> configuredTemplates) {
		ArrayList<Template> expansions = new ArrayList<Template>();
		for (Template ct : configuredTemplates) {
			Template expt = wg.getExpandedTemplate(ct);
			if(expt != null)
				expansions.add(expt);
		}
		return expansions;
	}
	
	public RuntimePlan runPlan(ExecutionPlan plan, boolean waitToComplete) {
		try {
			PlanExecutionEngine pengine = ExecutionFactory.createPlanExecutionEngine(
					LocalExecutionEngine.class.getCanonicalName(), this.props);
			RuntimePlan rplan = new RuntimePlan(plan);
			pengine.execute(rplan);
			if(waitToComplete)
				rplan.waitFor();
			return rplan;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void writeParameterSelections(ArrayList<Template> configuredWorkflows, String file) {
		ArrayList<HashMap<String, String>> paramBindings = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, Binding>> paramBindings_b = new ArrayList<HashMap<String, Binding>>();

		for (Template configuredWorkflow : configuredWorkflows) {
			HashMap<String, Binding> paramBinding_b = new HashMap<String, Binding>();
			for (Variable iv : configuredWorkflow.getInputVariables()) {
				if (iv.isParameterVariable() && iv.getBinding() != null) {
					paramBinding_b.put(iv.getName(), iv.getBinding());
				}
			}
			paramBindings_b.add(paramBinding_b);
		}

		while (paramBindings_b.size() > 0) {
			boolean hasSets = false;
			HashMap<String, Binding> paramBinding_b = paramBindings_b.remove(0);
			HashMap<String, String> paramBinding = new HashMap<String, String>();
			for (String varid : paramBinding_b.keySet()) {
				Binding b = paramBinding_b.get(varid);
				if (b.isSet()) {
					for (WingsSet s : b) {
						HashMap<String, Binding> paramBinding_x = new HashMap<String, Binding>(
								paramBinding_b);
						paramBinding_x.put(varid, (Binding) s);
						paramBindings_b.add(paramBinding_x);
					}
					hasSets = true;
				} else
					paramBinding.put(varid, b.toString());
			}
			if (!hasSets)
				paramBindings.add(paramBinding);
		}
		try {
			PrintStream out = new PrintStream(new FileOutputStream(file));
			out.println(new Gson().toJson(paramBindings));
			out.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public ArrayList<ExecutionPlan> getExecutionPlans(ArrayList<Template> configuredWorkflows) {
		ArrayList<ExecutionPlan> plans = new ArrayList<ExecutionPlan>();
		for (Template instance : configuredWorkflows) {
			ExecutionPlan plan = wg.getExecutionPlan(instance);
			if (plan != null) {
				plans.add(plan);
			}
		}
		return plans;
	}

	public void writePlans(ArrayList<PPlan> plans) {
		if (!PropertiesHelper.createDir(PropertiesHelper.getOutputDir())) {
			String tmpdir = System.getProperty("java.io.tmpdir");
			System.err.println("Using temporary directory: " + tmpdir);
			PropertiesHelper.setOutputDir(tmpdir);
		}

		String outputDir = PropertiesHelper.getOutputDir() + "/" + this.requestId;
		new File(outputDir).mkdir();
		for (PPlan plan : plans) {
			String planStr = plan.serialize();
			System.out.println(planStr);
		}
	}
	
	/*
	public ArrayList<DAX> getDaxes(ArrayList<Template> configuredWorkflows) {
		ArrayList<DAX> daxes = new ArrayList<DAX>();
		int i = 1;
		int size = configuredWorkflows.size();
		for (Template instance : configuredWorkflows) {
			DAX dax = ExecutionPlanHelperKB.getDAX(instance, pc, logger, this.requestId);
			if (dax != null) {
				dax.setInstanceId(instance.getID());
				dax.setIndex(i);
				dax.setTotalDaxes(size);
				dax.fillInputMaps(instance.getInputVariables());
				dax.fillIntermediateMaps(instance.getIntermediateVariables());
				dax.fillOutputMaps(instance.getOutputVariables());
				if (dax != null) {
					daxes.add(dax);
				}
				i++;
			}
		}

		ArrayList<String> daxids = new ArrayList<String>();

		// Store the request-id : dax-ids heirarchy in the log
		for (DAX dax : daxes) {
			daxids.add(dax.getID());
		}

		logger.info(LogEvent.createIdHierarchyLogMsg(LogEvent.REQUEST_ID, this.requestId,
				LogEvent.DAX_ID, daxids.iterator()));

		return daxes;
	}*/

	public void writeDaxes(ArrayList<DAX> daxes) {
		if (!PropertiesHelper.createDir(PropertiesHelper.getOutputDir())) {
			String tmpdir = System.getProperty("java.io.tmpdir");
			System.err.println("Using temporary directory: " + tmpdir);
			PropertiesHelper.setOutputDir(tmpdir);
		}

		String outputDir = PropertiesHelper.getOutputDir() + "/" + this.requestId;
		new File(outputDir).mkdir();
		for (DAX dax : daxes) {
			dax.write(outputDir + "/" + dax.getFile());
			if (dax.getOutputFormat() == DAX.SHELL) {
				File file = new File(outputDir + "/" + dax.getFile());
				file.setExecutable(true);
			}
			dax.writeMapping(outputDir + "/" + dax.getFile() + ".map");
		}
	}

	public void writeLogSummary(ArrayList<Template> candidateWorkflows,
			ArrayList<Template> boundWorkflows, ArrayList<Template> configuredWorkflows,
			ArrayList<DAX> daxes) {

		logger.info("Workflow Generation produced " + candidateWorkflows.size()
				+ " candidate workflows: ");
		for (Template template : candidateWorkflows) {
			logger.info("     Candidate Workflow: " + template.getID() + " " + template);
		}

		logger.info("and " + boundWorkflows.size() + " bound workflows: ");
		for (Template template : boundWorkflows) {
			logger.info("     Bound Workflow: " + template.getID() + " " + template);
		}

		logger.info("and " + configuredWorkflows.size() + " configured workflows: ");
		for (Template template : configuredWorkflows) {
			logger.info("     Configured Workflow: " + template.getID() + " " + template);
		}

		logger.info("and " + daxes.size() + " DAXes: ");
		for (DAX dax : daxes) {
			logger.info("     Executable Workflow: " + dax.getFile());
		}
	}

	private ArrayList<Template> randomSelection(ArrayList<Template> items, int num) {
		ArrayList<Template> ret = new ArrayList<Template>();

		Random random = new Random();

		int size = items.size();
		logger.info("Pruning search space by randomly choosing " + num + " out of " + size
				+ " templates");

		int[] indices = new int[size];
		for (int i = 0; i < size; i++) {
			indices[i] = 0;
		}

		for (int i = 0; i < num; i++) {
			boolean unique = false;
			int randomNo = 0;
			while (!unique) {
				randomNo = random.nextInt(size);
				if (indices[randomNo] == 0) {
					unique = true;
				}
				indices[randomNo] = 1;
			}
			logger.info("Choosing " + randomNo);
			ret.add(items.get(randomNo));
		}
		return ret;
	}

	public Seed getSeed() {
		return this.seed;
	}

	public Template getTemplate() {
		return this.template;
	}

	public WorkflowGenerationAPI getWG() {
		return this.wg;
	}

	public static void main(String[] args) {
		HashMap<String, String> options = Arguments.getOptions("Wings", args);
		if (options == null) {
			System.exit(1);
		}

		// Load Wings Properties
		PropertiesHelper.loadWingsProperties(options.get("conf"));

		if (options.get("logdir") != null) {
			PropertiesHelper.setLogDir(options.get("logdir"));
		}

		if (options.get("outputdir") != null) {
			PropertiesHelper.setOutputDir(options.get("outputdir"));
		}

		if (options.get("ontdir") != null) {
			PropertiesHelper.setOntologyDir(options.get("ontdir"));
		}

		String itemid = null;
		boolean isTemplate = false;

		// Check That Seed or Template is provided
		if (options.get("seed") != null) {
			itemid = options.get("seed");
		} else if (options.get("template") != null) {
			itemid = options.get("template");
			isTemplate = true;
		}
		if (itemid == null) {
			System.err.println("Error: Seed or Template Not Specified");
			Arguments.displayUsage("Wings");
			System.exit(1);
		}

		Wings wings = new Wings(itemid, options.get("requestid"), options.get("conf"), isTemplate);

		LogEvent ev = wings.start();
		wings.initializePC();
		wings.initializeRC();
		wings.initializeWorkflowGenerator();
		// Initialize the DC later
		// (DC initialization messes up Jena Maps and we can't load the
		// template)
		wings.setDC(wings.initializeDC());
		wings.initializeItem();

		// -------- Template/Seed Operations -------
		if (options.get("validate") != null) {
			wings.writeTemplateRDF(isTemplate ? wings.template : wings.seed,
					options.get("validate"));
			System.exit(0);
		}

		if (options.get("elaborate") != null) {
			Template it = wings.wg.getInferredTemplate(isTemplate ? wings.template : wings.seed);
			if (it == null) {
				wings.end(ev, 1);
			}
			wings.writeTemplateRDF(it, options.get("elaborate"));
			System.exit(0);
		}

		// ------- The Rest are Only Seed Operations -------
		if (isTemplate) {
			System.err.println("Error: Seed Required ( with -s ) for desired operation");
			Arguments.displayUsage("Wings");
			System.exit(1);
		}

		int trim = 0;
		if (options.get("trim") != null) {
			trim = Integer.parseInt(options.get("trim"));
		} else {
			trim = PropertiesHelper.getTrimmingNumber();
		}

		ArrayList<Template> candidates = wings.backwardSweep(wings.seed);
		if (candidates.size() == 0) {
			wings.end(ev, 1);
		}
		if (trim > 0 && candidates.size() > trim) {
			candidates = wings.randomSelection(candidates, trim);
		}

		ArrayList<Template> bindings = wings.selectInputData(candidates);
		if (bindings.size() == 0) {
			wings.end(ev, 1);
		}
		if (trim > 0 && bindings.size() > trim) {
			bindings = wings.randomSelection(bindings, trim);
		}

		if (options.get("getData") != null) {
			// Write selected data bindings
			wings.writeDataSelections(bindings, options.get("getData"));
			System.exit(0);
		}
		wings.getDataMetricsForInputData(bindings);

		ArrayList<Template> configurations = wings.forwardSweep(bindings);
		if (configurations.size() == 0) {
			wings.end(ev, 1);
		}
		if (trim > 0 && configurations.size() > trim) {
			configurations = wings.randomSelection(configurations, trim);
		}

		if (options.get("getParameters") != null) {
			// Write parameter bindings
			wings.writeParameterSelections(configurations, options.get("getParameters"));
			System.exit(0);
		}

		// Write P-Plans
		ArrayList<ExecutionPlan> plans = wings.getExecutionPlans(configurations);
		if (plans.size() == 0) {
			wings.end(ev, 1);
		}
		// Execution P-Plan
		// wings.writePlans(plans);

		// Write DAXes
		/*ArrayList<DAX> daxes = wings.getDaxes(configurations);
		if (daxes.size() == 0) {
			wings.end(ev, 1);
		}
		wings.writeDaxes(daxes);*/
		// wings.writeLogSummary(candidates, bindings, configurations, daxes);

		wings.end(ev, 0);
	}

	public LogEvent start() {
		LogEvent event = getEvent(LogEvent.EVENT_WG);
		logger.info(event.createStartLogMsg());
		logger.info(event.createLogMsg().add(
				LogEvent.ONTOLOGY_LOCATION,
				"file:" + PropertiesHelper.getOntologyDir() + "/"
						+ PropertiesHelper.getWorkflowOntologyPath()));
		return event;
	}

	public void end(LogEvent event, int exitcode) {
		logger.info(event.createEndLogMsg().add("exitcode", exitcode));
		System.exit(exitcode);
	}
}
