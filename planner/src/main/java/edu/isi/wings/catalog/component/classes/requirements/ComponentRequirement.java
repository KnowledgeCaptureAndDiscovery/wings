package edu.isi.wings.catalog.component.classes.requirements;

import java.util.ArrayList;

public class ComponentRequirement {
  private float storageGB;
  private float memoryGB;
  private boolean need64bit;
  private ArrayList<String> softwareIds;

  public ComponentRequirement() {
    softwareIds = new ArrayList<String>();
  }

  public float getStorageGB() {
    return storageGB;
  }

  public void setStorageGB(float storageGB) {
    this.storageGB = storageGB;
  }

  public float getMemoryGB() {
    return memoryGB;
  }

  public void setMemoryGB(float memoryGB) {
    this.memoryGB = memoryGB;
  }

  public boolean isNeed64bit() {
    return need64bit;
  }

  public void setNeed64bit(boolean need64bit) {
    this.need64bit = need64bit;
  }

  public ArrayList<String> getSoftwareIds() {
    return softwareIds;
  }

  public void addSoftwareId(String softwareId) {
    this.softwareIds.add(softwareId);
  }

  public String toString() {
    String str = "Softwares: "+this.softwareIds.toString()+"\n";
    str += "MemoryGB: "+this.memoryGB+"\n";
    str += "StorageGB: "+this.storageGB+"\n";
    str += "Needs64bit: "+this.need64bit;
    return str;
  }
}
