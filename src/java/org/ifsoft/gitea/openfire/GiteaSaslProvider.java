package org.ifsoft.gitea.openfire;

import java.security.Provider;

/**
 * A Provider implementation for an Gitea-specific SASL mechanisms.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class GiteaSaslProvider extends Provider
{
    /**
     * The provider name.
     */
    public static final String NAME = "GiteaSasl";

    /**
     * The provider version number.
     */
    public static final double VERSION = 1.0;

    /**
     * A description of the provider and its services.
     */
    public static final String INFO = "Gitea-specific SASL mechansims.";

    public GiteaSaslProvider()
    {
        super( NAME, VERSION, INFO );

        put( "SaslServerFactory." + GiteaSaslServer.MECHANISM_NAME, GiteaSaslServerFactory.class.getCanonicalName() );
    }
}