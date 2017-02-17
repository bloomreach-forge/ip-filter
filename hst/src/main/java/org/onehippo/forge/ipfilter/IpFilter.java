/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.forge.ipfilter;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.events.PersistedHippoEventsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.onehippo.forge.ipfilter.IpFilterConstants.*;
import static org.onehippo.forge.ipfilter.IpFilterUtils.*;

/**
 * Filter allowing only access for IP ranges that are configured
 */
public class IpFilter implements Filter, PersistedHippoEventListener {

    private static final Logger log = LoggerFactory.getLogger(IpFilter.class);

    /**
     * keep time of last data request event
     */
    private long lastTimeSent;
    /**
     * increment each time data request event is sent
     */
    private int eventRequestCount;

    /**
     * Object contains *raw* data: multiple hosts can be matched based on regexp.
     * *Processed* data is stored into cache object
     */
    private Map<String, AuthObject> rawData = new ConcurrentHashMap<>();

    private LoadingCache<String, AuthObject> cache;
    private boolean initialized;
    private String repositoryAddress;

    private final LoadingCache<String, Boolean> userCache = CacheBuilder.newBuilder()
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


        HippoServiceRegistry.registerService(this, PersistedHippoEventsService.class);
        lastTimeSent = System.currentTimeMillis();
        requestData();

    }


    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        requestData();
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
    public String getEventCategory() {
        return HippoEventConstants.CATEGORY_SECURITY;
    }

    @Override
    public String getChannelName() {
        return IP_FILTER_LISTENER_CHANNEL;
    }

    @Override
    public boolean onlyNewEvents() {
        return true;
    }

    @Override
    public void onHippoEvent(final HippoEvent event) {
        final String application = event.application();
        if (!Strings.isNullOrEmpty(application) && application.equals(APPLICATION)) {
            if (!initialized) {
                log.info("Initializing ip filter");
                initialized = true;
            } else {
                log.info("Reconfiguring ip filter");
            }
            log.debug("invalidating cache for: {}", event);
            final String data = (String) event.get(DATA);
            populateIpRanges(data);
            invalidateCaches();
        }
    }


    @Override
    public void destroy() {
        invalidateCaches();
        HippoServiceRegistry.unregisterService(this, PersistedHippoEventsService.class);
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

    private Status authenticate(final HttpServletRequest request) {
        final UserCredentials credentials = new UserCredentials(request.getHeader(HEADER_AUTHORIZATION));
        if (!credentials.valid()) {
            log.debug("Invalid credentials, null or empty");
            return Status.UNAUTHORIZED;
        }
        final Boolean cached = userCache.getUnchecked(credentials.getUsername());
        if (cached != null && cached) {
            return Status.OK;
        }
        Session session = null;
        try {
            // try to authenticate:
            session = getSession(credentials);
            if (session == null) {
                log.debug("No valid session for user: {}", credentials.getUsername());
                return Status.UNAUTHORIZED;
            }
            log.debug("Successfully validated user: {}", credentials.getUsername());
            userCache.put(credentials.getUsername(), Boolean.TRUE);
            return Status.OK;
        } finally {
            closeSession(session);
        }
    }

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

    private HippoRepository getHippoRepository(String address) {
        if (address == null || address.length() == 0) {
            log.error("Repository address (parameter " + REPOSITORY_ADDRESS_PARAM
                    + " not set. Unable to perform authorization. Return unauthorized.");
            return null;
        }
        try {
            return HippoRepositoryFactory.getHippoRepository(address);
        } catch (RepositoryException e) {
            log.error("Error while obtaining repository: ", e);
            return null;
        }
    }

    private Session getSession(final UserCredentials credentials) {
        HippoRepository hippoRepository = getHippoRepository(repositoryAddress);
        if (hippoRepository == null) {
            return null;
        }
        try {
            return hippoRepository.login(credentials.getUsername(), credentials.getPassword().toCharArray());
        } catch (LoginException e) {
            log.debug("Invalid credentials for username '{}'", credentials.getUsername());
            return null;
        } catch (RepositoryException e) {
            log.error("Error during authentication", e);
            return null;
        }
    }

    private void closeSession(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    private void populateIpRanges(final String data) {
        rawData.clear();
        if (Strings.isNullOrEmpty(data)) {
            log.warn("Data was null or empty");
            return;
        }
        final Map<String, AuthObject> newObjects = fromJsonAsMap(data);
        if (newObjects == null) {
            log.warn("Data couldn't be de-serialized, data:{}", data);
            return;
        }
        rawData.putAll(newObjects);
    }

    private void requestData() {
        if (initialized) {
            return;
        }
        if (eventRequestCount > EVENT_REQUEST_LIMIT_COUNT) {
            log.debug("Reached max number of event data requests {}", EVENT_REQUEST_LIMIT_COUNT);
            return;
        }
        /*
         prevent flood of request events:
         ideally, data is received when filter is initialized,
         but we can have a race condition there in case event bus is not ready yet.
         */
        final long current = System.currentTimeMillis();
        final long diff = current - lastTimeSent;
        if (diff < EVENT_REQUEST_DELAY) {
            log.debug("Last event sent sent: {} ms ago, skipping this event", diff);
            return;
        }

        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus == null) {
            log.warn("No event bus service found by class {}", HippoEventBus.class.getName());
        } else {
            // request data to be sent back to us
            lastTimeSent = current;
            eventRequestCount++;
            log.debug("Requesting IP filter data...");
            eventBus.post(new IpRequestEvent());
        }
    }

}
