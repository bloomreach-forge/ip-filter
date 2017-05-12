package org.onehippo.forge.ipfilter;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpFilterCms extends IpFilterCommon {

    private static final Logger log = LoggerFactory.getLogger(IpFilterCms.class);
    
    @Override
    protected Status authenticate(final HttpServletRequest request) {
        log.debug("Flag allowCmsUsers is set, however we are within CMS app so *skipping* cms authentication");
        return Status.OK;
    }
}
