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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import javax.jcr.*;
import javax.jcr.observation.Event;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IpFilterConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(IpFilterConfigLoader.class);
    private static final String FILE_PREFIX = "file:";

    private String configurationLocation;
    private Repository repository;
    private Credentials credentials;
    private Date lastLoadDate = new Date();
    protected volatile boolean needRefresh = true;
    private final Map<String, AuthObject> data = new ConcurrentHashMap<>();

    public boolean needReloading() {
        return needRefresh;
    }

    public synchronized Map<String, AuthObject> load() {
        // check if refresh is needed..if not return local copy
        if (!needReloading()) {
            return data;
        }

        log.debug("Previously loaded: {}", lastLoadDate);
        Session session = null;
        try {
            session = getSession();
            if (session == null) {
                log.warn("Session was null, cannot load ip filter config data");
                return data;
            }
            final Node node = session.getNode(configurationLocation);
            parseConfig(node);
        } catch (Exception e) {
            log.error("Error loading ip filter configuration", e);
        } finally {
            closeSession(session);
        }
        needRefresh = false;
        lastLoadDate = new Date();
        return data;
    }

    public synchronized void invalidate(final Event event) {
        // we invalidate on any event:
        needRefresh = true;
    }

    public Multimap<String, String> loadGlobalSettings() {
        try {
            // first check if system property is set
            final String systemProperty = getSystemProperty();
            if (Strings.isNullOrEmpty(systemProperty)) {
                log.info("No ipfilter.properties, catalina.home, nor catalina.base found");
                return ImmutableListMultimap.of();
            }
            log.debug("Loading properties from: {}", systemProperty);
            final Resource resource = new FileSystemResource(normalizeResourcePath(systemProperty));
            final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            Multimap<String, String> map = ArrayListMultimap.create();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                final String key = (String) entry.getKey();
                final String value = (String) entry.getValue();
                if (!Strings.isNullOrEmpty(value)) {
                    final Iterator<? extends String> iterator = Splitter.on(',').omitEmptyStrings().trimResults().split(value).iterator();
                    final List<String> list = Lists.newArrayList(iterator);
                    map.putAll(key, list);
                }
            }
            return map;

        } catch (IOException e) {
            log.error("Error loading properties", e);
        }
        return ImmutableListMultimap.of();
    }

    private String normalizeResourcePath(final String systemProperty) {
        if (systemProperty.startsWith(FILE_PREFIX)) {
            return systemProperty.substring(FILE_PREFIX.length(), systemProperty.length());
        }
        return systemProperty;
    }


    private String getSystemProperty() {
        String systemProperty = System.getProperty(IpFilterConstants.IP_FILTER_PROPERTY_NAME);
        if (Strings.isNullOrEmpty(systemProperty)) {
            log.debug("No ipfilter.properties system property found, check default, will try catalina.base and catalina.home");
            systemProperty = System.getProperty("catalina.base");
            systemProperty = createCatalinaPath(systemProperty);
            if (Strings.isNullOrEmpty(systemProperty)) {
                log.debug("No file within catalina.base found trying catalina.home");
                systemProperty = createCatalinaPath(System.getProperty("catalina.home"));
            }
        }
        return systemProperty;
    }

    private String createCatalinaPath(final String systemProperty) {
        if (Strings.isNullOrEmpty(systemProperty)) {
            return null;
        }
        final String path = systemProperty.endsWith(File.separator) ? systemProperty : systemProperty + File.separator;
        final String fullPath = path + "conf" + File.separator + IpFilterConstants.IP_FILTER_PROPERTY_NAME;
        final FileSystemResource resource = new FileSystemResource(normalizeResourcePath(systemProperty));
        if (resource.exists()) {
            return fullPath;
        }
        return null;
    }

    public Date getLastLoadDate() {
        return lastLoadDate;
    }

    protected Session getSession() {
        Session session = null;
        try {
            session = repository.login(credentials);
        } catch (RepositoryException e) {
            log.error("Error obtaining session", e);
        }
        return session;
    }

    protected void closeSession(final Session session) {
        if (session != null) {
            session.logout();
        }
    }

    private void parseConfig(final Node node) throws RepositoryException {
        final Map<String, AuthObject> objects = new HashMap<>();
        final Multimap<String, String> globalSettings = loadGlobalSettings();
        final NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            final Node configNode = nodes.nextNode();
            final AuthObject authObject = parse(configNode, globalSettings);
            if (authObject != null) {
                objects.put(configNode.getName(), authObject);
            }
        }
        data.clear();
        data.putAll(objects);
    }

    private AuthObject parse(final Node node, final Multimap<String, String> globalSettings) throws RepositoryException {
        final boolean enabled = JcrUtils.getBooleanProperty(node, IpFilterConstants.CONFIG_ENABLED, true);
        if (!enabled) {
            log.info("Configuration disabled for configuration at {}", node.getPath());
            return null;
        }

        final Set<String> ignoredPathSet = new HashSet<>();
        final Collection<String> globalPaths = globalSettings.get(IpFilterConstants.CONFIG_IGNORED_PATHS);
        final String[] globalIgnoredPaths = globalPaths == null ? ArrayUtils.EMPTY_STRING_ARRAY : globalPaths.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        final String[] ignoredPaths = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_IGNORED_PATHS, ArrayUtils.EMPTY_STRING_ARRAY);
        Collections.addAll(ignoredPathSet, ignoredPaths);
        Collections.addAll(ignoredPathSet, globalIgnoredPaths);

        final Set<String> hostSet = new HashSet<>();

        final String[] hosts = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_HOSTNAME, null);
        if (hosts == null) {
            log.error("Host names property ({}) is missing for configuration at {}", IpFilterConstants.CONFIG_HOSTNAME, node.getPath());
            return null;
        }
        Collections.addAll(hostSet, hosts);

        final Set<String> rangesSet = new HashSet<>();
        final Collection<String> globalAllowed = globalSettings.get(IpFilterConstants.CONFIG_ALLOWED_IP_RANGES);
        final String[] globalRange = globalAllowed == null ? ArrayUtils.EMPTY_STRING_ARRAY : globalAllowed.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        final String[] ranges = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_ALLOWED_IP_RANGES, ArrayUtils.EMPTY_STRING_ARRAY);
        Collections.addAll(rangesSet, ranges);
        Collections.addAll(rangesSet, globalRange);
        final boolean allowCmsUsers = JcrUtils.getBooleanProperty(node, IpFilterConstants.CONFIG_ALLOW_CMS_USERS, false);
        if (!allowCmsUsers && ranges.length == 0) {
            log.warn("Invalid configuration at {}: no IP addresses nor CMS users are enabled", node.getPath());
            return null;
        }


        final Map<String, Set<String>> ignoredHeaders = parseHeaders(node);

        final String forwardHeader = JcrUtils.getStringProperty(node, IpFilterConstants.CONFIG_FORWARDED_FOR_HEADER, IpFilterConstants.HEADER_X_FORWARDED_FOR);
        final boolean matchAll = JcrUtils.getBooleanProperty(node, IpFilterConstants.CONFIG_MATCH_ALL, false);

        return new AuthObject(ignoredPathSet, hostSet, rangesSet, ignoredHeaders, allowCmsUsers, forwardHeader, matchAll);
    }

    private Map<String, Set<String>> parseHeaders(final Node root) throws RepositoryException {
        final Map<String, Set<String>> ignoredHeaders = new HashMap<>();
        final NodeIterator nodes = root.getNodes();
        while (nodes.hasNext()) {
            final Node node = nodes.nextNode();
            final String[] ignoredHeaderValues = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_IGNORED_HEADER_VALUES, ArrayUtils.EMPTY_STRING_ARRAY);
            final Set<String> ignoredHeaderSet = new HashSet<>();
            Collections.addAll(ignoredHeaderSet, ignoredHeaderValues);
            String ignoredHeader = JcrUtils.getStringProperty(node, IpFilterConstants.CONFIG_IGNORED_HEADER, null);
            if (!Strings.isNullOrEmpty(ignoredHeader) && !ignoredHeaderSet.isEmpty()) {
                addIgnoreHeader(ignoredHeaders, ignoredHeader, ignoredHeaderSet);
            }
        }
        return ignoredHeaders;
    }


    private void addIgnoreHeader(final Map<String, Set<String>> existingMap, final String ignoredHeader, final Set<String> ignoredHeaderSet) {
        for (String value : ignoredHeaderSet) {
            if (!Strings.isNullOrEmpty(value)) {
                if (existingMap.get(ignoredHeader) == null) {
                    Set<String> values = new HashSet<>();
                    values.add(value);
                    existingMap.put(ignoredHeader, values);
                } else {
                    // add to existing set:
                    final Set<String> values = existingMap.get(ignoredHeader);
                    values.add(value);
                }
            }
        }
    }


    public String getConfigurationLocation() {
        return configurationLocation;
    }

    public void setConfigurationLocation(final String configurationLocation) {
        this.configurationLocation = configurationLocation;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }


}
