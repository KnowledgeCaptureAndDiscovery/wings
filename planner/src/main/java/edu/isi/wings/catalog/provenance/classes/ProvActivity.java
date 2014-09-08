package edu.isi.wings.catalog.provenance.classes;

public class ProvActivity {
  private String id;
  private String userId;
  private long time;
  private String log;
  private String type;
  private String objectId;
  
  public static String CREATE = "Create";
  public static String UPDATE = "Update";
  public static String UPLOAD = "Upload";

  public ProvActivity(String id) {
    this.id = id;
  }

  public ProvActivity(String type, String log) {
    super();
    this.type = type;
    this.log = log;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public String getLog() {
    return log;
  }

  public void setLog(String log) {
    this.log = log;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

}
