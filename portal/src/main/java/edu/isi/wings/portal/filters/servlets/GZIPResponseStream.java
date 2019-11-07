package edu.isi.wings.portal.filters.servlets;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * GZIPResponseStream
 * <p/>
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 *
 * @version $Id: GZIPResponseStream.java,v 1.2 2004/08/27 01:13:55 whitmore Exp $
 * @since blojsom 2.10
 */
public class GZIPResponseStream extends ServletOutputStream {

  protected ByteArrayOutputStream baos = null;
  protected GZIPOutputStream gzipstream = null;
  protected boolean closed = false;
  protected HttpServletResponse response = null;
  protected ServletOutputStream output = null;

  /**
   * Create a new GZIPResponseStream
   *
   * @param response Original HTTP servlet response
   * @throws IOException If there is an error creating the response stream
   */
  public GZIPResponseStream(HttpServletResponse response) throws IOException {
    super();
    closed = false;
    this.response = response;
    this.output = response.getOutputStream();
    baos = new ByteArrayOutputStream();
    gzipstream = new GZIPOutputStream(baos);
  }

  /**
   * Close this response stream
   *
   * @throws IOException If the stream is already closed or there is an error closing the stream
   */
  public void close() throws IOException {
    if (closed) {
      throw new IOException("This output stream has already been closed.");
    }

    gzipstream.finish();

    byte[] bytes = baos.toByteArray();

    response.addHeader("Content-Length", Integer.toString(bytes.length));
    response.addHeader("Content-Encoding", "gzip");
    try {
      output.write(bytes);
      output.flush();
      output.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    closed = true;
  }

  /**
   * Flush the response stream
   *
   * @throws IOException If the stream is already closed or there is an error flushing the stream
   */
  public void flush() throws IOException {
    if (closed) {
      throw new IOException("Cannot flush a closed output stream.");
    }

    gzipstream.flush();
  }

  /**
   * Write a byte to the stream
   *
   * @param b Byte to write
   * @throws IOException If the stream is closed or there is an error in writing
   */
  public void write(int b) throws IOException {
    if (closed) {
      throw new IOException("Cannot write to a closed output stream.");
    }

    gzipstream.write((byte) b);
  }

  /**
   * Write a byte array to the stream
   *
   * @param b Byte array to write
   * @throws IOException If the stream is closed or there is an error in writing
   */
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  /**
   * Write a byte array to the stream
   *
   * @param b   Byte array to write
   * @param off Offset of starting point in byte array to start writing
   * @param len Length of bytes to write
   * @throws IOException If the stream is closed or there is an error in writing
   */
  public void write(byte[] b, int off, int len) throws IOException {
    if (closed) {
      throw new IOException("Cannot write to a closed output stream.");
    }

    gzipstream.write(b, off, len);
  }

  /**
   * Returns <code>true</code> if the stream is closed, <code>false</code> otherwise
   *
   * @return <code>true</code> if the stream is closed, <code>false</code> otherwise
   */
  public boolean closed() {
    return (this.closed);
  }

  /**
   * Reset the stream. Currently a no-op.
   */
  public void reset() {
  }
}