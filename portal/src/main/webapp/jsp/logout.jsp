<%@ page session="true"%>
<%
String message = "User '"+request.getRemoteUser()+"' has been logged out.";
session.invalidate();
response.sendRedirect(request.getHeader("referer"));
%>