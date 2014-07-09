package edu.isi.wings.catalog.resource.classes;

import java.util.ArrayList;

public class Software extends Resource {
  private static final long serialVersionUID = 2206567086377827749L;

  private String name;
  private ArrayList<SoftwareVersion> versions;
  private ArrayList<String> environmentVariables;
  
  public Software(String id) {
    super(id);
    this.environmentVariables = new ArrayList<String>();
    this.versions = new ArrayList<SoftwareVersion>();
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ArrayList<String> getEnvironmentVariables() {
    return environmentVariables;
  }

  public void setEnvironmentVariables(ArrayList<String> environmentVariables) {
    this.environmentVariables = environmentVariables;
  }

  public void addEnvironmentVariable(String environmentVariable) {
    this.environmentVariables.add(environmentVariable);
  }

  public ArrayList<SoftwareVersion> getVersions() {
    return versions;
  }

  public void setVersions(ArrayList<SoftwareVersion> versions) {
    this.versions = versions;
  }

  public void addVersion(SoftwareVersion version) {
    this.versions.add(version);
  }
}
