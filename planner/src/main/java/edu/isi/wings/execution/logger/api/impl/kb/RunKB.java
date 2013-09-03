package edu.isi.wings.execution.logger.api.impl.kb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Properties;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.execution.engine.classes.ExecutionQueue;
import edu.isi.wings.execution.engine.classes.RuntimeInfo;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.logger.api.ExecutionMonitorAPI;
import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.workflow.plan.PlanFactory;

public class RunKB implements ExecutionLoggerAPI, ExecutionMonitorAPI {
	KBAPI kb;
	KBAPI libkb;

	Properties props;

	String ns;
	String onturl;
	String liburl;
	String newrunurl;
	String tdbRepository;
	OntFactory ontologyFactory;
	Object writerLock;

	protected HashMap<String, KBObject> objPropMap;
	protected HashMap<String, KBObject> dataPropMap;
	protected HashMap<String, KBObject> conceptMap;

	public RunKB(Properties props) {
		this.props = props;
		this.onturl = props.getProperty("ont.execution.url");
		this.liburl = props.getProperty("lib.domain.execution.url");
		this.newrunurl = props.getProperty("domain.executions.dir.url");
		this.tdbRepository = props.getProperty("tdb.repository.dir");

		if (tdbRepository == null) {
			this.ontologyFactory = new OntFactory(OntFactory.JENA);
		} else {
			this.ontologyFactory = new OntFactory(OntFactory.JENA, this.tdbRepository);
		}
		KBUtils.createLocationMappings(props, this.ontologyFactory);
		try {
			this.kb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN, true);
			this.kb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, false, true));
			this.libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
			this.initializeMaps();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeMaps() {
		this.objPropMap = new HashMap<String, KBObject>();
		this.dataPropMap = new HashMap<String, KBObject>();
		this.conceptMap = new HashMap<String, KBObject>();

		for (KBObject prop : this.kb.getAllObjectProperties()) {
			this.objPropMap.put(prop.getName(), prop);
		}
		for (KBObject prop : this.kb.getAllDatatypeProperties()) {
			this.dataPropMap.put(prop.getName(), prop);
		}
		for (KBObject con : this.kb.getAllClasses()) {
			this.conceptMap.put(con.getName(), con);
		}
		if (!dataPropMap.containsKey("hasLog"))
			dataPropMap.put("hasLog", this.kb.createDatatypeProperty(this.onturl + "#hasLog"));
	}

	@Override
	public void startLogging(RuntimePlan exe) {
		synchronized (this.writerLock) {
			try {
				KBAPI tkb = this.ontologyFactory.getKB(OntSpec.PLAIN);
				this.writeExecutionRun(tkb, exe);
				tkb.saveAs(exe.getURL());

				// exe.getPlan().save();

				KBObject exobj = kb.createObjectOfClass(exe.getID(), conceptMap.get("Execution"));
				this.updateRuntimeInfo(kb, exobj, exe.getRuntimeInfo());
				kb.save();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void updateRuntimeInfo(RuntimePlan exe) {
		synchronized (this.writerLock) {
			try {
				KBAPI tkb = this.ontologyFactory.getKB(exe.getURL(), OntSpec.PLAIN);
				this.updateExecutionRun(tkb, exe);
				tkb.save();

				KBObject exobj = kb.getIndividual(exe.getID());
				this.updateRuntimeInfo(kb, exobj, exe.getRuntimeInfo());
				kb.save();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void updateRuntimeInfo(RuntimeStep stepexe) {
		synchronized (this.writerLock) {
			try {
				KBAPI tkb = this.ontologyFactory.getKB(stepexe.getRuntimePlan().getURL(),
						OntSpec.PLAIN);
				this.updateExecutionStep(tkb, stepexe);
				tkb.save();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public ArrayList<RuntimePlan> getRunList() {
		ArrayList<RuntimePlan> rplans = new ArrayList<RuntimePlan>();
		for (KBObject exobj : this.kb.getInstancesOfClass(conceptMap.get("Execution"), true)) {
			RuntimePlan rplan = this.getExecutionRun(exobj, false);
			rplans.add(rplan);
		}
		return rplans;
	}

	@Override
	public RuntimePlan getRunDetails(String runid) {
		KBObject exobj = this.kb.getIndividual(runid);
		try {
			RuntimePlan rplan = this.getExecutionRun(exobj, true);
			return rplan;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean deleteRun(String runid) {
		return this.deleteExecutionRun(runid);
	}

	@Override
	public boolean runExists(String runid) {
		KBObject obj = this.kb.getIndividual(runid);
		if(obj != null)
			return true;
		return false;
	}
	

	@Override
	public void delete() {
		for(RuntimePlan rplan : this.getRunList()) {
			this.deleteRun(rplan.getID());
		}
		this.kb.delete();
	}

	@Override
	public void setWriterLock(Object lock) {
		this.writerLock = lock;
	}

	/*
	 * Private helper functions
	 */
	private KBObject writeExecutionRun(KBAPI tkb, RuntimePlan exe) {
		KBObject exobj = tkb.createObjectOfClass(exe.getID(), conceptMap.get("Execution"));
		KBObject xtobj = tkb.getResource(exe.getExpandedTemplateID());
		KBObject tobj = tkb.getResource(exe.getOriginalTemplateID());
		KBObject pobj = tkb.getResource(exe.getPlan().getID());
		tkb.setPropertyValue(exobj, objPropMap.get("hasExpandedTemplate"), xtobj);
		tkb.setPropertyValue(exobj, objPropMap.get("hasTemplate"), tobj);
		tkb.setPropertyValue(exobj, objPropMap.get("hasPlan"), pobj);
		for (RuntimeStep stepexe : exe.getQueue().getAllSteps()) {
			KBObject stepobj = this.writeExecutionStep(tkb, stepexe);
			tkb.addPropertyValue(exobj, objPropMap.get("hasStep"), stepobj);
		}
		this.updateRuntimeInfo(tkb, exobj, exe.getRuntimeInfo());
		return exobj;
	}

	private RuntimePlan getExecutionRun(KBObject exobj, boolean details) {
		// Create new runtime plan
		RuntimePlan rplan = new RuntimePlan(exobj.getID());
		rplan.setRuntimeInfo(this.getRuntimeInfo(this.kb, exobj));
		RuntimeInfo.Status status = rplan.getRuntimeInfo().getStatus();
		if (details
				|| (status == RuntimeInfo.Status.FAILURE || status == RuntimeInfo.Status.RUNNING)) {
			try {
				KBAPI tkb = this.ontologyFactory.getKB(rplan.getURL(), OntSpec.PLAIN);
				exobj = tkb.getIndividual(rplan.getID());
				// Get execution queue (list of steps)
				ExecutionQueue queue = new ExecutionQueue();
				KBObject exobj_r = tkb.getIndividual(rplan.getID());
				for (KBObject stepobj : tkb.getPropertyValues(exobj_r, objPropMap.get("hasStep"))) {
					RuntimeStep rstep = new RuntimeStep(stepobj.getID());
					rstep.setRuntimeInfo(this.getRuntimeInfo(tkb, stepobj));
					queue.addStep(rstep);
				}
				rplan.setQueue(queue);

				// Get provenance information
				KBObject xtobj = tkb.getPropertyValue(exobj, objPropMap.get("hasExpandedTemplate"));
				KBObject tobj = tkb.getPropertyValue(exobj, objPropMap.get("hasTemplate"));
				KBObject pobj = tkb.getPropertyValue(exobj, objPropMap.get("hasPlan"));
				rplan.setExpandedTemplateID(xtobj.getID());
				rplan.setOriginalTemplateID(tobj.getID());
				rplan.setPlan(PlanFactory.loadExecutionPlan(pobj.getID(), props));

				return rplan;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rplan;
	}

	private boolean deleteExecutionRun(String runid) {
		RuntimePlan rplan = new RuntimePlan(runid);
		try {
			KBAPI tkb = this.ontologyFactory.getKB(rplan.getURL(), OntSpec.PLAIN);
			KBObject exobj = tkb.getIndividual(rplan.getID());
			KBObject xtobj = tkb.getPropertyValue(exobj, objPropMap.get("hasExpandedTemplate"));
			KBObject pobj = tkb.getPropertyValue(exobj, objPropMap.get("hasPlan"));
			tkb.delete();

			ontologyFactory.getKB(new URIEntity(xtobj.getID()).getURL(), OntSpec.PLAIN).delete();
			ontologyFactory.getKB(new URIEntity(pobj.getID()).getURL(), OntSpec.PLAIN).delete();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		KBUtils.removeAllTriplesWith(this.kb, runid, false);
		return this.kb.save();
	}

	private KBObject writeExecutionStep(KBAPI tkb, RuntimeStep stepexe) {
		KBObject exobj = tkb.createObjectOfClass(stepexe.getID(), conceptMap.get("ExecutionStep"));
		this.updateRuntimeInfo(tkb, exobj, stepexe.getRuntimeInfo());
		return exobj;
	}

	private void updateExecutionRun(KBAPI tkb, RuntimePlan exe) {
		KBObject exobj = tkb.getIndividual(exe.getID());
		this.updateRuntimeInfo(tkb, exobj, exe.getRuntimeInfo());
	}

	private void updateExecutionStep(KBAPI tkb, RuntimeStep exe) {
		KBObject exobj = tkb.getIndividual(exe.getID());
		this.updateRuntimeInfo(tkb, exobj, exe.getRuntimeInfo());
	}

	private void updateRuntimeInfo(KBAPI tkb, KBObject exobj, RuntimeInfo rinfo) {
		tkb.setPropertyValue(exobj, dataPropMap.get("hasLog"),
				ontologyFactory.getDataObject(rinfo.getLog()));
		tkb.setPropertyValue(exobj, dataPropMap.get("hasStartTime"),
				this.getXSDDateTime(rinfo.getStartTime()));
		tkb.setPropertyValue(exobj, dataPropMap.get("hasEndTime"),
				this.getXSDDateTime(rinfo.getEndTime()));
		tkb.setPropertyValue(exobj, dataPropMap.get("hasExecutionStatus"),
				ontologyFactory.getDataObject(rinfo.getStatus().toString()));
	}

	private KBObject getXSDDateTime(Date date) {
		if (date == null)
			return null;
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		return ontologyFactory.getDataObject(new XSDDateTime(cal));
	}

	private RuntimeInfo getRuntimeInfo(KBAPI tkb, KBObject exobj) {
		RuntimeInfo info = new RuntimeInfo();
		KBObject sttime = this.kb.getPropertyValue(exobj, dataPropMap.get("hasStartTime"));
		KBObject endtime = this.kb.getPropertyValue(exobj, dataPropMap.get("hasEndTime"));
		KBObject status = this.kb.getPropertyValue(exobj, dataPropMap.get("hasExecutionStatus"));
		KBObject log = this.kb.getPropertyValue(exobj, dataPropMap.get("hasLog"));
		if (sttime != null && sttime.getValue() != null)
			info.setStartTime(((XSDDateTime) sttime.getValue()).asCalendar().getTime());
		if (endtime != null && endtime.getValue() != null)
			info.setEndTime(((XSDDateTime) endtime.getValue()).asCalendar().getTime());
		if (status != null && status.getValue() != null)
			info.setStatus(RuntimeInfo.Status.valueOf((String) status.getValue()));
		if (log != null && log.getValue() != null)
			info.setLog((String) log.getValue());
		return info;
	}

}
