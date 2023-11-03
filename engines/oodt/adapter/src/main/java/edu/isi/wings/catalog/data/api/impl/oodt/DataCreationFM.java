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

package edu.isi.wings.catalog.data.api.impl.oodt;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.data.classes.DataItem;
import edu.isi.wings.catalog.data.classes.DataTree;
import edu.isi.wings.catalog.data.classes.DataTreeNode;
import edu.isi.wings.catalog.data.classes.MetadataProperty;
import edu.isi.wings.catalog.data.classes.MetadataValue;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.util.oodt.CurationServiceAPI;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
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

public class DataCreationFM implements DataCreationAPI {

  XmlRpcFileManagerClient fmclient;
  CurationServiceAPI curatorApi;
  String policy;

  String fmurl;
  String curatorurl;

  String archivedir;

  String dcurl;
  String liburl;
  String onturl;

  private static HashMap<String, String> prodTypes = new HashMap<
    String,
    String
  >();

  public DataCreationFM(Properties props) {
    this.fmurl = props.getProperty("oodt.fmurl");
    this.curatorurl = props.getProperty("oodt.curatorurl");
    this.archivedir = props.getProperty("oodt.archivedir");
    this.policy = props.getProperty("oodt.fmpolicy");

    this.dcurl = props.getProperty("ont.data.url");
    this.liburl = props.getProperty("lib.domain.data.url");
    this.onturl = props.getProperty("ont.domain.data.url");

    File f = new File(this.archivedir);
    if (!f.exists()) f.mkdirs();

    try {
      this.curatorApi = new CurationServiceAPI(this.curatorurl, this.policy);
      this.fmclient = new XmlRpcFileManagerClient(new URL(this.fmurl));
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (ConnectionException e) {
      e.printStackTrace();
    }
  }

  @Override
  public DataTree getDataHierarchy() {
    return this.getDataHieararchy(false);
  }

  private DataTree getDataHieararchy(boolean typesonly) {
    DataItem rootitem = new DataItem("FileManager", DataItem.DATATYPE);
    DataTreeNode rootnode = new DataTreeNode(rootitem);
    HashMap<String, DataTreeNode> tnmap = new HashMap<String, DataTreeNode>();
    HashMap<String, String> pmap = this.curatorApi.getParentTypeMap();
    try {
      for (ProductType ptype : this.fmclient.getProductTypes()) {
        String pid = ptype.getProductTypeId();
        String parentid = pmap.get(pid);
        while (parentid != null && !parentid.endsWith("#DataObject")) {
          parentid = pmap.get(parentid);
        }
        if (parentid == null && !pid.endsWith("#DataObject")) continue;
        DataItem ptitem = new DataItem(pid, DataItem.DATATYPE);
        DataTreeNode ptitemnode = new DataTreeNode(ptitem);
        for (Product p : this.fmclient.getProductsByProductType(ptype)) {
          DataItem pitem = new DataItem(p.getProductId(), DataItem.DATA);
          DataTreeNode pitemnode = new DataTreeNode(pitem);
          ptitemnode.addChild(pitemnode);
        }
        tnmap.put(ptype.getProductTypeId(), ptitemnode);
        rootnode.addChild(ptitemnode);
      }
      for (String childid : pmap.keySet()) {
        String parentid = pmap.get(childid);
        DataTreeNode ptn = tnmap.get(parentid);
        DataTreeNode ctn = tnmap.get(childid);
        if (ptn != null && ctn != null) {
          rootnode.removeChild(ctn);
          ptn.addChild(ctn);
        }
      }
      return new DataTree(rootnode);
    } catch (CatalogException e) {
      e.printStackTrace();
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public DataTree getDatatypeHierarchy() {
    return this.getDataHieararchy(true);
  }

  @Override
  public DataTree getMetricsHierarchy() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ArrayList<String> getAllDatatypeIds() {
    ArrayList<String> ids = new ArrayList<String>();
    try {
      for (ProductType ptype : this.fmclient.getProductTypes()) {
        for (Product p : this.fmclient.getProductsByProductType(ptype)) {
          ids.add(p.getProductId());
        }
      }
    } catch (CatalogException e) {
      e.printStackTrace();
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return ids;
  }

  @Override
  public MetadataProperty getMetadataProperty(String propid) {
    try {
      Element el = this.fmclient.getElementById(propid);
      MetadataProperty prop = new MetadataProperty(
        el.getElementId(),
        MetadataProperty.DATATYPE
      );
      for (String domid : this.curatorApi.getProductTypeIdsHavingElement(el)) {
        prop.addDomain(domid);
      }
      return prop;
    } catch (ValidationLayerException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public ArrayList<MetadataProperty> getMetadataProperties(
    String dtypeid,
    boolean direct
  ) {
    ArrayList<MetadataProperty> props = new ArrayList<MetadataProperty>();
    try {
      HashMap<String, String> pmap = this.curatorApi.getParentTypeMap();
      String tmptypeid = dtypeid;
      while (true) {
        ProductType type = this.fmclient.getProductTypeById(tmptypeid);
        for (Element el : this.curatorApi.getElementsForProductType(
            type,
            true
          )) {
          MetadataProperty prop = new MetadataProperty(
            el.getElementId(),
            MetadataProperty.DATATYPE
          );
          prop.addDomain(type.getProductTypeId());
          prop.setRange(KBUtils.XSD + "string");
          props.add(prop);
        }
        if (pmap.containsKey(tmptypeid)) tmptypeid =
          pmap.get(tmptypeid); else break;
        if (tmptypeid == null || tmptypeid.equals("")) break;
      }
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return props;
  }

  @Override
  public ArrayList<MetadataProperty> getAllMetadataProperties() {
    ArrayList<MetadataProperty> list = new ArrayList<MetadataProperty>();
    HashMap<String, Boolean> elmap = new HashMap<String, Boolean>();
    try {
      for (ProductType type : this.fmclient.getProductTypes()) {
        for (Element el : this.curatorApi.getElementsForProductType(
            type,
            true
          )) {
          if (elmap.containsKey(el.getElementId())) continue;
          elmap.put(el.getElementId(), true);
          MetadataProperty prop = new MetadataProperty(
            el.getElementId(),
            MetadataProperty.DATATYPE
          );
          prop.addDomain(type.getProductTypeId());
          prop.setRange(KBUtils.XSD + "string");
          list.add(prop);
        }
      }
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return list;
  }

  @Override
  public DataItem getDatatypeForData(String dataid) {
    try {
      Product prod = this.fmclient.getProductById(dataid);
      return new DataItem(
        prod.getProductType().getProductTypeId(),
        DataItem.DATATYPE
      );
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public ArrayList<DataItem> getDataForDatatype(
    String dtypeid,
    boolean direct
  ) {
    ArrayList<DataItem> ditems = new ArrayList<DataItem>();
    try {
      for (Product prod : this.fmclient.getProductsByProductType(
          this.fmclient.getProductTypeById(dtypeid)
        )) {
        ditems.add(
          new DataItem(
            prod.getProductType().getProductTypeId(),
            DataItem.DATATYPE
          )
        );
      }
    } catch (CatalogException e) {
      e.printStackTrace();
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return ditems;
  }

  @Override
  public String getTypeNameFormat(String dtypeid) {
    // TODO Not Supported
    return null;
  }

  @Override
  public String getDataLocation(String dataid) {
    try {
      Product prod = this.fmclient.getProductById(dataid);
      for (Reference ref : this.fmclient.getProductReferences(prod)) {
        return ref.getDataStoreReference();
      }
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public ArrayList<MetadataValue> getMetadataValues(
    String dataid,
    ArrayList<String> propids
  ) {
    ArrayList<MetadataValue> vals = new ArrayList<MetadataValue>();
    try {
      Product prod = this.fmclient.getProductById(dataid);
      Metadata meta = this.fmclient.getMetadata(prod);
      for (String propid : propids) {
        Element el = this.fmclient.getElementById(propid);
        MetadataValue val = new MetadataValue(
          propid,
          meta.getMetadata(el.getElementName()),
          MetadataValue.DATATYPE
        );
        vals.add(val);
      }
    } catch (CatalogException e) {
      e.printStackTrace();
    } catch (ValidationLayerException e) {
      e.printStackTrace();
    }
    return vals;
  }

  @Override
  public boolean addDatatype(String dtypeid, String parentid) {
    String dtypename = new URIEntity(dtypeid).getName();
    String desc = "A product type for " + dtypename + " files";
    String ver = "org.apache.oodt.cas.filemgr.versioning.BasicVersioner";
    String repo = "file://" + this.archivedir;
    ProductType type = new ProductType(dtypeid, dtypeid, desc, repo, ver);
    try {
      this.fmclient.addProductType(type);
      if (parentid != null) return this.curatorApi.addParentForProductType(
          type,
          parentid
        );
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean removeDatatype(String dtypeid) {
    try {
      ProductType type = this.fmclient.getProductTypeById(dtypeid);
      for (Product prod : this.fmclient.getProductsByProductType(type)) {
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
  public boolean renameDatatype(String newtypeid, String oldtypeid) {
    try {
      HashMap<String, String> pmap = this.curatorApi.getParentTypeMap();
      this.addDatatype(
          newtypeid,
          pmap.containsKey(oldtypeid) ? pmap.get(oldtypeid) : null
        );
      this.save();

      ProductType otype = this.fmclient.getProductTypeById(oldtypeid);
      ProductType ntype = this.fmclient.getProductTypeById(newtypeid);
      for (Product prod : this.fmclient.getProductsByProductType(otype)) {
        prod.setProductType(ntype);
        this.fmclient.modifyProduct(prod);
      }
      boolean ok1 =
        this.curatorApi.addElementsForProductType(
            ntype,
            this.curatorApi.getElementsForProductType(otype, true)
          );
      boolean ok2 = this.curatorApi.removeAllElementsForProductType(otype);
      boolean ok3 = this.curatorApi.removeProductType(otype);
      return ok1 && ok2 && ok3;
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean moveDatatypeParent(
    String dtypeid,
    String fromtypeid,
    String totypeid
  ) {
    // Not currently supported
    return false;
  }

  @Override
  public boolean moveDataParent(String arg0, String arg1, String arg2) {
    // Not currently supported
    return false;
  }

  @Override
  public boolean addData(String dataid, String dtypeid) {
    prodTypes.put(dataid, dtypeid);
    return true;
  }

  @Override
  public boolean renameData(String newdataid, String olddataid) {
    // TODO Not Implemented
    return false;
  }

  @Override
  public boolean removeData(String dataid) {
    try {
      Product prod = this.fmclient.getProductById(dataid);
      this.fmclient.removeProduct(prod);
      return true;
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean setDataLocation(String dataid, String locuri) {
    String dtypeid = prodTypes.get(dataid);
    if (dtypeid == null) return false;
    if (locuri == null) return false;

    ProductType type;
    try {
      type = this.fmclient.getProductTypeById(dtypeid);
    } catch (RepositoryManagerException e1) {
      e1.printStackTrace();
      return false;
    }
    String dataname = new URIEntity(dataid).getName();

    ArrayList<Reference> refs = new ArrayList<Reference>();
    File locf = new File(locuri);
    if (!locf.exists()) return false;

    long filesize = locf.length();
    refs.add(new Reference(locf.toURI().toString(), "", filesize));

    Product prod = new Product(
      dataname,
      type,
      Product.STRUCTURE_FLAT,
      Product.STATUS_TRANSFER,
      refs
    );
    prod.setProductId(dataid);
    try {
      this.fmclient.ingestProduct(prod, new Metadata(), false);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean setTypeNameFormat(String dtypeid, String format) {
    // TODO Not Supported
    return false;
  }

  @Override
  public boolean addObjectPropertyValue(
    String dataid,
    String propid,
    String valid
  ) {
    return this.addDatatypePropertyValue(dataid, propid, valid);
  }

  @Override
  public boolean addDatatypePropertyValue(
    String dataid,
    String propid,
    Object val
  ) {
    return this.addDatatypePropertyValue(dataid, propid, val.toString(), null);
  }

  @Override
  public boolean addDatatypePropertyValue(
    String dataid,
    String propid,
    String val,
    String xsdtype
  ) {
    try {
      Product prod = this.fmclient.getProductById(dataid);
      if (prod == null) return false;
      Element el = this.fmclient.getElementById(propid);
      if (el == null) return false;
      Metadata meta = this.fmclient.getMetadata(prod);
      meta.removeMetadata(el.getElementName());
      meta.addMetadata(el.getElementName(), val);
      this.fmclient.updateMetadata(prod, meta);
      return true;
    } catch (ValidationLayerException e) {
      e.printStackTrace();
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean removePropertyValue(String dataid, String propid, Object val) {
    try {
      Product prod = this.fmclient.getProductById(dataid);
      if (prod == null) return false;
      Metadata newm = new Metadata();
      Metadata meta = this.fmclient.getMetadata(prod);
      for (String mpropid : meta.getAllKeys()) {
        if (mpropid.equals(propid)) continue;
        newm.addMetadata(mpropid, meta.getMetadata(mpropid));
      }
      this.fmclient.updateMetadata(prod, newm);
      return true;
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean removeAllPropertyValues(
    String dataid,
    ArrayList<String> propids
  ) {
    try {
      Product prod = this.fmclient.getProductById(dataid);
      if (prod == null) return false;
      Metadata newm = new Metadata();
      this.fmclient.updateMetadata(prod, newm);
      return true;
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean addMetadataProperty(
    String propid,
    String domain,
    String range
  ) {
    if (domain == null) return false;
    try {
      ProductType type = this.fmclient.getProductTypeById(domain);
      String propname = new URIEntity(propid).getName();
      Element el = new Element(
        propid,
        propname,
        "",
        "",
        "Element " + propname,
        ""
      );
      // FIXME: Range ignored for now
      ArrayList<Element> elementList = new ArrayList<Element>();
      elementList.add(el);
      return this.curatorApi.addElementsForProductType(type, elementList);
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean addMetadataPropertyDomain(String propid, String domain) {
    try {
      Element el = this.fmclient.getElementById(propid);
      if (el == null) return false;
      ProductType type = this.fmclient.getProductTypeById(domain);
      if (type == null) return false;
      ArrayList<Element> elist = new ArrayList<Element>();
      elist.add(el);
      this.curatorApi.addElementsForProductType(type, elist);
      return true;
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    } catch (ValidationLayerException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean removeMetadataProperty(String propid) {
    try {
      Element el = this.fmclient.getElementById(propid);
      if (el == null) return false;
      ArrayList<Element> elist = new ArrayList<Element>();
      elist.add(el);
      for (ProductType type : this.fmclient.getProductTypes()) {
        this.curatorApi.removeElementsForProductType(type, elist);
      }
      return true;
    } catch (ValidationLayerException e) {
      e.printStackTrace();
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean removeMetadataPropertyDomain(String propid, String domain) {
    try {
      Element el = this.fmclient.getElementById(propid);
      if (el == null) return false;
      ProductType type = this.fmclient.getProductTypeById(domain);
      if (type == null) return false;
      ArrayList<Element> elist = new ArrayList<Element>();
      elist.add(el);
      return this.curatorApi.removeElementsForProductType(type, elist);
    } catch (RepositoryManagerException e) {
      e.printStackTrace();
    } catch (ValidationLayerException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean renameMetadataProperty(String oldid, String newid) {
    // TODO NOT implemented
    return false;
  }

  @Override
  public boolean save() {
    this.fmclient.refreshConfigAndPolicy();
    return true;
  }

  @Override
  public void copyFrom(DataCreationAPI dc) {
    DataTree tree = dc.getDataHierarchy();
    DataTreeNode root = tree.getRoot();
    this.initialSync(dc, root, null);
    this.save();
  }

  @Override
  public DataCreationAPI getExternalCatalog() {
    // TODO NOT implemented
    return null;
  }

  @Override
  public void setExternalCatalog(DataCreationAPI dc) {
    // TODO NOT implemented

  }

  /*
   * Private functions
   */
  private void initialSync(
    DataCreationAPI dc,
    DataTreeNode node,
    DataTreeNode parent
  ) {
    // Replace top DataObject with local namespaced DataObject
    String dtypeid = node.getItem().getID();
    if (dtypeid.equals(this.dcurl + "#DataObject")) dtypeid =
      this.onturl + "#DataObject";

    ProductType ptype = null;
    try {
      ptype = this.fmclient.getProductTypeById(dtypeid);
    } catch (RepositoryManagerException e) {
      ptype = null;
    }

    if (ptype == null) {
      String pdtypeid = parent != null ? parent.getItem().getID() : null;
      if (
        pdtypeid != null && pdtypeid.equals(this.dcurl + "#DataObject")
      ) pdtypeid = this.onturl + "#DataObject";
      this.addDatatype(dtypeid, pdtypeid);
      this.save();

      ArrayList<String> props = new ArrayList<String>();
      HashMap<String, Element> propElements = new HashMap<String, Element>();
      for (MetadataProperty prop : dc.getMetadataProperties(
        node.getItem().getID(),
        false
      )) {
        props.add(prop.getID());
        Element el;
        try {
          el = this.fmclient.getElementById(prop.getID());
        } catch (ValidationLayerException e) {
          this.addMetadataProperty(prop.getID(), dtypeid, null);
          this.save();
          try {
            el = this.fmclient.getElementById(prop.getID());
          } catch (ValidationLayerException e1) {
            el = null;
          }
        }
        if (el != null) propElements.put(prop.getID(), el);
      }
      for (DataTreeNode cnode : node.getChildren()) {
        if (cnode.getItem().getType() == DataItem.DATA) {
          String dataid = cnode.getItem().getID();
          Product prod;
          try {
            prod = this.fmclient.getProductById(dataid);
          } catch (CatalogException e) {
            prod = null;
          }
          if (prod == null) {
            this.addData(dataid, dtypeid);
            this.setDataLocation(dataid, dc.getDataLocation(dataid));
            this.addMetadataValues(dataid, dc.getMetadataValues(dataid, props));
          }
        } else if (cnode.getItem().getType() == DataItem.DATATYPE) {
          this.initialSync(dc, cnode, node);
        }
      }
    } else {
      for (DataTreeNode cnode : node.getChildren()) {
        if (cnode.getItem().getType() == DataItem.DATATYPE) {
          this.initialSync(dc, cnode, node);
        }
      }
    }
  }

  private boolean addMetadataValues(
    String dataid,
    ArrayList<MetadataValue> mvalues
  ) {
    try {
      Product prod;
      try {
        prod = this.fmclient.getProductById(dataid);
      } catch (CatalogException e) {
        prod = null;
      }
      if (prod == null) return false;

      Metadata meta = new Metadata();
      for (MetadataValue mval : mvalues) {
        String propid = mval.getPropertyId();
        meta.addMetadata(propid, mval.getValue().toString());
      }
      this.fmclient.addMetadata(prod, meta);
      return true;
    } catch (CatalogException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean end() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean save(KBAPI arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean saveAll() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean start_batch_operation() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean start_read() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean start_write() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void stop_batch_operation() {
    // TODO Auto-generated method stub

  }

  @Override
  public HashMap<String, ArrayList<String>> getAllDatatypeDatasets() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean moveDatatypeParentInLibrary(
    String dtypeid,
    String fromtypeid,
    String totypeid
  ) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean renameDatatypeInLibrary(String newtypeid, String oldtypeid) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean removeMetadataPropertyInLibrary(String propid) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean renamePropertyInLibrary(String oldid, String newid) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean delete() {
    // TODO Auto-generated method stub
    return false;
  }
}
