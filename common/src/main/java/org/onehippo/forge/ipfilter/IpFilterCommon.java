/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.forge.ipfilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.events.PersistedHippoEventsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import static org.onehippo.forge.ipfilter.IpFilterConstants.CACHE_EXPIRES_IN_DAYS;
import static org.onehippo.forge.ipfilter.IpFilterConstants.CACHE_EXPIRE_IN_MINUTES;
import static org.onehippo.forge.ipfilter.IpFilterConstants.CACHE_SITE;
import static org.onehippo.forge.ipfilter.IpFilterConstants.DEFAULT_REPOSITORY_ADDRESS;
import static org.onehippo.forge.ipfilter.IpFilterConstants.INVALID_AUTH_OBJECT;
import static org.onehippo.forge.ipfilter.IpFilterConstants.REALM_PARAM;
import static org.onehippo.forge.ipfilter.IpFilterConstants.REPOSITORY_ADDRESS_PARAM;
import static org.onehippo.forge.ipfilter.IpFilterUtils.getHost;
import static org.onehippo.forge.ipfilter.IpFilterUtils.getIp;
import static org.onehippo.forge.ipfilter.IpFilterUtils.getParameter;
import static org.onehippo.forge.ipfilter.IpFilterUtils.getPath;
import static org.onehippo.forge.ipfilter.IpFilterUtils.handleForbidden;
import static org.onehippo.forge.ipfilter.IpFilterUtils.handleUnauthorized;

public abstract class IpFilterCommon implements Filter {

    private static final Logger log = LoggerFactory.getLogger(IpFilterCommon.class);

    protected IpFilterConfigLoader configLoader;

    private LoadingCache<String, AuthObject> cache;


    protected String repositoryAddress;
    protected boolean initialized;
    protected final LoadingCache<String, Boolean> userCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SITE)
            .expireAfterWrite(CACHE_EXPIRE_IN_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Boolean>() {
                @Override
                public Boolean load(final String key) throws Exception {
                    // we manage cache ourselves
                    return false;
                }
            });
    private final LoadingCache<IpHostPair, Boolean> ipCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SITE)
            .expireAfterWrite(CACHE_EXPIRE_IN_MINUTES, TimeUnit.MINUTES)
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
        realm = getParameter(filterConfig, REALM_PARAM, realm);
        repositoryAddress = getParameter(filterConfig, REPOSITORY_ADDRESS_PARAM, DEFAULT_REPOSITORY_ADDRESS);
        cache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(CACHE_EXPIRES_IN_DAYS, TimeUnit.DAYS)
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

        if (!initialized) {
            requestData();
            log.debug("Filter not initialized yet");
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

    @SuppressWarnings("unchecked")
    private Status allowed(final HttpServletRequest request) {
        final String host = getHost(request);
        final AuthObject authObject = cache.getUnchecked(host);
        // check if host is IP/auth protected:
        if (authObject == null || !authObject.isActive()) {
            log.debug("No configuration match for host: {}", host);
            return Status.OK;
        }
        // check if path is ignored:
        final boolean ignored = isIgnored(request, authObject);
        if (ignored) {
            return Status.OK;
        }
        final String ip = getIp(request, authObject.getForwardedForHeader());
        if (Strings.isNullOrEmpty(ip)) {
            // shouldn't happen I guess..
            log.warn("IP was null or empty");
            return Status.UNAUTHORIZED;
        }
        final IpHostPair pair = new IpHostPair(host, ip);
        boolean matched = ipCache.getUnchecked(pair);
        final Set<IpMatcher> ipMatchers = authObject.getIpMatchers();
        if (!matched) {
            // check if on whitelist
            for (IpMatcher matcher : ipMatchers) {
                if (matcher.matches(ip)) {
                    log.debug("Found match for host: {}, ip: {}", host, ip);
                    matched = true;
                    ipCache.put(pair, Boolean.TRUE);
                    break;
                }
            }
        }
        final boolean mustMatchAll = authObject.isMustMatchAll();
        // if ip already ok, check if basic authentication is needed:
        if (matched && !mustMatchAll) {
            log.debug("Matched based on IP address {}", ip);
            // no need to match username / password
            return Status.OK;
        }
        // if no match is found and we have IP configured, exit
        if (!matched && mustMatchAll && ipMatchers.size() > 0) {
            log.debug("No match for host: {}, ip: {}, no attempt for basic authentication, must match both but ip set was empty", host, ip);
            return Status.FORBIDDEN;
        }
        final boolean allowCmsUsers = authObject.isAllowCmsUsers();
        if (allowCmsUsers) {
            // must match basic authorization
            return authenticate(request);
        }
        if (mustMatchAll) {
            if (log.isInfoEnabled()) {
                log.error("Misconfiguration: match-all property is enabled but allow-cms-users is set to false");
            }
        }
        // no access
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

    /**
     * Check if path is ignored
     */
    private boolean isIgnored(final HttpServletRequest request, final AuthObject authObject) {
        final String path = getPath(request);
        final List<Pattern> ignoredPaths = authObject.getIgnoredPathPatterns();
        for (final Pattern ignoredPath : ignoredPaths) {
            final Matcher matcher = ignoredPath.matcher(path);
            if (matcher.matches()) {
                if (log.isDebugEnabled()) {
                    log.debug("Path is ignored: {}", getPath(request));
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

    protected abstract Status authenticate(final HttpServletRequest request);

    private void handleAuthorizationIssue(final HttpServletRequest req, final HttpServletResponse res, Status status) {
        try {
            switch (status) {
                case FORBIDDEN:
                    log.info("Request forbidden from: {}", req.getRemoteHost());
                    handleForbidden(res, realm);
                    break;
                case UNAUTHORIZED:
                    log.info("Request unauthorized from: {}", req.getRemoteHost());
                    handleUnauthorized(res, realm);
                    break;
                default:
                    log.warn("Unknown status found. Request unauthorized from: {}", req.getRemoteHost());
                    handleUnauthorized(res, realm);
                    break;
            }
        } catch (IOException e) {
            log.error("IOException raised in AuthenticationFilter", e);
        }
    }

    /**
     * Load auth object for matched host name:
     */
    private AuthObject loadIpRules(final String host) {
        final Map<String, AuthObject> rawData = configLoader.load();
        for (Map.Entry<String, AuthObject> entry : rawData.entrySet()) {
            final AuthObject value = entry.getValue();
            final List<Pattern> hostPatterns = value.getHostPatterns();
            for (Pattern pattern : hostPatterns) {
                final Matcher matcher = pattern.matcher(host);
                if (matcher.matches()) {
                    return value;
                }
            }
        }
        // just return inactive object
        return INVALID_AUTH_OBJECT;
    }


    private void requestData() {
        if (!initialized) {
            initializeConfigManger();
        }
        if (initialized && configLoader.needReloading()) {
            configLoader.load();
            invalidateCaches();
            log.info("Ip filter data reloaded");
        }
    }


    protected void initializeConfigManger() {
        // check CMS service first:
        final IpFilterService service = HippoServiceRegistry.getService(IpFilterService.class);
        if (service != null) {
            final Session session = service.getSession();
            if (session == null) {
                log.warn("IpFilterService has no session");
                return;
            }
            configLoader = new CmsConfigLoader(session, service);
            log.info("Successfully configured CMS service");
            initialized = true;
        }


    }

}
