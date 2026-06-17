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

import static org.junit.jupiter.api.Assertions.*;

class IpMatcherTest {

    @Test
    void valueOf_withNull_returnsNull() {
        assertNull(IpMatcher.valueOf(null));
    }

    @Test
    void valueOf_withBlankString_returnsNull() {
        assertNull(IpMatcher.valueOf("   "));
    }

    @Test
    void valueOf_withEmptyString_returnsNull() {
        assertNull(IpMatcher.valueOf(""));
    }

    @Test
    void valueOf_withValidIpv4_returnsMatcher() {
        IpMatcher matcher = IpMatcher.valueOf("192.168.1.1");
        assertNotNull(matcher);
        assertEquals("192.168.1.1", matcher.getIpRange());
    }

    @Test
    void valueOf_withCidrRange_returnsMatcher() {
        IpMatcher matcher = IpMatcher.valueOf("10.0.0.0/8");
        assertNotNull(matcher);
        assertEquals("10.0.0.0/8", matcher.getIpRange());
    }

    @Test
    void matches_withNull_returnsFalse() {
        IpMatcher matcher = IpMatcher.valueOf("192.168.1.1");
        assertFalse(matcher.matches(null));
    }

    @Test
    void matches_withBlankString_returnsFalse() {
        IpMatcher matcher = IpMatcher.valueOf("192.168.1.1");
        assertFalse(matcher.matches("   "));
    }

    @Test
    void matches_withExactIpMatch_returnsTrue() {
        IpMatcher matcher = IpMatcher.valueOf("192.168.1.42");
        assertTrue(matcher.matches("192.168.1.42"));
    }

    @Test
    void matches_withNonMatchingIp_returnsFalse() {
        IpMatcher matcher = IpMatcher.valueOf("192.168.1.42");
        assertFalse(matcher.matches("10.0.0.1"));
    }

    @Test
    void matches_withIpInsideCidrRange_returnsTrue() {
        IpMatcher matcher = IpMatcher.valueOf("192.168.1.0/24");
        assertTrue(matcher.matches("192.168.1.100"));
    }

    @Test
    void matches_withIpOutsideCidrRange_returnsFalse() {
        IpMatcher matcher = IpMatcher.valueOf("192.168.1.0/24");
        assertFalse(matcher.matches("192.168.2.1"));
    }

    @Test
    void matches_withLoopbackAgainstLoopbackMatcher_returnsTrue() {
        IpMatcher matcher = IpMatcher.valueOf("127.0.0.1");
        assertTrue(matcher.matches("127.0.0.1"));
    }

    @Test
    void equals_withSameIpRange_returnsTrue() {
        IpMatcher a = IpMatcher.valueOf("10.0.0.0/8");
        IpMatcher b = IpMatcher.valueOf("10.0.0.0/8");
        assertEquals(a, b);
    }

    @Test
    void equals_withDifferentIpRange_returnsFalse() {
        IpMatcher a = IpMatcher.valueOf("10.0.0.0/8");
        IpMatcher b = IpMatcher.valueOf("192.168.0.0/16");
        assertNotEquals(a, b);
    }

    @Test
    void equals_withSelf_returnsTrue() {
        IpMatcher matcher = IpMatcher.valueOf("10.0.0.1");
        assertEquals(matcher, matcher);
    }

    @Test
    void equals_withNull_returnsFalse() {
        IpMatcher matcher = IpMatcher.valueOf("10.0.0.1");
        assertNotEquals(null, matcher);
    }

    @Test
    void hashCode_withSameIpRange_returnsEqualHash() {
        IpMatcher a = IpMatcher.valueOf("172.16.0.0/12");
        IpMatcher b = IpMatcher.valueOf("172.16.0.0/12");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_returnsIpRange() {
        IpMatcher matcher = IpMatcher.valueOf("203.0.113.5");
        assertEquals("203.0.113.5", matcher.toString());
    }
}
