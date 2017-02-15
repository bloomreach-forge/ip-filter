package org.onehippo.forge.ipfilter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import org.apache.sanselan.formats.jpeg.iptc.IPTCConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AuthObject {

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(AuthObject.class);
    private boolean active = true;
    private boolean mustMatchAll;
    private boolean allowCmsUsers;
    private String forwardedForHeader;
    private String[] hosts;
    private Set<String> ranges;
    private Set<String> ignoredPaths;
    @JsonIgnore
    private Set<IpMatcher> ipMatchers;

    @JsonIgnore
    private List<Pattern> hostPatterns;
    @JsonIgnore
    private List<Pattern> ignoredPathPatterns;

    public AuthObject() {
    }

    public Set<String> getIgnoredPaths() {
        if (ignoredPaths == null) {
            ignoredPaths = new HashSet<>();
        }
        return ignoredPaths;
    }

    public void setIgnoredPaths(final Set<String> ignoredPaths) {
        this.ignoredPaths = ignoredPaths;
    }

    public AuthObject(final String[] hosts) {
        this.hosts = hosts;
    }

    public AuthObject(final boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }


    public boolean isAllowCmsUsers() {
        return allowCmsUsers;
    }

    public void setAllowCmsUsers(final boolean allowCmsUsers) {
        this.allowCmsUsers = allowCmsUsers;
    }

    public Set<String> getRanges() {
        return ranges;
    }

    public void setRanges(final Set<String> ranges) {
        this.ranges = ranges;
    }


    public boolean isMustMatchAll() {
        return mustMatchAll;
    }

    public void setMustMatchAll(final boolean mustMatchAll) {
        this.mustMatchAll = mustMatchAll;
    }


    public String[] getHosts() {
        return hosts;
    }

    public void setHosts(final String[] hosts) {
        this.hosts = hosts;
    }

    public List<Pattern> getHostPatterns() {
        if (hostPatterns == null) {
            if (hosts == null) {
                throw new IllegalStateException("No host names provided");
            }
            hostPatterns = new ArrayList<>();
            for (String host : hosts) {
                try {
                    hostPatterns.add(Pattern.compile(host));
                } catch (Exception e) {
                    log.error("Invalid host value {}", host);
                    log.error("Error compiling host pattern: ", e);
                }
            }
        }
        return hostPatterns;
    }

    public List<Pattern> getIgnoredPathPatterns() {
        if (ignoredPathPatterns == null) {
            ignoredPathPatterns = new ArrayList<>();
            final Set<String> ignoredPaths = getIgnoredPaths();
            for (String ignored : ignoredPaths) {
                try {
                    ignoredPathPatterns.add(Pattern.compile(ignored));
                } catch (Exception e) {
                    log.error("Invalid path value {}", ignored);
                    log.error("Error compiling path pattern: ", e);
                }
            }

        }

        return ignoredPathPatterns;
    }

    public void setHostPatterns(final List<Pattern> hostPatterns) {
        this.hostPatterns = hostPatterns;
    }


    public Set<IpMatcher> getIpMatchers() {
        if (ipMatchers == null) {
            ipMatchers = new HashSet<>();
            if (ranges != null) {
                for (String range : ranges) {
                    final IpMatcher matcher = IpMatcher.valueOf(range);
                    if (matcher != null) {
                        ipMatchers.add(matcher);
                    }
                }
            }
        }
        return ipMatchers;
    }

    public String getForwardedForHeader() {
        return forwardedForHeader;
    }

    public void setForwardedForHeader(final String forwardedForHeader) {
        this.forwardedForHeader = forwardedForHeader;
    }

    public void setIpMatchers(final Set<IpMatcher> ipMatchers) {
        this.ipMatchers = ipMatchers;
    }

    public void addIgnoredPath(final String path) {
        if (!Strings.isNullOrEmpty(path)) {
            // initialize
            getIgnoredPaths();
            ignoredPaths.add(path);
        }
    }
}
