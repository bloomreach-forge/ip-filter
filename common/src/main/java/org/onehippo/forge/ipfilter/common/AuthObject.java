/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthObject {

    private static final Logger log = LoggerFactory.getLogger(AuthObject.class);

    private final boolean valid;
    private final boolean mustMatchAll;
    private final boolean allowCmsUsers;
    private final String forwardedForHeader;
    private final Set<String> hosts;
    private final Set<String> ranges;
    private final Set<String> ignoredPaths;
    private final Set<IpMatcher> ipMatchers;
    private final Map<String, Set<String>> ignoredHeaders;
    private final List<Pattern> hostPatterns;
    private final List<Pattern> ignoredPathPatterns;
    public static final AuthObject INVALID = new AuthObject();

    private AuthObject() {
        this.valid = false;
        this.ignoredPaths = Collections.emptySet();
        this.hosts = Collections.emptySet();
        this.ranges = Collections.emptySet();
        this.ignoredHeaders = Collections.emptyMap();
        this.ignoredPathPatterns = Collections.emptyList();
        this.hostPatterns = Collections.emptyList();
        this.ipMatchers = Collections.emptySet();
        this.allowCmsUsers = false;
        this.mustMatchAll = false;
        this.forwardedForHeader = IpFilterConstants.HEADER_X_FORWARDED_FOR;
    }

    public AuthObject(final Set<String> ignoredPaths, final Set<String> hosts,
                      final Set<String> ranges, final Map<String, Set<String>> ignoredHeaders,
                      final boolean allowCmsUsers, final String forwardHeader, final boolean mustMatchAll) {
        this.valid = true;
        this.ignoredPaths = ignoredPaths;
        this.hosts = hosts;
        this.ranges = ranges;
        this.allowCmsUsers = allowCmsUsers;
        this.mustMatchAll = mustMatchAll;
        this.forwardedForHeader = forwardHeader;
        this.ignoredHeaders = ImmutableMap.copyOf(ignoredHeaders);
        this.ignoredPathPatterns = parsePatterns();
        this.hostPatterns = parseHostPatterns();
        this.ipMatchers = parseIpMatchers();
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isAllowCmsUsers() {
        return allowCmsUsers;
    }


    public boolean isMustMatchAll() {
        return mustMatchAll;
    }


    public List<Pattern> getHostPatterns() {
        return hostPatterns;
    }

    public List<Pattern> getIgnoredPathPatterns() {
        return ignoredPathPatterns;
    }

    public Set<IpMatcher> getIpMatchers() {
        return ipMatchers;
    }

    public String getForwardedForHeader() {
        return forwardedForHeader;
    }


    public Map<String, Set<String>> getIgnoredHeaders() {
        return ignoredHeaders;
    }


    //############################################
    // PARSERS
    //############################################
    private List<Pattern> parseHostPatterns() {
        if (hosts == null) {
            throw new IllegalStateException("No host names provided");
        }
        final List<Pattern> patterns = new ArrayList<>();
        for (String host: hosts) {
            if (Strings.isNullOrEmpty(host)) {
                log.warn("Skipping empty host value");
                continue;
            }
            try {
                patterns.add(Pattern.compile(host));
            } catch (Exception e) {
                log.error("Invalid host value {}", host);
                log.error("Error compiling host pattern: ", e);
            }
        }
        return ImmutableList.copyOf(patterns);
    }

    private Set<IpMatcher> parseIpMatchers() {
        final Set<IpMatcher> ipMatchers = new HashSet<>();
        if (ranges != null) {
            for (String range: ranges) {
                final IpMatcher matcher = IpMatcher.valueOf(range);
                if (matcher != null) {
                    ipMatchers.add(matcher);
                }
            }
        }
        return ImmutableSet.copyOf(ipMatchers);
    }

    private List<Pattern> parsePatterns() {
        final List<Pattern> patterns = new ArrayList<>();
        for (String ignored: ignoredPaths) {
            try {
                if (Strings.isNullOrEmpty(ignored)) {
                    log.debug("Ignoring empty path");
                    continue;
                }
                final Pattern pattern = Pattern.compile(ignored);
                patterns.add(pattern);
            } catch (Exception e) {
                //noinspection StringConcatenationArgumentToLogCall
                log.error("Error compiling path pattern " + ignored, e);
            }
        }
        return ImmutableList.copyOf(patterns);
    }


}
