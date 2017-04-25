package org.onehippo.forge.ipfilter;

import javax.jcr.observation.Event;

import org.hippoecm.hst.core.jcr.GenericEventListener;

public class IpFilterEventListener extends GenericEventListener {


    private IpFilterConfigLoader configLoader;

    public void setConfigLoader(final IpFilterConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @Override
    protected void onNodeAdded(Event event) {
        doInvalidation(event);
    }

    @Override
    protected void onNodeRemoved(Event event) {
        doInvalidation(event);
    }

    @Override
    protected void onPropertyAdded(Event event) {
        doInvalidation(event);
    }

    @Override
    protected void onPropertyChanged(Event event) {
        doInvalidation(event);
    }

    @Override
    protected void onPropertyRemoved(Event event) {
        doInvalidation(event);
    }

    private void doInvalidation(final Event path) {
        configLoader.invalidate(path);
    }
}
