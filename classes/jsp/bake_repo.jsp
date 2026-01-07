<%@ page import="org.ifsoft.gitea.openfire.Gitea, org.jivesoftware.util.JiveGlobals, org.jivesoftware.openfire.*, java.io.*, org.slf4j.*, org.eclipse.jgit.api.*, org.jbake.app.Oven" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.nio.file.Files" %>
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
            Path repoFolder = JiveGlobals.getHomePath().resolve("gitea").resolve("repos" + repo);

            if (!Files.exists(repoFolder))
            {
                git = Git.cloneRepository().setURI(giteaUrl + repo + ".git").setDirectory(repoFolder.toFile()).call();
                status = "cloned ok";            
            }
            else {
                git = Git.open(repoFolder.toFile());
                PullResult result = git.pull().call(); 
                status = "pulled ok";
            }

            File destination = Gitea.self.getHome().resolve("custom").resolve("public").resolve("www" + repo).toFile();
            Oven oven = new Oven(repoFolder.toFile(), destination, true);
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
   

