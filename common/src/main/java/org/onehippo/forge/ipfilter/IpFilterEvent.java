package org.onehippo.forge.ipfilter;

import org.onehippo.cms7.event.HippoEvent;

public class IpFilterEvent<T extends IpFilterEvent<T>> extends HippoEvent<T> {

    static final String ALLOWED_IP_RANGES = "allowed-ip-ranges";
    public static final String RANGES = "ip-ranges";
    public static final String APPLICATION = "ip-filter-app";

    public IpFilterEvent(final String application) {
        super(application);
    }

    public IpFilterEvent(final HippoEvent<?> event) {
        super(event);
    }


    public T ipRanges(final String ranges) {
        return put(RANGES, ranges);
    }

    public String ipRanges() {
        return get(RANGES);
    }

}
