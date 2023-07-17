package edu.isi.wings.portal.filters.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

public class GZIPReaderInterceptor implements ReaderInterceptor {

  @Override
  public Object aroundReadFrom(ReaderInterceptorContext context)
    throws IOException, WebApplicationException {
    List<String> ce = context.getHeaders().get("content-encoding");
    if (ce != null && ce.contains("gzip")) {
      final InputStream originalInputStream = context.getInputStream();
      context.setInputStream(new GZIPInputStream(originalInputStream));
    }
    return context.proceed();
  }
}
