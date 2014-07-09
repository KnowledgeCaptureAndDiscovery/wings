package edu.isi.wings.catalog.resource.classes;

import java.io.Serializable;

public class EnvironmentValue implements Serializable {
  private static final long serialVersionUID = 8526627538063714345L;
  private String variable;
  private String value;

  public String getVariable() {
    return variable;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}
