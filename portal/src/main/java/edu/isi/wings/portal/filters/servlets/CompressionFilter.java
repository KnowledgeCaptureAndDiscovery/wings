package edu.isi.wings.portal.filters.servlets;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CompressionFilter implements Filter {

  @Override
  public void destroy() {}

  @Override
  public void doFilter(
    ServletRequest req,
    ServletResponse res,
    FilterChain chain
  ) throws IOException, ServletException {
    try {
      if (req instanceof HttpServletRequest) {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        boolean compress = false;
        String ae = request.getHeader("accept-encoding");
        if (ae != null && ae.indexOf("gzip") >= 0) {
          compress = true;
        }
        if (compress) {
          String ctype = request.getHeader("content-type");
          if (
            (ctype != null) &&
            ((ctype.contains("zip") ||
                ctype.contains("compress") ||
                ctype.contains("image")))
          ) compress = false;
        }
        if (compress) {
          GZIPResponseWrapper wrappedResponse = new GZIPResponseWrapper(
            response
          );
          chain.doFilter(request, wrappedResponse);
          wrappedResponse.finishResponse();
          return;
        }
        chain.doFilter(request, res);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
    // TODO Auto-generated method stub

  }
}
