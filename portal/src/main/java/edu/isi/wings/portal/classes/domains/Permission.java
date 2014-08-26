package edu.isi.wings.portal.classes.domains;

public class Permission {
  String userid;
  boolean canRead;
  boolean canWrite;
  boolean canExecute;
  
  public Permission(String userid, boolean canRead, boolean canWrite,
      boolean canExecute) {
    super();
    this.userid = userid;
    this.canRead = canRead;
    this.canWrite = canWrite;
    this.canExecute = canExecute;
  }

  public Permission(String userid) {
    super();
    this.userid = userid;
  }

  public String getUserid() {
    return userid;
  }

  public void setUserid(String userid) {
    this.userid = userid;
  }

  public boolean canRead() {
    return canRead;
  }

  public void setCanRead(boolean canRead) {
    this.canRead = canRead;
  }

  public boolean canWrite() {
    return canWrite;
  }

  public void setCanWrite(boolean canWrite) {
    this.canWrite = canWrite;
  }

  public boolean canExecute() {
    return canExecute;
  }

  public void setCanExecute(boolean canExecute) {
    this.canExecute = canExecute;
  }
}
