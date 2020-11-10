<%@ page import="org.ifsoft.gitea.openfire.Gitea, org.jivesoftware.util.JiveGlobals, org.jivesoftware.openfire.*, java.io.*, org.slf4j.*, org.eclipse.jgit.api.*, org.jbake.app.Oven" %>
<%
    Logger Log = LoggerFactory.getLogger(getClass());
    String repo = request.getParameter("repo"); 
    String status = "error - unknown";
    
    if (repo != null)
    {
        try
        {
            Git git = null;
            String giteaUrl = "http://" + JiveGlobals.getProperty("gitea.ipaddr", "127.0.0.1") + ":" + JiveGlobals.getProperty("gitea.port", "3000");    
            String repoFolder = JiveGlobals.getHomeDirectory() + "/gitea/repos" + repo;
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

            File destination =  new File(Gitea.self.getHome() + "/custom/public/www" + repo);
            Oven oven = new Oven(repoFolderPath, destination, true);
            oven.setupPaths();
            oven.bake();   
            status = "baked ok";        

        } catch (Exception e) {
            status = e.toString();
            Log.error("checkNatives error", e);
        }   
    }
%>  
{"repo": "<%= repo %>", "status": "<%= status %>"}
   

