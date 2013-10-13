package edu.isi.wings.catalog.data.api.impl.oodt;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.oodt.cas.filemgr.structs.Element;
import org.apache.oodt.cas.filemgr.structs.ProductType;

import com.google.gson.Gson;

public class CurationServiceAPI {
	String policy;
	String curatorurl;
	Gson gson;
	String service = "/services/metadata/productType/";

	public CurationServiceAPI(String curatorurl, String policy) {
		this.curatorurl = curatorurl;
		this.policy = policy;
		this.gson = new Gson();
	}

	private String query(String method, String op, Object... args) {
		String url = this.curatorurl + this.service + op;
		try {
			String params = "policy=" + URLEncoder.encode(this.policy, "UTF-8");
			for (int i = 0; i < args.length; i += 2) {
				params += "&" + args[i] + "=" 
						+ URLEncoder.encode(args[i+1].toString(), "UTF-8");
			}
			
			if("GET".equals(method)) {
				URL urlobj = new URL(url + "?" + params);
				return IOUtils.toString(urlobj);
			}
			else {
				URL urlobj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) urlobj.openConnection();
				con.setRequestMethod(method);
				con.setDoOutput(true);
				DataOutputStream out = new DataOutputStream(con.getOutputStream());
				out.writeBytes(params);
				out.flush();
				out.close();
				
				String result = IOUtils.toString(con.getInputStream());
				con.disconnect();
				return result;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean removeProductType(ProductType type) {
		String result = this.query("DELETE", "remove", 
				"id", type.getProductTypeId());
		return Boolean.parseBoolean(result);
	}

	public HashMap<String, String> getParentTypeMap() {
		String result = this.query("GET", "getParentMap");
		HashMap<?, ?> map = gson.fromJson(result, HashMap.class);
		HashMap<String, String> ptypeMap = new HashMap<String, String>(); 
		for(Object key : map.keySet()) {
			ptypeMap.put((String)key, (String)map.get(key));
		}
		return ptypeMap;
	}

	public boolean addParentForProductType(ProductType type, String parentId) {
		String result = this.query("POST", "addParent", 
				"id", type.getProductTypeId(), 
				"parentId", parentId);
		return Boolean.parseBoolean(result);
	}

	public boolean removeParentForProductType(ProductType type) {
		String result = this.query("DELETE", "removeParent", "id", type.getProductTypeId());
		return Boolean.parseBoolean(result);
	}

	public boolean addElementsForProductType(ProductType type, List<Element> elementList) {
		String elementIds = "";
		for(Element element: elementList)
			elementIds += (elementIds != "" ? "," : "") + element.getElementId();
		String result = this.query("POST", "addElements", 
				"id", type.getProductTypeId(), 
				"elementIds", elementIds);
		return Boolean.parseBoolean(result);
	}

	public List<Element> getElementsForProductType(ProductType type, boolean direct) {
		String result = this.query("GET", "getElements",
				"id", type.getProductTypeId(), 
				"direct", direct);
		List<Element> elementList = new ArrayList<Element>();
		Object[] elementIds = gson.fromJson(result, ArrayList.class).toArray();
		for(Object elementId : elementIds) {
			String id = (String)elementId;
			elementList.add(new Element(id, id, "", "", "Automatically Added", ""));
		}
		return elementList;
	}

	public boolean removeAllElementsForProductType(ProductType type) {
		String result = this.query("DELETE", "removeAllElements", 
				"id", type.getProductTypeId());
		return Boolean.parseBoolean(result);
	}

	public boolean removeElementsForProductType(ProductType type, List<Element> elementList) {
		String elementIds = "";
		for(Element element: elementList)
			elementIds += (elementIds != "" ? "," : "") + element.getElementId();
		String result = this.query("DELETE", "removeElements", 
				"id", type.getProductTypeId(),
				"elementIds", elementIds);
		return Boolean.parseBoolean(result);
	}
	
	public List<String> getProductTypeIdsHavingElement(Element el) {
		String result = this.query("GET", "getTypesHavingElement" , 
				"id", el.getElementId());
		Object[] typeIds = gson.fromJson(result, ArrayList.class).toArray();
		return Arrays.asList((String[])typeIds);
	}
}
