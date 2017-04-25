package org.onehippo.forge.ipfilter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpFilterModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(IpFilterModule.class);
    
    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        log.debug("Re(configuring) {}", this.getClass().getName());
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {

    }
    
    @Override
    protected void doShutdown() {
        
    }


}
