<%@ page import="javax.xml.bind.DatatypeConverter, org.jivesoftware.openfire.*, org.jivesoftware.openfire.muc.*, org.xmpp.packet.*" %>
<%
    String username = "";
    String password = "";
    String auth = request.getHeader("authorization");
    String room = request.getParameter("room");      
    
    if (auth != null && room != null)
    {
        String token = auth.substring(6);
        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(token);   
        String[] usernameAndPassword = new String(decodedBytes).split(":", 2);    
        username = usernameAndPassword[0];
        password = usernameAndPassword[1];       
        
        MultiUserChatService mucService = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService("conference");    

        MUCRoom mucRoom;
        try {
            String domain = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
            mucRoom = mucService.getChatRoom(room, new JID(username + "@" + domain));
            mucRoom.setPersistent(true);
            mucRoom.setPublicRoom(true);
            //mucRoom.setPassword(mucRoom.getPassword());
            mucRoom.unlock(mucRoom.getRole());
        }
        catch (Exception e) {
        }        
    }         
%>  
{"username": "<%= username %>", "room": "<%= room %>"}
   

