package edu.isi.wings.portal.classes.util;

import javax.servlet.http.HttpServletRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

public class CookieHandler {

  public static Cookie httpCookieFromServletCookie(
    javax.servlet.http.Cookie cookie,
    HttpServletRequest request
  ) {
    if (cookie == null) {
      return null;
    }

    BasicClientCookie apacheCookie = null;

    // get all the relevant parameters
    String name = cookie.getName();
    String value = cookie.getValue();
    String domain = request.getServerName().replaceAll(".*\\.(?=.*\\.)", "");
    String path = request.getContextPath();
    boolean secure = cookie.getSecure();

    // create the apache cookie
    apacheCookie = new BasicClientCookie(name, value);
    apacheCookie.setDomain(domain);
    apacheCookie.setPath(path);
    apacheCookie.setSecure(secure);

    // return the apache cookie
    return apacheCookie;
  }

  public static Cookie[] httpCookiesFromServlet(HttpServletRequest request) {
    javax.servlet.http.Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    Cookie[] apacheCookies = new Cookie[cookies.length];
    for (int i = 0; i < cookies.length; i++) {
      apacheCookies[i] = httpCookieFromServletCookie(cookies[i], request);
    }
    return apacheCookies;
  }
}
