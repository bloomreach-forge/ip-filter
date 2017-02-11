package org.onehippo.forge.ipfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public final class UserCredentials {

    private static final Logger log = LoggerFactory.getLogger(UserCredentials.class);
    private final String username;
    private final String password;

    /**
     * Parse the username and password from the authorization header. If
     * the username and password cannot be found they are set to null.
     */
    public UserCredentials(final String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.length() < IpFilterConstants.BASIC_AUTH_PREFIX_LENGTH) {
            log.debug("Authorization header not found.");
            username = null;
            password = null;
            return;
        }

        String authPart = authorizationHeader.substring(IpFilterConstants.BASIC_AUTH_PREFIX_LENGTH);
        String userpass = new String(Base64.getDecoder().decode(authPart));
        if (userpass.indexOf(':') < 1) {
            log.debug("Invalid authorization header found.");
            username = null;
            password = null;
            return;
        }
        username = userpass.substring(0, userpass.indexOf(':'));
        password = userpass.substring(userpass.indexOf(':') + 1);
    }

    public boolean valid() {
        return password != null && username != null;
    }

    /**
     * Get the username.
     *
     * @return the username or null if the username was not found
     */
    String getUsername() {
        return username;
    }

    /**
     * Get the password.
     *
     * @return the password or null if the password was not found
     */
    String getPassword() {
        return password;
    }

}

