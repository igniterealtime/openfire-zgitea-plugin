package org.ifsoft.gitea.openfire;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A SaslServerFactory implementation that is used to instantiate Gitea-specific SaslServer instances.
 *
 * @original code Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class GiteaSaslServerFactory implements SaslServerFactory
{
    public SaslServer createSaslServer( String mechanism, String protocol, String serverName, Map<String, ?> props, CallbackHandler cbh ) throws SaslException
    {
        // Do not return an instance when the provided properties contain Policy settings that disallow our implementations.
        final Set<String> mechanismNames = getMechanismNamesSet( props );

        if ( mechanismNames.contains( mechanism ) && mechanism.equalsIgnoreCase( GiteaSaslServer.MECHANISM_NAME ) )
        {
            return new GiteaSaslServer();
        }

        return null;
    }

    public String[] getMechanismNames( Map<String, ?> props )
    {
        final Set<String> result = getMechanismNamesSet( props );
        return result.toArray( new String[ result.size() ] );
    }

    /**
     * Identical to #getMechanismNames, but returns a Set rather than an array.
     *
     * @see #getMechanismNames(Map)
     */
    protected final Set<String> getMechanismNamesSet( Map<String, ?> props )
    {
        final Set<String> supportedMechanisms = new HashSet<String>();
        supportedMechanisms.add( GiteaSaslServer.MECHANISM_NAME );

        if ( props != null )
        {
            for ( Map.Entry<String, ?> prop : props.entrySet() )
            {
                if ( !( prop.getValue() instanceof String ) )
                {
                    continue;
                }

                final String name = prop.getKey();
                final String value = (String) prop.getValue();

                if ( Sasl.POLICY_NOPLAINTEXT.equalsIgnoreCase( name ) && "true".equalsIgnoreCase( value ) )
                {
                    supportedMechanisms.remove( GiteaSaslServer.MECHANISM_NAME );
                }

                // TODO Determine if other policies are relevant.
            }
        }
        return supportedMechanisms;
    }
}
