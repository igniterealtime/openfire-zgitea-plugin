<%@ page import="javax.xml.bind.DatatypeConverter, org.jivesoftware.openfire.*, org.jivesoftware.openfire.muc.*, org.xmpp.packet.*" %>
<%
    String username = "";
    String password = "";
    String auth = request.getHeader("authorization");
    
    if (auth != null)
    {
        String token = auth.substring(6);
        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(token);   
        String[] usernameAndPassword = new String(decodedBytes).split(":", 2);    
        username = usernameAndPassword[0];
        password = usernameAndPassword[1];
    } 
%>  
{"username": "<%= username %>", "password": "<%= password %>"}
   

