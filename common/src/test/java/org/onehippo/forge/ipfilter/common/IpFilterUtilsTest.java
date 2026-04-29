/*
 * Copyright 2018-2022 Bloomreach
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

import org.junit.Test;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;



public class IpFilterUtilsTest {


    @Test
    public void testGetIp() {
        final Set<String> E = Collections.emptySet();
        final AuthObject object = new AuthObject(E, E, E, Collections.emptyMap(), true, null, Collections.emptySet(), true, true);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("127.0.0.1").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn(null).anyTimes();
        replay(request);
        String header = IpFilterUtils.getIp(request, object.getForwardedForHeader(), object.getTrustedProxyMatchers());
        assertEquals("127.0.0.1", header);
        // recreate: XFF present but no trusted proxies configured -> remoteAddr is returned (spoofing prevention)
        request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("127.0.0.1").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn("localhost").anyTimes();
        replay(request);
        header = IpFilterUtils.getIp(request, object.getForwardedForHeader(), object.getTrustedProxyMatchers());
        assertEquals("127.0.0.1", header);
    }

    @Test
    public void testGetIpXffIgnoredWithNoTrustedProxies() {
        // XFF present, no trusted proxies configured -> remoteAddr returned to prevent spoofing
        final HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("192.0.2.100").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn("203.0.113.50").anyTimes();
        replay(request);

        final String ip = IpFilterUtils.getIp(request, IpFilterConstants.HEADER_X_FORWARDED_FOR, Collections.emptySet());
        assertEquals("192.0.2.100", ip);
    }

    @Test
    public void testGetIpXffTrustedWhenRemoteAddrIsTrustedProxy() {
        // XFF present, remoteAddr is a configured trusted proxy -> XFF IP returned
        final IpMatcher proxyMatcher = IpMatcher.valueOf("10.0.0.1");
        final Set<IpMatcher> trustedProxies = Collections.singleton(proxyMatcher);

        final HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("10.0.0.1").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn("203.0.113.50").anyTimes();
        replay(request);

        final String ip = IpFilterUtils.getIp(request, IpFilterConstants.HEADER_X_FORWARDED_FOR, trustedProxies);
        assertEquals("203.0.113.50", ip);
    }

    @Test
    public void testGetIpXffIgnoredWhenRemoteAddrIsNotTrustedProxy() {
        // XFF present, remoteAddr is NOT a trusted proxy -> remoteAddr returned (prevent spoofing)
        final IpMatcher proxyMatcher = IpMatcher.valueOf("10.0.0.1");
        final Set<IpMatcher> trustedProxies = Collections.singleton(proxyMatcher);

        final HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("192.0.2.100").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn("203.0.113.50").anyTimes();
        replay(request);

        final String ip = IpFilterUtils.getIp(request, IpFilterConstants.HEADER_X_FORWARDED_FOR, trustedProxies);
        assertEquals("192.0.2.100", ip);
    }

    @Test
    public void testGetIpXffTrustedWhenRemoteAddrMatchesTrustedProxyCidrRange() {
        // XFF present, remoteAddr falls within a trusted proxy CIDR range -> XFF IP returned
        final IpMatcher cidrMatcher = IpMatcher.valueOf("10.0.0.0/24");
        final Set<IpMatcher> trustedProxies = Collections.singleton(cidrMatcher);

        final HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("10.0.0.5").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn("203.0.113.50").anyTimes();
        replay(request);

        final String ip = IpFilterUtils.getIp(request, IpFilterConstants.HEADER_X_FORWARDED_FOR, trustedProxies);
        assertEquals("203.0.113.50", ip);
    }

    @Test
    public void testGetIpXffIgnoredWhenRemoteAddrOutsideTrustedProxyCidrRange() {
        // XFF present, remoteAddr outside the trusted proxy CIDR range -> remoteAddr returned
        final IpMatcher cidrMatcher = IpMatcher.valueOf("10.0.0.0/24");
        final Set<IpMatcher> trustedProxies = Collections.singleton(cidrMatcher);

        final HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("10.0.1.1").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn("203.0.113.50").anyTimes();
        replay(request);

        final String ip = IpFilterUtils.getIp(request, IpFilterConstants.HEADER_X_FORWARDED_FOR, trustedProxies);
        assertEquals("10.0.1.1", ip);
    }

    @Test
    public void testGetHost() {
        final Set<String> E = Collections.emptySet();
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteHost()).andReturn("127.0.0.1").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).andReturn(null).anyTimes();
        replay(request);
        final BaseIpFilter baseIpFilter = new BaseIpFilter() {
            @Override
            protected Status authenticate(final AuthObject authObject, final HttpServletRequest request) {
                return null;
            }

            @Override
            protected void initializeConfigManager() {

            }

            @Override
            protected String getDisabledPropertyName() {
                return null;
            }
        };
        baseIpFilter.configLoader = new TestConfigLoader();
        String host = baseIpFilter.getHost(request);
        assertEquals("127.0.0.1", host);
        // recreate
        request = createMock(HttpServletRequest.class);
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).andReturn("localhost").anyTimes();
        replay(request);
        host = baseIpFilter.getHost(request);
        assertEquals("localhost", host);

    }

    @Test
    public void testGetHostWithTrailingPeriod() {
        final BaseIpFilter baseIpFilter = new BaseIpFilter() {
            @Override
            protected Status authenticate(final AuthObject authObject, final HttpServletRequest request) {
                return null;
            }

            @Override
            protected void initializeConfigManager() {

            }

            @Override
            protected String getDisabledPropertyName() {
                return null;
            }
        };
        baseIpFilter.configLoader = new TestConfigLoader();

        // Test with single trailing period
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).andReturn("test-www.fcib.bloomreach.cloud.").anyTimes();
        replay(request);
        String host = baseIpFilter.getHost(request);
        assertEquals("Hostname with trailing period should be normalized", "test-www.fcib.bloomreach.cloud", host);

        // Test with multiple trailing periods
        request = createMock(HttpServletRequest.class);
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).andReturn("example.com...").anyTimes();
        replay(request);
        host = baseIpFilter.getHost(request);
        assertEquals("Hostname with multiple trailing periods should be normalized", "example.com", host);

        // Test with no trailing period (normal case)
        request = createMock(HttpServletRequest.class);
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).andReturn("example.com").anyTimes();
        replay(request);
        host = baseIpFilter.getHost(request);
        assertEquals("Normal hostname should remain unchanged", "example.com", host);
    }

    @Test
    public void testNormalizeHostname() {
        // Test normal hostname without trailing period
        assertEquals("example.com", IpFilterUtils.normalizeHostname("example.com"));

        // Test hostname with single trailing period
        assertEquals("example.com", IpFilterUtils.normalizeHostname("example.com."));

        // Test hostname with multiple trailing periods
        assertEquals("example.com", IpFilterUtils.normalizeHostname("example.com..."));

        // Test fully qualified domain name with trailing period
        assertEquals("test-www.fcib.bloomreach.cloud", IpFilterUtils.normalizeHostname("test-www.fcib.bloomreach.cloud."));

        // Test subdomain with trailing period
        assertEquals("sub.domain.example.com", IpFilterUtils.normalizeHostname("sub.domain.example.com."));

        // Test null hostname
        assertNull(IpFilterUtils.normalizeHostname(null));

        // Test empty hostname
        assertTrue(IpFilterUtils.normalizeHostname("").isEmpty());

        // Test hostname that is only periods
        assertEquals("", IpFilterUtils.normalizeHostname("..."));

        // Test IP address with trailing period
        assertEquals("192.168.1.1", IpFilterUtils.normalizeHostname("192.168.1.1."));

        // Test IPv6 with trailing period
        assertEquals("2001:0db8:85a3::8a2e:0370:7334", IpFilterUtils.normalizeHostname("2001:0db8:85a3::8a2e:0370:7334."));
    }


}
