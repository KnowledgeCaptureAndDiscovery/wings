package edu.isi.wings.catalog.resource.classes;

public class SoftwareVersion extends Resource {
  private static final long serialVersionUID = -724942936669949377L;
  
  private String softwareGroupId;
  private int versionNumber;
  private String versionText;
  
  public SoftwareVersion(String id) {
    super(id);
  }

  public String getSoftwareGroupId() {
    return softwareGroupId;
  }

  public void setSoftwareGroupId(String softwareGroupId) {
    this.softwareGroupId = softwareGroupId;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(int versionNumber) {
    this.versionNumber = versionNumber;
  }

  public String getVersionText() {
    return versionText;
  }

  public void setVersionText(String versionText) {
    this.versionText = versionText;
  }
}
