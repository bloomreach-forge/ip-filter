/*
 * Copyright 2025 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.ipfilter.hst;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class UserCredentialsTest {

    private static String basicAuthHeader(String username, String password) {
        String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        return "Basic " + encoded;
    }

    @Test
    void constructor_withNullHeader_isNotValid() {
        assertFalse(new UserCredentials(null).valid());
    }

    @Test
    void constructor_withEmptyHeader_isNotValid() {
        assertFalse(new UserCredentials("").valid());
    }

    @Test
    void constructor_withTooShortHeader_isNotValid() {
        // "Basic " is 6 chars; anything shorter than that is rejected
        assertFalse(new UserCredentials("Bas").valid());
    }

    @Test
    void constructor_withValidBase64Credentials_isValid() {
        assertTrue(new UserCredentials(basicAuthHeader("admin", "secret")).valid());
    }

    @Test
    void constructor_withValidCredentials_parsesUsername() {
        UserCredentials creds = new UserCredentials(basicAuthHeader("johndoe", "p@ssw0rd"));
        assertEquals("johndoe", creds.getUsername());
    }

    @Test
    void constructor_withValidCredentials_parsesPassword() {
        UserCredentials creds = new UserCredentials(basicAuthHeader("johndoe", "p@ssw0rd"));
        assertEquals("p@ssw0rd", creds.getPassword());
    }

    @Test
    void constructor_withPasswordContainingColon_parsesCorrectly() {
        // Password "pass:word" — only the first colon is the separator
        UserCredentials creds = new UserCredentials(basicAuthHeader("user", "pass:word"));
        assertEquals("user", creds.getUsername());
        assertEquals("pass:word", creds.getPassword());
    }

    @Test
    void constructor_withBase64MissingColon_isNotValid() {
        // Base64 of "nocolon" — no colon at index >= 1
        String noColon = Base64.getEncoder().encodeToString("nocolon".getBytes());
        assertFalse(new UserCredentials("Basic " + noColon).valid());
    }

    @Test
    void constructor_withColonAtFirstPosition_isNotValid() {
        // ":password" — username is empty (indexOf(':') == 0 → < 1)
        String colonFirst = Base64.getEncoder().encodeToString(":password".getBytes());
        assertFalse(new UserCredentials("Basic " + colonFirst).valid());
    }

    @Test
    void toString_returnsUsername() {
        UserCredentials creds = new UserCredentials(basicAuthHeader("myuser", "mypass"));
        assertEquals("myuser", creds.toString());
    }

    @Test
    void toString_withInvalidCredentials_returnsNull() {
        assertNull(new UserCredentials(null).toString());
    }
}
