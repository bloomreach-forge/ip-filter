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
        final AuthObject object = new AuthObject(E, E, E, Collections.emptyMap(), true, null,true, true, false);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("127.0.0.1").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn(null).anyTimes();
        replay(request);
        String header = IpFilterUtils.getIp(request, object.getForwardedForHeader());
        assertEquals("127.0.0.1", header);
        // recreate
        request = createMock(HttpServletRequest.class);
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn("localhost").anyTimes();
        replay(request);
        header = IpFilterUtils.getIp(request, object.getForwardedForHeader());
        assertEquals("localhost", header);

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
    public void testPreviewTokenBypassesFilter() {
        final Set<String> E = Collections.emptySet();
        final AuthObject authObject = new AuthObject(E, E, E, Collections.emptyMap(), false, null, true, false, true);
        assertTrue("Preview token should be enabled", authObject.isPreviewTokenEnabled());

        final BaseIpFilter baseIpFilter = new BaseIpFilter() {
            @Override
            protected Status authenticate(final AuthObject authObject, final HttpServletRequest request) {
                return Status.FORBIDDEN;
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

        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn("/site/").anyTimes();
        expect(request.getContextPath()).andReturn("").anyTimes();
        expect(request.getParameter(IpFilterConstants.PREVIEW_TOKEN_PARAM_NAME))
            .andReturn("e071af76-68f4-4d79-9e3c-4bbca02d655f").anyTimes();
        replay(request);

        final boolean ignored = baseIpFilter.isIgnored(request, authObject);
        assertTrue("Request with preview token should be ignored", ignored);
    }

    @Test
    public void testPreviewTokenDisabledDoesNotBypass() {
        final Set<String> E = Collections.emptySet();
        final AuthObject authObject = new AuthObject(E, E, E, Collections.emptyMap(), false, null, true, false, false);
        assertFalse("Preview token should be disabled", authObject.isPreviewTokenEnabled());

        final BaseIpFilter baseIpFilter = new BaseIpFilter() {
            @Override
            protected Status authenticate(final AuthObject authObject, final HttpServletRequest request) {
                return Status.FORBIDDEN;
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

        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn("/site/").anyTimes();
        expect(request.getContextPath()).andReturn("").anyTimes();
        expect(request.getParameter(IpFilterConstants.PREVIEW_TOKEN_PARAM_NAME))
            .andReturn("e071af76-68f4-4d79-9e3c-4bbca02d655f").anyTimes();
        replay(request);

        final boolean ignored = baseIpFilter.isIgnored(request, authObject);
        assertFalse("Request with preview token should not be ignored when preview token is disabled", ignored);
    }

    @Test
    public void testMissingPreviewTokenDoesNotBypass() {
        final Set<String> E = Collections.emptySet();
        final AuthObject authObject = new AuthObject(E, E, E, Collections.emptyMap(), false, null, true, false, true);
        assertTrue("Preview token should be enabled", authObject.isPreviewTokenEnabled());

        final BaseIpFilter baseIpFilter = new BaseIpFilter() {
            @Override
            protected Status authenticate(final AuthObject authObject, final HttpServletRequest request) {
                return Status.FORBIDDEN;
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

        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn("/site/").anyTimes();
        expect(request.getContextPath()).andReturn("").anyTimes();
        expect(request.getParameter(IpFilterConstants.PREVIEW_TOKEN_PARAM_NAME))
            .andReturn(null).anyTimes();
        replay(request);

        final boolean ignored = baseIpFilter.isIgnored(request, authObject);
        assertFalse("Request without preview token should not be ignored", ignored);
    }

    @Test
    public void testEmptyPreviewTokenDoesNotBypass() {
        final Set<String> E = Collections.emptySet();
        final AuthObject authObject = new AuthObject(E, E, E, Collections.emptyMap(), false, null, true, false, true);
        assertTrue("Preview token should be enabled", authObject.isPreviewTokenEnabled());

        final BaseIpFilter baseIpFilter = new BaseIpFilter() {
            @Override
            protected Status authenticate(final AuthObject authObject, final HttpServletRequest request) {
                return Status.FORBIDDEN;
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

        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn("/site/").anyTimes();
        expect(request.getContextPath()).andReturn("").anyTimes();
        expect(request.getParameter(IpFilterConstants.PREVIEW_TOKEN_PARAM_NAME))
            .andReturn("").anyTimes();
        replay(request);

        final boolean ignored = baseIpFilter.isIgnored(request, authObject);
        assertFalse("Request with empty preview token should not be ignored", ignored);
    }

}
