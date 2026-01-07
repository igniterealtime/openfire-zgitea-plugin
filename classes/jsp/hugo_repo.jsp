<%@ page import="org.ifsoft.gitea.openfire.Gitea, org.jivesoftware.util.JiveGlobals, org.jivesoftware.openfire.*, java.io.*, org.slf4j.*, org.eclipse.jgit.api.*, de.mxro.process.*" %>
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

			String parameters = " -D --destination " + Gitea.self.getHome().resolve("custom").resolve("public").resolve("www"+repo);
			
            Spawn.startProcess(Gitea.self.getHugoExePath().toAbsolutePath() + parameters, repoFolderPath, new ProcessListener()
			{
				public void onOutputLine(final String line) {
					Log.info(line);
				}

				public void onOutputClosed() {
					Log.info("hugo process completed");
				}

				public void onErrorLine(final String line) {
					Log.error(line);
				}

				public void onError(final Throwable t) {
					Log.error("Hugo error", t);
				}
							
				public void onProcessQuit(int code)
				{
					Log.info("Hugo process quit " + code);
				}				
			});
            status = "published ok";        

        } catch (Exception e) {
            status = e.toString();
            Log.error("checkNatives error", e);
        }   
    }
%>  
{"repo": "<%= repo %>", "status": "<%= status %>"}
   

