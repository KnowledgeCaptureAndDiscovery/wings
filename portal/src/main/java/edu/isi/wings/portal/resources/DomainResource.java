package edu.isi.wings.portal.resources;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import edu.isi.wings.portal.controllers.DomainController;

@Path("{user}/domains")
public class DomainResource extends WingsResource {
  DomainController dc;
  
  @PostConstruct
  public void init() {
    super.init();
    if(this.hasPermissions())
      this.dc = new DomainController(config);
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML() {
    if(this.dc != null) {
      request.setAttribute("controller", this.dc);
      return this.callViewer("DomainViewer");
    }
    return null;
  }
  
  @GET
  @Path("getDomainDetails")
  @Produces(MediaType.APPLICATION_JSON)
  public String getDomainDetails(@QueryParam("domain") String domain) {
    if(this.dc != null)
      return this.dc.getDomainJSON(domain);
    return null;
  }
 
  @GET
  @Path("downloadDomain")
  @Produces("application/octet-stream")
  public Response downloadDomain(@QueryParam("domain") String domain) {
    if(this.dc != null)
      return this.dc.streamDomain(domain, this.context);
    return Response.status(Status.FORBIDDEN).build();
  }
  
  @POST
  @Path("selectDomain")
  @Produces(MediaType.TEXT_PLAIN)
  public String selectDomain(@FormParam("domain") String domain) {
    if(this.dc != null && this.isOwner() &&
        this.dc.selectDomain(domain))
      return "OK";
    return null;
  }
  
  @POST
  @Path("createDomain")
  @Produces(MediaType.APPLICATION_JSON)
  public String createDomain(@FormParam("domain") String domain) {
    if(this.dc != null && this.isOwner())
      return this.dc.createDomain(domain);
    return null;
  }
  
  @POST
  @Path("deleteDomain")
  @Produces(MediaType.TEXT_PLAIN)
  public String deleteDomain(@FormParam("domain") String domain) {
    if(this.dc != null && this.isOwner() &&
        this.dc.deleteDomain(domain))
      return "OK";
    return null;
  }
  
  @POST
  @Path("renameDomain")
  @Produces(MediaType.TEXT_PLAIN)
  public String renameDomain(
      @FormParam("domain") String domain,
      @FormParam("newname") String newname) {
    if(this.dc != null && this.isOwner() &&
        this.dc.renameDomain(domain, newname))
      return "OK";
    return null;
  }
  
  @POST
  @Path("importDomain")
  @Produces(MediaType.APPLICATION_JSON)
  public String importDomain(
      @FormParam("domain") String domain,
      @FormParam("location") String location) {
    if(this.dc != null && this.isOwner())
      return this.dc.importDomain(domain, location);
    return null;
  }

  @POST
  @Path("setDomainExecutionEngine")
  @Produces(MediaType.TEXT_PLAIN)
  public String setDomainExecutionEngine(
      @FormParam("domain") String domain,
      @FormParam("engine") String engine) {
    if(this.dc != null && this.isOwner() &&
        this.dc.setDomainExecutionEngine(domain, engine))
      return "OK";
    return null;
  }

  @POST
  @Path("setDomainPermissions")
  @Produces(MediaType.TEXT_PLAIN)
  public String setDomainPermissions(
      @FormParam("domain") String domain,
      @FormParam("permissions_json") String json) {
    if(this.dc != null && this.isOwner() &&
        this.dc.setDomainPermissions(domain, json))
      return "OK";
    return null;
  }

}
