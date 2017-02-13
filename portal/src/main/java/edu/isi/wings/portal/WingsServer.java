package edu.isi.wings.portal;

import javax.annotation.PreDestroy;
import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import edu.isi.wings.portal.filters.resources.CORSResponseFilter;
import edu.isi.wings.portal.filters.resources.GZIPReaderInterceptor;
import edu.isi.wings.portal.filters.resources.GZIPWriterInterceptor;
import edu.isi.wings.portal.resources.ComponentResource;
import edu.isi.wings.portal.resources.DataResource;
import edu.isi.wings.portal.resources.DomainResource;
import edu.isi.wings.portal.resources.PlannerResource;
import edu.isi.wings.portal.resources.ProvResource;
import edu.isi.wings.portal.resources.RunResource;
import edu.isi.wings.portal.resources.SoftwareResource;
import edu.isi.wings.portal.resources.TemplateResource;
import edu.isi.wings.portal.resources.UploadResource;
import edu.isi.wings.portal.resources.UserResource;

@ApplicationPath("users")
class WingsServer extends ResourceConfig {

  public WingsServer() {
    // Filters & Interceptors
    register(CORSResponseFilter.class);
    register(GZIPWriterInterceptor.class);
    register(GZIPReaderInterceptor.class);

    // Enable multi-part form posts
    register(MultiPartFeature.class);

    // Main Resources
    register(DataResource.class);
    register(ComponentResource.class);
    register(TemplateResource.class);
    register(PlannerResource.class);
    register(RunResource.class);
    register(DomainResource.class);
    register(ProvResource.class);
    register(SoftwareResource.class);
    register(UserResource.class);
    register(UploadResource.class);
  }

  @PreDestroy
  public void onDestroy() {
    // Cleanup tasks
  }

}
