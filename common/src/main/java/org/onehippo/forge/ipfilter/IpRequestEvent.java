package org.onehippo.forge.ipfilter;

import org.onehippo.cms7.event.HippoEvent;

public class IpRequestEvent<T extends IpRequestEvent<T>> extends HippoEvent<T> {

    public IpRequestEvent() {
        super("IpRequestEvent");
    }


    public IpRequestEvent(final String application) {
        super(application);
    }

    public IpRequestEvent(final HippoEvent<?> event) {
        super(event);
    }


}
