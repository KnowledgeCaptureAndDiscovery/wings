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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentHolder;
import edu.isi.wings.catalog.component.classes.ComponentRole;
import edu.isi.wings.catalog.component.classes.ComponentTree;
import edu.isi.wings.catalog.component.classes.ComponentTreeNode;
import edu.isi.wings.common.kb.KBUtils;

public class ComponentCreationKB extends ComponentKB implements ComponentCreationAPI {
  ComponentCreationAPI externalCatalog;
  
	public ComponentCreationKB(Properties props) {
		super(props, true, false, true);
		
		// Separate abstract and concrete if an older version of the catalog
		int catVersion = this.getCatalogVersion();
		if(catVersion < 1) {
		  this.separateAbstractAndConcrete();
		  this.setCatalogVersion(1);
		}
		
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
	
  // Cleanup code
  private void separateAbstractAndConcrete() {
    try {
      // Get all component information
      ComponentTree tree = this.getComponentHierarchy(true);
      //System.out.println(tree);

      // Delete existing
      this.delete();
      
      // Move components to the respective writers (abstract, or concrete library)
      ArrayList<ComponentTreeNode> queue = new ArrayList<ComponentTreeNode>();
      queue.add(tree.getRoot());
      HashMap<String, String> parents = new HashMap<String, String>();
      while(!queue.isEmpty()) {
        ComponentTreeNode tn = queue.remove(0);
        ComponentHolder cholder = tn.getCls();
        String parentId = parents.get(cholder.getID());
        if(cholder.getComponent() == null) {
          // Add category
          //System.out.println("Add component holder for "+cholder.getID() + " with parent " + parentId);
          this.addComponentHolder(cholder.getID(), parentId, false, this.abs_writerkb);
        }
        else {
          Component c = cholder.getComponent();
          KBAPI writerkb = c.isConcrete() ? this.lib_writerkb : this.abs_writerkb;
          // Add component
          //System.out.println("Add component " + c.getID() + " : " + c.getType() + " with parent " + parentId);;
          this.addComponent(c, parentId, writerkb); 
        }
        for(ComponentTreeNode childn : tn.getChildren()) {
          parents.put(childn.getCls().getID(), cholder.getID());
          queue.add(childn);
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
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

		ArrayList<String> tbd = new ArrayList<String>();

		this.start_read();
		boolean batchok = this.start_batch_operation();
		
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
				  tbd.add(compobj.getID());
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
		if(batchok)
		  this.stop_batch_operation();
		
		this.end();
		
		// cleanup hack (for components that have the top class as the holder)
		for(String compid: tbd) {
		  this.removeComponent(compid, true, true, false);
		}

		ComponentTree tree = new ComponentTree(rootnode);
		return tree;
	}

  @Override
  public boolean setComponentLocation(String cid, String location) {
    KBAPI writerkb = this.getWriterKB(cid);
    return this.setComponentLocation(cid, location, writerkb);

  }
  
  protected boolean setComponentLocation(String cid, String location, KBAPI writerkb) {
    boolean new_transaction = false;
    if (!this.is_in_transaction()) {
      this.start_write();
      new_transaction = true;
    }
    try {
      KBObject locprop = this.kb.getProperty(this.pcns + "hasLocation");
      KBObject cobj = writerkb.getResource(cid);
      KBObject locobj = writerkb.createLiteral(location);
      writerkb.setPropertyValue(cobj, locprop, locobj);
      if (this.externalCatalog != null)
        this.externalCatalog.setComponentLocation(cid, location);
      if (new_transaction)
        this.save();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (new_transaction) {
        this.end();
      }
    }
    return false;
  }

  @Override
  public boolean setComponentVersion(String cid, int version) {
    KBAPI writerkb = this.getWriterKB(cid);
    return this.setComponentVersion(cid, version, writerkb);
  }
  
  protected boolean setComponentVersion(String cid, int version, KBAPI writerkb) {
    boolean new_transaction = false;
    if (!this.is_in_transaction()) {
      this.start_write();
      new_transaction = true;
    }
    try {
      KBObject versionProp = this.kb.getProperty(this.pcns + "hasVersion");
      KBObject cobj = writerkb.getResource(cid);
      KBObject versionobj = writerkb.createLiteral(version);
      writerkb.setPropertyValue(cobj, versionProp, versionobj);
      if (new_transaction)
        this.save();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (new_transaction) {
        this.end();
      }
    }
    return false;
  }


  public boolean setModelCatalogIdentifier(String cid, String modelIdentifier) {
    KBAPI writerkb = this.getWriterKB(cid);
    return this.setModelCatalogIdentifier(cid, modelIdentifier, writerkb);
  }
  
  protected boolean setModelCatalogIdentifier(String cid, String modelIdentifier, KBAPI writerkb) {
    boolean new_transaction = false;
    if (!this.is_in_transaction()) {
      this.start_write();
      new_transaction = true;
    }
    try {
      KBObject modelIdProp = this.kb.getProperty(this.pcns + "source");
      KBObject cobj = writerkb.getResource(cid);
      KBObject locobj = writerkb.createLiteral(modelIdentifier);
      writerkb.setPropertyValue(cobj, modelIdProp, locobj);
      if (this.externalCatalog != null)
        this.externalCatalog.setComponentLocation(cid, modelIdentifier);
      if (new_transaction)
        this.save();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (new_transaction) {
        this.end();
      }
    }
    return false;
  }


	@Override
	public boolean updateComponent(Component comp) {
		if(comp == null) return false;
		
		// Remove existing component assertions and re-add the new component details
		try {
		  boolean ok1 = this.removeComponent(comp.getID(), false, false, false);		  
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
  public boolean incrementComponentVersion(String cid) {
    KBAPI writerkb = this.getWriterKB(cid);
    return this.incrementComponentVersion(cid, writerkb);
  }
	
	protected boolean incrementComponentVersion(String cid, KBAPI writerkb) {
    try {
      this.start_write();
      KBObject compobj = kb.getIndividual(cid);      
      KBObject versionProp = kb.getProperty(this.pcns + "hasVersion");
      KBObject versionVal = kb.getPropertyValue(compobj, versionProp);
      int currentVersion = (Integer) (versionVal.getValue() != null ? versionVal.getValue() : 0);
      
      int newVersion = currentVersion+1;
      KBObject cobj = writerkb.getResource(cid);
      KBObject newVersionVal = writerkb.createLiteral(newVersion);
      writerkb.setPropertyValue(cobj, versionProp, newVersionVal);
      return this.save();
    }
    catch(Exception e) {
      e.printStackTrace();
      this.end();
    }
    return false;	  
	}

	
	@Override
	public boolean save() {
	  boolean ok1 = false, ok2 = false;
    if(abs_writerkb != null)
      ok1 = this.save(abs_writerkb);	  
		if(lib_writerkb != null)
			ok2 = this.save(lib_writerkb);
		return ok1 && ok2;
	}

	@Override
	public boolean addComponent(Component comp, String pholderid) {
	  KBAPI writerkb = this.getWriterKB((comp.getType() == Component.CONCRETE));
	  return this.addComponent(comp, pholderid, writerkb);
	}
	  
	protected boolean addComponent(Component comp, String pholderid, KBAPI writerkb) {  
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
		
		try {
  		this.start_write();
  		
  		// If parent holder passed in, create a holder as subclass of parent holder
  		// Else assume that current holder already exists and fetch that
  		KBObject cls;
  		if(pholderid != null)
  			cls = writerkb.createClass(cholderid, pholderid);
  		else
  			cls = kb.getConcept(cholderid);
  
  		KBObject cobj = writerkb.createObjectOfClass(cid, cls);
  
  		KBObject inProp = kb.getProperty(this.pcns + "hasInput");
  		KBObject outProp = kb.getProperty(this.pcns + "hasOutput");
  
  		for (ComponentRole role : comp.getInputs()) {
  			role.setID(cid + "_" + role.getRoleName()); // HACK: role id is <compid>_<rolename/argid>
  			KBObject roleobj = this.createRole(role, writerkb);
  			if(roleobj == null)
  				return false;
  			writerkb.addTriple(cobj, inProp, roleobj);
  		}
  		for (ComponentRole role : comp.getOutputs()) {
  			role.setID(cid + "_" + role.getRoleName());
  			KBObject roleobj = this.createRole(role, writerkb);
  			if(roleobj == null)
  				return false;
  			writerkb.addTriple(cobj, outProp, roleobj);
  		}
  
  		if(comp.getSource() != null){
  			this.setModelCatalogIdentifier(cid, comp.getSource(), writerkb);
  		}
  		if(comp.getDocumentation() != null)
  			this.setComponentDocumentation(cobj, comp.getDocumentation(), writerkb);
  		
      if(comp.getComponentRequirement() != null)
        this.setComponentRequirements(cobj, comp.getComponentRequirement(), writerkb);
      
  		if(comp.getLocation() != null)
  			this.setComponentLocation(cid, comp.getLocation(), writerkb);
  		
  		if(comp.getRulesText() != null) {
  			this.setComponentRules(cid, comp.getRulesText(), writerkb);
  		}
  		
  		this.setComponentVersion(cid, comp.getVersion(), writerkb);
  		
      KBObject isConcreteProp = kb.getProperty(this.pcns + "isConcrete");
  		KBObject isConcreteVal = writerkb.createLiteral(comp.getType() == Component.CONCRETE);
  		writerkb.setPropertyValue(cobj, isConcreteProp, isConcreteVal);
  		
      if(this.externalCatalog != null)
        this.externalCatalog.addComponent(comp, pholderid);
  
  		return this.save();
		}
    finally {
      this.end();
    }
	}

	@Override
	public boolean addComponentHolder(String holderid, String pholderid, boolean is_concrete) {
    KBAPI writerkb = this.getWriterKB(is_concrete);
    return this.addComponentHolder(holderid, pholderid, is_concrete, writerkb);
	}
	
	protected boolean addComponentHolder(String holderid, String pholderid, boolean is_concrete, KBAPI writerkb) {
	  try {
  	  this.start_write();
  		writerkb.createClass(holderid, pholderid);
      if(this.externalCatalog != null)
        this.externalCatalog.addComponentHolder(holderid, pholderid, is_concrete);
  		return this.save();
	  }
    finally {
      this.end();
    }
	}
  
  private void setSubclassParentsToClass(String oldclsid, String newclsid) {
    KBObject oldcls = this.kb.getConcept(oldclsid);
    for(KBObject subcls: kb.getSubClasses(oldcls, true)) {
      KBObject dum = this.lib_writerkb.getConcept(subcls.getID());
      if(dum != null)
        this.lib_writerkb.setSuperClass(subcls.getID(), newclsid);
      else
        this.abs_writerkb.setSuperClass(subcls.getID(), newclsid);
    }
  }
  
  @Override
  public boolean moveChildComponentsTo(String oldid, String newid) {
    try {
      this.start_write();
      String oldclsid = this.getComponentHolderId(oldid);
      String newclsid = this.getComponentHolderId(newid);
      this.setSubclassParentsToClass(oldclsid, newclsid);
      return this.save();
    }
    finally {
      this.end();
    }
  }
	
	@Override
	public boolean removeComponentHolder(String ctype, boolean is_concrete) {
    KBAPI writerkb = this.getWriterKB(is_concrete);
    return this.removeComponentHolder(ctype, is_concrete, writerkb);
	}
	
	protected boolean removeComponentHolder(String ctype, boolean is_concrete, KBAPI writerkb) {
	  try {
  	  this.start_write();
  	  //this.setSubclassParentsToGrandparents(ctype);
  		KBUtils.removeAllTriplesWith(writerkb, ctype, false);
      if(this.externalCatalog != null)
        this.externalCatalog.removeComponentHolder(ctype, is_concrete);
  		return this.save();
	  }
    finally {
      this.end();
    }
	}

	@Override
	public boolean removeComponent(String cid, boolean remove_holder, boolean unlink, boolean remove_children) {
	  KBAPI writerkb = this.getWriterKB(cid);
	  return this.removeComponent(cid, remove_holder, unlink, remove_children, writerkb);
	}
	  
	protected boolean removeComponent(String cid, boolean remove_holder, boolean unlink, boolean remove_children, KBAPI writerkb) {
	  if(remove_children) {
      String holderid = this.getComponentHolderId(cid);
	    ComponentTree cnode = createComponentHierarchy(holderid, false);
	    for(ComponentTreeNode tn: cnode.getRoot().getChildren()) {
	      String childid = tn.getCls().getComponent().getID();
	      this.removeComponent(childid, remove_holder, unlink, remove_children);
	    }
	  }
	  
	  ArrayList<KBObject> inputobjs, outputobjs, sdobjs, hdobjs;
	  String loc;
	  try {
  	  this.start_read();  	  
  		KBObject compobj = kb.getIndividual(cid);
  		if(compobj == null) {
  		  return false;
  		}
  		inputobjs = this.getComponentInputs(compobj);
  		outputobjs = this.getComponentOutputs(compobj);
      sdobjs = this.kb.getPropertyValues(compobj, 
          this.objPropMap.get("hasSoftwareDependency"));
      hdobjs = this.kb.getPropertyValues(compobj,
          this.objPropMap.get("hasHardwareDependency"));
      loc = this.getComponentLocation(cid);
      this.end();
	  }
	  catch(Exception e) {
	    e.printStackTrace();
      this.end();
      return false;
    }
      
	  try {
      // Remove items from KB
      this.start_write();
      
      // - Remove component class (holder)
      if(remove_holder) {
        String holderid = this.getComponentHolderId(cid);
        //this.setSubclassParentsToGrandparents(holderid);
        KBUtils.removeAllTriplesWith(writerkb, holderid, false);
      }
      // - Remove inputs
  		for (KBObject obj : inputobjs) {
  			KBUtils.removeAllTriplesWith(writerkb, obj.getID(), false);
  		}
  		// - Remove outputs
  		for (KBObject obj : outputobjs) {
  			KBUtils.removeAllTriplesWith(writerkb, obj.getID(), false);
  		}
  		// - Remove software requirement items
  		for(KBObject obj : sdobjs)
  		  for(KBTriple t : writerkb.genericTripleQuery(obj, null, null))
  		    writerkb.removeTriple(t);
  		// - Remove hardware requirement items
      for(KBObject obj : hdobjs)
        for (KBTriple t : writerkb.genericTripleQuery(obj, null, null))
          writerkb.removeTriple(t);
      // - Remove the component itself
  		KBUtils.removeAllTriplesWith(writerkb, cid, false);
  		
  		this.save();
  		this.end();
	  }
    catch(Exception e) {
      e.printStackTrace();
      this.end();
      return false;
    }
  		
	  try {
  		// Remove the component directory
  		if (unlink) {
  			// Remove component if it is in the catalog's component directory
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
        this.externalCatalog.removeComponent(cid, remove_holder, unlink, remove_children);
	  }
	  catch(Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
	}

	@Override
	public boolean renameComponent(String oldid, String newid) {
	  try {
  	  this.start_write();
  	  KBAPI writerkb = this.getWriterKB(oldid);
  		KBUtils.renameAllTriplesWith(writerkb, this.getComponentHolderId(oldid), 
  				this.getComponentHolderId(newid), false);
  		KBUtils.renameAllTriplesWith(writerkb, oldid, newid, false);
      
  		if(this.externalCatalog != null)
        this.externalCatalog.renameComponent(oldid, newid);
  		
      return this.save();
	  }
    finally {
      this.end();
    }
	}


	@Override
	public void copyFrom(ComponentCreationAPI dc) {
		ComponentCreationKB dckb = (ComponentCreationKB)dc;
		
		try {
  		this.start_write();
  		dckb.start_read();
  		
  		this.abs_writerkb.copyFrom(dckb.abs_writerkb);
  		this.lib_writerkb.copyFrom(dckb.lib_writerkb);
  		
      // Namespace rename maps
      HashMap<String, String> nsmap = new HashMap<String, String>();
      nsmap.put(dckb.dcns, this.dcns);
      nsmap.put(dckb.pcns, this.pcns);
      nsmap.put(dckb.dcdomns, this.dcdomns);
      nsmap.put(dckb.pcdomns, this.pcdomns);
  		KBUtils.renameTripleNamespaces(abs_writerkb, nsmap);
  		KBUtils.renameTripleNamespaces(lib_writerkb, nsmap);
  		
  		KBUtils.renameAllTriplesWith(abs_writerkb, dckb.pcurl, this.pcurl, false);
  		KBUtils.renameAllTriplesWith(abs_writerkb, dckb.absurl, this.absurl, false);
  		KBUtils.renameAllTriplesWith(abs_writerkb, dckb.dconturl, this.dconturl, false);
  		
      KBUtils.renameAllTriplesWith(lib_writerkb, dckb.pcurl, this.pcurl, false);
      KBUtils.renameAllTriplesWith(lib_writerkb, dckb.absurl, this.absurl, false);
      KBUtils.renameAllTriplesWith(lib_writerkb, dckb.liburl, this.liburl, false);
      KBUtils.renameAllTriplesWith(lib_writerkb, dckb.dconturl, this.dconturl, false);
      
      // Change any specified locations of data
      KBObject locProp = lib_writerkb.getProperty(this.pcns+"hasLocation");
      ArrayList<KBTriple> triples = 
          lib_writerkb.genericTripleQuery(null, locProp, null);
      for(KBTriple t : triples) {
        lib_writerkb.removeTriple(t);
        if(t.getObject() == null || t.getObject().getValue() == null)
          continue;
        KBObject comp = t.getSubject();
        String loc = (String) t.getObject().getValue();
        File f = new File(loc);
        loc = this.codedir + File.separator + f.getName();
        lib_writerkb.setPropertyValue(comp, locProp, lib_writerkb.createLiteral(loc));
      }
      
  		//FIXME: A hack to get the imported domain's resource namespace. Should be explicit
  		String dcreslibns = dckb.liburl.replaceAll("\\/export\\/users\\/.+$", 
  		    "/export/common/resource/library.owl#");
  		KBUtils.renameTripleNamespace(lib_writerkb, dcreslibns, this.resliburl+"#");
  		this.save();
  		this.end();
      dckb.end();  		
  
  		this.start_read();
  		this.initializeAPI(true, true, true);
		}
    finally {
      this.end();
      dckb.end();
    }
	}

	@Override
	public boolean delete() {
		return 
		    this.start_write() && 
		    this.abs_writerkb.delete() &&
		    this.lib_writerkb.delete() &&
		    this.save() &&
		    this.end();
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
	private void setComponentDocumentation(KBObject compobj, String doc, KBAPI writerkb) {
		KBObject docProp = kb.getProperty(this.pcns + "hasDocumentation");
		writerkb.setPropertyValue(compobj, docProp, writerkb.createLiteral(doc));
	}

	private KBObject createRole(ComponentRole role, KBAPI writerkb) {	  
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
		} else if (!role.isParam() && role.getType() != null) {
			// Write the role type
			KBObject typeobj = kb.getConcept(role.getType());
			if(typeobj != null)
				writerkb.addClassForInstance(roleobj, typeobj);
		}
		return roleobj;
	}

}
