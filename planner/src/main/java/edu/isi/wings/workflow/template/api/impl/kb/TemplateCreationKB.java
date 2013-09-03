package edu.isi.wings.workflow.template.api.impl.kb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;
import edu.isi.wings.workflow.template.classes.ConstraintProperty;

public class TemplateCreationKB implements TemplateCreationAPI {
	String wflowns;
	String liburl;
	String onturl;
	String wdirurl;
	String dcdomns;
	String pcdomns;
	
	OntFactory ontologyFactory;
	KBAPI kb;
	KBAPI writerkb;
	Properties props;
	
	public TemplateCreationKB(Properties props) {
		this.props = props;
		this.onturl = props.getProperty("ont.workflow.url");
		this.liburl = props.getProperty("lib.domain.workflow.url");
		this.wdirurl = props.getProperty("domain.workflows.dir.url");
		this.dcdomns = props.getProperty("ont.domain.data.url") + "#";
		this.pcdomns = props.getProperty("ont.domain.component.ns");
		
		String hash = "#";
		this.wflowns = this.onturl + hash;

		String tdbRepository = (String) props.get("tdb.repository.dir");
		if (tdbRepository == null) {
			this.ontologyFactory = new OntFactory(OntFactory.JENA);
		} else {
			this.ontologyFactory = new OntFactory(OntFactory.JENA, tdbRepository);
		}
       	KBUtils.createLocationMappings(props, this.ontologyFactory);

		this.initializeAPI(false);
	}
	
	private void initializeAPI(boolean create_if_empty) {
		try {
			this.kb = this.ontologyFactory.getKB(liburl, OntSpec.PELLET, create_if_empty);
			this.kb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, create_if_empty, true));
			
			this.writerkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public ArrayList<String> getTemplateList() {
		ArrayList<String> list = new ArrayList<String>();
		if(this.kb == null)
			return list;
		//this.kb.beginread();
		KBObject tconcept = this.kb.getConcept(this.wflowns + "WorkflowTemplate");
		ArrayList<KBObject> tobjs = this.kb.getInstancesOfClass(tconcept, true);
		for(KBObject tobj : tobjs) {
			list.add(tobj.getID());
		}
		return list;
	}

	@Override
	public Template getTemplate(String tplid) {
		return new TemplateKB(this.props, tplid);
	}

	@Override
	public Template createTemplate(String tplid) {
		try {
			return new TemplateKB(this.props, tplid, true);
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void end() {
		if(this.kb != null)
			this.kb.end();
	}
	
	@Override
	public boolean saveTemplate(Template tpl) {
		try {
			// Save Template
			if(!tpl.save())
				return false;
			
			// Add to List if not already there
			KBObject tplobj = this.kb.getIndividual(tpl.getID());
			if(tplobj == null) {
				KBObject tconcept = this.kb.getConcept(this.wflowns + "WorkflowTemplate");
				this.writerkb.createObjectOfClass(tpl.getID(), tconcept);
				return this.writerkb.save();
			}
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean saveTemplateAs(Template tpl, String newid) {
		try {
			// Save Template As
			if(!tpl.saveAs(newid))
				return false;
			
			// Add to List if not already there
			KBObject tplobj = this.kb.getIndividual(newid);
			if(tplobj == null) {
				KBObject tconcept = this.kb.getConcept(this.wflowns + "WorkflowTemplate");
				this.writerkb.createObjectOfClass(newid, tconcept);
				return this.writerkb.save();
			}
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean removeTemplate(Template tpl) {
		try {
			// Delete Template
			if(!tpl.delete())
				return false;
			
			KBObject tplobj = this.kb.getIndividual(tpl.getID());
			if(tplobj != null) {
				//Remove template from kb
				KBObject tconcept = this.kb.getConcept(this.wflowns + "WorkflowTemplate");
				KBObject typeprop = this.kb.getProperty(KBUtils.RDF + "type");
				this.writerkb.removeTriple(tplobj, typeprop, tconcept);
				return this.writerkb.save();
			}
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public ArrayList<ConstraintProperty> getAllConstraintProperties() {
		// Return some hard-coded constraint properties
		// FIXME: These should be read from the KB
		ConstraintProperty samedata = new ConstraintProperty(this.wflowns + "hasSameDataAs", ConstraintProperty.OBJECT);
		samedata.addDomain(this.wflowns + "DataVariable");
		samedata.setRange(this.wflowns + "DataVariable");
		
		ConstraintProperty diffdata = new ConstraintProperty(this.wflowns + "hasDifferentDataFrom", ConstraintProperty.OBJECT);
		diffdata.addDomain(this.wflowns + "DataVariable");
		diffdata.setRange(this.wflowns + "DataVariable");
		
		ConstraintProperty paramvalue = new ConstraintProperty(this.wflowns + "hasParameterValue", ConstraintProperty.DATATYPE);
		paramvalue.addDomain(this.wflowns + "ParameterVariable");

		ConstraintProperty databinding = new ConstraintProperty(this.wflowns + "hasDataBinding", ConstraintProperty.OBJECT);
		databinding.addDomain(this.wflowns + "DataVariable");
		
		ConstraintProperty type = new ConstraintProperty(KBUtils.RDF + "type", ConstraintProperty.OBJECT);
		type.addDomain(this.wflowns + "DataVariable");
		type.addDomain(KBUtils.OWL + "Class");
		
		ConstraintProperty [] list = new ConstraintProperty[] {samedata, diffdata, paramvalue, databinding, type};
		return new ArrayList<ConstraintProperty>(Arrays.asList(list));
	}


	@Override
	public void copyFrom(TemplateCreationAPI tc) {
		TemplateCreationKB tckb = (TemplateCreationKB) tc;

		this.writerkb.copyFrom(tckb.writerkb);
		
		KBUtils.renameTripleNamespace(this.writerkb, tckb.wflowns, this.wflowns);
		KBUtils.renameAllTriplesWith(this.writerkb, tckb.onturl, this.onturl, false);
		KBUtils.renameAllTriplesWith(this.writerkb, tckb.liburl, this.liburl, false);
				
		for(String tplid : tckb.getTemplateList()) {
			// Load and save the template in the latest format
			TemplateKB tpl = (TemplateKB) tckb.getTemplate(tplid);
			tpl.save();
			
			String ntplid = tplid.replace(tckb.wdirurl, this.wdirurl); 
			String tplurl = tplid.replaceAll("#.*$", "");
			String ntplurl = tplurl.replace(tckb.wdirurl, this.wdirurl);
			try {
				KBAPI ntplkb = this.ontologyFactory.getKB(ntplurl, OntSpec.PLAIN);
				ntplkb.copyFrom(tpl.getKBCopy(true));
				KBUtils.renameTripleNamespace(ntplkb, tckb.wflowns, this.wflowns);
				KBUtils.renameTripleNamespace(ntplkb, tckb.dcdomns, this.dcdomns);
				KBUtils.renameTripleNamespace(ntplkb, tckb.pcdomns, this.pcdomns);
				
				KBUtils.renameTripleNamespace(ntplkb, tplurl+"#", ntplurl+"#");
				KBUtils.renameAllTriplesWith(ntplkb, tplurl, ntplurl, false);
				KBUtils.renameAllTriplesWith(ntplkb, tckb.onturl, this.onturl, false);
				KBUtils.renameAllTriplesWith(ntplkb, tckb.liburl, this.liburl, false);
				ntplkb.save();
			} catch (Exception e) {
				e.printStackTrace();
			}
			KBUtils.renameAllTriplesWith(this.writerkb, tplid, ntplid, false);
		}

		writerkb.save();
		
		this.initializeAPI(true);

	}

	@Override
	public void delete() {
		for(String tplid : this.getTemplateList()) {
			try {
				this.ontologyFactory.getKB(new URIEntity(tplid).getURL(), OntSpec.PLAIN).delete();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.writerkb.delete();
	}
}
