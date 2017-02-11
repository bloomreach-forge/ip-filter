package org.onehippo.forge.ipfilter;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private String[] hosts;
    private Set<String> ranges;
    @JsonIgnore
    private Set<IpMatcher> ipMatchers;

    @JsonIgnore
    private List<Pattern> hostPatterns;

    public AuthObject() {
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
                    log.error("Invalid value {}", host);
                    log.error("Error compiling pattern: ", e);
                }
            }
        }
        return hostPatterns;
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

    public void setIpMatchers(final Set<IpMatcher> ipMatchers) {
        this.ipMatchers = ipMatchers;
    }

}
