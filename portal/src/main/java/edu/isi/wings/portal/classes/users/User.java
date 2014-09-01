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
