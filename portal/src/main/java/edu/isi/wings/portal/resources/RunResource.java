package edu.isi.wings.portal.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;

import edu.isi.kcap.wings.opmm.DataTypes.Links;
import edu.isi.kcap.wings.opmm.DataTypes.ProvenanceResponseSchema;
import edu.isi.wings.portal.classes.util.TemplateBindings;
import edu.isi.wings.portal.controllers.RunController;

@Path("{user}/{domain}/executions")
public class RunResource extends WingsResource {
  RunController rc;

  @PostConstruct
  public void init() {
    super.init();
    if (this.hasPermissions() && !this.isPage("intro"))
      this.rc = new RunController(config);
  }

  @PreDestroy
  public void destroy() {
    if (this.rc != null) {
      this.rc.end();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML(
      @QueryParam("run_id") final String run_id) {
    if (this.rc != null) {
      request.setAttribute("controller", this.rc);
      request.setAttribute("run_id", run_id);
      return this.callViewer("RunViewer");
    }
    return null;
  }

  @GET
  @Path("intro")
  public void getIntroduction() {
    this.loadIntroduction("ManageRuns");
  }

  @GET
  @Path("getRunList")
  @Produces(MediaType.APPLICATION_JSON)
  public String getRunList(
      @QueryParam("pattern") final String pattern,
      @QueryParam("status") final String status,
      @QueryParam("start") int start,
      @QueryParam("limit") int limit) {
    if (this.rc != null)
      return this.rc.getRunListJSON(pattern, status, start, limit);
    return null;
  }

  @GET
  @Path("getRunListSimple")
  @Produces(MediaType.APPLICATION_JSON)
  public String getRunListSimple(
      @QueryParam("pattern") final String pattern,
      @QueryParam("status") final String status,
      @QueryParam("start") int start,
      @QueryParam("limit") int limit,
      @QueryParam("started_after") final long started_after) {
    if (this.rc != null) {
      Date startdate = null;
      if (started_after != 0)
        startdate = new Date(started_after * 1000);
      return this.rc.getRunListSimpleJSON(pattern, status, start, limit, startdate);
    }
    return null;
  }

  @POST
  @Path("getRunDetails")
  @Produces(MediaType.APPLICATION_JSON)
  public String getRunDetails(
      @FormParam("run_id") String run_id) {
    if (this.rc != null)
      return this.rc.getRunJSON(run_id);
    return null;
  }

  @POST
  @Path("getRunPlan")
  @Produces(MediaType.APPLICATION_JSON)
  public String getRunPlan(
      @FormParam("run_id") String run_id) {
    if (this.rc != null)
      return this.rc.getRunPlanJSON(run_id);
    return null;
  }

  @POST
  @Path("expandAndRunWorkflow")
  @Produces(MediaType.APPLICATION_JSON)
  public ArrayList<String> expandAndRunWorkflow(
      @JsonProperty("template_bindings") final TemplateBindings tbindings) {
    if (this.rc != null) {
      return rc.expandAndRunTemplate(tbindings, this.context);
    }
    return null;
  }

  @POST
  @Path("runWorkflow")
  @Produces(MediaType.TEXT_PLAIN)
  public String runWorkflow(
      @FormParam("template_id") String template_id,
      @FormParam("json") String json,
      @FormParam("constraints_json") String constraints_json,
      @FormParam("seed_json") String seed_json,
      @FormParam("seed_constraints_json") String seed_constraints_json) {
    if (this.rc != null)
      return rc.runExpandedTemplate(template_id, json, constraints_json,
          seed_json, seed_constraints_json, null, this.context);
    return null;
  }

  @POST
  @Path("reRunWorkflow")
  @Produces("application/json")
  public Response reRunWorkflow(
      @FormParam("run_id") String run_id) {
    if (this.rc != null) {
      return rc.reRunPlan(run_id, this.context);
    }
    return null;
  }

  @POST
  @Path("deleteRun")
  @Produces(MediaType.TEXT_PLAIN)
  public String deleteRun(@FormParam("json") String json) {
    if (this.rc != null)
      return rc.deleteRun(json, context);
    return null;
  }

  @POST
  @Path("deleteRuns")
  @Produces(MediaType.TEXT_PLAIN)
  public String deleteRuns(@FormParam("json") String json) {
    if (this.rc != null)
      return rc.deleteRuns(json, context);
    return null;
  }

  @POST
  @Path("stopRun")
  @Produces(MediaType.TEXT_PLAIN)
  public String stopRun(
      @FormParam("run_id") String run_id) {
    if (this.rc != null)
      return this.rc.stopRun(run_id, context) + "";
    return "false";
  }

  @POST
  @Path("publishRun")
  @Produces(MediaType.TEXT_PLAIN)
  public String publishRun(
      @FormParam("run_id") String run_id) {
    if (this.rc != null) {
      try {
        ProvenanceResponseSchema responseQuery = this.rc.publishRun(run_id);
        Gson gson = new Gson();
        String json = gson.toJson(responseQuery);
        return json;
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    return null;
  }

}