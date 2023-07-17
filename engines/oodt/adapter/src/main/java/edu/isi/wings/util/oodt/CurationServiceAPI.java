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

package edu.isi.wings.util.oodt;

import com.google.gson.Gson;
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
        params +=
          "&" +
          args[i] +
          "=" +
          URLEncoder.encode(args[i + 1].toString(), "UTF-8");
      }

      URL urlobj = new URL(url);
      if ("GET".equals(method)) urlobj = new URL(url + "?" + params);
      HttpURLConnection con = (HttpURLConnection) urlobj.openConnection();
      con.setRequestMethod(method);
      if (!"GET".equals(method)) {
        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(params);
        out.flush();
        out.close();
      }

      String result = IOUtils.toString(con.getInputStream());
      con.disconnect();
      return result;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public boolean removeProductType(ProductType type) {
    String result =
      this.query("DELETE", "remove", "id", type.getProductTypeId());
    return Boolean.parseBoolean(result);
  }

  public HashMap<String, String> getParentTypeMap() {
    String result = this.query("GET", "parentmap");
    HashMap<?, ?> map = gson.fromJson(result, HashMap.class);
    HashMap<String, String> ptypeMap = new HashMap<String, String>();
    for (Object key : map.keySet()) {
      ptypeMap.put((String) key, (String) map.get(key));
    }
    return ptypeMap;
  }

  public boolean addParentForProductType(ProductType type, String parentId) {
    String result =
      this.query(
          "POST",
          "parent/add",
          "id",
          type.getProductTypeId(),
          "parentId",
          parentId
        );
    return Boolean.parseBoolean(result);
  }

  public boolean removeParentForProductType(ProductType type) {
    String result =
      this.query("DELETE", "parent/remove", "id", type.getProductTypeId());
    return Boolean.parseBoolean(result);
  }

  public boolean addElementsForProductType(
    ProductType type,
    List<Element> elementList
  ) {
    String elementIds = "";
    for (Element element : elementList) elementIds +=
      (elementIds != "" ? "," : "") + element.getElementId();
    String result =
      this.query(
          "POST",
          "elements/add",
          "id",
          type.getProductTypeId(),
          "elementIds",
          elementIds
        );
    return Boolean.parseBoolean(result);
  }

  public List<Element> getElementsForProductType(
    ProductType type,
    boolean direct
  ) {
    String result =
      this.query(
          "GET",
          "elements",
          "id",
          type.getProductTypeId(),
          "direct",
          direct
        );
    List<Element> elementList = new ArrayList<Element>();
    Object[] elementIds = gson.fromJson(result, ArrayList.class).toArray();
    for (Object elementId : elementIds) {
      String id = (String) elementId;
      elementList.add(new Element(id, id, "", "", "Automatically Added", ""));
    }
    return elementList;
  }

  public boolean removeAllElementsForProductType(ProductType type) {
    String result =
      this.query(
          "DELETE",
          "elements/remove/all",
          "id",
          type.getProductTypeId()
        );
    return Boolean.parseBoolean(result);
  }

  public boolean removeElementsForProductType(
    ProductType type,
    List<Element> elementList
  ) {
    String elementIds = "";
    for (Element element : elementList) elementIds +=
      (elementIds != "" ? "," : "") + element.getElementId();
    String result =
      this.query(
          "DELETE",
          "elements/remove",
          "id",
          type.getProductTypeId(),
          "elementIds",
          elementIds
        );
    return Boolean.parseBoolean(result);
  }

  public List<String> getProductTypeIdsHavingElement(Element el) {
    String result =
      this.query("GET", "typeswithelement/" + el.getElementId(), "id");
    Object[] typeIds = gson.fromJson(result, ArrayList.class).toArray();
    return Arrays.asList((String[]) typeIds);
  }
}
