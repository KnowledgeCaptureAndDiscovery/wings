package edu.isi.wings.portal.resources;

import edu.isi.wings.portal.controllers.MetaworkflowController;
import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("{user}/{domain}/metaworkflows")
public class MetaworkflowResource extends WingsResource {

  MetaworkflowController mc;

  @PostConstruct
  public void init() {
    super.init();
    if (this.hasPermissions()) this.mc = new MetaworkflowController(config);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML() {
    if (this.mc != null) {
      request.setAttribute("controller", this.mc);
      return this.callViewer("MetaworkflowViewer");
    }
    return null;
  }

  @GET
  @Path("intro")
  public void getIntroduction() {
    this.loadIntroduction("Metaworkflows");
  }
}
