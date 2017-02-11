package org.onehippo.forge.ipfilter;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;

/**
 * Used to request data from hippo module, persistent event
 */
public class IpRequestEvent<T extends IpRequestEvent<T>> extends HippoEvent<T> {
    public IpRequestEvent() {
        super(IpFilterConstants.IP_REQUEST_EVENT);
        user(IpFilterConstants.APPLICATION);
        category(HippoEventConstants.CATEGORY_SECURITY);
    }
}
