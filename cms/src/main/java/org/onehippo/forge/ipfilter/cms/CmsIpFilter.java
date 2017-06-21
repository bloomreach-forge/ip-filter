/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.ipfilter.cms;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.ipfilter.common.BaseIpFilter;
import org.onehippo.forge.ipfilter.common.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmsIpFilter extends BaseIpFilter {

    private static final Logger log = LoggerFactory.getLogger(CmsIpFilter.class);

    private static final String SYSTEM_PROPERTY_DISABLED = "hippo.cms-ipfilter.disabled";

    @Override
    protected Status authenticate(final HttpServletRequest request) {
        log.debug("Authenticating for repository user, however we are within CMS application so *skipping* authentication");
        return Status.OK;
    }

    @Override
    protected void initializeConfigManager() {
        // check CMS service first
        final IpFilterService service = HippoServiceRegistry.getService(IpFilterService.class);
        if (service != null) {
            final Session session = service.getSession();
            if (session == null) {
                log.warn("IpFilterService has no session");
                return;
            }
            configLoader = new CmsConfigLoader(session, service);
            log.info("Successfully configured IpFilterService");
            initialized = true;
        }
        else {
            // info because always so on startup
            log.info("IpFilterService not yet available in registry");
        }
    }

    @Override
    protected String getDisabledPropertyName() {
        return SYSTEM_PROPERTY_DISABLED;
    }
}
