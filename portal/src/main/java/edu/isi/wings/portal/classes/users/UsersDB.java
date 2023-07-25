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

package edu.isi.wings.portal.classes.users;

import java.util.ArrayList;
import java.util.Iterator;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.catalina.Role;
import org.apache.catalina.UserDatabase;

/**
 * Requires the following markup in server.xml for this to work:
 * 
  <Context docBase="wings-portal" path="/wings-portal">
    <ResourceLink name="users" global="UserDatabase"/>
  </Context>
  
  <Resource name="UserDatabase" ...  readonly="false"/>
 */

public class UsersDB {

  public static String WINGS_USER_ROLE = "WingsUser";
  public static String WINGS_ADMIN_ROLE = "WingsAdmin";

  private UserDatabase udb;
  private ArrayList<User> users;

  public UsersDB() {
    this.initializeUserDatabase();
    this.initializeUserIds();
  }

  public boolean save() {
    try {
      this.udb.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public User getUser(String uname) {
    if (this.udb == null) return new User(uname, uname + "123", uname);

    for (User user : this.users) if (user.getId().equals(uname)) return user;
    return null;
  }

  public boolean addUser(String uname, String pwd, String fname) {
    if (this.udb == null) return false;
    org.apache.catalina.User user = this.udb.createUser(uname, pwd, fname);
    if (user != null) {
      this.users.add(new User(user));
      this.setUserRole(uname);
      return true;
    }
    return false;
  }

  public boolean saveUser(User user) {
    if (this.udb == null) return false;
    org.apache.catalina.User dbuser = this.udb.findUser(user.getId());
    if (dbuser != null) {
      if (user.getPassword() != null) dbuser.setPassword(user.getPassword());
      dbuser.setFullName(user.getFullname());

      // Reset all roles
      dbuser.removeRoles();
      this.setUserRole(user.getId());
      if (user.isAdmin()) this.setUserAdminRole(user.getId());
    }
    User curuser = this.getUser(user.getId());
    if (curuser != null) this.users.remove(curuser);
    this.users.add(new User(dbuser));
    return true;
  }

  public boolean removeUser(String uname) {
    if (this.udb == null) return false;
    org.apache.catalina.User user = this.udb.findUser(uname);
    User userx = this.getUser(uname);
    if (user != null) {
      this.udb.removeUser(user);
      this.users.remove(userx);
      return true;
    }
    return false;
  }

  public boolean setUserRole(String uname) {
    return this.setUserRole(uname, WINGS_USER_ROLE, "Wings User");
  }

  public boolean setUserAdminRole(String uname) {
    return this.setUserRole(uname, WINGS_ADMIN_ROLE, "Wings Admin");
  }

  private boolean setUserRole(String uname, String rolename, String desc) {
    org.apache.catalina.User user = this.udb.findUser(uname);
    if (user != null) {
      Role role = this.udb.findRole(rolename);
      if (role == null) role = this.udb.createRole(rolename, desc);
      if (role != null) {
        user.addRole(role);
        return true;
      }
    }
    return false;
  }

  public boolean hasUser(String uname) {
    // If udb doesn't work, just return true
    if (this.udb == null) return true;
    return this.getUser(uname) != null;
  }

  public ArrayList<User> getUsers() {
    if (this.udb == null) return null;
    return this.users;
  }

  public ArrayList<String> getUsersList() {
    ArrayList<String> userids = new ArrayList<String>();
    for (User user : this.users) {
      userids.add(user.getId());
    }
    return userids;
  }

  private void initializeUserDatabase() {
    try {
      Context ic = new InitialContext();
      this.udb = (UserDatabase) ic.lookup("java:comp/env/users");
    } catch (Exception e) {
      //e.printStackTrace();
      System.err.println("Could not initialize UserDB");
    }
  }

  private void initializeUserIds() {
    this.users = new ArrayList<User>();

    if (this.udb == null) return;
    for (
      Iterator<org.apache.catalina.User> userit = this.udb.getUsers();
      userit.hasNext();
    ) {
      boolean ok = false;
      org.apache.catalina.User user = userit.next();
      for (Iterator<Role> rit = user.getRoles(); rit.hasNext();) {
        String rolename = rit.next().getRolename();
        if (
          rolename.equals(WINGS_USER_ROLE) || rolename.equals(WINGS_ADMIN_ROLE)
        ) {
          ok = true;
          break;
        }
      }
      if (ok) this.users.add(new User(user));
    }
  }
}
