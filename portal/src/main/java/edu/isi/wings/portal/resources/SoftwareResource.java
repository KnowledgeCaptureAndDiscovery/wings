package edu.isi.wings.portal.resources;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import edu.isi.wings.portal.controllers.ResourceController;

@Path("common/resources")
public class SoftwareResource extends WingsResource {
  ResourceController rc;

  @PostConstruct
  public void init() {
    super.init();
    if(this.hasPermissions() && !this.isPage("intro"))
      this.rc = new ResourceController(config);
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML() {
    if(this.rc != null) {
      request.setAttribute("controller", this.rc);
      return this.callViewer("SoftwareViewer");
    }
    return null;
  }
  
  @GET
  @Path("intro")
  public void getIntroduction() {
    this.loadIntroduction("Resources");
  }
  
  @GET
  @Path("getMachineJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getComponentJSON(  
      @QueryParam("resid") String resid) {
    if(this.rc != null)
      return this.rc.getMachineJSON(resid);
    return null;
  }
  
  @GET
  @Path("getSoftwareJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getSoftwareJSON(   
      @QueryParam("resid") String resid) {
    if(this.rc != null)
      return this.rc.getSoftwareJSON(resid);
    return null;
  }
  
  @GET
  @Path("getSoftwareVersionJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getSoftwareVersionJSON(  
      @QueryParam("resid") String resid) {
    if(this.rc != null)
      return this.rc.getSoftwareVersionJSON(resid);
    return null;
  }
  
  @GET
  @Path("getAllSoftwareVersions")
  @Produces(MediaType.APPLICATION_JSON)
  public String getAllSoftwareVersions() {
    if(this.rc != null)
      return this.rc.getAllSoftwareVersions();
    return null;
  }

  @GET
  @Path("getAllSoftwareEnvironment")
  @Produces(MediaType.APPLICATION_JSON)
  public String getAllSoftwareEnvironment() {
    if(this.rc != null)
      return this.rc.getAllSoftwareEnvironment();
    return null;
  }
  
  @POST
  @Path("checkMachine")
  @Produces(MediaType.APPLICATION_JSON)
  public String checkMachine(  
      @FormParam("resid") String resid) {
    if(this.rc != null)
      return this.rc.checkMachine(resid);
    return null;
  }

  @POST
  @Path("addMachine")
  @Produces(MediaType.TEXT_PLAIN)
  public String addMachine(  
      @FormParam("resid") String resid) {
    if(this.rc != null)
      if(this.rc.addMachine(resid))
        return "OK";
    return null;
  }
  
  @POST
  @Path("addSoftware")
  @Produces(MediaType.TEXT_PLAIN)
  public String addSoftware(  
      @FormParam("resid") String resid) {
    if(this.rc != null)
      if(this.rc.addSoftware(resid))
        return "OK";
    return null;
  }
  
  @POST
  @Path("addSoftwareVersion")
  @Produces(MediaType.TEXT_PLAIN)
  public String addSoftwareVersion(  
      @FormParam("resid") String resid,
      @FormParam("softwareid") String softwareid) {
    if(this.rc != null)
      if(this.rc.addSoftwareVersion(resid, softwareid))
        return "OK";
    return null;
  }
  
  @POST
  @Path("saveMachineJSON")
  @Produces(MediaType.TEXT_PLAIN)
  public String saveMachineJSON(  
      @FormParam("resid") String resid,
      @FormParam("json") String json) {
    if(this.rc != null)
      if(this.rc.saveMachineJSON(resid, json))
        return "OK";
    return null;
  }
  
  @POST
  @Path("saveSoftwareJSON")
  @Produces(MediaType.TEXT_PLAIN)
  public String saveSoftwareJSON(  
      @FormParam("resid") String resid,
      @FormParam("json") String json) {
    if(this.rc != null)
      if(this.rc.saveSoftwareJSON(resid, json))
        return "OK";
    return null;
  }
  
  @POST
  @Path("saveSoftwareVersionJSON")
  @Produces(MediaType.TEXT_PLAIN)
  public String saveSoftwareVersionJSON(  
      @FormParam("resid") String resid,
      @FormParam("json") String json) {
    if(this.rc != null)
      if(this.rc.saveSoftwareVersionJSON(resid, json))
        return "OK";
    return null;
  }
  
  @POST
  @Path("removeMachine")
  @Produces(MediaType.TEXT_PLAIN)
  public String removeMachine(  
      @FormParam("resid") String resid) {
    if(this.rc != null)
      if(this.rc.removeMachine(resid))
        return "OK";
    return null;
  }
  
  @POST
  @Path("removeSoftware")
  @Produces(MediaType.APPLICATION_JSON)
  public String removeSoftware(  
      @FormParam("resid") String resid) {
    if(this.rc != null)
      if(this.rc.removeSoftware(resid))
        return "OK";
    return null;
  }
  
  @POST
  @Path("removeSoftwareVersion")
  @Produces(MediaType.TEXT_PLAIN)
  public String removeSoftwareVersion(  
      @FormParam("resid") String resid) {
    if(this.rc != null)
      if(this.rc.removeSoftwareVersion(resid))
        return "OK";
    return null;
  }

}