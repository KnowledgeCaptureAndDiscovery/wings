<%@ page session="true"%>
<%
String message = "Login failed. Invalid username or password";
%>
<jsp:forward page="login.jsp">
<jsp:param name="message" value="<%=message%>" />
</jsp:forward>