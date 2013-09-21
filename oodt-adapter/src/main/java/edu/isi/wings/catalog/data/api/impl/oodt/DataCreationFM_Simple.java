package edu.isi.wings.catalog.data.api.impl.oodt;

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

import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.data.classes.DataItem;
import edu.isi.wings.catalog.data.classes.DataTree;
import edu.isi.wings.catalog.data.classes.DataTreeNode;
import edu.isi.wings.catalog.data.classes.MetadataProperty;
import edu.isi.wings.catalog.data.classes.MetadataValue;
import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.kb.KBUtils;

public class DataCreationFM_Simple implements DataCreationAPI {

	XmlRpcFileManagerClient fmclient;	
	String fmurl;
	String archivedir;
	
	String dcurl;
	String liburl;
	String onturl;
	
	private static HashMap<String, String> prodTypes = new HashMap<String, String>();
	
	public DataCreationFM_Simple(Properties props) {
		this.fmurl = props.getProperty("oodt.fmurl");
		this.archivedir = props.getProperty("oodt.archivedir");

		this.dcurl = props.getProperty("ont.data.url");
		this.liburl = props.getProperty("lib.domain.data.url");
		this.onturl = props.getProperty("ont.domain.data.url");
		
		try {
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
	public DataTree getDataHierarchy() {
		return this.getDataHieararchy(false);
	}
	
	private DataTree getDataHieararchy(boolean typesonly) {
		DataItem rootitem = new DataItem("FileManager", DataItem.DATATYPE);
		DataTreeNode rootnode = new DataTreeNode(rootitem);
		HashMap<String, DataTreeNode> tnmap = new HashMap<String, DataTreeNode>();
		try {
			for(ProductType ptype : this.fmclient.getProductTypes()) {
				DataItem ptitem = new DataItem(ptype.getProductTypeId(), DataItem.DATATYPE);
				DataTreeNode ptitemnode = new DataTreeNode(ptitem);
				for (Product p : this.fmclient.getProductsByProductType(ptype)) {
					DataItem pitem = new DataItem(p.getProductId(), DataItem.DATA);
					DataTreeNode pitemnode = new DataTreeNode(pitem);
					ptitemnode.addChild(pitemnode);
				}
				tnmap.put(ptype.getProductTypeId(), ptitemnode);
				rootnode.addChild(ptitemnode);
			}
			return new DataTree(rootnode);
		}
		catch (CatalogException e) {
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
			for(ProductType ptype : this.fmclient.getProductTypes()) {
				for (Product p : this.fmclient.getProductsByProductType(ptype)) {
					ids.add(p.getProductId());
				}
			}
		}
		catch (CatalogException e) {
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
			MetadataProperty prop = new MetadataProperty(el.getElementId(), MetadataProperty.DATATYPE);
			return prop;
		} catch (ValidationLayerException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ArrayList<MetadataProperty> getMetadataProperties(String dtypeid, boolean direct) {
		ArrayList<MetadataProperty> props = new ArrayList<MetadataProperty>();
		try {
			ProductType type = this.fmclient.getProductTypeById(dtypeid);
			for(Element el : this.fmclient.getElementsByProductType(type)) {
				MetadataProperty prop = new MetadataProperty(el.getElementId(), MetadataProperty.DATATYPE);
				prop.addDomain(type.getProductTypeId());
				prop.setRange(KBUtils.XSD+"string");
				props.add(prop);
			}
		} catch (RepositoryManagerException e) {
			e.printStackTrace();
		} catch (ValidationLayerException e) {
			e.printStackTrace();
		}
		return props;
	}

	@Override
	public ArrayList<MetadataProperty> getAllMetadataProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataItem getDatatypeForData(String dataid) {
		try {
			Product prod = this.fmclient.getProductById(dataid);
			return new DataItem(prod.getProductType().getProductTypeId(), DataItem.DATATYPE);
		} catch (CatalogException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ArrayList<DataItem> getDataForDatatype(String dtypeid, boolean direct) {
		ArrayList<DataItem> ditems = new ArrayList<DataItem>();
		try {
			for(Product prod : 
				this.fmclient.getProductsByProductType(this.fmclient.getProductTypeById(dtypeid))) {
				ditems.add(new DataItem(prod.getProductType().getProductTypeId(), DataItem.DATATYPE));
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
			for(Reference ref : this.fmclient.getProductReferences(prod)) {
				return ref.getDataStoreReference();
			}
		} catch (CatalogException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ArrayList<MetadataValue> getMetadataValues(String dataid, ArrayList<String> propids) {
		ArrayList<MetadataValue> vals = new ArrayList<MetadataValue>();
		try {
			Product prod = this.fmclient.getProductById(dataid);
			Metadata meta = this.fmclient.getMetadata(prod);
			for(String key : meta.getAllKeys()) {
				MetadataValue val = new MetadataValue(key, 
						meta.getMetadata(key), MetadataValue.DATATYPE);
				vals.add(val);
			}
		} catch (CatalogException e) {
			e.printStackTrace();
		}
		return vals;
	}

	@Override
	public boolean addDatatype(String dtypeid, String parentid) {
		String dtypename = new URIEntity(dtypeid).getName();
		String desc = "A product type for "+dtypename+" files";
		String ver = "org.apache.oodt.cas.filemgr.versioning.BasicVersioner";
		String repo = "file://"+this.archivedir;
		ProductType type = new ProductType(dtypeid, dtypeid, desc, repo, ver);
		try {
			this.fmclient.addProductType(type);
			return true;
		} catch (RepositoryManagerException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean removeDatatype(String dtypeid) {
		try {
			ProductType type = this.fmclient.getProductTypeById(dtypeid);
			for(Product prod : this.fmclient.getProductsByProductType(type)) {
				this.fmclient.removeProduct(prod);
			}
			// Remove product type not implemented in fmclient
			return true;
		} catch (RepositoryManagerException e) {
			e.printStackTrace();
		} catch (CatalogException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean renameDatatype(String newtypeid, String oldtypeid) {
		// Not currently supported
		return false;
	}

	@Override
	public boolean moveDatatypeParent(String dtypeid, String fromtypeid, String totypeid) {
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
		// TODO Auto-generated method stub
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
		if(dtypeid == null)
			return false;
		if(locuri == null)
			return false;
		
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
		if(!locf.exists()) 
			return false;

		long filesize = locf.length();
		refs.add(new Reference(locf.toURI().toString(), "", filesize));
		
		Product prod = new Product(dataname, type, 
				Product.STRUCTURE_FLAT, Product.STATUS_TRANSFER, refs);
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
	public boolean addObjectPropertyValue(String dataid, String propid, String valid) {
		// TODO Not Supported
		return false;
	}

	@Override
	public boolean addDatatypePropertyValue(String dataid, String propid, Object val) {
		return this.addDatatypePropertyValue(dataid, propid, val.toString(), null);
	}

	@Override
	public boolean addDatatypePropertyValue(String dataid, String propid, String val, String xsdtype) {
		try {
			Product prod = this.fmclient.getProductById(dataid);
			if(prod == null)
				return false;
			Element el = this.fmclient.getElementById(propid);
			if(el == null)
				return false;
			Metadata meta = this.fmclient.getMetadata(prod);
			meta.removeMetadata(el.getElementName());
			meta.addMetadata(el.getElementName(), val);
			this.fmclient.updateMetadata(prod, meta);
			return true;
		} catch (CatalogException e) {
			e.printStackTrace();
		} catch (ValidationLayerException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean removePropertyValue(String dataid, String propid, Object val) {
		try {
			Product prod = this.fmclient.getProductById(dataid);
			if(prod == null)
				return false;
			Metadata newm = new Metadata();
			Metadata meta = this.fmclient.getMetadata(prod);
			for(String mpropid : meta.getAllKeys()) {
				if(mpropid.equals(propid))
					continue;
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
	public boolean removeAllPropertyValues(String dataid, ArrayList<String> propids) {
		try {
			Product prod = this.fmclient.getProductById(dataid);
			if(prod == null)
				return false;
			Metadata newm = new Metadata();
			this.fmclient.updateMetadata(prod, newm);
			return true;
		} catch (CatalogException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean addMetadataProperty(String propid, String domain, String range) {
		return true;
	}

	@Override
	public boolean addMetadataPropertyDomain(String propid, String domain) {
		return true;
	}

	@Override
	public boolean removeMetadataProperty(String propid) {	
		return true;
	}

	@Override
	public boolean removeMetadataPropertyDomain(String propid, String domain) {
		return true;
	}

	@Override
	public boolean renameMetadataProperty(String oldid, String newid) {
		// TODO NOT implemented
		return false;
	}

	@Override
	public boolean save() {
		return true;
	}

	@Override
	public void end() {
		// TODO NOT implemented

	}

	@Override
	public void delete() {
		// TODO NOT implemented

	}

	@Override
	public void copyFrom(DataCreationAPI dc) {
		DataTree tree = dc.getDataHierarchy();
		DataTreeNode root = tree.getRoot();
		this.initialSync(dc, root, null);
		this.save();
	}
	
	private void initialSync(DataCreationAPI dc, DataTreeNode node, DataTreeNode parent) {
		// Replace top DataObject with local namespaced DataObject
		String dtypeid = node.getItem().getID();
		if(dtypeid.equals(this.dcurl + "#DataObject"))
			dtypeid = this.onturl + "#DataObject";
		
		ProductType ptype = null;
		try {
			ptype = this.fmclient.getProductTypeById(dtypeid);
		} catch (RepositoryManagerException e) {
			ptype = null;
		}
		
		if (ptype == null) {
			String pdtypeid = parent != null ? parent.getItem().getID() : null;
			if(pdtypeid != null && pdtypeid.equals(this.dcurl + "#DataObject"))
				pdtypeid = this.onturl + "#DataObject";
			this.addDatatype(dtypeid, pdtypeid);
			this.save();
			
			ArrayList<String> props = new ArrayList<String>();
			for (MetadataProperty prop : dc.getMetadataProperties(node.getItem().getID(), false)) {
				props.add(prop.getID());
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
		}
		else {
			for (DataTreeNode cnode : node.getChildren()) {
				if (cnode.getItem().getType() == DataItem.DATATYPE) {
					this.initialSync(dc, cnode, node);
				}
			}
		}
	}
	
	private boolean addMetadataValues(String dataid, ArrayList<MetadataValue> mvalues) {
		try {
			Product prod;
			try {
				prod = this.fmclient.getProductById(dataid);
			} catch (CatalogException e) {
				prod = null;
			}
			if(prod == null)
				return false;
			
			Metadata meta = new Metadata();
			for(MetadataValue mval : mvalues) {
				String propid = mval.getPropertyId();
				Element el = null;
				try {
					el = this.fmclient.getElementById(propid);
				} catch (ValidationLayerException e) {
					e.printStackTrace();
				}
				if(el == null)
					return false;
				//String propname = new URIEntity(propid).getName();
				meta.addMetadata(el.getElementName(), mval.getValue().toString());
			}
			this.fmclient.addMetadata(prod, meta);
			return true;
		} catch (CatalogException e) {
			e.printStackTrace();
		}
		return false;
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

}
