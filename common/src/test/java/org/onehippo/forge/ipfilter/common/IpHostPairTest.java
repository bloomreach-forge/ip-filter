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

class IpHostPairTest {

    @Test
    void getIp_returnsConstructorValue() {
        assertEquals("192.168.1.1", new IpHostPair("192.168.1.1", "example.com").getIp());
    }

    @Test
    void getHost_returnsConstructorValue() {
        assertEquals("example.com", new IpHostPair("192.168.1.1", "example.com").getHost());
    }

    @Test
    void equals_withSameValues_returnsTrue() {
        assertEquals(new IpHostPair("10.0.0.1", "host.local"), new IpHostPair("10.0.0.1", "host.local"));
    }

    @Test
    void equals_withSelf_returnsTrue() {
        IpHostPair pair = new IpHostPair("10.0.0.1", "host.local");
        assertEquals(pair, pair);
    }

    @Test
    void equals_withDifferentIp_returnsFalse() {
        assertNotEquals(new IpHostPair("10.0.0.1", "host.local"), new IpHostPair("10.0.0.2", "host.local"));
    }

    @Test
    void equals_withDifferentHost_returnsFalse() {
        assertNotEquals(new IpHostPair("10.0.0.1", "host.local"), new IpHostPair("10.0.0.1", "other.local"));
    }

    @Test
    void equals_withNull_returnsFalse() {
        assertNotEquals(null, new IpHostPair("10.0.0.1", "host.local"));
    }

    @Test
    void equals_withDifferentClass_returnsFalse() {
        assertNotEquals("string", new IpHostPair("10.0.0.1", "host.local"));
    }

    @Test
    void hashCode_withSameValues_isEqual() {
        assertEquals(
                new IpHostPair("172.16.0.1", "app.internal").hashCode(),
                new IpHostPair("172.16.0.1", "app.internal").hashCode());
    }

    @Test
    void hashCode_withDifferentValues_isDifferent() {
        assertNotEquals(
                new IpHostPair("10.0.0.1", "host.a").hashCode(),
                new IpHostPair("10.0.0.1", "host.b").hashCode());
    }
}
