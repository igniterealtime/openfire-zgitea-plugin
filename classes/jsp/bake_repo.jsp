<%@ page import="org.jivesoftware.openfire.*" %>
<%
    String repo = request.getParameter("repo");  
%>  
{"repo": "<%= repo %>", "baking": "ok"}
   

