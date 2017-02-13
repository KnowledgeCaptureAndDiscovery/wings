package edu.isi.wings.portal.filters.servlets;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * GZIPResponseWrapper
 * <p/>
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 *
 * @version $Id: GZIPResponseWrapper.java,v 1.2 2004/08/27 01:13:55 whitmore Exp $
 * @since blojsom 2.10
 */
public class GZIPResponseWrapper extends HttpServletResponseWrapper {

  protected HttpServletResponse origResponse = null;
  protected ServletOutputStream stream = null;
  protected PrintWriter writer = null;

  /**
   * Create a new GZIPResponseWrapper
   *
   * @param response Original HTTP servlet response
   */
  public GZIPResponseWrapper(HttpServletResponse response) {
    super(response);
    origResponse = response;
  }

  /**
   * Create a new ServletOutputStream which returns a GZIPResponseStream
   *
   * @return GZIPResponseStream object
   * @throws IOException If there is an error creating the response stream
   */
  public ServletOutputStream createOutputStream() throws IOException {
    return (new GZIPResponseStream(origResponse));
  }

  /**
   * Finish the response
   */
  public void finishResponse() throws IOException {
    if (writer != null) {
      writer.close();
    } else {
      if (stream != null) {
        stream.close();
      }
    }

  }

  /**
   * Flush the output buffer
   *
   * @throws IOException If there is an error flushing the buffer
   */
  public void flushBuffer() throws IOException {
    stream.flush();
  }

  /**
   * Retrieve the output stream for this response wrapper
   *
   * @return {@link #createOutputStream()}
   * @throws IOException If there is an error retrieving the output stream
   */
  public ServletOutputStream getOutputStream() throws IOException {
    if (writer != null) {
      throw new IllegalStateException("getWriter() has already been called.");
    }

    if (stream == null)
      stream = createOutputStream();

    return (stream);
  }

  /**
   * Retrieve a writer for this response wrapper
   *
   * @return PrintWriter that wraps an OutputStreamWriter (using UTF-8 as encoding)
   * @throws IOException If there is an error retrieving the writer
   */
  public PrintWriter getWriter() throws IOException {
    if (writer != null) {
      return (writer);
    }
    if (stream != null) {
      throw new IllegalStateException("getOutputStream() has already been called.");
    }
    stream = createOutputStream();
    writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
    return (writer);
  }

  /**
   * Set the content length for the response. Currently a no-op.
   *
   * @param length Content length
   */
  public void setContentLength(int length) {
  }
}
