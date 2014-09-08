package edu.isi.wings.catalog.provenance.classes;

import java.util.ArrayList;

public class Provenance {
  private String objectId;
  private ArrayList<ProvActivity> activities;

  public Provenance(String objectId) {
    super();
    this.objectId = objectId;
    this.activities = new ArrayList<ProvActivity>();
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public ArrayList<ProvActivity> getActivities() {
    return activities;
  }

  public void setActivities(ArrayList<ProvActivity> activities) {
    this.activities = activities;
  }

  public void addActivity(ProvActivity activity) {
    this.activities.add(activity);
  }
}
