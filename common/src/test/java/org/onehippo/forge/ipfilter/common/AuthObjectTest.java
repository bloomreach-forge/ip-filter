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
package org.onehippo.forge.ipfilter.common;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthObjectTest {

    @Test
    void invalidSentinel_isNotValid() {
        assertFalse(AuthObject.INVALID.isValid());
    }

    @Test
    void invalidSentinel_hasDefaultForwardedForHeader() {
        assertEquals(IpFilterConstants.HEADER_X_FORWARDED_FOR, AuthObject.INVALID.getForwardedForHeader());
    }

    @Test
    void invalidSentinel_hasCacheEnabled() {
        assertTrue(AuthObject.INVALID.isCacheEnabled());
    }

    @Test
    void invalidSentinel_hasEmptyIpMatchers() {
        assertTrue(AuthObject.INVALID.getIpMatchers().isEmpty());
    }

    @Test
    void constructor_withValidArgs_isValid() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, true, false);
        assertTrue(auth.isValid());
    }

    @Test
    void constructor_withValidHost_parsesHostPattern() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Set.of("example\\.com"),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, true, false);
        assertEquals(1, auth.getHostPatterns().size());
        assertTrue(auth.getHostPatterns().get(0).matcher("example.com").matches());
    }

    @Test
    void constructor_withInvalidHostRegex_skipsInvalidEntry() {
        // "[invalid" is an unterminated character class — should be silently skipped
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Set.of("[invalid"),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, true, false);
        assertTrue(auth.getHostPatterns().isEmpty());
    }

    @Test
    void constructor_withEmptyHost_skipsEmptyEntry() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Set.of(""),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, true, false);
        assertTrue(auth.getHostPatterns().isEmpty());
    }

    @Test
    void constructor_withValidCidrRange_parsesIpMatcher() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                Set.of("10.0.0.0/8"), Collections.emptyMap(),
                false, null, true, false);
        assertEquals(1, auth.getIpMatchers().size());
        IpMatcher matcher = auth.getIpMatchers().iterator().next();
        assertTrue(matcher.matches("10.1.2.3"));
    }

    @Test
    void constructor_withNullRange_skipsNullEntry() {
        Set<String> rangesWithNull = new java.util.HashSet<>();
        rangesWithNull.add(null);
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                rangesWithNull, Collections.emptyMap(),
                false, null, true, false);
        assertTrue(auth.getIpMatchers().isEmpty());
    }

    @Test
    void constructor_withBlankRange_skipsBlankEntry() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                Set.of("   "), Collections.emptyMap(),
                false, null, true, false);
        assertTrue(auth.getIpMatchers().isEmpty());
    }

    @Test
    void constructor_withValidIgnoredPath_parsesPathPattern() {
        AuthObject auth = buildAuthObject(
                Set.of(".*/health.*"), Collections.emptySet(),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, true, false);
        assertEquals(1, auth.getIgnoredPathPatterns().size());
        assertTrue(auth.getIgnoredPathPatterns().get(0).matcher("/health/check").matches());
    }

    @Test
    void constructor_withInvalidPathRegex_skipsInvalidEntry() {
        AuthObject auth = buildAuthObject(
                Set.of("[bad-regex"), Collections.emptySet(),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, true, false);
        assertTrue(auth.getIgnoredPathPatterns().isEmpty());
    }

    @Test
    void constructor_withEmptyPath_skipsEmptyEntry() {
        AuthObject auth = buildAuthObject(
                Set.of(""), Collections.emptySet(),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, true, false);
        assertTrue(auth.getIgnoredPathPatterns().isEmpty());
    }

    @Test
    void constructor_withIgnoredHeaders_storesIgnoredHeaders() {
        Map<String, Set<String>> ignoredHeaders = new HashMap<>();
        ignoredHeaders.put("X-My-Header", Set.of("bypass-value"));
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), ignoredHeaders,
                false, null, true, false);
        assertFalse(auth.getIgnoredHeaders().isEmpty());
        assertTrue(auth.getIgnoredHeaders().get("X-My-Header").contains("bypass-value"));
    }

    @Test
    void constructor_allowCmsUsers_isPreserved() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptyMap(),
                true, null, true, false);
        assertTrue(auth.isAllowCmsUsers());
    }

    @Test
    void constructor_mustMatchAll_isPreserved() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, true, true);
        assertTrue(auth.isMustMatchAll());
    }

    @Test
    void constructor_customForwardHeader_isPreserved() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptyMap(),
                false, "X-Real-IP", true, false);
        assertEquals("X-Real-IP", auth.getForwardedForHeader());
    }

    @Test
    void constructor_cacheDisabled_isPreserved() {
        AuthObject auth = buildAuthObject(
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptyMap(),
                false, null, false, false);
        assertFalse(auth.isCacheEnabled());
    }

    // ---

    private static AuthObject buildAuthObject(Set<String> ignoredPaths, Set<String> hosts,
                                              Set<String> ranges, Map<String, Set<String>> ignoredHeaders,
                                              boolean allowCmsUsers, String forwardHeader,
                                              boolean cacheEnabled, boolean mustMatchAll) {
        return new AuthObject(ignoredPaths, hosts, ranges, ignoredHeaders,
                allowCmsUsers, forwardHeader, cacheEnabled, mustMatchAll);
    }
}
