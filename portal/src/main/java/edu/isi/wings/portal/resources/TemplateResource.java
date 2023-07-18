package edu.isi.wings.portal.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.isi.wings.portal.controllers.RunController;
import edu.isi.wings.portal.controllers.TemplateController;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
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

@Path("{user}/{domain}/workflows{edit:(/edit)?}{tell:(/tellme)?}")
public class TemplateResource extends WingsResource {

  TemplateController tc;

  @PathParam("edit")
  String edit;

  @PathParam("tell")
  String tell;

  HashMap<String, Boolean> options;
  boolean editor;
  boolean tellme;

  @PostConstruct
  public void init() {
    super.init();

    this.editor = ("/edit".equals(edit));
    this.tellme = ("/tellme".equals(tell));

    if (this.hasPermissions() && !this.isPage("intro")) this.tc =
      new TemplateController(config);

    this.options = new HashMap<String, Boolean>();
    @SuppressWarnings("unchecked")
    Enumeration<String> pnames = request.getParameterNames();
    while (pnames.hasMoreElements()) {
      String pname = pnames.nextElement();
      if (pname.matches("^hide_.*")) this.options.put(
          pname,
          Boolean.parseBoolean(request.getParameter(pname))
        );
    }
  }

  @PreDestroy
  public void destroy() {
    System.out.println("Template Resource Destroyed");
    if (this.tc != null) {
      this.tc.end();
    }
  }

  @GET
  @Path("intro")
  public void getIntroduction() {
    String intropage = "Template";
    if (!editor) intropage += "Browser"; else intropage +=
      "Editor" + (tellme ? "Tellme" : "");
    this.loadIntroduction(intropage);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML(@QueryParam("template_id") final String template_id) {
    if (this.tc != null) {
      request.setAttribute("controller", this.tc);
      request.setAttribute("options", this.options);
      request.setAttribute("editor", this.editor);
      request.setAttribute("tellme", this.tellme);
      return this.callViewer("TemplateViewer");
    }
    return null;
  }

  @GET
  @Path("{template_name}.owl")
  @Produces(MediaType.TEXT_HTML)
  public String getTemplateHTML(
    @PathParam("template_name") String template_name
  ) {
    if (this.tc != null) {
      config.setScriptPath(
        config.getScriptPath().replaceAll("/" + template_name + ".owl", "")
      );
      this.options.put("hide_selector", true);

      request.setAttribute("controller", this.tc);
      request.setAttribute("options", this.options);
      request.setAttribute("editor", this.editor);
      request.setAttribute("tellme", this.tellme);
      request.setAttribute("template_name", template_name);
      return this.callViewer("TemplateViewer");
    }
    return null;
  }

  @GET
  @Path("getTemplatesListJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getTemplatesListJSON() {
    if (this.tc != null) return this.tc.getTemplatesListJSON();
    return null;
  }

  @GET
  @Path("getViewerJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getViewerJSON(@QueryParam("template_id") String template_id) {
    if (this.tc != null) return this.tc.getViewerJSON(template_id);
    return null;
  }

  @GET
  @Path("getInputsJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getInputsJSON(@QueryParam("template_id") String template_id) {
    if (this.tc != null) return this.tc.getInputsJSON(template_id);
    return null;
  }

  @GET
  @Path("getEditorJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public String getEditorJSON(@QueryParam("template_id") String template_id) {
    if (this.tc != null) return this.tc.getEditorJSON(template_id);
    return null;
  }

  @POST
  @Path("layoutTemplate")
  @Produces(MediaType.APPLICATION_JSON)
  public String layoutTemplate(@JsonProperty("json") String json) {
    String dotexe = config.portalConfig.getDotFile();
    if (this.tc != null) try {
      return this.tc.layoutTemplate(json, dotexe);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @POST
  @Path("saveTemplateJSON")
  @Produces(MediaType.TEXT_PLAIN)
  public String saveTemplateJSON(
    @FormParam("template_id") String template_id,
    @FormParam("json") String json,
    @FormParam("constraints_json") String constraints_json
  ) {
    if (this.tc != null) return this.tc.saveTemplateJSON(
        template_id,
        json,
        constraints_json
      );
    return null;
  }

  @POST
  @Path("newTemplate")
  @Produces(MediaType.TEXT_PLAIN)
  public String newTemplate(@FormParam("template_id") String template_id) {
    if (this.tc != null) {
      RunController.invalidateCachedAPIs();
      return this.tc.newTemplate(template_id);
    }
    return null;
  }

  @POST
  @Path("deleteTemplate")
  @Produces(MediaType.TEXT_PLAIN)
  public String deleteTemplate(@FormParam("template_id") String template_id) {
    if (this.tc != null && this.isOwner() && !config.portalConfig.isSandboxed()) {
      RunController.invalidateCachedAPIs();
      return this.tc.deleteTemplate(template_id);
    }
    return null;
  }
}
