package edu.isi.wings.portal.filters.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

@Provider
public class GZIPWriterInterceptor implements WriterInterceptor {
  @Context
  HttpServletRequest request;
  
  @Override
  public void aroundWriteTo(WriterInterceptorContext context)
      throws IOException, WebApplicationException {

    boolean compress = false;
    String ae = request.getHeader("accept-encoding");
    if (ae != null && ae.indexOf("gzip") >= 0) {
      compress = true;
    }
    if(compress) {
      MultivaluedMap<String, Object> headers = context.getHeaders(); 
      for(Object type : headers.get("content-type")) {
        String ctype = type.toString();
        if(ctype.contains("zip") || 
            ctype.contains("compress") ||
            ctype.contains("image")) {
          compress = false;
          break;
        }
      }
      if(compress) {
        headers.add("Content-Encoding", "gzip");
        final OutputStream outputStream = context.getOutputStream();
        context.setOutputStream(new GZIPOutputStream(outputStream));
      }      
    }
    context.proceed();
  }
}
