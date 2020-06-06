package edu.isi.wings.portal.resources;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import edu.isi.wings.portal.controllers.ComponentController;
import edu.isi.wings.portal.controllers.RunController;

@Path("{user}/{domain}/components{type:(/type)?}{external:(/external)?}")
public class ComponentResource extends WingsResource {
  ComponentController cc;

  @PathParam("external") String external;
  @PathParam("type") String type;
  
  boolean loadExternal, loadConcrete;
  
  @PostConstruct
  public void init() {
    super.init();
    this.loadExternal = "/external".equals(external);
    this.loadConcrete = !"/type".equals(type);
    
    if(this.hasPermissions() && !this.isPage("intro"))
      this.cc = new ComponentController(config, loadConcrete, loadExternal);
  }
  
  @PreDestroy
  public void destroy() {
    if(this.cc != null) {
      this.cc.end();
    }
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML() {
    if(this.cc != null) {
      request.setAttribute("controller", this.cc);
      return this.callViewer("ComponentViewer");
    }
    return null;
  }
  
  @GET
  @Path("intro")
  public void getIntroduction() {
    String introPage = "ManageComponent" + (loadConcrete ? "s" : "Types");
    this.loadIntroduction(introPage);
  }
  
  @GET
  @Path("getComponentJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getComponentJSON(  
      @QueryParam("cid") String cid) {
    if(this.cc != null)
      return this.cc.getComponentJSON(cid);
    return null;
  }
  
  @GET
  @Path("getComponentHierarchyJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getComponentHierarchyJSON() {
    if(this.cc != null)
      return cc.json.toJson(cc.cc.getComponentHierarchy(false).getRoot());
    return null;
  }
  
  @GET
  @Path("fetch")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response fetchComponent(
      @QueryParam("cid") String cid) {
    if(this.cc != null)
      return this.cc.streamComponent(cid, context);
    return Response.status(Status.FORBIDDEN).build();    
  }
  
  @POST
  @Path("saveComponentJSON")
  @Produces(MediaType.TEXT_PLAIN)
  public String saveComponentJSON(
      @FormParam("cid") String cid,
      @FormParam("component_json") String json) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.saveComponentJSON(cid, json) && 
        this.cc.incrementComponentVersion(cid)) {
      RunController.invalidateCachedAPIs();
      return "OK";
    }
    return null;
  }
  
  @POST
  @Path("addComponent")
  @Produces(MediaType.TEXT_PLAIN)
  public String addComponent(
      @FormParam("cid") String cid,
      @FormParam("parent_cid") String parent_cid,
      @FormParam("parent_type") String parent_type) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() && 
        this.cc.addComponent(cid, parent_cid, parent_type)) {
      RunController.invalidateCachedAPIs();
      return "OK";
    }
    return null;
  }
  
  @POST
  @Path("addCategory")
  @Produces(MediaType.TEXT_PLAIN)
  public String addCategory(
      @FormParam("cid") String cid,
      @FormParam("parent_type") String parent_type) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() && 
        this.cc.addCategory(cid, parent_type)) {
      RunController.invalidateCachedAPIs();
      return "OK";
    }
    return null;
  }
  
  @POST
  @Path("delComponent")
  @Produces(MediaType.TEXT_PLAIN)
  public String delComponent(
      @FormParam("cid") String cid) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.delComponent(cid)) {
      RunController.invalidateCachedAPIs();
      return "OK";
    }
    return null;
  }

  @POST
  @Path("duplicateComponent")
  @Produces(MediaType.TEXT_PLAIN)
  public String duplicateComponent(
          @FormParam("new_cid") String new_cid,
          @FormParam("cid") String cid,
          @FormParam("parent_cid") String parent_cid,
          @FormParam("parent_type") String parent_type) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
            this.cc.duplicateComponent(cid, parent_cid, parent_type, new_cid)) {
      RunController.invalidateCachedAPIs();
      return "OK";
    }
    return null;
  }
  
  @POST
  @Path("delCategory")
  @Produces(MediaType.TEXT_PLAIN)
  public String delCategory(
      @FormParam("cid") String cid) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.delCategory(cid)) {
      RunController.invalidateCachedAPIs();
      return "OK";
    }
    return null;
  }
  
  @POST
  @Path("setComponentLocation")
  @Produces(MediaType.TEXT_PLAIN)
  public String setComponentLocation(
      @FormParam("cid") String cid,
      @FormParam("location") String location) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.setComponentLocation(cid, location) && 
        this.cc.incrementComponentVersion(cid)) {
      RunController.invalidateCachedAPIs();
      return "OK";
    }
    return null;
  }
  
  /*
   * Component directory filebrowser functions
   */
  @GET
  @Path("fb/list")
  @Produces(MediaType.APPLICATION_JSON)
  public String listComponentDirectory(
      @QueryParam("cid") String cid,
      @QueryParam("path") String path) {
    if(this.cc != null) {
      return this.cc.listComponentDirectory(cid, path);
    }
    return null;
  }
  
  @GET
  @Path("fb/get")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response fetchComponentFile(
      @QueryParam("cid") String cid,
      @QueryParam("path") String path) {
    if(this.cc != null)
      return this.cc.streamComponentFile(cid, path, context);
    return Response.status(Status.FORBIDDEN).build(); 
  }
  
  @POST
  @Path("fb/addDirectory")
  @Produces(MediaType.TEXT_PLAIN)
  public String addComponentDirectory(
      @FormParam("cid") String cid,
      @FormParam("path") String path) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.addComponentDirectory(cid, path))
      return "OK";
    return null;
  }
  
  @POST
  @Path("fb/addFile")
  @Produces(MediaType.TEXT_PLAIN)
  public String addComponentFile(
      @FormParam("cid") String cid,
      @FormParam("path") String path) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.addComponentFile(cid, path))
      return "OK";
    return null;
  }
  
  @POST
  @Path("fb/save")
  @Produces(MediaType.TEXT_PLAIN)
  public String saveComponentFile(
      @FormParam("cid") String cid,
      @FormParam("path") String path,
      @FormParam("filedata") String data) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.saveComponentFile(cid, path, data) && 
        this.cc.incrementComponentVersion(cid))
      return "OK";
    return null;
  }
  
  @POST
  @Path("fb/delete")
  @Produces(MediaType.TEXT_PLAIN)
  public String deleteComponentItem(
      @FormParam("cid") String cid,
      @FormParam("path") String path) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.deleteComponentItem(cid, path))
      return "OK";
    return null;
  }
  
  @POST
  @Path("fb/rename")
  @Produces(MediaType.TEXT_PLAIN)
  public String renameComponentItem(
      @FormParam("cid") String cid,
      @FormParam("path") String path,
      @FormParam("newname") String newname) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.renameComponentItem(cid, path, newname) && 
        this.cc.incrementComponentVersion(cid))
      return "OK";
    return null;
  }  
  
  @POST
  @Path("fb/initialize")
  @Produces(MediaType.TEXT_PLAIN)
  public String initializeComponentFiles(
      @FormParam("cid") String cid,
      @FormParam("language") String lang) {
    if(this.cc != null && this.isOwner() && !config.isSandboxed() &&
        this.cc.initializeComponentFiles(cid, lang) && 
        this.cc.incrementComponentVersion(cid))
      return "OK";
    return null;
  }
}