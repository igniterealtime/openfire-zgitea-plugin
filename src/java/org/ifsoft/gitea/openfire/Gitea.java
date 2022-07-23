package org.ifsoft.gitea.openfire;

import java.io.File;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.security.Security;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.net.SASLAuthentication;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.UserManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.StringUtils;

import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.servlets.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.webapp.WebAppContext;

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.util.security.*;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.*;

import java.lang.reflect.*;
import java.util.*;

import org.jitsi.util.OSUtils;
import de.mxro.process.*;


public class Gitea implements Plugin, PropertyEventListener, ProcessListener
{
    private static final Logger Log = LoggerFactory.getLogger(Gitea.class);
    private static final String GITEA_VERSION = "1.16.9";
    private XProcess giteaThread = null;
    private String giteaExePath = null;
    private String giteaHomePath = null;
	private String hugoExePath = null;
    private String giteaRoot = null;
    private ExecutorService executor;
    private ServletContextHandler giteaContext;
    private WebAppContext jspService;

    public static Gitea self;

    public void destroyPlugin()
    {
        PropertyEventDispatcher.removeListener(this);

        try {
            cleanupGiteaJDBC();

            if (executor != null)  executor.shutdown();
            if (giteaThread != null) giteaThread.destory();
            if (giteaContext != null) HttpBindManager.getInstance().removeJettyHandler(giteaContext);
            if (jspService != null) HttpBindManager.getInstance().removeJettyHandler(jspService);

        }
        catch (Exception e) {
            Log.error("Gitea destroyPlugin", e);
        }
    }

    public void initializePlugin(final PluginManager manager, final File pluginDirectory)
    {
        PropertyEventDispatcher.addListener(this);
        checkNatives(pluginDirectory);
        executor = Executors.newCachedThreadPool();
        startJSP(pluginDirectory);
        startGoProcesses();
        self = this;
    }

    public String getPort()
    {
        return "3000";
    }

    public String getHugoExePath()
    {
        return hugoExePath;
    }
	
    public String getHome()
    {
        return giteaHomePath;
    }

    public String getUrl()
    {
        return "https://" + XMPPServer.getInstance().getServerInfo().getHostname() + ":" + JiveGlobals.getProperty("httpbind.port.secure", "7443") + "/";
    }

    public String getIpAddress()
    {
        String ourHostname = XMPPServer.getInstance().getServerInfo().getHostname();
        String ourIpAddress = "127.0.0.1";

        try {
            ourIpAddress = InetAddress.getByName(ourHostname).getHostAddress();
        } catch (Exception e) {

        }

        return ourIpAddress;
    }

    public void onOutputLine(final String line)
    {
        Log.info("onOutputLine " + line);
    }

    public void onProcessQuit(int code)
    {
        Log.info("onProcessQuit " + code);
    }

    public void onOutputClosed() {
        Log.error("onOutputClosed");
    }

    public void onErrorLine(final String line)
    {
        Log.debug(line);
    }

    public void onError(final Throwable t)
    {
        Log.error("Thread error", t);
    }

    private void startJSP(File pluginDirectory)
    {
        jspService = new WebAppContext(null, pluginDirectory.getPath() + "/classes/jsp",  "/jsp");
        jspService.setClassLoader(this.getClass().getClassLoader());
        jspService.getMimeTypes().addMimeMapping("wasm", "application/wasm");

        SecurityHandler securityHandler = basicAuth("gitea");
        jspService.setSecurityHandler(securityHandler);

        final List<ContainerInitializer> initializers = new ArrayList<>();
        initializers.add(new ContainerInitializer(new JettyJasperInitializer(), null));
        jspService.setAttribute("org.eclipse.jetty.containerInitializers", initializers);
        jspService.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());

        Log.info("Gitea jsp service enabled");
        HttpBindManager.getInstance().addJettyHandler(jspService);
    }

    private void startGoProcesses()
    {
        boolean giteaEnabled = JiveGlobals.getBooleanProperty("gitea.enabled", true);

        if (giteaExePath != null && giteaEnabled)
        {
            createAdminUser();
            setupGiteaJDBC();

            giteaContext = new ServletContextHandler(null, "/", ServletContextHandler.SESSIONS);
            giteaContext.setClassLoader(this.getClass().getClassLoader());
			
			String parameters = " --config " + giteaRoot + "/app.ini";
            giteaThread = Spawn.startProcess(giteaExePath + parameters, new File(giteaHomePath), this);
			
            ServletHolder proxyServlet = new ServletHolder(ProxyServlet.Transparent.class);
            String giteaUrl = "http://" + JiveGlobals.getProperty("gitea.ipaddr", getIpAddress()) + ":" + JiveGlobals.getProperty("gitea.port", getPort());
            proxyServlet.setInitParameter("proxyTo", giteaUrl);
            proxyServlet.setInitParameter("prefix", "/");
            giteaContext.addServlet(proxyServlet, "/*");

            Log.info("Gitea enabled " + giteaExePath);

            HttpBindManager.getInstance().addJettyHandler(giteaContext);

        } else {
            Log.info("Gitea disabled");
        }
    }

    private void checkNatives(File pluginDirectory)
    {
        try
        {
            giteaRoot = (JiveGlobals.getHomeDirectory() + File.separator + "gitea").replace("\\", "/");

            File giteaRootPath = new File(giteaRoot);

            if (!giteaRootPath.exists()) {
                giteaRootPath.mkdirs();							
            }

            giteaHomePath = (pluginDirectory.getAbsolutePath() + File.separator + "classes").replace("\\", "/");
            String gitea = null;
            String hugo = null;			

            if(OSUtils.IS_LINUX64)
            {
                gitea = "gitea-" + GITEA_VERSION + "-linux-amd64";
                giteaHomePath = giteaHomePath + File.separator + "linux-64";
                giteaExePath = giteaHomePath + File.separator + gitea;
                makeFileExecutable(giteaExePath);
				
                hugoExePath = giteaHomePath + File.separator + "hugo";
                makeFileExecutable(hugoExePath);				
            }
            else if(OSUtils.IS_WINDOWS64)
            {
                gitea = "gitea-" + GITEA_VERSION + "-gogit-windows-4.0-amd64.exe";
                giteaHomePath = giteaHomePath + File.separator + "win-64";
                giteaExePath = giteaHomePath + File.separator + gitea;
                makeFileExecutable(giteaExePath);
				
                hugoExePath = giteaHomePath + File.separator + "hugo.exe";
                makeFileExecutable(hugoExePath);					

            } else {
                Log.error("checkNatives unknown OS " + pluginDirectory.getAbsolutePath());
                return;
            }
        }
        catch (Exception e)
        {
            Log.error("checkNatives error", e);
        }
    }

    private void makeFileExecutable(String path)
    {
        File file = new File(path);
        file.setReadable(true, true);
        file.setWritable(true, true);
        file.setExecutable(true, true);
        Log.info("checkNatives gitea executable path " + path);
    }

    private void setupGiteaJDBC()
    {
        boolean wpAuthProviderEnabled = JiveGlobals.getBooleanProperty("gitea.auth.provider.enabled", true);

        if (wpAuthProviderEnabled)
        {
            Log.info("Setting Gitea as new auth Provider");

            JiveGlobals.setProperty("jdbcAuthProvider.passwordSQL", "SELECT passwd FROM user WHERE lower_name=?");
            JiveGlobals.setProperty("jdbcAuthProvider.setPasswordSQL", "");
            JiveGlobals.setProperty("jdbcAuthProvider.allowUpdate", "false");
            JiveGlobals.setProperty("jdbcAuthProvider.passwordType", "plain");
            JiveGlobals.setProperty("jdbcAuthProvider.useConnectionProvider", "true");

            JiveGlobals.setProperty("provider.auth.className",  "org.jivesoftware.openfire.auth.HybridAuthProvider");
            JiveGlobals.setProperty("hybridAuthProvider.primaryProvider.className",  "org.jivesoftware.openfire.auth.JDBCAuthProvider");
            JiveGlobals.setProperty("hybridAuthProvider.secondaryProvider.className",  "org.jivesoftware.openfire.auth.DefaultAuthProvider");

            try
            {
                Security.addProvider( new GiteaSaslProvider() );
                SASLAuthentication.addSupportedMechanism( GiteaSaslServer.MECHANISM_NAME );
            }
            catch ( Exception ex )
            {
                Log.error( "An exception occurred", ex );
            }
        }

        boolean wpUserProviderEnabled = JiveGlobals.getBooleanProperty("gitea.user.provider.enabled", true);

        if (wpUserProviderEnabled)
        {
            Log.info("Setting Gitea as user Provider");

            JiveGlobals.setProperty("jdbcUserProvider.loadUserSQL", "SELECT lower_name, full_name, email FROM user WHERE lower_name=?");
            JiveGlobals.setProperty("jdbcUserProvider.userCountSQL", "SELECT COUNT(*) FROM user");
            JiveGlobals.setProperty("jdbcUserProvider.allUsersSQL", "SELECT lower_name FROM user");
            JiveGlobals.setProperty("jdbcUserProvider.searchSQL", "SELECT lower_name FROM user WHERE");
            JiveGlobals.setProperty("jdbcUserProvider.user_loginField", "lower_name");
            JiveGlobals.setProperty("jdbcUserProvider.nameField", "full_name");
            JiveGlobals.setProperty("jdbcUserProvider.emailField", "email");
            JiveGlobals.setProperty("jdbcUserProvider.useConnectionProvider", "true");

            JiveGlobals.setProperty("provider.user.className",  "org.jivesoftware.openfire.user.JDBCUserProvider");
        }

        boolean wpGroupProviderEnabled = JiveGlobals.getBooleanProperty("gitea.group.provider.enabled", true);

        if (wpGroupProviderEnabled)
        {
            Log.info("Setting Gitea as group Provider");

            JiveGlobals.setProperty("jdbcGroupProvider.groupCountSQL", "SELECT count(*) FROM team");
            JiveGlobals.setProperty("jdbcGroupProvider.allGroupsSQL", "SELECT lower_name FROM team");
            JiveGlobals.setProperty("jdbcGroupProvider.userGroupsSQL", "SELECT lower_name FROM team INNER JOIN team_user ON team.id = team_user.team_id WHERE team_user.uid IN (SELECT id FROM user WHERE lower_name=?)");
            JiveGlobals.setProperty("jdbcGroupProvider.descriptionSQL", "SELECT description FROM team WHERE lower_name=?");
            JiveGlobals.setProperty("jdbcGroupProvider.loadMembersSQL", "SELECT lower_name FROM user INNER JOIN team_user ON user.id = team_user.uid WHERE team_user.team_id IN (SELECT id FROM team WHERE lower_name=?) AND is_admin<>1");
            JiveGlobals.setProperty("jdbcGroupProvider.loadAdminsSQL",  "SELECT lower_name FROM user INNER JOIN team_user ON user.id = team_user.uid WHERE team_user.team_id IN (SELECT id FROM team WHERE lower_name=?) AND is_admin=1");
            JiveGlobals.setProperty("jdbcGroupProvider.useConnectionProvider", "true");

            JiveGlobals.setProperty("provider.group.className",  "org.jivesoftware.openfire.group.JDBCGroupProvider");
        }

        //JiveGlobals.setProperty("cache.groupMeta.maxLifetime", "60000");
        //JiveGlobals.setProperty("cache.group.maxLifetime", "60000");
        //JiveGlobals.setProperty("cache.userCache.maxLifetime", "60000");

        String iniFileName = giteaRoot + "/app.ini";
        File iniFilePath = new File(iniFileName);

        if (!iniFilePath.exists())
        {
            Log.info("Creating " + iniFileName);

            List<String> lines = new ArrayList<String>();
            lines.add( "[database]");
            lines.add( "DB_TYPE  = mysql");
            lines.add( "HOST     = 127.0.0.1:3306");
            lines.add( "NAME     = " + getDBName());
            lines.add( "USER     = " + getdBUsername());
            lines.add( "PASSWD   = " + getdBPassword());
            lines.add( "SSL_MODE = disable");
            lines.add( "CHARSET  = utf8");

            lines.add( "[server]");
            lines.add( "SSH_DOMAIN       = " + XMPPServer.getInstance().getServerInfo().getHostname());
            lines.add( "DOMAIN           = " + XMPPServer.getInstance().getServerInfo().getXMPPDomain());
            lines.add( "HTTP_PORT        = " + JiveGlobals.getProperty("gitea.port", getPort()));
            lines.add( "ROOT_URL         = " + JiveGlobals.getProperty("gitea.url", getUrl()));
            lines.add( "STATIC_ROOT_PATH = " + giteaHomePath);
            lines.add( "APP_DATA_PATH    = " + giteaRoot);

            lines.add( "[other]");
            lines.add( "SHOW_FOOTER_BRANDING = true");
            lines.add( "SHOW_FOOTER_VERSION = true");
            lines.add( "SHOW_FOOTER_TEMPLATE_LOAD_TIME = true");

            lines.add( "[cors]");
            lines.add( "ENABLED = true");

            try
            {
                Path file = Paths.get(iniFileName);
                Files.write(file, lines, Charset.forName("UTF-8"));
            }
            catch ( Exception e )
            {
                Log.error( "Unable to write file " + iniFileName, e );
            }
        }
    }

    private void cleanupGiteaJDBC()
    {
        Log.info("Cleanup Gitea as new auth Provider");

        JiveGlobals.deleteProperty("jdbcAuthProvider.passwordSQL");
        JiveGlobals.deleteProperty("jdbcAuthProvider.setPasswordSQL");
        JiveGlobals.deleteProperty("jdbcAuthProvider.allowUpdate");
        JiveGlobals.deleteProperty("jdbcAuthProvider.passwordType");
        JiveGlobals.deleteProperty("jdbcAuthProvider.useConnectionProvider");

        JiveGlobals.setProperty("provider.auth.className",  "org.jivesoftware.openfire.auth.DefaultAuthProvider");

        Log.info("Cleanup Gitea as user Provider");

        JiveGlobals.deleteProperty("jdbcUserProvider.loadUserSQL");
        JiveGlobals.deleteProperty("jdbcUserProvider.userCountSQL");
        JiveGlobals.deleteProperty("jdbcUserProvider.allUsersSQL");
        JiveGlobals.deleteProperty("jdbcUserProvider.searchSQL");
        JiveGlobals.deleteProperty("jdbcUserProvider.user_loginField");
        JiveGlobals.deleteProperty("jdbcUserProvider.nameField");
        JiveGlobals.deleteProperty("jdbcUserProvider.emailField");
        JiveGlobals.deleteProperty("jdbcUserProvider.useConnectionProvider");

        JiveGlobals.setProperty("provider.user.className",  "org.jivesoftware.openfire.user.DefaultUserProvider");

        Log.info("Cleanup Gitea as group Provider");

        JiveGlobals.deleteProperty("jdbcGroupProvider.groupCountSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.allGroupsSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.userGroupsSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.descriptionSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.loadMembersSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.loadAdminsSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.useConnectionProvider");

        JiveGlobals.setProperty("provider.group.className",  "org.jivesoftware.openfire.group.DefaultGroupProvider");

        JiveGlobals.deleteProperty("cache.groupMeta.maxLifetime");
        JiveGlobals.deleteProperty("cache.group.maxLifetime");
        JiveGlobals.deleteProperty("cache.userCache.maxLifetime");

        try {
            SASLAuthentication.removeSupportedMechanism( GiteaSaslServer.MECHANISM_NAME );
            Security.removeProvider( GiteaSaslProvider.NAME );
        } catch (Exception e) {}
    }

    private String getdBUsername()
    {
        return JiveGlobals.getXMLProperty("database.defaultProvider.username");
    }

    private String getdBPassword()
    {
        return JiveGlobals.getXMLProperty("database.defaultProvider.password");
    }

    private String getDBName()
    {
        String serverURL = JiveGlobals.getXMLProperty("database.defaultProvider.serverURL");
        String defaultName = "openfire";

        int pos = serverURL.indexOf("3306");

        if (pos > -1) defaultName = serverURL.substring(pos + 5);

        pos = defaultName.indexOf("?");

        if (pos > -1) defaultName = defaultName.substring(0, pos);

        return defaultName;
    }

    private void createAdminUser()
    {
        final UserManager userManager = XMPPServer.getInstance().getUserManager();
        final String administrator = JiveGlobals.getProperty("gitea.username", "administrator");
		
		String domain = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
		String authorizedJIDs = JiveGlobals.getProperty("admin.authorizedJIDs", "");

		if (authorizedJIDs.indexOf(administrator) == -1)
		{
			authorizedJIDs = authorizedJIDs + "," + administrator + "@" + domain;
			JiveGlobals.setProperty("admin.authorizedJIDs", authorizedJIDs);
		}		

        if ( !userManager.isRegisteredUser( administrator ) )
        {
            Log.info( "No administrator user detected. Generating one." );

            String password = JiveGlobals.getProperty("gitea.password", StringUtils.randomString( 40 ));

            if ( password == null || password.isEmpty() )
            {
                password = StringUtils.randomString( 40 );
            }

            try
            {
                userManager.createUser(administrator, password, "Administrator (generated)", null);
                JiveGlobals.setProperty("gitea.password", password );
            }
            catch ( Exception e )
            {
                Log.error( "Unable to provision an administrator user.", e );
            }
        }
    }

    private static final SecurityHandler basicAuth(String realm) {

        GiteaLoginService l = new GiteaLoginService(realm);
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{realm, "webapp-owner", "webapp-contributor"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName(realm);
        csh.addConstraintMapping(cm);
        csh.setLoginService(l);

        return csh;
    }

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


    public void propertySet(String property, Map params)
    {

    }

    public void propertyDeleted(String property, Map<String, Object> params)
    {

    }

    public void xmlPropertySet(String property, Map<String, Object> params) {

    }

    public void xmlPropertyDeleted(String property, Map<String, Object> params) {

    }

}