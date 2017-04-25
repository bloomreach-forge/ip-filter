package org.onehippo.forge.ipfilter;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class IpFilterConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(IpFilterConfigLoader.class);

    private String configurationLocation;
    private Repository repository;
    private Credentials credentials;
    private Date lastLoadDate = new Date();
    private volatile boolean needRefresh = true;
    private final Map<String, AuthObject> data = new ConcurrentHashMap<>();


    public boolean needReloading() {
        return needRefresh;
    }


    public synchronized Map<String, AuthObject> load() {
        // check if refresh is needed..if not return local copy
        if (!needRefresh) {
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
        final NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            final Node configNode = nodes.nextNode();
            final AuthObject authObject = parse(configNode);
            if (authObject != null) {
                objects.put(configNode.getName(), authObject);
            }
        }
        data.clear();
        data.putAll(objects);
    }

    private AuthObject parse(final Node node) throws RepositoryException {
        final String name = node.getName();
        final boolean enabled = JcrUtils.getBooleanProperty(node, IpFilterConstants.CONFIG_ENABLED, true);
        if (!enabled) {
            log.info("Configuration not enabled for: configuration: {}", name);
            return null;
        }
        final String[] hosts = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_HOSTNAME, null);
        if (hosts == null) {
            log.error("Host names property ({}) is missing for configuration: {}", IpFilterConstants.CONFIG_HOSTNAME, name);
            return null;
        }
        final String[] ranges = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_ALLOWED_IP_RANGES, ArrayUtils.EMPTY_STRING_ARRAY);
        final boolean allowCmsUsers = JcrUtils.getBooleanProperty(node, IpFilterConstants.CONFIG_ALLOW_CMS_USERS, false);

        if (!allowCmsUsers && ranges.length == 0) {
            log.warn("Invalid configuration ({}), no IP addresses nor CMS users are enabled", name);
            return null;
        }
        final AuthObject object = new AuthObject();
        object.setActive(true);
        object.setAllowCmsUsers(allowCmsUsers);
        object.setForwardedForHeader(JcrUtils.getStringProperty(node, IpFilterConstants.CONFIG_FORWARDED_FOR_HEADER, null));
        object.setMustMatchAll(JcrUtils.getBooleanProperty(node, IpFilterConstants.CONFIG_MATCH_ALL, false));
        object.setHosts(hosts);
        final Set<String> rangesSet = new HashSet<>();
        Collections.addAll(rangesSet, ranges);
        final String[] ignoredPaths = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_IGNORED_PATHS, ArrayUtils.EMPTY_STRING_ARRAY);
        final Set<String> ignored = new HashSet<>();
        Collections.addAll(ignored, ignoredPaths);
        // headers
        parseHeaders(object, node);
        object.setRanges(rangesSet);
        object.setIgnoredPaths(ignored);
        return object;
    }

    private void parseHeaders(final AuthObject object, final Node root) throws RepositoryException {
        final NodeIterator nodes = root.getNodes();
        while (nodes.hasNext()) {
            final Node node = nodes.nextNode();
            final String[] ignoredHeaderValues = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_IGNORED_HEADER_VALUES, ArrayUtils.EMPTY_STRING_ARRAY);
            final Set<String> ignoredHeaderSet = new HashSet<>();
            Collections.addAll(ignoredHeaderSet, ignoredHeaderValues);
            String ignoredHeader = JcrUtils.getStringProperty(node, IpFilterConstants.CONFIG_IGNORED_HEADER, null);
            if (!Strings.isNullOrEmpty(ignoredHeader) && !ignoredHeaderSet.isEmpty()) {
                object.addIgnoreHeader(ignoredHeader, ignoredHeaderSet);
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
