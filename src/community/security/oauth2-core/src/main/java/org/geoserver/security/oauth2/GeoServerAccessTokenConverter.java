/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;

/**
 * Base Access Token Converter with some GeoServer extras
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GeoServerAccessTokenConverter extends DefaultAccessTokenConverter {

    protected UserAuthenticationConverter userTokenConverter;

    /** Initializes the class with a default user auth converter */
    public GeoServerAccessTokenConverter(UserAuthenticationConverter defaultUserAuthConverter) {
        setUserTokenConverter(defaultUserAuthConverter);
    }

    /** Initializes the class with a default user auth converter */
    public GeoServerAccessTokenConverter() {
        setUserTokenConverter(new GeoServerUserAuthenticationConverter());
    }

    /**
     * Converter for the part of the data in the token representing a user.
     *
     * @param userTokenConverter the userTokenConverter to set
     */
    @Override
    public final void setUserTokenConverter(UserAuthenticationConverter userTokenConverter) {
        this.userTokenConverter = userTokenConverter;
        super.setUserTokenConverter(userTokenConverter);
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
        Map<String, String> parameters = new HashMap<>();
        Set<String> scope = parseScopes(map);
        Authentication user = userTokenConverter.extractAuthentication(map);
        String clientId = (String) map.get(CLIENT_ID);
        parameters.put(CLIENT_ID, clientId);
        Set<String> resourceIds = new LinkedHashSet<>(getAud(map));
        OAuth2Request request =
                new OAuth2Request(
                        parameters, clientId, null, true, scope, resourceIds, null, null, null);
        return new OAuth2Authentication(request, user);
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getAud(Map<String, ?> map) {
        if (!map.containsKey(AUD)) {
            return Collections.emptySet();
        }

        Object aud = map.get(AUD);
        if (aud instanceof Collection) return (Collection) aud;
        else return Collections.singletonList(String.valueOf(aud));
    }

    private Set<String> parseScopes(Map<String, ?> map) {
        // Parsing of scopes coming back from GeoNode are slightly different from
        // the default implementation. Instead of it being a collection it is a
        // String where multiple scopes are separated by a space.
        Object scopeAsObject = map.containsKey(SCOPE) ? map.get(SCOPE) : "";
        Set<String> scope = new LinkedHashSet<>();
        if (String.class.isAssignableFrom(scopeAsObject.getClass())) {
            String scopeAsString = (String) scopeAsObject;
            Collections.addAll(scope, scopeAsString.split(" "));
        } else if (Collection.class.isAssignableFrom(scopeAsObject.getClass())) {
            Collection<String> scopes = (Collection<String>) scopeAsObject;
            scope.addAll(scopes);
        }
        return scope;
    }
}
