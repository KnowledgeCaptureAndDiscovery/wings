package edu.isi.wings.catalog.component.classes.requirements;

import java.util.ArrayList;

import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;

public class Dependency {
  public static enum Operator {
    EQUAL, LESS_THAN, GREATER_THAN
  };

  private ArrayList<String> propertyIdChain;
  private Object desiredValue;
  private Operator operator;

  public Dependency(Object desiredValue, Operator operator) {
    this.propertyIdChain = new ArrayList<String>();
    this.desiredValue = desiredValue;
    this.operator = operator;
  }

  public ArrayList<String> getPropertyIdChain() {
    return propertyIdChain;
  }

  public void setPropertyIdChain(ArrayList<String> propertyIdChain) {
    this.propertyIdChain = propertyIdChain;
  }

  public void addPropertyIdToChain(String propertyId) {
    this.propertyIdChain.add(propertyId);
  }

  public boolean checkDependency(KBAPI api, KBObject obj) {
    for (String propId : propertyIdChain) {
      KBObject prop = api.getProperty(propId);
      obj = api.getPropertyValue(obj, prop);
      if (obj == null)
        break;
    }
    if(obj == null) return false;
    if(!obj.isLiteral()) {
      if(this.operator == Operator.EQUAL
        && obj.getID().equals(this.desiredValue))
        return true;
    }
    else {
      if(this.operator == Operator.EQUAL
          && obj.getValue().equals(this.desiredValue))
        return true;
      if(this.operator == Operator.GREATER_THAN
          && Double.parseDouble(obj.getValue().toString()) > 
              Double.parseDouble(this.desiredValue.toString()))
        return true;
      if(this.operator == Operator.LESS_THAN
          && Double.parseDouble(obj.getValue().toString()) < 
              Double.parseDouble(this.desiredValue.toString()))
        return true;
    }
    return false;
  }

  public Object getDesiredValue() {
    return desiredValue;
  }

  public void setDesiredValue(Object value) {
    this.desiredValue = value;
  }

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }
}
