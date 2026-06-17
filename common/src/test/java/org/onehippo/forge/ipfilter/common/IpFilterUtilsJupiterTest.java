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

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class IpFilterUtilsJupiterTest {

    @Test
    void getParameter_returnsInitParam_whenPresent() {
        FilterConfig config = mock(FilterConfig.class);
        when(config.getInitParameter("realm")).thenReturn("myRealm");
        assertEquals("myRealm", IpFilterUtils.getParameter(config, "realm", "default"));
    }

    @Test
    void getParameter_fallsBackToServletContextParam_whenInitParamNull() {
        FilterConfig config = mock(FilterConfig.class);
        ServletContext ctx = mock(ServletContext.class);
        when(config.getInitParameter("realm")).thenReturn(null);
        when(config.getServletContext()).thenReturn(ctx);
        when(ctx.getInitParameter("realm")).thenReturn("ctxRealm");
        assertEquals("ctxRealm", IpFilterUtils.getParameter(config, "realm", "default"));
    }

    @Test
    void getParameter_returnsDefaultValue_whenBothNull() {
        FilterConfig config = mock(FilterConfig.class);
        ServletContext ctx = mock(ServletContext.class);
        when(config.getInitParameter("realm")).thenReturn(null);
        when(config.getServletContext()).thenReturn(ctx);
        when(ctx.getInitParameter("realm")).thenReturn(null);
        assertEquals("default", IpFilterUtils.getParameter(config, "realm", "default"));
    }

    @Test
    void getPath_stripsContextPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/cms/content/documents");
        when(request.getContextPath()).thenReturn("/cms");
        assertEquals("/content/documents", IpFilterUtils.getPath(request));
    }

    @Test
    void getPath_withEmptyContextPath_returnsFullUri() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/site/page");
        when(request.getContextPath()).thenReturn("");
        assertEquals("/site/page", IpFilterUtils.getPath(request));
    }

    @Test
    void handleForbidden_setsForbiddenStatusAndWwwAuthenticateHeader() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        IpFilterUtils.handleForbidden(response, "TestRealm");
        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"TestRealm\"");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "You don't have permissions to view this page.");
        verify(response).flushBuffer();
    }

    @Test
    void handleUnauthorized_setsUnauthorizedStatusAndWwwAuthenticateHeader() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        IpFilterUtils.handleUnauthorized(response, "SecureArea");
        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"SecureArea\"");
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized.");
        verify(response).flushBuffer();
    }
}
