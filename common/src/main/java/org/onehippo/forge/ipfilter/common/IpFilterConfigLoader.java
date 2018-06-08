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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.forge.ipfilter.common.file.FileChangeObserver;
import org.onehippo.forge.ipfilter.common.file.FileWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public abstract class IpFilterConfigLoader implements FileChangeObserver {

    private static final Logger log = LoggerFactory.getLogger(IpFilterConfigLoader.class);

    private static final String FILE_PROTOCOL = "file://";
    private static final String CATALINA_BASE = "catalina.base";
    private static final String CATALINA_HOME = "catalina.home";

    private String configurationLocation;
    private Repository repository;
    private Credentials credentials;

    private Date lastLoadDate = new Date();
    protected volatile boolean needRefresh = true;

    private final Map<String, AuthObject> data = new ConcurrentHashMap<>();

    public IpFilterConfigLoader() {

        try {
            new FileWatchService(this, getWatchedDirectories(), getWatchedFiles());
        } catch (IOException e) {
            log.error("Error initializing file watch service", e);
        }
    }

    private Set<String> getWatchedFiles() {
        final String filePath = System.getProperty(IpFilterConstants.PROPERTIES_NAME);
        if (!Strings.isNullOrEmpty(filePath)) {
            final String path = normalizeResourcePath(filePath);
            final File file = new File(path);
            if (file.exists()) {
                final String fileName = file.getName();
                log.info("Adding file '{}' to be watched for changes", fileName);
                return ImmutableSet.of(fileName);
            } else {
                log.warn("path does not exist: {}", path);
                return Collections.emptySet();
            }
        }
        return ImmutableSet.of(IpFilterConstants.PROPERTIES_NAME);
    }

    private Set<String> getWatchedDirectories() {
        final String filePath = System.getProperty(IpFilterConstants.PROPERTIES_NAME);

        // if file is configured, watch only that directory
        if (!Strings.isNullOrEmpty(filePath)) {
            final String path = normalizeResourcePath(filePath);
            final File file = new File(path);
            if (file.exists()) {
                final File parentFile = file.getParentFile();
                final String absolutePath = parentFile.getAbsolutePath();
                log.info("Adding directory {} to be watched for changes", absolutePath);
                return ImmutableSet.of(absolutePath);
            }

            // configured file may not exist yet, but its directory can, NB on Windows "/" can be used as well
            final int lastSep = Math.max(path.lastIndexOf("/"), path.lastIndexOf(File.separator));
            if (lastSep > 0) {
                final String dirPath = path.substring(0, lastSep);
                final File dir = new File(dirPath);
                if (dir.exists()) {
                    final String absolutePath = dir.getAbsolutePath();
                    log.info("Adding directory {} to be watched for changes", absolutePath);
                    return ImmutableSet.of(absolutePath);
                }
            }

            log.warn("Not adding any watched directory: path {} does not exist", path);
            return Collections.emptySet();
        }

        final File catalinaBase = getConfDir(System.getProperty(CATALINA_BASE));
        final Set<String> dirs = new HashSet<>();
        if (catalinaBase != null) {
            dirs.add(catalinaBase.getAbsolutePath());
        }
        final File catalinaHome = getConfDir(System.getProperty(CATALINA_HOME));
        if (catalinaHome != null) {
            dirs.add(catalinaHome.getAbsolutePath());
        }

        return new ImmutableSet.Builder<String>().addAll(dirs).build();
    }

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

    @Override
    public void update(final File file) {
        log.info("file changed, need refresh: {}", file);
        needRefresh = true;
    }

    private Multimap<String, String> loadGlobalSettings() {
        try {
            // get path from System property or catalina/conf
            final String propertiesPath = getGlobalPropertiesPath();
            if (Strings.isNullOrEmpty(propertiesPath)) {
                log.info("No {} path found as system property of in conf directories of ${{}}, ${{}}",
                        IpFilterConstants.PROPERTIES_NAME, CATALINA_BASE, CATALINA_HOME);
                return ImmutableListMultimap.of();
            }

            log.debug("Loading properties from {}", propertiesPath);
            final String path = normalizeResourcePath(propertiesPath);
            if (!new File(path).exists()) {
                log.info("Path doesn't exists {}", path);
                return ImmutableListMultimap.of();
            }

            final Resource resource = new FileSystemResource(path);
            final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            final Multimap<String, String> map = ArrayListMultimap.create();
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
            log.error("Error loading {}", IpFilterConstants.PROPERTIES_NAME, e);
        }
        
        return ImmutableListMultimap.of();
    }

    private String normalizeResourcePath(final String filePath) {
        if (filePath.startsWith(FILE_PROTOCOL)) {
            return filePath.substring(FILE_PROTOCOL.length());
        }
        return filePath;
    }

    private String getGlobalPropertiesPath() {
        String propertiesPath = System.getProperty(IpFilterConstants.PROPERTIES_NAME);
        if (Strings.isNullOrEmpty(propertiesPath)) {
            log.debug("No system property {} found, will try ${{}}/conf", IpFilterConstants.PROPERTIES_NAME, CATALINA_BASE);
            propertiesPath = getPropertiesFromConfDir(System.getProperty(CATALINA_BASE));
            if (Strings.isNullOrEmpty(propertiesPath)) {
                log.debug("No file {} within ${{}}/conf found, will try ${{}}/conf", IpFilterConstants.PROPERTIES_NAME, CATALINA_BASE, CATALINA_HOME);
                propertiesPath = getPropertiesFromConfDir(System.getProperty(CATALINA_HOME));
            }
        }
        return propertiesPath;
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

    private File getConfDir(final String root) {
        if (Strings.isNullOrEmpty(root)) {
            return null;
        }
        final String path = root.endsWith(File.separator) ? root : root + File.separator;
        final File file = new File(path + "conf");
        if (file.exists() && file.isDirectory()) {
            return file;
        }
        return null;
    }

    private String getPropertiesFromConfDir(final String root) {
        final File confDir = getConfDir(root);
        if (confDir == null) {
            return null;
        }

        final String fullPath = confDir.getAbsolutePath() + File.separator + IpFilterConstants.PROPERTIES_NAME;
        final File file = new File(fullPath);
        if (file.exists()) {
            return fullPath;
        }

        return null;
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

    public Date getLastLoadDate() {
        return lastLoadDate;
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
