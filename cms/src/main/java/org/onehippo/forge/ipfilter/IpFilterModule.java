package org.onehippo.forge.ipfilter;

import com.google.common.base.Joiner;
import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IpFilterModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(IpFilterModule.class);
    private Session session;
    private List<String> ipRange = new CopyOnWriteArrayList<>();


    @Subscribe
    public void handleEvent(final IpRequestEvent event) {
        log.debug("Processing IP filter request event: {}", event);

        sendEvent();
    }

    private void sendEvent() {
        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus == null) {
            log.warn("No event bus service found by class {}", HippoEventBus.class.getName());
        } else {
            final String ranges = Joiner.on(",").join(ipRange);
            eventBus.post(new IpFilterEvent(IpFilterEvent.APPLICATION).ipRanges(ranges).user(session.getUserID()).category(HippoEventConstants.CATEGORY_SECURITY));
        }
    }


    private void refresh() {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.error("", e);
        }
    }

    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        log.debug("Reconfiguring {}", this.getClass().getName());
        ipRange.clear();
        if (node.hasProperty(IpFilterEvent.ALLOWED_IP_RANGES)) {
            final String[] values = JcrUtils.getMultipleStringProperty(node, IpFilterEvent.ALLOWED_IP_RANGES, ArrayUtils.EMPTY_STRING_ARRAY);
            Collections.addAll(ipRange, values);
            sendEvent();
        } else {
            log.warn("Configuration is missing {} property", IpFilterEvent.ALLOWED_IP_RANGES);
        }
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        this.session = session;
        HippoServiceRegistry.registerService(this, HippoEventBus.class);
    }

    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregisterService(this, HippoEventBus.class);
    }


}
