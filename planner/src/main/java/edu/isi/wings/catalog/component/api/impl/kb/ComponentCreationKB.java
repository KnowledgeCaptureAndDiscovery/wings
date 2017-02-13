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
import edu.isi.wings.ontapi.KBTriple;

public class ComponentCreationKB extends ComponentKB implements ComponentCreationAPI {
  ComponentCreationAPI externalCatalog;
  
	public ComponentCreationKB(Properties props, boolean load_concrete) {
		super(props, load_concrete, true, false, true);
		
    String extern = props.getProperty("extern_component_catalog");
    if(extern != null) {
      try {
        Class<?> classz = Class.forName(extern);
        ComponentCreationAPI externalCC = 
            (ComponentCreationAPI) classz.getDeclaredConstructor(Properties.class).newInstance(props);
        this.setExternalCatalog(externalCC);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
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
	public boolean setComponentLocation(String cid, String location) {
		KBObject locprop = this.kb.getProperty(this.pcns + "hasLocation");
		KBObject cobj = this.writerkb.getResource(cid);
		KBObject locobj = writerkb.createLiteral(location);
		this.writerkb.setPropertyValue(cobj, locprop, locobj);
    if(this.externalCatalog != null)
      this.externalCatalog.setComponentLocation(cid, location);
		return true;
	}
	
	@Override
	public boolean updateComponent(Component comp) {
		if(comp == null) return false;
		
		// Remove existing component assertions and re-add the new component details
		try {
		  boolean ok1 = this.removeComponent(comp.getID(), false, false);
		  boolean ok2 = this.addComponent(comp, null);
	    if(this.externalCatalog != null)
	      this.externalCatalog.updateComponent(comp);
	    
	    // TODO: If abstract, update all components defined in all libraries !
	    return ok1 && ok2;		  
		}
		catch (Exception e) {
		  e.printStackTrace();
		  return false;
		}
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
		if(comp.getDocumentation() != null)
			this.setComponentDocumentation(cobj, comp.getDocumentation());
		
    if(comp.getComponentRequirement() != null)
      this.setComponentRequirements(cobj, comp.getComponentRequirement(),
          this.kb, this.writerkb);
    
		if(comp.getLocation() != null)
			this.setComponentLocation(cid, comp.getLocation());
		
		if(comp.getRulesText() != null) {
			this.setComponentRules(cid, comp.getRulesText());
		}
		
		KBObject isConcreteVal = this.writerkb.createLiteral(comp.getType() == Component.CONCRETE);
		this.writerkb.setPropertyValue(cobj, isConcreteProp, isConcreteVal);
		
    if(this.externalCatalog != null)
      this.externalCatalog.addComponent(comp, pholderid);
		return true;
	}

	@Override
	public boolean addComponentHolder(String holderid, String pholderid) {
		writerkb.createClass(holderid, pholderid);
    if(this.externalCatalog != null)
      this.externalCatalog.addComponentHolder(holderid, pholderid);
		return true;
	}
	
	@Override
	public boolean removeComponentHolder(String ctype) {
		KBUtils.removeAllTriplesWith(writerkb, ctype, false);
    if(this.externalCatalog != null)
      this.externalCatalog.removeComponentHolder(ctype);
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
		
		ArrayList<KBObject> sdobjs = this.kb.getPropertyValues(compobj, 
		    this.objPropMap.get("hasSoftwareDependency"));
		for(KBObject obj : sdobjs)
		  for(KBTriple t : this.writerkb.genericTripleQuery(obj, null, null))
		    this.writerkb.removeTriple(t);

		ArrayList<KBObject> hdobjs = this.kb.getPropertyValues(compobj,
        this.objPropMap.get("hasHardwareDependency"));
    for(KBObject obj : hdobjs)
      for (KBTriple t : this.writerkb.genericTripleQuery(obj, null, null))
        this.writerkb.removeTriple(t);
		
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
    if(this.externalCatalog != null)
      this.externalCatalog.removeComponent(cid, remove_holder, unlink);
		return true;
	}

	@Override
	public boolean renameComponent(String oldid, String newid) {
		KBUtils.renameAllTriplesWith(writerkb, this.getComponentHolderId(oldid), 
				this.getComponentHolderId(newid), false);
		KBUtils.renameAllTriplesWith(writerkb, oldid, newid, false);
    if(this.externalCatalog != null)
      this.externalCatalog.renameComponent(oldid, newid);
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
		
		// Change any specified locations of data
		KBObject locProp = this.writerkb.getProperty(this.pcns+"hasLocation");
		ArrayList<KBTriple> triples = 
				this.writerkb.genericTripleQuery(null, locProp, null);
		for(KBTriple t : triples) {
			this.writerkb.removeTriple(t);
			if(t.getObject() == null || t.getObject().getValue() == null)
				continue;
			KBObject comp = t.getSubject();
			String loc = (String) t.getObject().getValue();
			File f = new File(loc);
			loc = this.codedir + File.separator + f.getName();
			this.writerkb.setPropertyValue(comp, locProp, this.writerkb.createLiteral(loc));
		}
		
		KBUtils.renameTripleNamespace(this.writerkb, dckb.dcns, this.dcns);
		KBUtils.renameTripleNamespace(this.writerkb, dckb.pcns, this.pcns);
		KBUtils.renameTripleNamespace(this.writerkb, dckb.dcdomns, this.dcdomns);
		KBUtils.renameTripleNamespace(this.writerkb, dckb.pcdomns, this.pcdomns);
		KBUtils.renameAllTriplesWith(this.writerkb, dckb.pcurl, this.pcurl, false);
		KBUtils.renameAllTriplesWith(this.writerkb, dckb.absurl, this.absurl, false);
		KBUtils.renameAllTriplesWith(this.writerkb, dckb.liburl, this.liburl, false);
		KBUtils.renameAllTriplesWith(this.writerkb, dckb.dconturl, this.dconturl, false);
		
		//FIXME: A hack to get the imported domain's resource namespace. Should be explicit
		String dcreslibns = dckb.liburl.replaceAll("\\/export\\/users\\/.+$", 
		    "/export/common/resource/library.owl#");
		KBUtils.renameTripleNamespace(this.writerkb, dcreslibns, this.resliburl+"#");
		
		this.writerkb.save();
		
		this.initializeAPI(true, true, true);
	}


	@Override
	public void delete() {
		this.writerkb.delete();
	}
	
  
  @Override
  public ComponentCreationAPI getExternalCatalog() {
    return this.externalCatalog;
  }

  @Override
  public void setExternalCatalog(ComponentCreationAPI cc) {
    this.externalCatalog = cc;
    this.externalCatalog.copyFrom(this);
  }
  
	/*
	 * Private helper functions
	 */
	
	private void setComponentDocumentation(KBObject compobj, String doc) {
		KBObject docProp = kb.getProperty(this.pcns + "hasDocumentation");
		kb.setPropertyValue(compobj, docProp, kb.createLiteral(doc));
	}

	private KBObject createRole(ComponentRole role) {
		KBObject argidProp = kb.getProperty(this.pcns + "hasArgumentID");
		KBObject dimProp = kb.getProperty(this.pcns + "hasDimensionality");
		KBObject pfxProp = kb.getProperty(this.pcns + "hasArgumentName");
		KBObject valProp = kb.getProperty(this.pcns + "hasValue");

		String roletype = this.pcns + (role.isParam() ? "ParameterArgument" : "DataArgument");
		KBObject roletypeobj = this.kb.getConcept(roletype);
		KBObject roleobj = writerkb.createObjectOfClass(role.getID(), roletypeobj);
		writerkb.setPropertyValue(roleobj, argidProp, writerkb.createLiteral(role.getRoleName()));
		writerkb.setPropertyValue(roleobj, dimProp, writerkb.createLiteral(role.getDimensionality()));
		writerkb.setPropertyValue(roleobj, pfxProp, writerkb.createLiteral(role.getPrefix()));
		
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
