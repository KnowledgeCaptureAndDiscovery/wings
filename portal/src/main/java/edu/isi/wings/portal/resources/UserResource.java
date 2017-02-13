package edu.isi.wings.portal.resources;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import edu.isi.wings.portal.controllers.UserController;

@Path("common/list")
public class UserResource extends WingsResource {
  UserController uc;

  @PostConstruct
  public void init() {
    super.init();

    if(this.hasPermissions() && !this.isPage("intro"))
      this.uc = new UserController(config);
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML() {
    if(this.uc != null) {
      request.setAttribute("controller", this.uc);
      return this.callViewer("UserViewer");
    }
    return null;
  }
  
  @GET
  @Path("intro")
  public void getIntroduction() {
    this.loadIntroduction("ManageUsers");
  }
  
  @GET
  @Path("getUserJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getUserJSON(  
      @QueryParam("userid") String userid) {
    if(this.uc != null)
      return this.uc.getUserJSON(userid);
    return null;
  }

  @POST
  @Path("saveUserJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String saveUserJSON(
      @FormParam("userid") String userid,
      @FormParam("json") String uservals_json) {
    if(this.uc != null)
      if(this.uc.saveUserJSON(userid, uservals_json))
        return "OK";
    return null;
  }
  
  @POST
  @Path("addUser")
  @Produces(MediaType.APPLICATION_JSON)
  public String addUser(
      @FormParam("userid") String userid) {
    if(this.uc != null)
      if(this.uc.addUser(userid))
        return "OK";
    return null;
  }
  
  @POST
  @Path("removeUser")
  @Produces(MediaType.APPLICATION_JSON)
  public String removeUser(
      @FormParam("userid") String userid) {
    if(this.uc != null)
      if(this.uc.removeUser(userid))
        return "OK";
    return null;
  }

}