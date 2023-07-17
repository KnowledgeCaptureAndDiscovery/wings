package edu.isi.wings.portal.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.isi.wings.portal.classes.util.TemplateBindings;
import edu.isi.wings.portal.controllers.PlanController;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

@Path("{user}/{domain}/plan")
public class PlannerResource extends WingsResource {

  PlanController wp;
  boolean noexplain = false;

  @PostConstruct
  public void init() {
    super.init();
    if (this.hasPermissions()) this.wp = new PlanController(config);
  }

  @PreDestroy
  public void destroy() {
    if (this.wp != null) {
      this.wp.end();
    }
  }

  @POST
  @Path("getExpansions")
  @Produces(MediaType.APPLICATION_JSON)
  public StreamingOutput getExpansions(
    @JsonProperty("template_bindings") final TemplateBindings tbindings
  ) {
    if (this.wp != null) {
      return new StreamingOutput() {
        @Override
        public void write(OutputStream os)
          throws IOException, WebApplicationException {
          PrintWriter out = new PrintWriter(os);
          wp.printExpandedTemplatesJSON(tbindings, noexplain, out);
          out.flush();
        }
      };
    }
    return null;
  }

  @POST
  @Path("getData")
  @Produces(MediaType.APPLICATION_JSON)
  public StreamingOutput getData(
    @JsonProperty("template_bindings") final TemplateBindings tbindings
  ) {
    if (this.wp != null) {
      return new StreamingOutput() {
        @Override
        public void write(OutputStream os)
          throws IOException, WebApplicationException {
          PrintWriter out = new PrintWriter(os);
          wp.printSuggestedDataJSON(tbindings, noexplain, out);
          out.flush();
        }
      };
    }
    return null;
  }

  @POST
  @Path("getParameters")
  @Produces(MediaType.APPLICATION_JSON)
  public StreamingOutput getParameters(
    @JsonProperty("template_bindings") final TemplateBindings tbindings
  ) {
    if (this.wp != null) {
      return new StreamingOutput() {
        @Override
        public void write(OutputStream os)
          throws IOException, WebApplicationException {
          PrintWriter out = new PrintWriter(os);
          wp.printSuggestedParametersJSON(tbindings, noexplain, out);
          out.flush();
        }
      };
    }
    return null;
  }

  @POST
  @Path("elaborateTemplateJSON")
  @Produces(MediaType.APPLICATION_JSON)
  public StreamingOutput elaborateTemplateJSON(
    @FormParam("template_id") final String template_id,
    @FormParam("json") final String json,
    @FormParam("constraints_json") final String constraints_json
  ) {
    if (this.wp != null) {
      return new StreamingOutput() {
        @Override
        public void write(OutputStream os)
          throws IOException, WebApplicationException {
          PrintWriter out = new PrintWriter(os);
          wp.printElaboratedTemplateJSON(
            template_id,
            json,
            constraints_json,
            out
          );
          out.flush();
        }
      };
    }
    return null;
  }
}
