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

package edu.isi.wings.portal.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;

import edu.isi.wings.catalog.data.DataFactory;
import edu.isi.wings.catalog.data.api.DataCreationAPI;
import edu.isi.wings.catalog.data.classes.DataItem;
import edu.isi.wings.catalog.data.classes.DataTree;
import edu.isi.wings.catalog.data.classes.DataTreeNode;
import edu.isi.wings.catalog.data.classes.MetadataProperty;
import edu.isi.wings.catalog.data.classes.MetadataValue;
import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;
import edu.isi.wings.catalog.provenance.classes.Provenance;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.StorageHandler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataController {
	public String dcns;
	public String domns;
	public String libns;

	public DataCreationAPI dc;
	public ProvenanceAPI prov;
	
	public boolean loadExternal;
	public Config config;
	public Properties props;

	public Gson json;

	public DataController(Config config, boolean load_external) {
		this.config = config;
		this.loadExternal = load_external;

		json = JsonHandler.createDataGson();
		this.props = config.getProperties();

		dc = DataFactory.getCreationAPI(props);
		if(this.loadExternal)
		  dc = dc.getExternalCatalog();
		prov = ProvenanceFactory.getAPI(props);

    this.dcns = (String) props.get("ont.data.url") + "#";
    this.domns = (String) props.get("ont.domain.data.url") + "#";
    this.libns = (String) props.get("lib.domain.data.url") + "#";
	}

	/*
	 * Querying Methods
	 */
	public String getDataJSON(String dataid) {
		try {
			if (this.dc == null)
				return "{}";
			String location = this.dc.getDataLocation(dataid);
			DataItem dtype = this.dc.getDatatypeForData(dataid);
			if(dtype == null)
			  return null;
			
			ArrayList<MetadataProperty> props = this.dc.getMetadataProperties(dtype.getID(), false);
			ArrayList<String> propids = new ArrayList<String>();
			for (MetadataProperty prop : props)
				propids.add(prop.getID());
			ArrayList<MetadataValue> vals = this.dc.getMetadataValues(dataid, propids);
			
			HashMap<String, Object> details = new HashMap<String, Object>();
			details.put("dtype", dtype.getID());
			details.put("location", location);
			details.put("props", props);
			details.put("vals", vals);
			return json.toJson(details);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		finally {
			dc.end();
			prov.end();
		}
	}

	public String getDatatypeJSON(String dtype) {
		try {
			if (this.dc == null)
				return "{}";

			ArrayList<MetadataProperty> props = this.dc.getMetadataProperties(dtype, false);
			String format = this.dc.getTypeNameFormat(dtype);
	
			HashMap<String, Object> details = new HashMap<String, Object>();
			details.put("properties", props);
			details.put("name_format", format);
			return json.toJson(details);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		finally {
			dc.end();
			prov.end();
		}
	}

	public String getDataHierarchyJSON() {
		try {
			if(dc == null)
				return null;
			DataTree tree = dc.getDataHierarchy();
			String dtree = null;
			if(tree != null) 
				dtree = json.toJson(tree.getRoot());
			return dtree;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getDataListJSON() {
    try {
      if(dc == null)
        return null;
      HashMap<String, ArrayList<String>> typeInstances = dc.getAllDatatypeDatasets();
      return json.toJson(typeInstances);
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }	  
	}
	
	public String getMetricsHierarchyJSON() {
		try {
			if(dc == null)
				return null;
			DataTree tree = dc.getMetricsHierarchy();
			String mtree = null;
			if(tree != null) 
				mtree = json.toJson(tree.getRoot());
			return mtree;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Response streamData(String dataid, ServletContext context) {
		try {
			String location = dc.getDataLocation(dataid);
			if(location != null) {
				// Check if this is a file url
				File f = null;
				try {
					URL url = new URL(location);
					f = new File(url.getPath());
				} catch (MalformedURLException e) {
					// Do nothing
				}
				// Else assume it's a file path
				if(f == null)
					f = new File(location);

		    return StorageHandler.streamFile(f.getAbsolutePath(), context);
			}
		}
		finally {
			dc.end();
			prov.end();
		}
		return null;
	}

	/*
	 * Writing Methods
	 */
	public synchronized boolean saveDataJSON(String dataid, String propvals_json) {
		if (dc == null)
			return false;
		try {
			JsonParser parser = new JsonParser();
			JsonElement propvals = parser.parse(propvals_json);

			DataItem dtype = dc.getDatatypeForData(dataid);
			ArrayList<MetadataProperty> props = dc.getMetadataProperties(dtype.getID(), false);

			ArrayList<String> propids = new ArrayList<String>();
			HashMap<String, MetadataProperty> pinfos = new HashMap<String, MetadataProperty>();
			for (MetadataProperty prop : props) {
				propids.add(prop.getID());
				pinfos.put(prop.getID(), prop);
			}

			dc.removeAllPropertyValues(dataid, propids);
			for (JsonElement propval : propvals.getAsJsonArray()) {
				JsonObject pval = propval.getAsJsonObject();
				String propid = pval.get("name").getAsString();
				String value = pval.get("value").getAsString();
				MetadataProperty pinfo = pinfos.get(propid);
				if (pinfo.isDatatypeProperty()) {
					if (value.equals("") && !pinfo.getRange().contains("string"))
						continue;
					dc.addDatatypePropertyValue(dataid, propid, value, pinfo.getRange());
				} else {
					dc.addObjectPropertyValue(dataid, propid, this.domns + value.toString());
				}
			}
			String provlog = "Updating metadata";
			Provenance p = new Provenance(dataid);
			p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));
			return prov.addProvenance(p) && dc.save() &&  prov.save();
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}
	
	public synchronized String registerData(String dataid, String newname, String metadata_json) {
    if (dc == null)
      return "No Data Catalog";
    try {
      JsonParser parser = new JsonParser();
      JsonElement propvals = parser.parse(metadata_json);
      JsonObject pvals = propvals.getAsJsonObject();
      if(pvals.get("type") == null)
        return "Datatype not known";
      
      // Choose the most specific type
      DataTree tree = dc.getDatatypeHierarchy();
      String dtypeid = null;
      for(JsonElement el : pvals.get("type").getAsJsonArray()) {
        String tmpid = el.getAsString();
        if(dtypeid == null)
          dtypeid = tmpid;
        else {
          DataTreeNode dnode = tree.findNode(dtypeid);
          if(dnode.hasChild(tree.findNode(tmpid), false))
            dtypeid = tmpid;
        }
      }
      
      String dloc = dc.getDataLocation(dataid);
      if(dloc == null)
        return "Existing data not found on server";
      
      String newid = this.libns + newname;
      String newloc = dc.getDataLocation(newid);
      if(!dc.addData(newid, dtypeid))
        return "Could not add data";

      if(!dataid.equals(newid)) {
        File origf = new File(dloc);
        File newf = new File(origf.getParentFile().getAbsolutePath()+File.separator+newname);
        newloc = newf.getAbsolutePath();
        if(origf.exists() && !newf.exists())
          FileUtils.copyFile(origf, newf);
      }
      if(newloc == null)
        return "Cannot find location for new data";
      
      if(!dc.setDataLocation(newid, newloc))
         return "Could not set data location";
      
      ArrayList<MetadataProperty> props = dc.getMetadataProperties(dtypeid, false);
      HashMap<String, MetadataProperty> pinfos = new HashMap<String, MetadataProperty>();
      for (MetadataProperty prop : props) {
        pinfos.put(prop.getName(), prop);
      }

      for(Entry<String, JsonElement> entry : pvals.entrySet()) {
        String pname = entry.getKey();
        for(JsonElement el : entry.getValue().getAsJsonArray()) {
          String value = el.getAsString();
          MetadataProperty pinfo = pinfos.get(pname);
          if(pinfo != null) {
            if (pinfo.isDatatypeProperty()) {
              if (value.equals("") && !pinfo.getRange().contains("string"))
                continue;
              dc.addDatatypePropertyValue(newid, pinfo.getID(), value, pinfo.getRange());
            } else {
              dc.addObjectPropertyValue(newid, pinfo.getID(), this.domns + value.toString());
            }
          }
        }
      }
      Provenance p = new Provenance(newid);
      p.addActivity(new ProvActivity(ProvActivity.CREATE, "Saving data from a run"));
      if(prov.addProvenance(p)
        && dc.save() && prov.save())
        return "OK";
      return "";
    }
    catch(Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }
    finally {
      dc.end();
      prov.end();
    }
	}

	public synchronized String saveDatatypeJSON(String dtypeid, String props_json) {
		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<String> warnings = new ArrayList<String>();
		
		if(dtypeid == null || props_json == null)
			return "[\"Null inputs\"]";

		try {
			JsonParser parser = new JsonParser();
			JsonElement jsonel = parser.parse(props_json);
			JsonObject ops = jsonel.getAsJsonObject();

			// Same datatype's name format
			if (ops.get("format") != null && !ops.get("format").isJsonNull()) {
				String fmt = ops.get("format").getAsString();
				this.dc.setTypeNameFormat(dtypeid, fmt);
			}

			// Check all properties being added
			JsonObject addops = ops.get("add").getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : addops.entrySet()) {
				String propid = entry.getKey();
				JsonObject prop = entry.getValue().getAsJsonObject();
				if (prop.get("range") == null) {
					errors.add("No range specified for property propid");
					continue;
				}
				String range = prop.get("range").getAsString();
				MetadataProperty eprop = dc.getMetadataProperty(propid);
				if (eprop != null) {
					// A property with this id already exists. 
					// Add dtypeid as a domain of the property
					this.dc.addMetadataPropertyDomain(propid, dtypeid);
				}
				else {
					this.dc.addMetadataProperty(propid, dtypeid, range);
				}
			}

			// Check all properties being deleted
			JsonObject delops = ops.get("del").getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : delops.entrySet()) {
				String propid = entry.getKey();
				MetadataProperty eprop = dc.getMetadataProperty(propid);
				if(eprop.getDomains().size() > 1) {
					// There are more than one datatypes using this property
					// just remove the property from this datatype
					this.dc.removeMetadataPropertyDomain(propid, dtypeid);
					eprop.getDomains().remove(dtypeid);
					warnings.add("Note that the property you deleted currently also "
							+ "exists for other datatypes: " 
							+ eprop.getDomains());
				}
				else {
					this.dc.removeMetadataProperty(propid);
				}
			}

			// Check all properties being modified
			JsonObject modops = ops.get("mod").getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : modops.entrySet()) {
				String propid = entry.getKey();
				JsonObject prop = entry.getValue().getAsJsonObject();
				if (prop.get("range") == null) {
					errors.add("No range specified for property propid");
					continue;
				}
				String range = prop.get("range").getAsString();
				String npropid = prop.get("pid").getAsString();
				MetadataProperty eprop = this.dc.getMetadataProperty(propid);
				MetadataProperty enprop = this.dc.getMetadataProperty(npropid);
				if(enprop != null && !propid.equals(npropid) && 
				    !range.equals(enprop.getRange())) {
				  errors.add("Property "+enprop.getName()
				      + " already exists with a different range: "
				      + enprop.getRange());
				  continue;
				}
				if (!eprop.getRange().equals(range)) {
					this.dc.removeMetadataProperty(propid);
					this.dc.addMetadataProperty(npropid, dtypeid, range);
					// Re-add any other domains that the property might have had
					for(String domid : eprop.getDomains()) {
						if(!domid.equals(dtypeid))
							this.dc.addMetadataPropertyDomain(npropid, domid);
					}
				} else if (!propid.equals(npropid)) {
					this.dc.renameMetadataProperty(propid, npropid);
				}
				if(eprop.getDomains().size() > 1) {
					eprop.getDomains().remove(dtypeid);
					warnings.add("Note that the property you modified also "
							+ "exists for other datatypes, and would have been modified for them as well : " 
							+ eprop.getDomains());
				}
			}
			if(errors.size() == 0) {
	      String provlog = "Updating datatype properties";
	      Provenance p = new Provenance(dtypeid);
	      p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));
	      if(!prov.addProvenance(p) || !dc.save() || !prov.save()) {
	        errors.add("Could not add provenance");
	      }
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			errors.add(e.getMessage());
		}
		finally {
			dc.end();
			prov.end();
		}
		HashMap<String, Object> retobj = new HashMap<String, Object>();
		retobj.put("errors", errors);
		retobj.put("warnings", warnings);
		return json.toJson(retobj);
	}

	public synchronized boolean addDatatype(String ptype, String dtype) {
		if(ptype == null || dtype == null)
			return false;
		try {
      String provlog = "Creating datatype with parent "+KBUtils.getLocalName(ptype);
      Provenance p = new Provenance(dtype);
      p.addActivity(new ProvActivity(ProvActivity.CREATE, provlog));
			return dc.addDatatype(dtype, ptype) && prov.addProvenance(p)
				&& dc.save() && prov.save();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}

	public synchronized boolean moveDatatypeTo(String dtypeid, String fromtype, String totype) {
		if(dtypeid == null || fromtype == null || totype == null)
			return false;
		try {
      String provlog = "Moving Datatype from " + KBUtils.getLocalName(fromtype) + 
          " to " + KBUtils.getLocalName(totype);
      Provenance p = new Provenance(dtypeid);
      p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));
			return dc.moveDatatypeParent(dtypeid, fromtype, totype) && prov.addProvenance(p)
					&& dc.save() && prov.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}

  public synchronized boolean moveDataTo(String dataid, String fromtype, String totype) {
    if(dataid == null || fromtype == null || totype == null)
      return false;
    try {
      String provlog = "Moving Data from " + KBUtils.getLocalName(fromtype) + 
          " to " + KBUtils.getLocalName(totype);
      Provenance p = new Provenance(dataid);
      p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));
      return dc.moveDataParent(dataid, fromtype, totype) && prov.addProvenance(p)
          && dc.save() && prov.save();
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    finally {
      dc.end();
      prov.end();
    }
  }
  
	public synchronized boolean delDatatypes(String[] dtypes) {
		try {
			for (String dtypeid : dtypes) {
				if(!dc.removeDatatype(dtypeid)) {
					return false;
				}
	      prov.removeAllProvenance(dtypeid);
			}
			return dc.save() && prov.save();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}

	public synchronized boolean addDataForDatatype(String dataid, String dtypeid) {
		try {
		  String provlog = "Creating data of type "+KBUtils.getLocalName(dtypeid);
      Provenance p = new Provenance(dataid);
      p.addActivity(new ProvActivity(ProvActivity.CREATE, provlog));
			return dc.addData(dataid, dtypeid) && prov.addProvenance(p)
				&& dc.save() && prov.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}
	
	public synchronized boolean addBatchData(String dtypeid, String[] dids, String[] locations) {
		try {
			for(int i=0; i<dids.length; i++) {
				if(!dc.addData(dids[i], dtypeid))
					return false;
				if(locations.length > i && locations[i] != null)
					if(!dc.setDataLocation(dids[i], locations[i]))
						return false;
			}
			return dc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}

	public synchronized boolean delData(String dataid) {
		try {
			return dc.removeData(dataid) && prov.removeAllProvenance(dataid)
				&& dc.save() && prov.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}
	
	public synchronized boolean renameData(String dataid, String newid) {
		try {
		  String provlog = "Renaming " + KBUtils.getLocalName(dataid) 
		      + " to " + KBUtils.getLocalName(newid);
      Provenance p = new Provenance(dataid);
      p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));
			return dc.renameData(newid, dataid) && prov.addProvenance(p)
				&& dc.save() && prov.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}
	
	public synchronized boolean setDataLocation(String dataid, String location) {
		try {
      String provlog = "Setting location";
      Provenance p = new Provenance(dataid);
      p.addActivity(new ProvActivity(ProvActivity.UPLOAD, provlog));
			return dc.setDataLocation(dataid, location) && prov.addProvenance(p)
				&& dc.save() && prov.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}
	
	public synchronized boolean renameDataType(String dtypeid, String newid) {
		try {
      String provlog = "Renaming " + KBUtils.getLocalName(dtypeid) 
          + " to " + KBUtils.getLocalName(newid);
      Provenance p = new Provenance(dtypeid);
      p.addActivity(new ProvActivity(ProvActivity.UPDATE, provlog));
			return dc.renameDatatype(newid, dtypeid) && prov.addProvenance(p)
				&& dc.save() && prov.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}
	
	public synchronized boolean importFromExternalCatalog(String dataid, String dtypeid, 
			String propvals_json, String location) {
		if (dc == null)
			return false;
		try {
			JsonParser parser = new JsonParser();
			JsonElement propvals = parser.parse(propvals_json);

			DataItem dtype = dc.getDatatypeForData(dataid);
			if(dtype == null) {
				dc.addData(dataid, dtypeid);
			}
			for (Map.Entry<String, JsonElement> entry : propvals.getAsJsonObject().entrySet()) {
				String propid = entry.getKey();
				String value = entry.getValue().getAsString();

				// Using default range as string for now
				String range = KBUtils.XSD + "string";
				MetadataProperty eprop = dc.getMetadataProperty(propid);
				if (eprop == null) {
					// Property doesn't exist
					this.dc.addMetadataProperty(propid, dtypeid, range);
				}
				else {
				  if(!eprop.getDomains().contains(dtypeid)) {
				    // Property exists for another class. Add this class as domain
				    this.dc.addMetadataPropertyDomain(propid, dtypeid);
				  }
				  range = eprop.getRange();
				}
				
				this.dc.addDatatypePropertyValue(dataid, propid, value, range);
			}
			dc.setDataLocation(dataid, location);
			return dc.save();
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
			prov.end();
		}
	}
}
