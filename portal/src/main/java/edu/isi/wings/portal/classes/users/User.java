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

import java.util.Iterator;

import org.apache.catalina.Role;

public class User {
  String id;
  String password;
  String fullname;
  boolean isAdmin;
  
  public User(String id, String password, String fullname) {
    super();
    this.id = id;
    this.password = password;
    this.fullname = fullname;
    this.isAdmin = false;
  }
  
  public User(org.apache.catalina.User user) {
    this.id = user.getUsername();
    this.fullname = user.getFullName();
    this.password = user.getPassword();
    this.isAdmin = false;
    for(Iterator<Role> roleiter = user.getRoles(); roleiter.hasNext(); ) {
      String rolename = roleiter.next().getRolename();
      if(rolename.equals(UsersDB.WINGS_ADMIN_ROLE))
        this.isAdmin = true;
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFullname() {
    return fullname;
  }

  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }

}
