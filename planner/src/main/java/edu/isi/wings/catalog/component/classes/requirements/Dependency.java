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

package edu.isi.wings.catalog.component.classes.requirements;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import java.util.ArrayList;

public class Dependency {

  public static enum Operator {
    EQUAL,
    LESS_THAN,
    GREATER_THAN,
  }

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
      if (obj == null) break;
    }
    if (obj == null) return false;
    if (!obj.isLiteral()) {
      if (
        this.operator == Operator.EQUAL && obj.getID().equals(this.desiredValue)
      ) return true;
    } else {
      if (
        this.operator == Operator.EQUAL &&
        obj.getValue().equals(this.desiredValue)
      ) return true;
      if (
        this.operator == Operator.GREATER_THAN &&
        Double.parseDouble(obj.getValueAsString()) >
        Double.parseDouble(this.desiredValue.toString())
      ) return true;
      if (
        this.operator == Operator.LESS_THAN &&
        Double.parseDouble(obj.getValueAsString()) <
        Double.parseDouble(this.desiredValue.toString())
      ) return true;
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
