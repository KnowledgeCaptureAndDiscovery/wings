package edu.isi.wings.portal.resources;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.classes.ProvActivity;

@Path("common/provenance")
public class ProvResource extends WingsResource {
  ProvenanceAPI prov;
  
  @PostConstruct
  public void init() {
    super.init();
    if(this.hasPermissions() && config.isAdminViewer())
      this.prov = ProvenanceFactory.getAPI(config.getProperties());
  }
  
  @GET
  @Path("getItemProvenance")
  @Produces(MediaType.APPLICATION_JSON)
  public ArrayList<ProvActivity> getItemProvenance(  
      @QueryParam("id") String id) {
    if(this.prov != null) {
      return this.prov.getProvenance(id).getActivities();
    }
    return null;
  }
  
  @GET
  @Path("getUserActivity")
  @Produces(MediaType.APPLICATION_JSON)
  public ArrayList<ProvActivity> getUserActivity(  
      @QueryParam("userid") String userid) {
    if(this.prov != null) {
      return this.prov.getAllUserActivities(userid);
    }
      
    return null;
  }

}