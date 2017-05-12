package org.onehippo.forge.ipfilter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = IpFilterService.class)
public class IpFilterModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(IpFilterModule.class);

    private IpFilterService service;
    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        log.debug("Re(configuring) {}", this.getClass().getName());
        if (service != null) {
            service.setConfigurationChanged(true);
        }
    }
    
    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        service = new IpFilterServiceImpl(session);
        HippoServiceRegistry.registerService(service, IpFilterService.class);
    }
    
    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregisterService(service, IpFilterService.class);
    }


}
