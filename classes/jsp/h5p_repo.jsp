<%@ page import="org.ifsoft.gitea.openfire.Gitea, org.jivesoftware.util.JiveGlobals, org.jivesoftware.openfire.*, java.io.*, org.slf4j.*, org.eclipse.jgit.api.*" %>
<%
    final Logger Log = LoggerFactory.getLogger(getClass());
    String repo = request.getParameter("repo"); 
    String status = "error - unknown";
    
    if (repo != null)
    {
        try
        {
            Git git = null;
            String giteaUrl = "http://" + JiveGlobals.getProperty("gitea.ipaddr", "127.0.0.1") + ":" + JiveGlobals.getProperty("gitea.port", "3000");    
            String repoFolder = Gitea.self.getHome() + "/custom/public/www" + repo;
            File repoFolderPath = new File(repoFolder); 

            if (!repoFolderPath.exists())
            {
                git = Git.cloneRepository().setURI(giteaUrl + repo + ".git").setDirectory(repoFolderPath).call(); 
                status = "cloned ok";            
            }
            else {
                git = Git.open(repoFolderPath);
                PullResult result = git.pull().call(); 
                status = "pulled ok";
            }   

        } catch (Exception e) {
            status = e.toString();
            Log.error("h5p_repo error", e);
        }   
    }
%>  
{"repo": "<%= repo %>", "status": "<%= status %>"}
   

