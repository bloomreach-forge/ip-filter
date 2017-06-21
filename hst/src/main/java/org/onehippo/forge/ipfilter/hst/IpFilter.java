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
package org.onehippo.forge.ipfilter.hst;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.onehippo.forge.ipfilter.common.BaseIpFilter;
import org.onehippo.forge.ipfilter.common.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onehippo.forge.ipfilter.common.IpFilterConstants;

/**
 * Filter allowing only access for IP ranges that are configured
 */
public class IpFilter extends BaseIpFilter {

    private static final Logger log = LoggerFactory.getLogger(IpFilter.class);

    private static final String SYSTEM_PROPERTY_DISABLED = "hippo.ipfilter.disabled";

    @Override
    protected Status authenticate(final HttpServletRequest request) {
        final UserCredentials credentials = new UserCredentials(request.getHeader(IpFilterConstants.HEADER_AUTHORIZATION));
        if (!credentials.valid()) {
            log.debug("Invalid credentials, null or empty");
            return Status.UNAUTHORIZED;
        }
        final Boolean cached = userCache.getUnchecked(credentials.getUsername());
        if (cached != null && cached) {
            log.debug("Cached user: {}", credentials);
            return Status.OK;
        }
        Session session = null;
        try {
            // try to authenticate:
            session = getSession(credentials);
            if (session == null) {
                log.debug("No valid session for user: {}", credentials.getUsername());
                return Status.UNAUTHORIZED;
            }
            log.debug("Successfully validated user: {}", credentials.getUsername());
            userCache.put(credentials.getUsername(), Boolean.TRUE);
            return Status.OK;
        } finally {
            closeSession(session);
        }
    }

    private void closeSession(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Override
    protected void initializeConfigManager() {

        if (HstServices.isAvailable()) {
            final ComponentManager componentManager = HstServices.getComponentManager();
            configLoader = componentManager.getComponent(HstConfigLoader.class.getName(), HstConfigLoader.class.getPackage().getName());
            if (configLoader == null) {
                log.error("Configuration loader was null");
            } else {
                initialized = true;
            }
        } else {
            log.info("HstService not available yet...waiting..");
        }

    }

    @Override
    protected String getDisabledPropertyName() {
        return SYSTEM_PROPERTY_DISABLED;
    }

    private Session getSession(final UserCredentials credentials) {
        HippoRepository hippoRepository = getHippoRepository(repositoryAddress);
        if (hippoRepository == null) {
            return null;
        }
        try {
            return hippoRepository.login(credentials.getUsername(), credentials.getPassword().toCharArray());
        } catch (LoginException e) {
            log.debug("Invalid credentials for username '{}'", credentials.getUsername());
            return null;
        } catch (RepositoryException e) {
            log.error("Error during authentication", e);
            return null;
        }
    }

    private HippoRepository getHippoRepository(String address) {
        if (address == null || address.length() == 0) {
            log.error("Repository address parameter {} not set. Unable to perform authorization. Return unauthorized.",
                    IpFilterConstants.REPOSITORY_ADDRESS_PARAM);
            return null;
        }
        try {
            return HippoRepositoryFactory.getHippoRepository(address);
        } catch (RepositoryException e) {
            log.error("Error while obtaining repository: ", e);
            return null;
        }
    }
}
