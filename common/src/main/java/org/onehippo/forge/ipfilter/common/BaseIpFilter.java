/*
 * Copyright 2018-2020 BloomReach Inc. (http://www.bloomreach.com)
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
package org.onehippo.forge.ipfilter.common;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

public abstract class BaseIpFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(BaseIpFilter.class);

    protected IpFilterConfigLoader configLoader;

    private LoadingCache<String, AuthObject> cache;

    protected boolean initialized;

    protected final LoadingCache<String, Boolean> userCache = CacheBuilder.newBuilder()
            .maximumSize(IpFilterConstants.CACHE_SITE)
            .expireAfterWrite(IpFilterConstants.CACHE_EXPIRE_IN_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Boolean>() {
                @Override
                public Boolean load(final String key) throws Exception {
                    // we manage cache ourselves
                    return false;
                }
            });

    private final LoadingCache<IpHostPair, Boolean> ipCache = CacheBuilder.newBuilder()
            .maximumSize(IpFilterConstants.CACHE_SITE)
            .expireAfterWrite(IpFilterConstants.CACHE_EXPIRE_IN_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<IpHostPair, Boolean>() {
                @Override
                public Boolean load(final IpHostPair key) throws Exception {
                    // we manage cache ourselves
                    return false;
                }
            });

    private String realm;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        realm = IpFilterUtils.getParameter(filterConfig, IpFilterConstants.REALM_PARAM, realm);

        cache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(IpFilterConstants.CACHE_EXPIRES_IN_DAYS, TimeUnit.DAYS)
                .build(new CacheLoader<String, AuthObject>() {
                    private final ExecutorService executor = Executors.newFixedThreadPool(1);

                    public AuthObject load(String key) {
                        return loadIpRules(key);
                    }

                    @Override
                    public ListenableFuture<AuthObject> reload(final String key, final AuthObject oldValue) throws Exception {
                        final ListenableFutureTask<AuthObject> task = ListenableFutureTask.create(() -> load(key));
                        executor.execute(task);
                        return task;
                    }
                });
        requestData();
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        if ("true".equals(System.getProperty(getDisabledPropertyName()))) {
            log.debug("{} disabled by system property {}", this.getClass().getSimpleName(), getDisabledPropertyName());
            chain.doFilter(request, response);
            return;
        }

        if (!initialized) {
            requestData();
        }

        if (!initialized) {
            log.debug("{}: not initialized yet", this.getClass().getSimpleName());
            handleAuthorizationIssue((HttpServletRequest) request, (HttpServletResponse) response, Status.FORBIDDEN);
            return;
        }

        if (configLoader.needReloading()) {
            invalidateCaches();
        }

        final Status status = allowed((HttpServletRequest) request);
        if (status == Status.OK) {
            chain.doFilter(request, response);
            return;
        }
        handleAuthorizationIssue((HttpServletRequest) request, (HttpServletResponse) response, status);
    }

    private Status allowed(final HttpServletRequest request) {

        if (log.isDebugEnabled()){
            printRequestHeaders(request);
            printRequestCookies(request);
        }

        final String host = getHost(request);
        final AuthObject authObject = cache.getUnchecked(host);

        // check if host is IP/auth protected
        if (authObject == null || !authObject.isValid()) {
            log.debug("{} configuration object match for host: {}", (authObject == null) ? "No" : "Invalid", host);
            return Status.OK;
        }

        // check if path is ignored:
        final boolean ignored = isIgnored(request, authObject);
        if (ignored) {
            return Status.OK;
        }

        final String ip = IpFilterUtils.getIp(request, authObject.getForwardedForHeader());
        if (Strings.isNullOrEmpty(ip)) {
            // shouldn't happen
            log.warn("{}: IP was null or empty. Host is {}", this.getClass().getSimpleName(), host);
            return Status.UNAUTHORIZED;
        }

        // check if on whitelist
        final IpHostPair pair = new IpHostPair(host, ip);
        final boolean cacheEnabled = authObject.isCacheEnabled();
        boolean matched = ipCache.getUnchecked(pair);
        final Set<IpMatcher> ipMatchers = authObject.getIpMatchers();
        if (!matched) {
            for (IpMatcher matcher : ipMatchers) {
                if (matcher.matches(ip)) {
                    log.debug("Found match for host: {}, ip: {}, path: {}", host, ip, IpFilterUtils.getPath(request));
                    matched = true;
                    if (cacheEnabled) {
                        log.debug("Adding to ip cache: {}", ip);
                        ipCache.put(pair, Boolean.TRUE);
                    }else{
                        log.debug("Skipping ip address cache, caching is disabled");
                    }
                    break;
                }
            }
        }

        // if IP already ok, check if basic authentication is needed:
        final boolean mustMatchAll = authObject.isMustMatchAll();
        if (matched && !mustMatchAll) {
            log.debug("Matched based on IP address {}, path {}", ip, IpFilterUtils.getPath(request));
            // no need to match username / password
            return Status.OK;
        }

        // if no match is found and we have IP configured, exit
        if (!matched && mustMatchAll && ipMatchers.size() > 0) {
            log.debug("No match for host: {}, ip: {}. No attempt for basic authentication, must match both but IP set was empty", host, ip);
            return Status.FORBIDDEN;
        }

        final boolean allowCmsUsers = authObject.isAllowCmsUsers();
        if (allowCmsUsers) {
            // must match basic authorization
            return authenticate(authObject, request);
        }

        if (mustMatchAll) {
            log.error("{}: ambiguous configuration: match-all property is enabled but allow-cms-users is set to false. " +
                    "Still authenticating against the repository now.", this.getClass().getSimpleName());
            return authenticate(authObject, request);
        }

        // no access
        if (log.isDebugEnabled()) {
            log.debug("Falling back to forbidden access for host: {}, ip: {}, path: {}", host, ip, IpFilterUtils.getPath(request));
        }
        return Status.FORBIDDEN;
    }

    @Override
    public void destroy() {
        invalidateCaches();
    }

    private void invalidateCaches() {
        log.debug("Invalidating all cache");
        cache.invalidateAll();
        userCache.invalidateAll();
        ipCache.invalidateAll();
    }

    protected  String getHost(final HttpServletRequest request) {

        final Set<String> forwardedForHostHeaders = configLoader.getForwardedForHostHeaders();
        for (String h : forwardedForHostHeaders) {
            log.debug("fetching host for following header: {}", h);
            final String hostHeader = request.getHeader(h);
            if (!Strings.isNullOrEmpty(hostHeader)) {
                log.debug("Will try to match  configuration for host: {}", hostHeader);
                return hostHeader;
            }
        }


        final String remoteHost = request.getRemoteHost();
        log.debug("Missing header {}, using: {}", IpFilterConstants.HEADER_X_FORWARDED_HOST, remoteHost);
        return remoteHost;
    }

    private void printRequestHeaders(final HttpServletRequest request) {
        log.trace("====== REQUEST HEADERS  ======");
        final Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                final String name = headerNames.nextElement();
                final String value = request.getHeader(name);
                log.trace("{} = {}", name, value);
            }
        }
        log.trace("====== REQUEST HEADERS END ======");
    }

    private void printRequestCookies(final HttpServletRequest request) {
        log.trace("====== COOKIES ======");
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                final String c = "Name: " + cookie.getName()
                        + ", Domain: " + cookie.getDomain()
                        + ", Path: " + cookie.getPath()
                        + ", Secure: " + cookie.getSecure()
                        + ", HttpOnly: " + cookie.isHttpOnly()
                        + ", Version: " + cookie.getVersion()
                        + ", MaxAge: " + cookie.getMaxAge();
                log.trace("Cookie: {}", c);
            }
        }

        log.trace("====== COOKIES END ======");
    }


    /**
     * Check if path is ignored
     */
    private boolean isIgnored(final HttpServletRequest request, final AuthObject authObject) {
        final String path = IpFilterUtils.getPath(request);
        final List<Pattern> ignoredPaths = authObject.getIgnoredPathPatterns();
        for (final Pattern ignoredPath : ignoredPaths) {
            final Matcher matcher = ignoredPath.matcher(path);
            if (matcher.matches()) {
                if (log.isDebugEnabled()) {
                    log.debug("Path is ignored because of pattern {}: {}", ignoredPath.pattern(), path);
                }
                return true;
            }
        }

        // check if we have header ignore:
        final Map<String, Set<String>> multimap = authObject.getIgnoredHeaders();
        if (multimap == null || multimap.isEmpty()) {
            return false;
        }
        final Set<Map.Entry<String, Set<String>>> entries = multimap.entrySet();
        for (Map.Entry<String, Set<String>> entry : entries) {
            final String ignoreHeader = entry.getKey();
            final String value = request.getHeader(ignoreHeader);
            if (!Strings.isNullOrEmpty(value)) {
                final Collection<String> headerValues = multimap.get(ignoreHeader);
                if (headerValues == null) {
                    continue;
                }
                final boolean matched = headerValues.contains(value);
                if (matched) {
                    log.debug("Matched header {} for value {}", ignoreHeader, value);
                    return true;
                } else {
                    log.debug("Header value mismatch, header {},  value {}", ignoreHeader, value);
                }
            }
        }
        return false;

    }

    /**
     * Authenticate against the repository for Hippo users.
     */
    protected abstract Status authenticate(final AuthObject authObject, final HttpServletRequest request);

    private void handleAuthorizationIssue(final HttpServletRequest req, final HttpServletResponse res, Status status) {
        try {
            switch (status) {
                case FORBIDDEN:
                    log.info("{}: request forbidden from: {}", this.getClass().getSimpleName(), req.getRemoteHost());
                    IpFilterUtils.handleForbidden(res, realm);
                    break;
                case UNAUTHORIZED:
                    log.info("{}: request unauthorized from: {}", this.getClass().getSimpleName(), req.getRemoteHost());
                    IpFilterUtils.handleUnauthorized(res, realm);
                    break;
                default:
                    log.warn("{}: unknown status found. Request unauthorized from: {}", this.getClass().getSimpleName(), req.getRemoteHost());
                    IpFilterUtils.handleUnauthorized(res, realm);
                    break;
            }
        } catch (IOException e) {
            log.error("IOException raised in " + this.getClass().getSimpleName(), e);
        }
    }

    /**
     * Load auth object for matched host name:
     */
    private AuthObject loadIpRules(final String host) {
        log.debug("Loading rules for host {}", host);
        final Map<String, AuthObject> rawData = configLoader.load();
        for (Map.Entry<String, AuthObject> entry : rawData.entrySet()) {
            final AuthObject value = entry.getValue();
            final List<Pattern> hostPatterns = value.getHostPatterns();
            for (Pattern pattern : hostPatterns) {
                final Matcher matcher = pattern.matcher(host);
                if (matcher.matches()) {
                    log.debug("Loaded: {}", value);
                    return value;
                }
            }
        }

        // just return invalid object
        return AuthObject.INVALID;
    }

    private void requestData() {
        if (!initialized) {
            initializeConfigManager();
        }
        if (initialized && configLoader.needReloading()) {
            configLoader.load();
            invalidateCaches();
            log.info("{}: data reloaded", this.getClass().getSimpleName());
        }
    }

    protected abstract void initializeConfigManager();

    protected abstract String getDisabledPropertyName();
}
