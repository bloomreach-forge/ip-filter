package org.onehippo.forge.ipfilter;

import com.google.common.base.Strings;
import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;

import static org.onehippo.forge.ipfilter.IpFilterConstants.*;

public class IpFilterModule extends AbstractReconfigurableDaemonModule implements PersistedHippoEventListener {

    private static final Logger log = LoggerFactory.getLogger(IpFilterModule.class);
    private static final int EVENT_DELAY = 1000;
    private static final String EMPTY_DATA = "{}";
    private long lastTimeSent = System.currentTimeMillis();
    private String data = EMPTY_DATA;
    private final Object lock = new Object();
    private boolean configured;
    private boolean initialized;


    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        log.debug("Reconfiguring {}", this.getClass().getName());
        configured = false;
        synchronized (lock) {
            final Map<String, AuthObject> objects = new HashMap<>();
            final NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                final Node configNode = nodes.nextNode();
                final AuthObject authObject = parse(configNode);
                if (authObject != null) {
                    objects.put(configNode.getName(), authObject);
                }
            }
            data = IpFilterUtils.toJson(objects);
            if (data == null) {
                data = EMPTY_DATA;
            }
        }
        configured = true;
        sendEvent();
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
        object.setMustMatchAll(JcrUtils.getBooleanProperty(node, IpFilterConstants.CONFIG_MATCH_ALL, false));
        object.setHosts(hosts);
        final Set<String> rangesSet = new HashSet<>();
        Collections.addAll(rangesSet, ranges);
        final String[] ignoredPaths = JcrUtils.getMultipleStringProperty(node, IpFilterConstants.CONFIG_IGNORED_PATHS, ArrayUtils.EMPTY_STRING_ARRAY);
        final Set<String> ignored = new HashSet<>();
        Collections.addAll(ignored, ignoredPaths);
        object.setRanges(rangesSet);
        object.setIgnoredPaths(ignored);
        return object;
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        initialized = true;
        this.session = session;
        HippoServiceRegistry.registerService(this, HippoEventBus.class);
    }

    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregisterService(this, HippoEventBus.class);
    }

    /**
     * sends a persistent event with
     * all data serialized in JSON format
     */
    private void sendEvent() {
        if (!initialized || !configured) {
            log.warn("Not configured or initialized yet, skipping sending of event");
        }
        final long current = System.currentTimeMillis();
        final long diff = current - lastTimeSent;
        // update last time
        lastTimeSent = current;
        /*
         * in a clustered environment, each site node will request data.
         * because we are sending a persistent event we should
         * try to avoid flooding system with unneeded event sending
         */
        if (diff < EVENT_DELAY) {
            log.debug("Last event sent sent: {} ms ago, skipping this event", diff);
            return;
        }
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus == null) {
            log.warn("No event bus service found by class {}", HippoEventBus.class.getName());
        } else {
            if (data == null) {
                data = EMPTY_DATA;
            }
            eventBus.post(new IpFilterEvent(IpFilterConstants.APPLICATION).data(data));
        }
    }


    @Override
    public String getEventCategory() {
        return HippoEventConstants.CATEGORY_SECURITY;
    }

    @Override
    public String getChannelName() {
        return IP_FILTER_MODULE_CHANNEL;
    }

    @Override
    public boolean onlyNewEvents() {
        return true;
    }

    @Override
    public void onHippoEvent(final HippoEvent event) {
        final String application = event.application();
        if (!Strings.isNullOrEmpty(application) && application.equals(IpFilterConstants.IP_REQUEST_EVENT)) {
            log.debug("Processing IP filter request event: {}", event);
            sendEvent();
        }

    }
}
