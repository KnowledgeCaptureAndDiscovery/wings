package edu.isi.wings.portal.controllers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.google.gson.Gson;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.html.CSSLoader;
import edu.isi.wings.portal.classes.html.JSLoader;
import edu.isi.wings.portal.classes.users.User;
import edu.isi.wings.portal.classes.users.UsersDB;

@SuppressWarnings("unused")
public class UserController {
  private int guid;
  private Config config;
  private Properties props;
  private Gson json;
  private UsersDB api;

  public UserController(int guid, Config config) {
    this.guid = guid;
    this.config = config;
    json = JsonHandler.createDataGson();
    this.props = config.getProperties();
    this.api = new UsersDB();
  }

  public void show(PrintWriter out) {
    try {
      ArrayList<edu.isi.wings.portal.classes.users.User> users = api.getUsers();
      
      out.println("<html>");
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
            + config.isAdminViewer()
            + ");\n"
            + "userViewer_" + guid + ".initialize();\n"
          + "});");
      out.println("</script>");
      out.println("</html>");
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
      User user = api.getUser(userid);
      if(user == null)
        return false;
      return this.api.removeUser(userid)
          && this.api.save();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}
