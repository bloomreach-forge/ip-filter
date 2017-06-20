/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.ipfilter.hst;

import org.onehippo.forge.ipfilter.common.IpFilterConstants;
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


    @Override
    public String toString() {
        return username;
    }
}

