package org.onehippo.forge.ipfilter;

import javax.jcr.Session;

public class IpFilterServiceImpl implements IpFilterService {

    private final Session session;
    /**
     * NOTE: default must be true to load initial data
     */
    private boolean changed  = true;

    public IpFilterServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public boolean configurationChanged() {
        return changed;
    }

    @Override
    public void setConfigurationChanged(final boolean configurationChanged) {
        changed = configurationChanged;
    }

}


