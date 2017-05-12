package org.onehippo.forge.ipfilter;

import javax.jcr.Session;

public class CmsConfigLoader extends IpFilterConfigLoader{

    private final Session session;
    private final IpFilterService service;

    public CmsConfigLoader(final Session session, final IpFilterService service) {
        this.session = session;
        this.service = service;
        setConfigurationLocation("/hippo:configuration/hippo:modules/ipfilter-module/hippo:moduleconfig");
    }


    @Override
    public boolean needReloading() {
        if (service.configurationChanged()) {
            needRefresh = true;
            service.setConfigurationChanged(false);
        }
        return needRefresh;

    }

    @Override
    protected void closeSession(final Session session) {
        // ignore for CMS module, we share one session
    }

    @Override
    public Session getSession() {
        return session;
    }
}
