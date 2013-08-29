package edu.isi.wings.catalog.component.api.impl.kb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentHolder;
import edu.isi.wings.catalog.component.classes.ComponentRole;
import edu.isi.wings.catalog.component.classes.ComponentTree;
import edu.isi.wings.catalog.component.classes.ComponentTreeNode;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.ontapi.KBObject;

public class ComponentCreationKB extends ComponentKB implements ComponentCreationAPI {
	public ComponentCreationKB(Properties props, boolean load_concrete) {
		super(props, load_concrete, true, false);
	}

	@Override
	public ComponentTree getComponentHierarchy(boolean details) {
		return this.createComponentHierarchy(this.topclass, details);
	}

	private ComponentTree createComponentHierarchy(String classid, boolean details) {
		ComponentHolder rootitem = new ComponentHolder(classid);
		ComponentTreeNode rootnode = new ComponentTreeNode(rootitem);
		ArrayList<ComponentTreeNode> queue = new ArrayList<ComponentTreeNode>();
		queue.add(rootnode);

		while (!queue.isEmpty()) {
			ComponentTreeNode node = queue.remove(0);
			ComponentHolder cls = node.getCls();
			if (cls.getID() == null)
				continue;
			
			KBObject clsobj = kb.getConcept(cls.getID());
			if(clsobj == null) continue;
			
			ArrayList<KBObject> compobjs = kb.getInstancesOfClass(clsobj, true);
			for (KBObject compobj : compobjs) {
				if(cls.getID().equals(this.topclass)) {
					// The top class cannot be used as a holder. If we find any
					// components having the top class as the holder, we delete them
					this.removeComponent(compobj.getID(), true, true);
				}
				else {
					// Add the component as the holder's component
					cls.setComponent(this.getComponent(compobj.getID(), details));
				}
			}
			
			ArrayList<KBObject> subclasses = this.kb.getSubClasses(clsobj, true);
			for (KBObject subcls : subclasses) {
				if (!subcls.getNamespace().equals(this.pcdomns)
						&& !subcls.getNamespace().equals(this.pcns))
					continue;
				ComponentHolder clsitem = new ComponentHolder(subcls.getID());
				ComponentTreeNode childnode = new ComponentTreeNode(clsitem);
				node.addChild(childnode);
				queue.add(childnode);
			}
		}
		ComponentTree tree = new ComponentTree(rootnode);
		return tree;
	}

	@Override
	public Component getComponent(String cid, boolean details) {
		KBObject compobj = kb.getIndividual(cid);
		if(compobj == null) return null;
		
		KBObject concobj = kb.getDatatypePropertyValue(compobj, this.dataPropMap.get("isConcrete"));
		boolean isConcrete = false;
		if(concobj != null && concobj.getValue() != null)
			isConcrete = ((Boolean) concobj.getValue()).booleanValue();
		int ctype = isConcrete ? Component.CONCRETE : Component.ABSTRACT;

		Component comp = new Component(compobj.getID(), ctype);
		if (isConcrete) {
			comp.setLocation(this.getComponentLocation(cid));
		}

		if (details) {
			ArrayList<KBObject> inobjs = this.getComponentInputs(compobj);
			for (KBObject inobj : inobjs) {
				comp.addInput(this.getRole(inobj));
			}
			ArrayList<KBObject> outobjs = this.getComponentOutputs(compobj);
			for (KBObject outobj : outobjs) {
				comp.addOutput(this.getRole(outobj));
			}
			comp.setRules(this.getDirectComponentRules(cid));
			comp.setInheritedRules(this.getInheritedComponentRules(cid));
		}
		return comp;
	}
	
	@Override
	public boolean setComponentLocation(String cid, String location) {
		KBObject locprop = this.kb.getProperty(this.pcns + "hasLocation");
		KBObject cobj = this.writerkb.getResource(cid);
		KBObject locobj = ontologyFactory.getDataObject(location);
		this.writerkb.setPropertyValue(cobj, locprop, locobj);
		return true;
	}
	
	@Override
	public boolean updateComponent(Component comp) {
		if(comp == null) return false;
		
		// Remove existing component assertions and re-add the new component details
		boolean ok1 = this.removeComponent(comp.getID(), false, false);
		boolean ok2 = this.addComponent(comp, null);
		
		// TODO: If abstract, update all components defined in all libraries !
		return ok1 && ok2;
	}

	@Override
	public boolean save() {
		if(this.writerkb != null)
			return this.writerkb.save();
		return false;
	}
	
	@Override
	public void end() {
		if(this.kb != null)
			this.kb.end();
		if(this.writerkb != null)
			this.writerkb.end();
	}

	@Override
	public boolean addComponent(Component comp, String pholderid) {		
		// Check for uniqueness of the role names passed in
		HashSet<String> unique = new HashSet<String>();
		for (ComponentRole role : comp.getInputs())
			unique.add(role.getRoleName());
		for (ComponentRole role : comp.getOutputs())
			unique.add(role.getRoleName());
		
		// If there are some duplicate role ids, return false
		if(unique.size() < (comp.getInputs().size() + comp.getOutputs().size()))
			return false;

		String cid = comp.getID();
		String cholderid = this.getComponentHolderId(cid);
		
		// If parent holder passed in, create a holder as subclass of parent holder
		// Else assume that current holder already exists and fetch that
		KBObject cls;
		if(pholderid != null)
			cls = writerkb.createClass(cholderid, pholderid);
		else
			cls = kb.getConcept(cholderid);

		KBObject cobj = this.writerkb.createObjectOfClass(cid, cls);

		KBObject inProp = kb.getProperty(this.pcns + "hasInput");
		KBObject outProp = kb.getProperty(this.pcns + "hasOutput");
		KBObject isConcreteProp = kb.getProperty(this.pcns + "isConcrete");

		for (ComponentRole role : comp.getInputs()) {
			role.setID(cid + "_" + role.getRoleName()); // HACK: role id is <compid>_<rolename/argid>
			KBObject roleobj = this.createRole(role);
			if(roleobj == null)
				return false;
			this.writerkb.addTriple(cobj, inProp, roleobj);
		}
		for (ComponentRole role : comp.getOutputs()) {
			role.setID(cid + "_" + role.getRoleName());
			KBObject roleobj = this.createRole(role);
			if(roleobj == null)
				return false;
			this.writerkb.addTriple(cobj, outProp, roleobj);
		}

		if(comp.getLocation() != null)
			this.setComponentLocation(cid, comp.getLocation());
		
		if(comp.getRulesText() != null) {
			this.setComponentRules(cid, comp.getRulesText());
		}
		
		KBObject isConcreteVal = ontologyFactory.getDataObject(comp.getType() == Component.CONCRETE);
		this.writerkb.setPropertyValue(cobj, isConcreteProp, isConcreteVal);
		return true;
	}

	@Override
	public boolean addComponentHolder(String holderid, String pholderid) {
		writerkb.createClass(holderid, pholderid);
		return true;
	}
	
	@Override
	public boolean removeComponentHolder(String ctype) {
		KBUtils.removeAllTriplesWith(writerkb, ctype, false);
		return true;
	}

	@Override
	public boolean removeComponent(String cid, boolean remove_holder, boolean unlink) {
		KBObject compobj = kb.getIndividual(cid);
		if(compobj == null) return false;
		
		//Remove holder
		if(remove_holder) {
			String holderid = this.getComponentHolderId(cid);
			this.removeComponentHolder(holderid);
			/*KBObject holdercls = this.kb.getConcept(holderid);
			if(holdercls == null) return false;
			
			ArrayList<KBObject> subholders = this.kb.getSubClasses(holdercls, false);
			ArrayList<KBObject> subcomps = this.kb.getInstancesOfClass(holdercls, false);
			for(KBObject subholder : subholders) {
				this.removeComponentHolder(subholder.getID());
			}
			for(KBObject subcomp : subcomps) {
				this.removeComponent(subcomp.getID(), false, unlink);
			}*/
		}
		
		ArrayList<KBObject> inputobjs = this.getComponentInputs(compobj);
		ArrayList<KBObject> outputobjs = this.getComponentOutputs(compobj);
		for (KBObject obj : inputobjs) {
			KBUtils.removeAllTriplesWith(writerkb, obj.getID(), false);
		}
		for (KBObject obj : outputobjs) {
			KBUtils.removeAllTriplesWith(writerkb, obj.getID(), false);
		}
		KBUtils.removeAllTriplesWith(writerkb, cid, false);
		
		// Delete the component directory
		if (unlink) {
			// Remove component if it is in the catalog's component directory
			String loc = this.getComponentLocation(cid);
			if(loc != null) {
				File f = new File(loc);
				if(f.getParentFile().getAbsolutePath().equals(this.codedir)) {
					if(f.isDirectory())
						try {
							FileUtils.deleteDirectory(f);
						} catch (IOException e) {
							e.printStackTrace();
						}
					else
						f.delete();
				}
			}
		}
		return true;
	}

	@Override
	public boolean renameComponent(String oldid, String newid) {
		KBUtils.renameAllTriplesWith(writerkb, this.getComponentHolderId(oldid), 
				this.getComponentHolderId(newid), false);
		KBUtils.renameAllTriplesWith(writerkb, oldid, newid, false);
		return true;
	}
	
	@Override
	public String getComponentHolderId(String cid) {
		//Component holder id is <compid>Class
		return cid + "Class";
	}


	@Override
	public void copyFrom(ComponentCreationAPI dc) {
		ComponentCreationKB dckb = (ComponentCreationKB)dc;
		
		this.writerkb.copyFrom(dckb.writerkb);
		KBUtils.renameTripleNamespace(this.writerkb, dckb.dcns, this.dcns);
		KBUtils.renameTripleNamespace(this.writerkb, dckb.pcns, this.pcns);
		KBUtils.renameTripleNamespace(this.writerkb, dckb.dcdomns, this.dcdomns);
		KBUtils.renameTripleNamespace(this.writerkb, dckb.pcdomns, this.pcdomns);
		KBUtils.renameAllTriplesWith(this.writerkb, dckb.pcurl, this.pcurl, false);
		KBUtils.renameAllTriplesWith(this.writerkb, dckb.absurl, this.absurl, false);
		KBUtils.renameAllTriplesWith(this.writerkb, dckb.liburl, this.liburl, false);
		KBUtils.renameAllTriplesWith(this.writerkb, dckb.dconturl, this.dconturl, false);
		this.writerkb.save();
		
		this.initializeAPI(true, true);
	}

	/*
	 * Private helper functions
	 */
	
	private ArrayList<KBObject> getComponentInputs(KBObject compobj) {
		KBObject inProp = kb.getProperty(this.pcns + "hasInput");
		return kb.getPropertyValues(compobj, inProp);
	}

	private ArrayList<KBObject> getComponentOutputs(KBObject compobj) {
		KBObject outProp = kb.getProperty(this.pcns + "hasOutput");
		return kb.getPropertyValues(compobj, outProp);
	}

	private ComponentRole getRole(KBObject argobj) {
		ComponentRole arg = new ComponentRole(argobj.getID());
		KBObject argidProp = kb.getProperty(this.pcns + "hasArgumentID");
		KBObject dimProp = kb.getProperty(this.pcns + "hasDimensionality");
		KBObject pfxProp = kb.getProperty(this.pcns + "hasArgumentName");
		KBObject valProp = kb.getProperty(this.pcns + "hasValue");

		ArrayList<KBObject> alltypes = kb.getAllClassesOfInstance(argobj, true);

		for (KBObject type : alltypes) {
			if (type.getID().equals(this.pcns + "ParameterArgument"))
				arg.setParam(true);
			else if (type.getID().equals(this.pcns + "DataArgument"))
				arg.setParam(false);
			else if (type.getNamespace().equals(this.dcdomns)
					|| type.getNamespace().equals(this.dcns))
				arg.setType(type.getID());
		}
		KBObject role = kb.getPropertyValue(argobj, argidProp);
		KBObject dim = kb.getPropertyValue(argobj, dimProp);
		KBObject pfx = kb.getPropertyValue(argobj, pfxProp);

		if (arg.isParam()) {
			KBObject val = kb.getPropertyValue(argobj, valProp);
			if (val != null) {
				arg.setType(val.getDataType());
				arg.setParamDefaultalue(val.getValue());
			}
		}
		if (role != null && role.getValue() != null)
			arg.setRoleName(role.getValue().toString());
		if (dim != null && dim.getValue() != null)
			arg.setDimensionality((Integer) dim.getValue());
		if (pfx != null && pfx.getValue() != null)
			arg.setPrefix(pfx.getValue().toString());
		return arg;
	}

	private KBObject createRole(ComponentRole role) {
		KBObject argidProp = kb.getProperty(this.pcns + "hasArgumentID");
		KBObject dimProp = kb.getProperty(this.pcns + "hasDimensionality");
		KBObject pfxProp = kb.getProperty(this.pcns + "hasArgumentName");
		KBObject valProp = kb.getProperty(this.pcns + "hasValue");

		String roletype = this.pcns + (role.isParam() ? "ParameterArgument" : "DataArgument");
		KBObject roletypeobj = this.kb.getConcept(roletype);
		KBObject roleobj = writerkb.createObjectOfClass(role.getID(), roletypeobj);
		writerkb.setPropertyValue(roleobj, argidProp, ontologyFactory.getDataObject(role.getRoleName()));
		writerkb.setPropertyValue(roleobj, dimProp, ontologyFactory.getDataObject(role.getDimensionality()));
		writerkb.setPropertyValue(roleobj, pfxProp, ontologyFactory.getDataObject(role.getPrefix()));
		
		if (role.isParam() && role.getType() != null) {
			// Write the parameter default value
			String xsdtype = role.getType();
			Object val = role.getParamDefaultalue();
			String valstr = "";
			if (val != null) {
				if (xsdtype.matches(".*int.*") && val.getClass() == Double.class)
					val = ((Double)val).intValue(); // HACK: gson sometimes converts ints into double
				valstr = val.toString();
			}
			else if (xsdtype.matches(".*int.*"))
				valstr = "0";
			else if (xsdtype.matches(".*bool.*"))
				valstr = "false";
			else if (xsdtype.matches(".*float.*"))
				valstr = "0.0";

			KBObject defobj = writerkb.createXSDLiteral(valstr, xsdtype);
			if(defobj == null)
				return null;
			writerkb.setPropertyValue(roleobj, valProp, defobj);
		} else if (!role.isParam()) {
			// Write the role type
			KBObject typeobj = kb.getConcept(role.getType());
			if(typeobj != null)
				writerkb.addClassForInstance(roleobj, typeobj);
		}
		return roleobj;
	}
}
