/*
 * Copyright 2017-2020 Bloomreach
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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

public final class IpFilterUtils {

    private static final Logger log = LoggerFactory.getLogger(IpFilterUtils.class);
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();


    private IpFilterUtils() {
    }

    public static String getParameter(final FilterConfig filterConfig, final String paramName, final String defaultValue) {
        String value = filterConfig.getInitParameter(paramName);
        if (value == null) {
            value = filterConfig.getServletContext().getInitParameter(paramName);
        }
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }


    public static String getIp(HttpServletRequest request, final String name) {
        final String headerName = Strings.isNullOrEmpty(name) ? IpFilterConstants.HEADER_X_FORWARDED_FOR : name;
        final String header = request.getHeader(headerName);
        if (Strings.isNullOrEmpty(header)) {
            log.debug("Header: {} was empty", headerName);
            return request.getRemoteAddr();
        }
        final Iterable<String> ipAddresses = COMMA_SPLITTER.split(header);
        final Iterator<String> iterator = ipAddresses.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return request.getRemoteAddr();
    }



    public static String getPath(final HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /**
     * Handle the case of an authenticated user trying to view a page without the appropriate privileges.
     *
     * @param response the HttpServletResponse
     * @throws IOException Thrown if working with the response goes wrong
     */
    public static void handleForbidden(final HttpServletResponse response, final String realm) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + '"');
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "You don't have permissions to view this page.");
        response.flushBuffer();
    }

    /**
     * Handle the case of an authenticated user.
     *
     * @param response the HttpServletResponse
     * @throws IOException Thrown if working with the response goes wrong
     */
    public static void handleUnauthorized(final HttpServletResponse response, final String realm) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + '"');
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized.");
        response.flushBuffer();
    }
}
