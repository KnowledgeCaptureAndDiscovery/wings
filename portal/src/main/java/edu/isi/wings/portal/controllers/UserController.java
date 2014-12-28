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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.html.CSSLoader;
import edu.isi.wings.portal.classes.html.HTMLLoader;
import edu.isi.wings.portal.classes.html.JSLoader;
import edu.isi.wings.portal.classes.users.User;
import edu.isi.wings.portal.classes.users.UsersDB;

import com.google.gson.Gson;

@SuppressWarnings("unused")
public class UserController {
  private int guid;
  private Config config;
  private Properties props;
  private Gson json;
  private UsersDB api;
  private String provScript;

  public UserController(int guid, Config config) {
    this.guid = guid;
    this.config = config;
    json = JsonHandler.createDataGson();
    this.props = config.getProperties();
    this.api = new UsersDB();
    this.provScript = config.getCommunityPath() + "/provenance";
  }

  public void show(PrintWriter out) {
    try {
      ArrayList<edu.isi.wings.portal.classes.users.User> users = api.getUsers();
      
      HTMLLoader.printHeader(out);
      out.println("<head>");
      out.println("<title>Manage Users</title>");
      JSLoader.loadConfigurationJS(out, config);
      CSSLoader.loadUserViewer(out, config.getContextRootPath());
      JSLoader.loadUserViewer(out, config.getContextRootPath());
      out.println("</head>");
  
      out.println("<script>");
      out.println("var communityViewer_" + guid + ";");
      out.println("Ext.onReady(function() {"
          + "userViewer_" + guid + " = new UserViewer('" + guid + "', { "
              + "users: " + json.toJson(users)
            + " }, " 
            + "'" + config.getScriptPath() + "', "
            + "'" + this.provScript + "', "
            + config.isAdminViewer()
            + ");\n"
            + "userViewer_" + guid + ".initialize();\n"
          + "});");
      out.println("</script>");
      HTMLLoader.printFooter(out);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getUserJSON(String userid) {
    try {
      User user = api.getUser(userid);
      if(user.getPassword() != null)
        user.setPassword(user.getPassword().replaceAll(".", "*"));
      return json.toJson(user);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public boolean saveUserJSON(String userid, String uservals_json) {
    if (this.api == null)
      return false;

    try {
      User user = json.fromJson(uservals_json, User.class);
      // Can only save your own information, or if the viewer is admin
      if(!this.config.getViewerId().equals(user.getId())
          && !this.config.isAdminViewer())
        return false;
      
      // If the password is all "*" (i.e. shadowed), then set to null
      if(user.getPassword().matches("\\**"))
        user.setPassword(null);
      
      // A non-admin user cannot set itself to be admin
      if(!config.isAdminViewer())
        user.setAdmin(false);
      
      return this.api.saveUser(user) 
          && this.api.save();
      
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  
  public boolean addUser(String userid) {
    try {
      User user = api.getUser(userid);
      if(user != null)
        return false;
      return this.api.addUser(userid, null, null)
          && this.api.save();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
  public boolean removeUser(String userid) {
    try {
      if(config.getViewerId().equals(userid))
        return false;
      User user = api.getUser(userid);
      if(user == null)
        return false;
      
      if(this.api.removeUser(userid)) {
        // Remove user domains
        config.setUserId(userid);
        config.setViewerId(userid);
        DomainController dc = new DomainController(this.guid, this.config);
        for(String domain : dc.getDomainsList()) {
          dc.deleteDomain(domain);
        }
        // Remove user directory
        FileUtils.deleteDirectory(new File(config.getUserDir()));
        
        ProvenanceAPI prov = ProvenanceFactory.getAPI(config.getProperties());
        prov.removeUser(userid);
        prov.save();
        
        return this.api.save();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}
