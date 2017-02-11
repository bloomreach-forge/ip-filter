package org.onehippo.forge.ipfilter;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;

/**
 * Persistent event used to transfer data from hippo module to IpFilter, persistent event
 */
public class IpFilterEvent<T extends IpFilterEvent<T>> extends HippoEvent<T> {

    public IpFilterEvent(final String application) {
        super(application);
        user(IpFilterConstants.APPLICATION);
        category(HippoEventConstants.CATEGORY_SECURITY);

    }

    public IpFilterEvent(final HippoEvent<?> event) {
        super(event);
        user(IpFilterConstants.APPLICATION);
        category(HippoEventConstants.CATEGORY_SECURITY);
    }


    public T data(final String data) {
        return put(IpFilterConstants.DATA, data);
    }

    public String data() {
        return get(IpFilterConstants.DATA);
    }

}
