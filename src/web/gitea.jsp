<%@ page import="java.util.*" %>
<%@ page import="org.ifsoft.gitea.openfire.*" %>
<%@ page import="org.jivesoftware.openfire.*" %>
<%@ page import="org.jivesoftware.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%

    boolean update = request.getParameter("update") != null;
    String errorMessage = null;

    // Get handle on the plugin
    Gitea plugin = Gitea.self;

    if (update)
    {    
        String username = request.getParameter("username");     
        JiveGlobals.setProperty("gitea.username", username);     

        String password = request.getParameter("password");     
        JiveGlobals.setProperty("gitea.password", password);   
        
        String port = request.getParameter("port");     
        JiveGlobals.setProperty("gitea.port", port);   
        
        String ipaddr = request.getParameter("ipaddr");     
        JiveGlobals.setProperty("gitea.ipaddr", ipaddr);   
        
        String url = request.getParameter("url");     
        JiveGlobals.setProperty("gitea.url", url);         

        String sync_db = request.getParameter("sync_db");
        JiveGlobals.setProperty("gitea.sync.db", (sync_db != null && sync_db.equals("on")) ? "true": "false");     
        
        String enabled = request.getParameter("enabled");
        JiveGlobals.setProperty("gitea.enabled", (enabled != null && enabled.equals("on")) ? "true": "false");    	
    }

%>
<html>
<head>
   <title><fmt:message key="plugin.title.description" /></title>

   <meta name="pageID" content="gitea-settings"/>
</head>
<body>
<% if (errorMessage != null) { %>
<div class="error">
    <%= errorMessage%>
</div>
<br/>
<% } %>

<div class="jive-table">
<form action="gitea.jsp" method="post">
    <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.settings.description"/></th>
            </tr>
            </thead>
            <tbody>  
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="enabled"<%= (JiveGlobals.getProperty("gitea.enabled", "true").equals("true")) ? " checked" : "" %>>
                    <fmt:message key="config.page.configuration.enabled" />       
                </td>  
            </tr>
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="sync_db"<%= (JiveGlobals.getProperty("gitea.sync.db", "false").equals("true")) ? " checked" : "" %>>
                    <fmt:message key="config.page.configuration.sync_db" />       
                </td>  
            </tr>			
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.username"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="username" required
                       value="<%= JiveGlobals.getProperty("gitea.username", "admin") %>">
                </td>
            </tr>   
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.password"/>
                </td>
                <td><input type="password" size="50" maxlength="100" name="password" required
                       value="<%= JiveGlobals.getProperty("gitea.password", "admin") %>">
                </td>
            </tr>              
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.ipaddr"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="ipaddr" required
                       value="<%= JiveGlobals.getProperty("gitea.ipaddr", plugin.getIpAddress()) %>">
                </td>                               
            </tr>                   
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.port"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="port" required
                       value="<%= JiveGlobals.getProperty("gitea.port", plugin.getPort()) %>">
                </td>                               
            </tr>  
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.url"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="url" required
                       value="<%= JiveGlobals.getProperty("gitea.url", plugin.getUrl()) %>">
                </td>                               
            </tr>            
            </tbody>
        </table>
    </p>
   <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.configuration.save.title"/></th>
            </tr>
            </thead>
            <tbody>         
            <tr>
                <th colspan="2"><input type="submit" name="update" value="<fmt:message key="config.page.configuration.submit" />">&nbsp;&nbsp;<fmt:message key="config.page.configuration.restart.warning"/></th>
            </tr>       
            </tbody>            
        </table> 
    </p>
</form>
</div>
</body>
</html>
