/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.forge.ipfilter;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.events.PersistedHippoEventsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Filter allowing only access for IP ranges that are configured  by init-parameter 'allowed-ip-ranges'.
 */
public class IpFilter implements Filter, PersistedHippoEventListener {

    private static final Logger log = LoggerFactory.getLogger(IpFilter.class);
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    private Collection<IpMatcher> staticConfiguration;


    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

        staticConfiguration = new CopyOnWriteArrayList<>();
        final String parameter = filterConfig.getInitParameter(IpFilterEvent.ALLOWED_IP_RANGES);
        if (!Strings.isNullOrEmpty(parameter)) {
            populateIpRanges(parameter);
        }
        HippoServiceRegistry.registerService(this, PersistedHippoEventsService.class);
        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus == null) {
            log.warn("No event bus service found by class {}", HippoEventBus.class.getName());
        } else {
            eventBus.post(new IpRequestEvent());
        }

    }

    private void populateIpRanges(final String parameter) {
        staticConfiguration.clear();
        final Iterable<String> ranges = COMMA_SPLITTER.split(parameter);
        for (String range : ranges) {
            staticConfiguration.add(IpMatcher.valueOf(range));
        }
    }

    private Collection<IpMatcher> loadIpRules() {

        return staticConfiguration;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (allowed((HttpServletRequest) request)) {
            chain.doFilter(request, response);
            return;
        }
        final HttpServletResponse resp = (HttpServletResponse) response;
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);

    }

    private boolean allowed(final HttpServletRequest request) {
        final String ip = getIp(request);
        if (Strings.isNullOrEmpty(ip)) {
            return false;
        }
        log.debug("IP found for '{}': {}", request.getPathInfo(), ip);
        final Iterator<IpMatcher> i = staticConfiguration.iterator();
        try {
            while (i.hasNext()) {
                final IpMatcher ipMatcher = i.next();
                boolean match = ipMatcher.matches(ip);
                if (match) {
                    log.debug("IP {} matching rule {}", ip, ipMatcher);
                    return true;
                } else {
                    log.trace("IP {} NOT matching rule {}", ip, ipMatcher);
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid IP address: {}", ip);
        }

        return false;
    }

    @Override
    public void destroy() {
        HippoServiceRegistry.unregisterService(this, PersistedHippoEventsService.class);
    }


    private static String getIp(HttpServletRequest request) {
        if (request != null) {
            final String header = request.getHeader("X-Forwarded-For");
            if (Strings.isNullOrEmpty(header)) {
                return request.getRemoteAddr();
            }
            final Iterable<String> ipAddresses = COMMA_SPLITTER.split(header);
            final Iterator<String> iterator = ipAddresses.iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    @Override
    public String getEventCategory() {
        return HippoEventConstants.CATEGORY_SECURITY;
    }

    @Override
    public String getChannelName() {
        return "ip-filter-listener";
    }

    @Override
    public boolean onlyNewEvents() {
        return true;
    }

    @Override
    public void onHippoEvent(final HippoEvent event) {
        final String application = event.application();
        if (!Strings.isNullOrEmpty(application) && application.equals(IpFilterEvent.APPLICATION)) {
            log.debug("invalidating cache for: {}", event);
            final String ranges = (String) event.get(IpFilterEvent.RANGES);
            populateIpRanges(ranges);

        }
    }

}
