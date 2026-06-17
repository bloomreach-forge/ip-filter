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

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

class BaseIpFilterTest {

    private static final String DISABLED_PROP = "test.ipfilter.disabled";

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @Mock private FilterConfig filterConfig;
    @Mock private ServletContext servletContext;
    @Mock private IpFilterConfigLoader configLoader;

    private TestableIpFilter filter;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        filter = new TestableIpFilter();
        lenient().when(filterConfig.getServletContext()).thenReturn(servletContext);
        lenient().when(filterConfig.getInitParameter(anyString())).thenReturn(null);
        lenient().when(servletContext.getInitParameter(anyString())).thenReturn(null);
        filter.init(filterConfig);
    }

    @Test
    void doFilter_whenDisabledBySystemProperty_passesThrough() throws Exception {
        System.setProperty(DISABLED_PROP, "true");
        try {
            filter.doFilter(request, response, chain);
            verify(chain).doFilter(request, response);
            verifyNoInteractions(response);
        } finally {
            System.clearProperty(DISABLED_PROP);
        }
    }

    @Test
    void doFilter_whenNotInitialized_returnsForbidden() throws Exception {
        lenient().when(request.getRemoteHost()).thenReturn("127.0.0.1");
        filter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyNoInteractions(chain);
    }

    @Test
    void doFilter_whenInitializedAndNoAuthObject_allowsThrough() throws Exception {
        initializeFilter(Collections.emptyMap());
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn(null);
        when(request.getRemoteHost()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_whenNeedsReloading_invalidatesCachesAndContinues() throws Exception {
        when(configLoader.needReloading()).thenReturn(true, false);
        when(configLoader.load()).thenReturn(Collections.emptyMap());
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn(null);
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        filter.setConfigLoader(configLoader);
        filter.setInitialized(true);

        filter.doFilter(request, response, chain);

        verify(configLoader).load();
    }

    @Test
    void doFilter_whenPathIsIgnored_allowsThrough() throws Exception {
        AuthObject auth = authObject(Set.of(".*/health.*"), Set.of("127\\.0\\.0\\.1"), Set.of("10.0.0.0/8"), Collections.emptyMap());
        initializeFilter(Map.of("cfg", auth));
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/site/health/check");
        when(request.getContextPath()).thenReturn("/site");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_whenIpMatchesWhitelist_allowsThrough() throws Exception {
        AuthObject auth = authObject(Collections.emptySet(), Set.of("127\\.0\\.0\\.1"), Set.of("10.0.0.0/8"), Collections.emptyMap());
        initializeFilter(Map.of("cfg", auth));
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/page");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).thenReturn("10.1.2.3");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_whenIpNotInWhitelist_returnsForbidden() throws Exception {
        AuthObject auth = authObject(Collections.emptySet(), Set.of("127\\.0\\.0\\.1"), Set.of("192.168.1.0/24"), Collections.emptyMap());
        initializeFilter(Map.of("cfg", auth));
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/page");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).thenReturn("10.0.0.99");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyNoMoreInteractions(chain);
    }

    @Test
    void doFilter_whenIgnoredHeaderMatches_allowsThrough() throws Exception {
        AuthObject auth = authObject(Collections.emptySet(), Set.of("127\\.0\\.0\\.1"), Set.of("192.168.1.0/24"),
                Map.of("X-Internal", Set.of("secret-value")));
        initializeFilter(Map.of("cfg", auth));
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/page");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).thenReturn("10.0.0.99");
        when(request.getHeader("X-Internal")).thenReturn("secret-value");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_whenMustMatchAllAndNoIpMatch_returnsForbidden() throws Exception {
        // mustMatchAll=true, allowCmsUsers=true, but IP doesn't match → FORBIDDEN
        // (per BaseIpFilter: !matched && mustMatchAll && ipMatchers.size()>0 → FORBIDDEN)
        AuthObject auth = new AuthObject(
                Collections.emptySet(), Set.of("127\\.0\\.0\\.1"),
                Set.of("192.168.1.0/24"), Collections.emptyMap(),
                true, null, true, true);
        initializeFilter(Map.of("cfg", auth));
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/page");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).thenReturn("10.0.0.99");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    void doFilter_whenAllowCmsUsersAndAuthenticateReturnsOk_allowsThrough() throws Exception {
        // no IP match, allowCmsUsers=true → calls authenticate → returns OK
        AuthObject auth = new AuthObject(
                Collections.emptySet(), Set.of("127\\.0\\.0\\.1"),
                Collections.emptySet(), Collections.emptyMap(),
                true, null, true, false);
        initializeFilter(Map.of("cfg", auth));
        filter.setAuthenticateStatus(Status.OK);
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/page");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).thenReturn("10.0.0.99");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_whenAllowCmsUsersAndAuthenticateReturnsUnauthorized_returnsUnauthorized() throws Exception {
        AuthObject auth = new AuthObject(
                Collections.emptySet(), Set.of("127\\.0\\.0\\.1"),
                Collections.emptySet(), Collections.emptyMap(),
                true, null, true, false);
        initializeFilter(Map.of("cfg", auth));
        filter.setAuthenticateStatus(Status.UNAUTHORIZED);
        when(configLoader.getForwardedForHostHeaders()).thenReturn(Set.of(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/page");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).thenReturn("10.0.0.99");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
    }

    @Test
    void destroy_doesNotThrow() throws Exception {
        filter.setConfigLoader(configLoader);
        filter.setInitialized(true);
        filter.destroy();
    }

    // --- helpers ---

    private void initializeFilter(Map<String, AuthObject> data) {
        filter.setConfigLoader(configLoader);
        filter.setInitialized(true);
        lenient().when(configLoader.needReloading()).thenReturn(false);
        lenient().when(configLoader.load()).thenReturn(data);
    }

    private static AuthObject authObject(Set<String> paths, Set<String> hosts, Set<String> ranges,
                                          java.util.Map<String, Set<String>> headers) {
        return new AuthObject(paths, hosts, ranges, headers, false, null, true, false);
    }

    static class TestableIpFilter extends BaseIpFilter {
        private Status authenticateStatus = Status.OK;

        void setConfigLoader(IpFilterConfigLoader loader) { this.configLoader = loader; }
        void setInitialized(boolean value) { this.initialized = value; }
        void setAuthenticateStatus(Status status) { this.authenticateStatus = status; }

        @Override
        protected Status authenticate(AuthObject authObject, HttpServletRequest request) {
            return authenticateStatus;
        }
        @Override
        protected void initializeConfigManager() { /* stays uninit unless setInitialized called */ }
        @Override
        protected String getDisabledPropertyName() { return DISABLED_PROP; }
    }
}
