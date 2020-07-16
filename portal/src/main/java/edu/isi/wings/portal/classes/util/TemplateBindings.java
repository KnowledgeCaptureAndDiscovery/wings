package edu.isi.wings.portal.classes.util;

import java.util.ArrayList;
import java.util.HashMap;

public class TemplateBindings {
  String templateId;
  HashMap<String, ArrayList<String>> dataBindings;
  HashMap<String, String> componentBindings;
  HashMap<String, ArrayList<Object>> parameterBindings;
  HashMap<String, String> parameterTypes;
  String callbackUrl;
  String callbackCookies;

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  public HashMap<String, ArrayList<String>> getDataBindings() {
    return dataBindings;
  }

  public void setDataBindings(HashMap<String, ArrayList<String>> dataBindings) {
    this.dataBindings = dataBindings;
  }

  public HashMap<String, String> getComponentBindings() {
    return componentBindings;
  }

  public void setComponentBindings(HashMap<String, String> componentBindings) {
    this.componentBindings = componentBindings;
  }

  public HashMap<String, ArrayList<Object>> getParameterBindings() {
    return parameterBindings;
  }

  public void setParameterBindings(HashMap<String, ArrayList<Object>> parameterBindings) {
    this.parameterBindings = parameterBindings;
  }

  public HashMap<String, String> getParameterTypes() {
    return parameterTypes;
  }

  public void setParameterTypes(HashMap<String, String> parameterTypes) {
    this.parameterTypes = parameterTypes;
  }

  public String getCallbackUrl() {
    return callbackUrl;
  }

  public void setCallbackUrl(String callbackUrl) {
    this.callbackUrl = callbackUrl;
  }

  public String getCallbackCookies() {
    return callbackCookies;
  }

  public void setCallbackCookies(String callbackCookies) {
    this.callbackCookies = callbackCookies;
  }
}
