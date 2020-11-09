package org.ifsoft.gitea.openfire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.user.*;
import org.jivesoftware.openfire.vcard.VCardManager;
import org.jivesoftware.openfire.vcard.VCardEventDispatcher;
import org.jivesoftware.util.JiveGlobals;

import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Base64;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import net.sf.json.*;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

/**
 * A SaslServer implementation that is specific to Gitea.
 *
 * @original code Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class GiteaSaslServer implements SaslServer
{
    private final static Logger Log = LoggerFactory.getLogger( GiteaSaslServer.class );
    public static final String MECHANISM_NAME = "GITEA";
    private String authorizationID = null;

    public GiteaSaslServer()
    {

    }

    @Override
    public String getMechanismName()
    {
        return MECHANISM_NAME;
    }

    @Override
    public byte[] evaluateResponse( byte[] response ) throws SaslException
    {
        if ( response == null )
        {
            throw new IllegalArgumentException( "Argument 'response' cannot be null." );
        }

        final String token = new String( response, StandardCharsets.UTF_8);
        Log.debug("GITEA authentication token " + token);

        try {
            HttpClient client = new DefaultHttpClient();
            String giteaUrl = "http://" + JiveGlobals.getProperty("gitea.ipaddr", Gitea.self.getIpAddress()) + ":" + JiveGlobals.getProperty("gitea.port", Gitea.self.getPort()) + "/api/v1/user";

            HttpGet get = new HttpGet(giteaUrl);

            get.setHeader("content-type", "application/json");
            get.setHeader("authorization", "Basic " + token);

            HttpResponse resp = client.execute(get);
            BufferedReader rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));

            String lines = "";
            String line;

            while ((line = rd.readLine()) != null)
            {
                lines = lines + line;
            }

            JSONObject json = new JSONObject(lines);
            Log.debug("GITEA authentication json " + json);

            if (json.has("login"))
            {
                authorizationID = json.getString("login");
                handleVCard(json);
            }

            return null;

        } catch (Exception e) {
            Log.error("GITEA authentication failure", e);
            throw new SaslException("GITEA authentication failure - " + e.toString());
        }
    }

    private void handleVCard(JSONObject json)
    {
        Log.debug("handleVCard\n" + json);

        // Openfire vcard PhotoResizer failing. Disabling for now.

        String temp = JiveGlobals.getProperty("avatar.resize.enable-on-load", "true");
        JiveGlobals.setProperty("avatar.resize.enable-on-load", "false");

        try {
            Element vCard = null;

            try {
                vCard = VCardManager.getProvider().loadVCard(authorizationID);
            } catch (Exception e) {}

            boolean newVCard = (vCard == null);

            String[] avatar = {"", ""};
            String[] fullname = {"", ""};

            String full_name = json.getString("full_name");
            if (!"".equals(full_name)) fullname = full_name.split(" ");

            if (json.has("avatar_url"))
            {
                String dataUri = getDataURIForURL(json.getString("avatar_url"));

                if (dataUri != null)
                {
                    avatar = dataUri.substring(5).split(";base64,");
                }
            }

            String str = "<vCard xmlns=\"vcard-temp\"><FN>@displayname@</FN><N><FAMILY>@lastname@</FAMILY><GIVEN>@firstname@</GIVEN></N><URL>@url@</URL><NICKNAME>@nickname@</NICKNAME><EMAIL><USERID>@email@</USERID></EMAIL><PHOTO><TYPE>@photoType@</TYPE><BINVAL>@photoBinVal@</BINVAL></PHOTO></vCard>"
                    .replace("@displayname@", full_name)
                    .replace("@lastname@", fullname[1])
                    .replace("@firstname@", fullname[0])
                    .replace("@email@", json.getString("email"))
                    .replace("@photoType@", avatar[0])
                    .replace("@photoBinVal@", avatar[1])
                    .replace("@url@", json.getString("avatar_url"))
                    .replace("@nickname@", json.getString("username"));

            Log.debug("handleVCard\n" + str);

            SAXReader xmlReader = new SAXReader();
            xmlReader.setEncoding("UTF-8");
            vCard = xmlReader.read(new StringReader(str)).getRootElement();

            if (newVCard) {
                VCardManager.getProvider().createVCard(authorizationID, vCard);
                VCardEventDispatcher.dispatchVCardCreated(authorizationID, vCard);
            } else {
                VCardManager.getProvider().updateVCard(authorizationID, vCard);
                VCardEventDispatcher.dispatchVCardUpdated(authorizationID, vCard);
            }

        } catch (Exception e) {
            Log.error("vcard error", e);
        }

        JiveGlobals.setProperty("avatar.resize.enable-on-load", temp);
    }

    private String getDataURIForURL(String avatarUrl)
    {
        Log.debug("getDataURIForURL " + avatarUrl);

        if (avatarUrl.startsWith("data:")) return avatarUrl;

        URI dataUri = null;

        try {
            URL url = new URL(avatarUrl);

            if (null != url)
            {
                try (InputStream inStreamGuess = url.openStream();
                     InputStream inStreamConvert = url.openStream();
                     ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    String contentType = URLConnection.guessContentTypeFromStream(inStreamGuess);

                    if (null == contentType) contentType = "image/png";

                    byte[] chunk = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = inStreamConvert.read(chunk)) > 0)
                    {
                        os.write(chunk, 0, bytesRead);
                    }
                    os.flush();
                    dataUri = new URI("data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(os.toByteArray()));

                } catch (IOException e) {
                    Log.error("error loading data from url", e);
                } catch (URISyntaxException e) {
                    Log.error("error building uri", e);
                }
            }
        } catch (MalformedURLException e) {
            Log.error("error building url " + avatarUrl, e);
        }
        return dataUri.toString();
    }

    public boolean isComplete()
    {
        return true;
    }

    public String getAuthorizationID()
    {
        if ( !isComplete() )
        {
            throw new IllegalStateException( MECHANISM_NAME + " authentication has not completed." );
        }

        return authorizationID;
    }

    public Object getNegotiatedProperty( String propName )
    {
        if ( !isComplete() )
        {
            throw new IllegalStateException( MECHANISM_NAME + " authentication has not completed." );
        }

        if ( Sasl.QOP.equals( propName ) )
        {
            return "auth";
        }
        return null;
    }

    public void dispose() throws SaslException
    {
        authorizationID = null;
    }

    public byte[] unwrap( byte[] incoming, int offset, int len ) throws SaslException
    {
        if ( !isComplete() )
        {
            throw new IllegalStateException( MECHANISM_NAME + " authentication has not completed." );
        }

        throw new IllegalStateException( MECHANISM_NAME + " supports neither integrity nor privacy." );
    }

    public byte[] wrap( byte[] outgoing, int offset, int len ) throws SaslException
    {
        if ( !isComplete() )
        {
            throw new IllegalStateException( MECHANISM_NAME + " authentication has not completed." );
        }

        throw new IllegalStateException( MECHANISM_NAME + " supports neither integrity nor privacy." );
    }
}
