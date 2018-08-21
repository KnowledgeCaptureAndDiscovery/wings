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

package edu.isi.wings.catalog.component.api.impl.oodt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.oodt.cas.filemgr.structs.Element;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.structs.ProductType;
import org.apache.oodt.cas.filemgr.structs.Reference;
import org.apache.oodt.cas.filemgr.structs.exceptions.CatalogException;
import org.apache.oodt.cas.filemgr.structs.exceptions.ConnectionException;
import org.apache.oodt.cas.filemgr.structs.exceptions.RepositoryManagerException;
import org.apache.oodt.cas.filemgr.structs.exceptions.ValidationLayerException;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;
import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentHolder;
import edu.isi.wings.catalog.component.classes.ComponentRole;
import edu.isi.wings.catalog.component.classes.ComponentTree;
import edu.isi.wings.catalog.component.classes.ComponentTreeNode;
import edu.isi.wings.catalog.component.classes.requirements.ComponentRequirement;
import edu.isi.wings.common.URIEntity;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.wings.util.oodt.CurationServiceAPI;

public class ComponentCreationFM implements ComponentCreationAPI {
  XmlRpcFileManagerClient fmclient;
  CurationServiceAPI curatorApi;
  String policy;

  String fmurl;
  String curatorurl;

  String archivedir;

  String ccurl;
  String absurl;
  String liburl;

  OntFactory ontologyFactory;

  private static HashMap<String, String> compHolders = new HashMap<String, String>();
  private static HashMap<String, Metadata> compMeta = new HashMap<String, Metadata>();

  public ComponentCreationFM(Properties props) {
    this.fmurl = props.getProperty("oodt.fmurl");
    this.curatorurl = props.getProperty("oodt.curatorurl");
    this.archivedir = props.getProperty("oodt.archivedir");
    this.policy = props.getProperty("oodt.fmpolicy");

    this.ccurl = props.getProperty("ont.component.url");
    this.absurl = props.getProperty("lib.abstract.url");
    this.liburl = props.getProperty("lib.concrete.url");

    ontologyFactory = new OntFactory(OntFactory.JENA);
    File f = new File(this.archivedir);
    if(!f.exists()) f.mkdirs();

    try {
      this.curatorApi = new CurationServiceAPI(this.curatorurl, this.policy);
      this.fmclient = new XmlRpcFileManagerClient(new URL(this.fmurl));
    } 
    catch (MalformedURLException e) {
      e.printStackTrace();
    } 
    catch (ConnectionException e) {
      e.printStackTrace();
    }
  }

  @Override
  public ComponentTree getComponentHierarchy(boolean details) {
    String rootid = this.absurl + "#Component";
    ComponentHolder rootholder = new ComponentHolder(rootid);
    ComponentTreeNode rootnode = new ComponentTreeNode(rootholder);
    HashMap<String, ComponentTreeNode> tnmap = new HashMap<String, ComponentTreeNode>();
    HashMap<String, String> pmap = this.curatorApi.getParentTypeMap();
    try {
      for(ProductType ptype : this.fmclient.getProductTypes()) {
        String pid = ptype.getProductTypeId();
        String parentid = pmap.get(pid);
        while(parentid != null && !parentid.equals(rootid)) {
          parentid = pmap.get(parentid);
        }
        if(parentid == null) 
          continue;
        ComponentHolder pholder = new ComponentHolder(pid);
        ComponentTreeNode pnode = new ComponentTreeNode(pholder);
        for (Product p : this.fmclient.getProductsByProductType(ptype)) {
          pholder.setComponent(this.getComponent(p.getProductId(), true));
        }
        tnmap.put(ptype.getProductTypeId(), pnode);
        rootnode.addChild(pnode);
      }
      for(String childid: pmap.keySet()) {
        String parentid = pmap.get(childid);
        ComponentTreeNode ptn = tnmap.get(parentid);
        ComponentTreeNode ctn = tnmap.get(childid);
        if(ptn != null && ctn != null) {
          rootnode.removeChild(ctn);
          ptn.addChild(ctn);
        }
      }
      return new ComponentTree(rootnode);
    }
    catch (CatalogException e) {
      e.printStackTrace();
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Component getComponent(String cid, boolean details) {
    try {
      Product prod = this.fmclient.getProductById(cid);
      Metadata meta = this.fmclient.getMetadata(prod);
      int comptype = Component.ABSTRACT; 
      String conc = meta.getMetadata("IsConcrete");
      if(conc != null && conc.equals("true"))
        comptype = Component.CONCRETE;
      Component comp = new Component(cid, comptype);
      for(String input : meta.getAllMetadata("Inputs"))
        comp.addInput(this.getRoleFromString(input));
      for(String output : meta.getAllMetadata("Outputs"))
        comp.addOutput(this.getRoleFromString(output));
      comp.setDocumentation(meta.getMetadata("Documentation"));
      comp.setRules(ontologyFactory.parseRules(meta.getMetadata("Rules")));
      comp.setComponentRequirement(this.getRequirementFromString(
          meta.getMetadata("Requirement")));
      String loc = this.getComponentLocation(cid); 
      comp.setLocation(loc);
      return comp;
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public String getComponentLocation(String cid) {
    try {
      Product prod = this.fmclient.getProductById(cid);
      for(Reference ref : this.fmclient.getProductReferences(prod)) {
        return ref.getDataStoreReference();
      }
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public String getComponentHolderId(String cid) {
    try {
      Product prod = this.fmclient.getProductById(cid);
      return prod.getProductType().getProductTypeId();
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public boolean addComponent(Component comp, String pholderid) {
    Metadata meta = new Metadata();
    ArrayList<String> inputs = new ArrayList<String>();
    ArrayList<String> outputs = new ArrayList<String>();
    for(ComponentRole role : comp.getInputs())
      inputs.add(this.getRoleString(role));
    for(ComponentRole role : comp.getOutputs())
      outputs.add(this.getRoleString(role));
    meta.addMetadata("Inputs", inputs);
    meta.addMetadata("Outputs", outputs);
    meta.addMetadata("IsConcrete", 
        (comp.getType() == Component.CONCRETE) ? "true" : "false");
    if(comp.getDocumentation() != null)
      meta.addMetadata("Documentation", comp.getDocumentation());
    if(comp.getRules() != null)
      meta.addMetadata("Rules", StringUtils.join(comp.getRules(), "\n"));
    ComponentRequirement compreq = comp.getComponentRequirement();
    if(compreq != null)
      meta.addMetadata("Requirement", this.getRequirementString(compreq));

    compHolders.put(comp.getID(), pholderid);
    compMeta.put(comp.getID(), meta);
    return true;
  }

  @Override
  public boolean addComponentHolder(String holderid, String pholderid) {
    String holdername = new URIEntity(holderid).getName();
    String desc = "A product type for "+holdername+" component class";
    String ver = "org.apache.oodt.cas.filemgr.versioning.BasicVersioner";
    String repo = "file://"+this.archivedir;
    ProductType type = new ProductType(holderid, holderid, desc, repo, ver);
    try {
      this.fmclient.addProductType(type);
      if(pholderid != null)
        return this.curatorApi.addParentForProductType(type, pholderid);
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean updateComponent(Component comp) {
    if(comp == null) return false;
    
    String locuri = this.getComponentLocation(comp.getID());
    // Remove existing component assertions and re-add the new component details
    boolean ok1 = this.removeComponent(comp.getID(), false, false);
    boolean ok2 = this.addComponent(comp, null);
    this.setComponentLocation(comp.getID(), locuri);
    
    // TODO: If abstract, update all components defined in all libraries !
    return ok1 && ok2;
  }

  @Override
  public boolean renameComponent(String oldid, String newid) {
    // Not implemented
    return false;
  }

  @Override
  public boolean removeComponent(String cid, boolean remove_holder,
      boolean unlink) {
    try {
      Product prod = this.fmclient.getProductById(cid);
      this.fmclient.removeProduct(prod);
      return true;
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean removeComponentHolder(String holderid) {
    try {
      ProductType type = this.fmclient.getProductTypeById(holderid);
      for(Product prod : this.fmclient.getProductsByProductType(type)) {
        this.fmclient.removeProduct(prod);
      }
      return this.curatorApi.removeProductType(type);
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean setComponentLocation(String cid, String locuri) {
    if(locuri == null) {
      try {
        File f = File.createTempFile("dummy-", "-abstract");
        locuri = f.getAbsolutePath();
      }
      catch (IOException e) {
        return false;
      }
    }
    File locf = new File(locuri);
    // Zip component directory (if it is a directory)
    if(locf.isDirectory())
      locf = this.zipDirectory(locf);    
    if(locf == null || !locf.exists()) 
      return false;

    String holderid = compHolders.get(cid);
    if(holderid == null)
      return false;

    ProductType type;
    try {
      type = this.fmclient.getProductTypeById(holderid);
    } catch (RepositoryManagerException e1) {
      e1.printStackTrace();
      return false;
    }
    String compname = new URIEntity(cid).getName();

    ArrayList<Reference> refs = new ArrayList<Reference>();
    long filesize = locf.length();
    refs.add(new Reference(locf.toURI().toString(), "", filesize));

    Product prod = new Product(compname, type, 
        Product.STRUCTURE_FLAT, Product.STATUS_TRANSFER, refs);
    prod.setProductId(cid);
    try {
      Metadata meta = compMeta.get(cid);
      this.fmclient.ingestProduct(prod, meta, false);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean save() {
    this.fmclient.refreshConfigAndPolicy();
    return true;
  }

  @Override
  public void end() {
    // TODO Auto-generated method stub

  }

  @Override
  public void delete() {
    // TODO Auto-generated method stub

  }

  @Override
  public void copyFrom(ComponentCreationAPI cc) {
    ComponentTree tree = cc.getComponentHierarchy(true);
    ComponentTreeNode root = tree.getRoot();
    this.initialSync(cc, root, null);
    this.save();
  }

  @Override
  public ComponentCreationAPI getExternalCatalog() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setExternalCatalog(ComponentCreationAPI dc) {
    // TODO Auto-generated method stub
  }

  /*
   * Private functions
   */
  private void initialSync(ComponentCreationAPI cc, ComponentTreeNode node, 
      ComponentTreeNode parent) {
    // Replace top DataObject with local namespaced DataObject
    String holderid = node.getCls().getID();
    if(holderid.equals(this.ccurl + "#Component"))
      holderid = this.absurl + "#Component";

    ProductType ptype = null;
    try {
      ptype = this.fmclient.getProductTypeById(holderid);
    } catch (RepositoryManagerException e) {
      ptype = null;
    }

    if (ptype == null) {
      String pholderid = parent != null ? parent.getCls().getID() : null;
      if(pholderid != null && pholderid.equals(this.ccurl + "#Component"))
        pholderid = this.absurl + "#Component";
      this.addComponentHolder(holderid, pholderid);
      this.save();

      if(holderid.equals(this.absurl + "#Component")) {
        try {
          ptype = this.fmclient.getProductTypeById(holderid);
        } catch (RepositoryManagerException e1) {
          e1.printStackTrace();
        }
        if(ptype == null)
          return;

        String[] props = new String[] {"Inputs", "Outputs", "IsConcrete", 
            "Documentation", "Requirement", "Rules"};
        // MD5 ? (Also add it for DataCreationFM)
        ArrayList<Element> elementList = new ArrayList<Element>();
        for(String prop: props) {
          Element el = null;
          try {
            el = this.fmclient.getElementById(prop);
          } catch (ValidationLayerException e) {
            el = new Element(prop, prop, "", "", "Element "+prop, "");
            elementList.add(el);
          }
        }
        this.curatorApi.addElementsForProductType(ptype, elementList);
        this.save();
      }

      Component comp = node.getCls().getComponent();
      if (comp != null) {
        String cid = comp.getID();
        Product prod;
        try {
          prod = this.fmclient.getProductById(comp.getID());
        } catch (CatalogException e) {
          prod = null;
        }
        if (prod == null) {
          this.addComponent(comp, holderid);
          this.setComponentLocation(cid, cc.getComponentLocation(cid));
        }
      }
    }
    for (ComponentTreeNode cnode : node.getChildren()) {
      this.initialSync(cc, cnode, node);
    }
  }

  private String getRoleString(ComponentRole role) {
    String sep = "|";
    return role.getID() + sep + 
        role.getRoleName() + sep + 
        role.getPrefix() + sep +
        role.getType() + sep + 
        role.getDimensionality() + sep +
        role.isParam() + sep +
        role.getParamDefaultalue();
  }

  private ComponentRole getRoleFromString(String str) {
    if(str == null)
      return null;
    String[] vals = str.split("\\|");
    if(vals.length < 7)
      return null;
    ComponentRole role = new ComponentRole(vals[0]);
    role.setRoleName(vals[1]);
    role.setPrefix(vals[2]);
    role.setType(vals[3]);
    role.setDimensionality(Integer.parseInt(vals[4]));
    role.setParam(Boolean.parseBoolean(vals[5]));
    role.setParamDefaultalue(vals[6]);
    return role;
  }

  private String getRequirementString(ComponentRequirement req) {
    String sep = "|";
    return req.getMemoryGB() + sep + 
        req.getStorageGB() + sep + 
        req.isNeed64bit() + sep +
        StringUtils.join(req.getSoftwareIds(), ",");
  }

  private ComponentRequirement getRequirementFromString(String str) {
    if(str == null)
      return null;
    String[] vals = str.split("\\|");
    if(vals.length < 3)
      return null;
    ComponentRequirement req = new ComponentRequirement();
    req.setMemoryGB(Float.parseFloat(vals[0]));
    req.setStorageGB(Float.parseFloat(vals[1]));
    req.setNeed64bit(Boolean.parseBoolean(vals[2]));
    if(vals.length > 3)
      for(String softwareId: vals[3].split(","))
        req.addSoftwareId(softwareId);
    return req;
  }

  private File zipDirectory(File srcFile) {
    try {
      File zipFile = File.createTempFile("comp-", ".zip");
      FileOutputStream fos = new FileOutputStream(zipFile);
      ZipOutputStream zos = new ZipOutputStream(fos);
      this.addDirToArchive(zos, srcFile);
      zos.close();
      return zipFile;
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
    }
    return null;
  }

  private void addDirToArchive(ZipOutputStream zos, File srcFile) {
    File[] files = srcFile.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (files[i].isDirectory()) {
        addDirToArchive(zos, files[i]);
        continue;
      }
      try {
        byte[] buffer = new byte[1024];
        FileInputStream fis = new FileInputStream(files[i]);
        zos.putNextEntry(new ZipEntry(files[i].getName()));
        int length;
        while ((length = fis.read(buffer)) > 0) {
          zos.write(buffer, 0, length);
        }
        zos.closeEntry();
        fis.close();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }
}


